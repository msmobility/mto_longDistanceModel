package de.tum.bgu.msm.longDistance.zoneSystem;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created by carlloga on 02-05-17.
 */
public class ZoneDisaggregator implements ModelComponent {

    private static Logger logger = Logger.getLogger(ZoneDisaggregator.class);
    private ResourceBundle rb;
    private Collection<Zone> zoneList;
    private Map<Integer, Map<Integer, Zone>> combinedZoneMap;
    private DataSet dataSet;

    private int[] niagaraFallsIds;
    private ArrayList<Zone> niagaraFallsList = new ArrayList<>();
    private double niagaraFactor;
    private float alphaPopDom;
    private float alphaDistDom;
    private float alphaPopInt;
    private float alphaDistInt;

    public ZoneDisaggregator() {

    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        combinedZoneMap = new HashMap<>();
        //parameters
        alphaPopDom = JsonUtilMto.getFloatProp(prop, "disaggregation.domestic.alpha_pop");
        alphaDistDom = JsonUtilMto.getFloatProp(prop, "disaggregation.domestic.alpha_dist");
        alphaPopInt = JsonUtilMto.getFloatProp(prop, "disaggregation.international.alpha_pop");
        alphaDistInt = JsonUtilMto.getFloatProp(prop, "disaggregation.international.alpha_dist");
        niagaraFallsIds = JsonUtilMto.getArrayIntProp(prop, "disaggregation.domestic.niagara_zones");
        niagaraFactor = JsonUtilMto.getFloatProp(prop, "disaggregation.domestic.niagara_factor");
        logger.info("Zone disaggregator set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;
        this.zoneList = dataSet.getZones().values();
        //build the map for raster cells
        for (Zone z : zoneList) {
            if (combinedZoneMap.containsKey(z.getCombinedZoneId())) {
                combinedZoneMap.get(z.getCombinedZoneId()).put(z.getId(), z);
            } else {
                Map<Integer, Zone> internalZoneMap = new HashMap<>();
                internalZoneMap.put(z.getId(), z);
                combinedZoneMap.put(z.getCombinedZoneId(), internalZoneMap);
            }

        }
        //build a list of special zones
        for (int i : niagaraFallsIds) {
            niagaraFallsList.add(dataSet.getZones().get(i));
        }
        logger.info("Zone disaggregator loaded");
    }

    public void run(DataSet dataSet, int nThreads) {

        logger.info("Starting disaggregation");
        dataSet.getAllTrips().parallelStream().forEach(t -> {
            disaggregateDestination(t);
        });
        logger.info("Finished disaggregation");

    }


    public void disaggregateDestination(LongDistanceTrip trip) {

        Zone destZone;

        if (trip.getDestCombinedZoneId() == 30 && trip.getTripPurpose() == 2) {
            //the leisure trip ends in Niagara falls --> go to the falls
            destZone = selectDestinationInNiagara(trip);

        } else {
            if (trip.getMode() != 0) {
                //trips by public transport
                destZone = selectDestinationZonePopBased(trip);
            } else {
                destZone = selectDestinationZonePopDistanceBased(trip);
            }

            //trip.setDestZone(internalZoneMap.get(new EnumeratedIntegerDistribution(alternatives, expUtilities).sample()));


        }

        trip.setDestZone(destZone);

        trip.setTravelDistanceLevel1(dataSet.getAutoTravelDistance(trip.getOrigZone().getId(), trip.getDestZone().getId()));
    }

    private Zone selectDestinationInNiagara(LongDistanceTrip trip) {
        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        int[] alternatives = new int[internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()) {
            alternatives[i] = z.getId();
            expUtilities[i] = (getCivicValues(z, trip));
            if (niagaraFallsList.contains(z))
                expUtilities[i] = expUtilities[i] * niagaraFactor;
            i++;
        }

        return internalZoneMap.get(Util.select(expUtilities, alternatives));

    }


    private Zone selectDestinationZonePopBased(LongDistanceTrip trip) {

        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        int[] alternatives = new int[internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()) {
            alternatives[i] = z.getId();
            expUtilities[i] = (getCivicValues(z, trip));
            i++;
        }

        return internalZoneMap.get(Util.select(expUtilities, alternatives));
    }

    private Zone selectDestinationZonePopDistanceBased(LongDistanceTrip trip) {

        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        float alphaPop = trip.isInternational() ? alphaPopInt : alphaPopDom;
        float alphaDist = trip.isInternational() ? alphaDistInt : alphaDistDom;


        int[] alternatives = new int[internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()) {
            alternatives[i] = z.getId();
            float distance = dataSet.getAutoTravelDistance(trip.getOrigZone().getId(), z.getId());
            //todo threshold
            if (distance > 40) {
                expUtilities[i] = Math.pow(getCivicValues(z, trip), alphaPop) *
                        Math.pow(distance, alphaDist);
            } else {
                expUtilities[i] = 0;
            }
            i++;
        }

        return internalZoneMap.get(Util.select(expUtilities, alternatives));
    }


    public double getCivicValues(Zone zone, LongDistanceTrip trip) {

        double civic = 0;

        switch (trip.getTripPurpose()) {
            case 0:
                //visit
                civic = zone.getPopulation();
                break;
            case 1:
                //business
                civic = zone.getPopulation() + zone.getEmployment();
                break;
            case 2:
                //leisure
                civic = zone.getPopulation() + zone.getEmployment();
                break;
        }

        return civic;
    }

}

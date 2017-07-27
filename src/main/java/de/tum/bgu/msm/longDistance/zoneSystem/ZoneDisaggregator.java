package de.tum.bgu.msm.longDistance.zoneSystem;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import org.apache.log4j.Logger;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 02-05-17.
 */
public class ZoneDisaggregator {

    private static Logger logger = Logger.getLogger(ZoneDisaggregator.class);
    private ResourceBundle rb;
    private ArrayList<Zone> zoneList;
    private Map<Integer, Map<Integer, Zone>> combinedZoneMap;
    private MtoLongDistData mtoLongDistData;

    private int[] niagaraFallsIds;
    private ArrayList<Zone> niagaraFallsList;
    private double niagaraFactor;
    private float alphaPopDom;
    private float alphaDistDom;
    private float alphaPopInt;
    private float alphaDistInt;

    public ZoneDisaggregator(ResourceBundle rb, JSONObject prop, MtoLongDistData mtoLongDistData){
        this.rb = rb;
        combinedZoneMap = new HashMap<>();
        this.mtoLongDistData = mtoLongDistData;
        logger.info("Zone disaggregator set up");

        alphaPopDom = JsonUtilMto.getFloatProp(prop, "disaggregation.dom.alpha_pop");
        alphaDistDom = JsonUtilMto.getFloatProp(prop, "disaggregation.dom.alpha_dist");
        alphaPopInt = JsonUtilMto.getFloatProp(prop, "disaggregation.int.alpha_pop");
        alphaDistInt = JsonUtilMto.getFloatProp(prop, "disaggregation.int.alpha_dist");

        niagaraFallsIds = JsonUtilMto.getArrayIntProp(prop, "disaggregation.dom.niagara_zones");
        niagaraFactor = JsonUtilMto.getFloatProp(prop, "disaggregation.dom.niagara_factor");


    }

    public void loadZoneDisaggregator(){
        this.zoneList = mtoLongDistData.getZoneList();
        for (Zone z : zoneList) {
            if (combinedZoneMap.containsKey(z.getCombinedZoneId())){ ;
                combinedZoneMap.get(z.getCombinedZoneId()).put(z.getId(), z);
            } else {
                Map<Integer, Zone> internalZoneMap = new HashMap<>();
                internalZoneMap.put(z.getId(), z);
                combinedZoneMap.put(z.getCombinedZoneId(), internalZoneMap);
            }

        }

        for (int i : niagaraFallsIds){
            niagaraFallsList.add(mtoLongDistData.getZoneLookup().get(i));
        }

        logger.info("Zone disaggregator loaded");
    }

    public void disaggregateDestination(LongDistanceTrip trip) {

        Zone destZone;

        if (trip.getDestCombinedZoneId() == 30 && trip.getTripPurpose()==2) {
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
            trip.setDestZone(destZone);

            trip.setTravelDistanceLevel1(mtoLongDistData.getAutoTravelDistance(trip.getOrigZone().getId(), trip.getDestZone().getId()));

        }
    }

    private Zone selectDestinationInNiagara(LongDistanceTrip trip) {
        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        int[] alternatives = new int [internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()){
            alternatives[i] = z.getId();
            expUtilities[i] = (z.getPopulation());
            if (niagaraFallsList.contains(z))
                expUtilities[i] = expUtilities[i]*niagaraFactor;
            i++;
        }

        return internalZoneMap.get(Util.select(expUtilities, alternatives));

    }


    private Zone selectDestinationZonePopBased(LongDistanceTrip trip) {

        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        int[] alternatives = new int [internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()){
            alternatives[i] = z.getId();
            expUtilities[i] = (z.getPopulation());
            i++;
        }

        return internalZoneMap.get(Util.select(expUtilities, alternatives));
    }

    private Zone selectDestinationZonePopDistanceBased(LongDistanceTrip trip) {

        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        float alphaPop = trip.isInternational()? alphaPopInt : alphaPopDom;
        float alphaDist = trip.isInternational()? alphaDistInt : alphaDistDom;


        int[] alternatives = new int [internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()){
            alternatives[i] = z.getId();
            expUtilities[i] = Math.pow(z.getPopulation(), alphaPop)*
                    Math.pow(mtoLongDistData.getAutoTravelDistance(trip.getOrigZone().getId(),z.getId()), alphaDist);
            i++;
        }

        return internalZoneMap.get(Util.select(expUtilities, alternatives));
    }


}

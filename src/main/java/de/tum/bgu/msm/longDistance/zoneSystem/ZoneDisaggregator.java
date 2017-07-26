package de.tum.bgu.msm.longDistance.zoneSystem;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import org.apache.log4j.Logger;

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

    public ZoneDisaggregator(ResourceBundle rb, JsonUtilMto prop,  MtoLongDistData mtoLongDistData){
        this.rb = rb;
        combinedZoneMap = new HashMap<>();
        this.mtoLongDistData = mtoLongDistData;

        logger.info("Zone disaggregator set up");
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

        logger.info("Zone disaggregator loaded");
    }

    public void disaggregateDestination(LongDistanceTrip trip){

        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestCombinedZoneId());

        int[] alternatives = new int [internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()){
            alternatives[i] = z.getId();
            expUtilities[i] = (z.getPopulation());
            i++;
        }

        //trip.setDestZone(internalZoneMap.get(new EnumeratedIntegerDistribution(alternatives, expUtilities).sample()));
        trip.setDestZone(internalZoneMap.get(Util.select(expUtilities, alternatives)));

        trip.setTravelDistanceLevel1(mtoLongDistData.getAutoTravelDistance(trip.getOrigZone().getId(), trip.getDestZone().getId()));

    }


}

package de.tum.bgu.msm.longDistance.zoneSystem;

import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 02-05-17.
 */
public class ZoneDissagregator {

    private ResourceBundle rb;
    private ArrayList<Zone> zoneList;
    private Map<Integer, Map<Integer, Zone>> combinedZoneMap;

    public ZoneDissagregator(ResourceBundle rb, ArrayList<Zone> zoneList){
        this.rb = rb;
        this.zoneList = zoneList;
        combinedZoneMap = new HashMap<>();


        for (Zone z : zoneList) {
            if (combinedZoneMap.containsKey(z.getCombinedZoneId())){ ;
            combinedZoneMap.get(z.getCombinedZoneId()).put(z.getId(), z);
        } else {
                Map<Integer, Zone> internalZoneMap = new HashMap<>();
                internalZoneMap.put(z.getId(), z);
                combinedZoneMap.put(z.getCombinedZoneId(), internalZoneMap);
            }

        }

    }

    public void dissagregateDestination(LongDistanceTrip trip){

        Map<Integer, Zone> internalZoneMap = combinedZoneMap.get(trip.getDestZoneId());

        int[] alternatives = new int [internalZoneMap.size()];
        double[] expUtilities = new double[internalZoneMap.size()];
        int i = 0;

        for (Zone z : internalZoneMap.values()){
            alternatives[i] = z.getId();
            expUtilities[i] = (z.getPopulation());
            i++;
        }

        trip.setDestZone(internalZoneMap.get(new EnumeratedIntegerDistribution(alternatives, expUtilities).sample()));

    }


}

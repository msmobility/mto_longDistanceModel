package de.tum.bgu.msm.longDistance;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/**
 * Created by Carlos Llorca  on 7/5/2016.
 * Technical University of Munich
 *
 * Class to hold a long distance trip
 *
 */
public class LongDistanceTrip {

    private static final Map<Integer, LongDistanceTrip> tripMap = new HashMap<>();
    private int tripId;
    private int personId;
    private boolean international;
    private int tripPurpose;
    private int tripState;
    private int originCma;
    private int nights;
    public  int travelParty;
    //ArrayList<Long> destinations;

    public LongDistanceTrip(int tripId, int personId, boolean international, int tripPurpose, int tripState, int originCma, int nights, int travelParty /**,ArrayList<Long> destinations**/) {
        this.tripId = tripId;
        this.personId = personId;
        this.international = international;
        this.tripPurpose = tripPurpose;
        this.tripState = tripState;
        this.originCma = originCma;
        this.nights = nights;
        this.travelParty = travelParty;
        //this.destinations = new ArrayList<>();
        tripMap.put(tripId,this);
    }

    public static LongDistanceTrip[] getLongDistanceTripArray() {
        return tripMap.values().toArray(new LongDistanceTrip[tripMap.size()]);
    }

    public int getLongDistanceTripId() {
        return tripId;
    }

    public int getPersonId() {
        return personId;
    }

    public boolean isLongDistanceInternational() {
        return international;
    }

    public int getLongDistanceTripState() {
        return tripState;
    }

    public int getLongDistanceTripPurpose() {
        return tripPurpose;
    }

    public int getLongDistanceOriginCma() {
        return originCma;
    }

    public int getLongDistanceNights() {
        return nights;
    }
}

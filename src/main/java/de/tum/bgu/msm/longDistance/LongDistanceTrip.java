package de.tum.bgu.msm.longDistance;



import de.tum.bgu.msm.syntheticPopulation.Person;

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

    private int tripId;
    private int personId;
    private boolean international;
    private int tripPurpose;
    private int tripState;
    private int originZone;
    private int nights;
    private int hhAdultsTravelPartySize;
    private int hhKidsTravelPartySize;
    private ArrayList<Person> hhTravelParty;
    private int nonHhTravelPartySize;



    //ArrayList<Long> destinations;

    public LongDistanceTrip(int tripId, int personId, boolean international, int tripPurpose, int tripState, int originZone, int nights,
                            int hhAdultsTravelPartySize, int hhKidsTravelPartySize, ArrayList hhTravelParty, int nonHhTravelPartySize /**,ArrayList<Long> destinations**/) {
        this.tripId = tripId;
        this.personId = personId;
        this.international = international;
        this.tripPurpose = tripPurpose;
        this.tripState = tripState;
        this.originZone = originZone;
        this.nights = nights;
        this.hhAdultsTravelPartySize = hhAdultsTravelPartySize;
        this.hhKidsTravelPartySize = hhKidsTravelPartySize;
        this.hhTravelParty = hhTravelParty;
        this.nonHhTravelPartySize = nonHhTravelPartySize;
        //this.destinations = new ArrayList<>();
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

    public int getLongDistanceOriginZone() {
        return originZone;
    }

    public int getLongDistanceNights() {
        return nights;
    }

    public int getAdultsHhTravelPartySize() { return hhAdultsTravelPartySize; }

    public int getKidsHhTravelPartySize() { return hhKidsTravelPartySize; }

    public int getNonHhTravelPartySize() {
        return nonHhTravelPartySize;
    }
}

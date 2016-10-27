package de.tum.bgu.msm.longDistance;



import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.syntheticPopulation.Person;

import java.util.HashMap;
import java.util.List;
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
    private int nights;
    private int hhAdultsTravelPartySize;
    private int hhKidsTravelPartySize;
    private ArrayList<Person> hhTravelParty;
    private int nonHhTravelPartySize;
    private Zone origZone;
    private static int tripCounter = 0;
    private Zone destination;


    //ArrayList<Long> destinations;

    public LongDistanceTrip(int personId, boolean international, int tripPurpose, int tripState, Zone origZone, int nights,
                            int hhAdultsTravelPartySize, int hhKidsTravelPartySize/**,ArrayList hhTravelParty**/, int nonHhTravelPartySize /**,ArrayList<Long> destinations**/) {
        this.tripId = tripCounter;
        this.personId = personId;
        this.international = international;
        this.tripPurpose = tripPurpose;
        this.tripState = tripState;
        this.origZone = origZone;
        this.nights = nights;
        this.hhAdultsTravelPartySize = hhAdultsTravelPartySize;
        this.hhKidsTravelPartySize = hhKidsTravelPartySize;
        //this.hhTravelParty = hhTravelParty;
        this.nonHhTravelPartySize = nonHhTravelPartySize;
        //this.destinations = new ArrayList<>();
        tripCounter++;
    }

    public LongDistanceTrip(TableDataSet tripsDomesticTable, int row, Map<Integer, Zone> zoneLookup) {

        List<String> tripPurposes = mtoLongDistance.getTripPurposes();
        List<String> tripStates = mtoLongDistance.getTripStates();

        this.tripId = (int) tripsDomesticTable.getValueAt(row, "tripId");
        this.personId = (int) tripsDomesticTable.getValueAt(row, "personId");
        this.international = tripsDomesticTable.getBooleanValueAt(row, "international");
        this.tripPurpose = tripPurposes.indexOf(tripsDomesticTable.getStringValueAt(row, "tripPurpose"));
        this.tripState = tripStates.indexOf(tripsDomesticTable.getStringValueAt(row, "tripState"));
        int origZoneId = (int) tripsDomesticTable.getValueAt(row, "tripOriginZone");
        origZone = zoneLookup.get(origZoneId);
        this.nights = (int) tripsDomesticTable.getValueAt(row, "numberOfNights");
        this.hhAdultsTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "hhAdultsTravelParty");
        this.hhKidsTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "hhKidsTravelParty");
        this.nonHhTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "nonHhTravelParty");
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

    public Zone getLongDistanceOrigZone() {
        return origZone;
    }

    public int getLongDistanceNights() {
        return nights;
    }

    public int getAdultsHhTravelPartySize() { return hhAdultsTravelPartySize; }

    public int getKidsHhTravelPartySize() { return hhKidsTravelPartySize; }

    public int getNonHhTravelPartySize() {
        return nonHhTravelPartySize;
    }

    public Zone getOrigZone() { return origZone; }


    public void setDestination(Zone destination) {
        this.destination = destination;
    }
}

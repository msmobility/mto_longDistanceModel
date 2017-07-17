package de.tum.bgu.msm.longDistance;



import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;

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

    private static int tripCounter = 0;
    static final List<String> tripStates = MtoLongDistData.getTripStates();
    static final List<String> tripPurposes = MtoLongDistData.getTripPurposes();
    private boolean is_summer = true; //TODO: make this take value of configuration

    private int tripId;
    private Person traveller;
    private boolean international;
    private int tripPurpose;
    private int tripState;
    private int nights;
    private int hhAdultsTravelPartySize;
    private int hhKidsTravelPartySize;
    private ArrayList<Person> hhTravelParty;
    private int nonHhTravelPartySize;
    private Zone origZone;
    private int destinationCombinedZoneId = -1;
    private Zone destZone;
    private int travelMode=-1;
    private ZoneType destZoneType;
    private float travelDistanceLevel2 = -1;
    private float travelDistanceLevel1 = -1;





    //ArrayList<Long> destinations;

    public LongDistanceTrip(Person traveller, boolean international, int tripPurpose, int tripState, Zone origZone, boolean summer, int nights,
                            int hhAdultsTravelPartySize, int hhKidsTravelPartySize/**,ArrayList hhTravelParty**/, int nonHhTravelPartySize /**,ArrayList<Long> destinations**/) {
        this.tripId = tripCounter;
        this.traveller = traveller;
        this.international = international;
        this.tripPurpose = tripPurpose;
        this.tripState = tripState;
        this.origZone = origZone;
        this.is_summer = summer;
        this.nights = nights;
        this.hhAdultsTravelPartySize = hhAdultsTravelPartySize;
        this.hhKidsTravelPartySize = hhKidsTravelPartySize;
        //this.hhTravelParty = hhTravelParty;
        this.nonHhTravelPartySize = nonHhTravelPartySize;
        //this.destinations = new ArrayList<>();
        tripCounter++;
    }

    public LongDistanceTrip(TableDataSet tripsDomesticTable, int row, Map<Integer, Zone> zoneLookup, SyntheticPopulation syntheticPopulation, boolean assignDestination) {

        this.tripId = (int) tripsDomesticTable.getValueAt(row, "tripId");
        int personId = (int) tripsDomesticTable.getValueAt(row, "personId");
        this.traveller = syntheticPopulation.getPersonFromId(personId);

        this.international = tripsDomesticTable.getBooleanValueAt(row, "international");
        this.tripPurpose = tripPurposes.indexOf(tripsDomesticTable.getStringValueAt(row, "tripPurpose"));
        this.tripState = tripStates.indexOf(tripsDomesticTable.getStringValueAt(row, "tripState"));
        int origZoneId = (int) tripsDomesticTable.getValueAt(row, "tripOriginZone");
        origZone = zoneLookup.get(origZoneId);

        if (assignDestination) this.destinationCombinedZoneId = (int) tripsDomesticTable.getValueAt(row, "tripDestCombinedZone");
        if (assignDestination) this.destZoneType = ZoneType.getZoneType(tripsDomesticTable.getStringValueAt(row, "destZoneType"));

        //included for mode choice
        this.nights = (int) tripsDomesticTable.getValueAt(row, "numberOfNights");
        this.hhAdultsTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "hhAdultsTravelParty");
        this.hhKidsTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "hhKidsTravelParty");
        this.nonHhTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "nonHhTravelParty");
    }

    public int getLongDistanceTripId() {
        return tripId;
    }

    public int getPersonId() {
        if (traveller == null) return 99999999;
        else return traveller.getPersonId();
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

    //todo remove duplicate
    public Zone getOrigZone() { return origZone; }

    public int getDestZoneId() { return destinationCombinedZoneId; }


    public void setDestination(int destinationZoneId) {
        this.destinationCombinedZoneId = destinationZoneId;
    }

    public Person getTraveller() {
        return traveller;
    }

    public void setMode(int travelMode) {
        this.travelMode = travelMode;
    }

    public ZoneType getDestZoneType() {
        return destZoneType;
    }

    public void setDestZoneType(ZoneType destZoneType) {
        this.destZoneType = destZoneType;
    }

    public Zone getDestZone() {
        return destZone;
    }

    public void setDestZone(Zone destZone) {
        this.destZone = destZone;
    }

    public float getTravelDistanceLevel2() {
        return travelDistanceLevel2;
    }

    public void setTravelDistanceLevel2(float travelDistanceLevel2) {
        this.travelDistanceLevel2 = travelDistanceLevel2;
    }

    public float getTravelDistanceLevel1() {
        return travelDistanceLevel1;
    }

    public void setTravelDistanceLevel1(float travelDistanceLevel1) {
        this.travelDistanceLevel1 = travelDistanceLevel1;
    }

    public static String getHeader() {
        return "tripId,personId,international,tripPurpose,tripState,tripOriginZone,tripOriginCombinedZone,tripOriginType," +
                "tripDestCombinedZone"  +  ",tripMode,"
                +"numberOfNights,hhAdultsTravelParty,hhKidsTravelParty,nonHhTravelParty,destZoneType,destZone,travelDistanceLvl2,travelDistanceLvl1"
//                + ",personAge,personGender," +
        //        "personEducation,personWorkStatus,personIncome,adultsInHh,kidsInHh"
                ;
    }
    @Override
    public String toString() {
        LongDistanceTrip tr = this;
        String str = null;
        if (tr.getLongDistanceOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
            Person traveller = tr.getTraveller();

            str = (tr.getLongDistanceTripId()
                    + "," + tr.getPersonId()
                    + "," + tr.isLongDistanceInternational()
                    + "," + tripPurposes.get(tr.getLongDistanceTripPurpose())
                    + "," + tripStates.get(tr.getLongDistanceTripState())
                    + "," + tr.getLongDistanceOrigZone().getId()
                    + "," + tr.getLongDistanceOrigZone().getCombinedZoneId()
                    + "," + tr.getLongDistanceOrigZone().getZoneType()
                    + "," + tr.getDestZoneId()
                    + "," + tr.getMode()
                    + "," + tr.getLongDistanceNights()
                    + "," + tr.getAdultsHhTravelPartySize()
                    + "," + tr.getKidsHhTravelPartySize()
                    + "," + tr.getNonHhTravelPartySize()
                    + "," + tr.getDestZoneType()
                    + "," + tr.getDestZone().getId()
                    + "," + tr.getTravelDistanceLevel2()
                    + "," + tr.getTravelDistanceLevel1()
                    /*+ "," + traveller.getAge()
                    + "," + Character.toString(traveller.getGender())
                    + "," + traveller.getEducation()
                    + "," + traveller.getWorkStatus()
                    + "," + traveller.getIncome()
                    + "," + traveller.getAdultsHh()
                    + "," + traveller.getKidsHh()*/
            );
        } else {
            str =  (tr.getLongDistanceTripId()
                    + "," + tr.getPersonId()
                    + "," + tr.isLongDistanceInternational()
                    + "," + tripPurposes.get(tr.getLongDistanceTripPurpose())
                    + "," + tripStates.get(tr.getLongDistanceTripState())
                    + "," + tr.getLongDistanceOrigZone().getId()
                    + "," + tr.getLongDistanceOrigZone().getCombinedZoneId()
                    + "," + tr.getLongDistanceOrigZone().getZoneType()
                    + "," + tr.getDestZoneId()
                    + "," + tr.getMode()
                    + "," + tr.getLongDistanceNights()
                    + "," + tr.getAdultsHhTravelPartySize()
                    + "," + tr.getKidsHhTravelPartySize()
                    + "," + tr.getNonHhTravelPartySize()
                    + "," + tr.getDestZoneType()
                    + "," + tr.getDestZone().getId()
                    + "," + tr.getTravelDistanceLevel2()
                    + "," + tr.getTravelDistanceLevel1()
                    //+ ",-1,,-1,-1,-1,-1,-1"
            );

        }
        return str;
    }

    public int getMode() {
        return travelMode;
    }

    public boolean isSummer() {
        return is_summer;
    }

    public String getTripPurpose() {
        return tripPurposes.get(getLongDistanceTripPurpose());
    }
}

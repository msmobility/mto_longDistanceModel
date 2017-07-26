package de.tum.bgu.msm.longDistance;



import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;

import java.util.List;
import java.util.Map;

/**
 * Created by Carlos Llorca  on 7/5/2016.
 * Technical University of Munich
 *
 * Class to hold a long distance trip
 *
 */
public class LongDistanceTrip {

    private static int tripCounter = 0;
    private static final List<String> tripStates = MtoLongDistData.getTripStates();
    private static final List<String> tripPurposes = MtoLongDistData.getTripPurposes();

    private int tripId;
    private Person traveller;
    private boolean international;
    private int tripPurpose;
    private int tripState;
    private boolean is_summer = true;
    private int nights;
    private int hhAdultsTravelPartySize;
    private int hhKidsTravelPartySize;
    //private ArrayList<Person> hhTravelParty;
    private int nonHhTravelPartySize;
    private Zone origZone;
    private int destCombinedZoneId = -1;
    private ZoneType destZoneType;
    private Zone destZone;
    private int travelMode=-1;
    private float travelDistanceLevel2 = -1;
    private float travelDistanceLevel1 = -1;


    public LongDistanceTrip(Person traveller, boolean international, int tripPurpose, int tripState, Zone origZone, boolean summer, int nights,
                            int hhAdultsTravelPartySize, int hhKidsTravelPartySize/**,ArrayList hhTravelParty**/, int nonHhTravelPartySize ) {
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
        tripCounter++;
    }

    //contructor in developing phase - read trips from list instead of trip generation
    public LongDistanceTrip(TableDataSet tripsDomesticTable, int row, Map<Integer, Zone> zoneLookup, SyntheticPopulation syntheticPopulation, boolean assignDestination) {

        this.tripId = (int) tripsDomesticTable.getValueAt(row, "tripId");
        int personId = (int) tripsDomesticTable.getValueAt(row, "personId");
        this.traveller = syntheticPopulation.getPersonFromId(personId);

        this.international = tripsDomesticTable.getBooleanValueAt(row, "international");
        this.tripPurpose = tripPurposes.indexOf(tripsDomesticTable.getStringValueAt(row, "tripPurpose"));
        this.tripState = tripStates.indexOf(tripsDomesticTable.getStringValueAt(row, "tripState"));
        int origZoneId = (int) tripsDomesticTable.getValueAt(row, "tripOriginZone");
        origZone = zoneLookup.get(origZoneId);

        this.nights = (int) tripsDomesticTable.getValueAt(row, "numberOfNights");
        this.hhAdultsTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "hhAdultsTravelParty");
        this.hhKidsTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "hhKidsTravelParty");
        this.nonHhTravelPartySize = (int) tripsDomesticTable.getValueAt(row, "nonHhTravelParty");

        if (assignDestination) this.destCombinedZoneId = (int) tripsDomesticTable.getValueAt(row, "tripDestCombinedZone");
        if (assignDestination) this.destZoneType = ZoneType.getZoneType(tripsDomesticTable.getStringValueAt(row, "destZoneType"));
    }

    public int getTripId() {
        return tripId;
    }

    public int getTravellerId() {
        if (traveller == null) return 99999999;
        else return traveller.getPersonId();
    }

    public Person getTraveller() {
        return traveller;
    }

    public boolean isInternational() {
        return international;
    }

    public int getTripState() {
        return tripState;
    }

    public int getTripPurpose() {
        return tripPurpose;
    }

    public int getNights() {
        return nights;
    }

    public int getAdultsHhTravelPartySize() { return hhAdultsTravelPartySize; }

    public int getKidsHhTravelPartySize() { return hhKidsTravelPartySize; }

    public int getNonHhTravelPartySize() {
        return nonHhTravelPartySize;
    }

    public Zone getOrigZone() { return origZone; }

    public int getDestCombinedZoneId() { return destCombinedZoneId; }

    public void setCombinedDestZoneId(int destinationZoneId) {
        this.destCombinedZoneId = destinationZoneId;
    }

    public void setMode(int travelMode) {
        this.travelMode = travelMode;
    }

    public int getMode() {
        return travelMode;
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
        if (tr.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
            Person traveller = tr.getTraveller();

            str = (tr.getTripId()
                    + "," + tr.getTravellerId()
                    + "," + tr.isInternational()
                    + "," + tripPurposes.get(tr.getTripPurpose())
                    + "," + tripStates.get(tr.getTripState())
                    + "," + tr.getOrigZone().getId()
                    + "," + tr.getOrigZone().getCombinedZoneId()
                    + "," + tr.getOrigZone().getZoneType()
                    + "," + tr.getDestCombinedZoneId()
                    + "," + tr.getMode()
                    + "," + tr.getNights()
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
            str =  (tr.getTripId()
                    + "," + tr.getTravellerId()
                    + "," + tr.isInternational()
                    + "," + tripPurposes.get(tr.getTripPurpose())
                    + "," + tripStates.get(tr.getTripState())
                    + "," + tr.getOrigZone().getId()
                    + "," + tr.getOrigZone().getCombinedZoneId()
                    + "," + tr.getOrigZone().getZoneType()
                    + "," + tr.getDestCombinedZoneId()
                    + "," + tr.getMode()
                    + "," + tr.getNights()
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



    public boolean isSummer() {
        return is_summer;
    }

    public String getTripPurposeAsString() {
        return tripPurposes.get(getTripPurpose());
    }
}

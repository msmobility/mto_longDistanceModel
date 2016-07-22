package de.tum.bgu.msm.dataAnalysis;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by carlloga on 7/13/2016.
 * class to hold an international visitor travel
 *
 */


public class surveyIntVisitorTravel {

    static Logger logger = Logger.getLogger(surveyIntVisitorTravel.class);

    private static final Map<Integer,surveyIntVisitorTravel> intVisitorTravelMap = new HashMap<>();

    int pumfId;
    int refQuarter;
    int refYear;
    int purpose;
    String entryPort;
    int entryMode;
    int origCountry;
    //zero position is equal to position 1 and means "first country visited". This is to be coherent with nightsByPlace
    int[] provinces = new int[16];

    int[] nightsByPlace = new int[16];
    float weight;
    int travelParty;
    int entryRoute;
    int sgrCode;


    //constructor


    public surveyIntVisitorTravel(int pumfId, int refQuarter, int refYear, int purpose, String entryPort, int entryMode,
                                  int origCountry, int[] provinces, int[] nightsByPlace, float weight, int travelParty, int entryRoute, int sgrCode) {
        this.pumfId = pumfId;
        this.refQuarter = refQuarter;
        this.refYear = refYear;
        this.purpose = purpose;
        this.entryPort = entryPort;
        this.entryMode = entryMode;
        this.origCountry = origCountry;
        this.provinces = provinces;
        this.nightsByPlace = nightsByPlace;
        this.weight = weight;
        this.travelParty = travelParty;
        this.entryRoute = entryRoute;
        this.sgrCode = sgrCode;
        intVisitorTravelMap.put(pumfId,this);
    }



    public static surveyIntVisitorTravel[] getIntVisitorTravelArray() {
        return intVisitorTravelMap.values().toArray(new surveyIntVisitorTravel[intVisitorTravelMap.size()]);
    }

    public int getPumfId() {
        return pumfId;
    }

    public int getRefQuarter() {
        return refQuarter;
    }

    public int getRefYear() {
        return refYear;
    }

    public int getPurpose() {
        return purpose;
    }

    public String getEntryPort() {
        return entryPort;
    }

    public int getEntryMode() {
        return entryMode;
    }

    public int getOrigCountry() {
        return origCountry;
    }

    public int[] getProvinces() {
        return provinces;
    }

    public int[] getNightsByPlace() {
        return nightsByPlace;
    }

    public float getWeight() {
        return weight;
    }

    public int getTravelParty() {
        return travelParty;
    }

    public int getEntryRoute() {
        return entryRoute;
    }

    public int getSgrCode() {
        return sgrCode;
    }
}



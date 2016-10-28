package de.tum.bgu.msm.dataAnalysis;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold trip object of the ITS survey
 *
 * @author Rolf Moeckel
 * Created on 25 May 2016 in Munich, Germany
 *
 **/


//Edited by Carlos Llorca  on 29 June 2016

public class SurveyIntTravel implements Serializable {

    static Logger logger = Logger.getLogger(SurveyIntTravel.class);

    private static final Map<Integer,SurveyIntTravel> intTripMap = new HashMap<>();
    int origProvince;
    int pumfId;
    int refQuarter;
    int refYear;
    int purpose;
    int entryMode;
    //zero position is equal to position 1 and means "first country visited". This is to be coherent with nightsByPlace
    int[] country = new int[16];
    int[] nightsByPlace = new int[16];
    float weight;
    int travelParty;



    //constructor

    public SurveyIntTravel(int origProvince, int pumfId, int refYear, int refQuarter, int purpose, int entryMode, int country[], int[] nightsByPlace, float weight, int travelParty){
        this.origProvince = origProvince;
        this.pumfId = pumfId;
        this.refQuarter = refQuarter;
        this.refYear = refYear;
        this.purpose = purpose;
        this.entryMode = entryMode;
        this.country = country;
        this.nightsByPlace = nightsByPlace;
        this.weight = weight;
        this.travelParty = travelParty;
        intTripMap.put(pumfId,this);

    }


    public static SurveyIntTravel[] getIntTravelArray() {
        return intTripMap.values().toArray(new SurveyIntTravel[intTripMap.size()]);
    }

    public int getOrigProvince() {
        return origProvince;
    }

    public int getPumfId() {
        return pumfId;
    }

    public int getRefYear() {
        return refYear;
    }

    public int getPurpose() {
        return purpose;
    }

    public int getRefQuarter() {
        return refQuarter;
    }

    public int getEntryMode() {
        return entryMode;
    }

    public int[] getCountry() {
        return country;
    }

    public int[] getNights() {
        return nightsByPlace;
    }

    public float getWeight() {
        return weight;
    }

    public float getTravelParty() {
        return travelParty;
    }


//commented by Carlos Llorca on 29 June 2016
/*
    public surveyIntTravel(int pumfId, int refYear) {
        // constructor of new survey person

        this.refYear = refYear;
        intTripMap.put(pumfId,this);
    }*/
}

package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold tour object of the TSRC survey
 *
 * @author Rolf Moeckel
 * Created on 17 Mar. 2016 in Munich
 *
 **/

public class surveyTour implements Serializable {

    static Logger logger = Logger.getLogger(surveyTour.class);

    private static final Map<Long, surveyTour> tourMap = new HashMap<>();
    long tourId;
    int refYear;
    int origProvince;
    int destProvince;
    int travelParty;
    int travelPartyAdult;
    int travelPartyKids;
    private int mainMode;
    int homeCma;
    int tripPurp;
    int numberNights;
    int numIdentical;
    float hhWeight;
    float tripWeight;
    ArrayList<int[]> tourStops;
    int pumfId;

    public surveyTour(int tourId, int pumfId, int refYear, int origProvince, int destProvince, int travelParty, int travelPartyAdult, int travelPartyKids, int mainMode, int homeCma,
                      int tripPurp, int numberNights, int numIdentical, float hhWeight, float tripWeight) {
        // constructor of new survey tour

        this.tourId = util.createTourId(pumfId, tourId);
        this.pumfId = pumfId;
        this.refYear = refYear;
        this.origProvince = origProvince;
        this.destProvince = destProvince;
        this.travelParty = travelParty;
        this.travelPartyAdult = travelPartyAdult;
        this.travelPartyKids = travelPartyKids;
        this.mainMode = mainMode;
        this.homeCma = homeCma;
        this.tripPurp = tripPurp;
        this.numberNights = numberNights;
        this.numIdentical = numIdentical;
        this.hhWeight = hhWeight;
        this.tripWeight = tripWeight;
        tourStops = new ArrayList<>();
        tourMap.put(this.tourId,this);
    }


    public void addTripDestinations(int cmarea, int nights) {
        int[] stopData = {cmarea, nights};
        tourStops.add(stopData);

    }

    public static surveyTour[] getSurveyTourArray() {
        return tourMap.values().toArray(new surveyTour[tourMap.size()]);
    }

    public static surveyTour getTourFromId(long id) {
        return tourMap.get(id);
    }

    public long getTourId() {return tourId;}

    public long getRefYear() {return refYear;}

    public int getOrigProvince() {
        return origProvince;
    }

    public int getDestProvince() {
        return destProvince;
    }

    public int getTravelParty() {
        return travelParty;
    }

    public int getTravelPartyAdult() {
        return travelPartyAdult;
    }

    public int getTravelPartyKids() {
        return travelPartyKids;
    }

    public int getMainMode() {
        return mainMode;
    }

    public int getHomeCma() {
        return homeCma;
    }

    public int getTripPurp() {
        return tripPurp;
    }

    public int getNumberNights() {
        return numberNights;
    }

    public int getNumIdentical() {
        return numIdentical;
    }

    public int getNumberOfStop () {
        return tourStops.size();
    }

    public float getHhWeight () {
        return hhWeight;
    }

    public float getTripWeight () {
        return tripWeight;
    }

    public int getPersonId(){return pumfId; }


}

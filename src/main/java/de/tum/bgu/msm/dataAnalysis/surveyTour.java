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

    private int origProvince;
    private int destProvince;
    private int mainMode;
    private int homeCma;
    private int tripPurp;
    private int numberNights;
    private int numIdentical;
    private ArrayList<int[]> tourStops;
    private int tripId;

    protected surveyTour(int tripId, int pumfId, int origProvince, int destProvince, int mainMode, int homeCma,
               int tripPurp, int numberNights, int numIdentical) {
        // constructor of new survey tour


        this.tripId = tripId;
        this.origProvince = origProvince;
        this.destProvince = destProvince;
        this.mainMode = mainMode;
        this.homeCma = homeCma;
        this.tripPurp = tripPurp;
        this.numberNights = numberNights;
        this.numIdentical = numIdentical;
        tourStops = new ArrayList<>();
    }


    public void addTripDestinations(int cmarea, int nights) {
        int[] stopData = {cmarea, nights};
        tourStops.add(stopData);

    }

    public int getOrigProvince() {
        return origProvince;
    }

    public int getDestProvince() {
        return destProvince;
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

    public int getTripId() {
        return tripId;
    }
}

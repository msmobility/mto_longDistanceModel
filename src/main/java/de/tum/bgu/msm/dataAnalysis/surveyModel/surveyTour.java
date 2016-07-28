package de.tum.bgu.msm.dataAnalysis.surveyModel;

import org.apache.log4j.Logger;
import org.omg.CORBA.NameValuePairHelper;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    private ArrayList<SurveyVisit> tourStops;
    private int tripId;
    private surveyPerson person;
    private int origCD;

    protected surveyTour(int tripId, surveyPerson person, int origProvince, int origCD, int destProvince, int mainMode, int homeCma,
               int tripPurp, int numberNights, int numIdentical) {
        // constructor of new survey tour

        this.person = person;
        this.tripId = tripId;
        this.origProvince = origProvince;
        this.origCD = origCD;
        this.destProvince = destProvince;
        this.mainMode = mainMode;
        this.homeCma = homeCma;
        this.tripPurp = tripPurp;
        this.numberNights = numberNights;
        this.numIdentical = numIdentical;
        tourStops = new ArrayList<>();
    }


    public void addTripDestinations(SurveyVisit sv) {
        tourStops.add(sv);

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

    public ArrayList<SurveyVisit> getStops() {
        return tourStops;
    }

    public long getDistinctNumRegions() {
        return  getStops().stream().filter(v -> v.cma != homeCma).map(v -> v.cma).distinct().count();
    }

    public boolean isReturnTrip() {
        return homeCma == tourStops.get(tourStops.size()-1).cma;
    }

    public SurveyVisit[] getTourStops() { //TODO: include homeCma
        SurveyVisit[] stops = new SurveyVisit[tourStops.size() + 1];
        stops[0] = new SurveyVisit(-1, getOrigProvince(), getOrigCD(), getHomeCma(), 0);
        for (int i = 0; i < getStops().size(); i++) {
            stops[i + 1] = getStops().get(i);
        }
        return stops;
    }

    public void sortVisits() {
        tourStops.sort((o1, o2) -> Integer.compare(o1.visitId,o2.visitId));
    }

    public String getUniqueId() {
        return Long.toString(getPerson().getPumfId()) + Integer.toString(getTripId());
    }

    public surveyPerson getPerson() {
        return person;
    }

    public int getOrigCD() {
        return origCD;
    }

}

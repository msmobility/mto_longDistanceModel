package de.tum.bgu.msm.dataAnalysis.surveyModel;

import org.apache.log4j.Logger;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

/**
 * Class to hold person object of travelers and non-travelers of the TSRC survey
 *
 * @author Rolf Moeckel
 * Created on 26 Feb. 2016 in Vienna, VA
 *
**/

public class surveyPerson implements Serializable {

    static Logger logger = Logger.getLogger(surveyPerson.class);

    int refYear;
    int refMonth;
    long pumfId;
    float weight;
    float weight2;
    int prov;
    int cma;
    int ageGroup;
    int gender;
    int education;
    int laborStat;
    int hhIncome;
    int adultsInHh;
    int kidsInHh;
    HashMap<Integer, surveyTour> tours;


    public surveyPerson(int refYear, int refMonth, int pumfId, float weight, float weight2, int prov, int cma, int ageGroup,
                        int gender, int education, int laborStat, int hhIncome, int adultsInHh, int kidsInHh) {
        // constructor of new survey person

        this.refYear = refYear;
        this.refMonth = refMonth;
        this.pumfId = pumfId;
        this.weight = weight;
        this.weight2 = weight2;
        this.prov = prov;
        this.cma = cma;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.education = education;
        this.laborStat = laborStat;
        this.hhIncome = hhIncome;
        this.adultsInHh = adultsInHh;
        this.kidsInHh = kidsInHh;
        this.tours = new HashMap<>();
    }

    public int getRefYear() {
        return refYear;
    }

    public void addTour(surveyTour tour) {
        tours.put(tour.getTripId(), tour);
    }

    public int getNumberOfTrips() {
        return tours.size();
    }

    public int getHhIncome() {
        return hhIncome;
    }

    public int getAgeGroup() {
        return ageGroup;
    }

    public int getGender() {
        return gender;
    }

    public long getPumfId() {
        return pumfId;
    }

    public int getEducation() {
        return education;
    }

    public int getLaborStat() {
        return laborStat;
    }

    public int getAdultsInHh() {
        return adultsInHh;
    }

    public int getKidsInHh() {
        return kidsInHh;
    }

    public int getProv() {
        return prov;
    }

    public int getCma() {
        return cma;
    }

    public Collection<surveyTour> getTours() {
        return tours.values();
    }

    public float getWeight() {
        return weight;
    }

    public int getRefMonth() {
        return refMonth;
    }

    public surveyTour getTourFromId(int tripId) {
        return tours.get(tripId);
    }
}

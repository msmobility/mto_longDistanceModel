package de.tum.bgu.msm.dataAnalysis.surveyModel;

import de.tum.bgu.msm.dataAnalysis.dataDictionary.Survey;
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
    int cd;
    int cma;
    int ageGroup;
    int gender;
    int education;
    int laborStat;
    int hhIncome;
    int adultsInHh;
    int kidsInHh;
    private int tripfile_count;
    private int overnight_count;
    private int sameday_count;
    private int total_count;
    HashMap<Integer, surveyTour> tours;

    //addded by Carlos Llorca on 6/7/16 to evaluate the tripGeneration of the surveyPerson individuals instead of Synthetic pop
    //this should not be used if this survey population is not used to test the models
    public boolean  isAway = false;
    public boolean  isDaytrip = false ;
    public boolean  isInOutTrip = false;

    public surveyPerson(int refYear, int refMonth, int pumfId, float weight, float weight2, int prov, int cd, int cma, int ageGroup,
                        int gender, int education, int laborStat, int hhIncome, int adultsInHh, int kidsInHh) {
        // constructor of new survey person

        this.refYear = refYear;
        this.refMonth = refMonth;
        this.pumfId = pumfId;
        this.weight = weight;
        this.weight2 = weight2;
        this.prov = prov;
        this.cd = cd;
        this.cma = cma;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.education = education;
        this.laborStat = laborStat;
        this.hhIncome = hhIncome;
        this.adultsInHh = adultsInHh;
        this.kidsInHh = kidsInHh;
    }

    public surveyPerson(Survey survey, String recString) {
        this.refYear = survey.readInt(recString, "REFYEARP");  // ascii position in file: 01-04
        this.refMonth = survey.readInt(recString, "REFMTHP");  // ascii position in file: 05-06

        int origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 07-13
        this.pumfId = origPumfId * 100 + refYear%100;

        this.weight = survey.readFloat(recString, "WTPM");  // ascii position in file: 14-25
        this.weight2 = survey.readFloat(recString, "WTPM2");  // ascii position in file: 26-37
        this.prov = survey.readInt(recString, "RESPROV");  // ascii position in file: 38-39
        this.cd = survey.readInt(recString, "RESCD2");  // ascii position in file: 43-46
        this.cma = survey.readInt(recString, "RESCMA2");  // ascii position in file: 43-46

        this.gender = survey.readInt(recString, "SEX");
        this.laborStat = survey.readInt(recString, "LFSSTATG");

        this.ageGroup = survey.readInt(recString, "AGE_GR2");
        this.education = survey.readInt(recString, "EDLEVGR");

        this.hhIncome = survey.readInt(recString, "INCOMGR2");  // ascii position in file: 51-51
        this.adultsInHh = survey.readInt(recString, "G_ADULTS");  // ascii position in file: 52-53
        this.kidsInHh = survey.readInt(recString, "G_KIDS");  // ascii position in file: 54-55

        this.tripfile_count = survey.readInt(recString, "TRIP_CNT");
        this.overnight_count = survey.readInt(recString, "ON_CNT");
        this.sameday_count = survey.readInt(recString, "ON_CNT");
        this.total_count = survey.readInt(recString, "TRIPCTOT");

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

    public int getCd() {
        return cd;
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

    public int getTripfile_count() {
        return tripfile_count;
    }

    public int getOvernight_count() {
        return overnight_count;
    }

    public int getSameday_count() {
        return sameday_count;
    }

    public int getTotal_count() {
        return total_count;
    }

    public surveyTour getTourFromId(int tripId) {
        return tours.get(tripId);
    }

    public float getWeight2() {
        return weight2;
    }
}

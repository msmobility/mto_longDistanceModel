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

public class SurveyPerson implements Serializable {

    static Logger logger = Logger.getLogger(SurveyPerson.class);

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
    HashMap<Integer, SurveyTour> tours;


    public SurveyPerson(Survey survey, String recString) {
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
        this.laborStat =survey.readInt(recString, "LFSSTATG");

        this.ageGroup =  survey.readInt(recString, "AGE_GR2");
        this.education = survey.readInt(recString, "EDLEVGR");

        this.hhIncome = survey.readInt(recString, "INCOMGR2");  // ascii position in file: 51-51
        this.adultsInHh = survey.readInt(recString, "G_ADULTS");  // ascii position in file: 52-53
        this.kidsInHh = survey.readInt(recString, "G_KIDS");  // ascii position in file: 54-55

        this.tours = new HashMap<>();

    }

    public int getRefYear() {
        return refYear;
    }

    public void addTour(SurveyTour tour) {
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

    public Collection<SurveyTour> getTours() {
        return tours.values();
    }

    public float getWeight() {
        return weight;
    }

    public int getRefMonth() {
        return refMonth;
    }

    public SurveyTour getTourFromId(int tripId) {
        return tours.get(tripId);
    }
}

package de.tum.bgu.msm;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold person object of travelers and non-travelers of the TSRC survey
 *
 * @author Rolf Moeckel
 * Created on 26 Feb. 2016 in Vienna, VA
 *
**/

public class surveyPerson implements Serializable {

    static Logger logger = Logger.getLogger(surveyPerson.class);

    private static final Map<Integer,surveyPerson> personMap = new HashMap<>();
    int refYear;
    int refMonth;
    int pumfId;
    float weight;
    float weight2;
    int prov;
    int ageGroup;
    int gender;
    int education;
    int laborStat;
    int hhIncome;
    int adultsInHh;
    int kidsInHh;
    ArrayList<Integer> tours;


    public surveyPerson(int refYear, int refMonth, int pumfId, float weight, float weight2, int prov, int ageGroup,
                        int gender, int education, int laborStat, int hhIncome, int adultsInHh, int kidsInHh) {
        // constructor of new survey person

        this.refYear = refYear;
        this.refMonth = refMonth;
        this.pumfId = pumfId;
        this.weight = weight;
        this.weight2 = weight2;
        this.prov = prov;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.education = education;
        this.laborStat = laborStat;
        this.hhIncome = hhIncome;
        this.adultsInHh = adultsInHh;
        this.kidsInHh = kidsInHh;
        this.tours = new ArrayList<>();
        personMap.put(pumfId,this);
    }


    public static surveyPerson getPersonFromId(int id) {
        return personMap.get(id);
    }

    public static int getPersonCount() {
        return personMap.size();
    }

    public static Collection<surveyPerson> getPersons() {
        return personMap.values();
    }

    public static surveyPerson[] getPersonArray() {
        return personMap.values().toArray(new surveyPerson[personMap.size()]);
    }


    public void addTour(int tourId) {
        tours.add(util.createTourId(pumfId, tourId));
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

    public int getPumfId() {
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

    public ArrayList<Integer> getTours() {
        return tours;
    }

    public float getWeight() {
        return weight;
    }

}

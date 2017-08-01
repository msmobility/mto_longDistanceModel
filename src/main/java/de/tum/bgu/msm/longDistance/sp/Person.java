package de.tum.bgu.msm.longDistance.sp;


import static java.lang.Integer.parseInt;

/**
 *
 * Ontario Provincial Model
 * Class to store synthetic persons
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 22 April 2016
 * Version 1
 *
 */

public class Person {

    private int id;
    private int age;
    private char gender;
    private int education;
    private int workStatus;
    private int occupation;

    private Household hh;

    private boolean  isAway = false;
    private boolean  isDaytrip = false ;
    private boolean  isInOutTrip = false;

    private float[][] travelProbabilities = null;
// rows 1 to 3: away, daytrip, inOutTrip, home
// columns 1 to 3: visit, business, leisure

    //variables added to homogenize with survey data
    private boolean isYoung;
    private boolean isRetired;
    private boolean isFemale;
    private boolean isHighSchool;
    private boolean isPostSecondary;
    private boolean isUniversity;
    private boolean isEmployed;
    private boolean isIncome2 = false;
    private boolean isIncome3 = false;
    private boolean isIncome4 = false;


    public Person(int id, int hhId, int age, char gender, int occupation, int education, int workStatus, Household hh) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.education = education;
        this.workStatus = workStatus;
        this.hh = hh;
        if (hh != null) hh.addPersonForInitialSetup(this);

        this.isYoung  = age < 25 ? true : false;
        this.isRetired = age > 64 ? true : false;
        this.isFemale = gender == 'F' ? true : false;
        this.isHighSchool = education == 2 ? true : false;
        this.isPostSecondary = education > 2 && education < 6 ? true : false;
        this.isUniversity = education > 5 ? true : false;
        this.isEmployed = workStatus < 3? true : false;

        if (hh.getHhInc() >= 100000) {
            //is in income group 4
            isIncome4= true;
        } else if (hh.getHhInc() >= 70000) {
            //is in income gorup 3
            isIncome3 = true;
        } else if (hh.getHhInc() >= 50000) {
            //is in income group 2
            isIncome2 = true;
        }

    }



    public int getPersonId() {return id;}

    public char getGender() {
        return gender;}

    public int getAge() {return age;}

    public int getIncome() {return hh.getHhInc();}

    public int getAdultsHh() {
        int adultsHh = 0;
        for (Person p : hh.getPersonsOfThisHousehold()) {
            if (p.getAge() >= 18) {
                adultsHh++;
            }
        }
        return adultsHh;
    }

    public int getKidsHh() {
        return hh.getHhSize()- getAdultsHh();
    }

    public int getEducation() {return education;}

    public int getWorkStatus() {return workStatus;}

    public Household getHousehold() {return hh;}

    public boolean isAway() {
        return isAway;
    }

    public boolean isDaytrip() {
        return isDaytrip;
    }

    public boolean isInOutTrip() {
        return isInOutTrip;
    }

    public void setAway(boolean away) {
        isAway = away;
    }

    public void setDaytrip(boolean daytrip) {
        isDaytrip = daytrip;
    }

    public void setInOutTrip(boolean inOutTrip) {
        isInOutTrip = inOutTrip;
    }

    public float[][] getTravelProbabilities() {
        return travelProbabilities;
    }

    public void setTravelProbabilities(float[][] travelProbabilities) {
        this.travelProbabilities = travelProbabilities;
    }

    public boolean isYoung() {
        return isYoung;
    }

    public boolean isRetired() {
        return isRetired;
    }

    public boolean isFemale() {
        return isFemale;
    }

    public boolean isHighSchool() {
        return isHighSchool;
    }

    public boolean isPostSecondary() {
        return isPostSecondary;
    }

    public boolean isUniversity() {
        return isUniversity;
    }

    public boolean isIncome2() {
        return isIncome2;
    }

    public boolean isIncome3() {
        return isIncome3;
    }

    public boolean isIncome4() {
        return isIncome4;
    }

    public boolean isEmployed() {
        return isEmployed;
    }
}

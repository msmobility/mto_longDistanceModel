package de.tum.bgu.msm.syntheticPopulation;
 import org.apache.log4j.Logger;


 import java.lang.reflect.Array;
 import java.util.ArrayList;
 import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    static Logger logger = Logger.getLogger(Person.class);


    private int id;
    private int age;
    private char gender;
    private int occupation;
    private int education;
    private int workStatus;
    //private int fullOrPartTime;
    //private int hoursWorked;
    //private int industrySector;
    private Household hh;
    //addded by Carlos Llorca on 5/7/16
    public boolean  isAway = false;
    public boolean  isDaytrip = false ;
    public boolean  isInOutTrip = false;
    //added by Carlos Llorca on 19/7/16
    public float[][] travelProbabilities = null;
// rows 1 to 3: away, daytrip, inOutTrip, home
// columns 1 to 3: visit, business, leisure
    public Person(int id, int hhId, int age, char gender, int occupation, int education, int workStatus, Household hh) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.education = education;
        this.workStatus = workStatus;
        this.hh = hh;
        if (hh != null) hh.addPersonForInitialSetup(this);
    }


    //add gets for inputs for trip generation by Carlos Llorca on 7/4/16
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

}

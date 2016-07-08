package de.tum.bgu.msm.syntheticPopulation;

import org.apache.log4j.Logger;

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

    private static final Map<Integer, Person> personMap = new HashMap<>();
    private int id;
    private int age;
    private String gender;
    private int occupation;
    private int education;
    private int workStatus;
    private int fullOrPartTime;
    private int hoursWorked;
    private int industrySector;
    private Household hh;
    //addded by Carlos Llorca on 5/7/16
    public boolean  isAway = false;
    public boolean  isDaytrip = false ;
    public boolean  isInOutTrip = false;

    public Person(int id, int hhId, int age, String gender, int occupation, int education, int workStatus) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.education = education;
        this.workStatus = workStatus;
        this.hh = Household.getHouseholdFromId(hhId);
        hh.addPersonForInitialSetup(this);
        personMap.put(id,this);
    }


    public static Person[] getSyntheticPersonArray() {
        return personMap.values().toArray(new Person[personMap.size()]);
    }


    public static Person getPersonFromId(int personId) {
        return personMap.get(personId);
    }


    public static int getPersonCount() {
        return personMap.size();
    }


    public static Collection<Person> getPersons() {
        return personMap.values();
    }

    //add gets for inputs for trip generation by Carlos Llorca on 7/4/16
    public int getPersonId() {return id;}

    public String getGender() {
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

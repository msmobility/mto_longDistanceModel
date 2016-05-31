package de.tum.bgu.msm.syntheticPopulation;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
    private int employment;
    private int fullOrPartTime;
    private int hoursWorked;
    private int industrySector;
    private Household hh;

    public Person(int id, int hhId, int age, String gender, int occupation, int education) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.education = education;
        this.hh = Household.getHouseholdFromId(hhId);
        hh.addPersonForInitialSetup(this);
        personMap.put(id,this);
    }


    public static Person[] getPersonArray() {
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

}

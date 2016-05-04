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
    private int ageGroup;
    private int gender;
    private int occupation;
    private int education;
    private int employment;
    private int fullOrPartTime;
    private int hoursWorked;
    private int industrySector;
    private Household hh;

    public Person(int id, int ageGroup, int gender, int occupation, int education, int employment, int fullOrPartTime,
                  int hoursWorked, int industrySector) {
        this.id = id;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.occupation = occupation;
        this.education = education;
        this.employment = employment;
        this.fullOrPartTime = fullOrPartTime;
        this.hoursWorked = hoursWorked;
        this.industrySector = industrySector;
        int hhId = id / 1000;
        Household personsHousehold = Household.getHouseholdFromId(hhId);
        if (personsHousehold == null) {
            logger.error("Inconsistent synthetic population. Found person " + id + " of household " + hhId + ", but" +
            " this household does not exist in the household file. Program terminated.");
            System.exit(9);
        }
        this.hh = personsHousehold;
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

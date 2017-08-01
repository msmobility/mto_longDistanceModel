package de.tum.bgu.msm.longDistance.sp;
 import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
 import org.apache.log4j.Logger;

/**
 *
 * Ontario Provincial Model
 * Class to store synthetic households
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 22 April 2016
 * Version 1
 *
 */

public class Household {

    static Logger logger = Logger.getLogger(Household.class);

    private int id;
    private int hhSize;
    private int hhInc;
    private int ddType;
    private int numWrks;
    private int numKids;
    private int taz;
    private Zone zone;
    private Person[] persons;


    public Household(int id, int hhInc, int ddType, int taz, Zone zone) {
        this.id      = id;
        this.hhSize  = 0;
        this.hhInc   = hhInc;
        this.ddType  = ddType;
//        this.numWrks = numWrks;
//        this.numKids = numKids;
//        persons = new Person[hhSize];
        this.taz = taz;
        this.zone = zone;
        this.persons = new Person[0];
    }


    public void addPersonForInitialSetup (Person per) {
        // This method adds a person to the household (only used for initial setup)

        Person[] personsAddedSoFar = persons;
        persons = new Person[personsAddedSoFar.length + 1];
        System.arraycopy(personsAddedSoFar, 0, persons, 0, persons.length-1);
        persons[persons.length-1] = per;
        hhSize++;
    }

    public int getId() {
        return id;
    }

    public int getHhSize() {
        return hhSize;
    }

    public int getHhInc() {
        return hhInc;
    }

    public int getTaz() {
        return taz;
    }

    public Zone getZone() {return zone;}

    public Person[] getPersonsOfThisHousehold() {
        return persons;
    }
}

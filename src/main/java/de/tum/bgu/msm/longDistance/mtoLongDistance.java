package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import javafx.collections.FXCollections;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 *
 */

public class mtoLongDistance {
    static Logger logger = Logger.getLogger(mtoLongDistance.class);
    private ResourceBundle rb;

    public mtoLongDistance(ResourceBundle rb) {
        this.rb = rb;
    }

    public static ArrayList<String> tripPurposes = new ArrayList<>();
    public static ArrayList<String> tripStates = new ArrayList<>();

    public void runLongDistanceModel () {
        // main method to run long-distance model

        readSP rsp = new readSP(rb);
        ArrayList<Zone> internalZoneList= rsp.readInternalZones();
        rsp.readSyntheticPopulation(internalZoneList);

        //added omx.jar; if not this doesn't work
        mtoLongDistData md = new mtoLongDistData(rb);
        ArrayList<Zone> zoneList = md.readInternalAndExternalZones(internalZoneList);
        md.readSkim();
        md.calculateAccessibility(zoneList);

        logger.info("Accessibility calculated");


        //add the purposes and states to be used in the trip generation
        tripPurposes.add("visit");
        tripPurposes.add("business");
        tripPurposes.add("leisure");

        tripStates.add("away");
        tripStates.add("daytrip");
        tripStates.add("inout");

        tripGeneration tgdomestic = new tripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_domestic = tgdomestic.runTripGeneration();

        logger.info("Domestic Trips generated");

        //this must be done after domestic
        internationalTripGeneration tginternational = new internationalTripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_international = tginternational.runInternationalTripGeneration();

        logger.info("International trips generated");

        //next method is used to analyze the outputs of the tripGeneration
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();
        trips.addAll(trips_international);
        trips.addAll(trips_domestic);

        mtoAnalyzeTrips tripAnalysis = new mtoAnalyzeTrips(rb);
        tripAnalysis.runMtoAnalyzeTrips(trips, internalZoneList);


    }

    public static ArrayList<String> getTripPurposes() {
        return tripPurposes;
    }

    public static ArrayList<String> getTripStates() {
        return tripStates;
    }

    public static float[] readPersonSocioDemographics(Person pers) {
        float personDescription[] = new float[14];
        //intercept always = 1
        personDescription[0] = 1;
        //Young = 1 if age is under 25
        if (pers.getAge() < 25) {
            personDescription[1] = 1;
        } else {
            personDescription[1] = 0;
        }
        //Retired = 1 if age is over 64
        if (pers.getAge() > 64) {
            personDescription[2] = 1;
        } else {
            personDescription[2] = 0;
        }
        //Gender =  1 if is female
        if (Objects.equals(pers.getGender(), "F")) {
            personDescription[3] = 1;
        } else {
            personDescription[3] = 0;
        }
        //household sizes
        personDescription[4] = pers.getAdultsHh();
        personDescription[5] = pers.getKidsHh();
        //High School
        if (pers.getEducation() == 2) {
            personDescription[6] = 1;
        } else {
            personDescription[6] = 0;
        }
        // Post Secondary
        if (pers.getEducation() > 2 & pers.getEducation() < 6) {
            personDescription[7] = 1;
        } else {
            personDescription[7] = 0;
        }
        //University
        if (pers.getEducation() > 5 & pers.getEducation() < 9) {
            personDescription[8] = 1;
        } else {
            personDescription[8] = 0;
        }
        //Employed = 0 if unemployed
        if (pers.getWorkStatus() > 2) {
            personDescription[9] = 0;
        } else {
            personDescription[9] = 1;
        }
        if (pers.getIncome() >= 100000) {
            //is in income group 4
            personDescription[10] = 0;
            personDescription[11] = 0;
            personDescription[12] = 1;
        } else if (pers.getIncome() >= 70000) {
            //is in income gorup 3
            personDescription[10] = 0;
            personDescription[11] = 1;
            personDescription[12] = 0;
        } else if (pers.getIncome() >= 50000) {
            //is in income group 2
            personDescription[10] = 1;
            personDescription[11] = 0;
            personDescription[12] = 0;
        } else {
            personDescription[10] = 0;
            personDescription[11] = 0;
            personDescription[12] = 0;
        }

        personDescription[13] = (float) pers.getHousehold().getZone().getAccessibility();

        return personDescription;
    }

    public static ArrayList<Person> addAdultsHhTravelParty(Person pers, String tripPurpose, TableDataSet travelPartyProbabilities) {

        ArrayList<Person> hhTravelParty = new ArrayList<>();
        int hhmember = 0;
        hhTravelParty.add(0, pers);
        double randomChoice2 = Math.random();
        Household hhold = pers.getHousehold();
        for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
            if (pers2 != pers & !pers2.isAway & !pers2.isDaytrip & !pers2.isInOutTrip & pers2.getAge() > 17) {
                String column = tripPurpose + "." + Math.min(pers.getAdultsHh(), 5);
                double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                if (randomChoice2 < probability2) {
                    if (pers.isAway) pers2.isAway = true;
                    else if (pers.isDaytrip) pers2.isDaytrip = true;
                    else if (pers.isInOutTrip) pers2.isInOutTrip = true;
                    hhmember++;
                    hhTravelParty.add(hhmember, pers2);
                }
            }
        }
        return hhTravelParty;
    }

    public static ArrayList<Person> addKidsHhTravelParty(Person pers, String tripPurpose, TableDataSet travelPartyProbabilities) {
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        int hhmember = 0;
        double randomChoice2 = Math.random();
        Household hhold = pers.getHousehold();
        for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
            if (pers2 != pers & !pers2.isAway & !pers2.isDaytrip & !pers2.isInOutTrip & pers2.getAge() < 18) {
                String column = "kids." + tripPurpose + "." + Math.min(pers.getKidsHh(), 5);
                double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember+1, 5), column);
                if (randomChoice2 < probability2) {
                    if (pers.isAway) pers2.isAway = true;
                    else if (pers.isDaytrip) pers2.isDaytrip = true;
                    else if (pers.isInOutTrip) pers2.isInOutTrip = true;
                    hhTravelParty.add(hhmember, pers2);
                    hhmember++;
                }
            }
        }
        return hhTravelParty;
    }

    public static int addNonHhTravelPartySize(String tripPurpose, TableDataSet travelPartyProbabilities) {

        double randomChoice3 = Math.random();
        int k = 0;
        String column = tripPurpose + ".nonHh";
        while (randomChoice3 < travelPartyProbabilities.getIndexedValueAt(k + 1, column) & k < 10)
            k++;
        return k;
    }

    public static int estimateTripDuration(double[] probability) {
        int tripDuration = 1;
        double randomChoice4 = Math.random();
        while (tripDuration < 30 & randomChoice4 < probability[0] / (probability[0] + probability[2])) {
            randomChoice4 = Math.random();
            tripDuration++;
        }
        return tripDuration;
    }


}

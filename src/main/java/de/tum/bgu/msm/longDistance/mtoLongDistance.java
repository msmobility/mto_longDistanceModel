package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import javafx.collections.FXCollections;
import org.apache.log4j.Logger;

import java.util.*;

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

    //public static ArrayList<String> tripPurposes = new ArrayList<>();
    public static List<String> tripPurposes = Arrays.asList("visit","business","leisure");

    //public static ArrayList<String> tripStates = new ArrayList<>();

    public static List<String> tripStates = Arrays.asList("away","daytrip","inout");

    public void runLongDistanceModel () {
        // main method to run long-distance model

        //read synthetic population
        readSP rsp = new readSP(rb);
        ArrayList<Zone> internalZoneList= rsp.readInternalZones();
        rsp.readSyntheticPopulation(internalZoneList);

        //read internal zone employment and external zones
        //added omx.jar; if not this doesn't work
        mtoLongDistData md = new mtoLongDistData(rb);
        md.readInternalZonesEmployment(internalZoneList);
        ArrayList<Zone> externalZoneList = md.readExternalZones();

        //join all zones
        ArrayList<Zone> zoneList = new ArrayList<>();
        zoneList.addAll(internalZoneList);
        zoneList.addAll(externalZoneList);

        //read skims
        md.readSkim();

        //calculate accessibility
        //TODO in the future may be required to calculated different types of accessibility
        //input parameters for accessibility calculations: from "fromZones" to "toZones"
        List<String> fromZones = ResourceUtil.getListWithUserDefinedSeparator(rb,"orig.zone.type",",");
        List<String> toZones = ResourceUtil.getListWithUserDefinedSeparator(rb,"dest.zone.type",",");
        //manually would be List<String> fromZones = Arrays.asList("ONTARIO","EXTCANADA");

        md.calculateAccessibility(zoneList, fromZones, toZones);
        md.writeOutAccessibilities(zoneList);

        logger.info("Accessibility calculated");

        //generate domestic trips
        tripGeneration tgdomestic = new tripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_domestic = tgdomestic.runTripGeneration();
        logger.info("Domestic Trips from Ontario generated");

        //generate international trips (must be done after domestic)
        internationalTripGeneration tginternational = new internationalTripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_international = tginternational.runInternationalTripGeneration();
        logger.info("International trips from Ontario generated");

        //generate visitors
        VisitorsTripGeneration vgen = new VisitorsTripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_visitors  = vgen.runVisitorsTripGeneration(externalZoneList);
        logger.info("Visitor trips to Ontario generated");

        //analyze and write out generated trips
        //first, join the different list of trips
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();
        trips.addAll(trips_international);
        trips.addAll(trips_domestic);
        trips.addAll(trips_visitors);

        mtoAnalyzeTrips tripAnalysis = new mtoAnalyzeTrips(rb);
        if(ResourceUtil.getBooleanProperty(rb,"analyze.trips",false)) tripAnalysis.runMtoAnalyzeTrips(trips, zoneList);
    }

    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
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

    //methods to add persons to the travel party (valid for ONTARIO residents)

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

    //method to estimate trip duration (used only for ONTARIO residents

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

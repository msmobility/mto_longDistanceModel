package de.tum.bgu.msm.longDistance.tripGeneration;


import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.MtoLongDistance;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.*;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by Carlos Llorca on 7/4/2016.
 * Technical Universty of Munich
 * <p>
 * Class to generate trips for the synthetic population
 * <p>
 * works for domestic trips
 */

public class DomesticTripGeneration {

    private List<String> tripPurposes = MtoLongDistData.getTripPurposes();
    private List<String> tripStates = MtoLongDistData.getTripStates();

    private MtoLongDistData ldd;

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    private ResourceBundle rb;

    public DomesticTripGeneration(ResourceBundle rb) {
        this.rb = rb;
        this.ldd = ldd;
    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runTripGeneration(ReadSP syntheticPopulation) {
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();

        //domestic trip generation
        //read the coefficients and probabilities of increasing travel parties

        String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
        TableDataSet tripGenerationCoefficients = Util.readCSVfile(tripGenCoefficientsFilename);
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));

        String travelPartyProbabilitiesFilename = rb.getString("domestic.parties");;
        TableDataSet travelPartyProbabilities =  Util.readCSVfile(travelPartyProbabilitiesFilename);
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));


         // initialize the trip count to zero
        int tripCount = 0;
        for (Household hhold : syntheticPopulation.getHouseholds()) {
            //pick and shuffle the members of the household
            ArrayList<Person> membersList = new ArrayList<>(Arrays.asList(hhold.getPersonsOfThisHousehold()));
            Collections.shuffle(membersList);


            for (Person pers : membersList) {
                if (!pers.isAway & !pers.isDaytrip & !pers.isInOutTrip & pers.getAge() > 17) {
                    //obtain a vector of socio-demographics of person and transform to TSRC
                    float[] personDescription = readPersonSocioDemographics(pers);
                    for (String tripPurpose : tripPurposes) {
                    //the model would only be applied to a person who is an adult and is not in a long distance travel already
                        double[] utility = new double[3];
                        double[] probability = new double[3];
                        for (String tripState : tripStates){
                           utility[tripStates.indexOf(tripState)] = estimateMlogitUtility(personDescription, tripPurpose, tripState, tripGenerationCoefficients);
                        }
                        double utilities;
                        utilities = 1 + Math.exp(utility[0]) + Math.exp(utility[1]) + Math.exp(utility[2]);
                        //calculates the probabilities
                        probability[0] = Math.exp(utility[0]) / utilities;
                        probability[1] = Math.exp(utility[1]) / utilities;
                        probability[2] = Math.exp(utility[2]) / utilities;

                        //store the probabilities for later international trip generation
                        //TODO maybe this is only needed for non traveller and this way international trip generation is faster
                        pers.travelProbabilities = new float[3][3];
                        for (String tripState : tripStates){
                            pers.travelProbabilities[tripStates.indexOf(tripState)][tripPurposes.indexOf(tripPurpose)] = (float) probability[tripStates.indexOf(tripState)];
                        }
                        // generate a random value for each person and purpose to get the domestic travel decisions
                        double randomChoice1 = Math.random();
                        if (randomChoice1 < probability[0]){
                            pers.isAway = true;
                            LongDistanceTrip trip = createLongDistanceTrip(pers, tripPurpose,"away", probability, tripCount, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        } else if (randomChoice1 < probability[1] + probability[0] ){
                            pers.isDaytrip = true;
                            LongDistanceTrip trip = createLongDistanceTrip(pers, tripPurpose, "daytrip", probability, tripCount, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        } else if (randomChoice1 < probability[2] + probability[1] + probability[0] ){
                            pers.isInOutTrip = true;
                            LongDistanceTrip trip = createLongDistanceTrip(pers, tripPurpose, "inout", probability, tripCount, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        }
                    }
                }
            }
        }
        return trips;
    }

    public static float[] readPersonSocioDemographics(Person pers) {
        float personDescription[] = new float[15];
        //change size to 15 if "winter" is added
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
        if (pers.getGender() == 'F') {
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

        //variable is winter
        if (Mto.getWinter()){
        personDescription[14] = 1;
         } else {
        personDescription[14] = 0;
        }

        return personDescription;
    }

    public static double estimateMlogitUtility(float[] personDescription, String tripPurpose, String tripState, TableDataSet tripGenerationCoefficients) {
        double utility = 0;
        double probability;
        // set sum of utilities of the 4 alternatives

        //j is an index for tripStates
        // 0 = away
        // 1 = daytrip
        // 2 = inout trip
        //the next loop calculate the utilities for the 3 states (stay at home has utility 0)

        String coefficientColumn = tripState + "." + tripPurpose;
        for (int var = 0; var < personDescription.length; var++) {
            //var is an index for the variables of person and coefficients of the model
            utility += tripGenerationCoefficients.getIndexedValueAt(var + 1, coefficientColumn) * personDescription[var];
        }
        return utility;
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

    private LongDistanceTrip createLongDistanceTrip(Person pers, String tripPurpose, String tripState, double probability[], int tripCount, TableDataSet travelPartyProbabilities){

        ArrayList<Person> adultsHhTravelParty = addAdultsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = addKidsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(adultsHhTravelParty);
        int nonHhTravelPartySize = addNonHhTravelPartySize(tripPurpose, travelPartyProbabilities);
        int tripDuration;
        if (pers.isDaytrip) tripDuration = 0;
        else {
            tripDuration = estimateTripDuration(probability);
        }
        return new LongDistanceTrip(pers, false, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState),
                pers.getHousehold().getZone(), tripDuration, adultsHhTravelParty.size(), kidsHhTravelParty.size(), nonHhTravelPartySize);

    }



}






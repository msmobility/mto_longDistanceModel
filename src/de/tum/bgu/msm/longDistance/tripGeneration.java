package de.tum.bgu.msm.longDistance;


import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.syntheticPopulation.*;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;


import static de.tum.bgu.msm.longDistance.LongDistanceTrip.getLongDistanceTripFromId;
import static de.tum.bgu.msm.syntheticPopulation.Household.getHouseholdArray;
import static de.tum.bgu.msm.syntheticPopulation.Person.*;


/**
 * Created by Carlos Llorca on 7/4/2016.
 * Technical Universty of Munich
 * <p>
 * Class to generate trips for the synthetic population
 *
 * works for domestic trips
 *
 */

public class tripGeneration {

    private TableDataSet tripGenerationCoefficients;
    private TableDataSet travelPartyProbabilities;

    String[] tripPurposes = {"visit", "business", "leisure"};
    String[] tripStates = {"away", "daytrip", "inout"};


    // TODO review if next 4 lines are needed or should be changed
    static Logger logger = Logger.getLogger(tripGeneration.class);
    private ResourceBundle rb;

    public tripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }

    //method to run the trip generation
    public void runTripGeneration() {

        //domestic trip generation
        //read the coefficients
        //read probabilities of increasing travel parties

            String tripGenCoefficientsFilename = "input/tripGeneration/tripGenerationCoefficients.csv";
            tripGenerationCoefficients = util.readCSVfile(tripGenCoefficientsFilename);
            tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));

            String travelPartyProbabilitiesFilename = "input/tripGeneration/travelPartyProbabilities.csv";
            travelPartyProbabilities = util.readCSVfile(travelPartyProbabilitiesFilename);
            travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));



        //apply model to determine whether a person is at home, away, in a daytrip or in an inbound/outbound trip
        // initialize the trip count to zero
        int tripCount = 0;
        for (Household hhold : getHouseholdArray()) {
            //next lines shuffle the list of hh members

            Person[] hhMembers = hhold.getPersonsOfThisHousehold();
            List<Person> membersList= new ArrayList<>();
            for (int m=0; m < hhold.getHhSize(); m++ ) {
                membersList.add(hhMembers[m]);
            }
            Collections.shuffle(membersList);
            for (int m = 0; m < membersList.size(); m++) {
                hhMembers[m] = membersList.get(m);
            }
            for (Person pers: membersList){

                int hhmember = 0;
                //obtain a vector of socio-demographics of person "PersonFactors". It requires transforming part of the variables from Synthetic Pop structure to the mlogit models (TSRC)
                int PersonFactors[] = new int[13];
                //intercept always = 1
                PersonFactors[0] = 1;
                //Young = 1 if age is under 25
                if (pers.getAge() < 25) {
                    PersonFactors[1] = 1;
                } else {
                    PersonFactors[1] = 0;
                }
                //Retired = 1 if age is over 64
                if (pers.getAge() > 64) {
                    PersonFactors[2] = 1;
                } else {
                    PersonFactors[2] = 0;
                }
                //Gender =  1 if is female
                if (Objects.equals(pers.getGender(), "F")) {
                    PersonFactors[3] = 1;
                } else {
                    PersonFactors[3] = 0;
                }
                //household sizes
                PersonFactors[4] = pers.getAdultsHh();
                PersonFactors[5] = pers.getKidsHh();
                //High School
                if (pers.getEducation() == 2) {
                    PersonFactors[6] = 1;
                } else {
                    PersonFactors[6] = 0;
                }
                // Post Secondary
                if (pers.getEducation() > 2 & pers.getEducation() < 6) {
                    PersonFactors[7] = 1;
                } else {
                    PersonFactors[7] = 0;
                }
                //University
                if (pers.getEducation() > 5 & pers.getEducation() < 9) {
                    PersonFactors[8] = 1;
                } else {
                    PersonFactors[8] = 0;
                }
                //Employed = 0 if unemployed
                if (pers.getWorkStatus() > 2) {
                    PersonFactors[9] = 0;
                } else {
                    PersonFactors[9] = 1;
                }
                if (pers.getIncome() >= 100000) {
                    //is in income group 4
                    PersonFactors[10] = 0;
                    PersonFactors[11] = 0;
                    PersonFactors[12] = 1;
                } else if (pers.getIncome() >= 70000) {
                    //is in income gorup 3
                    PersonFactors[10] = 0;
                    PersonFactors[11] = 1;
                    PersonFactors[12] = 0;
                } else if (pers.getIncome() >= 50000) {
                    //is in income group 2
                    PersonFactors[10] = 1;
                    PersonFactors[11] = 0;
                    PersonFactors[12] = 0;
                } else {
                    PersonFactors[10] = 0;
                    PersonFactors[11] = 0;
                    PersonFactors[12] = 0;
                }


                //apply the equations for each trip purpose
                //TODO randomize the selection of purpose instead of starting with visit
                for (int i = 0; i < tripPurposes.length; i++) {
                    //i is an index of purpose
                    // 0 = visit
                    // 1 = business
                    // 2 = leisure
                    String tripPurpose = tripPurposes[i];
                    // generate a random value for each person and purpose to get the domestic travel decisions
                    double randomChoice1 = Math.random();
                    //the model would only be applied to a person who is an adult and is not in a long distance travel already
                    if (!pers.isAway & !pers.isDaytrip & !pers.isInOutTrip & pers.getAge() > 17) {
                        //the mlogit model is applied if the last person in the household was not travelling, on the contrary, build a travel party without accounting for these probabilities
                        //****if (!lastIsAway & !lastIsDaytrip & !lastIsInOut) {


                        //sets utilities to zero and probabilities to zero
                        double utility[] = new double[3];
                        double probability[] = new double[3];
                        // set sum of utilities of the 4 alternatives
                        double utilities;
                        //j is an index for tripStates
                        // 0 = away
                        // 1 = daytrip
                        // 2 = inout trip
                        //the next loop calculate the utilities for the 3 states (stay at home has utility 0)
                        for (int j = 0; j < tripStates.length; j++) {
                            String tripState = tripStates[j];
                            String coefficientColumn = tripState + "." + tripPurpose;
                            for (int var = 0; var < PersonFactors.length; var++) {
                                //var is an index for the variables of person and coefficients of the model
                                utility[j] += tripGenerationCoefficients.getIndexedValueAt(var + 1, coefficientColumn) * PersonFactors[var];
                            }
                        }
                        utilities = 1 + Math.exp(utility[0]) + Math.exp(utility[1]) + Math.exp(utility[2]);
                        //calculates the probabilities
                        probability[0] = Math.exp(utility[0]) / utilities;
                        probability[1] = Math.exp(utility[1]) / utilities;
                        probability[2] = Math.exp(utility[2]) / utilities;
                        pers.travelProbabilities[0][i] = (float)probability[0];
                        pers.travelProbabilities[1][i] = (float)probability[1];
                        pers.travelProbabilities[2][i] = (float)probability[2];
                        if (randomChoice1 > probability[2] + probability[1] + probability[0]) {
                            pers.isAway = false;
                            pers.isDaytrip = false;
                            pers.isInOutTrip = false;
                            hhmember = 0;
                            //there is no trip with the purpose i
                        } else if (randomChoice1 > probability[1] + probability[0]) {
                            pers.isAway = false;
                            pers.isDaytrip = false;
                            pers.isInOutTrip = true;
                            //is InOutTrip with the purpose i
                            //calculates travel parties
                            //first, in the household
                            ArrayList<Person> hhTravelParty = new ArrayList<>(1);
                            hhTravelParty.add(0,pers);
                            double randomChoice2 = Math.random();
                            for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
                                if (pers2 != pers & !pers2.isAway & !pers2.isDaytrip & !pers2.isInOutTrip & pers2.getAge() > 17) {
                                    String column = tripPurpose + "." + Math.min(pers.getAdultsHh(),5);
                                    double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                                    if (randomChoice2 < probability2) {
                                        pers2.isAway = false;
                                        pers2.isDaytrip = false;
                                        pers2.isInOutTrip = true;
                                        hhmember++;
                                        hhTravelParty.add(hhmember,pers2);
                                    }
                                }
                            }
                            //second: non hh members
                            double randomChoice3 = Math.random();
                            int k=0;
                            String column =  tripPurpose + ".nonHh";
                            while (randomChoice3 < travelPartyProbabilities.getIndexedValueAt(k+1, column) & k < 10 ) k++;
                            //estimate the trip duration
                            //being away, one can only continue being away (0) or being in (2)
                            int tripDuration = 1;
                            double randomChoice4 = Math.random();
                            while (tripDuration < 30 & randomChoice3 < probability[0] / (probability[0] + probability[2])) {
                                randomChoice4 = Math.random();
                                tripDuration++;
                            }
                            //generate a trip
                            new LongDistanceTrip(tripCount, pers.getPersonId(), false, i, 2, hhold.getTaz(), tripDuration, hhmember + 1, hhTravelParty, k);
                            tripCount++;
                        } else if (randomChoice1 > probability[0]) {
                            pers.isAway = false;
                            pers.isDaytrip = true;
                            pers.isInOutTrip = false;
                            // is daytrip with the purpose i
                            ArrayList<Person> hhTravelParty = new ArrayList<>(1);
                            hhTravelParty.add(0,pers);
                            double randomChoice2 = Math.random();
                            for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
                                if (pers2 != pers & !pers2.isAway & !pers2.isDaytrip & !pers2.isInOutTrip & pers2.getAge() > 17) {
                                    String column = tripPurpose + "." + Math.min(pers.getAdultsHh(),5);
                                    double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                                    if (randomChoice2 < probability2 ) {
                                        pers2.isAway = false;
                                        pers2.isDaytrip = true;
                                        pers2.isInOutTrip = false;
                                        hhmember++;
                                        hhTravelParty.add(hhmember,pers2);
                                    }
                                }
                            }
                            int k=0;
                            String column =  tripPurpose + ".nonHh";
                            double randomChoice3 = Math.random();
                            while (randomChoice3 < travelPartyProbabilities.getIndexedValueAt(k+1, column) & k < 10 ) k++;
                            //generate a daytrip
                            new LongDistanceTrip(tripCount, pers.getPersonId(), false, i, 1, hhold.getTaz(), 0, hhmember + 1, hhTravelParty, k);
                            tripCount++;
                        } else {
                            pers.isAway = true;
                            pers.isDaytrip = false;
                            pers.isInOutTrip = false;
                            ArrayList<Person> hhTravelParty = new ArrayList<>(1);
                            hhTravelParty.add(0,pers);
                            double randomChoice2 = Math.random();
                            //estimate travel party
                            for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
                                if (pers2 != pers & !pers2.isAway & !pers2.isDaytrip & !pers2.isInOutTrip & pers2.getAge() > 17) {
                                    String column = tripPurpose + "." + Math.min(pers.getAdultsHh(),5);
                                    double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                                    if (randomChoice2 < probability2) {
                                        pers2.isAway = true;
                                        pers2.isDaytrip = false;
                                        pers2.isInOutTrip = false;
                                        hhmember++;
                                        hhTravelParty.add(hhmember,pers2);
                                    }
                                }
                            }
                            int k=0;
                            String column =  tripPurpose + ".nonHh";
                            double randomChoice3 = Math.random();
                            while (randomChoice3 < travelPartyProbabilities.getIndexedValueAt(k+1, column) & k < 10 ) k++;
                            //estimate trip duration
                            //while away, one can only be away or out
                            int tripDuration = 1;
                            double randomChoice4 = Math.random();
                            while (tripDuration < 30 & randomChoice3 < probability[0] / (probability[0] + probability[2])) {
                                randomChoice4 = Math.random();
                                tripDuration++;
                            }
                            //generate a trip
                            new LongDistanceTrip(tripCount, pers.getPersonId(), false, i, 0, hhold.getTaz(),  tripDuration, hhmember + 1, hhTravelParty, k);
                            tripCount++;
                        }
                    }
                }
            }
        }
    }
}






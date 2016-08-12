package de.tum.bgu.msm.longDistance;


import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.syntheticPopulation.*;
import org.apache.log4j.Logger;

import java.util.*;

import static de.tum.bgu.msm.syntheticPopulation.Household.getHouseholdArray;


/**
 * Created by Carlos Llorca on 7/4/2016.
 * Technical Universty of Munich
 * <p>
 * Class to generate trips for the synthetic population
 * <p>
 * works for domestic trips
 */

public class tripGeneration {

    private ArrayList<String> tripPurposes = mtoLongDistance.getTripPurposes();
    private ArrayList<String> tripStates = mtoLongDistance.getTripStates();



    static Logger logger = Logger.getLogger(tripGeneration.class);
    private ResourceBundle rb;

    public tripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runTripGeneration() {
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();



        //domestic trip generation
        //read the coefficients and probabilities of increasing travel parties

        //todo add properties
        String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
        TableDataSet tripGenerationCoefficients = util.readCSVfile(tripGenCoefficientsFilename);
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));

        String travelPartyProbabilitiesFilename = rb.getString("domestic.parties");;
        TableDataSet travelPartyProbabilities =  util.readCSVfile(travelPartyProbabilitiesFilename);
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));


         // initialize the trip count to zero
        int tripCount = 0;
        for (Household hhold : getHouseholdArray()) {
            //pick and shuffle the members of the household
            ArrayList<Person> membersList = new ArrayList<>(Arrays.asList(hhold.getPersonsOfThisHousehold()));
            Collections.shuffle(membersList);

            for (Person pers : membersList) {

                //obtain a vector of socio-demographics of person and transform to TSRC
                float[] personDescription = mtoLongDistance.readPersonSocioDemographics(pers);
                for (String tripPurpose : tripPurposes) {
                    //the model would only be applied to a person who is an adult and is not in a long distance travel already
                    if (!pers.isAway & !pers.isDaytrip & !pers.isInOutTrip & pers.getAge() > 17) {

                        double[] probability = estimateMlogitFormula(personDescription, tripPurpose, tripGenerationCoefficients);
                        //store the probabilities for later international trip generation
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

    private double[] estimateMlogitFormula(float[] personDescription, String tripPurpose, TableDataSet tripGenerationCoefficients) {
        double utility[] = new double[3];
        double probability[] = new double[3];
        // set sum of utilities of the 4 alternatives
        double utilities;
        //j is an index for tripStates
        // 0 = away
        // 1 = daytrip
        // 2 = inout trip
        //the next loop calculate the utilities for the 3 states (stay at home has utility 0)
        for (String tripState: tripStates) {
            String coefficientColumn = tripState + "." + tripPurpose;
            for (int var = 0; var < personDescription.length; var++) {
                //var is an index for the variables of person and coefficients of the model
                utility[tripStates.indexOf(tripState)] += tripGenerationCoefficients.getIndexedValueAt(var + 1, coefficientColumn) * personDescription[var];
            }
        }
        utilities = 1 + Math.exp(utility[0]) + Math.exp(utility[1]) + Math.exp(utility[2]);
        //calculates the probabilities
        probability[0] = Math.exp(utility[0]) / utilities;
        probability[1] = Math.exp(utility[1]) / utilities;
        probability[2] = Math.exp(utility[2]) / utilities;

        return probability;
    }

    private LongDistanceTrip createLongDistanceTrip(Person pers, String tripPurpose, String tripState, double probability[], int tripCount, TableDataSet travelPartyProbabilities){

        ArrayList<Person> adultsHhTravelParty = mtoLongDistance.addAdultsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = mtoLongDistance.addKidsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(adultsHhTravelParty);
        int nonHhTravelPartySize = mtoLongDistance.addNonHhTravelPartySize(tripPurpose, travelPartyProbabilities);
        int tripDuration;
        if (pers.isDaytrip) tripDuration = 0;
        else {
            tripDuration = mtoLongDistance.estimateTripDuration(probability);
        }

        return new LongDistanceTrip(tripCount, pers.getPersonId(), false, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), pers.getHousehold().getZone(), tripDuration, adultsHhTravelParty.size(), kidsHhTravelParty.size(), hhTravelParty, nonHhTravelPartySize);

    }


}






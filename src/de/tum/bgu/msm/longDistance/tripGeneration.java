package de.tum.bgu.msm.longDistance;


import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.syntheticPopulation.*;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;


import static de.tum.bgu.msm.syntheticPopulation.Person.*;


/**
 * Created by Carlos Llorca on 7/4/2016.
 * Technical Universty of Munich
 *
 * Class to generate trips for the synthetic population
 */

public class tripGeneration {

    private TableDataSet tripGenerationCoefficients;

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

        //TODO consider adding this fileName as mto_property
        String tripGenCoefficientsFilename = "input/tripGeneration/tripGenerationCoefficients.csv";

        tripGenerationCoefficients = util.readCSVfile(tripGenCoefficientsFilename);
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));


        //apply model to determine whether a person is at home, away, in a daytrip or in an inbound/outbound trip
        // initialize the tripcounts
        int tripCount = 0;
        for (Person pers : getSyntheticPersonArray()) {

            //apply the equation to obtain utility: vector of factors namely PersonFactors. It requires transforming the
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
            //housheold sizes
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
            } else if (pers.getIncome() >= 70000 ) {
                //is in income gorup 3
                PersonFactors[10] = 0;
                PersonFactors[11] = 1;
                PersonFactors[12] = 0;
            } else if (pers.getIncome() >= 50000 ) {
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
                   if (randomChoice1 > probability[2]+probability[1] + probability[0]) {
                        pers.isAway = false;
                        pers.isDaytrip = false;
                        pers.isInOutTrip = false;
                        //there is no trip with the purpose i
                    } else if (randomChoice1 > probability[1]+ probability[0] ) {
                        pers.isAway = false;
                        pers.isDaytrip = false;
                        pers.isInOutTrip = true;
                        //is InOutTrip with the purpose i
                        new LongDistanceTrip(tripCount++, pers.getPersonId(), false, i, 2, 0, 0,1);
                    } else if (randomChoice1 > probability[0] ) {
                        pers.isAway = false;
                        pers.isDaytrip = true;
                        pers.isInOutTrip = false;
                        //is daytrip with the purpose i
                        new LongDistanceTrip(tripCount++, pers.getPersonId(), false, i, 1, 0, 0,1);
                    } else {
                        pers.isAway = true;
                        pers.isDaytrip = false;
                        pers.isInOutTrip = false;
                        //is away with the purpose i
                        new LongDistanceTrip(tripCount++, pers.getPersonId(), false, i, 0, 0, 0,1);
                    }
                }
            }
        }
        int multTripCount =0;
        for (LongDistanceTrip tr1: LongDistanceTrip.getLongDistanceTripArray()) {
            Person traveller1 = getPersonFromId(tr1.getPersonId());
            Household hh1 = traveller1.getHousehold();
            for (Person p1: hh1.getPersonsOfThisHousehold()){
                if (traveller1.isAway& p1.isAway & p1 != traveller1) {
                    tr1.travelParty++;
                    multTripCount++;
                } else if (traveller1.isDaytrip& p1.isDaytrip & p1 != traveller1){
                    tr1.travelParty++;
                    multTripCount++;
                }else if (traveller1.isInOutTrip& p1.isInOutTrip & p1 != traveller1){
                    tr1.travelParty++;
                    multTripCount++;
                }
            }
        }
        logger.info("Multiple trips detected: " + multTripCount);
    }
}






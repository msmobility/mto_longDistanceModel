package de.tum.bgu.msm.longDistance;


import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.dataAnalysis.surveyPerson;
import de.tum.bgu.msm.syntheticPopulation.*;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;


import static de.tum.bgu.msm.dataAnalysis.surveyPerson.getPersonArray;


/**
 * Created by carlloga on 7/4/2016.
 *
 * Class to generate trips for the survey population
 */

public class surveyTripGeneration {

    private TableDataSet tripGenerationCoefficients;

    String[] tripPurposes = {"visit", "business", "leisure"};
    String[] tripStates = {"away", "daytrip", "inout"};



    static Logger logger = Logger.getLogger(tripGeneration.class);
    private ResourceBundle rb;

    public surveyTripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }

    //method to run the trip generation
    public void runSurveyTripGeneration() {


        //domestic trip generation
        //read the coefficients
        //workDirectory = rb.getString("work.directory");

        tripGenerationCoefficients = util.readCSVfile("input/tripGeneration/tripGenerationCoefficients.csv");
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));


        //apply model to determine whether a person is at home, away, in a daytrip or in an inbound/outbound trip

        int k = 0;
        for (surveyPerson pers : getPersonArray()) {

            if (pers.getHhIncome() != 9 &  pers.getProv() == 35) {

                //apply the equation to obtain utility: vector of factors
                int PersonFactors[] = new int[13];
                //intercept always = 1
                PersonFactors[0] = 1;
                //Young = 1 if age is age group is 1
                if (pers.getAgeGroup() == 1) {
                    PersonFactors[1] = 1;
                } else {
                    PersonFactors[1] = 0;
                }
                //Retired = 1 if age group is 6
                if (pers.getAgeGroup() == 6) {
                    PersonFactors[2] = 1;
                } else {
                    PersonFactors[2] = 0;
                }
                //Gender =  1 if is female
                if (pers.getGender() == 2) {
                    PersonFactors[3] = 1;
                } else {
                    PersonFactors[3] = 0;
                }
                //houshold sizes
                PersonFactors[4] = pers.getAdultsInHh();
                PersonFactors[5] = pers.getKidsInHh();


                //High School
                if (pers.getEducation() == 2) {
                    PersonFactors[6] = 1;
                } else {
                    PersonFactors[6] = 0;
                }
                // Post Secondary
                if (pers.getEducation() == 3) {
                    PersonFactors[7] = 1;
                } else {
                    PersonFactors[7] = 0;
                }
                //University
                if (pers.getEducation() == 4) {
                    PersonFactors[8] = 1;
                } else {
                    PersonFactors[8] = 0;
                }
                //Employed = 0 if unemployed
                if (pers.getLaborStat() == 1) {
                    PersonFactors[9] = 1;
                } else {
                    PersonFactors[9] = 0;
                }


                if (pers.getHhIncome() == 4) {
                    //is in income group 4
                    PersonFactors[10] = 0;
                    PersonFactors[11] = 0;
                    PersonFactors[12] = 1;
                } else if (pers.getHhIncome() == 3) {
                    //is in income gorup 3
                    PersonFactors[10] = 0;
                    PersonFactors[11] = 1;
                    PersonFactors[12] = 0;
                } else if (pers.getHhIncome() == 2) {
                    //is in income group 2
                    PersonFactors[10] = 1;
                    PersonFactors[11] = 0;
                    PersonFactors[12] = 0;
                } else {
                    PersonFactors[10] = 0;
                    PersonFactors[11] = 0;
                    PersonFactors[12] = 0;
                }


                for (int i = 0; i < tripPurposes.length; i++) {
                    //i is an index of purposes
                    String tripPurpose = tripPurposes[i];

                    // a random value for each person and purpose
                    double randomChoice1 = Math.random();

                    //the model would only be applied to a person who is an adult and is not in a long distance travel already
                    if (!pers.isAway & !pers.isDaytrip & !pers.isInOutTrip) {

                        //sets utilities to zero and probabilities to zero
                        double utility[] = new double[3];
                        double probability[] = new double[3];

                        // sum of utilities of the 4 alternatives
                        double utilities;


                        //j is an index for tripStates
                        // 0 = away
                        // 1 = daytrip
                        // 2 = inout trip

                        for (int j = 0; j < tripStates.length; j++) {
                            String tripState = tripStates[j];

                            String column = tripState + "." + tripPurpose;

                            for (int var = 0; var < PersonFactors.length; var++) {
                                //var is an index for the variables of person and coefficients of the model
                                utility[j] += tripGenerationCoefficients.getIndexedValueAt(var + 1, column) * PersonFactors[var];
                            }
                        }


                        utilities = 1 + Math.exp(utility[0]) + Math.exp(utility[1]) + Math.exp(utility[2]);

                        //calculates the probabilities
                        probability[0] = Math.exp(utility[0]) / utilities;
                        probability[1] = Math.exp(utility[1]) / utilities;
                        probability[2] = Math.exp(utility[2]) / utilities;


                        if (randomChoice1 > probability[2] + probability[1] + probability[0]) {
                            pers.isAway = false;
                            pers.isDaytrip = false;
                            pers.isInOutTrip = false;
                            //there is no trip with the purpose i

                        } else if (randomChoice1 > probability[1] + probability[0]) {
                            pers.isAway = false;
                            pers.isDaytrip = false;
                            pers.isInOutTrip = true;
                            //is InOutTrip with the purpose i
                            new LongDistanceTrip(k, (int) pers.getPumfId(), false, i, 2, 0, (int) pers.getWeight(),1);

                        } else if (randomChoice1 > probability[0]) {
                            pers.isAway = false;
                            pers.isDaytrip = true;
                            pers.isInOutTrip = false;
                            //is daytrip with the purpose i
                            new LongDistanceTrip(k, (int) pers.getPumfId(), false, i, 1, 0, (int) pers.getWeight(),1);
                        } else {
                            pers.isAway = true;
                            pers.isDaytrip = false;
                            pers.isInOutTrip = false;
                            //is away with the purpose i
                            new LongDistanceTrip(k, (int) pers.getPumfId(), false, i, 0, 0, (int) pers.getWeight(),1);

                        }


                    }

                }
                k++;
            }


        }
        logger.info("Survey Persons read:" + k);
    }
}






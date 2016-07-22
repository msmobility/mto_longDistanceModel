package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.tripGeneration;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 7/19/2016.
 * <p>
 * Class to generate international trips following Rolf's method: pick up individuals according to their probability
 * of domestic trips until the number of trips per day is reached
 * <p>
 * <p>
 * Based on number of trips and increased later with travel parties.
 */
public class internationalTripGeneration {

    private TableDataSet numberOfInternationalTrips;

    String[] tripPurposes = {"visit", "business", "leisure"};
    String[] tripStates = {"away", "daytrip", "inout"};

    static Logger logger = Logger.getLogger(tripGeneration.class);
    private ResourceBundle rb;

    public internationalTripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }

    //method to run the trip generation
    public void runInternationalTripGeneration() {

        String numberOfInternationalTripsFilename = "input/tripGeneration/intNumberOfTrips.csv";

        numberOfInternationalTrips = util.readCSVfile(numberOfInternationalTripsFilename);
        numberOfInternationalTrips.buildIndex(numberOfInternationalTrips.getColumnPosition("tripState"));

        float[][] sumProbabilities = new float[3][3];
        int[] personIdMatrix = new int[Person.getSyntheticPersonArray().length];
        float[][][] probabilityMatrix = new float[3][3][Person.getSyntheticPersonArray().length];
        int p = 0;
        //get the sum
        for (Person pers : Person.getSyntheticPersonArray()) {
            personIdMatrix[p] = pers.getPersonId();

            for (int i = 0; i < tripPurposes.length; i++) {
                for (int j = 0; j < tripStates.length; j++) {
                    sumProbabilities[i][j] += pers.travelProbabilities[i][j];
                    probabilityMatrix[i][j][p] = pers.travelProbabilities[i][j];
                }
            }
            p++;
        }


        logger.info("Int Trip: sum of probabilities done");

        int tripId = 1000000;

        for (int i = 0; i < tripPurposes.length; i++) {
            for (int j = 0; j < tripStates.length; j++) {
                logger.info("starting trip generation for purpose " + i + " and state " + j);
                int tripCount = 0;


                // multiplied by 2 to get more chances of assigning all trips
                float[] randomChoice = new float[(int) numberOfInternationalTrips.getIndexedValueAt(j, tripPurposes[i]) * 2];
                for (int k = 0; k < randomChoice.length; k++) {
                    randomChoice[k] = (float) Math.random();
                }
                //sort the matrix for faster lookup
                Arrays.sort(randomChoice);
                p = 0;
                float cumulative = probabilityMatrix[i][j][p];
                for (int k = 0; k < randomChoice.length; k++) {
                    while (randomChoice[k] > cumulative & p < personIdMatrix.length - 1 ) {
                        p++;
                        cumulative += probabilityMatrix[i][j][p] / sumProbabilities[i][j];

                    }

                        Person pers = Person.getPersonFromId(personIdMatrix[p]);
                        if (!pers.isDaytrip & !pers.isAway & !pers.isInOutTrip & pers.getAge() > 17 & tripCount < numberOfInternationalTrips.getIndexedValueAt(j, tripPurposes[i])) {
                            if (j == 0) pers.isAway = true;
                            else if (j == 1) pers.isDaytrip = true;
                            else if (j == 2) pers.isInOutTrip = true;
                            Household hh = pers.getHousehold();
                            new LongDistanceTrip(tripId, pers.getPersonId(), true, i, j, hh.getTaz(), 0, 0);
                            //logger.info(tripCount + " trips generated for person " + p);
                            tripId++;
                            tripCount++;
                            //exit from the loop
                            //still not implemented travel party and duration
                        }


                    }
                    logger.info(tripCount + " trips generated for purpose " + i + " and state " + j);

                    //}


            }


        }
        logger.info("Int Trip: all trips generated");
    }


}




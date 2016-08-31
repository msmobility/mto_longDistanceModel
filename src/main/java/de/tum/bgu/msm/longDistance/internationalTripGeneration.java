package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

import de.tum.bgu.msm.longDistance.tripGeneration.*;


/**
 * Created by Carlos on 7/19/2016.
 * Based on number of trips and increased later with travel parties.
 */
public class internationalTripGeneration {



    private ArrayList<String> tripPurposes = mtoLongDistance.getTripPurposes();
    private ArrayList<String> tripStates = mtoLongDistance.getTripStates();

    static Logger logger = Logger.getLogger(tripGeneration.class);
    private ResourceBundle rb;

    public internationalTripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runInternationalTripGeneration() {

        ArrayList<LongDistanceTrip> trips = new ArrayList<>();

        String internationalTriprates = rb.getString("int.trips");
        TableDataSet internationalTripRates = util.readCSVfile(internationalTriprates);
        internationalTripRates.buildIndex(internationalTripRates.getColumnPosition("tripState"));

        String intTravelPartyProbabilitiesFilename = rb.getString("int.parties");;
        TableDataSet travelPartyProbabilities = util.readCSVfile(intTravelPartyProbabilitiesFilename);
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));

        double[][] sumProbabilities = new double[3][3];
        int[] personIdMatrix = new int[Person.getSyntheticPersonArray().length];
        double[][][] probabilityMatrix = new double[3][3][Person.getSyntheticPersonArray().length];
        int p = 0;
        //get the sum
        for (Person pers : Person.getSyntheticPersonArray()) {
            personIdMatrix[p] = pers.getPersonId();

            for (String tripPurpose : tripPurposes) {
                for (String tripState : tripStates) {
                    int i = tripPurposes.indexOf(tripPurpose);
                    int j = tripStates.indexOf(tripState);
                    sumProbabilities[i][j] += pers.travelProbabilities[i][j];
                    probabilityMatrix[i][j][p] = pers.travelProbabilities[i][j];
                }
            }
            p++;
        }
        logger.info("Int Trip: sum of probabilities done");
        for (String tripPurpose : tripPurposes) {
            for (String tripState : tripStates) {
                int tripCount = 0;
                // multiplied by 2 to get more chances of assigning all trips, if not, there are some individuals that are travelling already

                int numberOfTrips = (int)(internationalTripRates.getIndexedValueAt(tripStates.indexOf(tripState), tripPurpose)*personIdMatrix.length);
                double[] randomChoice = new double[numberOfTrips*2 ];
                for (int k = 0; k < randomChoice.length; k++) {
                    randomChoice[k] = Math.random();
                }
                //sort the matrix for faster lookup
                Arrays.sort(randomChoice);
                //look up for the n travellers
                p = 0;
                double cumulative = probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p];
                for (double randomNumber : randomChoice){
                    while (randomNumber > cumulative & p < personIdMatrix.length - 1) {
                        p++;
                        cumulative += probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p] / sumProbabilities[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)];
                    }
                    Person pers = Person.getPersonFromId(personIdMatrix[p]);
                    if (!pers.isDaytrip & !pers.isAway & !pers.isInOutTrip & pers.getAge() > 17 & tripCount < numberOfTrips) {
                        switch (tripState) {
                            case "away" :
                                pers.isAway = true;
                            case "daytrip":
                                pers.isDaytrip = true;
                            case "inout":
                                pers.isInOutTrip = true;
                        }
                        LongDistanceTrip trip = createIntLongDistanceTrip(pers, tripPurpose,tripState, tripCount, travelPartyProbabilities);
                        trips.add(trip);
                        tripCount++;
                    }
                }
                logger.info(tripCount + " trips generated for purpose " + tripPurpose + " and state " + tripState);
            }
        }
        logger.info("Int Trip: all trips generated");
        return trips;
    }

    private LongDistanceTrip createIntLongDistanceTrip(Person pers, String tripPurpose, String tripState, int tripCount, TableDataSet travelPartyProbabilities ){
        ArrayList<Person> adultsHhTravelParty = mtoLongDistance.addAdultsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = mtoLongDistance.addKidsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(adultsHhTravelParty);
        int nonHhTravelPartySize = mtoLongDistance.addNonHhTravelPartySize(tripPurpose, travelPartyProbabilities);
        return new LongDistanceTrip(tripCount, pers.getPersonId(), true, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), pers.getHousehold().getZone(),
                0, adultsHhTravelParty.size(), kidsHhTravelParty.size(), nonHhTravelPartySize);

    }

}

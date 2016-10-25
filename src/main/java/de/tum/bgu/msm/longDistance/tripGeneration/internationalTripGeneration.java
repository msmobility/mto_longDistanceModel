package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.mtoLongDistance;
import de.tum.bgu.msm.longDistance.tripGeneration.tripGeneration;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * Created by Carlos on 7/19/2016.
 * Based on number of trips and increased later with travel parties.
 */
public class internationalTripGeneration {



    private List<String> tripPurposes = mtoLongDistance.getTripPurposes();
    private List<String> tripStates = mtoLongDistance.getTripStates();

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

        String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
        TableDataSet tripGenerationCoefficients = util.readCSVfile(tripGenCoefficientsFilename);
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));

        double[][] sumProbabilities = new double[tripPurposes.size()][tripStates.size()];
        int[] personIdMatrix = new int[Person.getSyntheticPersonArray().length];
        double[][][] probabilityMatrix = new double[tripPurposes.size()][tripStates.size()][Person.getSyntheticPersonArray().length];
        int p = 0;
        //recalculate the probabilities adapted to the new accessibility values
        for (Person pers : Person.getSyntheticPersonArray()) {
            personIdMatrix[p] = pers.getPersonId();
            for (String tripPurpose : tripPurposes) {
                for (String tripState : tripStates){
                    int j = tripStates.indexOf(tripState);
                    int i = tripPurposes.indexOf(tripPurpose);
                    //get the probabilities from tripGeneration (domestic trips) and adapt them to the number of trips by accessibility to US
                    //a calibration factor of 0.20 increases in a 20% the probability of travelling at the zone TO BE CALIBRATED
                    double calibrationFactor = 2;
                    probabilityMatrix[i][j][p] = pers.travelProbabilities[i][j]*(1+pers.getHousehold().getZone().getAccessibility()/100*calibrationFactor);
                    sumProbabilities[i][j] += probabilityMatrix[i][j][p];
                    }
                }
        p++;
        }
        logger.info("Int Trip: sum of probabilities done");
        for (String tripPurpose : tripPurposes) {
            for (String tripState : tripStates) {
                int tripCount = 0;
                // added more places to get more chances of assigning all trips, if not, there are some individuals that are travelling already
                int numberOfTrips = (int)(internationalTripRates.getIndexedValueAt(tripStates.indexOf(tripState), tripPurpose)*personIdMatrix.length);
                double[] randomChoice = new double[(int)(numberOfTrips*1.2) ];
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
                logger.info(tripCount + " international trips generated in Ontario, with purpose " + tripPurpose + " and state " + tripState);
            }
        }
        return trips;
    }
    private LongDistanceTrip createIntLongDistanceTrip(Person pers, String tripPurpose, String tripState, int tripCount, TableDataSet travelPartyProbabilities ){
        ArrayList<Person> adultsHhTravelParty = tripGeneration.addAdultsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = tripGeneration.addKidsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(adultsHhTravelParty);
        int nonHhTravelPartySize = tripGeneration.addNonHhTravelPartySize(tripPurpose, travelPartyProbabilities);

        return new LongDistanceTrip(pers.getPersonId(), true, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), pers.getHousehold().getZone(),
                0, adultsHhTravelParty.size(), kidsHhTravelParty.size(), nonHhTravelPartySize);



    }
}
package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;


/**
 * Created by Carlos on 7/19/2016.
 * Based on number of trips and increased later with travel parties.
 */
public class InternationalTripGeneration {

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    static final List<String> tripStates = MtoLongDistData.getTripStates();
    static final List<String> tripPurposes = MtoLongDistData.getTripPurposes();
    private ResourceBundle rb;
    private double[][] sumProbabilities;
    private int[] personIds;
    private double[][][] probabilityMatrix;
    private SyntheticPopulation synPop;
    private TableDataSet travelPartyProbabilities;
    private TableDataSet internationalTripRates;
    private TableDataSet tripGenerationCoefficients;

    public InternationalTripGeneration(ResourceBundle rb, SyntheticPopulation synPop) {
        this.synPop = synPop;
        this.rb = rb;

        String internationalTriprates = rb.getString("int.trips");
        internationalTripRates = Util.readCSVfile(internationalTriprates);
        internationalTripRates.buildIndex(internationalTripRates.getColumnPosition("tripState"));

        String intTravelPartyProbabilitiesFilename = rb.getString("int.parties");;
        travelPartyProbabilities = Util.readCSVfile(intTravelPartyProbabilitiesFilename);
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));

        String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
        tripGenerationCoefficients = Util.readCSVfile(tripGenCoefficientsFilename);
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));
    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runInternationalTripGeneration() {

        ArrayList<LongDistanceTrip> trips = new ArrayList<>();



        //initialize probMatrices
        sumProbabilities = new double[tripPurposes.size()][tripStates.size()];
        personIds = new int[synPop.getPersons().size()];
        probabilityMatrix = new double[tripPurposes.size()][tripStates.size()][synPop.getPersons().size()];

        //copy the probabilities into arrays
        IntStream.range(0, synPop.getPersons().size()).parallel().forEach(p -> {
            Person pers = synPop.getPersonFromId(p);
            personIds[p] = pers.getPersonId();
            if (pers.getTravelProbabilities() != null) {
                for (String tripPurpose : tripPurposes) {
                    for (String tripState : tripStates) {
                        int j = tripStates.indexOf(tripState);
                        int i = tripPurposes.indexOf(tripPurpose);
                        //todo this change in probability due to accessibility is discontinued
                        //get the probabilities from tripGeneration (domestic trips) and adapt them to the number of trips by accessibility to US
                        //a calibration factor of 0.20 increases in a 20% the probability of travelling at the zone TO BE CALIBRATED
                        //double calibrationFactor = 2;
                        //probabilityMatrix[i][j][p] = pers.getTravelProbabilities()[i][j] * (1 + pers.getHousehold().getZone().getAccessibility() / 100 * calibrationFactor);
                        if (pers.isAway() || pers.isDaytrip() || pers.isInOutTrip() || pers.getAge() < 18 ){
                            probabilityMatrix[i][j][p] = 0f;
                            //cannot be an adult travelling
                        } else {
                            probabilityMatrix[i][j][p] = pers.getTravelProbabilities()[i][j];
                        }
                        sumProbabilities[i][j] += probabilityMatrix[i][j][p];
                    }
                }
            }
        });

        logger.info("sum of probabilities done");


        for (String tripPurpose : tripPurposes) {
            for (String tripState : tripStates) {
                int tripCount = 0;

                int numberOfTrips = (int)(internationalTripRates.getIndexedValueAt(tripStates.indexOf(tripState), tripPurpose)*personIds.length);



                for (int iteration = 0; iteration < 5; iteration++){
                    int n = numberOfTrips - tripCount;
                    //double[] randomChoice = new double[(int)(numberOfTrips*1.1) ];
                    double[] randomChoice = new double[n];
                    for (int k = 0; k < randomChoice.length; k++) {
                        randomChoice[k] = Math.random();
                    }
                    //sort the matrix for faster lookup
                    Arrays.sort(randomChoice);
                    //look up for the n travellers
                    int p = 0;
                    double cumulative = probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p];
                    for (double randomNumber : randomChoice){
                        while (randomNumber > cumulative && p < personIds.length - 1) {
                            p++;
                            cumulative += probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p] /
                                    sumProbabilities[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)];
                        }

                        Person pers = synPop.getPersonFromId(personIds[p]);
                        if (!pers.isDaytrip() && !pers.isAway() && !pers.isInOutTrip() && pers.getAge() > 17 && tripCount < numberOfTrips) {
                            LongDistanceTrip trip = createIntLongDistanceTrip(pers, tripPurpose,tripState, tripCount, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        }
                    }
                    if (numberOfTrips - tripCount == 0){
                        //logger.info("Number of iterations: " + iteration);
                        break;
                    }
                }

                logger.info(tripCount + " international trips generated in Ontario, with purpose " + tripPurpose + " and state " + tripState);
            }
        }
        return trips;
    }


    private LongDistanceTrip createIntLongDistanceTrip(Person pers, String tripPurpose, String tripState, int tripCount, TableDataSet travelPartyProbabilities ){

        switch (tripState) {
            case "away" :
                pers.setAway(true);
            case "daytrip":
                pers.setDaytrip(true);
            case "inout":
                pers.setInOutTrip(true);
        }

        ArrayList<Person> adultsHhTravelParty = DomesticTripGeneration.addAdultsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = DomesticTripGeneration.addKidsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(kidsHhTravelParty);
        int nonHhTravelPartySize = DomesticTripGeneration.addNonHhTravelPartySize(tripPurpose, travelPartyProbabilities);

        return new LongDistanceTrip(pers, true, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), pers.getHousehold().getZone(), true,
                0, adultsHhTravelParty.size(), kidsHhTravelParty.size(), nonHhTravelPartySize);

        //TODO assign nights!!!

    }


    /*public int[] selectTravelers(String tripState, String tripPurpose, int numberOfTrips){
        double sumProb = sumProbabilities[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)];
        double[] probabilities = probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)];

        double[] randomChoice = new double[(int)(numberOfTrips*1.2) ];
        for (int k = 0; k < randomChoice.length; k++) {
            randomChoice[k] = Math.random();
        }
        for (double randomNumber : randomChoice){
            while (randomNumber > cumulative && p < personIds.length - 1) {
                p++;
                cumulative += probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p] / sumProbabilities[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)];
            }

            Person pers = syntheticPopulation.getPersonFromId(personIds[p]);
            if (!pers.isDaytrip() && !pers.isAway() && !pers.isInOutTrip() && pers.getAge() > 17 && tripCount < numberOfTrips) {
                switch (tripState) {
                    case "away" :
                        pers.setAway(true);
                    case "daytrip":
                        pers.setDaytrip(true);
                    case "inout":
                        pers.setInOutTrip(true);
                }
                LongDistanceTrip trip = createIntLongDistanceTrip(pers, tripPurpose,tripState, tripCount, travelPartyProbabilities);
                trips.add(trip);
                tripCount++;
            }
        }


        return new int[5];
    }*/




}

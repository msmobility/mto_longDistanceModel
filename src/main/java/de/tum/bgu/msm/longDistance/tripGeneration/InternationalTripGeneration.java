package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.sp.Person;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;


/**
 * Created by Carlos on 7/19/2016.
 * Based on number of trips and increased later with travel parties.
 */
public class InternationalTripGeneration {

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    static final List<String> tripStates = ZonalData.getTripStates();
    static final List<String> tripPurposes = ZonalData.getTripPurposes();
    //private ResourceBundle rb;
    private JSONObject prop;
    private double[][] sumProbabilities;
    private int[] personIds;
    private double[][][] probabilityMatrix;
    //private SyntheticPopulation synPop;
    private TableDataSet travelPartyProbabilities;
    private TableDataSet internationalTripRates;
    private TableDataSet tripGenerationCoefficients;

    private TableDataSet originCombinedZones;

    private DataSet dataSet;

    public InternationalTripGeneration(JSONObject prop) {
//        this.synPop = synPop;
//        this.rb = rb;
        this.prop = prop;


        //String internationalTriprates = rb.getString("int.trips");
        internationalTripRates = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.international.rates_file"));
        internationalTripRates.buildIndex(internationalTripRates.getColumnPosition("tripState"));

        //String intTravelPartyProbabilitiesFilename = rb.getString("int.parties");;
        travelPartyProbabilities = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.international.party_file"));
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));

//        String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
//        tripGenerationCoefficients = Util.readCSVfile(tripGenCoefficientsFilename);
//        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));

    }


    public void loadInternationalTripGeneration(DataSet dataSet){

        this.dataSet = dataSet;
        //method to calculate the accessibility to US as a measure of the probability of starting and international trip
//        List<String> fromZones;
//        List<String> toZones;
//        fromZones = Arrays.asList("ONTARIO");
//        float alpha = (float) ResourceUtil.getDoubleProperty(rb, "int.access.alpha");
//        float beta = (float) ResourceUtil.getDoubleProperty(rb, "int.access.beta");
//        toZones = Arrays.asList("EXTUS");
//        zonalData.calculateAccessibility(zonalData.getZoneList(), fromZones, toZones, alpha , beta);

        originCombinedZones = dataSet.getDcIntOutbound().getOrigCombinedZones();


    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runInternationalTripGeneration() {



        ArrayList<LongDistanceTrip> trips = new ArrayList<>();

        //initialize probMatrices
        sumProbabilities = new double[tripPurposes.size()][tripStates.size()];
        personIds = new int[dataSet.getPersons().size()];
        probabilityMatrix = new double[tripPurposes.size()][tripStates.size()][dataSet.getPersons().size()];

        //normalize p(travel) per purpose/state by sum of the probability for each person
        sumProbs();

        //run trip generation
        for (String tripPurpose : tripPurposes) {
            for (String tripState : tripStates) {
                int tripCount = 0;
                //get the total number of trips to generate
                int numberOfTrips = (int)(internationalTripRates.getIndexedValueAt(tripStates.indexOf(tripState), tripPurpose)*personIds.length);
                //select the travellers - repeat more than once because the two random numbers can be in the interval of 1 person
                for (int iteration = 0; iteration < 5; iteration++){
                    int n = numberOfTrips - tripCount;
                    double[] randomChoice = new double[n];
                    for (int k = 0; k < randomChoice.length; k++) {
                        randomChoice[k] = LDModel.rand.nextDouble()*sumProbabilities[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)];;
                    }
                    //sort the matrix for faster lookup
                    Arrays.sort(randomChoice);
                    //look up for the n travellers
                    int p = 0;
                    double cumulative = probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p];

                    for (double randomNumber : randomChoice){
                        while (randomNumber > cumulative && p < personIds.length - 1) {
                            p++;
                            cumulative += probabilityMatrix[tripPurposes.indexOf(tripPurpose)][tripStates.indexOf(tripState)][p];
                        }
                        Person pers = dataSet.getPersonFromId(personIds[p]);
                        if (!pers.isDaytrip() && !pers.isAway() && !pers.isInOutTrip() && pers.getAge() > 17 && tripCount < numberOfTrips) {

                            LongDistanceTrip trip = createIntLongDistanceTrip(pers, tripPurpose,tripState, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        }
                    }
                    if (numberOfTrips - tripCount == 0){
                        //logger.info("Number of iterations: " + iteration);
                        break;
                    }
                }
                //logger.info(tripCount + " international trips generated in Ontario, with purpose " + tripPurpose + " and state " + tripState);
            }
        }
        return trips;
    }


    private LongDistanceTrip createIntLongDistanceTrip(Person pers, String tripPurpose, String tripState, TableDataSet travelPartyProbabilities ){

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

        int tripDuration;
        if (pers.isDaytrip()) tripDuration = 0;
        else {
            tripDuration = 1;
        }

        return new LongDistanceTrip(pers, true, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), pers.getHousehold().getZone(), true,
                tripDuration, adultsHhTravelParty.size(), kidsHhTravelParty.size(), nonHhTravelPartySize);

    }


    public void sumProbs(){
        List <Person> persons = new ArrayList<>(dataSet.getPersons().values());

        //make random list of persons
        Collections.shuffle(persons, LDModel.rand);
        double exponent = 2;

        int p = 0;
        for (Person pers : persons) {
            //IntStream.range(0, synPop.getPersons().size()).parallel().forEach(p -> {
            //Person pers = synPop.getPersonFromId(p);
            personIds[p] = pers.getPersonId();
            if (pers.getTravelProbabilities() != null) {
                for (String tripPurpose : tripPurposes) {
                    for (String tripState : tripStates) {
                        int j = tripStates.indexOf(tripState);
                        int i = tripPurposes.indexOf(tripPurpose);
                        if (pers.isAway() || pers.isDaytrip() || pers.isInOutTrip() || pers.getAge() < 18) {
                            probabilityMatrix[i][j][p] = 0f;
                            //cannot be an adult travelling
                        } else {
                            //probabilityMatrix[i][j][p] = pers.getTravelProbabilities()[i][j];
                            //correct here the probability by the accessibility to US - using access at level 2 zone
                            probabilityMatrix[i][j][p] = pers.getTravelProbabilities()[i][j] * Math.pow(originCombinedZones.getIndexedValueAt(pers.getHousehold().getZone().getCombinedZoneId(), "usAccess"),exponent);
                        }
                        sumProbabilities[i][j] += probabilityMatrix[i][j][p];
                    }
                }
            }
            p++;

        }

        //});
        //logger.info("  sum of probabilities done for " + p + " persons");

    }

}

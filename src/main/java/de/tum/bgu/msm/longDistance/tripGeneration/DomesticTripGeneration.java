package de.tum.bgu.msm.longDistance.tripGeneration;


import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.accessibilityAnalysis.AccessibilityAnalysis;
import de.tum.bgu.msm.longDistance.sp.Household;
import de.tum.bgu.msm.longDistance.sp.Person;
import de.tum.bgu.msm.longDistance.sp.SyntheticPopulation;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

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

    private List<String> tripPurposes = ZonalData.getTripPurposes();
    private List<String> tripStates = ZonalData.getTripStates();

    private DataSet dataSet;

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    private ResourceBundle rb;
    private JSONObject prop;

    private TableDataSet tripGenerationCoefficients;
    private TableDataSet travelPartyProbabilities;

    //private SyntheticPopulation synPop;

    private float alphaAccess;
    private float betaAccess;



    public DomesticTripGeneration(JSONObject prop) {
        this.rb = rb;
        this.prop = prop;

        //this.synPop = synPop;

        //String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
        tripGenerationCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.domestic.coef_file"));
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));
        tripGenerationCoefficients.buildStringIndex(tripGenerationCoefficients.getColumnPosition("factorName"));

        //String travelPartyProbabilitiesFilename = rb.getString("domestic.parties");

        travelPartyProbabilities = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.domestic.party_file"));
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));


        //alphaAccess = (float) ResourceUtil.getDoubleProperty(rb, "domestic.access.alpha");
        //betaAccess = (float) ResourceUtil.getDoubleProperty(rb, "domestic.access.beta");

        alphaAccess = JsonUtilMto.getFloatProp(prop,"trip_generation.domestic.accessibility.alpha");
        betaAccess = JsonUtilMto.getFloatProp(prop,"trip_generation.domestic.accessibility.beta");

    }


    public void loadTripGeneration(DataSet dataSet){

        this.dataSet = dataSet;

        List<String> fromZones;
        List<String> toZones;
        //accessibility in Canada to Canada Trips - assing short-distance accessibility to zones
        fromZones = Arrays.asList("ONTARIO");
        toZones = Arrays.asList("ONTARIO", "EXTCANADA");
        AccessibilityAnalysis.calculateAccessibility(dataSet, fromZones, toZones, alphaAccess , betaAccess);
    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runTripGeneration() {
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();

        //initialize utility vectors
        double[] expUtilities = new double[4];
        expUtilities[3] = 1; //base case - or do not travel

        //this option may give randomness to the results
        //synPop.getHouseholds().forEach(hhold -> {
          for (Household hhold : dataSet.getHouseholds().values()) {

            //pick and shuffle the members of the household
            ArrayList<Person> membersList = new ArrayList<>(Arrays.asList(hhold.getPersonsOfThisHousehold()));
            Collections.shuffle(membersList, LDModel.rand);

            for (Person pers : membersList) {
                //array to store 3 x 3 trip probabilities for later use in international
                float[][] tgProbabilities = new float[3][3];
                if (!pers.isAway() && !pers.isDaytrip() && !pers.isInOutTrip() && pers.getAge() > 17) {

                    for (String tripPurpose : tripPurposes) {

                        for (String tripState : tripStates) {
                            //expUtilities[tripStates.indexOf(tripState)] = Math.exp(estimateMlogitUtility(personDescription, tripPurpose, tripState, tripGenerationCoefficients));
                            expUtilities[tripStates.indexOf(tripState)] = Math.exp(calculateUtility(pers, tripPurpose, tripState));
                        }

                        double denominator = Arrays.stream(expUtilities).sum();
                        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / denominator).toArray();

                        //store the probability for later int trips
                        for (String tripState : tripStates) {
                            tgProbabilities[tripStates.indexOf(tripState)][tripPurposes.indexOf(tripPurpose)] = (float) probabilities[tripStates.indexOf(tripState)];
                        }
                        //select the trip state
                        double randomNumber1 = LDModel.rand.nextDouble();
                        int tripStateChoice = 3;

                        if (randomNumber1 < probabilities[0]) {
                            tripStateChoice = 0;
                            pers.setAway(true);
                        } else if (randomNumber1 < probabilities[1] + probabilities[0]) {
                            tripStateChoice = 1;
                            pers.setDaytrip(true);
                        } else if (randomNumber1 < probabilities[2] + probabilities[1] + probabilities[0]) {
                            tripStateChoice = 2;
                            pers.setInOutTrip(true);
                        }

                        if (tripStateChoice < 3) {
                            LongDistanceTrip trip = createLongDistanceTrip(pers, tripPurpose, tripStates.get(tripStateChoice), probabilities,  travelPartyProbabilities);
                            trips.add(trip);
                            //tripCount++;
                        }
                    }
                    //assign probabilities to the person
                    pers.setTravelProbabilities(tgProbabilities);
                }


            }

        };
        return trips;
    }

    public double calculateUtility(Person pers, String tripPurpose, String tripState) {

        double accessibility = pers.getHousehold().getZone().getAccessibility();

        int winter = Mto.getWinter()? 1:0;



        //read coefficients
        String coefficientColumn = tripState + "." + tripPurpose;

        double intercept = tripGenerationCoefficients.getStringIndexedValueAt("(intercept)", coefficientColumn);
        double b_young = tripGenerationCoefficients.getStringIndexedValueAt("Young", coefficientColumn);
        double b_retired = tripGenerationCoefficients.getStringIndexedValueAt("Retired", coefficientColumn);
        double b_female = tripGenerationCoefficients.getStringIndexedValueAt("Female", coefficientColumn);
        double b_adultsHh = tripGenerationCoefficients.getStringIndexedValueAt("adultsInHousehold", coefficientColumn);
        double b_kidsHh = tripGenerationCoefficients.getStringIndexedValueAt("kidsInHousehold", coefficientColumn);
        double b_highSchool = tripGenerationCoefficients.getStringIndexedValueAt("HighSchool", coefficientColumn);
        double b_postSecondary = tripGenerationCoefficients.getStringIndexedValueAt("PostSecondary", coefficientColumn);
        double b_university = tripGenerationCoefficients.getStringIndexedValueAt("University", coefficientColumn);
        double b_employed = tripGenerationCoefficients.getStringIndexedValueAt("Employed", coefficientColumn);
        double b_income2 = tripGenerationCoefficients.getStringIndexedValueAt("income2", coefficientColumn);
        double b_income3 = tripGenerationCoefficients.getStringIndexedValueAt("income3", coefficientColumn);
        double b_income4 = tripGenerationCoefficients.getStringIndexedValueAt("income4", coefficientColumn);
        double b_accessibility = tripGenerationCoefficients.getStringIndexedValueAt("accessibility", coefficientColumn);
        double b_winter = tripGenerationCoefficients.getStringIndexedValueAt("winter", coefficientColumn);

        return intercept +
                b_young * Boolean.compare(pers.isYoung(), false) +
                b_retired * Boolean.compare(pers.isRetired(), false) +
                b_female * Boolean.compare(pers.isFemale(), false) +
                b_adultsHh * pers.getAdultsHh() +
                b_kidsHh * pers.getKidsHh() +
                b_highSchool * Boolean.compare(pers.isHighSchool(), false) +
                b_postSecondary * Boolean.compare(pers.isPostSecondary(), false) +
                b_university * Boolean.compare(pers.isUniversity(), false) +
                b_employed * Boolean.compare(pers.isEmployed(), false) +
                b_income2 * Boolean.compare(pers.isIncome2(), false) +
                b_income3 * Boolean.compare(pers.isIncome3(), false) +
                b_income4 * Boolean.compare(pers.isIncome4(), false) +
                b_accessibility * accessibility +
                b_winter * winter;

    }

    //this method is no longer used
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
        if (Mto.getWinter()) {
            personDescription[14] = 1;
        } else {
            personDescription[14] = 0;
        }

        return personDescription;
    }

    //this method is no longer used
    public static double estimateMlogitUtility(float[] personDescription, String tripPurpose, String tripState, TableDataSet tripGenerationCoefficients) {
        double utility = 0;
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


    private LongDistanceTrip createLongDistanceTrip(Person pers, String tripPurpose, String tripState, double probability[], TableDataSet travelPartyProbabilities) {

        ArrayList<Person> adultsHhTravelParty = addAdultsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = addKidsHhTravelParty(pers, tripPurpose, travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(kidsHhTravelParty);
        int nonHhTravelPartySize = addNonHhTravelPartySize(tripPurpose, travelPartyProbabilities);
        int tripDuration;
        if (pers.isDaytrip()) tripDuration = 0;
        else {
            tripDuration = estimateSimpleTripDuration(tripState);
        }
        return new LongDistanceTrip(pers, false, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState),
                pers.getHousehold().getZone(), true, tripDuration, adultsHhTravelParty.size(), kidsHhTravelParty.size(), nonHhTravelPartySize);

    }

    public static int estimateTripDuration(double[] probability) {
        int tripDuration = 1;
        double randomChoice4 = LDModel.rand.nextDouble();
        while (tripDuration < 30 && randomChoice4 < probability[0] / (probability[0] + probability[2])) {
            randomChoice4 = LDModel.rand.nextDouble();
            tripDuration++;
        }
        return tripDuration;
    }

    public static int estimateSimpleTripDuration(String tripState) {
        int tripDuration = tripState == "daytrip"? 0 : 1;

        return tripDuration;
    }


    public static ArrayList<Person> addAdultsHhTravelParty(Person pers, String tripPurpose, TableDataSet travelPartyProbabilities) {

        ArrayList<Person> hhTravelParty = new ArrayList<>();
        int hhmember = 0;
        hhTravelParty.add(0, pers);
        double randomChoice2 = LDModel.rand.nextDouble();
        Household hhold = pers.getHousehold();
        for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
            if (pers2 != pers && !pers2.isAway() && !pers2.isDaytrip() && !pers2.isInOutTrip() && pers2.getAge() > 17) {
                String column = tripPurpose + "." + Math.min(pers.getAdultsHh(), 5);
                double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                if (randomChoice2 < probability2) {
                    if (pers.isAway()) pers2.setAway(true);
                    else if (pers.isDaytrip()) pers2.setDaytrip(true);
                    else if (pers.isInOutTrip()) pers2.setInOutTrip(true);
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
        double randomChoice2 = LDModel.rand.nextDouble();
        Household hhold = pers.getHousehold();
        for (Person pers2 : hhold.getPersonsOfThisHousehold()) {
            if (pers2 != pers && !pers2.isAway() && !pers2.isDaytrip() && !pers2.isInOutTrip() && pers2.getAge() < 18) {
                String column = "kids." + tripPurpose + "." + Math.min(pers.getKidsHh(), 5);
                double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                if (randomChoice2 < probability2) {
                    if (pers.isAway()) pers2.setAway(true);
                    else if (pers.isDaytrip()) pers2.setDaytrip(true);
                    else if (pers.isInOutTrip()) pers2.setInOutTrip(true);
                    hhTravelParty.add(hhmember, pers2);
                    hhmember++;
                }
            }
        }
        return hhTravelParty;
    }

    public static int addNonHhTravelPartySize(String tripPurpose, TableDataSet travelPartyProbabilities) {
        // methods selects party size for travel groups that are composed of non-household members
        // note that additional travelers on this trip are not specified in the synthetic population (simplified approach)
        double randomChoice3 = LDModel.rand.nextDouble();
        int k = 0;
        String column = tripPurpose + ".nonHh";
        while (randomChoice3 < travelPartyProbabilities.getIndexedValueAt(k + 1, column) && k < 10)
            k++;
        return k;
    }


}






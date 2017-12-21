package de.tum.bgu.msm.longDistance.modeChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.sp.Person;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

import java.util.ResourceBundle;

/**
 * Created by carlloga on 15.03.2017.
 */
public class DomesticModeChoice {
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);

    ResourceBundle rb;

    private int[] modes = {0, 1, 2, 3};
    private String[] modeNames = {"auto", "air", "rail", "bus"};
    // 0 is auto, 1 is plane, 2 is train, 3 is rail

    //the arrays of matrices are stored in the order of modes
    String travelTimeFileName;
    String priceFileName;
    String transfersFileName;
    String freqFileName;
    String lookUpName;

    private Matrix[] travelTimeMatrix = new Matrix[4];
    private Matrix[] priceMatrix = new Matrix[4];
    private Matrix[] transferMatrix = new Matrix[4];
    private Matrix[] frequencyMatrix = new Matrix[4];


    private TableDataSet mcOntarioCoefficients;
    private TableDataSet mcExtCanadaCoefficients;
    private TableDataSet combinedZones;
    private String[] tripPurposeArray;
    private String[] tripStateArray;

    private boolean calibration;
    private double[][] calibrationMatrix;
    private double[][] calibrationMatrixVisitors;


    public DomesticModeChoice(JSONObject prop) {
        this.rb = rb;

        //mcOntarioCoefficients = Util.readCSVfile(rb.getString("mc.domestic.coefs"));
        mcOntarioCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.domestic.ontarian.coef_file"));

        mcOntarioCoefficients.buildStringIndex(1);

        //mcExtCanadaCoefficients = Util.readCSVfile(rb.getString("mc.extcanada.coefs"));
        mcExtCanadaCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.domestic.other_canadian.coef_file"));
        mcExtCanadaCoefficients.buildStringIndex(1);

        //taken from destination choice
        //combinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        combinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.alternatives_file"));
        combinedZones.buildIndex(1);

        //matrix names
//        travelTimeFileName = rb.getString("travel.time.combined");
//        priceFileName = rb.getString("price.combined");
//        transfersFileName = rb.getString("transfer.combined");
//        freqFileName = rb.getString("freq.combined");
//        lookUpName = rb.getString("skim.mode.choice.lookup");

        travelTimeFileName = JsonUtilMto.getStringProp(prop,"mode_choice.skim.time_file");
        priceFileName = JsonUtilMto.getStringProp(prop,"mode_choice.skim.price_file");
        transfersFileName = JsonUtilMto.getStringProp(prop,"mode_choice.skim.transfer_file");
        freqFileName = JsonUtilMto.getStringProp(prop,"mode_choice.skim.frequency_file");
        lookUpName = JsonUtilMto.getStringProp(prop,"mode_choice.skim.lookup");


        calibration = JsonUtilMto.getBooleanProp(prop,"mode_choice.calibration");


        logger.info("Domestic MC set up");

    }


    public void loadDomesticModeChoice(DataSet dataSet){

        tripPurposeArray = dataSet.tripPurposes.toArray(new String[dataSet.tripPurposes.size()]);
        tripStateArray = dataSet.tripStates.toArray(new String[dataSet.tripStates.size()]);

        calibrationMatrix = new double[tripPurposeArray.length][modes.length];
        calibrationMatrixVisitors = new double[tripPurposeArray.length][modes.length];

        readSkimByMode(rb);
        logger.info("Domestic MC loaded");
    }




    public void readSkimByMode(ResourceBundle rb) {
        // read skim file



        for (int m : modes) {

            String matrixName = modeNames[m];

            OmxFile skim = new OmxFile(travelTimeFileName);
            skim.openReadOnly();
            OmxMatrix omxMatrix = skim.getMatrix(matrixName);
            travelTimeMatrix[m] = Util.convertOmxToMatrix(omxMatrix);
            OmxLookup omxLookUp = skim.getLookup(lookUpName);
            int[] externalNumbers = (int[]) omxLookUp.getLookup();
            travelTimeMatrix[m].setExternalNumbersZeroBased(externalNumbers);

            skim = new OmxFile(priceFileName);
            skim.openReadOnly();
            omxMatrix = skim.getMatrix(matrixName);
            priceMatrix[m] = Util.convertOmxToMatrix(omxMatrix);
            priceMatrix[m].setExternalNumbersZeroBased(externalNumbers);

            skim = new OmxFile(transfersFileName);
            skim.openReadOnly();
            omxMatrix = skim.getMatrix(matrixName);
            transferMatrix[m] = Util.convertOmxToMatrix(omxMatrix);
            transferMatrix[m].setExternalNumbersZeroBased(externalNumbers);

            skim = new OmxFile(freqFileName);
            skim.openReadOnly();
            omxMatrix = skim.getMatrix(matrixName);
            frequencyMatrix[m] = Util.convertOmxToMatrix(omxMatrix);
            frequencyMatrix[m].setExternalNumbersZeroBased(externalNumbers);

        }

        logger.info("  skims files for mode choice read");
    }

    public int selectModeDomestic(LongDistanceTrip trip) {

        double[] expUtilities;
        if (trip.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
            //calculate exp(Ui) for each destination
            expUtilities = Arrays.stream(modes).mapToDouble(m -> Math.exp(calculateUtilityFromOntario(trip, m, trip.getDestCombinedZoneId()))).toArray();
        } else {
            //calculate exp(Ui) for each destination
            expUtilities = Arrays.stream(modes).mapToDouble(m -> Math.exp(calculateUtilityFromExtCanada(trip, m, trip.getDestCombinedZoneId()))).toArray();
        }
        double probability_denominator = Arrays.stream(expUtilities).sum();

        //if there is no access by any mode for the selected OD pair, just go by car
        if (probability_denominator == 0) {
            expUtilities[0] = 1;
        }

        //choose one destination, weighted at random by the probabilities
        return Util.select(expUtilities, modes);
        //return new EnumeratedIntegerDistribution(modes, expUtilities).sample();

    }


    public double calculateUtilityFromExtCanada(LongDistanceTrip trip, int m, int destination) {

        double utility;
        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];
        String column = modeNames[m] + "." + tripPurpose;
        String tripState = tripStateArray[trip.getTripState()];

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();

        double interMetro = combinedZones.getIndexedValueAt(origin, "alt_is_metro")
                * combinedZones.getIndexedValueAt(destination, "alt_is_metro");
        double ruralRural = 0;
        if (combinedZones.getIndexedValueAt(origin, "alt_is_metro") == 0 && combinedZones.getIndexedValueAt(destination, "alt_is_metro") == 0) {
            ruralRural = 1;
        }

        double time = travelTimeMatrix[m].getValueAt(origin, destination);
        double price = priceMatrix[m].getValueAt(origin, destination);
        double frequency = frequencyMatrix[m].getValueAt(origin, destination);

        double vot = mcExtCanadaCoefficients.getStringIndexedValueAt("vot", column);

        double impedance = 0;
        if (vot != 0) {
            impedance = price / (vot / 60) + time;
        }


        //todo solve intrazonal times
        if (origin == destination) {
            if (m == 0) {
                time = 60;
                price = 20;
            }
        }

        double k_calibration = mcExtCanadaCoefficients.getStringIndexedValueAt("k_calibration", column);
        double b_intercept = mcExtCanadaCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency = mcExtCanadaCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price = mcExtCanadaCoefficients.getStringIndexedValueAt("price", column);
        double b_time = mcExtCanadaCoefficients.getStringIndexedValueAt("time", column);

        double b_interMetro = mcExtCanadaCoefficients.getStringIndexedValueAt("inter_metro", column);
        double b_ruralRural = mcExtCanadaCoefficients.getStringIndexedValueAt("rural_rural", column);

        double b_overnight = mcExtCanadaCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party = mcExtCanadaCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance = mcExtCanadaCoefficients.getStringIndexedValueAt("impedance", column);
        double alpha_impedance = mcExtCanadaCoefficients.getStringIndexedValueAt("alpha", column);


        if (calibration) k_calibration = calibrationMatrixVisitors[trip.getTripPurpose()][m];

        utility = b_intercept + b_frequency * frequency +
                k_calibration +
                b_price * price +
                b_time * time +
//                b_incomeLow * incomeLow +
//                b_incomeHigh * incomeHigh +
//                b_young * young +
                b_interMetro * interMetro +
                b_ruralRural * ruralRural +
//                b_male * male +
//                b_educationUniv * educationUniv +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(alpha_impedance * impedance);


        if (time < 0) utility = Double.NEGATIVE_INFINITY;


        return utility;

    }


    public double calculateUtilityFromOntario(LongDistanceTrip trip, int m, int destination) {


        double utility;
        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];
        String column = modeNames[m] + "." + tripPurpose;
        String tripState = tripStateArray[trip.getTripState()];

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();
        //int destination = trip.getDestCombinedZoneId();

        //zone-related variables

        double interMetro = combinedZones.getIndexedValueAt(origin, "alt_is_metro")
                * combinedZones.getIndexedValueAt(destination, "alt_is_metro");
        double ruralRural = 0;
        if (combinedZones.getIndexedValueAt(origin, "alt_is_metro") == 0 && combinedZones.getIndexedValueAt(destination, "alt_is_metro") == 0) {
            ruralRural = 1;
        }


        double time = travelTimeMatrix[m].getValueAt(origin, destination);
        double price = priceMatrix[m].getValueAt(origin, destination);
        double frequency = frequencyMatrix[m].getValueAt(origin, destination);

        double vot = mcOntarioCoefficients.getStringIndexedValueAt("vot", column);

        double impedance = 0;
        if (vot != 0) {
            impedance = price / (vot / 60) + time;
        }


        //todo solve intrazonal times
        if (origin == destination) {
            if (m == 0) {
                time = 60;
                price = 20;
            }
        }

        //person-related variables
        Person p = trip.getTraveller();

        double incomeLow = p.getIncome() <= 50000 ? 1 : 0;
        double incomeHigh = p.getIncome() >= 100000 ? 1 : 0;

        double young = p.getAge() < 25 ? 1 : 0;
        double female = p.getGender() == 'F' ? 1 : 0;

        double educationUniv = p.getEducation() > 5 ? 1 : 0;

        //getCoefficients
        double k_calibration = mcOntarioCoefficients.getStringIndexedValueAt("k_calibration", column);
        double b_intercept = mcOntarioCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency = mcOntarioCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price = mcOntarioCoefficients.getStringIndexedValueAt("price", column);
        double b_time = mcOntarioCoefficients.getStringIndexedValueAt("time", column);
        double b_incomeLow = mcOntarioCoefficients.getStringIndexedValueAt("income_low", column);
        double b_incomeHigh = mcOntarioCoefficients.getStringIndexedValueAt("income_high", column);
        double b_young = mcOntarioCoefficients.getStringIndexedValueAt("young", column);
        double b_interMetro = mcOntarioCoefficients.getStringIndexedValueAt("inter_metro", column);
        double b_ruralRural = mcOntarioCoefficients.getStringIndexedValueAt("rural_rural", column);
        double b_female = mcOntarioCoefficients.getStringIndexedValueAt("female", column);
        double b_educationUniv = mcOntarioCoefficients.getStringIndexedValueAt("education_univ", column);
        double b_overnight = mcOntarioCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party = mcOntarioCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance = mcOntarioCoefficients.getStringIndexedValueAt("impedance", column);
        double alpha_impedance = mcOntarioCoefficients.getStringIndexedValueAt("alpha", column);

        //this updates calibration factor from during-runtime calibration matrix
        if (calibration) k_calibration = calibrationMatrix[trip.getTripPurpose()][m];

        utility = b_intercept + k_calibration +
                b_frequency * frequency +
                b_price * price +
                b_time * time +
                b_incomeLow * incomeLow +
                b_incomeHigh * incomeHigh +
                b_young * young +
                b_interMetro * interMetro +
                b_ruralRural * ruralRural +
                b_female * female +
                b_educationUniv * educationUniv +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(alpha_impedance * impedance);


        if (time < 0) utility = Double.NEGATIVE_INFINITY;

        return utility;

    }

    public int[] getModes() {
        return modes;
    }

    public Matrix[] getTravelTimeMatrix() {
        return travelTimeMatrix;
    }

    public Matrix[] getPriceMatrix() {
        return priceMatrix;
    }

    public Matrix[] getTransferMatrix() {
        return transferMatrix;
    }

    public Matrix[] getFrequencyMatrix() {
        return frequencyMatrix;
    }

    public void updateCalibrationDomestic(double[][] calibrationMatrix) {
        for (int purp = 0; purp < tripPurposeArray.length; purp++) {
            for (int mode = 0; mode < modes.length; mode++) {
                this.calibrationMatrix[purp][mode] += calibrationMatrix[purp][mode];
            }
        }
    }

    public void updateCalibrationDomesticVisitors(double[][] calibrationMatrix) {
        for (int purp = 0; purp < tripPurposeArray.length; purp++) {
            for (int mode = 0; mode < modes.length; mode++) {
                this.calibrationMatrixVisitors[purp][mode] += calibrationMatrix[purp][mode];
            }
        }
    }

    public double[][] getCalibrationMatrixVisitors() {
        return calibrationMatrixVisitors;
    }

    public double[][] getCalibrationMatrix() {
        return calibrationMatrix;
    }

    public float getDomesticModalTravelTime(LongDistanceTrip trip){
        if (trip.getOrigZone().getZoneType().equals(ZoneType.EXTOVERSEAS) || trip.getDestZoneType().equals(ZoneType.EXTOVERSEAS) ){
            return -1.f;
        } else {
            return travelTimeMatrix[trip.getMode()].getValueAt(trip.getOrigZone().getCombinedZoneId(), trip.getDestCombinedZoneId());
        }
    }

}

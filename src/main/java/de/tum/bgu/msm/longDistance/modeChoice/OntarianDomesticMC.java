package de.tum.bgu.msm.longDistance.modeChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.sp.Person;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import ncsa.hdf.object.Dataset;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 15.03.2017.
 */
public class OntarianDomesticMC implements ModeChoiceModelI{
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);

    ResourceBundle rb;

    // 0 is auto, 1 is plane, 2 is train, 3 is rail


    //the arrays of matrices are stored in the order of modes


    private Matrix[] travelTimeMatrix = new Matrix[4];
    private Matrix[] priceMatrix = new Matrix[4];
    private Matrix[] transferMatrix = new Matrix[4];
    private Matrix[] frequencyMatrix = new Matrix[4];


    private TableDataSet mcOntarioCoefficients;
    private TableDataSet combinedZones;
    private String[] tripPurposeArray;
    private String[] tripStateArray;

    private int[] modes;
    private String[] modeNames;

    private boolean calibration;
    private double[][] calibrationMatrix;
    private double[][] calibrationMatrixVisitors;


    public OntarianDomesticMC(JSONObject prop) {
        this.rb = rb;

        //mcOntarioCoefficients = Util.readCSVfile(rb.getString("mc.domestic.coefs"));
        mcOntarioCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.domestic.ontarian.coef_file"));

        mcOntarioCoefficients.buildStringIndex(1);

        combinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.alternatives_file"));
        combinedZones.buildIndex(1);
        calibration = JsonUtilMto.getBooleanProp(prop,"mode_choice.calibration");

        logger.info("Domestic MC set up");
    }

    @Override
    public void load(DataSet dataSet) {
        tripPurposeArray = dataSet.tripPurposes.toArray(new String[dataSet.tripPurposes.size()]);
        tripStateArray = dataSet.tripStates.toArray(new String[dataSet.tripStates.size()]);
        modes = dataSet.modes;
        modeNames = dataSet.modeNames;

        calibrationMatrix = new double[tripPurposeArray.length][dataSet.modes.length];
        calibrationMatrixVisitors = new double[tripPurposeArray.length][dataSet.modes.length];

        travelTimeMatrix = dataSet.getTravelTimeMatrix();
        priceMatrix = dataSet.getPriceMatrix();
        transferMatrix = dataSet.getTransferMatrix();
        frequencyMatrix = dataSet.getFrequencyMatrix();


        logger.info("Domestic MC loaded");
    }




    @Override
    public int selectMode(LongDistanceTrip trip) {
        double[] expUtilities;

            expUtilities = Arrays.stream(modes).mapToDouble(m -> Math.exp(calculateUtility(trip, m, trip.getDestCombinedZoneId()))).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();

        //if there is no access by any mode for the selected OD pair, just go by car
        if (probability_denominator == 0) {
            expUtilities[0] = 1;
        }

        //choose one destination, weighted at random by the probabilities
        return Util.select(expUtilities, modes);
        //return new EnumeratedIntegerDistribution(modes, expUtilities).sample();
    }

    @Override
    public double calculateUtility(LongDistanceTrip trip, int m, int destination) {
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

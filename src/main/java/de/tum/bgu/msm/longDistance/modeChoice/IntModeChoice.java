package de.tum.bgu.msm.longDistance.modeChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/26/2017.
 */
public class IntModeChoice {

    private static Logger logger = Logger.getLogger(IntModeChoice.class);

    ResourceBundle rb;

    private int[] modes = {0, 1, 2, 3};
    private String[] modeNames = {"auto", "air", "rail", "bus"};
    // 0 is auto, 1 is plane, 2 is train, 3 is rail

    //the arrays of matrices are stored in the order of modes
    private Matrix[] travelTimeMatrix = new Matrix[4];
    private Matrix[] priceMatrix = new Matrix[4];
    private Matrix[] transferMatrix = new Matrix[4];
    private Matrix[] frequencyMatrix = new Matrix[4];

    String[] tripPurposeArray;
    String[] tripStateArray;

    private TableDataSet mcIntOutboundCoefficients;
    private TableDataSet mcIntInboundCoefficients;

    private boolean calibration;
    private double[][] calibrationMatrixOutbound;
    private double[][] calibrationMatrixInbound;

    private DomesticModeChoice dmChoice;




    public IntModeChoice(JSONObject prop) {
        //this.rb = rb;

       // mcIntOutboundCoefficients = Util.readCSVfile(rb.getString("mc.int.outbound.coefs"));
        mcIntOutboundCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.international.outbound.coef_file"));
        mcIntOutboundCoefficients.buildStringIndex(1);

        mcIntInboundCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.international.outbound.coef_file"));
        mcIntInboundCoefficients.buildStringIndex(1);


        calibration = JsonUtilMto.getBooleanProp(prop,"mode_choice.calibration");


        logger.info("International MC set up");

    }


    public void loadIntModeChoice(DataSet dataSet){
        this.dmChoice = dataSet.getMcDomestic();

        tripPurposeArray = dataSet.tripPurposes.toArray(new String[dataSet.tripPurposes.size()]);
        tripStateArray = dataSet.tripStates.toArray(new String[dataSet.tripStates.size()]);

        calibrationMatrixOutbound = new double[tripPurposeArray.length][modes.length];
        calibrationMatrixInbound = new double[tripPurposeArray.length][modes.length];

        travelTimeMatrix = dmChoice.getTravelTimeMatrix();
        priceMatrix = dmChoice.getPriceMatrix();
        transferMatrix = dmChoice.getTransferMatrix();
        frequencyMatrix = dmChoice.getFrequencyMatrix();

        logger.info("International MC loaded");
    }

    public int selectMode(LongDistanceTrip trip){

        double[] expUtilities;

        if(trip.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || trip.getOrigZone().getZoneType().equals(ZoneType.EXTCANADA)){
            expUtilities = Arrays.stream(modes)
                    //calculate exp(Ui) for each destination
                    .mapToDouble(m -> Math.exp(calculateUtilityFromCanada(trip, m, trip.getDestCombinedZoneId()))).toArray();
        } else {
            expUtilities = Arrays.stream(modes)
                    //calculate exp(Ui) for each destination
                    .mapToDouble(m -> Math.exp(calculateUtilityToCanada(trip, m, trip.getDestCombinedZoneId()))).toArray();
        }


        //choose one destination, weighted at random by the probabilities
        //return new EnumeratedIntegerDistribution(modes, expUtilities).sample();
        return Util.select(expUtilities, modes);


    }

    public double calculateUtilityToCanada(LongDistanceTrip trip, int m, int destination) {

        double utility;
        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];
        String column = modeNames[m] + "." + tripPurpose;
        String tripState = tripStateArray[trip.getTripState()];

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")){
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();



        //zone-related variables

        double time = travelTimeMatrix[m].getValueAt(origin, destination);
        double price = priceMatrix[m].getValueAt(origin, destination);
        double frequency = frequencyMatrix[m].getValueAt(origin, destination);

        double vot= mcIntInboundCoefficients.getStringIndexedValueAt("vot", column);


        double impedance = 0;
        if (vot != 0){
            impedance = price/(vot/60) + time;
        }


        //todo solve intrazonal times
        if (origin==destination){
            if (m==0) {
                time = 60;
                price = 20;
            }
        }

        //getCoefficients
        double b_intercept = mcIntInboundCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency= mcIntInboundCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price= mcIntInboundCoefficients.getStringIndexedValueAt("price", column);
        double b_time= mcIntInboundCoefficients.getStringIndexedValueAt("time", column);
        double b_overnight= mcIntInboundCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party= mcIntInboundCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance= mcIntInboundCoefficients.getStringIndexedValueAt("impedance", column);
        double beta_time = mcIntInboundCoefficients.getStringIndexedValueAt("beta_time", column);
        double k_calibration = mcIntInboundCoefficients.getStringIndexedValueAt("k_calibration", column);

        //calibration factor update during runtime
        if (calibration) {
            k_calibration = calibrationMatrixInbound[trip.getTripPurpose()][m];
        }


                utility = b_intercept + k_calibration +
                        b_frequency*frequency +
                b_price * price +
                b_time * time +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(beta_time*impedance);


        if (time < 0 ) utility = Double.NEGATIVE_INFINITY;


        return utility;
    }

    public double calculateUtilityFromCanada(LongDistanceTrip trip, int m, int destination){

        double utility;
        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];
        String column = modeNames[m] + "." + tripPurpose;
        String tripState = tripStateArray[trip.getTripState()];

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")){
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();


        //zone-related variables

        double time = travelTimeMatrix[m].getValueAt(origin, destination);
        double price = priceMatrix[m].getValueAt(origin, destination);
        double frequency = frequencyMatrix[m].getValueAt(origin, destination);

        double vot= mcIntOutboundCoefficients.getStringIndexedValueAt("vot", column);

        double impedance = 0;
        if (vot != 0){
            impedance = price/(vot/60) + time;
        }


        //todo solve intrazonal times
        if (origin==destination){
            if (m==0) {
                time = 60;
                price = 20;
            }
        }

        //getCoefficients
        double b_intercept = mcIntOutboundCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency= mcIntOutboundCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price= mcIntOutboundCoefficients.getStringIndexedValueAt("price", column);
        double b_time= mcIntOutboundCoefficients.getStringIndexedValueAt("time", column);
        double b_overnight= mcIntOutboundCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party= mcIntOutboundCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance= mcIntOutboundCoefficients.getStringIndexedValueAt("impedance", column);
        double beta_time = mcIntOutboundCoefficients.getStringIndexedValueAt("beta_time", column);

        double k_calibration = mcIntOutboundCoefficients.getStringIndexedValueAt("k_calibration", column);

        //calibration during runtime
        if (calibration) {
            k_calibration = calibrationMatrixOutbound[trip.getTripPurpose()][m];
        }

        utility = b_intercept + k_calibration +
                b_frequency*frequency +
                b_price * price +
                b_time * time +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(beta_time*impedance);


        if (time < 0 ) utility = Double.NEGATIVE_INFINITY;


        return utility;

    }

    public int[] getModes() {
        return modes;
    }

    public void updateCalibrationOutbound(double[][] calibrationMatrix) {
        for (int purp = 0; purp < tripPurposeArray.length; purp++) {
            for (int mode = 0; mode < modes.length; mode++) {
                this.calibrationMatrixOutbound[purp][mode] += calibrationMatrix[purp][mode];
            }
        }
    }

    public void updateCalibrationInbound(double[][] calibrationMatrix) {
        for (int purp = 0; purp < tripPurposeArray.length; purp++) {
            for (int mode = 0; mode < modes.length; mode++) {
                this.calibrationMatrixInbound[purp][mode] += calibrationMatrix[purp][mode];
            }
        }
    }

    public double[][] getCalibrationMatrixOutbound() {
        return calibrationMatrixOutbound;
    }

    public double[][] getCalibrationMatrixInbound() {
        return calibrationMatrixInbound;
    }

    public float getInternationalModalTravelTime(LongDistanceTrip trip){
        if (trip.getOrigZone().getZoneType().equals(ZoneType.EXTOVERSEAS) || trip.getDestZoneType().equals(ZoneType.EXTOVERSEAS) ){
            return -1.f;
        } else {
            return travelTimeMatrix[trip.getMode()].getValueAt(trip.getOrigZone().getCombinedZoneId(), trip.getDestCombinedZoneId());
        }

    }
}

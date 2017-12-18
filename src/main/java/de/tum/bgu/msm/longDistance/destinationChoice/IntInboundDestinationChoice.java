package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/12/2017.
 */
public class IntInboundDestinationChoice {

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    private TableDataSet destCombinedZones;
    private TableDataSet coefficients;
    private Matrix autoTravelTime;
    private int[] alternatives;
    private String[] tripPurposeArray;
    private String[] tripStateArray;
    private DomesticDestinationChoice dcModel;
    private IntModeChoice intModeChoice;
    private boolean calibration;
    private double[] calibrationV;



    public IntInboundDestinationChoice(JSONObject prop) {
        //coef format
        // table format: coeff | visit | leisure | business
        //this.rb = rb;
        //coefficients = Util.readCSVfile(rb.getString("dc.int.us.in.coefs"));
        coefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.international.inbound.coef_file"));
        coefficients.buildStringIndex(1);


        //load alternatives
        //destCombinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        destCombinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.alternatives_file"));
        destCombinedZones.buildIndex(1);
        alternatives = destCombinedZones.getColumnAsInt("alt");


        //calibration = ResourceUtil.getBooleanProperty(rb,"dc.calibration",false);
        calibration = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration");
        this.calibrationV = new double[] {1,1,1};

        logger.info("International DC (inbound) set up");

    }

    public void loadIntInboundDestinationChoice(DataSet dataSet){

        tripPurposeArray = dataSet.tripPurposes.toArray(new String[dataSet.tripPurposes.size()]);
        tripStateArray = dataSet.tripStates.toArray(new String[dataSet.tripStates.size()]);
        this.dcModel = dataSet.getDcDomestic();
        this.intModeChoice = dataSet.getMcInt();
        //load combined zones distance skim
        autoTravelTime = dcModel.getAutoDist();

        logger.info("International DC (inbound) loaded");

    }



    public int selectDestinationFromUs(LongDistanceTrip trip) {

        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives).mapToDouble(a -> Math.exp(calculateCanZoneUtilityFromUs(trip, tripPurpose, a))).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();

        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        //return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();
        return Util.select(probabilities,alternatives);
    }


    public int selectDestinationFromOs(LongDistanceTrip trip) {

        //String tripPurpose = tripPurposeArray[trip.getTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives).mapToDouble(a -> calculateCanZoneUtilityFromOs(a)).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();


        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        //return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();

        return Util.select(probabilities,alternatives);
    }


    public void readSkim(ResourceBundle rb) {
        // read skim file


        String matrixName = "skim.int.out.file";
        String hwyFileName = rb.getString(matrixName);
        logger.info("  Reading skims file" + hwyFileName);
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.int.out.matrix"));
        autoTravelTime = Util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("skim.int.out.lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoTravelTime.setExternalNumbersZeroBased(externalNumbers);
    }


    public double calculateCanZoneUtilityFromUs(LongDistanceTrip trip, String tripPurpose, int destination) {

//read coefficients
        String tripState = tripStateArray[trip.getTripState()];

        double b_population = coefficients.getStringIndexedValueAt("population", tripPurpose);
        double b_dist = coefficients.getStringIndexedValueAt("b_dist", tripPurpose);
        double alpha_dist = coefficients.getStringIndexedValueAt("alpha_dist", tripPurpose);
        double b_dtLogsum = coefficients.getStringIndexedValueAt("dtLogsum", tripPurpose);
        double b_onLogsum = coefficients.getStringIndexedValueAt("onLogsum", tripPurpose);
        double b_civic = coefficients.getStringIndexedValueAt("civic", tripPurpose);
        double b_log_civic = coefficients.getStringIndexedValueAt("log_civic", tripPurpose);
        double b_skiing = coefficients.getStringIndexedValueAt("skiing", tripPurpose);
        double b_altIsMetro = coefficients.getStringIndexedValueAt("altIsMetro", tripPurpose);
        double b_hotel = coefficients.getStringIndexedValueAt("hotel", tripPurpose);
        double b_log_hotel = coefficients.getStringIndexedValueAt("log_hotel", tripPurpose);
        double b_niagara = coefficients.getStringIndexedValueAt("niagara", tripPurpose);

        double k_dtLogsum = coefficients.getStringIndexedValueAt("k_dtLogsum", tripPurpose);
        double k_onLogsum = coefficients.getStringIndexedValueAt("k_onLogsum", tripPurpose);

        if (calibration) {
            switch (trip.getTripPurpose()) {
                case 2:
                    //tripPurpose = "leisure";
                    k_dtLogsum = calibrationV[2];
                    k_onLogsum = k_dtLogsum;
                    break;
                case 0:
                    //tripPurpose = "visit";
                    k_dtLogsum = calibrationV[0];
                    k_onLogsum = k_dtLogsum;
                    break;
                case 1:
                    //tripPurpose = "business";
                    k_dtLogsum = calibrationV[1];
                    k_onLogsum = k_dtLogsum;
                    break;
            }
        }

        //get the logsum
        double logsum = 0;
        int[] modes = intModeChoice.getModes();
        for (int m : modes) {
            logsum += Math.exp(intModeChoice.calculateUtilityToCanada(trip, m, destination));
        }
        if(logsum ==0){
            return Double.NEGATIVE_INFINITY;
            //deal with trips that logsum == 0 --> means that no mode is available
            //logger.info(trip.getOrigZone().getCombinedZoneId() + " to " + destination);
        } else {
            logsum = Math.log(logsum);
        }


        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        //read trip data
        double dist = autoTravelTime.getValueAt(trip.getOrigZone().getCombinedZoneId(), destination);

        //read destination data
        double population = destCombinedZones.getIndexedValueAt(destination, "population");
        double employment = destCombinedZones.getIndexedValueAt(destination, "employment");

        double civic = Math.log(population + employment);

        double log_civic = civic>0? Math.log(population + employment): 0;


        double hotel = destCombinedZones.getIndexedValueAt(destination, "hotel");
        double log_hotel = hotel>0? Math.log(hotel): 0;
        double skiing = destCombinedZones.getIndexedValueAt(destination, "skiing");
        int altIsMetro = (int) destCombinedZones.getIndexedValueAt(destination, "alt_is_metro");

        int altIsNiagara = destination == 30? 1 : 0;


        //calculate utility
        return b_population * population +
                b_dist * Math.exp(alpha_dist * dist) +
                b_dtLogsum * (1 - overnight) * logsum * k_dtLogsum +
                b_onLogsum * overnight * logsum * k_onLogsum +
                b_civic * civic +
                b_log_civic* log_civic +
                b_skiing * skiing +
                b_altIsMetro * altIsMetro +
                b_hotel * hotel +
                b_log_hotel * log_hotel +
                b_niagara * altIsNiagara;

    }

    public double calculateCanZoneUtilityFromOs(int destination) {

//read coefficients

        return destCombinedZones.getIndexedValueAt(destination, "population");


    }

    public void updateIntInboundCalibrationV(double[] b_calibrationVector) {
        this.calibrationV[0] = this.calibrationV[0]*b_calibrationVector[0];
        this.calibrationV[1] = this.calibrationV[1]*b_calibrationVector[1];
        this.calibrationV[2] = this.calibrationV[2]*b_calibrationVector[2];
    }

    public double[] getCalibrationV() {
        return calibrationV;
    }
}

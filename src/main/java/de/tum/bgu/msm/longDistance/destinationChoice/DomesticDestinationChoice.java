package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created by Joe on 26/10/2016.
 */
public class DomesticDestinationChoice {
    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    public static final int CHOICE_SET_SIZE = 117;
    private TableDataSet combinedZones;
    private TableDataSet coefficients;
    private Matrix autoDist;
    private int[] alternatives;
    String[] tripPurposeArray;
    private DomesticModeChoice domesticModeChoice;
    private boolean calibration;
    private double[] domDcCalibrationV;
    private RandomGenerator rng;

    //private String[] fileMatrixLooup = new String[3];


    public DomesticDestinationChoice(JSONObject prop) {


        //fileMatrixLooup[0] = JsonUtilMto.getStringProp(prop, )
        //fileMatrixLooup[1] = JsonUtilMto.getStringProp(prop, )
        //fileMatrixLooup[2] = JsonUtilMto.getStringProp(prop, )

        //coef format
        // table format: coeff | visit | leisure | business
        //coefficients = Util.readCSVfile(rb.getString("dc.domestic.coefs"));
        coefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.coef_file"));
        coefficients.buildStringIndex(1);


        //load alternatives

        //combinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        combinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.alternatives_file"));
        combinedZones.buildIndex(1);
        alternatives = combinedZones.getColumnAsInt("alt");

        //calibration = ResourceUtil.getBooleanProperty(rb,"dc.calibration",false);
        calibration = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration");
        this.domDcCalibrationV = new double[] {1,1,1};


        logger.info("Domestic DC set up");
        //enum integer distribution does not accept Random but Random Generator
        //rng = RandomGeneratorFactory.createRandomGenerator(LDModel.rand);

    }

    public void loadDomesticDestinationChoice(DataSet dataSet){

        tripPurposeArray = dataSet.tripPurposes.toArray(new String[dataSet.tripPurposes.size()]);
        this.domesticModeChoice = dataSet.getMcDomestic();

        //load combined zones distance skim
        //readSkim();

        autoDist = domesticModeChoice.getTravelTimeMatrix()[0];
        logger.info("Domestic DC loaded");

    }

/*
    public void readSkim() {
        // read skim file



        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.combinedzones.distance"));
        autoDist = Util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("skim.combinedzones.lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoDist.setExternalNumbersZeroBased(externalNumbers);

        logger.info("  Skim matrix was read: " + hwyFileName);

    }
*/



    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTrip trip) {

        //        switch (trip.getTripPurpose()) {
//            case 2:
//                tripPurpose = "leisure";
//                break;
//            case 0:
//                tripPurpose = "visit";
//                break;
//            case 1:
//                tripPurpose = "business";
//                break;
//        }
        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives)
                //calculate exp(Ui) for each destination
                .mapToDouble(a -> Math.exp(calculateUtility(trip, tripPurpose, a))).toArray();
        //calculate the probability for each trip, based on the destination utilities
        double probability_denominator = Arrays.stream(expUtilities).sum();

        //calculate the probability for each trip, based on the destination utilities
        double[] probabilities = Arrays.stream(expUtilities).map(u -> u/probability_denominator).toArray();

        //choose one destination, weighted at random by the probabilities
        return Util.select(probabilities,alternatives);
        //return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();

    }

    public ZoneType getDestinationZoneType(int destinationZoneId){
        //method to give the destination zone type from a destination

        if (combinedZones.getIndexedStringValueAt(destinationZoneId,"loc").equals("ontario")){
            return ZoneType.ONTARIO;
        } else {
            return ZoneType.EXTCANADA;
        }

    }

    private double calculateUtility(LongDistanceTrip trip, String tripPurpose, int destination) {
        // Method to calculate utility of all possible destinations for LongDistanceTrip trip

        int origin = trip.getOrigZone().getCombinedZoneId();
        float distance = autoDist.getValueAt(origin, destination);


        //filters are not applied so all the trips are generated:
        //if (distance < 40 || distance > 1000) return Double.NEGATIVE_INFINITY;
        //if (distance < 40) return Double.NEGATIVE_INFINITY;
        //if (origin == destination && trip.getOrigZone().getZoneType() == ZoneType.EXTCANADA) return Double.NEGATIVE_INFINITY;

//        String origin_east_west = combinedZones.getIndexedStringValueAt(origin,"loc");
  //      String destination_east_west = combinedZones.getIndexedStringValueAt(destination,"loc");
        //if (origin_east_west.equals(destination_east_west) && !"ontario".equals(origin_east_west)) return Double.NEGATIVE_INFINITY;

        double civic = combinedZones.getIndexedValueAt(destination,"population") + combinedZones.getIndexedValueAt(destination,"employment");
        double m_intra = origin == destination ? combinedZones.getIndexedValueAt(origin,"alt_is_metro") : 0;
        double mm_inter = origin != destination ? combinedZones.getIndexedValueAt(origin,"alt_is_metro")
                * combinedZones.getIndexedValueAt(destination,"alt_is_metro") : 0;
        double r_intra = origin == destination ? combinedZones.getIndexedValueAt(origin,"alt_is_metro") : 0;
        double fs_niagara = destination == 30 ? 1 : 0;
        double fs_outdoors = combinedZones.getIndexedValueAt(destination,"outdoors");
        double fs_skiing = combinedZones.getIndexedValueAt(destination,"skiing");
        double fs_medical = combinedZones.getIndexedValueAt(destination,"medical");
        double fs_sightseeing = combinedZones.getIndexedValueAt(destination,"sightseeing");
        double fs_hotel = combinedZones.getIndexedValueAt(destination,"hotel");

        //logsums
        //use non person based mode choice model
        //get the logsum
        double logsum = 0;
        int[] modes = domesticModeChoice.getModes();
        for (int m : modes) {
            logsum += Math.exp(domesticModeChoice.calculateUtilityFromExtCanada(trip, m, destination));
        }
        if(logsum ==0){
            return Double.NEGATIVE_INFINITY;
            //deals with trips that logsum == 0 --> means that no mode is available - no one is travelling there?
        } else {
            logsum = Math.log(logsum);
        }

        double dtLogsum = trip.getTripState()==1? logsum: 0;
        double onLogsum = trip.getTripState()!=1? logsum: 0;

        //Coefficients
        double alpha = coefficients.getStringIndexedValueAt("alpha", tripPurpose);



        double b_calibration_dt = coefficients.getStringIndexedValueAt("b_calibration_dt", tripPurpose);
        double b_calibration_on = coefficients.getStringIndexedValueAt("b_calibration_on", tripPurpose);


        if (calibration) {
            switch (trip.getTripPurpose()) {
                case 2:
                    //tripPurpose = "leisure";
                    b_calibration_dt = domDcCalibrationV[2];
                    b_calibration_on = domDcCalibrationV[2];
                    break;
                case 0:
                    //tripPurpose = "visit";
                    b_calibration_dt = domDcCalibrationV[0];
                    b_calibration_on = domDcCalibrationV[0];
                    break;
                case 1:
                    //tripPurpose = "business";
                    b_calibration_dt = domDcCalibrationV[1];
                    b_calibration_on = domDcCalibrationV[1];
                    break;
            }
        }

        double b_distance_log = coefficients.getStringIndexedValueAt("dist_log", tripPurpose);

        double b_civic = coefficients.getStringIndexedValueAt("log_civic", tripPurpose);

        double b_m_intra = coefficients.getStringIndexedValueAt("intrametro", tripPurpose);
        double b_mm_inter = coefficients.getStringIndexedValueAt("intermetro", tripPurpose);
        double b_r_intra = coefficients.getStringIndexedValueAt("intrarural", tripPurpose);
        double b_niagara = coefficients.getStringIndexedValueAt("niagara", tripPurpose);

        double b_outdoors = coefficients.getStringIndexedValueAt("log_outdoors", tripPurpose);
        double b_skiing = coefficients.getStringIndexedValueAt("log_skiing", tripPurpose);

        double b_medical = coefficients.getStringIndexedValueAt("log_medical", tripPurpose);
        double b_hotel = coefficients.getStringIndexedValueAt("log_hotel", tripPurpose);
        double b_sightseeing = coefficients.getStringIndexedValueAt("log_sightseeing", tripPurpose);

        double b_dtLogsum = coefficients.getStringIndexedValueAt("dtLogsum", tripPurpose);
        double b_onLogsum = coefficients.getStringIndexedValueAt("onLogsum", tripPurpose);


        //log conversions
        double log_distance = distance > 0 ? Math.log(distance) : 0;
        civic = civic > 0 ? Math.log(civic) : 0;
        //fs_niagara = fs_niagara > 0 ? Math.log(fs_niagara) : 0;
        fs_outdoors = fs_outdoors > 0 ? Math.log(fs_outdoors) : 0;
        fs_skiing = fs_skiing > 0 ? Math.log(fs_skiing) : 0;
        fs_medical = fs_medical > 0 ? Math.log(fs_medical) : 0;
        fs_sightseeing = fs_sightseeing > 0 ? Math.log(fs_sightseeing) : 0;
        fs_hotel = fs_hotel > 0 ? Math.log(fs_hotel) : 0;

        double u =
                //b_distance * Math.exp(-alpha * distance)
                 b_distance_log * log_distance
                + b_dtLogsum * dtLogsum * b_calibration_dt
                + b_onLogsum * onLogsum * b_calibration_on
                + b_civic * civic
                + b_mm_inter * mm_inter
                + b_m_intra * m_intra
                + b_r_intra * r_intra
                + b_niagara * fs_niagara
                + b_outdoors * (trip.isSummer() ? 1 : 0)* fs_outdoors
                + b_skiing * (!trip.isSummer() ? 1 : 0) *  fs_skiing
                       // + b_outdoors * fs_outdoors
                       // + b_skiing * fs_skiing
                + b_medical * fs_medical
                + b_hotel * fs_hotel
                + b_sightseeing * fs_sightseeing;

        return u;
    }

    public double[] getDomDcCalibrationV() {
        return domDcCalibrationV;
    }

    public void updatedomDcCalibrationV(double[] b_calibrationVector) {
        this.domDcCalibrationV[0] = this.domDcCalibrationV[0]*b_calibrationVector[0];
        this.domDcCalibrationV[1] = this.domDcCalibrationV[1]*b_calibrationVector[1];
        this.domDcCalibrationV[2] = this.domDcCalibrationV[2]*b_calibrationVector[2];
    }

    public Matrix getAutoDist() {
        return autoDist;
    }
}


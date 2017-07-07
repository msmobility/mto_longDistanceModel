package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by Joe on 26/10/2016.
 */
public class DomesticDestinationChoice {
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    public static final int CHOICE_SET_SIZE = 117;
    private TableDataSet combinedZones;
    private TableDataSet coefficients;
    private Matrix autoDist;
    private int[] alternatives;
    String[] tripPurposeArray;
    private DomesticModeChoice domesticModeChoice;

    public DomesticDestinationChoice(ResourceBundle rb, MtoLongDistData ldData, DomesticModeChoice domesticModeChoice) {
        //coef format
        // table format: coeff | visit | leisure | business
        coefficients = Util.readCSVfile(rb.getString("dc.domestic.coefs"));
        coefficients.buildStringIndex(1);
        tripPurposeArray = ldData.tripPurposes.toArray(new String[ldData.tripPurposes.size()]);

        //load alternatives - need to calculate distance, lang_barrier, and metro-regional for each OD pair
        combinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        combinedZones.buildIndex(1);

        //load combined zones distance skim
        readSkim(rb);

        alternatives = combinedZones.getColumnAsInt("alt");

        this.domesticModeChoice = domesticModeChoice;
    }

    public void readSkim(ResourceBundle rb) {
        // read skim file

        String matrixName = "auto.skim.combinedzones.2013";
        String hwyFileName = rb.getString(matrixName);
        logger.info("  Reading skims file" + hwyFileName);
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.combinedzones.distance"));
        autoDist = Util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("skim.combinedzones.lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoDist.setExternalNumbersZeroBased(externalNumbers);
    }

    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTrip trip) {

        //        switch (trip.getLongDistanceTripPurpose()) {
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
        String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives)
                //calculate exp(Ui) for each destination
                .mapToDouble(a -> Math.exp(calculateUtility(trip, tripPurpose, a))).toArray();
        //calculate the probability for each trip, based on the destination utilities
        double probability_denominator = Arrays.stream(expUtilities).sum();

        //calculate the probability for each trip, based on the destination utilities
        double[] probabilities = Arrays.stream(expUtilities).map(u -> u/probability_denominator).toArray();

        //choose one destination, weighted at random by the probabilities
        return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();

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

        //if (distance < 40 || distance > 1000) return Double.NEGATIVE_INFINITY;
        //if (distance < 40) return Double.NEGATIVE_INFINITY;
        //if (origin == destination && trip.getOrigZone().getZoneType() == ZoneType.EXTCANADA) return Double.NEGATIVE_INFINITY;

        //TODO: check if this west-east transformation is suitable
        String origin_east_west = combinedZones.getIndexedStringValueAt(origin,"loc");
        String destination_east_west = combinedZones.getIndexedStringValueAt(destination,"loc");
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
            //todo how to deal with trips that logsum == 0 --> means that no mode is available
            //logger.info(trip.getOrigZone().getCombinedZoneId() + " to " + destination);
        } else {
            logsum = Math.log(logsum);
        }

        double dtLogsum = trip.getLongDistanceTripState()==1? logsum: 0;
        double onLogsum = trip.getLongDistanceTripState()!=1? logsum: 0;



        //Coefficients
        double alpha = coefficients.getStringIndexedValueAt("alpha", tripPurpose);

        double k = coefficients.getStringIndexedValueAt("k", tripPurpose); //calibration coefficient

        double b_distance_exp = k * coefficients.getStringIndexedValueAt("dist_exp", tripPurpose);
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
                b_distance_exp * Math.exp(-alpha * distance)
                + b_distance_log * log_distance
                + b_dtLogsum*dtLogsum
                + b_onLogsum*onLogsum
                + b_civic * civic
                + b_mm_inter * mm_inter
                + b_m_intra * m_intra
                + b_r_intra * r_intra
                + b_niagara * fs_niagara
                + b_outdoors * (trip.isSummer() ? 1 : 0)* fs_outdoors
                + b_skiing * (!trip.isSummer() ? 1 : 0) *  fs_skiing
                + b_medical * fs_medical
                + b_hotel * fs_hotel
                + b_sightseeing * fs_sightseeing;

        return u;
    }


    public Matrix getAutoDist() {
        return autoDist;
    }
}


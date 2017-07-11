package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/12/2017.
 */
public class IntInboundDestinationChoice {

    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    private TableDataSet destCombinedZones;
    private TableDataSet coefficients;
    private Matrix autoTravelTime;
    private int[] alternatives;
    private String[] tripPurposeArray;
    private String[] tripStateArray;
    private IntModeChoice intModeChoice;


    public IntInboundDestinationChoice(ResourceBundle rb, MtoLongDistData ldData, IntModeChoice intMcModel) {
        //coef format
        // table format: coeff | visit | leisure | business
        coefficients = Util.readCSVfile(rb.getString("dc.int.us.in.coefs"));
        coefficients.buildStringIndex(1);
        tripPurposeArray = ldData.tripPurposes.toArray(new String[ldData.tripPurposes.size()]);
        tripStateArray = ldData.tripStates.toArray(new String[ldData.tripStates.size()]);

        //load alternatives
        destCombinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        destCombinedZones.buildIndex(1);

        //load combined zones distance skim
        readSkim(rb);
        alternatives = destCombinedZones.getColumnAsInt("alt");

        intModeChoice = intMcModel;
    }


    public int selectDestinationFromUs(LongDistanceTrip trip) {

        String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives).mapToDouble(a -> Math.exp(calculateCanZoneUtilityFromUs(trip, tripPurpose, a))).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();

        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();
    }


    public int selectDestinationFromOs(LongDistanceTrip trip) {

        //String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives).mapToDouble(a -> calculateCanZoneUtilityFromOs(a)).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();


        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();
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
        String tripState = tripStateArray[trip.getLongDistanceTripState()];

        double b_population = coefficients.getStringIndexedValueAt("population", tripPurpose);
        double b_dist = coefficients.getStringIndexedValueAt("b_dist", tripPurpose);
        double alpha_dist = coefficients.getStringIndexedValueAt("alpha_dist", tripPurpose);
        double b_dtLogsum = coefficients.getStringIndexedValueAt("dtLogsum", tripPurpose);
        double b_onLogsum = coefficients.getStringIndexedValueAt("onLogsum", tripPurpose);
        double b_civic = coefficients.getStringIndexedValueAt("civic", tripPurpose);
        double b_skiing = coefficients.getStringIndexedValueAt("skiing", tripPurpose);
        double b_altIsMetro = coefficients.getStringIndexedValueAt("altIsMetro", tripPurpose);
        double b_hotel = coefficients.getStringIndexedValueAt("hotel", tripPurpose);

        double k_dtLogsum = coefficients.getStringIndexedValueAt("k_dtLogsum", tripPurpose);
        double k_onLogsum = coefficients.getStringIndexedValueAt("k_onLogsum", tripPurpose);

        //todo manual test of calibration parameters
        switch (trip.getLongDistanceTripPurpose()) {
            case 2:
                //tripPurpose = "leisure";
                k_dtLogsum = 1;
                k_onLogsum = k_dtLogsum;
                break;
            case 0:
                //tripPurpose = "visit";
                k_dtLogsum = 1;
                k_onLogsum = k_dtLogsum;
                break;
            case 1:
                //tripPurpose = "business";
                k_dtLogsum = 1;
                k_onLogsum = k_dtLogsum;
                break;
        }

        //get the logsum
        double logsum = 0;
        int[] modes = intModeChoice.getModes();
        for (int m : modes) {
            logsum += Math.exp(intModeChoice.calculateUtilityToCanada(trip, m, destination));
        }
        if(logsum ==0){
            return Double.NEGATIVE_INFINITY;
            //todo how to deal with trips that logsum == 0 --> means that no mode is available
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

        double hotel = destCombinedZones.getIndexedValueAt(destination, "hotel");
        double skiing = destCombinedZones.getIndexedValueAt(destination, "skiing");
        int altIsMetro = (int) destCombinedZones.getIndexedValueAt(destination, "alt_is_metro");


        //calculate utility
        return b_population * population +
                b_dist * Math.exp(alpha_dist * dist) +
                b_dtLogsum * (1 - overnight) * logsum * k_dtLogsum +
                b_onLogsum * overnight * logsum * k_onLogsum +
                b_civic * civic +
                b_skiing * skiing +
                b_altIsMetro * altIsMetro +
                b_hotel * hotel;

    }

    public double calculateCanZoneUtilityFromOs(int destination) {

//read coefficients

        return destCombinedZones.getIndexedValueAt(destination, "population");


    }

}

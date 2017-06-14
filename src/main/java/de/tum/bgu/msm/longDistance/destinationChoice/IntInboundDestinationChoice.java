package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.modeChoice.IntOutboundModeChoice;
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
    String[] tripPurposeArray;
    private IntOutboundModeChoice intOutboundModeChoice;


    public IntInboundDestinationChoice(ResourceBundle rb, MtoLongDistData ldData, IntOutboundModeChoice intMcModel){
        //coef format
        // table format: coeff | visit | leisure | business
        coefficients = Util.readCSVfile(rb.getString("dc.int.us.in.coefs"));
        coefficients.buildStringIndex(1);
        tripPurposeArray = ldData.tripPurposes.toArray(new String[ldData.tripPurposes.size()]);

        //load alternatives
        destCombinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        destCombinedZones.buildIndex(1);

        //load combined zones distance skim
        readSkim(rb);
        alternatives = destCombinedZones.getColumnAsInt("alt");

        intOutboundModeChoice = intMcModel;
    }


    public int selectDestinationFromUs(LongDistanceTrip trip) {

        String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives).mapToDouble(a -> Math.exp(calculateCanZoneUtilityFromUs(trip, tripPurpose, a))).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();

        double[] probabilities = Arrays.stream(expUtilities).map(u -> u/probability_denominator).toArray();

        return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();
    }


    public int selectDestinationFromOs (LongDistanceTrip trip){

        //String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];

        double[] expUtilities = Arrays.stream(alternatives).mapToDouble(a -> calculateCanZoneUtilityFromOs(a)).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();

        double[] probabilities = Arrays.stream(expUtilities).map(u -> u/probability_denominator).toArray();

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


    public double calculateCanZoneUtilityFromUs (LongDistanceTrip trip, String tripPurpose, int destination){

//read coefficients

        double b_population = coefficients.getStringIndexedValueAt("population", tripPurpose);
        double b_dist = coefficients.getStringIndexedValueAt("b_dist", tripPurpose);
        double alpha_dist = coefficients.getStringIndexedValueAt("alpha_dist", tripPurpose);

        //get the logsum
        double logsum = 0;
                int[] modes = intOutboundModeChoice.getModes();
        for (int m: modes){
            logsum += Math.exp(intOutboundModeChoice.calculateUtility(trip, m, destination));
        }
        logsum = Math.log(logsum);


        //read trip data
        double dist = autoTravelTime.getValueAt(trip.getOrigZone().getCombinedZoneId(), destination);

        //read destination data
        double population =  destCombinedZones.getIndexedValueAt(destination,"population");

        return b_population * population +
                b_dist * Math.exp(alpha_dist * dist);


    }

    public double calculateCanZoneUtilityFromOs (int destination){

//read coefficients

        return destCombinedZones.getIndexedValueAt(destination,"population");


    }



}

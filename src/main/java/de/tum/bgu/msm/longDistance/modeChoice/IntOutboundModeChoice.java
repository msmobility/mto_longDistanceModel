package de.tum.bgu.msm.longDistance.modeChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.Person;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/26/2017.
 */
public class IntOutboundModeChoice {

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

    private TableDataSet mcInternationalCoefficients;




    public IntOutboundModeChoice(ResourceBundle rb, MtoLongDistData ldData, DomesticModeChoice dmChoice) {
        this.rb = rb;

        tripPurposeArray = ldData.tripPurposes.toArray(new String[ldData.tripPurposes.size()]);
        tripStateArray = ldData.tripStates.toArray(new String[ldData.tripStates.size()]);

        mcInternationalCoefficients = Util.readCSVfile(rb.getString("mc.int.outbound.coefs"));
        mcInternationalCoefficients.buildStringIndex(1);

        travelTimeMatrix = dmChoice.getTravelTimeMatrix();
        priceMatrix = dmChoice.getPriceMatrix();
        transferMatrix = dmChoice.getTransferMatrix();
        frequencyMatrix = dmChoice.getFrequencyMatrix();
    }

    public int selectMode(LongDistanceTrip trip){

        double[] expUtilities = Arrays.stream(modes)
                //calculate exp(Ui) for each destination
                .mapToDouble(m -> Math.exp(calculateUtility(trip, m, trip.getDestZoneId()))).toArray();


        double probability_denominator = Arrays.stream(expUtilities).sum();
        //todo if there is no access by any mode for the selected OD pair, just go by car
        if (probability_denominator == 0){
            expUtilities[0] = 1;
        }

        //calculate the probability for each trip, based on the destination utilities
        //double[] probabilities = Arrays.stream(expUtilities).map(u -> u/probability_denominator).toArray();

        //choose one destination, weighted at random by the probabilities
        return new EnumeratedIntegerDistribution(modes, expUtilities).sample();


    }

    public double calculateUtility(LongDistanceTrip trip, int m, int destination){

        double utility;
        String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];
        String column = modeNames[m] + "." + tripPurpose;
        String tripState = tripStateArray[trip.getLongDistanceTripState()];

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

        double vot= mcInternationalCoefficients.getStringIndexedValueAt("vot", column);

//        todo scenario testing - remove for final version
//        if (origin >18 & origin < 28 & destination == 103 & m == 2){
//            //"a high speed train between toronto a montreal that reduces time to the half
//            time = time / 2;
//
//        }



//        if (origin >18 & origin < 28 & destination == 103){
//            //price = price * 2;
//            logger.info("mode" +  modeNames[m] + "intermetro: " + interMetro + " ruralRural: " + ruralRural + "travelTime" + time);
//        }


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
        double b_intercept = mcInternationalCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency= mcInternationalCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price= mcInternationalCoefficients.getStringIndexedValueAt("price", column);
        double b_time= mcInternationalCoefficients.getStringIndexedValueAt("time", column);
        double b_overnight= mcInternationalCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party= mcInternationalCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance= mcInternationalCoefficients.getStringIndexedValueAt("impedance", column);

        utility = b_intercept + b_frequency*frequency +
                b_price * price +
                b_time * time +
                b_overnight * overnight +
                b_party * party +
                b_impedance * impedance;


        if (time < 0 ) utility = Double.NEGATIVE_INFINITY;


        return utility;

    }

    public int[] getModes() {
        return modes;
    }
}

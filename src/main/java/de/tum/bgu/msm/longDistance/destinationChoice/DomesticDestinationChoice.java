package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.sun.java.browser.plugin2.DOM;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.mtoLongDistData;

import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
/**
 * Created by Joe on 26/10/2016.
 */
public class DomesticDestinationChoice {
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    public static final int CHOICE_SET_SIZE = 117;
    private TableDataSet combinedZones;
    private TableDataSet coefficients;
    private Matrix autoTravelTime;

    public DomesticDestinationChoice(ResourceBundle rb) {
        //coef format
        // table format: coeff | visit | leisure | business
        coefficients = util.readCSVfile(rb.getString("dc.domestic.coefs"));
        coefficients.buildStringIndex(1);

        //load alternatives - need to calculate distance, lang_barrier, and metro-regional for each OD pair
        combinedZones = util.readCSVfile(rb.getString("dc.combined.zones"));
        combinedZones.buildIndex(1);

        //load combined zones distance skim
        readSkim(rb);


    }

    public void readSkim(ResourceBundle rb) {
        // read skim file
        logger.info("  Reading skims files");

        String matrixName = "auto.skim.combinedzones." + mto.getYear();
        String hwyFileName = rb.getString(matrixName);
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.combinedzones.time"));
        autoTravelTime = util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup("combinedZone");
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoTravelTime.setExternalNumbersZeroBased(externalNumbers);
    }

    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTrip trip) {
        //need a legitimate trip purpose
    //    if (trip.getTripPurpose() > 3) {
     //       return 0;
     //   }

        List<Pair<Integer, Double>> utilities = Arrays.stream(combinedZones.getColumnAsInt("alt"))
                //calculate exp(Ui) for each destination
                .mapToObj(d ->  new Pair<> ( d, Math.exp(calculateDestinationUtility(trip, d))))
                .collect(Collectors.toList());
        //calculate the probability for each trip, based on the destination utilities
        double probability_denominator = utilities.stream()
                .mapToDouble(Pair::getSecond).sum();

        //calculate the probability for each trip, based on the destination utilities
        List<Pair<Integer, Double>> probabilities = utilities.stream()
                .map(p -> new Pair<> (p.getFirst(), p.getSecond()/probability_denominator))
                .collect(Collectors.toList());

        //choose one destination, weighted at random by the probabilities
        int selectedDestination = new EnumeratedDistribution<>(probabilities).sample();

        return selectedDestination;

    }

    private double calculateDestinationUtility(LongDistanceTrip trip, int destination) {
        String tripPurpose = "";

        switch (trip.getLongDistanceTripPurpose()) {
            case 0:
                tripPurpose = "leisure";
                break;
            case 1:
                tripPurpose = "visit";
                break;
            case 2:
                tripPurpose = "business";
                break;
        }

        //TODO: check ordering or number -> string value (ie purpose 1 = "visit""?)
        int origin = trip.getOrigZone().getCombinedZoneId();
        float distance = autoTravelTime.getValueAt(origin, destination);

        int lang_barrier = 1 - ((int) combinedZones.getIndexedValueAt(origin,"speak_french") * (int) combinedZones.getIndexedValueAt(destination,"speak_french"));
        int mm = (int) combinedZones.getIndexedValueAt(origin,"alt_is_metro") * (int) combinedZones.getIndexedValueAt(destination,"alt_is_metro");
        int mr = (int) combinedZones.getIndexedValueAt(origin,"alt_is_metro") * (1 - (int) combinedZones.getIndexedValueAt(destination,"alt_is_metro"));

        double b_distance = coefficients.getStringIndexedValueAt("dist_exp", tripPurpose);
        double b_population = coefficients.getStringIndexedValueAt("pop_log", tripPurpose);
        double b_lang_barrier = coefficients.getStringIndexedValueAt("lang.barrier", tripPurpose);
        double b_mm = coefficients.getStringIndexedValueAt("mm", tripPurpose);
        double b_mr = coefficients.getStringIndexedValueAt("rm", tripPurpose);

        double u = b_distance * Math.exp(-1.0 * distance)
                + b_population * Math.log(combinedZones.getIndexedValueAt(destination,"population"))
                + b_lang_barrier * lang_barrier
                + b_mm * mm
                + b_mr * mr;

        return u;
    }



}


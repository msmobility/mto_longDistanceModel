package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.syntheticPopulation.readSP;
import javafx.collections.FXCollections;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 *
 */

public class mtoLongDistance {
    static Logger logger = Logger.getLogger(mtoLongDistance.class);
    private ResourceBundle rb;


    public mtoLongDistance(ResourceBundle rb) {
        this.rb = rb;
    }


    public void runLongDistanceModel () {
        // main method to run long-distance model

        readSP rsp = new readSP(rb);
        rsp.readZonalData();
        rsp.readSyntheticPopulation();

        //TODO next 3 lines commented by Carlos Llorca on 4/7/2016 because skim matrix and accessibility is not available
        //mtoLongDistData md = new mtoLongDistData(rb);
        //md.readSkim();
        //md.calculateAccessibility(rsp);

        tripGeneration tgdomestic = new tripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_domestic = tgdomestic.runTripGeneration();

        logger.info("Domestic Trips generated");

        //this must be after domestic
        internationalTripGeneration tginternationa2 = new internationalTripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_international = tginternationa2.runInternationalTripGeneration();

        logger.info("International trips generated");

        //next method is used to analyze the outputs of the tripGeneration
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();
        trips.addAll(trips_international);
        trips.addAll(trips_domestic);

        mtoAnalyzeTrips tripAnalysis = new mtoAnalyzeTrips(rb);
        tripAnalysis.runMtoAnalyzeTrips(trips);


    }
}

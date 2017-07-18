package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.accessibilityAnalysis.AccessibilityAnalysis;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationModel;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneDisaggregator;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import de.tum.bgu.msm.Util;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 */

public class MtoLongDistance {

    public static Random rand;

    static Logger logger = Logger.getLogger(MtoLongDistance.class);
    private ResourceBundle rb;
    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private MtoLongDistData mtoLongDistData;
    private SyntheticPopulation syntheticPopulationReader;
    private TripGenerationModel tripGenModel;
    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;
    private DomesticModeChoice mcDomesticModel;
    private IntModeChoice intModeChoice;
    private ZoneDisaggregator zd;

    //SET UP the models
    public MtoLongDistance(ResourceBundle rb) {
        this.rb = rb;
        Util.initializeRandomNumber(rb);

        mtoLongDistData = new MtoLongDistData(rb);
        syntheticPopulationReader = new SyntheticPopulation(rb, mtoLongDistData);
        tripGenModel = new TripGenerationModel(rb, mtoLongDistData, syntheticPopulationReader);
        mcDomesticModel = new DomesticModeChoice(rb, mtoLongDistData);
        intModeChoice = new IntModeChoice(rb, mtoLongDistData, mcDomesticModel);
        dcModel = new DomesticDestinationChoice(rb, mtoLongDistData, mcDomesticModel);
        dcOutboundModel = new IntOutboundDestinationChoice(rb, mtoLongDistData, intModeChoice, dcModel);
        dcInBoundModel = new IntInboundDestinationChoice(rb, mtoLongDistData, intModeChoice, dcModel);
        zd = new ZoneDisaggregator(rb,mtoLongDistData);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void loadLongDistanceModel() {
        //LOAD the models
        mtoLongDistData.loadZonalData();
        syntheticPopulationReader.loadSyntheticPopulation();
        mtoLongDistData.populateZones(syntheticPopulationReader);
        tripGenModel.loadTripGenerationModels();
        dcModel.loadDomesticDestinationChoice();
        dcInBoundModel.loadIntInboundDestinationChoice();
        dcOutboundModel.loadIntOutboundDestinationChoiceModel();
        mcDomesticModel.loadDomesticModeChoice();
        intModeChoice.loadIntModeChoice();
        zd.loadZoneDisaggregator();
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void runLongDistanceModel() {

        //todo test to avoid parallelization
        //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "0");


        //RUN models
        boolean runTG = ResourceUtil.getBooleanProperty(rb, "run.trip.gen", false);
        boolean runDC = ResourceUtil.getBooleanProperty(rb, "run.dest.choice", false);

        //developing tools to skip TG and/or DC if needed
        if (!runTG) {
            if (runDC) {
                //load saved trips without destination
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(ResourceUtil.getProperty(rb, "trip.in.file"));
                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader, false);
                    allTrips.add(ldt);
                }
            } else {
                //load saved trip with destinations
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(ResourceUtil.getProperty(rb, "trip.in.file"));

                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader, true);
                    allTrips.add(ldt);
                }
            }
        }





        if (runTG) {
            allTrips = tripGenModel.runTripGeneration();
        }

        if (runDC) runDestinationChoice(allTrips);

        runModeChoice(allTrips);

        boolean calibrationDC = ResourceUtil.getBooleanProperty(rb, "dc.calibration", false);;
        boolean calibrationMC = ResourceUtil.getBooleanProperty(rb, "mc.calibration", false);;
        if (calibrationDC || calibrationMC){
            calibrateModel(calibrationDC, calibrationMC);
        }

        runDisaggregation(allTrips);

        if (ResourceUtil.getBooleanProperty(rb, "write.trips", false)) {
            syntheticPopulationReader.writeSyntheticPopulation();
            writeTrips(allTrips);
        }


        if (ResourceUtil.getBooleanProperty(rb, "analyze.accessibility", false)) {

            AccessibilityAnalysis accAna = new AccessibilityAnalysis(rb, mtoLongDistData);
            accAna.calculateAccessibilityForAnalysis();
        }

    }


    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
            if (!t.isLongDistanceInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                t.setDestination(destZoneId);
                t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
            } else {
                if (t.getOrigZone().getZoneType() == ZoneType.ONTARIO || t.getOrigZone().getZoneType() == ZoneType.EXTCANADA) {
                    // residents to international
                    int destZoneId = dcOutboundModel.selectDestination(t);
                    t.setDestination(destZoneId);
                    t.setDestZoneType(dcOutboundModel.getDestinationZoneType(destZoneId));
                    if (t.getDestZoneType().equals(ZoneType.EXTUS))
                        t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));

                } else if (t.getOrigZone().getZoneType() == ZoneType.EXTUS) {
                    // us visitors with destination in CANADA
                    int destZoneId = dcInBoundModel.selectDestinationFromUs(t);
                    t.setDestination(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                    t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
                } else {
                    //os visitors to Canada
                    int destZoneId = dcInBoundModel.selectDestinationFromOs(t);
                    t.setDestination(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                }
            }

        });
    }


    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!t.isLongDistanceInternational()) {
                //domestic mode choice for synthetic persons in Ontario
                int mode = mcDomesticModel.selectModeDomestic(t);
                t.setMode(mode);
                // international mode choice
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getOrigZone().getZoneType().equals(ZoneType.EXTCANADA)) {
                //residents
                if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from Canada to US
                    int mode = intModeChoice.selectMode(t);
                    t.setMode(mode);
                } else {
                    //international from Canada to OS
                    t.setMode(1); //always by air
                }
                //visitors
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS)) {
                //international visitors from US
                int mode = intModeChoice.selectMode(t);
                t.setMode(mode);
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTOVERSEAS)) {
                //international visitors from US
                t.setMode(1); //always by air
            }

        });
    }

    public void runDisaggregation(ArrayList<LongDistanceTrip> allTrips) {
        logger.info("Starting disaggregation");
        allTrips.parallelStream().forEach(t -> {
            zd.disaggregateDestination(t);
        });
        logger.info("Finished disaggregation");
    }

    public void writeTrips(ArrayList<LongDistanceTrip> trips) {
        logger.info("Writing out data for trip generation (trips)");

        String OutputTripsFileName = rb.getString("trip.out.file");
        PrintWriter pw = Util.openFileForSequentialWriting(OutputTripsFileName, false);


        pw.println(LongDistanceTrip.getHeader());


        for (LongDistanceTrip tr : trips) {
            //if (tr.getOrigZone().getZoneType() == ZoneType.ONTARIO){
            pw.println(tr.toString());
        }


        pw.close();
    }

//    public void writePopByZone() {
//        PrintWriter pw = Util.openFileForSequentialWriting(rb.getString("zone.out.file"), false);
//        pw.println("zone,hh,pp");
//        for (Zone zone : mtoLongDistData.getInternalZoneList()) {
//            pw.println(zone.getId() + "," + zone.getHouseholds() + "," + zone.getPopulation());
//        }
//        pw.close();
//    }




    public void calibrateModel(boolean dc, boolean mc){

        int maxIter = 10;
        double[][][] calibrationMatrixMc = new double[4][3][4];
        double[][] calibrationMatrixDc = new double[3][3];
        Calibration c = new Calibration();

        if (dc) {
            for (int iteration = 0; iteration < maxIter; iteration++) {

                logger.info("Calibration of destination choice: Iteration = " + iteration);
                calibrationMatrixDc = c.calculateCalibrationMatrix(allTrips);
                dcModel.updatedomDcCalibrationV(calibrationMatrixDc[0]);
                dcOutboundModel.updateIntOutboundCalibrationV(calibrationMatrixDc[1]);
                dcInBoundModel.updateIntInboundCalibrationV(calibrationMatrixDc[2]);

                runDestinationChoice(allTrips);
            }
        }

        if (mc){
            for (int iteration = 0; iteration < maxIter; iteration++) {

                logger.info("Calibration of mode choice: Iteration = " + iteration);
                calibrationMatrixMc = c.calculateMCCalibrationFactors(allTrips, iteration, maxIter);
                mcDomesticModel.updateCalibrationDomestic(calibrationMatrixMc[0]);
                mcDomesticModel.updateCalibrationDomesticVisitors(calibrationMatrixMc[3]);
                intModeChoice.updateCalibrationOutbound(calibrationMatrixMc[1]);
                intModeChoice.updateCalibrationInbound(calibrationMatrixMc[2]);

                //runDestinationChoice(allTrips);
                runModeChoice(allTrips);
            }

        }

        runDestinationChoice(allTrips);
        runModeChoice(allTrips);

        logger.info("---------------------------------------------------------");
        logger.info("-----------------RESULTS DC------------------------------");
        logger.info("k_domestic_dc visit = " + dcModel.getDomDcCalibrationV()[0]);
        logger.info("k_domestic_dc business = " + dcModel.getDomDcCalibrationV()[1]);
        logger.info("k_domestic_dc leisure = " + dcModel.getDomDcCalibrationV()[2]);
        logger.info("k_int_out_dc visit = " + dcOutboundModel.getCalibrationV()[0]);
        logger.info("k_int_out_dc business = " + dcOutboundModel.getCalibrationV()[1]);
        logger.info("k_int_out_dc leisure = " + dcOutboundModel.getCalibrationV()[2]);
        logger.info("k_int_in_dc visit = " + dcInBoundModel.getCalibrationV()[0]);
        logger.info("k_int_in_dc business = " + dcInBoundModel.getCalibrationV()[1]);
        logger.info("k_int_in_dc leisure = " + dcInBoundModel.getCalibrationV()[2]);
        logger.info("---------------------------------------------------------");

        logger.info("---------------------------------------------------------");
        logger.info("-----------------RESULTS MC------------------------------");
        String type = "k_domestic_mc_";
        logger.info(type + "visit: auto=" + mcDomesticModel.getCalibrationMatrix()[0][0] +
                ",air=" + mcDomesticModel.getCalibrationMatrix()[0][1] +
                ",rail=" + mcDomesticModel.getCalibrationMatrix()[0][2] +
                ",bus=" + mcDomesticModel.getCalibrationMatrix()[0][3]);
        logger.info(type + "business: auto=" + mcDomesticModel.getCalibrationMatrix()[1][0] +
                ",air=" + mcDomesticModel.getCalibrationMatrix()[1][1] +
                ",rail=" + mcDomesticModel.getCalibrationMatrix()[1][2] +
                ",bus=" + mcDomesticModel.getCalibrationMatrix()[1][3]);
        logger.info(type + "leisure: auto=" + mcDomesticModel.getCalibrationMatrix()[2][0] +
                ",air=" + mcDomesticModel.getCalibrationMatrix()[2][1] +
                ",rail=" + mcDomesticModel.getCalibrationMatrix()[2][2] +
                ",bus=" + mcDomesticModel.getCalibrationMatrix()[2][3]);
        type = "k_int_out_mc_";
        logger.info(type + "visit: auto=" + intModeChoice.getCalibrationMatrixOutbound()[0][0] +
                ",air=" + intModeChoice.getCalibrationMatrixOutbound()[0][1] +
                ",rail=" + intModeChoice.getCalibrationMatrixOutbound()[0][2] +
                ",bus=" + intModeChoice.getCalibrationMatrixOutbound()[0][3]);
        logger.info(type + "business: auto=" + intModeChoice.getCalibrationMatrixOutbound()[1][0] +
                ",air=" + intModeChoice.getCalibrationMatrixOutbound()[1][1] +
                ",rail=" + intModeChoice.getCalibrationMatrixOutbound()[1][2] +
                ",bus=" + intModeChoice.getCalibrationMatrixOutbound()[1][3]);
        logger.info(type + "leisure: auto=" + intModeChoice.getCalibrationMatrixOutbound()[2][0] +
                ",air=" + intModeChoice.getCalibrationMatrixOutbound()[2][1] +
                ",rail=" + intModeChoice.getCalibrationMatrixOutbound()[2][2] +
                ",bus=" + intModeChoice.getCalibrationMatrixOutbound()[2][3]);
        type = "k_int_in_mc";
        logger.info(type + "visit: auto=" + intModeChoice.getCalibrationMatrixInbound()[0][0] +
                ",air=" + intModeChoice.getCalibrationMatrixInbound()[0][1] +
                ",rail=" + intModeChoice.getCalibrationMatrixInbound()[0][2] +
                ",bus=" + intModeChoice.getCalibrationMatrixInbound()[0][3]);
        logger.info(type + "business: auto=" + intModeChoice.getCalibrationMatrixInbound()[1][0] +
                ",air=" + intModeChoice.getCalibrationMatrixInbound()[1][1] +
                ",rail=" + intModeChoice.getCalibrationMatrixInbound()[1][2] +
                ",bus=" + intModeChoice.getCalibrationMatrixInbound()[1][3]);
        logger.info(type + "leisure: auto=" + intModeChoice.getCalibrationMatrixOutbound()[2][0] +
                ",air=" + intModeChoice.getCalibrationMatrixInbound()[2][1] +
                ",rail=" + intModeChoice.getCalibrationMatrixInbound()[2][2] +
                ",bus=" + intModeChoice.getCalibrationMatrixInbound()[2][3]);


        type = "k_domesticVisitors_mc_";
        logger.info(type + "visit: auto=" + mcDomesticModel.getCalibrationMatrixVisitors()[0][0] +
                ",air=" + mcDomesticModel.getCalibrationMatrixVisitors()[0][1] +
                ",rail=" + mcDomesticModel.getCalibrationMatrixVisitors()[0][2] +
                ",bus=" + mcDomesticModel.getCalibrationMatrixVisitors()[0][3]);
        logger.info(type + "business: auto=" + mcDomesticModel.getCalibrationMatrixVisitors()[1][0] +
                ",air=" + mcDomesticModel.getCalibrationMatrixVisitors()[1][1] +
                ",rail=" + mcDomesticModel.getCalibrationMatrixVisitors()[1][2] +
                ",bus=" + mcDomesticModel.getCalibrationMatrixVisitors()[1][3]);
        logger.info(type + "leisure: auto=" + mcDomesticModel.getCalibrationMatrixVisitors()[2][0] +
                ",air=" + mcDomesticModel.getCalibrationMatrixVisitors()[2][1] +
                ",rail=" + mcDomesticModel.getCalibrationMatrixVisitors()[2][2] +
                ",bus=" + mcDomesticModel.getCalibrationMatrixVisitors()[2][3]);
        logger.info("---------------------------------------------------------");
    }



}

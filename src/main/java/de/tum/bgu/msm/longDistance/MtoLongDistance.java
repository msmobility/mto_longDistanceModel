package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.JsonUtilMto;
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
    private JsonUtilMto prop;

    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private MtoLongDistData mtoLongDistData;
    private SyntheticPopulation syntheticPopulationReader;
    private TripGenerationModel tripGenModel;
    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;
    private DomesticModeChoice mcDomesticModel;
    private IntModeChoice intModeChoice;
    private Calibration c;
    private ZoneDisaggregator zd;

    //developing options
    private boolean runTG;
    private boolean runDC;
    private boolean calibrationDC;
    private boolean calibrationMC;
    private String inputTripFile;

    //output options
    private boolean writeTrips;
    //private boolean analyzeAccess;

    //SET UP the models
    public MtoLongDistance(ResourceBundle rb, JsonUtilMto prop) {
        this.rb = rb;
        this.prop = prop;
        Util.initializeRandomNumber(rb);

        //read developing options
        //calibrationDC = ResourceUtil.getBooleanProperty(rb, "dc.calibration", false);;
        calibrationDC = prop.getBooleanProp("dc.calibration");
        //calibrationMC = ResourceUtil.getBooleanProperty(rb, "mc.calibration", false);;
        calibrationMC = prop.getBooleanProp("mc.calibration");
        //runTG = ResourceUtil.getBooleanProperty(rb, "run.trip.gen", false);
        runTG = prop.getBooleanProp("run.develop.tg");
        //runDC = ResourceUtil.getBooleanProperty(rb, "run.dest.choice", false);
        runDC = prop.getBooleanProp("run.develop.dc");
        inputTripFile = prop.getStringProp("run.develop.trip_input_file");

        //read output options
        writeTrips = ResourceUtil.getBooleanProperty(rb, "write.trips", false);
        //analyzeAccess = ResourceUtil.getBooleanProperty(rb, "analyze.accessibility", false);

        mtoLongDistData = new MtoLongDistData(rb, prop);
        syntheticPopulationReader = new SyntheticPopulation(rb, prop, mtoLongDistData);
        tripGenModel = new TripGenerationModel(rb, prop, mtoLongDistData, syntheticPopulationReader);
        mcDomesticModel = new DomesticModeChoice(rb, prop, mtoLongDistData);
        intModeChoice = new IntModeChoice(rb, prop, mtoLongDistData, mcDomesticModel);
        dcModel = new DomesticDestinationChoice(rb, prop, mtoLongDistData, mcDomesticModel);
        dcOutboundModel = new IntOutboundDestinationChoice(rb, prop, mtoLongDistData, intModeChoice, dcModel);
        dcInBoundModel = new IntInboundDestinationChoice(rb, prop, mtoLongDistData, intModeChoice, dcModel);
        c = new Calibration();
        zd = new ZoneDisaggregator(rb, prop, mtoLongDistData);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void loadLongDistanceModel() {
        //LOAD the models
        mtoLongDistData.loadZonalData();
        syntheticPopulationReader.loadSyntheticPopulation();
        mtoLongDistData.populateZones(syntheticPopulationReader);
        tripGenModel.loadTripGenerationModels(dcOutboundModel);
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


        //TRIP GENERATION and DESTINATION CHOICE
        if (runTG && runDC){
            //run the full model
            allTrips = tripGenModel.runTripGeneration();
            runDestinationChoice(allTrips);
        } else {
            //run the in-development model
           runDevelopingTgAndDcModels();
        }

        //MODE CHOICE
        runModeChoice(allTrips);

        //CALIBRATION TOOLS
        if (calibrationDC || calibrationMC){
            calibrateModel(calibrationDC, calibrationMC);
        }

        //DISAGGREGATION
        runDisaggregation(allTrips);

        //OUTPUTS
        writeLongDistanceOutputs();


    }



    public void runDevelopingTgAndDcModels(){

        //developing tools to skip TG and/or DC if needed
        if (!runTG) {
            if (runDC) {
                //load saved trips without destination
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(inputTripFile);
                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader, false);
                    allTrips.add(ldt);
                }
                //and then run destination choice
                runDestinationChoice(allTrips);

            } else {
                //load saved trip with destinations
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(inputTripFile);

                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader, true);
                    allTrips.add(ldt);
                }
            }
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

    public void calibrateModel(boolean dc, boolean mc){

        int maxIter = 10;
        double[][][] calibrationMatrixMc = new double[4][3][4];
        double[][] calibrationMatrixDc = new double[3][3];


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

        c.printOutCalibrationResults(dcModel, dcOutboundModel, dcInBoundModel, mcDomesticModel, intModeChoice);

    }

    public void runDisaggregation(ArrayList<LongDistanceTrip> allTrips) {
        logger.info("Starting disaggregation");
        allTrips.parallelStream().forEach(t -> {
            zd.disaggregateDestination(t);
        });
        logger.info("Finished disaggregation");
    }


    public void writeLongDistanceOutputs(){
        if (writeTrips) {

            syntheticPopulationReader.writeSyntheticPopulation();
            writeTrips(allTrips);
        }


//        if (analyzeAccess) {
//
//            AccessibilityAnalysis accAna = new AccessibilityAnalysis(rb, mtoLongDistData);
//            accAna.calculateAccessibilityForAnalysis();
//        }

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



}

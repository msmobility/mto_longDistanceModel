package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.destinationChoice.DcModel;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.McModel;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationModel;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneDisaggregator;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.sp.SyntheticPopulation;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.*;

/**
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 */

public class LDModel implements ModelComponent {

    public static Random rand;

    static Logger logger = Logger.getLogger(LDModel.class);
    private ResourceBundle rb;
    private JSONObject prop;



    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private ZonalData zonalData;
    private SyntheticPopulation syntheticPopulationReader;
    private TripGenerationModel tripGenModel;
    private DcModel dcModel;
    private McModel mcModel;

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
    public LDModel() {


        syntheticPopulationReader = new SyntheticPopulation();
        zonalData = new ZonalData();
        tripGenModel = new TripGenerationModel();
        dcModel = new DcModel();
        mcModel  = new McModel();
        zd = new ZoneDisaggregator();

    }

    public void setup(JSONObject prop){
 //this.rb = rb;
        this.prop = prop;
        Util.initializeRandomNumber(prop);



        //read developing options
        //calibrationDC = ResourceUtil.getBooleanProperty(rb, "dc.calibration", false);;
        calibrationDC = JsonUtilMto.getBooleanProp(prop,"dc.calibration");
        //calibrationMC = ResourceUtil.getBooleanProperty(rb, "mc.calibration", false);;
        calibrationMC = JsonUtilMto.getBooleanProp(prop,"mc.calibration");
        //runTG = ResourceUtil.getBooleanProperty(rb, "run.trip.gen", false);
        runTG = JsonUtilMto.getBooleanProp(prop,"run.develop.tg");
        //runDC = ResourceUtil.getBooleanProperty(rb, "run.dest.choice", false);
        runDC = JsonUtilMto.getBooleanProp(prop,"run.develop.dc");
        inputTripFile = JsonUtilMto.getStringProp(prop,"run.develop.trip_input_file");

        //read output options
        writeTrips = JsonUtilMto.getBooleanProp(prop, "out.write_trips");
        //analyzeAccess = ResourceUtil.getBooleanProperty(rb, "analyze.accessibility", false);




        zonalData.setup(prop);
        syntheticPopulationReader.setup(prop);
        tripGenModel.setup(prop);
        dcModel.setup(prop);
        mcModel.setup(prop);



        c = new Calibration();

        zd.setup(prop);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {
        //LOAD the models
        zonalData.load(dataSet);
        syntheticPopulationReader.load(dataSet);

        mcModel.load(dataSet);

        dcModel.load(dataSet);
        //the order of loading models is still a bit odd but functional




        tripGenModel.load(dataSet);


        zd.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        //property change to avoid parallelization
        //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "0");


        //TRIP GENERATION and DESTINATION CHOICE
        if (runTG && runDC){
            //run the full model
            tripGenModel.run(dataSet, -1);
            dcModel.run(dataSet, -1);
        } else {
            //run the in-development model
           //runDevelopingTgAndDcModels();
        }

        //MODE CHOICE
        mcModel.run(dataSet, -1);

        //CALIBRATION TOOLS
        if (calibrationDC || calibrationMC){
            //calibrateModel(calibrationDC, calibrationMC);
        }

        //DISAGGREGATION
        runDisaggregation(allTrips);

        //OUTPUTS
        writeLongDistanceOutputs();


    }



   /* public void runDevelopingTgAndDcModels(){

        //developing tools to skip TG and/or DC if needed
        if (!runTG) {
            if (runDC) {
                //load saved trips without destination
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(inputTripFile);
                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, zonalData.getZoneLookup(), syntheticPopulationReader, false);
                    allTrips.add(ldt);
                }
                //and then run destination choice
                runDestinationChoice(allTrips);

            } else {
                //load saved trip with destinations
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(inputTripFile);

                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, zonalData.getZoneLookup(), syntheticPopulationReader, true);
                    allTrips.add(ldt);
                }
            }
        }


    }
*/






    /*public void calibrateModel(boolean dc, boolean mc){

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

    }*/

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
//            AccessibilityAnalysis accAna = new AccessibilityAnalysis(rb, zonalData);
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

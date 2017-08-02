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

import javax.xml.crypto.Data;
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

    //modules
    private ZonalData zonalData;
    private SyntheticPopulation syntheticPopulationReader;
    private TripGenerationModel tripGenModel;
    private DcModel dcModel;
    private McModel mcModel;
    private Calibration calib;
    private ZoneDisaggregator zd;

    //developing options
    private boolean runTG;
    private boolean runDC;
    private String inputTripFile;

    //output options
    private boolean writeTrips;
    private String outputTripFile;



    public LDModel() {
        syntheticPopulationReader = new SyntheticPopulation();
        zonalData = new ZonalData();
        tripGenModel = new TripGenerationModel();
        dcModel = new DcModel();
        mcModel  = new McModel();
        zd = new ZoneDisaggregator();
        calib = new Calibration();
    }

    public void setup(JSONObject prop){

        Util.initializeRandomNumber(prop);

        //options
        runTG = JsonUtilMto.getBooleanProp(prop,"run.develop.tg");
        runDC = JsonUtilMto.getBooleanProp(prop,"run.develop.dc");
        inputTripFile = JsonUtilMto.getStringProp(prop,"run.develop.trip_input_file");
        outputTripFile = JsonUtilMto.getStringProp(prop, "out.trip_file");
        writeTrips = JsonUtilMto.getBooleanProp(prop, "out.write_trips");

        //setup modules
        zonalData.setup(prop);
        syntheticPopulationReader.setup(prop);
        tripGenModel.setup(prop);
        dcModel.setup(prop);
        mcModel.setup(prop);
        calib.setup(prop);
        zd.setup(prop);

        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {
        dataSet.setModeChoiceModel(mcModel);
        dataSet.setDestinationChoiceModel(dcModel);


        //LOAD the modules
        zonalData.load(dataSet);
        syntheticPopulationReader.load(dataSet);
        mcModel.load(dataSet);
        dcModel.load(dataSet);

        tripGenModel.load(dataSet);
        calib.load(dataSet);
        zd.load(dataSet);

        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        //property change to avoid parallelization
        //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "0");

        //run models
        if (runTG && runDC){
            //run the full model
            tripGenModel.run(dataSet, -1);
            dcModel.run(dataSet, -1);
        } else {
            //run the in-development model
            runDevelopingTgAndDcModels(dataSet);
        }
        mcModel.run(dataSet, -1);
        calib.run(dataSet, -1);
        zd.run(dataSet, -1);

        //print outputs
        writeLongDistanceOutputs(dataSet);


    }



    public void runDevelopingTgAndDcModels(DataSet dataSet){



        //developing tools to skip TG and/or DC if needed
        if (!runTG) {
            if (runDC) {

                ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
                //load saved trips without destination
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(inputTripFile);
                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, dataSet.getZones(), dataSet, false);
                    allTrips.add(ldt);
                }
                dataSet.setAllTrips(allTrips);

                //and then run destination choice
                dcModel.run(dataSet, -1);

            } else {
                ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
                //load saved trip with destinations
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(inputTripFile);

                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, dataSet.getZones(), dataSet, true);
                    allTrips.add(ldt);
                }

                dataSet.setAllTrips(allTrips);
            }
        }


    }











    public void writeLongDistanceOutputs(DataSet dataSet){
        if (writeTrips) {

            syntheticPopulationReader.writeSyntheticPopulation();
            writeTrips(dataSet.getAllTrips());
        }


//        if (analyzeAccess) {
//
//            AccessibilityAnalysis accAna = new AccessibilityAnalysis(rb, zonalData);
//            accAna.calculateAccessibilityForAnalysis();
//        }

    }

    public void writeTrips(ArrayList<LongDistanceTrip> trips) {
        logger.info("Writing out data for trip generation (trips)");


        PrintWriter pw = Util.openFileForSequentialWriting(outputTripFile, false);


        pw.println(LongDistanceTrip.getHeader());


        for (LongDistanceTrip tr : trips) {
            //if (tr.getOrigZone().getZoneType() == ZoneType.ONTARIO){
            pw.println(tr.toString());
        }


        pw.close();
    }



}

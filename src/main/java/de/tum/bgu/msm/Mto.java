package de.tum.bgu.msm;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.MtoAnalyzeData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.MtoSurveyData;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Module to simulate long-distance travel
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 *
 */

public class Mto {
    // main class
    private static Logger logger = Logger.getLogger(Mto.class);
    private ResourceBundle rb;

    private JSONObject prop;


    private static int year;
    private static boolean winter;


    private Mto(ResourceBundle rb, JSONObject prop) {
        // constructor
        this.rb = rb;
        this.prop = prop;
    }


    public static void main(String[] args) {
        // main model run method

        logger.info("Ontario Provincial Model (MTO)");
        // Check how many arguments were passed in
        if(args.length != 3)
        {
            logger.error("Error: Please provide three arguments, 1. the model resources as rb, 2. the model resources as json, 3. the start year");
            System.exit(0);
        }
        long startTime = System.currentTimeMillis();
        ResourceBundle rb = Util.mtoInitialization(args[0]);
        JsonUtilMto jsonUtilMto = new JsonUtilMto(args[1]);
        //check this to read the json file from the code folder if needed
        //JsonUtilMto jsonUtilMto = new JsonUtilMto("INSERT LOCATION HERE");
        JSONObject prop = jsonUtilMto.getJsonProperties();

        //year = Integer.parseInt(args[1]);
        year = JsonUtilMto.getIntProp(prop, "year");
        //winter = ResourceUtil.getBooleanProperty(rb,"winter",false);
        winter = JsonUtilMto.getBooleanProp(prop,"winter");


        Mto model = new Mto(rb, prop);

        if (ResourceUtil.getBooleanProperty(rb, "analyze.survey.data", false)) model.runDataAnalysis();
        //if (ResourceUtil.getBooleanProperty(rb, "run.long.dist.mod", true)) model.runLongDistModel();
        //if (JsonUtilMto.getBooleanProp(prop, "run.full_model")) model.runLongDistModel();

        float endTime = Util.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        int seconds = (int)((endTime - 60*hours - min)*60);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes and " + seconds + " seconds." );
    }


    private void runDataAnalysis() {
        // main method to run TRSC and ITS survey data analysis
        MtoSurveyData data = SurveyDataImporter.importData(rb);
        MtoAnalyzeData ld = new MtoAnalyzeData(rb, data);
        ld.runAnalyses();
        logger.info("Module runDataAnalysis completed.");
    }


    private void runLongDistModel() {
        // main method to run long-distance model
        logger.info("Started runLongDistModel for the year " + year);
        DataSet dataSet = new DataSet();



        String inputFolder = "";
        String outputFolder = "./output/";

        LDModel ldModel = new LDModel();

        ldModel.setup(prop, inputFolder, outputFolder);
        ldModel.load(dataSet);
        ldModel.run(dataSet, -1);

        logger.info("Module runLongDistModel completed.");

    }






    public static boolean getWinter() { return winter; }
}

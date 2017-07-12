package de.tum.bgu.msm;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.MtoAnalyzeData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.MtoSurveyData;
import de.tum.bgu.msm.longDistance.MtoLongDistance;

import org.apache.log4j.Logger;


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


    private static int year;
    private static boolean winter;


    private Mto(ResourceBundle rb) {
        // constructor
        this.rb = rb;
    }


    public static void main(String[] args) {
        // main model run method

        logger.info("Ontario Provincial Model (MTO)");
        // Check how many arguments were passed in
        if(args.length != 2)
        {
            logger.error("Error: Please provide two parguments, 1. the model resources, 2. the start year");
            System.exit(0);
        }
        long startTime = System.currentTimeMillis();
        ResourceBundle rb = Util.mtoInitialization(args[0]);

        //todo test version of getting json properties
        JsonUtilMto prop = new JsonUtilMto("./file.json");
        System.out.println(prop.bool("run.full_model"));
        System.out.println(prop.stri("zone_files.external.us"));
        System.out.println(prop.lon("year"));
        System.out.println(prop.dble("alpha"));

        year = Integer.parseInt(args[1]);
        winter = ResourceUtil.getBooleanProperty(rb,"winter",false);

        Mto model = new Mto(rb);
        if (ResourceUtil.getBooleanProperty(rb, "analyze.survey.data", false)) model.runDataAnalysis();
        if (ResourceUtil.getBooleanProperty(rb, "run.long.dist.mod", true)) model.runLongDistModel();

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
        MtoLongDistance ld = new MtoLongDistance(rb);
        ld.runLongDistanceModel();
        logger.info("Module runLongDistModel completed.");

    }


    public static int getYear() {
        return year;
    }

    public static boolean getWinter() { return winter; }
}

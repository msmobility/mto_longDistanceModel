package de.tum.bgu.msm;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.mtoAnalyzeData;
import de.tum.bgu.msm.dataAnalysis.mtoSurveyData;
import de.tum.bgu.msm.longDistance.mtoLongDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class mto {
    // main class
    private static Logger logger = LogManager.getLogger(mto.class);
    private ResourceBundle rb;
    private static int year;


    private mto(ResourceBundle rb) {
        // constructor
        this.rb = rb;
    }


    public static void main(String[] args) {
        // main model run method

        logger.info("Ontario Provincial Model (MTO)");
        long startTime = System.currentTimeMillis();
        ResourceBundle rb = util.mtoInitialization(args[0]);
        year = Integer.parseInt(args[1]);

        mto model = new mto(rb);
        if (ResourceUtil.getBooleanProperty(rb, "analyze.tsrc.data", false)) model.runDataAnalysis();
        if (ResourceUtil.getBooleanProperty(rb, "run.long.dist.mod", true)) model.runLongDistModel();

        float endTime = util.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes.");
    }


    private void runDataAnalysis() {
        // main method to run data analysis

        mtoSurveyData data = new mtoSurveyData(rb);
        data.readInput();
        mtoAnalyzeData ld = new mtoAnalyzeData(rb, data);
        ld.runAnalyses();
        logger.info("Module runDataAnalysis completed.");
    }


    private void runLongDistModel() {
        // main method to run long-distance model
        logger.info("Started runLongDistModel for the year " + year);
        mtoLongDistance ld = new mtoLongDistance(rb);
        ld.runLongDistanceModel();
        logger.info("Module runLongDistModel completed.");
    }


    public static int getYear() {
        return year;
    }
}

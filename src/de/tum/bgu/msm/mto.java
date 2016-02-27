package de.tum.bgu.msm;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
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
    static Logger logger = Logger.getLogger(mto.class);
    private ResourceBundle rb;


    public mto(ResourceBundle rb) {
        // contructor
        this.rb = rb;
    }


    public static void main(String[] args) {
        // main run method

        ResourceBundle rb = util.mtoInitialization(args[0]);
        mto model = new mto(rb);
        model.run();
    }


    public void run () {
        // main run method
        logger.info("Ontario Provincial Model (MTO)");
        long startTime = System.currentTimeMillis();
        mtoData data = new mtoData(rb);
        data.readInput();
        mtoLongDistance ld = new mtoLongDistance(rb, data);
        ld.runLongDistanceModel();

        float endTime = util.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes.");
        logger.info("Model run completed.");
    }
}

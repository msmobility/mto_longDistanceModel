package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.syntheticPopulation.readSP;
import org.apache.log4j.Logger;

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
        mtoLongDistData md = new mtoLongDistData(rb);
        md.readSkim();
        md.calculateAccessibility(rsp);
    }
}

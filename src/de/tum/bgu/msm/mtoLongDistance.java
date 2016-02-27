package de.tum.bgu.msm;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Class to simulate long-distance travel
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 *
 */
public class mtoLongDistance {
    static Logger logger = Logger.getLogger(mto.class);
    private ResourceBundle rb;
    private mtoData data;


    public mtoLongDistance(ResourceBundle rb, mtoData data) {
        this.rb = rb;
        this.data = data;
    }


    public void runLongDistanceModel() {
        // run long-distance model

    }

}

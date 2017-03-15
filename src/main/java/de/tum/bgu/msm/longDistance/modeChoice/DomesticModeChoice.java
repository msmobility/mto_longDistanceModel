package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.LongDistanceTrip;

import java.util.ResourceBundle;

/**
 * Created by carlloga on 15.03.2017.
 */
public class DomesticModeChoice {

    ResourceBundle rb;
    //distance and price matrices
    //coefficients table

    public DomesticModeChoice(ResourceBundle rb) {
        this.rb = rb;

    }

    //constructor with read coefficients and matrices

    public int selectMode(LongDistanceTrip trip) {

        int mode = 0;

        //for each mode/trip calculate utility

        //transfor utility to porbability

        //sample and pick up mode

        return mode;


    }

    public double calculateUtility(LongDistanceTrip trip, int destination) {

        double utility = Double.NEGATIVE_INFINITY;


        return utility;

    }


}

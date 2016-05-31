package de.tum.bgu.msm.dataAnalysis;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold trip object of the ITS survey
 *
 * @author Rolf Moeckel
 * Created on 25 May 2016 in Munich, Germany
 *
 **/

public class surveyIntTravel implements Serializable {

    static Logger logger = Logger.getLogger(surveyIntTravel.class);

    private static final Map<Integer,surveyIntTravel> intTripMap = new HashMap<>();
    int refYear;


    public surveyIntTravel(int pumfId, int refYear) {
        // constructor of new survey person

        this.refYear = refYear;
        intTripMap.put(pumfId,this);
    }
}

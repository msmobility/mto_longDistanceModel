package de.tum.bgu.msm.longDistance.timeOfDay;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class TimeOfDayChoice implements ModelComponent {

    private static Logger logger = Logger.getLogger(TimeOfDayChoice.class);
    private int[] departureTimesInHours; //float to use fractions of hour if needed
    private double[] probabilitiesAirRail;
    private double[] probabilitiesAutoBus;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        departureTimesInHours = JsonUtilMto.getArrayIntProp(prop, "departure_time.intervals");
        probabilitiesAirRail = JsonUtilMto.getArrayDoubleProp(prop, "departure_time.probabilities_air_rail");
        checkLengthOfArrays(departureTimesInHours, probabilitiesAirRail);
        probabilitiesAutoBus = JsonUtilMto.getArrayDoubleProp(prop, "departure_time.probabilities_auto_bus");
        checkLengthOfArrays(departureTimesInHours, probabilitiesAutoBus);

    }

    @Override
    public void load(DataSet dataSet) {

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        ArrayList<LongDistanceTrip> trips = dataSet.getAllTrips();
        logger.info("Running time-of-day choice for " + trips.size() + " trips");

        trips.parallelStream().forEach(trip -> {
            if (trip.getTripState() == 0){
                trip.setDepartureTimeInHours(-1);
            } else {
                if (trip.getMode() == 0 | trip.getMode() == 3){
                    trip.setDepartureTimeInHours(Util.select(probabilitiesAutoBus, departureTimesInHours));
                } else {
                    trip.setDepartureTimeInHours(Util.select(probabilitiesAirRail, departureTimesInHours));
                }
            }
        });

        logger.info("Finished time-of-day choice");


    }

    public void checkLengthOfArrays(int[] array1, double[] array2){
        if (array1.length != array2.length && array1.length != 24){
            logger.error("Incorrect length of the departure time distributions: time intervals = " + array1.length + " probabilties = " + array2.length);
            throw new RuntimeException();
        }


    }
}

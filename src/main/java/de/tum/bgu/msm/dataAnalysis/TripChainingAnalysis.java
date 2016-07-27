package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.surveyModel.mtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.surveyTour;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Joe on 27/07/2016.
 */
public class TripChainingAnalysis {

    private final ResourceBundle rb;
    private final Logger logger = Logger.getLogger(this.getClass());

    public TripChainingAnalysis(ResourceBundle rb) {
        this.rb = rb;
    }

    public static void main (String[] args) {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        TripChainingAnalysis tca = new TripChainingAnalysis(rb);
        tca.run();

    }

    private void run() {
        mtoSurveyData data = SurveyDataImporter.importData(rb);
        Stream<surveyTour> allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        //Map<Long, List<surveyTour>> cmaHistogram = allTours.filter(t -> t.getHomeCma() > 0).collect(Collectors.groupingBy(surveyTour::getDistinctNumRegions));
        List<surveyTour> oneWayTrips = allTours.filter(t -> !t.isReturnTrip()).collect(Collectors.toList());


        allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        Map<String, int[]> tripStops = allTours.collect(Collectors.toMap(surveyTour::getUniqueId, surveyTour::getTourStops));

        logger.info(oneWayTrips.size());

    }

}

package de.tum.bgu.msm.dataAnalysis;

import com.vividsolutions.jts.geom.Geometry;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyVisit;
import de.tum.bgu.msm.dataAnalysis.surveyModel.mtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.surveyTour;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;
import com.vividsolutions.jts.geom.LineString;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
        List<surveyTour> oneWayTrips = allTours.filter(t -> !t.isReturnTrip()).collect(toList());

        allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        Map<surveyTour, SurveyVisit[]> tripStops = allTours.collect(Collectors.toMap(t -> t, surveyTour::getTourStops));


        List<surveyTour> ontarioTrips = data.getPersons().stream()
                .flatMap(p -> p.getTours().stream())
                .filter(p -> p.getStops().stream().anyMatch(v -> v.stopInProvince(35)))
                .filter(p -> p.getStops().stream().allMatch(SurveyVisit::cdStated))
                .collect(Collectors.toList());


        Map<LineString, List<LineString>> uniqueTours =
                ontarioTrips.stream()
                .map(st -> st.generateTourLineString(data))
                        .collect(Collectors.groupingBy(i -> i));

        util.outputTourCounts(data, "output/tripCounts.csv", uniqueTours);

        logger.info(oneWayTrips.size());

    }

}

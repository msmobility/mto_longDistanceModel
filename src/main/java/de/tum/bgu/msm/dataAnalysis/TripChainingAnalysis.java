package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyVisit;
import de.tum.bgu.msm.dataAnalysis.surveyModel.mtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.surveyTour;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
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

        Map<String, Long> histo = tripStops.values().stream()
                .filter(stops -> Arrays.stream(stops).anyMatch(v -> v.stopInProvince(35)))
                .filter(stops -> Arrays.stream(stops).allMatch(SurveyVisit::cdStated))
                .map(stops -> Arrays.stream(stops).map(v -> (util.cdToXY(data, v.getUniqueCD()))).collect(toList()))
                .filter(coordinates -> coordinates.stream().allMatch(x -> !x.isEmpty()))
                //.filter(a1 -> Arrays.stream(a1).distinct().count() == 1)
                .map(util::buildTourWKT)
                .collect(groupingBy(a -> a, HashMap::new, counting()));

        util.outputCsvTours(data, "output/tripChaining.csv", tripStops);
        util.outputTourCounts(data, "output/tripCounts.csv", histo);

        logger.info(oneWayTrips.size());

    }

}

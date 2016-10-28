package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyVisit;
import de.tum.bgu.msm.dataAnalysis.surveyModel.MtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyTour;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
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

        ResourceBundle rb = Util.mtoInitialization(args[0]);

        TripChainingAnalysis tca = new TripChainingAnalysis(rb);
        tca.run();

    }

    private void run() {
        MtoSurveyData data = SurveyDataImporter.importData(rb);
        Stream<SurveyTour> allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        //Map<Long, List<surveyTour>> cmaHistogram = allTours.filter(t -> t.getHomeCma() > 0).collect(Collectors.groupingBy(surveyTour::getDistinctNumRegions));

        allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        Map<SurveyTour, ArrayList<SurveyVisit>> tripStops = allTours.collect(Collectors.toMap(t -> t, SurveyTour::getStops));

        //TODO: hat about summed weights?
        List<SurveyTour> ontarioTrips = data.getPersons().stream()
                .flatMap(p -> p.getTours().stream())
                //.filter(t -> t.getMainMode() == 1 || t.getMainMode() == 3 || t.getMainMode() == 4) //1: Car, 3: RV, 4: Bus
                //.filter(t -> t.getMainModeStr().equals("Auto") || t.getMainModeStr().equals("Air")) //2: Air, 5: Train
                .filter(t -> t.getMainModeStr().equals("Auto")) //2: Air, 5: Train
                .filter(t -> t.getOrigProvince() == 35)
                .filter(t -> t.getUniqueOrigCD() != 5925)
                .filter(t -> t.getStops().stream().allMatch(SurveyVisit::cdStated))
                //.filter(t -> t.getStops().size() == 4 && t.getMainModeStr().equals("Air"))
                //.filter(t -> t.getStops().stream().filter(v -> !v.airport.equals("000")).count() > 2) //odd airport
                .filter(t -> t.getStops().size() > 2)
                .filter(t -> t.getStops().stream().filter(v -> v.nights > 0).count() > 2)
                .collect(Collectors.toList());

        logger.info("Number of trips passing through ontario: " + ontarioTrips.size());


        //only ontario:
        List<SurveyTour> internalOntarioTrips = data.getPersons().stream()
                .flatMap(p -> p.getTours().stream())
                .filter(t -> t.getOrigProvince() == 35)
                .filter(t -> t.getUniqueOrigCD() != 5925)
                .filter(t -> t.getStops().stream().allMatch(v -> v.province == 35))
                .filter(t -> t.getStops().size() > 2)
                .collect(Collectors.toList());

        Map<String, List<SurveyTour>> uniqueTours =
                ontarioTrips.stream()
                .collect(Collectors.groupingBy(st -> String.valueOf(st.getMainMode())+st.generateTourLineString(data)));

        String output_filename = rb.getString("output.folder") + File.separator + "tripCounts.csv";
                Util.outputTourCounts(data, output_filename, uniqueTours);

    }

}

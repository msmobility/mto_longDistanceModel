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

        allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        Map<surveyTour, ArrayList<SurveyVisit>> tripStops = allTours.collect(Collectors.toMap(t -> t, surveyTour::getStops));

        //TODO: hat about summed weights?
        List<surveyTour> ontarioTrips = data.getPersons().stream()
                .flatMap(p -> p.getTours().stream())
                //.filter(t -> t.getMainMode() == 1 || t.getMainMode() == 3 || t.getMainMode() == 4) //1: Car, 3: RV, 4: Bus
                .filter(t -> t.getMainModeStr().equals("Auto") || t.getMainModeStr().equals("Air")) //2: Air, 5: Train
                .filter(t -> t.getStops().stream().anyMatch(v -> v.stopInProvince(35)))
                .filter(t -> t.getUniqueOrigCD() != 5925)
                .filter(t -> t.getStops().stream().allMatch(SurveyVisit::cdStated))
               // .filter(t -> t.getStops().size() == 4 && t.getMainModeStr().equals("Air"))
                .collect(Collectors.toList());

        logger.info("Number of trips passing through ontario: " + ontarioTrips.size());


        Map<String, List<surveyTour>> uniqueTours =
                ontarioTrips.stream()
                .collect(Collectors.groupingBy(st -> String.valueOf(st.getMainMode())+st.generateTourLineString(data)));

        util.outputTourCounts(data, "output/tripCounts.csv", uniqueTours);

    }

}

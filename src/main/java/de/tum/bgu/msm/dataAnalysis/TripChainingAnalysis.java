package de.tum.bgu.msm.dataAnalysis;

import com.vividsolutions.jts.geom.Geometry;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.Survey;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyVisit;
import de.tum.bgu.msm.dataAnalysis.surveyModel.mtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.surveyTour;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;
import com.vividsolutions.jts.geom.LineString;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private final mtoSurveyData data;

    public TripChainingAnalysis(ResourceBundle rb) {
        this.rb = rb;
        this.data = SurveyDataImporter.importData(rb);

    }

    public static void main (String[] args) throws Exception {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        TripChainingAnalysis tca = new TripChainingAnalysis(rb);
        //tca.run();
        //DatabaseInteractions.loadPersonsToDb(tca.data);
        DatabaseInteractions.loadTripsToDb(tca.data);
        //DatabaseInteractions.loadVisitsToDb(tca.data);
        //tca.outputTourList();

    }

    private void run() {
        Stream<surveyTour> allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        //Map<Long, List<surveyTour>> cmaHistogram = allTours.filter(t -> t.getHomeCma() > 0).collect(Collectors.groupingBy(surveyTour::getDistinctNumRegions));
        allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        Map<surveyTour, ArrayList<SurveyVisit>> tripStops = allTours.collect(Collectors.toMap(t -> t, surveyTour::getStops));

        //TODO: hat about summed weights?
        List<surveyTour> ontarioTrips = data.getPersons().stream()
                .flatMap(p -> p.getTours().stream())
                //.filter(t -> t.getMainMode() == 1 || t.getMainMode() == 3 || t.getMainMode() == 4) //1: Car, 3: RV, 4: Bus
                //.filter(t -> t.getMainModeStr().equals("Auto") || t.getMainModeStr().equals("Air")) //2: Air, 5: Train
                //.filter(t -> t.getMainModeStr().equals("Auto")) //2: Air, 5: Train
                .filter(t -> t.getOrigProvince() == 35)
                .filter(t -> t.getUniqueOrigCD() != 5925)
                .filter(t -> t.getStops().stream().allMatch(SurveyVisit::cdStated))
                //.filter(t -> t.getStops().size() == 4 && t.getMainModeStr().equals("Air"))
                .filter(t -> t.getStops().stream().filter(v -> !v.airport.equals("000")).count() > 2) //odd airport
                .filter(t -> t.getStops().size() > 2)
                .filter(t -> t.getStops().stream().filter(v -> v.nights > 0).count() > 2)
                .collect(Collectors.toList());

        logger.info("Number of trips passing through ontario: " + ontarioTrips.size());

        Map<String, List<surveyTour>> uniqueTours =
                ontarioTrips.stream()
                .collect(Collectors.groupingBy(st -> String.valueOf(st.getMainMode())+st.generateTourLineString(data)));

        String output_filename = rb.getString("output.folder") + File.separator + "tripCounts.csv";
                util.outputTourCounts(data, output_filename, uniqueTours);

    }

    public void outputTourList() {
        //trips by mode, #legs (exclude airport visits), maximum distance, total tour distance
        ArrayList<String[]> tourLengths = new ArrayList<>();

        List<surveyTour> tours = data.getPersons()
                .stream()
                .flatMap(p -> p.getTours().stream())
                //.filter(t -> t.getOrigCD() != t.getDestCD()) //only want long distance trips
                .filter(t -> t.getDistance() >= 80 && t.getDistance() != 99999) //only want long distance trips
                .collect(Collectors.toList());

        String headers = "year,month,pumfid,tripid,origin_prov, origin_cd, origin_cma, dest_prov, des_cd, dest_cma, weight, purpose, mode, distance, tour_distance, legs, type";
        String filename = rb.getString("output.folder") + File.separator + "trip_list.csv";

        try {
            FileWriter writer = new FileWriter(filename);
            //write headers
            writer.write(headers);
            writer.write("\n");

            for (surveyTour t : tours) {
                //where distance == 99999?
                int i=0;
                String[] row = new String[17];
                row[i++] = Integer.toString(t.getPerson().getRefYear());
                row[i++] = Integer.toString(t.getPerson().getRefMonth());
                row[i++] = Long.toString(t.getPerson().getPumfId());
                row[i++] = Integer.toString(t.getTripId());
                row[i++] = Integer.toString(t.getOrigProvince());
                row[i++] = Integer.toString(t.getOrigProvince()*100 + t.getOrigCD());
                row[i++] = Integer.toString(t.getOrigCma());
                row[i++] = Integer.toString(t.getDestProvince());
                row[i++] = Integer.toString(t.getDestProvince()*100 + t.getDestCD());
                row[i++] = Integer.toString(t.getDestCma());
                row[i++] = Double.toString(t.getWeight());
                row[i++] = Integer.toString(t.getTripPurp());
                row[i++] = t.getMainModeStr();
                row[i++] = Integer.toString(t.getDistance());
                row[i++] = Integer.toString(t.getTourDistance());
                row[i++] = Integer.toString(t.getNumberOfStop());
                row[i++] = Integer.toString(t.getTripType());

                writer.write(Arrays.stream(row).collect(Collectors.joining(",")));
                writer.write("\n");

            }

        } catch (IOException e) {
            logger.error(e);
        }



        //are trips also return
    }

}

package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by Joe on 14/12/2016.
 */
public class Scenario {

    ZonalData zonalData;
    DataSet dataSet;
    JSONObject prop;
    ResourceBundle rb;
    private DomesticDestinationChoice dcModel;
    private DomesticModeChoice mcModel;
    Logger logger = Logger.getLogger(Scenario.class);
    ArrayList<LongDistanceTrip> allTrips;

    public Scenario(ResourceBundle rb, JSONObject prop) {

        String inputFolder = null;
        String outputFolder = null;

        this.rb = rb;
        this.prop = prop;
        this.dataSet = dataSet;
        zonalData = new ZonalData();
        zonalData.setup(prop, inputFolder, outputFolder);
        mcModel = new DomesticModeChoice(prop);
        dcModel = new DomesticDestinationChoice(prop);

    }

    public static void main(String[] args) {
        ResourceBundle rb = Util.mtoInitialization(args[0]);
        JsonUtilMto jsonUtilMto = new JsonUtilMto("./javaFiles/properties.json");
        JSONObject prop = jsonUtilMto.getJsonProperties();

        Scenario scenario = new Scenario(rb, prop);


        scenario.iterate();

        //scenario.writeTrips();
    }

    void iterate() {
        int iterations = 20;
        int[][] purpose_counter = new int[3][iterations];
        loadtrips();
        logger.info("Running Destination Choice Model for " + allTrips.size() + " trips, " + iterations + " times...");

        IntStream.range(0, iterations).forEach(i -> {
            runDestinationChoice(allTrips);

            allTrips.stream().filter(t -> t.getDestCombinedZoneId() == 24)
                    .forEach(t -> {
                        purpose_counter[t.getTripPurpose()][i] += 1;
                    });

            logger.info(String.format("%d,%d,%d",
                    purpose_counter[1][i], purpose_counter[2][i], purpose_counter[0][i]));

        });

        for (int p = 0; p < 3; p++) {
            int average = (int) Arrays.stream(purpose_counter[p]).average().getAsDouble();
            int min = Arrays.stream(purpose_counter[p]).min().getAsInt();
            int max = Arrays.stream(purpose_counter[p]).max().getAsInt();
            System.out.println(String.format("%s,%d,%d,%d", ZonalData.getTripPurposes().get(p), average, min, max));

        }

        writeTrips();

    }

    private void loadtrips() {
        allTrips = new ArrayList<>();
        logger.info("\tLoading trips");
        TableDataSet tripsDomesticTable = Util.readCSVfile(rb.getString("scenario.trip.file"));
        tripsDomesticTable.buildIndex(1);
        double num_trips = 1000000; //no other
        int[] tripIds = tripsDomesticTable.getColumnAsInt("id");
        EnumeratedIntegerDistribution tripSelector = new EnumeratedIntegerDistribution(tripIds, tripsDomesticTable.getColumnAsDouble("wtep"));
        for (int j=1; j<=num_trips; j++) {
            int tripId = tripSelector.sample();

            int origZoneId = (int) tripsDomesticTable.getIndexedValueAt(tripId, "lvl2_orig");
            //int num_trips = (int) tripsDomesticTable.getValueAt(i, "wtep") / (365*4);
            String purpose = tripsDomesticTable.getIndexedStringValueAt(tripId, "purpose");
            String season = tripsDomesticTable.getIndexedStringValueAt(tripId, "season");
            boolean is_summer = "summer".equals(season);
            int purpose_int = 0;
            switch (purpose) {
                case "Leisure": purpose_int = 2; break;
                case "Visit":purpose_int = 0; break;
                case "Business": purpose_int = 1; break;
                case "other" : purpose_int = 3; break;
            }

            Zone dummyZone = new Zone(0,0,0, ZoneType.ONTARIO, origZoneId);

            LongDistanceTrip ldt = new LongDistanceTrip(null, false, purpose_int, 0, dummyZone, is_summer, 0, 0, 0, 0);
            allTrips.add(ldt);

        }

    }

    private void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        trips.parallelStream().forEach( t -> { //Easy parallel makes for fun times!!!
            if (!t.isInternational()) {
                int destZoneId = dcModel.selectDestination(t);
                t.setCombinedDestZoneId(destZoneId);
            }
        });
    }
    private void writeTrips() {
        logger.info("Writing out data for trip generation (trips)");

        String OutputTripsFileName = rb.getString("scenario.out.file");
        PrintWriter pw = Util.openFileForSequentialWriting(OutputTripsFileName, false);

        pw.println(LongDistanceTrip.getHeader());
        for (LongDistanceTrip tr : allTrips) {
            pw.println(tr.toString());
        }
        pw.close();
    }
}

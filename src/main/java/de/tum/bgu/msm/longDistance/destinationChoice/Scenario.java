package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.Mto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.Person;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.log4j.Logger;
import org.apache.commons.math3.util.Pair;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Joe on 14/12/2016.
 */
public class Scenario {

    MtoLongDistData mtoLongDistData;
    ResourceBundle rb;
    private DomesticDestinationChoice dcModel;
    Logger logger = Logger.getLogger(Scenario.class);
    ArrayList<LongDistanceTrip> allTrips;

    public Scenario(ResourceBundle rb) {

        this.rb = rb;
        mtoLongDistData = new MtoLongDistData(rb);
        dcModel = new DomesticDestinationChoice(rb);

    }

    public static void main(String[] args) {
        ResourceBundle rb = Util.mtoInitialization(args[0]);

        Scenario scenario = new Scenario(rb);


        scenario.iterate();

        //scenario.writeTrips();
    }

    void iterate() {
        int iterations = 1;
        int[][] purpose_counter = new int[3][iterations];
        loadtrips();
        logger.info("Running Destination Choice Model for " + allTrips.size() + " trips, " + iterations + " times...");

        IntStream.range(0, iterations).forEach(i -> {
            runDestinationChoice(allTrips);

            allTrips.stream().filter(t -> t.getDestZoneId() == 24)
                    .forEach(t -> {
                        purpose_counter[t.getLongDistanceTripPurpose()][i] += 1;
                    });

            logger.info(String.format("%d, %d, %d",
                    purpose_counter[1][i], purpose_counter[2][i], purpose_counter[0][i]));

        });

        for (int p = 0; p < 3; p++) {
            int average = (int) Arrays.stream(purpose_counter[p]).average().getAsDouble();
            int min = Arrays.stream(purpose_counter[p]).min().getAsInt();
            int max = Arrays.stream(purpose_counter[p]).max().getAsInt();
            System.out.println(String.format("%s, %d, %d, %d", MtoLongDistData.getTripPurposes().get(p), average, min, max));

        }

        writeTrips();

    }

    void loadtrips() {
        allTrips = new ArrayList<>();
        logger.info("\tLoading trips");
        TableDataSet tripsDomesticTable = Util.readCSVfile(rb.getString("scenario.trip.file"));
        double num_trips = 1000000; //no other

        List<Pair<Integer, Double>> tripWeights = IntStream.rangeClosed(1,tripsDomesticTable.getRowCount())
                .mapToObj(i -> new Pair<>(i, (double)tripsDomesticTable.getValueAt(i, "wtep"))).collect(Collectors.toList());
        EnumeratedDistribution<Integer> tripSelector = new EnumeratedDistribution<>(tripWeights);
        for (int j=1; j<=num_trips; j++) {
            int tripIndex = tripSelector.sample();

            int origZoneId = (int) tripsDomesticTable.getValueAt(tripIndex, "lvl2_orig");
            //int num_trips = (int) tripsDomesticTable.getValueAt(i, "wtep") / (365*4);
            String purpose = tripsDomesticTable.getStringValueAt(tripIndex, "purpose");
            String season = tripsDomesticTable.getStringValueAt(tripIndex, "season");
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

    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        trips.parallelStream().forEach( t -> { //Easy parallel makes for fun times!!!
            if (!t.isLongDistanceInternational()) {
                int destZoneId = dcModel.selectDestination(t);
                t.setDestination(destZoneId);
            }
        });
    }
    public void writeTrips() {
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

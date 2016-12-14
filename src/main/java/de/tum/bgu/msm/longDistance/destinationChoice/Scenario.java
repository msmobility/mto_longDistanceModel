package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.Person;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.ResourceBundle;
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

        IntStream.range(0, iterations).forEach(i -> {
            run();

            allTrips.stream().filter(t -> t.getDestZoneId() == 24)
                    .forEach(t -> {
                        purpose_counter[t.getLongDistanceTripPurpose()][i] += 1;
                    });

        //    logger.info(String.format("%d, %d, %d",
        //            purpose_counter[0][i], purpose_counter[1][i], purpose_counter[2][i]));

        });

        logger.info(String.format("\n" + "\tbusiness: %d\n" +"\tleisure:  %d\n\tvisit: \t  %d",
                (int) Arrays.stream(purpose_counter[1]).average().getAsDouble(),
                (int) Arrays.stream(purpose_counter[2]).average().getAsDouble(),
                (int) Arrays.stream(purpose_counter[0]).average().getAsDouble()
        ));
        writeTrips();

    }

    void run() {
        allTrips = new ArrayList<>();

        TableDataSet tripsDomesticTable = Util.readCSVfile("C:\\Users\\Joe\\canada\\data\\mnlogit\\mnlogit_trips_no_intra_province.csv");
        Map<Integer, Zone>  zoneLookup = mtoLongDistData.getZoneLookup();
        for (int i=1; i<=tripsDomesticTable.getRowCount(); i++) {

            int origZoneId = (int) tripsDomesticTable.getValueAt(i, "lvl2_orig");
            String purpose = tripsDomesticTable.getStringValueAt(i, "purpose");
            String season = tripsDomesticTable.getStringValueAt(i, "season");
            boolean is_summer = "summer".equals(season);
            int purpose_int = 0;
            switch (purpose) {
                case "Leisure": purpose_int = 2; break;
                case "Visit":purpose_int = 0; break;
                case "Business": purpose_int = 1; break;
                case "other" : purpose_int = 3; break;
            }

            Zone dummyZone = new Zone(0,0,0, ZoneType.ONTARIO, origZoneId);

            if (purpose_int < 3) {
                LongDistanceTrip ldt = new LongDistanceTrip(null, false, purpose_int, 0, dummyZone, is_summer,0 , 0, 0, 0);
                allTrips.add(ldt);
            }
        }

        runDestinationChoice(allTrips);


    }

    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
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

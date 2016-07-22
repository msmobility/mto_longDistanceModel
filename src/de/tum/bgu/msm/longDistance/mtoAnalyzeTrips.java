package de.tum.bgu.msm.longDistance;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.mtoAnalyzeData;
import de.tum.bgu.msm.util;

import java.io.PrintWriter;
import java.util.ResourceBundle;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.syntheticPopulation.*;
import static de.tum.bgu.msm.syntheticPopulation.Person.*;

import org.apache.log4j.Logger;


/**
 * Created by Carlos Llorca on 7/5/2016.
 * Technical University of Munich
 *
 * class to analyze the trips from tripGeneration by writing them into CSV files
 *
 */
public class mtoAnalyzeTrips {
    private ResourceBundle rb;
    static Logger logger = Logger.getLogger(mtoAnalyzeTrips.class);


    public mtoAnalyzeTrips(ResourceBundle rb) {
        this.rb = rb;
    }

    public void runMtoAnalyzeTrips() {
        logger.info("Writing out data for trip generation (trips)");
        //TODO add this file path and name to mto_properties?
        String OutputTripsFileName = "output/trips";
                PrintWriter pw = util.openFileForSequentialWriting(OutputTripsFileName + ".csv", false);

        pw.print("tripId, personId, international, tripPurpose, tripState, tripOriginZone, numberOfNights, travelParty");
        pw.println();
        for (LongDistanceTrip tr : LongDistanceTrip.getLongDistanceTripArray()) {

            pw.print(tr.getLongDistanceTripId() + "," + tr.getPersonId() + "," + tr.isLongDistanceInternational() + "," +
                    tr.getLongDistanceTripPurpose() + "," + tr.getLongDistanceTripState() + "," + tr.getLongDistanceOriginZone()+ "," + tr.getLongDistanceNights() + "," + tr.getTravelParty());
            pw.println();
        }
        pw.close();
        //todo include some person-related variables to analyze income, age, etc. of travellers and compare it to the synthetic/survey population


        logger.info("Writing out data for trip generation (travellers)");
        //TODO add this file path and name to mto_properties?
        String OutputTravellersFilename = "output/travellers";
        PrintWriter pw2 = util.openFileForSequentialWriting(OutputTravellersFilename + ".csv", false);

        pw2.print("personId, away, daytrip, inOutTrip");
        pw2.println();
        for (Person trav : getSyntheticPersonArray()) {
            //takes only persons travelling
            if (trav.isAway  | trav.isDaytrip  | trav.isInOutTrip ) {
                pw2.print(trav.getPersonId() + "," + trav.isAway + "," + trav.isDaytrip + "," + trav.isInOutTrip );
                pw2.println();
            }
        }
        pw2.close();
    }
}

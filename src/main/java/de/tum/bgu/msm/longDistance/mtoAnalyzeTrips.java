package de.tum.bgu.msm.longDistance;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.mtoAnalyzeData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.util;

import java.io.PrintWriter;
import java.util.*;

import de.tum.bgu.msm.*;
import de.tum.bgu.msm.syntheticPopulation.*;
import static de.tum.bgu.msm.syntheticPopulation.Person.*;
import static de.tum.bgu.msm.syntheticPopulation.Household.*;

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

    private List<String> tripPurposes = mtoLongDistance.getTripPurposes();
    private List<String> tripStates = mtoLongDistance.getTripStates();

    public mtoAnalyzeTrips(ResourceBundle rb) {
        this.rb = rb;
    }

    public void runMtoAnalyzeTrips(ArrayList<LongDistanceTrip> trips, ArrayList<Zone> zoneList) {
        logger.info("Writing out data for trip generation (trips)");

        String OutputTripsFileName = rb.getString("trip.out.file");
                PrintWriter pw = util.openFileForSequentialWriting(OutputTripsFileName, false);


        pw.print("tripId, personId, international, tripPurpose, tripState, tripOriginZone, tripOriginType, numberOfNights, hhAdultsTravelParty, hhKidsTravelParty, nonHhTravelParty, personAge, personGender, " +
                "personEducation, personWorkStatus, personIncome, adultsInHh, kidsInHh");
        pw.println();
        for (LongDistanceTrip tr : trips) {

            if (tr.getLongDistanceOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
                Person traveller = getPersonFromId(tr.getPersonId());

                pw.print(tr.getLongDistanceTripId() + "," + tr.getPersonId() + "," + tr.isLongDistanceInternational() + "," +
                        tripPurposes.get(tr.getLongDistanceTripPurpose()) + "," + tripStates.get(tr.getLongDistanceTripState()) + "," + tr.getLongDistanceOrigZone().getId() + "," + tr.getLongDistanceOrigZone().getZoneType() + ","
                        + tr.getLongDistanceNights() + "," + tr.getAdultsHhTravelPartySize()
                        + "," + tr.getKidsHhTravelPartySize() + "," + tr.getNonHhTravelPartySize() + "," + traveller.getAge() + "," + traveller.getGender() + "," + traveller.getEducation() + "," + traveller.getWorkStatus() +
                        "," + traveller.getIncome() + "," + traveller.getAdultsHh() + "," + traveller.getKidsHh());
                pw.println();
            } else {
                pw.print(tr.getLongDistanceTripId() + "," + tr.getPersonId() + "," + tr.isLongDistanceInternational() + "," +
                        tripPurposes.get(tr.getLongDistanceTripPurpose()) + "," + tripStates.get(tr.getLongDistanceTripState())+ "," + tr.getLongDistanceOrigZone().getId() + "," + tr.getLongDistanceOrigZone().getZoneType() + ","
                        + tr.getLongDistanceNights() + "," + tr.getAdultsHhTravelPartySize()
                        + "," + tr.getKidsHhTravelPartySize() + "," + tr.getNonHhTravelPartySize());
                pw.println();

            }


            }
        pw.close();

        logger.info("Writing out data for trip generation (travellers)");

        String OutputTravellersFilename = rb.getString("trav.out.file");
        PrintWriter pw2 = util.openFileForSequentialWriting(OutputTravellersFilename, false);

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

        logger.info("Writing out data for trip generation (trips by zone)");


        String OutputZonesFilename = rb.getString("zone.out.file");
        PrintWriter pw3 = util.openFileForSequentialWriting(OutputZonesFilename , false);

        pw3.print("zone, population, domesticVisit, domesticBusiness, domesticLeisure, internationalVisit, internationalBusiness, internationalLeisure");
        pw3.println();

        for (Zone zone : zoneList){
            //todo improve this method because it is too slow
            int visitTrips = 0;
            int businessTrips = 0;
            int leisureTrips = 0;
            int internationalVisitTrips = 0;
            int internationalBusinessTrips = 0;
            int internationalLeisureTrips = 0;

            for (LongDistanceTrip trip : trips) {
                if (zone.equals(trip.getLongDistanceOrigZone())){
                    if (trip.isLongDistanceInternational()) {
                        switch (trip.getLongDistanceTripPurpose()) {
                            case 0:
                                internationalVisitTrips++;
                                break;
                            case 1:
                                internationalBusinessTrips++;
                                break;
                            case 2:
                                internationalLeisureTrips++;
                                break;
                        }
                    } else {
                        switch (trip.getLongDistanceTripPurpose()) {
                            case 0:
                                visitTrips++;
                                break;
                            case 1:
                                businessTrips++;
                                break;
                            case 2:
                                leisureTrips++;
                                break;
                        }
                    }
                }

            }
            pw3.print(zone.getId() + "," + zone.getPopulation() + "," +   visitTrips + "," + businessTrips + "," + leisureTrips + "," +
                    internationalVisitTrips + "," + internationalBusinessTrips + "," + internationalLeisureTrips);
            pw3.println();
        }
        pw3.close();
    }
}

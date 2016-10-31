package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.syntheticPopulation.ReadSP;
import org.apache.commons.math3.analysis.function.Log;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;



/**
 * Created by Joe on 28/10/2016.
 */
public class TripGenerationModel {
    private ResourceBundle rb;
    private MtoLongDistData mtoLongDistData;
    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);

    public TripGenerationModel(ResourceBundle rb, MtoLongDistData mtoLongDistData) {
        this.rb = rb;
        this.mtoLongDistData = mtoLongDistData;

    }

    public ArrayList<LongDistanceTrip> runTripGeneration (ReadSP syntheticPopulation) {
        // main method to run long-distance model

        //read synthetic population

        ArrayList<Zone> zoneList = mtoLongDistData.getZoneList();


        //initialize parameters for accessibility
        List<String> fromZones;
        List<String> toZones;
        float alphaAuto;
        float betaAuto;
        //calculate accessibility (not used in the model, only for external analysis)
        if(ResourceUtil.getBooleanProperty(rb,"analyze.accessibility",false)) {
            //read skims
            //md.readSkim("auto");
            mtoLongDistData.readSkim("transit");
            //input parameters for accessibility calculations from mto properties
            alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
            betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");
            fromZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "orig.zone.type", ",");
            toZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "dest.zone.type", ",");
            mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, alphaAuto, betaAuto);
            mtoLongDistData.writeOutAccessibilities(zoneList);
        }

        ArrayList<LongDistanceTrip> trips_domestic;
        ArrayList<LongDistanceTrip> trips_international;
        ArrayList<LongDistanceTrip> trips_visitors;


        //read skim for auto and run the model
        mtoLongDistData.readSkim("auto");

        //generate domestic trips
        //recalculate accessibility to Canada
        fromZones = Arrays.asList("ONTARIO");
        toZones=Arrays.asList("ONTARIO","EXTCANADA");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.1);
        logger.info("Accessibility for domestic trips from Ontario calculated");
        DomesticTripGeneration tgdomestic = new DomesticTripGeneration(rb);
        trips_domestic = tgdomestic.runTripGeneration(syntheticPopulation);
        logger.info("Domestic Trips from Ontario generated");

        //generate international trips (must be done after domestic)
        //recalculate accessibility to external international zones
        toZones = Arrays.asList("EXTUS","EXTOVERSEAS");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.01);
        logger.info("Accessibility for international trips from Ontario calculated");
        //calculate trips
        InternationalTripGeneration tginternational = new InternationalTripGeneration(rb);
        trips_international = tginternational.runInternationalTripGeneration(syntheticPopulation);
        logger.info("International trips from Ontario generated");

        //generate visitors
        //recalculate accessibility to Ontario
        fromZones = Arrays.asList("ONTARIO","EXTCANADA","EXTUS","EXTOVERSEAS");
        toZones=Arrays.asList("ONTARIO");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.01);
        logger.info("Accessibility for visitors to Ontario calculated");
        VisitorsTripGeneration vgen = new VisitorsTripGeneration(rb);
        trips_visitors = vgen.runVisitorsTripGeneration(mtoLongDistData.getExternalZoneList());
        logger.info("Visitor trips to Ontario generated");

        //analyze and write out generated trips
        //first, join the different list of trips
        ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
        allTrips.addAll(trips_international);
        allTrips.addAll(trips_domestic);
        allTrips.addAll(trips_visitors);

        return allTrips;


    }

    public void runMtoAnalyzeTrips(ArrayList<LongDistanceTrip> trips, ArrayList<Zone> zoneList, ReadSP syntheticPopulation) {
        logger.info("Writing out data for trip generation (trips)");

        String OutputTripsFileName = rb.getString("trip.out.file");
        PrintWriter pw = Util.openFileForSequentialWriting(OutputTripsFileName, false);
        List<String> tripPurposes = mtoLongDistData.tripPurposes;
        List<String> tripStates = mtoLongDistData.tripStates;

        pw.print("tripId,personId,international,tripPurpose,tripState,tripOriginZone,tripOriginCombinedZone,tripOriginType," +
                "numberOfNights,hhAdultsTravelParty,hhKidsTravelParty,nonHhTravelParty,personAge,personGender," +
                "personEducation,personWorkStatus,personIncome,adultsInHh,kidsInHh");
        pw.println();
        for (LongDistanceTrip tr : trips) {

            if (tr.getLongDistanceOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
                Person traveller = tr.getTraveller();

                pw.print(tr.getLongDistanceTripId() + "," + tr.getPersonId() + "," + tr.isLongDistanceInternational() + "," +
                        tripPurposes.get(tr.getLongDistanceTripPurpose()) + "," + tripStates.get(tr.getLongDistanceTripState()) + ","
                        + tr.getLongDistanceOrigZone().getId() + "," + tr.getLongDistanceOrigZone().getCombinedZoneId() + "," + tr.getLongDistanceOrigZone().getZoneType() + ","
                        + tr.getLongDistanceNights() + "," + tr.getAdultsHhTravelPartySize()
                        + "," + tr.getKidsHhTravelPartySize() + "," + tr.getNonHhTravelPartySize() + "," + traveller.getAge() + "," + traveller.getGender() + "," + traveller.getEducation() + "," + traveller.getWorkStatus() +
                        "," + traveller.getIncome() + "," + traveller.getAdultsHh() + "," + traveller.getKidsHh());
                pw.println();
            } else {
                pw.print(tr.getLongDistanceTripId() + "," + tr.getPersonId() + "," + tr.isLongDistanceInternational() + "," +
                        tripPurposes.get(tr.getLongDistanceTripPurpose()) + "," + tripStates.get(tr.getLongDistanceTripState()) + ","
                        + tr.getLongDistanceOrigZone().getId() + "," + tr.getLongDistanceOrigZone().getCombinedZoneId() + "," + tr.getLongDistanceOrigZone().getZoneType() + ","
                        + tr.getLongDistanceNights() + "," + tr.getAdultsHhTravelPartySize()
                        + "," + tr.getKidsHhTravelPartySize() + "," + tr.getNonHhTravelPartySize()
                        + ",-1,\"\",-1,-1,-1,-1,-1");
                pw.println();

            }


        }
        pw.close();

        logger.info("Writing out data for trip generation (travellers)");

        String OutputTravellersFilename = rb.getString("trav.out.file");
        PrintWriter pw2 = Util.openFileForSequentialWriting(OutputTravellersFilename, false);

        pw2.print("personId, away, daytrip, inOutTrip");
        pw2.println();
        for (Person trav : syntheticPopulation.getPersons()) {
            //takes only persons travelling
            if (trav.isAway  | trav.isDaytrip  | trav.isInOutTrip ) {
                pw2.print(trav.getPersonId() + "," + trav.isAway + "," + trav.isDaytrip + "," + trav.isInOutTrip );
                pw2.println();
            }
        }
        pw2.close();

        logger.info("Writing out data for trip generation (trips by zone)");


        String OutputZonesFilename = rb.getString("zone.out.file");
        PrintWriter pw3 = Util.openFileForSequentialWriting(OutputZonesFilename , false);

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

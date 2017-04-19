package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationModel;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;

/**
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 */

public class MtoLongDistance {
    static Logger logger = Logger.getLogger(MtoLongDistance.class);
    private ResourceBundle rb;

    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private MtoLongDistData mtoLongDistData;

    private TripGenerationModel tripGenModel;

    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;

    private DomesticModeChoice mcDomesticModel;

    private SyntheticPopulation syntheticPopulationReader;

    public MtoLongDistance(ResourceBundle rb) {

        this.rb = rb;
        mtoLongDistData = new MtoLongDistData(rb);
        syntheticPopulationReader = new SyntheticPopulation(rb, mtoLongDistData);
        mtoLongDistData.populateZones(syntheticPopulationReader);
        tripGenModel = new TripGenerationModel(rb, mtoLongDistData);

        dcModel = new DomesticDestinationChoice(rb, mtoLongDistData);
        dcOutboundModel = new IntOutboundDestinationChoice(rb, mtoLongDistData);
        dcInBoundModel = new IntInboundDestinationChoice(rb, mtoLongDistData);

        mcDomesticModel = new DomesticModeChoice(rb, mtoLongDistData);

    }

    public void runLongDistanceModel() {

        if (ResourceUtil.getBooleanProperty(rb, "run.trip.gen", false)) {
            allTrips = tripGenModel.runTripGeneration(syntheticPopulationReader);
            //currently only internal zone list

        } else {
            if (ResourceUtil.getBooleanProperty(rb, "run.dest.choice", false)) {

                //load saved trips without destination
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(ResourceUtil.getProperty(rb, "trip.in.file"));

                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader, false);
                    allTrips.add(ldt);

                }
            } else {
                //load saved trip with destinations
                logger.info("Loading generated trips");
                TableDataSet tripsDomesticTable = Util.readCSVfile(ResourceUtil.getProperty(rb, "trip.in.file"));

                for (int i = 0; i < tripsDomesticTable.getRowCount(); i++) {
                    LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i + 1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader, true);
                    allTrips.add(ldt);
                }

            }
        }

        if (ResourceUtil.getBooleanProperty(rb, "run.dest.choice", false)) {
            runDestinationChoice(allTrips);

        }

//        //todo filter trips from selected OD pairs for mode choice scenario
//        ArrayList<LongDistanceTrip> selectedTrips = new ArrayList<>();
//        for (LongDistanceTrip tr : allTrips){
//            if (tr.getDestZoneId()==103 & tr.getOrigZone().getCombinedZoneId()> 18 & tr.getOrigZone().getCombinedZoneId()< 28) {
//                    selectedTrips.add(tr);
//            }
//        }
//        ArrayList<LongDistanceTrip> allTrips = selectedTrips;


        if (ResourceUtil.getBooleanProperty(rb, "run.mode.choice", false)) {
//
            runModeChoice(allTrips);
        }


        if (ResourceUtil.getBooleanProperty(rb, "write.trips", false)) {
            syntheticPopulationReader.writeSyntheticPopulation();
            //writePopByZone();
            writeTrips(allTrips);
        }


    }

    //destination Choice
    //if run trip gen is false, then load trips from file
    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
            if (!t.isLongDistanceInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                t.setDestination(destZoneId);
            } else {
                if (t.getOrigZone().getZoneType() == ZoneType.ONTARIO || t.getOrigZone().getZoneType() == ZoneType.EXTCANADA){
                    int destZoneId = dcOutboundModel.selectDestination(t);
                    t.setDestination(destZoneId);
                } else if (t.getOrigZone().getZoneType() == ZoneType.EXTUS){
                    // visitor trips with an international origin and destination within CANADA
                    int destZoneId = dcInBoundModel.selectDestinationFromUs(t);
                    t.setDestination(destZoneId);
                } else {
                    //visitors from OS
                    int destZoneId = dcInBoundModel.selectDestinationFromOs(t);
                    t.setDestination(destZoneId);
                }
            }

        });
    }


    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
            if (!t.isLongDistanceInternational() && t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
                int mode = mcDomesticModel.selectMode(t);
                t.setMode(mode);
            }

        });
    }


    public void writeTrips(ArrayList<LongDistanceTrip> trips) {
        logger.info("Writing out data for trip generation (trips)");

        String OutputTripsFileName = rb.getString("trip.out.file");
        PrintWriter pw = Util.openFileForSequentialWriting(OutputTripsFileName, false);


        pw.println(LongDistanceTrip.getHeader());


        for (LongDistanceTrip tr : trips) {
            //if (tr.getOrigZone().getZoneType() == ZoneType.ONTARIO){
            pw.println(tr.toString());
        }


        pw.close();
    }

    public void writePopByZone() {
        PrintWriter pw = Util.openFileForSequentialWriting(rb.getString("zone.out.file"), false);
        pw.println("zone,hh,pp");
        for (Zone zone : mtoLongDistData.getInternalZoneList()) {
            pw.println(zone.getId() + "," + zone.getHouseholds() + "," + zone.getPopulation());
        }
        pw.close();
    }


}

package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.VisitorsTripGeneration;
import de.tum.bgu.msm.longDistance.tripGeneration.internationalTripGeneration;
import de.tum.bgu.msm.longDistance.tripGeneration.tripGeneration;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.zoneSystem.mtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 *
 */

public class mtoLongDistance {
    static Logger logger = Logger.getLogger(mtoLongDistance.class);
    private ResourceBundle rb;
    private ArrayList<LongDistanceTrip> trips_domestic;
    private ArrayList<LongDistanceTrip> trips_international;
    private ArrayList<LongDistanceTrip> trips_visitors;
    private final ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private Map<Integer, Zone> zoneLookup;
    private readSP syntheticPopulationReader;
    private ArrayList<Zone> internalZoneList;
    private ArrayList<Zone> externalZoneList;
    private mtoLongDistData mtoLongDistData;
    private DomesticDestinationChoice dcModel;
    private mtoAnalyzeTrips tripAnalysis;

    public mtoLongDistance(ResourceBundle rb) {

        this.rb = rb;
        this.tripAnalysis = new mtoAnalyzeTrips(rb);
        mtoLongDistData = new mtoLongDistData(rb);
        syntheticPopulationReader = new readSP(rb);
        dcModel = new DomesticDestinationChoice(rb);

    }

    //public static ArrayList<String> tripPurposes = new ArrayList<>();
    public static List<String> tripPurposes = Arrays.asList("visit","business","leisure");

    //public static ArrayList<String> tripStates = new ArrayList<>();

    public static List<String> tripStates = Arrays.asList("away","daytrip","inout");



    public void runLongDistanceModel () {

        //read internal zone employment and external zones
        internalZoneList = syntheticPopulationReader.readInternalZones();

        mtoLongDistData.readInternalZonesEmployment(internalZoneList);
        externalZoneList = mtoLongDistData.readExternalZones();

        //join all zones
        ArrayList<Zone> zoneList = new ArrayList<>();
        zoneList.addAll(internalZoneList);
        zoneList.addAll(externalZoneList);

        zoneLookup = zoneList.stream().collect(Collectors.toMap(Zone::getId, x -> x));

        if(ResourceUtil.getBooleanProperty(rb,"run.trip.gen",false)) {
            runTripGeneration();
            //currently only internal zone list
            if(ResourceUtil.getBooleanProperty(rb,"analyze.trips",false)) tripAnalysis.runMtoAnalyzeTrips(allTrips, zoneList);

        } else {
            //load saved trips
            logger.info("Loading generated trips");
            TableDataSet tripsDomesticTable = util.readCSVfile(ResourceUtil.getProperty(rb, "trip.out.file"));
            trips_domestic = new ArrayList<>();
            trips_international = new ArrayList<>();
            trips_visitors = new ArrayList<>();
            for (int i=0; i<tripsDomesticTable.getRowCount(); i++) {
                LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i+1, zoneLookup);
                if (ldt.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
                    if (ldt.isLongDistanceInternational()) trips_international.add(ldt);
                    else  trips_domestic.add(ldt);
                }
                else {
                    trips_visitors.add(ldt);
                }
                allTrips.add(ldt);

            }
        }
        if(ResourceUtil.getBooleanProperty(rb,"run.dest.choice",false)) {
            runDestinationChoice(trips_domestic);

        }


    }


    public void runTripGeneration () {
        // main method to run long-distance model

        //read synthetic population
        syntheticPopulationReader.readSyntheticPopulation(internalZoneList);

        ArrayList<Zone> zoneList = new ArrayList<>(zoneLookup.values());


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


        //read skim for auto and run the model
        mtoLongDistData.readSkim("auto");

        //generate domestic trips
        //recalculate accessibility to Canada
        fromZones = Arrays.asList("ONTARIO");
        toZones=Arrays.asList("ONTARIO","EXTCANADA");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.1);
        logger.info("Accessibility for domestic trips from Ontario calculated");
        tripGeneration tgdomestic = new tripGeneration(rb);
        trips_domestic = tgdomestic.runTripGeneration();
        logger.info("Domestic Trips from Ontario generated");

        //generate international trips (must be done after domestic)
        //recalculate accessibility to external international zones
        toZones = Arrays.asList("EXTUS","EXTOVERSEAS");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.01);
        logger.info("Accessibility for international trips from Ontario calculated");
        //calculate trips
        internationalTripGeneration tginternational = new internationalTripGeneration(rb);
        trips_international = tginternational.runInternationalTripGeneration();
        logger.info("International trips from Ontario generated");

        //generate visitors
        //recalculate accessibility to Ontario
        fromZones = Arrays.asList("ONTARIO","EXTCANADA","EXTUS","EXTOVERSEAS");
        toZones=Arrays.asList("ONTARIO");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.01);
        logger.info("Accessibility for visitors to Ontario calculated");
        VisitorsTripGeneration vgen = new VisitorsTripGeneration(rb);
        trips_visitors = vgen.runVisitorsTripGeneration(externalZoneList);
        logger.info("Visitor trips to Ontario generated");

        //analyze and write out generated trips
        //first, join the different list of trips
        allTrips.addAll(trips_international);
        allTrips.addAll(trips_domestic);
        allTrips.addAll(trips_visitors);


    }

        //destination Choice
        //if run trip gen is false, then load trips from file
    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model");
        trips.parallelStream().forEach( t -> { //Easy parallel makes for fun times!!!
            int destZoneId = dcModel.selectDestination(t);
            t.setDestination(destZoneId);
        });
    }


    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }







}

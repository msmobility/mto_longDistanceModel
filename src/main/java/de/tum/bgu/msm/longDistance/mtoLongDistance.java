package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import javafx.collections.FXCollections;
import org.apache.log4j.Logger;

import java.util.*;

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

    public mtoLongDistance(ResourceBundle rb) {
        this.rb = rb;
    }

    //public static ArrayList<String> tripPurposes = new ArrayList<>();
    public static List<String> tripPurposes = Arrays.asList("visit","business","leisure");

    //public static ArrayList<String> tripStates = new ArrayList<>();

    public static List<String> tripStates = Arrays.asList("away","daytrip","inout");



    public void runLongDistanceModel () {
        // main method to run long-distance model

        //read synthetic population
        readSP rsp = new readSP(rb);
        ArrayList<Zone> internalZoneList= rsp.readInternalZones();
        rsp.readSyntheticPopulation(internalZoneList);

        //read internal zone employment and external zones
        //added omx.jar; if not this doesn't work
        mtoLongDistData md = new mtoLongDistData(rb);
        md.readInternalZonesEmployment(internalZoneList);
        ArrayList<Zone> externalZoneList = md.readExternalZones();

        //join all zones
        ArrayList<Zone> zoneList = new ArrayList<>();
        zoneList.addAll(internalZoneList);
        zoneList.addAll(externalZoneList);

        //read skims
        md.readSkim();

        //initialize parameters for accessibility
        List<String> fromZones;
        List<String> toZones;
        float alphaAuto;
        float betaAuto;
        //calculate accessibility (not used in the model, only for external analysis)
        if(ResourceUtil.getBooleanProperty(rb,"analyze.accessibility",false)) {
            //input parameters for accessibility calculations from mto properties
            alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
            betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");
            fromZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "orig.zone.type", ",");
            toZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "dest.zone.type", ",");
            md.calculateAccessibility(zoneList, fromZones, toZones, alphaAuto, betaAuto);
            md.writeOutAccessibilities(zoneList);
        }



        //generate domestic trips
        //recalculate accessibility to Canada
        fromZones = Arrays.asList("ONTARIO");
        toZones=Arrays.asList("ONTARIO","EXTCANADA");
        md.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.1);
        logger.info("Accessibility for domestic trips from Ontario calculated");
        tripGeneration tgdomestic = new tripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_domestic = tgdomestic.runTripGeneration();
        logger.info("Domestic Trips from Ontario generated");

        //generate international trips (must be done after domestic)
        //recalculate accessibility to external international zones
        toZones = Arrays.asList("EXTUS","EXTOVERSEAS");
        md.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.01);
        logger.info("Accessibility for international trips from Ontario calculated");
        //calculate trips
        internationalTripGeneration tginternational = new internationalTripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_international = tginternational.runInternationalTripGeneration();
        logger.info("International trips from Ontario generated");

        //generate visitors

        fromZones = Arrays.asList("ONTARIO","EXTCANADA","EXTUS","EXTOVERSEAS");
        toZones=Arrays.asList("ONTARIO");
        md.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float)-0.01);
        logger.info("Accessibility for visitors to Ontario calculated");
        VisitorsTripGeneration vgen = new VisitorsTripGeneration(rb);
        ArrayList<LongDistanceTrip> trips_visitors  = vgen.runVisitorsTripGeneration(externalZoneList);
        logger.info("Visitor trips to Ontario generated");

        //analyze and write out generated trips
        //first, join the different list of trips
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();
        trips.addAll(trips_international);
        trips.addAll(trips_domestic);
        trips.addAll(trips_visitors);

        mtoAnalyzeTrips tripAnalysis = new mtoAnalyzeTrips(rb);
        if(ResourceUtil.getBooleanProperty(rb,"analyze.trips",false)) tripAnalysis.runMtoAnalyzeTrips(trips, zoneList);
    }

    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }







}

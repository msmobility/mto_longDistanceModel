package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationModel;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.ReadSP;
import de.tum.bgu.msm.Util;
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

public class MtoLongDistance {
    static Logger logger = Logger.getLogger(MtoLongDistance.class);
    private ResourceBundle rb;

    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private MtoLongDistData mtoLongDistData;

    private TripGenerationModel tripGenModel;
    private DomesticDestinationChoice dcModel;
    private MtoAnalyzeTrips tripAnalysis;
    private ReadSP syntheticPopulationReader;

    public MtoLongDistance(ResourceBundle rb) {

        this.rb = rb;
        this.tripAnalysis = new MtoAnalyzeTrips(rb);
        mtoLongDistData = new MtoLongDistData(rb);
        syntheticPopulationReader = new ReadSP(rb, mtoLongDistData);
        tripGenModel = new TripGenerationModel(rb, syntheticPopulationReader, mtoLongDistData);
        dcModel = new DomesticDestinationChoice(rb);

    }

    //public static ArrayList<String> tripPurposes = new ArrayList<>();
    public static List<String> tripPurposes = Arrays.asList("visit","business","leisure");

    //public static ArrayList<String> tripStates = new ArrayList<>();

    public static List<String> tripStates = Arrays.asList("away","daytrip","inout");

    public void runLongDistanceModel () {

        if(ResourceUtil.getBooleanProperty(rb,"run.trip.gen",false)) {
            allTrips = tripGenModel.runTripGeneration();
            //currently only internal zone list
            if(ResourceUtil.getBooleanProperty(rb,"analyze.trips",false)) tripAnalysis.runMtoAnalyzeTrips(allTrips, mtoLongDistData.getZoneList());

        } else {
            //load saved trips
            logger.info("Loading generated trips");
            TableDataSet tripsDomesticTable = Util.readCSVfile(ResourceUtil.getProperty(rb, "trip.out.file"));

            for (int i=0; i<tripsDomesticTable.getRowCount(); i++) {
                LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i+1, mtoLongDistData.getZoneLookup());
                allTrips.add(ldt);

            }
        }
        if(ResourceUtil.getBooleanProperty(rb,"run.dest.choice",false)) {
            runDestinationChoice(allTrips);

        }

    }




        //destination Choice
        //if run trip gen is false, then load trips from file
    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach( t -> { //Easy parallel makes for fun times!!!
            if (!t.isLongDistanceInternational() && t.getOrigZone().getZoneType() == ZoneType.ONTARIO) {
                int destZoneId = dcModel.selectDestination(t);
                t.setDestination(destZoneId);
            }
        });
    }


    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }







}

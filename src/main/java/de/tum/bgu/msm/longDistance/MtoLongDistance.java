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
    private ReadSP syntheticPopulationReader;

    public MtoLongDistance(ResourceBundle rb) {

        this.rb = rb;
        mtoLongDistData = new MtoLongDistData(rb);
        syntheticPopulationReader = new ReadSP(rb, mtoLongDistData);
        mtoLongDistData.populateZones(syntheticPopulationReader);
        tripGenModel = new TripGenerationModel(rb, mtoLongDistData);
        dcModel = new DomesticDestinationChoice(rb);

    }

    public void runLongDistanceModel () {

        if(ResourceUtil.getBooleanProperty(rb,"run.trip.gen",false)) {
            allTrips = tripGenModel.runTripGeneration(syntheticPopulationReader);
            //currently only internal zone list
            if(ResourceUtil.getBooleanProperty(rb,"analyze.trips",false)) tripGenModel.runMtoAnalyzeTrips(allTrips, mtoLongDistData.getZoneList(), syntheticPopulationReader);

        } else {
            //load saved trips
            logger.info("Loading generated trips");
            TableDataSet tripsDomesticTable = Util.readCSVfile(ResourceUtil.getProperty(rb, "trip.out.file"));

            for (int i=0; i<tripsDomesticTable.getRowCount(); i++) {
                LongDistanceTrip ldt = new LongDistanceTrip(tripsDomesticTable, i+1, mtoLongDistData.getZoneLookup(), syntheticPopulationReader);
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






}

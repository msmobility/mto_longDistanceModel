package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.accessibilityAnalysis.AccessibilityAnalysis;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationModel;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneDisaggregator;
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
    private SyntheticPopulation syntheticPopulationReader;

    private TripGenerationModel tripGenModel;

    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;

    private DomesticModeChoice mcDomesticModel;
    private IntModeChoice intModeChoice;

    private ZoneDisaggregator zd;

    public MtoLongDistance(ResourceBundle rb) {

        this.rb = rb;

        //sp and zone system
        mtoLongDistData = new MtoLongDistData(rb);
        syntheticPopulationReader = new SyntheticPopulation(rb, mtoLongDistData);
        mtoLongDistData.populateZones(syntheticPopulationReader);

        //tg model
        tripGenModel = new TripGenerationModel(rb, mtoLongDistData, syntheticPopulationReader);

        //mode choice models
        mcDomesticModel = new DomesticModeChoice(rb, mtoLongDistData);
        intModeChoice = new IntModeChoice(rb, mtoLongDistData, mcDomesticModel);

        //destination choice models
        dcModel = new DomesticDestinationChoice(rb, mtoLongDistData, mcDomesticModel);
        dcOutboundModel = new IntOutboundDestinationChoice(rb, mtoLongDistData, intModeChoice);
        dcInBoundModel = new IntInboundDestinationChoice(rb, mtoLongDistData, intModeChoice);

        //disaggregation model
        zd = new ZoneDisaggregator(rb, mtoLongDistData.getZoneList());

    }

    public void runLongDistanceModel() {

        boolean runTG = ResourceUtil.getBooleanProperty(rb, "run.trip.gen", false);
        boolean runDC = ResourceUtil.getBooleanProperty(rb, "run.dest.choice", false);

        //developing tools to skip TG and/or DC if needed
        if (!runTG) {
            if (runDC) {
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

        boolean calibration = true;

        if (runTG) allTrips = tripGenModel.runTripGeneration();

        if (calibration) calibrateDestinationChoice(allTrips);

        if (runDC && !calibration) runDestinationChoice(allTrips);


        runModeChoice(allTrips);

        runDisaggregation(allTrips);

        if (ResourceUtil.getBooleanProperty(rb, "write.trips", false)) {
            syntheticPopulationReader.writeSyntheticPopulation();
            writeTrips(allTrips);
        }


        if (ResourceUtil.getBooleanProperty(rb, "analyze.accessibility", false)) {

            AccessibilityAnalysis accAna = new AccessibilityAnalysis(rb, mtoLongDistData);
            accAna.calculateAccessibilityForAnalysis();
        }

    }


    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
            if (!t.isLongDistanceInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                t.setDestination(destZoneId);
                t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
            } else {
                if (t.getOrigZone().getZoneType() == ZoneType.ONTARIO || t.getOrigZone().getZoneType() == ZoneType.EXTCANADA) {
                    // residents to international
                    int destZoneId = dcOutboundModel.selectDestination(t);
                    t.setDestination(destZoneId);
                    t.setDestZoneType(dcOutboundModel.getDestinationZoneType(destZoneId));
                    if(t.getDestZoneType().equals(ZoneType.EXTUS)) t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));

                } else if (t.getOrigZone().getZoneType() == ZoneType.EXTUS) {
                    // us visitors with destination in CANADA
                    int destZoneId = dcInBoundModel.selectDestinationFromUs(t);
                    t.setDestination(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                    t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
                } else {
                    //os visitors to Canada
                    int destZoneId = dcInBoundModel.selectDestinationFromOs(t);
                    t.setDestination(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                }
            }

        });
    }


    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!t.isLongDistanceInternational()) {
                //domestic mode choice for synthetic persons in Ontario
                int mode = mcDomesticModel.selectModeDomestic(t);
                t.setMode(mode);
                // international mode choice
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getOrigZone().getZoneType().equals(ZoneType.EXTCANADA)) {
                //residents
                if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from Canada to US
                    int mode = intModeChoice.selectMode(t);
                    t.setMode(mode);
                } else {
                    //international from Canada to OS
                    t.setMode(1); //always by air
                }
                //visitors
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS)) {
                //international visitors from US
                int mode = intModeChoice.selectMode(t);
                t.setMode(mode);
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTOVERSEAS)) {
                //international visitors from US
                t.setMode(1); //always by air
            }

        });
    }

    public void runDisaggregation(ArrayList<LongDistanceTrip> trips){
        logger.info("Starting disaggregation");
        allTrips.parallelStream().forEach(t -> {
            zd.disaggregateDestination(t);
        });
        logger.info("Finished disaggregation");
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

//    public void writePopByZone() {
//        PrintWriter pw = Util.openFileForSequentialWriting(rb.getString("zone.out.file"), false);
//        pw.println("zone,hh,pp");
//        for (Zone zone : mtoLongDistData.getInternalZoneList()) {
//            pw.println(zone.getId() + "," + zone.getHouseholds() + "," + zone.getPopulation());
//        }
//        pw.close();
//    }

    public void calibrateDestinationChoice (ArrayList <LongDistanceTrip> allTrips){

        runDestinationChoice(allTrips);
        int maxIterDc = 10;
        double[][] calibrationMatrix = new double[3][3];

        for (int iteration = 0; iteration < maxIterDc; iteration ++) {

            Calibration c = new Calibration();
            calibrationMatrix = c.calculateCalibrationMatrix(allTrips);
            dcModel.updatedomDcCalibrationV(calibrationMatrix[0]);
            runDestinationChoice(allTrips);


        }

        logger.info("k_domestic_dc visit = " + calibrationMatrix[0][0]);
        logger.info("k_domestic_dc business = " + calibrationMatrix[0][1]);
        logger.info("k_domestic_dc leisure = " + calibrationMatrix[0][2]);

    }


}

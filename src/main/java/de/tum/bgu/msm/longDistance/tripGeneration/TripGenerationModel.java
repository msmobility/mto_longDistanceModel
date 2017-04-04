package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import org.apache.log4j.Logger;

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

    public ArrayList<LongDistanceTrip> runTripGeneration(SyntheticPopulation syntheticPopulation) {
        // main method to run long-distance model

        //read synthetic population

        ArrayList<Zone> zoneList = mtoLongDistData.getZoneList();


        //initialize parameters for accessibility
        List<String> fromZones;
        List<String> toZones;
        //calculate accessibility (not used in the model, only for external analysis)
        if (ResourceUtil.getBooleanProperty(rb, "analyze.accessibility", false)) {
            //read skims
            //md.readSkim("auto");
            mtoLongDistData.readSkim("transit");
            //input parameters for accessibility calculations from mto properties
            float alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
            float betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");
            fromZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "orig.zone.type", ",");
            toZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "dest.zone.type", ",");
            mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, alphaAuto, betaAuto);
            mtoLongDistData.writeOutAccessibilities(zoneList);
        }

        ArrayList<LongDistanceTrip> trips_domestic;
        ArrayList<LongDistanceTrip> trips_international;
        ArrayList<LongDistanceTrip> trips_visitors;
        ArrayList<LongDistanceTrip> trips_extCanInt;


        //read skim for auto and run the model
        mtoLongDistData.readSkim("auto");

        //generate domestic trips
        //recalculate accessibility to Canada
        fromZones = Arrays.asList("ONTARIO");
        toZones = Arrays.asList("ONTARIO", "EXTCANADA");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float) -0.1);
        logger.info("Accessibility for domestic trips from Ontario calculated");
        DomesticTripGeneration tgdomestic = new DomesticTripGeneration(rb);
        trips_domestic = tgdomestic.runTripGeneration(syntheticPopulation);
        logger.info("Domestic Trips from Ontario generated");

        //generate international trips (must be done after domestic)
        //recalculate accessibility to external international zones
        toZones = Arrays.asList("EXTUS", "EXTOVERSEAS");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float) -0.01);
        logger.info("Accessibility for international trips from Ontario calculated");
        //calculate trips
        InternationalTripGeneration tginternational = new InternationalTripGeneration(rb);
        trips_international = tginternational.runInternationalTripGeneration(syntheticPopulation);
        logger.info("International trips from Ontario generated");

        //generate visitors
        //recalculate accessibility to Ontario
        fromZones = Arrays.asList("ONTARIO", "EXTCANADA", "EXTUS", "EXTOVERSEAS");
        toZones = Arrays.asList("ONTARIO");
        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, (float) 1, (float) -0.01);
        logger.info("Accessibility for visitors to Ontario calculated");
        VisitorsTripGeneration vgen = new VisitorsTripGeneration(rb);
        trips_visitors = vgen.runVisitorsTripGeneration(mtoLongDistData.getExternalZoneList());
        logger.info("Visitor trips to Ontario generated");

        ExtCanToIntTripGeneration extCanToIntTripGeneration = new ExtCanToIntTripGeneration(rb);
        trips_extCanInt = extCanToIntTripGeneration.runExtCanInternationalTripGeneration(mtoLongDistData.getExternalZoneList());

        //analyze and write out generated trips
        //first, join the different lists of trips
        ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
        allTrips.addAll(trips_international); // Ontarians who to international destinations
        allTrips.addAll(trips_domestic);      // Ontarians who travel within Canada
        allTrips.addAll(trips_visitors);      // Visitors from outside of Ontario (Rest of Canada or internationals) who visit Canada
        allTrips.addAll(trips_extCanInt);     // Canadians who do not live in Ontario traveling to international destinations (who may travel through Ontario)

        return allTrips;


    }

}

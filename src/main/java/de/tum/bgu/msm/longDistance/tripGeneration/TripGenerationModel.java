package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
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
    private JsonUtilMto prop;
    private MtoLongDistData mtoLongDistData;
    static Logger logger = Logger.getLogger(TripGenerationModel.class);
    private SyntheticPopulation synPop;

    //trip gen models
    private DomesticTripGeneration domesticTripGeneration;
    private InternationalTripGeneration internationalTripGeneration;
    private VisitorsTripGeneration visitorsTripGeneration;
    //private ExtCanToIntTripGeneration extCanToIntTripGeneration;

    public TripGenerationModel(ResourceBundle rb, JsonUtilMto prop, MtoLongDistData mtoLongDistData, SyntheticPopulation synPop) {
        this.rb = rb;
        this.prop = prop;
        this.mtoLongDistData = mtoLongDistData;
        this.synPop = synPop;

        //create the trip generation models
        domesticTripGeneration = new DomesticTripGeneration(rb, prop, synPop, mtoLongDistData);
        internationalTripGeneration = new InternationalTripGeneration(rb, prop, synPop, mtoLongDistData);
        visitorsTripGeneration = new VisitorsTripGeneration(rb, prop);
        //extCanToIntTripGeneration = new ExtCanToIntTripGeneration(rb);

        logger.info("Trip Generation model set up");
    }

    public void loadTripGenerationModels(IntOutboundDestinationChoice intDcModel){

        domesticTripGeneration.loadTripGeneration();
        internationalTripGeneration.loadInternationalTripGeneration(intDcModel);
        logger.info("Trip generation loaded");

    }

    public ArrayList<LongDistanceTrip> runTripGeneration() {

        //initialize list of trips
        ArrayList<LongDistanceTrip> trips_dom_ontarian; //trips from Ontario to all Canada - sp based
        ArrayList<LongDistanceTrip> trips_int_ontarian; //trips from Ontario to other countries - sp based
        //ArrayList<LongDistanceTrip> trips_int_canadian; //trips from non-Ontario to other countries
        ArrayList<LongDistanceTrip> trips_visitors; //trips from non-Ontario to all Canada, and trips from other country to Canada

        //generate domestic trips
        trips_dom_ontarian = domesticTripGeneration.runTripGeneration();
        logger.info("  " + trips_dom_ontarian.size() + " domestic trips from Ontario generated");

        //generate international trips (must be done after domestic)
        trips_int_ontarian = internationalTripGeneration.runInternationalTripGeneration();
        logger.info("  " + trips_int_ontarian.size() + " international trips from Ontario generated");

        //generate visitors
        trips_visitors = visitorsTripGeneration.runVisitorsTripGeneration(mtoLongDistData.getExternalZoneList());
        //logger.info("  Visitor trips to Canada generated");

        //trips_int_canadian = extCanToIntTripGeneration.runExtCanInternationalTripGeneration(mtoLongDistData.getExternalZoneList());
        //logger.info("  International trips from non-Ontarian zones generated");

        //join all the trips
        ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
        allTrips.addAll(trips_int_ontarian);
        allTrips.addAll(trips_dom_ontarian);
        allTrips.addAll(trips_visitors);
        //allTrips.addAll(trips_int_canadian);

        return allTrips;

    }

}

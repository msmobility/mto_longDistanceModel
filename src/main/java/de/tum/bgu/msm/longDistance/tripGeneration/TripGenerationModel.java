package de.tum.bgu.msm.longDistance.tripGeneration;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.sp.SyntheticPopulation;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.ResourceBundle;



/**
 * Created by Joe on 28/10/2016.
 */
public class TripGenerationModel implements ModelComponent {
    private ResourceBundle rb;
    private JSONObject prop;
    private DataSet dataSet;
    static Logger logger = Logger.getLogger(TripGenerationModel.class);
    private SyntheticPopulation synPop;

    //trip gen models
    private DomesticTripGeneration domesticTripGeneration;
    private InternationalTripGeneration internationalTripGeneration;
    private VisitorsTripGeneration visitorsTripGeneration;
    //private ExtCanToIntTripGeneration extCanToIntTripGeneration;

    public TripGenerationModel() {
    }


    public void setup(JSONObject prop, String inputFolder, String outputFolder ){
        this.prop = prop;

//        this.synPop = synPop;

        //create the trip generation models
        domesticTripGeneration = new DomesticTripGeneration(prop);
        internationalTripGeneration = new InternationalTripGeneration(prop);
        visitorsTripGeneration = new VisitorsTripGeneration(prop);
        //extCanToIntTripGeneration = new ExtCanToIntTripGeneration(rb);

        logger.info("Trip Generation model set up");
    }

    public void load(DataSet dataSet){
        this.dataSet = dataSet;

        domesticTripGeneration.loadTripGeneration(dataSet);
        internationalTripGeneration.loadInternationalTripGeneration(dataSet);
        visitorsTripGeneration.loadVisitorsTripGeneration(dataSet);

        logger.info("Trip generation loaded");
    }

    public void run(DataSet dataSet, int nThreads){

        dataSet.setAllTrips(generateTrips());

    }



    public ArrayList<LongDistanceTrip> generateTrips() {

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
        trips_visitors = visitorsTripGeneration.runVisitorsTripGeneration();
        //logger.info("  Visitor trips to Canada generated");

        //trips_int_canadian = extCanToIntTripGeneration.runExtCanInternationalTripGeneration(zonalData.getExternalZoneList());
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

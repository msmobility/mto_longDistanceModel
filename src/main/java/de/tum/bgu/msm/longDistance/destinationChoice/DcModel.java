package de.tum.bgu.msm.longDistance.destinationChoice;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Created by carlloga on 8/2/2017.
 */
public class DcModel implements ModelComponent {

    static Logger logger = Logger.getLogger(DcModel.class);

    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        dcModel = new DomesticDestinationChoice(prop);
        dcOutboundModel = new IntOutboundDestinationChoice(prop);
        dcInBoundModel = new IntInboundDestinationChoice(prop);
    }

    @Override
    public void load(DataSet dataSet) {
        //store the models in the dataset
        dataSet.setDcDomestic(dcModel);
        dataSet.setDcIntOutbound(dcOutboundModel);
        dataSet.setDcIntInbound(dcInBoundModel);

        //load submodels
        dcModel.loadDomesticDestinationChoice(dataSet);
        dcInBoundModel.loadIntInboundDestinationChoice(dataSet);
        dcOutboundModel.loadIntOutboundDestinationChoiceModel(dataSet);

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runDestinationChoice(dataSet.getAllTrips());
    }


    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
            if (!t.isInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                t.setCombinedDestZoneId(destZoneId);
                t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
            } else {
                if (t.getOrigZone().getZoneType() == ZoneType.ONTARIO || t.getOrigZone().getZoneType() == ZoneType.EXTCANADA) {
                    // residents to international
                    int destZoneId = dcOutboundModel.selectDestination(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcOutboundModel.getDestinationZoneType(destZoneId));
                    if (t.getDestZoneType().equals(ZoneType.EXTUS))
                        t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));

                } else if (t.getOrigZone().getZoneType() == ZoneType.EXTUS) {
                    // us visitors with destination in CANADA
                    int destZoneId = dcInBoundModel.selectDestinationFromUs(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                    t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
                } else {
                    //os visitors to Canada
                    int destZoneId = dcInBoundModel.selectDestinationFromOs(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                }
            }

        });
    }
}

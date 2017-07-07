package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 8/31/2016.
 */
public class VisitorsTripGeneration {

    private TableDataSet visitorPartyProbabilities;
    //private TableDataSet visitorRateCoefficients;
    private TableDataSet visitorsRatePerZone;

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    static final List<String> tripStates = MtoLongDistData.getTripStates();
    static final List<String> tripPurposes = MtoLongDistData.getTripPurposes();
    private ResourceBundle rb;

    public VisitorsTripGeneration(ResourceBundle rb) {

        this.rb = rb;

        String visitorPartyProbabilitiesFilename = rb.getString("visitor.parties");
        visitorPartyProbabilities = Util.readCSVfile(visitorPartyProbabilitiesFilename);
        visitorPartyProbabilities.buildIndex(visitorPartyProbabilities.getColumnPosition("travelParty"));

        //String visitorsRateFilename = rb.getString("visitor.rates");
        //visitorRateCoefficients = Util.readCSVfile(visitorsRateFilename);
        //visitorRateCoefficients.buildIndex(visitorRateCoefficients.getColumnPosition("factor"));
        //no longer used

        String visitorsRatePerZoneFilename = rb.getString("visitor.zone.rates");
        visitorsRatePerZone = Util.readCSVfile(visitorsRatePerZoneFilename);
        visitorsRatePerZone.buildIndex(visitorsRatePerZone.getColumnPosition("zone"));
    }


    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runVisitorsTripGeneration(ArrayList<Zone> externalZoneList) {


        ArrayList<LongDistanceTrip> visitorTrips = new ArrayList<>();

        int tripCount = 0;
        for (Zone zone : externalZoneList) {
            for (String tripPurpose : tripPurposes) {
                //get rates per zone for all travellers
                for (String tripState : tripStates) {
                    String column = tripState + "." + tripPurpose;
                    double tripRate;
                    tripRate = visitorsRatePerZone.getIndexedValueAt(zone.getId(), column);
                    int numberOfTrips = (int) (tripRate * zone.getPopulation());
                    for (int i = 0; i < numberOfTrips; i++) {
                        LongDistanceTrip trip = createVisitorLongDistanceTrip(tripPurpose, tripState, visitorPartyProbabilities, zone);
                        tripCount++;
                        visitorTrips.add(trip);
                    }
                }
            }
        }
        logger.info(tripCount + " visitors trips generated");
        return visitorTrips;
    }

    private LongDistanceTrip createVisitorLongDistanceTrip(String tripPurpose, String tripState, TableDataSet visitorPartyProbabilities, Zone zone) {
        boolean international;
        int adultsHh;
        int kidsHh;
        int nonHh;
        if (zone.getZoneType().equals(ZoneType.EXTCANADA)) international = false;
        else international = true;

        //generation of trip parties (no assignment of person, only sizes)
        adultsHh = 1;
        kidsHh = 0;
        nonHh = 0;
        String column = "adults." + tripPurpose;
        double randomChoice = Math.random();
        while (adultsHh < 9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(adultsHh, 5), column))
            adultsHh++;

        column = "kids." + tripPurpose;
        randomChoice = Math.random();
        while (kidsHh < 9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(kidsHh + 1, 9), column))
            kidsHh++;

        column = "nonHh." + tripPurpose;
        randomChoice = Math.random();
        while (nonHh < 9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(nonHh + 1, 9), column))
            nonHh++;


        return new LongDistanceTrip(null, international, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), zone, true,
                0, adultsHh, kidsHh, nonHh);

        //todo assign duration!

    }


}

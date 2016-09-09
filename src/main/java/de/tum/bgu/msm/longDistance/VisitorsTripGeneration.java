package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.Person;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 8/31/2016.
 */
public class VisitorsTripGeneration {

    private List<String> tripPurposes = mtoLongDistance.getTripPurposes();
    private List<String> tripStates = mtoLongDistance.getTripStates();

    static Logger logger = Logger.getLogger(tripGeneration.class);
    private ResourceBundle rb;

    public VisitorsTripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }


    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runVisitorsTripGeneration(ArrayList<Zone> externalZoneList) {

        String visitorPartyProbabilitiesFilename = rb.getString("visitor.parties");;
        TableDataSet visitorPartyProbabilities =  util.readCSVfile(visitorPartyProbabilitiesFilename);
        visitorPartyProbabilities.buildIndex(visitorPartyProbabilities.getColumnPosition("travelParty"));

        String visitorsRateFilename = rb.getString("visitor.rates");;
        TableDataSet visitorRateCoefficients =  util.readCSVfile(visitorsRateFilename);
        visitorRateCoefficients.buildIndex(visitorRateCoefficients.getColumnPosition("factor"));

        ArrayList<LongDistanceTrip> visitorTrips = new ArrayList<>();

        int tripCount =0;
        for (Zone zone : externalZoneList) {
            for (String tripPurpose : tripPurposes) {
                for (String tripState : tripStates) {
                    //1. get the trip rates (table or model?)
                    String column = tripState + "." + tripPurpose;
                    int usDummy;
                    if (zone.getZoneType().equals(ZoneType.EXTUS)) usDummy = 1;
                    else usDummy = 0;
                    int osDummy;
                    if (zone.getZoneType().equals(ZoneType.EXTOVERSEAS)) osDummy = 1;
                    else osDummy = 0;
                    //currently uses a binary choice model for each purpose and state, with Utility = a+b*US + c*Acc (similarly to an average trip rate)
                    double expTerm = Math.exp(visitorRateCoefficients.getIndexedValueAt(1, column) +
                            visitorRateCoefficients.getIndexedValueAt(2, column)*usDummy +
                            visitorRateCoefficients.getIndexedValueAt(3, column)*osDummy +
                            visitorRateCoefficients.getIndexedValueAt(4, column)*zone.getAccessibility());
                    double tripRate = expTerm/(1+expTerm);
                    int numberOfTrips = (int) (tripRate * zone.getPopulation());
                    for (int i = 0; i < numberOfTrips; i++) {
                        LongDistanceTrip trip = createVisitorLongDistanceTrip (tripPurpose, tripState, tripCount, visitorPartyProbabilities, zone);
                        tripCount++;
                        visitorTrips.add(trip);
                    }
                }
            }
        }
        logger.info(tripCount + " visitors trips generated");
        return visitorTrips;
    }

    private LongDistanceTrip createVisitorLongDistanceTrip(String tripPurpose, String tripState, int tripCount, TableDataSet visitorPartyProbabilities, Zone zone ){
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
        while (adultsHh<9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(adultsHh, 5), column)) adultsHh++;

        column = "kids." + tripPurpose;
        randomChoice = Math.random();
        while (kidsHh<9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(kidsHh + 1, 9), column)) kidsHh++;

        column = "nonHh." + tripPurpose;
        randomChoice = Math.random();
        while (nonHh<9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(nonHh+1, 9), column)) nonHh++;


        return new LongDistanceTrip(99999999, international, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), zone,
                0, adultsHh, kidsHh, nonHh);

    }


}

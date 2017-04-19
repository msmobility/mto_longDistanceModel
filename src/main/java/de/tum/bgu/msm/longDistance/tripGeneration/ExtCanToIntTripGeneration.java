package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 19/1/17.
 */
public class ExtCanToIntTripGeneration {

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    static final List<String> tripStates = MtoLongDistData.getTripStates();
    static final List<String> tripPurposes = MtoLongDistData.getTripPurposes();
    private ResourceBundle rb;

    public ExtCanToIntTripGeneration(ResourceBundle rb) {
        this.rb = rb;
    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> runExtCanInternationalTripGeneration(ArrayList<Zone> externalZoneList) {
        ArrayList<LongDistanceTrip> trips = new ArrayList<>();

        String externalCanIntRatesName = rb.getString("ext.can.int.zone.rates");
        TableDataSet externalCanIntRates = Util.readCSVfile(externalCanIntRatesName);
        externalCanIntRates.buildIndex(externalCanIntRates.getColumnPosition("zone"));

        String visitorPartyProbabilitiesFilename = rb.getString("visitor.parties");
        TableDataSet travelPartyProbabilities = Util.readCSVfile(visitorPartyProbabilitiesFilename);
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));


        int tripCount = 0;
        for (Zone zone : externalZoneList) {
            if (zone.getZoneType().equals(ZoneType.EXTCANADA)) {
                for (String tripPurpose : tripPurposes) {
                    for (String tripState : tripStates) {
                        String column = tripState + "." + tripPurpose;
                        double tripRate;
                        //generates all travellers and apply later destination choice
                        tripRate = externalCanIntRates.getIndexedValueAt(zone.getId(), column);

                        int numberOfTrips = (int) Math.round(tripRate * zone.getPopulation());
                        for (int i = 0; i < numberOfTrips; i++) {
                            LongDistanceTrip trip = createExtCanIntLongDistanceTrip(tripPurpose, tripState, zone, travelPartyProbabilities);
                            tripCount++;
                            trips.add(trip);
                        }
                    }
                }
            }
        }


        logger.info(tripCount + " Canadian and non-Ontarian international trips generated");
        return trips;
    }

    private LongDistanceTrip createExtCanIntLongDistanceTrip(String tripPurpose, String tripState, Zone zone, TableDataSet travelPartyProbabilities) {

        boolean international = true;
        int adultsHh;
        int kidsHh;
        int nonHh;
        //generation of trip parties (no assignment of person, only sizes)
        adultsHh = 1;
        kidsHh = 0;
        nonHh = 0;
        String column = "adults." + tripPurpose;
        double randomChoice = Math.random();
        while (adultsHh < 9 && randomChoice < travelPartyProbabilities.getIndexedValueAt(Math.min(adultsHh, 5), column))
            adultsHh++;

        column = "kids." + tripPurpose;
        randomChoice = Math.random();
        while (kidsHh < 9 && randomChoice < travelPartyProbabilities.getIndexedValueAt(Math.min(kidsHh + 1, 9), column))
            kidsHh++;

        column = "nonHh." + tripPurpose;
        randomChoice = Math.random();
        while (nonHh < 9 && randomChoice < travelPartyProbabilities.getIndexedValueAt(Math.min(nonHh + 1, 9), column))
            nonHh++;


        return new LongDistanceTrip(null, international, tripPurposes.indexOf(tripPurpose), tripStates.indexOf(tripState), zone, true,
                0, adultsHh, kidsHh, nonHh);

    }
}

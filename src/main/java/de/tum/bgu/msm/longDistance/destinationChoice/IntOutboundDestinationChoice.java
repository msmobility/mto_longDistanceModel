package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by carlloga on 4/12/2017.
 */
public class IntOutboundDestinationChoice {

    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    private TableDataSet destCombinedZones;
    private TableDataSet coefficients;
    private Matrix autoTravelTime;
    private int[] alternativesUS;
    private int[] alternativesOS;

    String[] tripPurposeArray;

    Map<Integer, Zone> externalOsMap = new HashMap<>();


    public IntOutboundDestinationChoice(ResourceBundle rb, MtoLongDistData ldData){

        coefficients = Util.readCSVfile(rb.getString("dc.int.out.coefs"));
        coefficients.buildStringIndex(1);
        tripPurposeArray = ldData.tripPurposes.toArray(new String[ldData.tripPurposes.size()]);

        //load alternatives
        destCombinedZones = Util.readCSVfile(rb.getString("dc.us.combined"));
        destCombinedZones.buildIndex(1);

        //load combined zones distance skim
        readSkim(rb);

        alternativesUS = destCombinedZones.getColumnAsInt("combinedZone");

        ldData.getExternalZoneList().forEach(zone -> {
            if (zone.getZoneType() == ZoneType.EXTOVERSEAS){
                externalOsMap.put(zone.getCombinedZoneId(), zone);
            }
        });
        alternativesOS = new int [externalOsMap.size()];

        int index = 0;
        for (Integer id : externalOsMap.keySet()) {
            alternativesOS[index] = id;
            index++;
        }

    }

    public int selectDestination(LongDistanceTrip trip) {

        int destination;
        //0 visit, 1 business and 2 leisure

        String tripPurpose = tripPurposeArray[trip.getLongDistanceTripPurpose()];

        if (selectUs(trip, tripPurpose)){

            double[] expUtilities = Arrays.stream(alternativesUS).mapToDouble(a -> Math.exp(calculateUsZoneUtility(trip, tripPurpose, a))).toArray();
            double probability_denominator = Arrays.stream(expUtilities).sum();

            double[] probabilities = Arrays.stream(expUtilities).map(u -> u/probability_denominator).toArray();

            destination =  new EnumeratedIntegerDistribution(alternativesUS, probabilities).sample();

        } else {

            double[] expUtilitiesOs = Arrays.stream(alternativesOS).mapToDouble(a -> calculateOsZoneUtility(a)).toArray();

            double probability_denominator = Arrays.stream(expUtilitiesOs).sum();

            double[] probabilities = Arrays.stream(expUtilitiesOs).map(u -> u/probability_denominator).toArray();

            destination =  new EnumeratedIntegerDistribution(alternativesOS, probabilities).sample();
        }

        return destination;
    }

    public ZoneType getDestinationZoneType(int destinationZoneId){
        //method to give the destination zone type from a destination

        if (externalOsMap.keySet().contains(destinationZoneId)){
            return ZoneType.EXTOVERSEAS;
        } else {
            return ZoneType.EXTUS;
        }
     }



    public void readSkim(ResourceBundle rb) {
        // read skim file
        logger.info("  Reading skims files");

        String matrixName = "skim.int.out.file";
        String hwyFileName = rb.getString(matrixName);
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.int.out.matrix"));
        autoTravelTime = Util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("skim.int.out.lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoTravelTime.setExternalNumbersZeroBased(externalNumbers);
    }

    public boolean selectUs(LongDistanceTrip trip, String tripPurpose){

        //binary choice model for US/OS (per purpose)

        double exp_utility = Math.exp(coefficients.getStringIndexedValueAt("isUs", tripPurpose));

        double probability = exp_utility / (1 + exp_utility);

        if (trip.getLongDistanceTripState() == 1){
            //daytrips are always to US
            return true;
        } else{
            if (Math.random() < probability){
                return true;
            } else {
                return false;
            }
        }
    }


    public double calculateUsZoneUtility(LongDistanceTrip trip, String tripPurpose, int destination){

        //read coefficients

        double b_population = coefficients.getStringIndexedValueAt("population", tripPurpose);
        double b_dist = coefficients.getStringIndexedValueAt("b_dist", tripPurpose);
        double alpha_dist = coefficients.getStringIndexedValueAt("alpha_dist", tripPurpose);

        //read trip data
        double dist = autoTravelTime.getValueAt(trip.getOrigZone().getCombinedZoneId(), destination);

        //read destination data
        double population =  destCombinedZones.getIndexedValueAt(destination, "population");

        return b_population * population +
                b_dist * Math.exp(alpha_dist * dist);
    }


    public double calculateOsZoneUtility(int destination){

        return externalOsMap.get(destination).getPopulation();

    }

}

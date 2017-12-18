package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created by carlloga on 4/12/2017.
 */
public class IntOutboundDestinationChoice {

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    private TableDataSet destCombinedZones;
    private TableDataSet coefficients;
    private Matrix autoTravelTime;
    private int[] alternativesUS;
    private int[] alternativesOS;

    private TableDataSet origCombinedZones;
    private ZonalData ldData;

    private IntModeChoice intModeChoice;
    private DomesticDestinationChoice dcModel;
    private String[] tripPurposeArray;
    private String[] tripStateArray;

    Map<Integer, Zone> externalOsMap = new HashMap<>();

    boolean calibration;
    private double[] calibrationV;


    public IntOutboundDestinationChoice(JSONObject prop) {

        //this.rb = rb;
        //coefficients = Util.readCSVfile(rb.getString("dc.int.out.coefs"));
        coefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.international.outbound.coef_file"));
        coefficients.buildStringIndex(1);

//        this.ldData = ldData;



        //load alternatives
        //destCombinedZones = Util.readCSVfile(rb.getString("dc.us.combined"));
        destCombinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.international.outbound.alternatives_file"));
        destCombinedZones.buildIndex(1);
        alternativesUS = destCombinedZones.getColumnAsInt("combinedZone");

        //load alternatives (origins, to read accessibility to US of the zone)
        //origCombinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        origCombinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.alternatives_file"));
        origCombinedZones.buildIndex(1);



        //calibration = ResourceUtil.getBooleanProperty(rb, "dc.calibration", false);
        calibration = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration");
        this.calibrationV = new double[]{1, 1, 1};

        logger.info("International DC (outbound) set up");

    }

    public void loadIntOutboundDestinationChoiceModel(DataSet dataSet) {

        tripPurposeArray = dataSet.tripPurposes.toArray(new String[dataSet.tripPurposes.size()]);
        tripStateArray = dataSet.tripStates.toArray(new String[dataSet.tripStates.size()]);

        this.dcModel = dataSet.getDcDomestic();
        this.intModeChoice = dataSet.getMcInt();


        //load combined zones distance skim
        autoTravelTime = dataSet.getDcDomestic().getAutoDist();

        dataSet.getExternalZones().forEach(zone -> {
            if (zone.getZoneType() == ZoneType.EXTOVERSEAS) {
                externalOsMap.put(zone.getCombinedZoneId(), zone);
            }
        });

        alternativesOS = new int[externalOsMap.size()];
        int index = 0;
        for (Integer id : externalOsMap.keySet()) {
            alternativesOS[index] = id;
            index++;
        }

        logger.info("International DC (outbound) loaded");
    }

    public int selectDestination(LongDistanceTrip trip) {

        int destination;
        //0 visit, 1 business and 2 leisure

        String tripPurpose = tripPurposeArray[trip.getTripPurpose()];

        if (selectUs(trip, tripPurpose)) {

            double[] expUtilities = Arrays.stream(alternativesUS).mapToDouble(a -> Math.exp(calculateUsZoneUtility(trip, tripPurpose, a))).toArray();
            double probability_denominator = Arrays.stream(expUtilities).sum();

            double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

            //destination = new EnumeratedIntegerDistribution(alternativesUS, probabilities).sample();
            destination =  Util.select(probabilities,alternativesUS);

        } else {

            double[] expUtilitiesOs = Arrays.stream(alternativesOS).mapToDouble(a -> calculateOsZoneUtility(a)).toArray();

            double probability_denominator = Arrays.stream(expUtilitiesOs).sum();

            double[] probabilities = Arrays.stream(expUtilitiesOs).map(u -> u / probability_denominator).toArray();

            //destination = new EnumeratedIntegerDistribution(alternativesOS, probabilities).sample();
            destination =  Util.select(probabilities,alternativesOS);
        }

        return destination;
    }

    public ZoneType getDestinationZoneType(int destinationZoneId) {
        //method to give the destination zone type from a destination

        if (externalOsMap.keySet().contains(destinationZoneId)) {
            return ZoneType.EXTOVERSEAS;
        } else {
            return ZoneType.EXTUS;
        }
    }


    public void readSkim(ResourceBundle rb) {
        // read skim file


        String matrixName = "skim.int.out.file";
        String hwyFileName = rb.getString(matrixName);
        logger.info("  Reading skims file" + hwyFileName);

        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.int.out.matrix"));
        autoTravelTime = Util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("skim.int.out.lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoTravelTime.setExternalNumbersZeroBased(externalNumbers);
    }

    public boolean selectUs(LongDistanceTrip trip, String tripPurpose) {

        double utility;
        //binary choice model for US/OS (per purpose)
        if (trip.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
            //trips from Ontario = use accessibility to get the probability
            double b_intercept = coefficients.getStringIndexedValueAt("isUs", tripPurpose);
            double b_usAccess = coefficients.getStringIndexedValueAt("isUsAcc", tripPurpose);

            double usAccess = origCombinedZones.getIndexedValueAt(trip.getOrigZone().getCombinedZoneId(), "usAccess");

            utility = Math.exp(b_intercept + b_usAccess * usAccess);
        } else {
            //if from external canada do not have accessibility to us in the choice model
            double b_intercept = coefficients.getStringIndexedValueAt("isUsExternal", tripPurpose);
            utility = Math.exp(b_intercept);
        }

        double probability = utility / (1 + utility);

        if (trip.getTripState() == 1) {
            //daytrips are always to US
            return true;
        } else {
            if (LDModel.rand.nextDouble() < probability) {
                return true;
            } else {
                return false;
            }
        }
    }


    public double calculateUsZoneUtility(LongDistanceTrip trip, String tripPurpose, int destination) {

        //read coefficients

        String tripState = tripStateArray[trip.getTripState()];

        double b_population = coefficients.getStringIndexedValueAt("population", tripPurpose);
        double b_log_population = coefficients.getStringIndexedValueAt("log_population", tripPurpose);
        double dtLogsum = coefficients.getStringIndexedValueAt("dtLogsum", tripPurpose);
        double onLogsum = coefficients.getStringIndexedValueAt("onLogsum", tripPurpose);

        double k_dtLogsum = coefficients.getStringIndexedValueAt("k_dtLogsum", tripPurpose);
        double k_onLogsum = coefficients.getStringIndexedValueAt("k_onLogsum", tripPurpose);

        if (calibration) {
            switch (trip.getTripPurpose()) {
                case 2:
                    //tripPurpose = "leisure";
                    k_dtLogsum = calibrationV[2];
                    k_onLogsum = k_dtLogsum;
                    break;
                case 0:
                    //tripPurpose = "visit";
                    k_dtLogsum = calibrationV[0];
                    k_onLogsum = k_dtLogsum;
                    break;
                case 1:
                    //tripPurpose = "business";
                    k_dtLogsum = calibrationV[1];
                    k_onLogsum = k_dtLogsum;
                    break;
            }
        }

        //read trip data
        double dist = autoTravelTime.getValueAt(trip.getOrigZone().getCombinedZoneId(), destination);

        double logsum = 0;
        int[] modes = intModeChoice.getModes();
        for (int m : modes) {
            logsum += Math.exp(intModeChoice.calculateUtilityFromCanada(trip, m, destination));
        }
        if (logsum == 0) {
            return Double.NEGATIVE_INFINITY;
            //deals with trips that logsum == 0 --> means that no mode is available
            //logger.info(trip.getOrigZone().getCombinedZoneId() + " to " + destination);
        } else {
            logsum = Math.log(logsum);
        }

        //read destination data
        double population = destCombinedZones.getIndexedValueAt(destination, "population");

        double log_population = population > 0 ? Math.log(destCombinedZones.getIndexedValueAt(destination, "population")) : 0;


        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        return b_population * population +
                b_log_population * log_population +
                dtLogsum * (1 - overnight) * logsum * k_dtLogsum +
                onLogsum * overnight * logsum * k_onLogsum;
    }


    public double calculateOsZoneUtility(int destination) {

        return externalOsMap.get(destination).getStaticAttraction();

    }

    public void updateIntOutboundCalibrationV(double[] b_calibrationVector) {
        this.calibrationV[0] = this.calibrationV[0] * b_calibrationVector[0];
        this.calibrationV[1] = this.calibrationV[1] * b_calibrationVector[1];
        this.calibrationV[2] = this.calibrationV[2] * b_calibrationVector[2];
    }

    public double[] getCalibrationV() {
        return calibrationV;
    }

    public TableDataSet getOrigCombinedZones() {
        return origCombinedZones;
    }
}

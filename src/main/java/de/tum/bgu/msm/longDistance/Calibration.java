package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.destinationChoice.DcModel;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.McModel;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by carlloga on 12-07-17.
 */
public class Calibration implements ModelComponent {

    private boolean calibrationDC;
    private boolean calibrationMC;

    private int maxIter;


    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;
    private DcModel dcM;

    private DomesticModeChoice mcDomesticModel;
    private IntModeChoice intModeChoice;
    private McModel mcM;

    static Logger logger = Logger.getLogger(Calibration.class);

    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();

    public Calibration() {
    }

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {


        calibrationDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration");
        calibrationMC = JsonUtilMto.getBooleanProp(prop, "mode_choice.calibration");
        maxIter = 50;

    }

    @Override
    public void load(DataSet dataSet) {


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        dcModel = dataSet.getDcDomestic();
        dcOutboundModel = dataSet.getDcIntOutbound();
        dcInBoundModel = dataSet.getDcIntInbound();
        //dcM = dataSet.getDestinationChoiceModel();

        mcDomesticModel = dataSet.getMcDomestic();
        intModeChoice = dataSet.getMcInt();
        //mcM = dataSet.getModeChoiceModel();


        if (calibrationDC || calibrationMC) {
            calibrateModel(calibrationDC, calibrationMC, dataSet);
            getAverageModalShares(dataSet.getAllTrips());
        }
    }


    public void calibrateModel(boolean dc, boolean mc, DataSet dataSet) {

        for (LongDistanceTrip t : dataSet.getAllTrips()){
            if (t.getTripState() !=0){
                allTrips.add(t);
            }
        }


        double[][][] calibrationMatrixMc = new double[4][3][4];
        double[][] calibrationMatrixDc = new double[3][3];


        for (int iteration = 0; iteration < maxIter; iteration++) {

            logger.info("Calibration of destination choice: Iteration = " + iteration);
            calibrationMatrixDc = calculateCalibrationMatrix(allTrips);
            dcModel.updatedomDcCalibrationV(calibrationMatrixDc[0]);
            dcOutboundModel.updateIntOutboundCalibrationV(calibrationMatrixDc[1]);
            dcInBoundModel.updateIntInboundCalibrationV(calibrationMatrixDc[2]);

            runDc();
            runMc();

            logger.info("Calibration of mode choice: Iteration = " + iteration);
            calibrationMatrixMc = calculateMCCalibrationFactors(allTrips);
            mcDomesticModel.updateCalibrationDomestic(calibrationMatrixMc[0]);
            mcDomesticModel.updateCalibrationDomesticVisitors(calibrationMatrixMc[3]);
            intModeChoice.updateCalibrationOutbound(calibrationMatrixMc[1]);
            intModeChoice.updateCalibrationInbound(calibrationMatrixMc[2]);

            //runDestinationChoice(allTrips);
            runDc();
            runMc();


            //getAverageModalShares(allTrips);



        }


        /*dcM.run(dataSet, -1);
        mcM.run(dataSet, -1);*/

        printOutCalibrationResults(dcModel, dcOutboundModel, dcInBoundModel, mcDomesticModel, intModeChoice);

        //dataSet.setAllTrips(allTrips);

        /*printOutCalibrationResults(dataSet.getDcDomestic(),
                 dataSet.getDcIntOutbound(), dataSet.getDcIntInbound(), dataSet.getMcDomestic(), dataSet.getMcInt());*/

    }


    public double[][] getAverageTripDistances(ArrayList<LongDistanceTrip> allTrips) {

        double[][] averageDistances = new double[3][3];
        double[][] counts = new double[3][3];

        for (LongDistanceTrip t : allTrips) {
            if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getDestZoneType().equals(ZoneType.ONTARIO)) {
                //trip that starts in Ontario or end in ontario
                if (!t.isInternational()) {
                    //domestic from Ontario - row 0
                    if (t.getTravelDistanceLevel2() < 2000) {
                        averageDistances[0][t.getTripPurpose()] += t.getTravelDistanceLevel2() * getTripWeight(t);
                        counts[0][t.getTripPurpose()] += getTripWeight(t);
                    }

                } else if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from ontario to us - row 1
                    if (t.getTravelDistanceLevel2() < 4000) {
                        averageDistances[1][t.getTripPurpose()] += t.getTravelDistanceLevel2() * getTripWeight(t);
                        counts[1][t.getTripPurpose()] += getTripWeight(t);
                    }
                } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS) /*&& t.getDestZoneType().equals(ZoneType.ONTARIO)*/) {
                    //international from US to ontario + row 2
                    if (t.getTravelDistanceLevel2() < 4000) {
                        averageDistances[2][t.getTripPurpose()] += t.getTravelDistanceLevel2() * getTripWeight(t);
                        counts[2][t.getTripPurpose()] += getTripWeight(t);
                    }
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (counts[i][j] > 0) averageDistances[i][j] = averageDistances[i][j] / counts[i][j];
            }
        }

        logger.info("dc,type0,distance,visit," + averageDistances[0][0] + ",business," + averageDistances[0][1] + ",leisure," + averageDistances[0][2]);
        logger.info("dc,type1,distance,visit," + averageDistances[1][0] + ",business," + averageDistances[1][1] + ",leisure," + averageDistances[1][2]);
        logger.info("dc,type2,distance,visit," + averageDistances[2][0] + ",business," + averageDistances[2][1] + ",leisure," + averageDistances[2][2]);
        return averageDistances;

    }

    public double[][] calculateCalibrationMatrix(ArrayList<LongDistanceTrip> allTrips) {

        double[][] averageDistances = getAverageTripDistances(allTrips);

        double[][] calibrationMatrix = new double[3][3];

        double expansionFactor = 1;

        //hard coded for calibration
        calibrationMatrix[0][0] = (averageDistances[0][0] / 133 - 1) * expansionFactor + 1; //domestic visit
        calibrationMatrix[0][1] = (averageDistances[0][1] / 175 - 1) * expansionFactor + 1; //domestic business
        calibrationMatrix[0][2] = (averageDistances[0][2] / 134 - 1) * expansionFactor + 1; //domestic leisure
        calibrationMatrix[1][0] = (averageDistances[1][0] / 642 - 1) * expansionFactor + 1;//to us visit
        calibrationMatrix[1][1] = (averageDistances[1][1] / 579 - 1) * expansionFactor + 1;//to us business
        calibrationMatrix[1][2] = (averageDistances[1][2] / 515 - 1) * expansionFactor + 1;//to us leisure
        calibrationMatrix[2][0] = (averageDistances[2][0] / 697 - 1) * expansionFactor + 1;//from us visit
        calibrationMatrix[2][1] = (averageDistances[2][1] / 899 - 1) * expansionFactor + 1;//from us business
        calibrationMatrix[2][2] = (averageDistances[2][2] / 516 - 1) * expansionFactor + 1;//from us leisure


        logger.info("dc,type0,k,visit," + calibrationMatrix[0][0] + ",business," + calibrationMatrix[0][1] + ",leisure," + calibrationMatrix[0][2]);
        logger.info("dc,type1,k,visit," + calibrationMatrix[1][0] + ",business," + calibrationMatrix[1][1] + ",leisure," + calibrationMatrix[1][2]);
        logger.info("dc,type2,k,visit," + calibrationMatrix[2][0] + ",business," + calibrationMatrix[2][1] + ",leisure," + calibrationMatrix[2][2]);

        return calibrationMatrix;

    }

    public double[][][] getAverageModalShares(ArrayList<LongDistanceTrip> allTrips) {
        double[][][] countsByMode = new double[4][3][4];

        //int tripCounter = 0;

        for (LongDistanceTrip t : allTrips) {
            if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getDestZoneType().equals(ZoneType.ONTARIO)) {
                if (!t.isInternational()) {
                    //domestic to or from ontario
                    if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
                        //domestic from Ontario - row 0
                        countsByMode[0][t.getTripPurpose()][t.getMode()] += getTripWeight(t);
                    } else {
                        //domestic to Ontario - row 3
                        countsByMode[3][t.getTripPurpose()][t.getMode()] += getTripWeight(t);
                    }


                } else if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from ontario to US - row 1
                    countsByMode[1][t.getTripPurpose()][t.getMode()] += getTripWeight(t);

                } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS)) {
                    //international from US to ontario - row 2
                    countsByMode[2][t.getTripPurpose()][t.getMode()] += getTripWeight(t);
                }
            }
        }


        double[][][] modalShares = new double[4][3][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                String log = "mc,shares,type," + i;
                double total = countsByMode[i][j][0] + countsByMode[i][j][1] + countsByMode[i][j][2] + countsByMode[i][j][3];
                if (total > 0) {
                    for (int m = 0; m < 4; m++) {
                        modalShares[i][j][m] = countsByMode[i][j][m] / total;
                        log += ",purpose," + j + ",mode" + m + "," + modalShares[i][j][m];
                    }
                }
                logger.info(log);
                //logger.info(total);
            }
        }

        return modalShares;
    }

    public double[][][] calculateMCCalibrationFactors(ArrayList<LongDistanceTrip> allTrips) {

        double[][][] calibrationMatrix = new double[4][3][4];

        double[][][] modalShares = getAverageModalShares(allTrips);

        double[][][] surveyShares = new double[4][3][4];

        double expansionFactor = 1;

        //hard coded for calibration
        //domestic
        int type = 0;
        surveyShares[type][0][0] = 0.93; // visit - auto
        surveyShares[type][0][1] = 0.01; // visit - air
        surveyShares[type][0][2] = 0.03; // visit - rail
        surveyShares[type][0][3] = 0.03; // visit - bus

        surveyShares[type][1][0] = 0.86; // business
        surveyShares[type][1][1] = 0.06; // business
        surveyShares[type][1][2] = 0.03; // business
        surveyShares[type][1][3] = 0.05; // business

        surveyShares[type][2][0] = 0.96; // leisure
        surveyShares[type][2][1] = 0.00; // leisure
        surveyShares[type][2][2] = 0.03; // leisure
        surveyShares[type][2][3] = 0.01; // leisure

        //int outbound
        type = 1;
        surveyShares[type][0][0] = 0.76; // visit - auto
        surveyShares[type][0][1] = 0.23; // visit - air
        surveyShares[type][0][2] = 0.00; // visit - rail
        surveyShares[type][0][3] = 0.01; // visit - bus

        surveyShares[type][1][0] = 0.74; // business
        surveyShares[type][1][1] = 0.25; // business
        surveyShares[type][1][2] = 0.00; // business
        surveyShares[type][1][3] = 0.01; // business

        surveyShares[type][2][0] = 0.87; // leisure
        surveyShares[type][2][1] = 0.10; // leisure
        surveyShares[type][2][2] = 0.00; // leisure
        surveyShares[type][2][3] = 0.01; // leisure

        //int inbound
        type = 2;
        surveyShares[type][0][0] = 0.75; // visit - auto
        surveyShares[type][0][1] = 0.24; // visit - air
        surveyShares[type][0][2] = 0.00; // visit - rail
        surveyShares[type][0][3] = 0.01; // visit - bus

        surveyShares[type][1][0] = 0.39; // business
        surveyShares[type][1][1] = 0.60; // business
        surveyShares[type][1][2] = 0.00; // business
        surveyShares[type][1][3] = 0.01; // business

        surveyShares[type][2][0] = 0.85; // leisure
        surveyShares[type][2][1] = 0.06; // leisure
        surveyShares[type][2][2] = 0.00; // leisure
        surveyShares[type][2][3] = 0.09; // leisure

        type = 3;
        surveyShares[type][0][0] = 0.67; // visit - auto
        surveyShares[type][0][1] = 0.24; // visit - air
        surveyShares[type][0][2] = 0.05; // visit - rail
        surveyShares[type][0][3] = 0.04; // visit - bus

        surveyShares[type][1][0] = 0.35; // business
        surveyShares[type][1][1] = 0.59; // business
        surveyShares[type][1][2] = 0.02; // business
        surveyShares[type][1][3] = 0.04; // business

        surveyShares[type][2][0] = 0.84; // leisure
        surveyShares[type][2][1] = 0.08; // leisure
        surveyShares[type][2][2] = 0.06; // leisure
        surveyShares[type][2][3] = 0.02; // leisure

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                String log = "mc,k,type," + i;
                for (int m = 0; m < 4; m++) {
                    calibrationMatrix[i][j][m] = (surveyShares[i][j][m] - modalShares[i][j][m]) * expansionFactor;
                    log += ",purpose," + j + ",mode" + m + "," + calibrationMatrix[i][j][m];
                }
                logger.info(log);
            }
        }

        return calibrationMatrix;

        /*type = 0;
        System.out.println("domestic");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        type = 1;
        System.out.println("international_outbound");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        type = 2;
        System.out.println("international_inbound");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        type = 3;
        System.out.println("domestic visitors ");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        */

    }


    public double getTripWeight(LongDistanceTrip t) {
        double weight = 0;
        switch (t.getTripState()) {
            case 0:
                weight = 0;
                break;
            case 1:
                weight = 1;
                break;
            case 2:
                weight = 0.5;
                break;
        }

        //tripStates = Arrays.asList("away", "daytrip", "inout");
        return weight;
    }

    public void printOutCalibrationResults(DomesticDestinationChoice domDc, IntOutboundDestinationChoice intOutDc, IntInboundDestinationChoice intInDc,
                                           DomesticModeChoice domMc, IntModeChoice intMc) {

        logger.info("---------------------------------------------------------");
        logger.info("-----------------RESULTS DC------------------------------");
        logger.info("k_domestic_dc visit = " + domDc.getDomDcCalibrationV()[0]);
        logger.info("k_domestic_dc business = " + domDc.getDomDcCalibrationV()[1]);
        logger.info("k_domestic_dc leisure = " + domDc.getDomDcCalibrationV()[2]);
        logger.info("k_int_out_dc visit = " + intOutDc.getCalibrationV()[0]);
        logger.info("k_int_out_dc business = " + intOutDc.getCalibrationV()[1]);
        logger.info("k_int_out_dc leisure = " + intOutDc.getCalibrationV()[2]);
        logger.info("k_int_in_dc visit = " + intInDc.getCalibrationV()[0]);
        logger.info("k_int_in_dc business = " + intInDc.getCalibrationV()[1]);
        logger.info("k_int_in_dc leisure = " + intInDc.getCalibrationV()[2]);
        logger.info("---------------------------------------------------------");

        logger.info("---------------------------------------------------------");
        logger.info("-----------------RESULTS MC------------------------------");
        String type = "k_domestic_mc_";
        logger.info(type + "visit: auto=" + domMc.getCalibrationMatrix()[0][0] +
                ",air=" + domMc.getCalibrationMatrix()[0][1] +
                ",rail=" + domMc.getCalibrationMatrix()[0][2] +
                ",bus=" + domMc.getCalibrationMatrix()[0][3]);
        logger.info(type + "business: auto=" + domMc.getCalibrationMatrix()[1][0] +
                ",air=" + domMc.getCalibrationMatrix()[1][1] +
                ",rail=" + domMc.getCalibrationMatrix()[1][2] +
                ",bus=" + domMc.getCalibrationMatrix()[1][3]);
        logger.info(type + "leisure: auto=" + domMc.getCalibrationMatrix()[2][0] +
                ",air=" + domMc.getCalibrationMatrix()[2][1] +
                ",rail=" + domMc.getCalibrationMatrix()[2][2] +
                ",bus=" + domMc.getCalibrationMatrix()[2][3]);
        type = "k_int_out_mc_";
        logger.info(type + "visit: auto=" + intMc.getCalibrationMatrixOutbound()[0][0] +
                ",air=" + intMc.getCalibrationMatrixOutbound()[0][1] +
                ",rail=" + intMc.getCalibrationMatrixOutbound()[0][2] +
                ",bus=" + intMc.getCalibrationMatrixOutbound()[0][3]);
        logger.info(type + "business: auto=" + intMc.getCalibrationMatrixOutbound()[1][0] +
                ",air=" + intMc.getCalibrationMatrixOutbound()[1][1] +
                ",rail=" + intMc.getCalibrationMatrixOutbound()[1][2] +
                ",bus=" + intMc.getCalibrationMatrixOutbound()[1][3]);
        logger.info(type + "leisure: auto=" + intMc.getCalibrationMatrixOutbound()[2][0] +
                ",air=" + intMc.getCalibrationMatrixOutbound()[2][1] +
                ",rail=" + intMc.getCalibrationMatrixOutbound()[2][2] +
                ",bus=" + intMc.getCalibrationMatrixOutbound()[2][3]);
        type = "k_int_in_mc";
        logger.info(type + "visit: auto=" + intMc.getCalibrationMatrixInbound()[0][0] +
                ",air=" + intMc.getCalibrationMatrixInbound()[0][1] +
                ",rail=" + intMc.getCalibrationMatrixInbound()[0][2] +
                ",bus=" + intMc.getCalibrationMatrixInbound()[0][3]);
        logger.info(type + "business: auto=" + intMc.getCalibrationMatrixInbound()[1][0] +
                ",air=" + intMc.getCalibrationMatrixInbound()[1][1] +
                ",rail=" + intMc.getCalibrationMatrixInbound()[1][2] +
                ",bus=" + intMc.getCalibrationMatrixInbound()[1][3]);
        logger.info(type + "leisure: auto=" + intMc.getCalibrationMatrixInbound()[2][0] +
                ",air=" + intMc.getCalibrationMatrixInbound()[2][1] +
                ",rail=" + intMc.getCalibrationMatrixInbound()[2][2] +
                ",bus=" + intMc.getCalibrationMatrixInbound()[2][3]);


        type = "k_domesticVisitors_mc_";
        logger.info(type + "visit: auto=" + domMc.getCalibrationMatrixVisitors()[0][0] +
                ",air=" + domMc.getCalibrationMatrixVisitors()[0][1] +
                ",rail=" + domMc.getCalibrationMatrixVisitors()[0][2] +
                ",bus=" + domMc.getCalibrationMatrixVisitors()[0][3]);
        logger.info(type + "business: auto=" + domMc.getCalibrationMatrixVisitors()[1][0] +
                ",air=" + domMc.getCalibrationMatrixVisitors()[1][1] +
                ",rail=" + domMc.getCalibrationMatrixVisitors()[1][2] +
                ",bus=" + domMc.getCalibrationMatrixVisitors()[1][3]);
        logger.info(type + "leisure: auto=" + domMc.getCalibrationMatrixVisitors()[2][0] +
                ",air=" + domMc.getCalibrationMatrixVisitors()[2][1] +
                ",rail=" + domMc.getCalibrationMatrixVisitors()[2][2] +
                ",bus=" + domMc.getCalibrationMatrixVisitors()[2][3]);
        logger.info("---------------------------------------------------------");

    }

    public void runDc() {
        logger.info("Running Destination Choice Model for " + allTrips.size() + " trips");
        allTrips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
            if(t.getTripState()!= 0) {
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
            }

        });

    }

    public void runMc() {
        logger.info("Running Mode Choice Model for " + allTrips.size() + " trips");
        allTrips.parallelStream().forEach(t -> {
            if(t.getTripState()!= 0) {
                if (!t.isInternational()) {
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
            }

        });

    }




}

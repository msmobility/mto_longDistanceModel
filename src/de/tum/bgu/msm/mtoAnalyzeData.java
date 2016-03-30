package de.tum.bgu.msm;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Class to hold person object of travelers and non-travelers of the TSRC survey
 *
 * @author Rolf Moeckel
 * Created on 26 Feb. 2016 in Vienna, VA
 *
 **/

public class mtoAnalyzeData {
    static Logger logger = Logger.getLogger(mto.class);
    private ResourceBundle rb;
    private mtoData data;


    public mtoAnalyzeData(ResourceBundle rb, mtoData data) {
        this.rb = rb;
        this.data = data;
        // Constructor
    }


    public void runAnalyses () {
        // run selected long-distance travel analyzes
        if (!ResourceUtil.getBooleanProperty(rb, "analyze.ld.data")) return;
        logger.info("Running selected analyses of long-distance data");
        countTravelersByIncome();
        tripsByModeAndOriginProvince();
    }


    private void countTravelersByIncome () {

        int[] incomeCountTrips = new int[10];
        int[] incomeCountPersons = new int[10];
        surveyPerson[] spList = surveyPerson.getPersonArray();
        for (surveyPerson sp: spList) {
            incomeCountPersons[sp.getHhIncome()] += sp.getWeight();
            if (sp.getNumberOfTrips() > 0) {
                incomeCountTrips[sp.getHhIncome()] += sp.getWeight() * sp.getNumberOfTrips();
            }
        }

        logger.info("Travelers and non-travelers by income");
        for (int inc = 1; inc <= 4; inc++) System.out.println("IncomeGroupPersons" + inc + ": " + incomeCountPersons[inc]);
        System.out.println("IncomeNotStatedPersons: " + incomeCountPersons[9]);
        for (int inc = 1; inc <= 4; inc++) System.out.println("IncomeGroupTrips" + inc + ": " + incomeCountTrips[inc]);
        System.out.println("IncomeNotStatedTrips: " + incomeCountTrips[9]);
    }


    private void tripsByModeAndOriginProvince() {

        // Create HashMap by destination province by mode
        HashMap<Integer, Float[]> destinationCounter = new HashMap<>();
        for (int pr: data.getProvinceList().getColumnAsInt("Code")) {
            destinationCounter.put(pr, new Float[]{0f,0f,0f,0f,0f,0f,0f,0f,0f});
        }

        // Create list of main modes
        int[] mainModes = data.getMainModeList().getColumnAsInt("Code");
        int highestCode = util.getHighestVal(mainModes);
        int[] mainModeIndex = new int[highestCode + 1];
        for (int mode = 0; mode < mainModes.length; mode++) {
            mainModeIndex[mainModes[mode]] = mode;
        }

        // Create list of CMA regions
        int[] homeCmaList = data.getCmaList().getColumnAsInt("CMAUID");
        int[] cmaIndex = new int[util.getHighestVal(homeCmaList) + 1];
        for (int i = 0; i < homeCmaList.length; i++) cmaIndex[homeCmaList[i]] = i;
        float[] tripsByHomeCma = new float[homeCmaList.length];

        // Create list of trip purposes
        int[] purpList = data.getTripPurposes().getColumnAsInt("Code");
        int highestPurp = util.getHighestVal(purpList);
        int[] purposeIndex = new int[highestPurp + 1];
        for (int purp = 0; purp < purpList.length; purp++) purposeIndex[purpList[purp]] = purp;
        float[] purpCnt = new float[purpList.length];

        float[][] modePurpCnt = new float[mainModes.length][purpList.length];
        int[] stopFrequency = new int[100];

//      iterate over all tours:
        for (surveyPerson sp: surveyPerson.getPersonArray()) {
            if (sp.getNumberOfTrips() == 0) continue;  //Person did not make long-distance trip
            float weight = sp.getWeight();
            ArrayList<Integer> ldTrips = sp.tours;
            for (int tripRecord: ldTrips) {
                surveyTour st = surveyTour.getTourFromId(tripRecord);
                int origProvince = st.getOrigProvince();
                if (origProvince != 35) continue;
                System.out.println(sp.pumfId+" "+tripRecord+" "+origProvince);
                int destProvince = st.getDestProvince();
                int mainMode = st.getMainMode();
                int homeCma = st.getHomeCma();
                int tripPurp = st.getTripPurp();
                Float[] tripsByMode = destinationCounter.get(destProvince);
                tripsByMode[mainModeIndex[mainMode]] = tripsByMode[mainModeIndex[mainMode]] + weight;
                if (util.containsElement(homeCmaList, homeCma)) tripsByHomeCma[cmaIndex[homeCma]] += weight;
                purpCnt[purposeIndex[tripPurp]] += weight;
                modePurpCnt[mainModeIndex[mainMode]][purposeIndex[tripPurp]] += weight;
                stopFrequency[st.getNumberOfStop()]++;
            }
        }
        String txt1 = "Destination";
        for (int mode : mainModes)
            txt1 += "," + data.getMainModeList().getIndexedStringValueAt(mode, "MainMode");
        logger.info(txt1);
        for (Integer pr : data.getProvinceList().getColumnAsInt("Code")) {
            String txt2 = "Trips to " + pr + " (" + data.getProvinceList().getIndexedStringValueAt(pr, "Province") + ")";
            for (int mode : mainModes) txt2 += "," + destinationCounter.get(pr)[mainModeIndex[mode]];
            logger.info(txt2);
        }
        logger.info("Trips by purpose");
        for (int i: purpList) {
            if (purpCnt[purposeIndex[i]] > 0) logger.info(data.getTripPurposes().getIndexedStringValueAt(i, "Purpose") + ";" + purpCnt[purposeIndex[i]]);
        }

        logger.info("Trip origin by CMA");
        for (int i: homeCmaList) {
            if (tripsByHomeCma[cmaIndex[i]] > 0) logger.info(i + " " + tripsByHomeCma[cmaIndex[i]]);
        }

        logger.info("Trips by mode and purpose");
        String tx = "Purpose";
        for (int mode: mainModes) tx = tx.concat("," + data.getMainModeList().getIndexedStringValueAt(mode, "MainMode"));
        logger.info(tx);
        for (int purp: purpList) {
            tx = data.getTripPurposes().getIndexedStringValueAt(purp, "Purpose");
            for (int mode: mainModes) {
                tx = tx.concat("," + modePurpCnt[mainModeIndex[mode]][purposeIndex[purp]]);
            }
            logger.info(tx);
        }
        logger.info ("Tour stop frequency:");
        for (int i = 0; i < stopFrequency.length; i++) logger.info(i + " stops: " + stopFrequency[i]);
    }
}

package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.*;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Class to hold person object of travelers and non-travelers of the TSRC survey
 *
 * @author Rolf Moeckel
 *         Created on 26 Feb. 2016 in Vienna, VA
 **/

public class mtoAnalyzeData {
    static Logger logger = Logger.getLogger(mtoAnalyzeData.class);
    private ResourceBundle rb;
    private mtoSurveyData data;


    public mtoAnalyzeData(ResourceBundle rb, mtoSurveyData data) {
        this.rb = rb;
        this.data = data;
        // Constructor
    }


    public void runAnalyses() {
        // runDataAnalysis selected long-distance travel analyzes
        logger.info("Running selected analyses of long-distance data");
        if (ResourceUtil.getBooleanProperty(rb, "log.tsrc.summary")) {
            countTravelersByIncome();
            tripsByModeAndOriginProvince();
        }
        if (ResourceUtil.getBooleanProperty(rb, "write.tsrc.data")) writeOutData();
    }


    private void countTravelersByIncome() {

        int[] incomeCountTrips = new int[10];
        int[] incomeCountPersons = new int[10];
        surveyPerson[] spList = surveyPerson.getPersonArray();
        for (surveyPerson sp : spList) {
            incomeCountPersons[sp.getHhIncome()] += sp.getWeight();
            if (sp.getNumberOfTrips() > 0) {
                incomeCountTrips[sp.getHhIncome()] += sp.getWeight() * sp.getNumberOfTrips();
            }
        }

        logger.info("Travelers and non-travelers by income");
        for (int inc = 1; inc <= 4; inc++)
            System.out.println("IncomeGroupPersons" + inc + ": " + incomeCountPersons[inc]);
        System.out.println("IncomeNotStatedPersons: " + incomeCountPersons[9]);
        for (int inc = 1; inc <= 4; inc++) System.out.println("IncomeGroupTrips" + inc + ": " + incomeCountTrips[inc]);
        System.out.println("IncomeNotStatedTrips: " + incomeCountTrips[9]);
    }


    private void tripsByModeAndOriginProvince() {

        // Create HashMap by destination province by mode
        HashMap<Integer, Float[]> destinationCounter = new HashMap<>();
        for (int pr : data.getProvinceList().getColumnAsInt("Code")) {
            destinationCounter.put(pr, new Float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f});
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
        for (surveyPerson sp : surveyPerson.getPersonArray()) {
            if (sp.getNumberOfTrips() == 0) continue;  //Person did not make long-distance trip
            float weight = sp.getWeight();
            ArrayList<Long> ldTrips = sp.tours;
            for (long tripRecord : ldTrips) {
                surveyTour st = surveyTour.getTourFromId(tripRecord);
                int origProvince = st.getOrigProvince();
                if (origProvince != 35) continue;
                System.out.println(sp.pumfId + " " + tripRecord + " " + origProvince);
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
        for (int i : purpList) {
            if (purpCnt[purposeIndex[i]] > 0)
                logger.info(data.getTripPurposes().getIndexedStringValueAt(i, "Purpose") + ";" + purpCnt[purposeIndex[i]]);
        }

        logger.info("Trip origin by CMA");
        for (int i : homeCmaList) {
            if (tripsByHomeCma[cmaIndex[i]] > 0) logger.info(i + " " + tripsByHomeCma[cmaIndex[i]]);
        }

        logger.info("Trips by mode and purpose");
        String tx = "Purpose";
        for (int mode : mainModes)
            tx = tx.concat("," + data.getMainModeList().getIndexedStringValueAt(mode, "MainMode"));
        logger.info(tx);
        for (int purp : purpList) {
            tx = data.getTripPurposes().getIndexedStringValueAt(purp, "Purpose");
            for (int mode : mainModes) {
                tx = tx.concat("," + modePurpCnt[mainModeIndex[mode]][purposeIndex[purp]]);
            }
            logger.info(tx);
        }
        logger.info("Tour stop frequency:");
        for (int i = 0; i < stopFrequency.length; i++) logger.info(i + " stops: " + stopFrequency[i]);
    }


    public void writeOutData() {
        // write out travel data for model estimation
        logger.info("Writing out data for external model estimation");
        String fileName = ResourceUtil.getProperty(rb, "tsrc.out.file");
        PrintWriter pw = util.openFileForSequentialWriting(fileName + ".csv", false);
        String[] purposes = {"Holiday", "Visit", "Business", "Other"};
        pw.print("id,year,month,ageGroup,gender,adultsInHousehold,kidsInHousehold,education,laborStatus,province,cma," +
                "income,expansionFactor,longDistanceTrips,daysAtHome");
        for (String txt : purposes)
            pw.print(",daysOnInOutboundTravel" + txt + ",daysOnDaytrips" + txt + ",daysAway" + txt);
        pw.println();

        for (surveyPerson sp : surveyPerson.getPersonArray()) {
            ArrayList<Long> tours = sp.getTours();
            int[] daysInOut = new int[purposes.length];
            int[] daysDayTrip = new int[purposes.length];
            int[] daysAway = new int[purposes.length];
            int daysHome = util.getDaysOfMonth(sp.getRefYear(), sp.getRefMonth());
            // First, count day trips
            for (long tour : tours) {
                surveyTour st = surveyTour.getTourFromId(tour);
                int tripPurp = 0;
                try {
                    tripPurp = translateTripPurpose(purposes, st.getTripPurp());
                } catch (Exception e) {
                    logger.error(tour); //+" "+st.tourStops+" "+st.getOrigProvince());
                    logger.error(st.getTripPurp());
                }
                if (st.getNumberNights() == 0) {
                    daysDayTrip[tripPurp]++;
                    daysHome--;
                }
            }
            // Next, add trips with overnight stay, ensuring that noone exceeds 30 days per month
            for (long tour : tours) {
                surveyTour st = surveyTour.getTourFromId(tour);
                int tripPurp = translateTripPurpose(purposes, st.getTripPurp());
                if (st.getNumberNights() > 0) {
                    // Assumption: No trip is recorded for more than 30 days. If trip lasted longer than daysHome left,
                    // only the return trip is counted (as the outbound trip must have happened the previous month)
                    if (daysHome > 0) {
                        daysInOut[tripPurp]++;                            // return trip
                        daysHome--;
                    }
                    daysAway[tripPurp] += Math.min(st.getNumberNights() - 1, daysHome); // days away
                    daysHome -= Math.min(st.getNumberNights() - 1, daysHome);
                    if (daysHome > 0) {
                        daysInOut[tripPurp]++;                            // outbound trip
                        daysHome--;
                    }
                }
            }
            pw.print(sp.getPumfId() + "," + sp.getRefYear() + "," + sp.getRefMonth() + "," + sp.getAgeGroup() + "," + sp.getGender() + "," +
                    sp.getAdultsInHh() + "," + sp.getKidsInHh() + "," + sp.getEducation() + "," + sp.getLaborStat() +
                     "," + sp.getProv() + "," + sp.getCma() + "," + sp.getHhIncome() + "," + sp.getWeight() + "," +
                    sp.getNumberOfTrips() + "," + daysHome);
            for (int i = 0; i < purposes.length; i++)
                pw.print("," + daysInOut[i] + "," + daysDayTrip[i] + "," +
                        daysAway[i]);
            pw.println();
        }
        pw.close();
    }


    private int translateTripPurpose(String[] purposes, int purp) {

        int[] code = new int[8];
        code[0] = 0;
        code[1] = 0;
        code[2] = 1;
        code[3] = 3;
        code[4] = 3;
        code[5] = 3;
        code[6] = 2;
        code[7] = 2;
        //logger.info("Translated purpose " + purp + " into code " + code[purp] + " (" + purposes[code[purp]] + ")");
        return code[purp];
    }
}

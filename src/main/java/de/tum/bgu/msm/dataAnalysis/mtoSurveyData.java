package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Class to hold data
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 14 December 2015
 * Version 1
 *
 */
public class mtoSurveyData {
    static Logger logger = Logger.getLogger(mto.class);
    private ResourceBundle rb;
    private String workDirectory;
    private TableDataSet provinceList;
    private TableDataSet mainModeList;
    private TableDataSet cmaList;
    private TableDataSet tripPurposes;


    public mtoSurveyData(ResourceBundle rb) {
        this.rb = rb;
    }


    public void readInput () {
        // read input data
        workDirectory = rb.getString("work.directory");

        provinceList = util.readCSVfile(rb.getString("province.list"));
        provinceList.buildIndex(provinceList.getColumnPosition("Code"));

        mainModeList = util.readCSVfile(rb.getString("main.mode.list"));
        mainModeList.buildIndex(mainModeList.getColumnPosition("Code"));

        cmaList = util.readCSVfile(rb.getString("cma.list"));
        cmaList.buildIndex(cmaList.getColumnPosition("CMAUID"));

        tripPurposes = util.readCSVfile(rb.getString("trip.purp"));
        tripPurposes.buildIndex(tripPurposes.getColumnPosition("Code"));

        // read all TSRC data
        for (int year: ResourceUtil.getIntegerArray(rb, "tsrc.years")) readTSRCdata(year);
        // read all ITS data
        for (int year: ResourceUtil.getIntegerArray(rb, "its.years")) readITSdata(year);

    }


    public void readITSdata(int year) {
        // read ITS data
        logger.info ("  Reading ITS data for " + year);
        String fileName = workDirectory + rb.getString("its.data.dir") + "/" + ResourceUtil.getProperty(rb, "its.data");
        String recString;
        int recCount = 0;
        PrintWriter out = util.openFileForSequentialWriting(rb.getString("its.out.file") + ".csv", false);
        out.println("province,cma,weight");
//        float[][] purp = new float[5][365];
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            while ((recString = in.readLine()) != null) {
                recCount++;
                String origProvince = recString.substring(8, 10);  // ascii position in file: 009-010
                String origCMA =      recString.substring(10, 14);  // ascii position in file: 011-014
                if (origProvince.equals("35")){
                    // origin == ontario
                    recCount++;
                    int purpose =      convertToInteger(recString.substring( 18, 19));  // ascii position in file: 019-019
                    int entryMode =    convertToInteger(recString.substring( 25, 26));  // ascii position in file: 026-026
                    int country =      convertToInteger(recString.substring( 38, 43));  // ascii position in file: 039-043
                    int nightsByPlace[] = new int[11];
                    nightsByPlace[1] = convertToInteger(recString.substring( 48, 51));  // ascii position in file: 049-051
                    nightsByPlace[2] = convertToInteger(recString.substring( 75, 78));  // ascii position in file: 076-078
                    nightsByPlace[3] = convertToInteger(recString.substring(102,105));  // ascii position in file: 103-105
                    nightsByPlace[4] = convertToInteger(recString.substring(129,132));  // ascii position in file: 130-132
                    nightsByPlace[5] = convertToInteger(recString.substring(156,159));  // ascii position in file: 157-159
                    nightsByPlace[6] = convertToInteger(recString.substring(183,186));  // ascii position in file: 184-186
                    nightsByPlace[7] = convertToInteger(recString.substring(210,213));  // ascii position in file: 211-213
                    nightsByPlace[8] = convertToInteger(recString.substring(237,240));  // ascii position in file: 238-240
                    nightsByPlace[9] = convertToInteger(recString.substring(264,267));  // ascii position in file: 265-267
                    nightsByPlace[10]= convertToInteger(recString.substring(291,294));  // ascii position in file: 292-294
                    nightsByPlace[0] = convertToInteger(recString.substring(345,348));  // ascii position in file: 346-348
//                    purp[purpose][nightsByPlace[0]] += weight;
                }
                float weight =  convertToFloat(recString.substring(475,491));    // ascii position in file: 476-492
                out.println(origProvince + "," + origCMA + "," + weight);
            }
            out.close();
        } catch (Exception e) {
            logger.error("Could not read ITS data: " + e);
        }
        logger.info("  Read " + recCount + " ITS records with a residence in Ontario (35)");

//        for (int days=0;days<365;days++) logger.info("Days " + days + ": " + (purp[1][days]+purp[3][days]) + "," +
//                purp[2][days] + "," + purp[4][days]);
    }


    public void readTSRCdata(int year) {
        // read TSRC data
        logger.info ("  Reading TSRC data for " + year);

        String dirName = workDirectory + ResourceUtil.getProperty(rb, ("tsrc.data.dir"));

        readTSRCpersonData(dirName, year);
        readTSRCtripData(dirName, year);
        readTSRCvisitData(dirName, year);

//        // Create HashMap by destination province by mode
//        HashMap<Integer, Integer[]> destinationCounter = new HashMap<>();
//        for (int pr: provinceList.getColumnAsInt("Code")) {
//            destinationCounter.put(pr, new Integer[]{0,0,0,0,0,0,0,0,0});
//        }
//
//        // Create list of main modes
//        int[] mainModes = mainModeList.getColumnAsInt("Code");
//        int highestCode = util.getHighestVal(mainModes);
//        int[] mainModeIndex = new int[highestCode + 1];
//        for (int mode = 0; mode < mainModes.length; mode++) {
//            mainModeIndex[mainModes[mode]] = mode;
//        }
//
//        // Create list of CMA regions
//        int[] homeCmaList = cmaList.getColumnAsInt("CMAUID");
//        int[] cmaIndex = new int[util.getHighestVal(homeCmaList) + 1];
//        for (int i = 0; i < homeCmaList.length; i++) cmaIndex[homeCmaList[i]] = i;
//        int[] tripsByHomeCma = new int[homeCmaList.length];
//
//        // Create list of trip purposes
//        int[] purpList = tripPurposes.getColumnAsInt("Code");
//        int highestPurp = util.getHighestVal(purpList);
//        int[] purposeIndex = new int[highestPurp + 1];
//        for (int purp = 0; purp < purpList.length; purp++) purposeIndex[purpList[purp]] = purp;
//        int[] purpCnt = new int[purpList.length];
//
//        int[][] modePurpCnt = new int[mainModes.length][purpList.length];



//        iterate over all tours:
//        Integer[] tripsByMode = destinationCounter.get(destProvince);
//        tripsByMode[mainModeIndex[mainMode]] = tripsByMode[mainModeIndex[mainMode]] + 1;
//        if (util.containsElement(homeCmaList, homeCma)) tripsByHomeCma[cmaIndex[homeCma]]++;
//        purpCnt[purposeIndex[tripPurp]]++;
//        modePurpCnt[mainModeIndex[mainMode]][purposeIndex[tripPurp]]++;
//
//        String txt1 = "Destination";
//        for (int mode: mainModes) txt1 += "," + mainModeList.getIndexedStringValueAt(mode, "MainMode");
//        logger.info(txt1);
//        for (Integer pr: provinceList.getColumnAsInt("Code")) {
//            String txt2 = "Trips to " + pr + " (" + provinceList.getIndexedStringValueAt(pr, "Province") + ")";
//            for (int mode: mainModes) txt2 += ","+destinationCounter.get(pr)[mainModeIndex[mode]];
//            logger.info(txt2);
//        }
//
//        logger.info("Trips by purpose");
//        for (int i: purpList) {
//            if (purpCnt[purposeIndex[i]] > 0) logger.info(tripPurposes.getIndexedStringValueAt(i, "Purpose") + ";" + purpCnt[purposeIndex[i]]);
//        }
//
//        logger.info("Trip origin by CMA");
//        for (int i: homeCmaList) {
//            if (tripsByHomeCma[cmaIndex[i]] > 0) logger.info(i + " " + tripsByHomeCma[cmaIndex[i]]);
//        }
//
//        logger.info("Trips by mode and purpose");
//        String tx = "Purpose";
//        for (int mode: mainModes) tx = tx.concat("," + mainModeList.getIndexedStringValueAt(mode, "MainMode"));
//        logger.info(tx);
//        for (int purp: purpList) {
//            tx = tripPurposes.getIndexedStringValueAt(purp, "Purpose");
//            for (int mode: mainModes) {
//                tx = tx.concat("," + modePurpCnt[mainModeIndex[mode]][purposeIndex[purp]]);
//            }
//            logger.info(tx);
//        }
    }


    private void readTSRCpersonData (String dirName, int year) {
        // read person file

        String personFileName = ResourceUtil.getProperty(rb, ("tsrc.persons"));
        String recString;
        int totRecCount = 0;
        for (int month = 1; month <= 12; month++) {
            int recCount = 0;
            try {
                String fullFileName;
                if (month <= 9) fullFileName= dirName + File.separator + year + File.separator + personFileName + year +
                        "_Mth0" + month + "_pumf.txt";
                else fullFileName= dirName + File.separator + year + File.separator + personFileName + year +
                        "_Mth" + month + "_pumf.txt";
                BufferedReader in = new BufferedReader(new FileReader(fullFileName));
                while ((recString = in.readLine()) != null) {
                    recCount++;
                    int refYear = convertToInteger(recString.substring(0, 4));  // ascii position in file: 01-04
                    int refMonth = convertToInteger(recString.substring(4, 6));  // ascii position in file: 05-06
                    int origPumfId = convertToInteger(recString.substring(6, 13));  // ascii position in file: 07-13
                    int pumfId = origPumfId * 100 + refYear%100;
                    float weight = convertToFloat(recString.substring(13, 25));  // ascii position in file: 14-25
                    float weight2 = convertToFloat(recString.substring(25, 37));  // ascii position in file: 26-37
                    int prov = convertToInteger(recString.substring(37, 39));  // ascii position in file: 38-39
                    int cma = convertToInteger(recString.substring(42, 46));  // ascii position in file: 43-46
                    int ageGroup = convertToInteger(recString.substring(46, 47));  // ascii position in file: 47-47
                    int gender = convertToInteger(recString.substring(47, 48));  // ascii position in file: 48-48
                    int education = convertToInteger(recString.substring(48, 49));  // ascii position in file: 49-49
                    int laborStat = convertToInteger(recString.substring(49, 50));  // ascii position in file: 50-50
                    int hhIncome = convertToInteger(recString.substring(50, 51));  // ascii position in file: 51-51
                    int adultsInHh = convertToInteger(recString.substring(51, 53));  // ascii position in file: 52-53
                    int kidsInHh = convertToInteger(recString.substring(53, 55));  // ascii position in file: 54-55
                    new surveyPerson(refYear, refMonth, pumfId, weight, weight2, prov, cma, ageGroup, gender, education,
                            laborStat, hhIncome, adultsInHh, kidsInHh);
                }
            } catch (Exception e) {
                logger.error("Could not read TSRC person data: " + e);
            }
            // logger.info("  Read " + recCount + " person records for the month " + month);
            totRecCount += recCount;
        }
        logger.info("  Read " + totRecCount + " person records");
    }


    private void readTSRCtripData (String dirName, int year) {
        // read trip file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.trips"));
        String recString;
        int recCount = 0;
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_pumf.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                recCount++;
                int refYear = convertToInteger(recString.substring(0, 4));  // ascii position in file: 001-004
                int origPumfId = convertToInteger(recString.substring(6, 13));  // ascii position in file: 007-013
                int pumfId = origPumfId * 100 + refYear%100;
                int tripId =       convertToInteger(recString.substring(13, 15));  // ascii position in file: 014-015
                int origProvince = convertToInteger(recString.substring(16, 18));  // ascii position in file: 017-018
                int destProvince = convertToInteger(recString.substring(25, 27));  // ascii position in file: 026-027
                int mainMode =     convertToInteger(recString.substring(79, 81));  // ascii position in file: 080-081
                int homeCma =      convertToInteger(recString.substring(21, 25));  // ascii position in file: 022-025
                int tripPurp =     convertToInteger(recString.substring(72, 74));  // ascii position in file: 073-074
                int numberNights = convertToInteger(recString.substring(120, 123));  // ascii position in file: 121-123
                int numIdentical = convertToInteger(recString.substring(173, 175));  // ascii position in file: 174-175
                new surveyTour(tripId, pumfId, origProvince, destProvince, mainMode, homeCma, tripPurp, numberNights,
                        numIdentical);
                surveyPerson sp = surveyPerson.getPersonFromId(pumfId);
                if (numIdentical < 30) {
                    for (int i = 1; i <= numIdentical; i++) sp.addTour(tripId);
                } else {
                    sp.addTour(tripId);
                }
                recCount++;
            }
        } catch (Exception e) {
            logger.error("Could not read TSRC trip data: " + e);
        }
        logger.info("  Read " + recCount + " tour records.");
    }


    private void readTSRCvisitData (String dirName, int year) {
        // read visit location file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.visits"));
        String recString;
        int recCount = 0;
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_PUMF.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                recCount++;
                int refYear = convertToInteger(recString.substring(0, 4));  // ascii position in file: 001-004
                int origPumfId = convertToInteger(recString.substring( 6, 13));  // ascii position in file: 007-013
                int pumfId = origPumfId * 100 + refYear%100;
                int tripId = convertToInteger(recString.substring(13, 15));  // ascii position in file: 014-015
                int cmarea = convertToInteger(recString.substring(22, 26));  // ascii position in file: 023-026
                int nights = convertToInteger(recString.substring(26, 29));  // ascii position in file: 027-029
                surveyTour st = surveyTour.getTourFromId(util.createTourId(pumfId, tripId));
                st.addTripDestinations (cmarea, nights);
                recCount++;
            }
        } catch (Exception e) {
            logger.error("Could not read TSRC visit data: " + e);
        }
        logger.info("  Read " + recCount + " visit records.");
    }


    private int convertToInteger(String s) {
        // converts s to an integer value, one or two leading spaces are allowed

        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            boolean spacesOnly = true;
            for (int pos = 0; pos < s.length(); pos++) {
                if (!s.substring(pos, pos+1).equals(" ")) spacesOnly = false;
            }
            if (spacesOnly) return -999;
            else {
                logger.fatal("String " + s + " cannot be converted into an integer.");
                return 0;
            }
        }
    }

    private float convertToFloat(String s) {
        // converts s to a float value

        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            if (s.contains(" . ")) return -999;
            boolean spacesOnly = true;
            for (int pos = 0; pos < s.length(); pos++) {
                if (!s.substring(pos, pos+1).equals(" ")) spacesOnly = false;
            }
            if (spacesOnly) return -999;
            else {
                logger.fatal("String " + s + " cannot be converted into a float.");
                return 0;
            }
        }
    }


    public TableDataSet getProvinceList() {
        return provinceList;
    }

    public TableDataSet getMainModeList() {
        return mainModeList;
    }

    public TableDataSet getCmaList() {
        return cmaList;
    }

    public TableDataSet getTripPurposes() {
        return tripPurposes;
    }
}




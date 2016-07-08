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
 * Ontario Provincial Model
 * Class to hold data
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 14 December 2015
 * Version 1
 */
public class mtoSurveyData {
    static Logger logger = Logger.getLogger(mto.class);
    private ResourceBundle rb;
    private String workDirectory;
    private TableDataSet provinceList;
    private TableDataSet mainModeList;
    private TableDataSet cmaList;
    private TableDataSet tripPurposes;
    //added by Carlos Llorca
    private TableDataSet itsCountryCodes;
    private TableDataSet itsPurposes;
    private TableDataSet itsEntryModes;


    public mtoSurveyData(ResourceBundle rb) {
        this.rb = rb;
    }


    public void readInput() {
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

        //added by Carlos Llorca to convert old country codes to new country codes in ITS survey data. TODO Suggest rename files with a "surname" for ITS or TSRC related data
        //TODO add this file name to mto_properties
        String countryCodeFilename = "input/itsCountryCodes.csv";
        itsCountryCodes = util.readCSVfile(countryCodeFilename);
        itsCountryCodes.buildIndex(itsCountryCodes.getColumnPosition("codeOld"));

        //added by Carlos Llorca to convert old purposes
        //TODO add this file name to mto_properties
        String itsPurposesFilename = "input/itsPurposes.csv";
        itsPurposes = util.readCSVfile(itsPurposesFilename);
        itsPurposes.buildIndex(itsPurposes.getColumnPosition("codeOld"));

        //added by Carlos Llorca to convert old entry modes
        //TODO add this file name to mto_properties
        String itsEntryModesFilename = "input/itsEntryModes.csv";
        itsEntryModes = util.readCSVfile(itsEntryModesFilename);
        itsEntryModes.buildIndex(itsEntryModes.getColumnPosition("codeOld"));

        // read all TSRC data
        for (int year : ResourceUtil.getIntegerArray(rb, "tsrc.years")) readTSRCdata(year);
        // read all ITS data
        for (int year : ResourceUtil.getIntegerArray(rb, "its.years")) readITSdata(year);

    }


    public void readITSdata(int year) {

        //edited by Carlos Llorca on 29 June 2016

        //if sentence to select between "old" and "new" ITS data sets.
        if (year == 2013 || year == 2014) {
            // read ITS data from years 2013 and 2014 (namely new)

            logger.info("  Reading ITS data for " + year);
            String fileName = workDirectory + rb.getString("its.data.dir") + "/" + ResourceUtil.getProperty(rb, "its.data") + year + "_PUMF.txt";
            String recString;
            int recCount = 0;
            //purp: matrix that contains the number of trips expanded, with number of nights equal to row number and per purpose (at columns)
            //float[][] purp = new float[5][365];
            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                while ((recString = in.readLine()) != null) {
                    //recCount++;
                    int origProvince = convertToInteger(recString.substring(8, 10));  // ascii position in file: 009-010
                    //comment next line to deactivate ONTARIO filter (the closing } below too!)
                    //if (origProvince.equals("35")){
                    // origin == ontario
                    recCount++;
                    String origCMA = recString.substring(10, 14);  // ascii position in file: 011-01
                    int refYear = year;
                    int origPumfId = convertToInteger(recString.substring(0, 7));  // ascii position in file: 001-007
                    int refQuarter = convertToInteger(recString.substring(7, 8));  // ascii position in file: 008-008
                    int travelParty = convertToInteger(recString.substring(16, 18)); // ascii position in file: 017-018
                    int purpose = convertToInteger(recString.substring(18, 19));  // ascii position in file: 019-019
                    int entryMode = convertToInteger(recString.substring(25, 26));  // ascii position in file: 026-026
                    int country[] = new int[15];
                    country[0] = convertToInteger(recString.substring(38, 43));  // ascii position in file: 039-043
                    //country[0] is the first country visited, equal to country [1]
                    country[1] = convertToInteger(recString.substring(38, 43));  // ascii position in file: 039-043
                    country[2] = convertToInteger(recString.substring(65, 70));  // ascii position in file:
                    country[3] = convertToInteger(recString.substring(92, 97));  // ascii position in file:
                    country[4] = convertToInteger(recString.substring(119, 124));  // ascii position in file:
                    country[5] = convertToInteger(recString.substring(146, 151));  // ascii position in file:
                    country[6] = convertToInteger(recString.substring(174, 178));  // ascii position in file:
                    country[7] = convertToInteger(recString.substring(200, 205));  // ascii position in file:
                    country[8] = convertToInteger(recString.substring(227, 232));  // ascii position in file:
                    country[9] = convertToInteger(recString.substring(254, 259));  // ascii position in file:
                    country[10] = convertToInteger(recString.substring(281, 286));  // ascii position in file:
                    int pumfId = origPumfId * 100 + refYear % 100;
                    int nightsByPlace[] = new int[15];
                    nightsByPlace[1] = convertToInteger(recString.substring(48, 51));  // ascii position in file: 049-051
                    nightsByPlace[2] = convertToInteger(recString.substring(75, 78));  // ascii position in file: 076-078
                    nightsByPlace[3] = convertToInteger(recString.substring(102, 105));  // ascii position in file: 103-105
                    nightsByPlace[4] = convertToInteger(recString.substring(129, 132));  // ascii position in file: 130-132
                    nightsByPlace[5] = convertToInteger(recString.substring(156, 159));  // ascii position in file: 157-159
                    nightsByPlace[6] = convertToInteger(recString.substring(183, 186));  // ascii position in file: 184-186
                    nightsByPlace[7] = convertToInteger(recString.substring(210, 213));  // ascii position in file: 211-213
                    nightsByPlace[8] = convertToInteger(recString.substring(237, 240));  // ascii position in file: 238-240
                    nightsByPlace[9] = convertToInteger(recString.substring(264, 267));  // ascii position in file: 265-267
                    nightsByPlace[10] = convertToInteger(recString.substring(291, 294));  // ascii position in file: 292-294
                    nightsByPlace[0] = convertToInteger(recString.substring(345, 348));  // ascii position in file: 346-348
                    float weight = convertToFloat(recString.substring(475, 491));    // ascii position in file: 476-492


                    //clean multiple US visits to get only 1 trip to US. The
                    for (int i = 1; i < country.length; i++) {
                        for (int j = 1; j < country.length & j != i; j++) {
                            if (country[i] == country[j] & nightsByPlace[i] != 999 & nightsByPlace[j] != 999) {
                                nightsByPlace[i] = nightsByPlace[i] + nightsByPlace[j];
                                nightsByPlace[j] = 999;
                            }
                        }
                    }


                    new surveyIntTravel(origProvince, pumfId, refYear, refQuarter, purpose, entryMode, country, nightsByPlace, weight, travelParty);
                    //store weight (addition) in the purp matrix
                    //purp[purpose][nightsByPlace[0]] += weight;

                    //comment next line to deactivate ONTARIO filter
                    //}
                }
            } catch (Exception e) {
                logger.error("Could not read ITS data: " + e);
            }
            logger.info("  Read " + recCount + " ITS records with a residence in Ontario (35)");

            //write the matrix to calculate probability of being away, inbound or outbound; in the logger

            //...for (int days=0;days<365;days++) logger.info("Days " + days + ": " + (purp[1][days]+purp[3][days]) + "," +
            //        purp[2][days] + "," + purp[4][days]);
        } else if (year == 2011 || year == 2012) {
            // read ITS data from years 2011 and 2012
            logger.info("  Reading ITS data for " + year);
            //provide the names of the files
            // TODO Do it more systematic
            String fileNames[] = new String[8];
            if (year == 2011) {
                fileNames[0] = "CO111.FIN_CUMF.DAT";
                fileNames[1] = "CO112.FIN_CUMF.DAT";
                fileNames[2] = "CO113.FIN_CUMF.DAT";
                fileNames[3] = "CO114.FIN_CUMF.DAT";
                fileNames[4] = "CU111.FIN_CUMF.DAT";
                fileNames[5] = "CU112.FIN_CUMF.DAT";
                fileNames[6] = "CU113.FIN_CUMF.DAT";
                fileNames[7] = "CU114.FIN_CUMF.DAT";

            } else {
                fileNames[0] = "CO121.FIN_CUMF.DAT";
                fileNames[1] = "CO122.FIN_CUMF.DAT";
                fileNames[2] = "CO123.FIN_CUMF.DAT";
                fileNames[3] = "CO124.FIN_PUMF.DAT";
                fileNames[4] = "CU121.FIN_CUMF.DAT";
                fileNames[5] = "CU122.FIN_CUMF.DAT";
                fileNames[6] = "CU123.FIN_CUMF.DAT";
                fileNames[7] = "CU124.FIN_PUMF.DAT";

            }
            int recCount = 0;
            for (String txt : fileNames) {
                String fileName = workDirectory + rb.getString("its.data.dir") + "/" + txt;
                String recString;
                try {
                    BufferedReader in = new BufferedReader(new FileReader(fileName));
                    while ((recString = in.readLine()) != null) {
                        int origProvince = convertToInteger(recString.substring(16, 18));
                        //comment next line to deactivate ONTARIO filter (the closing } below too!)
                        //if (origProvince.equals("35")){
                        // origin == ontario
                        recCount++;
                        //int entryDay      = convertToInteger(recString.substring(48,50));
                        int entryMonth = convertToInteger(recString.substring(50, 52));
                        int refQuarter;
                        if (entryMonth < 4)
                            refQuarter = 1;
                        else if (entryMonth < 7)
                            refQuarter = 2;
                        else if (entryMonth < 10)
                            refQuarter = 3;
                        else
                            refQuarter = 4;

                        int refYear = convertToInteger(recString.substring(52, 56));
                        int purpose = convertToInteger(recString.substring(103, 105));
                        int entryMode = convertToInteger(recString.substring(217, 219));
                        int country[] = new int[16];
                        int nightsByPlace[] = new int[16];
                        int pumfId = recCount * 100 + refYear % 100;
                        nightsByPlace[0] = convertToInteger(recString.substring(576, 579));
                        country[0] = convertToInteger(recString.substring(581, 586));
                        //country[0] is the first country visited, equal to country [1]
                        country[1] = convertToInteger(recString.substring(581, 586));
                        country[2] = convertToInteger(recString.substring(621, 626));
                        country[3] = convertToInteger(recString.substring(661, 666));
                        country[4] = convertToInteger(recString.substring(701, 706));
                        country[5] = convertToInteger(recString.substring(741, 746));
                        country[6] = convertToInteger(recString.substring(781, 786));
                        country[7] = convertToInteger(recString.substring(828, 828));
                        country[8] = convertToInteger(recString.substring(861, 866));
                        country[9] = convertToInteger(recString.substring(901, 906));
                        country[10] = convertToInteger(recString.substring(941, 946));
                        country[11] = convertToInteger(recString.substring(981, 986));
                        country[12] = convertToInteger(recString.substring(1021, 1026));
                        country[13] = convertToInteger(recString.substring(1061, 1066));
                        country[14] = convertToInteger(recString.substring(1101, 1106));
                        country[15] = convertToInteger(recString.substring(1141, 1146));

                        nightsByPlace[1] = convertToInteger(recString.substring(605, 608));
                        nightsByPlace[2] = convertToInteger(recString.substring(645, 648));
                        nightsByPlace[3] = convertToInteger(recString.substring(685, 688));
                        nightsByPlace[4] = convertToInteger(recString.substring(725, 728));
                        nightsByPlace[5] = convertToInteger(recString.substring(765, 768));
                        nightsByPlace[6] = convertToInteger(recString.substring(805, 808));
                        nightsByPlace[7] = convertToInteger(recString.substring(845, 848));
                        nightsByPlace[8] = convertToInteger(recString.substring(885, 888));
                        nightsByPlace[9] = convertToInteger(recString.substring(925, 928));
                        nightsByPlace[10] = convertToInteger(recString.substring(965, 968));
                        nightsByPlace[11] = convertToInteger(recString.substring(1005, 1008));
                        nightsByPlace[12] = convertToInteger(recString.substring(1045, 1048));
                        nightsByPlace[13] = convertToInteger(recString.substring(1085, 1088));
                        nightsByPlace[14] = convertToInteger(recString.substring(1125, 1128));
                        nightsByPlace[15] = convertToInteger(recString.substring(1165, 1168));

                        int travelParty = convertToInteger(recString.substring(56, 58));
                        float weight = convertToFloat(recString.substring(1326, 1342));

                        //convert old codes to new codes in ITS survey
                        //when the country is not in the list is does not anything
                        for (int j = 0; j < 16; j++) {
                            try {
                                if(country[j] > 0)
                                    country[j] = (int) itsCountryCodes.getIndexedValueAt(country[j], 2);
                                } catch (Exception e) {
                                //TODO review file because it gives an exception (countries are not found?)
                                logger.error("Country codes for ITS old format not found: " + country[j]);
                            }
                        }

                        //convert old purposes to new purposes in ITS survey
                        try {
                            purpose = (int) itsPurposes.getIndexedValueAt(purpose, 3);
                        } catch (Exception e) {
                            logger.error("Purpose codes for ITS old format not found: " + e);
                        }

                        //convert entry modes to new purposes in ITS survey
                        try {
                            entryMode = (int) itsEntryModes.getIndexedValueAt(entryMode, 3);
                        } catch (Exception e) {
                            logger.error("Entry mode codes for ITS old format not found: " + e);

                        }

                        //clean multiple US states visits to 1 country visit in the nightsByPlace and Country matrices
                        //actually it does not clean the matrix nightsByPlace but sums
                        // the number of nights tyo the first country repeated and puts 999 nights in the rest

                        for (int i = 1; i < country.length; i++) {
                            for (int j = 1; j < country.length & j != i; j++) {
                                if (country[i] == country[j] & nightsByPlace[i] != 999 & nightsByPlace[j] != 999) {
                                    nightsByPlace[i] = nightsByPlace[i] + nightsByPlace[j];
                                    nightsByPlace[j] = 999;
                                }
                            }
                        }


                        //store as a new surveyIntTravel object
                        new surveyIntTravel(origProvince, pumfId, refYear, refQuarter, purpose, entryMode, country, nightsByPlace, weight, travelParty);

                        //comment next line to deactivate ONTARIO filter
                        //}
                    }
                } catch (Exception e) {
                    logger.error("Could not read ITS data: " + e);
                }


            }

            logger.info("  Read " + recCount + " ITS records with a residence in Ontario (35)");
        }
    }

    //

    public void readTSRCdata(int year) {
        // read TSRC data
        logger.info("  Reading TSRC data for " + year);

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


    private void readTSRCpersonData(String dirName, int year) {
        // read person file

        String personFileName = ResourceUtil.getProperty(rb, ("tsrc.persons"));
        String recString;
        int totRecCount = 0;
        for (int month = 1; month <= 12; month++) {
            int recCount = 0;
            try {
                String fullFileName;
                if (month <= 9)
                    fullFileName = dirName + File.separator + year + File.separator + personFileName + year +
                            "_Mth0" + month + "_pumf.txt";
                else fullFileName = dirName + File.separator + year + File.separator + personFileName + year +
                        "_Mth" + month + "_pumf.txt";
                BufferedReader in = new BufferedReader(new FileReader(fullFileName));
                while ((recString = in.readLine()) != null) {
                    recCount++;
                    int refYear = convertToInteger(recString.substring(0, 4));  // ascii position in file: 01-04
                    int refMonth = convertToInteger(recString.substring(4, 6));  // ascii position in file: 05-06
                    int origPumfId = convertToInteger(recString.substring(6, 13));  // ascii position in file: 07-13
                    int pumfId = origPumfId * 100 + refYear % 100;
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


    private void readTSRCtripData(String dirName, int year) {
        // read trip file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.trips"));
        String recString;
        int recCount = 0;
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_pumf.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                //the next line is eliminated so as not to count double times the import
                // /recCount++;
                int refYear = convertToInteger(recString.substring(0, 4));  // ascii position in file: 001-004
                int origPumfId = convertToInteger(recString.substring(6, 13));  // ascii position in file: 007-013
                int pumfId = origPumfId * 100 + refYear % 100;
                int tripId = convertToInteger(recString.substring(13, 15));  // ascii position in file: 014-015
                int origProvince = convertToInteger(recString.substring(16, 18));  // ascii position in file: 017-018
                int destProvince = convertToInteger(recString.substring(25, 27));  // ascii position in file: 026-027
                int travelParty = convertToInteger(recString.substring(63, 65));   // ascii position in file: 064-065
                int travelPartyAdult = convertToInteger(recString.substring(67, 69)); // ascii position in file: 068-069
                int mainMode = convertToInteger(recString.substring(79, 81));  // ascii position in file: 080-081
                int homeCma = convertToInteger(recString.substring(21, 25));  // ascii position in file: 022-025
                int tripPurp = convertToInteger(recString.substring(72, 74));  // ascii position in file: 073-074
                int numberNights = convertToInteger(recString.substring(120, 123));  // ascii position in file: 121-123
                int numIdentical = convertToInteger(recString.substring(173, 175));  // ascii position in file: 174-175
                float hhWeight = convertToFloat(recString.substring(34, 46));      // ascii position in file: 035-046
                float tripWeight = convertToFloat(recString.substring(46, 58));      // ascii position in file: 047-058

                //next line modified on 30 June by Carlos Llorca to extend the number of variables of the surveyTour object
                new surveyTour(tripId, pumfId, refYear, origProvince, destProvince, travelParty, travelPartyAdult, mainMode, homeCma, tripPurp, numberNights,
                        numIdentical, hhWeight, tripWeight);



                surveyPerson sp = surveyPerson.getPersonFromId(pumfId);
                if (numIdentical < 30) {

                    //Carlos Llorca on 01 July: I think 1 identical trip means 2 trips so I add the "+1" 3 lines above
                    // TODO check if this change is right
                    for (int i = 1; i <= numIdentical + 1; i++) sp.addTour(tripId);
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


    private void readTSRCvisitData(String dirName, int year) {
        // read visit location file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.visits"));
        String recString;
        int recCount = 0;
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_PUMF.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                //next line is out not to double the records
                //recCount++;
                int refYear = convertToInteger(recString.substring(0, 4));  // ascii position in file: 001-004
                int origPumfId = convertToInteger(recString.substring(6, 13));  // ascii position in file: 007-013
                int pumfId = origPumfId * 100 + refYear % 100;
                int tripId = convertToInteger(recString.substring(13, 15));  // ascii position in file: 014-015
                int cmarea = convertToInteger(recString.substring(22, 26));  // ascii position in file: 023-026
                int nights = convertToInteger(recString.substring(26, 29));  // ascii position in file: 027-029
                surveyTour st = surveyTour.getTourFromId(util.createTourId(pumfId, tripId));
                st.addTripDestinations(cmarea, nights);
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
                if (!s.substring(pos, pos + 1).equals(" ")) spacesOnly = false;
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
                if (!s.substring(pos, pos + 1).equals(" ")) spacesOnly = false;
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




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
        for (int year : ResourceUtil.getIntegerArray(rb, "its.years")) {
            readITSData(year);
            readITSVisitorsData(year);
        }

    }


    public void readITSData(int year) {

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
            logger.info("  Read " + recCount + " ITS trips of Canadian residents");

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

            logger.info("  Read " + recCount + " ITS trips of Canadian residents");
        }
    }


    public void readITSVisitorsData(int year) {

        //edited by Carlos Llorca on 29 June 2016

        //if sentences to select between "old" and "news" ITS data sets.

        if ( year == 2014) {


            logger.info("  Reading ITS Visitors data for " + year);
            String fileName = workDirectory + rb.getString("its.data.dir") + "/ITS_VISAE_" + year + "_PUMF.txt";
            String recString;
            int recCount = 0;

            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                while ((recString = in.readLine()) != null) {
                    //recCount++;
                    int origProvince = convertToInteger(recString.substring(8, 10));  // ascii position in file: 009-010
                    //comment next line to deactivate ONTARIO filter (the closing } below too!)
                    //if (origProvince.equals("35")){
                    // origin == ontario
                    recCount++;
                    int refYear = year;
                    int origPumfId = convertToInteger(recString.substring(0, 7));
                    int pumfId = origPumfId * 100 + refYear % 100;
                    int refQuarter = convertToInteger(recString.substring(7, 8));
                    int travelParty = convertToInteger(recString.substring(10, 12));
                    int tripPurpose = convertToInteger(recString.substring(12, 13));
                    int entryMode = convertToInteger(recString.substring(15, 16));
                    int entryRoute = convertToInteger(recString.substring(16, 17));
                    int country = 0;
                    if (entryRoute == 1) {
                        country = 11840;
                    }

                    int sgrCode = convertToInteger(recString.substring(288, 291));
                    int province[] = new int[15];
                    int nightsByPlace[] = new int[15];
                    province[1] = convertToInteger(recString.substring(33, 35));
                    nightsByPlace[1] = convertToInteger(recString.substring(43, 46));
                    province[2] = convertToInteger(recString.substring(52, 54));
                    nightsByPlace[2] = convertToInteger(recString.substring(62, 65));
                    province[3] = convertToInteger(recString.substring(71, 73));
                    nightsByPlace[3] = convertToInteger(recString.substring(81, 84));
                    province[4] = convertToInteger(recString.substring(90, 92));
                    nightsByPlace[4] = convertToInteger(recString.substring(100, 103));
                    province[5] = convertToInteger(recString.substring(109, 111));
                    nightsByPlace[5] = convertToInteger(recString.substring(119, 122));
                    province[6] = convertToInteger(recString.substring(128, 130));
                    nightsByPlace[6] = convertToInteger(recString.substring(138, 141));
                    province[7] = convertToInteger(recString.substring(147, 149));
                    nightsByPlace[7] = convertToInteger(recString.substring(157, 160));
                    province[8] = convertToInteger(recString.substring(166, 168));
                    nightsByPlace[8] = convertToInteger(recString.substring(176, 179));
                    province[9] = convertToInteger(recString.substring(185, 187));
                    nightsByPlace[9] = convertToInteger(recString.substring(195, 198));
                    province[10] = convertToInteger(recString.substring(204, 206));
                    nightsByPlace[10] = convertToInteger(recString.substring(214, 217));
                    nightsByPlace[0] = convertToInteger(recString.substring(343, 346));
                    float weight = convertToFloat(recString.substring(591, 607));
                    province[0] = convertToInteger(recString.substring(33, 35));
/*
                    //clean multiple US visits to get only 1 trip to US. The
                    for (int i = 1; i < country.length; i++) {
                        for (int j = 1; j < country.length & j != i; j++) {
                            if (country[i] == country[j] & nightsByPlace[i] != 999 & nightsByPlace[j] != 999) {
                                nightsByPlace[i] = nightsByPlace[i] + nightsByPlace[j];
                                nightsByPlace[j] = 999;
                            }
                        }
                    }
*/
                    new surveyIntVisitorTravel(pumfId, refQuarter, refYear, tripPurpose, "FALSE",
                            entryMode, country, province, nightsByPlace, weight, travelParty, entryRoute, sgrCode);

                }
            } catch (Exception e) {
                logger.error("Could not read ITS Visitors data: " + e);
            }
            logger.info("  Read " + recCount + " ITS Visitors");

        } else if (year ==2013) {

            logger.info("  Reading ITS Visitors data for " + year);
            String fileName = workDirectory + rb.getString("its.data.dir") + "/ITS_VISAE_" + year + "_PUMF.txt";
            String recString;
            int recCount = 0;

            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                while ((recString = in.readLine()) != null) {
                    //recCount++;
                    int origProvince = convertToInteger(recString.substring(8, 10));  // ascii position in file: 009-010
                    //comment next line to deactivate ONTARIO filter (the closing } below too!)
                    //if (origProvince.equals("35")){
                    // origin == ontario
                    recCount++;
                    int refYear = year;
                    int origPumfId = convertToInteger(recString.substring(0, 7));
                    int pumfId = origPumfId * 100 + refYear % 100;
                    int refQuarter = convertToInteger(recString.substring(7, 8));
                    int travelParty = convertToInteger(recString.substring(10, 12));
                    int tripPurpose = convertToInteger(recString.substring(12, 13));
                    int entryMode= convertToInteger(recString.substring(13,14));
                    int entryRoute= convertToInteger(recString.substring(14,15));

                    int country = 0;
                    if (entryRoute == 1) {
                        country = 11840;
                    }

                    int sgrCode= convertToInteger(recString.substring(286,289));

                    int province[] = new int[15];
                    int nightsByPlace[] = new int[15];
                    province[1]= convertToInteger(recString.substring(31,33));
                    nightsByPlace[1]= convertToInteger(recString.substring(41,44));
                    province[2]= convertToInteger(recString.substring(50,52));
                    nightsByPlace[2]= convertToInteger(recString.substring(60,63));
                    province[3]= convertToInteger(recString.substring(68,69));
                    nightsByPlace[3]= convertToInteger(recString.substring(75,79));
                    province[4]= convertToInteger(recString.substring(87,88));
                    nightsByPlace[4]= convertToInteger(recString.substring(94,98));
                    province[5]= convertToInteger(recString.substring(106,107));
                    nightsByPlace[5]= convertToInteger(recString.substring(113,117));
                    province[6]= convertToInteger(recString.substring(126,128));
                    nightsByPlace[6]= convertToInteger(recString.substring(136,139));
                    province[7]= convertToInteger(recString.substring(145,147));
                    nightsByPlace[7]= convertToInteger(recString.substring(155,158));
                    province[8]= convertToInteger(recString.substring(164,166));
                    nightsByPlace[8]= convertToInteger(recString.substring(174,177));
                    province[9]= convertToInteger(recString.substring(183,185));
                    nightsByPlace[9]= convertToInteger(recString.substring(193,196));
                    province[10]= convertToInteger(recString.substring(202,204));
                    nightsByPlace[10]= convertToInteger(recString.substring(212,215));

                    nightsByPlace[0]= convertToInteger(recString.substring(341,344));

                    float weight= convertToFloat(recString.substring(517,534));

                    province[0]= convertToInteger(recString.substring(31,33));

/*
                    //clean multiple US visits to get only 1 trip to US. The
                    for (int i = 1; i < country.length; i++) {
                        for (int j = 1; j < country.length & j != i; j++) {
                            if (country[i] == country[j] & nightsByPlace[i] != 999 & nightsByPlace[j] != 999) {
                                nightsByPlace[i] = nightsByPlace[i] + nightsByPlace[j];
                                nightsByPlace[j] = 999;
                            }
                        }
                    }
*/
                    new surveyIntVisitorTravel(pumfId, refQuarter, refYear, tripPurpose, "FALSE",
                            entryMode, country, province, nightsByPlace, weight, travelParty, entryRoute, sgrCode);

                }
            } catch (Exception e) {
                logger.error("Could not read ITS Visitors data: " + e);
            }
            logger.info("  Read " + recCount + " ITS Visitors");


        } else if (year == 2011 || year == 2012) {
            // read ITS data from years 2011 and 2012
            logger.info("  Reading ITS data for " + year);
            //provide the names of the files
            // TODO Do it more systematic
            String fileNames[] = new String[8];
            if (year == 2011) {
                //overseas visitors
                fileNames[0] ="OV111.FIN_CUMF.DAT";
                fileNames[1] ="OV112.FIN_CUMF.DAT";
                fileNames[2] ="OV113.FIN_CUMF.DAT";
                fileNames[3] ="OV114.FIN_CUMF.DAT";
                //US visitors
                fileNames[4] ="US111.FIN_CUMF.DAT";
                fileNames[5] ="US112.FIN_CUMF.DAT";
                fileNames[6] ="US113.FIN_CUMF.DAT";
                fileNames[7] ="US114.FIN_CUMF.DAT";


            } else {
                //overseas visitors
                fileNames[0] ="OV121.FIN_CUMF.DAT";
                fileNames[1] ="OV122.FIN_CUMF.DAT";
                fileNames[2] ="OV123.FIN_CUMF.DAT";
                fileNames[3] ="OV124.FIN_PUMF.DAT";
                //visitors
                fileNames[4] ="US121.FIN_CUMF.DAT";
                fileNames[5] ="US122.FIN_CUMF.DAT";
                fileNames[6] ="US123.FIN_CUMF.DAT";
                fileNames[7] ="US124.FIN_PUMF.DAT";


            }
            int recCount = 0;
            //first: loop for overseas data files
            for (int i=0; i<4; i++) {
                String txt = fileNames[i];
                String fileName = workDirectory + rb.getString("its.data.dir") + "/" + txt;
                String recString;
                try {
                    BufferedReader in = new BufferedReader(new FileReader(fileName));
                    while ((recString = in.readLine()) != null) {
                        recCount++;
                        String entryPort    = recString.substring(50,54);
                        int travelParty     = convertToInteger(recString.substring(74,76));
                        int tripPurpose     = convertToInteger(recString.substring(121,123));
                        int origCountry     = convertToInteger(recString.substring(16,21));


                        int entryMonth      = convertToInteger(recString.substring(56,58));
                        int refYear         = convertToInteger(recString.substring(58,62));

                        int refQuarter;
                        if (entryMonth < 4)
                            refQuarter = 1;
                        else if (entryMonth < 7)
                            refQuarter = 2;
                        else if (entryMonth < 10)
                            refQuarter = 3;
                        else
                            refQuarter = 4;



                        int province[] = new int[16];
                        int nightsByPlace[] = new int[16];
                        int pumfId = recCount * 100 + refYear % 100;
                        int entryRoute= convertToInteger(recString.substring(517,518));


                        province[0]         = convertToInteger(recString.substring(231,233));
                        province[1]         = convertToInteger(recString.substring(231,233));
                        nightsByPlace[1]    = convertToInteger(recString.substring(241,244));
                        province[2]         = convertToInteger(recString.substring(250,252));
                        nightsByPlace[2]    = convertToInteger(recString.substring(260,263));
                        province[3]         = convertToInteger(recString.substring(269,271));
                        nightsByPlace[3]    = convertToInteger(recString.substring(279,282));
                        province[4]         = convertToInteger(recString.substring(288,290));
                        nightsByPlace[4]    = convertToInteger(recString.substring(298,301));
                        province[5]         = convertToInteger(recString.substring(307,309));
                        nightsByPlace[5]    = convertToInteger(recString.substring(317,320));
                        province[6]         = convertToInteger(recString.substring(326,328));
                        nightsByPlace[6]    = convertToInteger(recString.substring(336,339));
                        province[7]         = convertToInteger(recString.substring(345,347));
                        nightsByPlace[7]    = convertToInteger(recString.substring(355,358));
                        province[8]         = convertToInteger(recString.substring(364,366));
                        nightsByPlace[8]    = convertToInteger(recString.substring(374,377));
                        province[9]         = convertToInteger(recString.substring(383,385));
                        nightsByPlace[9]    = convertToInteger(recString.substring(393,396));
                        province[10]        = convertToInteger(recString.substring(402,404));
                        nightsByPlace[10]   = convertToInteger(recString.substring(412,415));
                        province[11]        = convertToInteger(recString.substring(421,423));
                        nightsByPlace[11]   = convertToInteger(recString.substring(431,434));
                        province[12]        = convertToInteger(recString.substring(440,442));
                        nightsByPlace[12]   = convertToInteger(recString.substring(450,453));
                        province[13]        = convertToInteger(recString.substring(459,461));
                        nightsByPlace[13]   = convertToInteger(recString.substring(469,472));
                        province[14]        = convertToInteger(recString.substring(478,480));
                        nightsByPlace[14]   = convertToInteger(recString.substring(488,491));
                        province[15]        = convertToInteger(recString.substring(497,499));
                        nightsByPlace[15]   = convertToInteger(recString.substring(507,510));
                        nightsByPlace[0]    = convertToInteger(recString.substring(833,836));

                        float weight        = convertToFloat(recString.substring(934,950));




                        //convert old codes to new codes in ITS survey
                        //when the country is not in the list is does not anything
//                        for (int j = 0; j < 16; j++) {
                            try {
                                if(origCountry > 0)
                                    origCountry = (int) itsCountryCodes.getIndexedValueAt(origCountry, 2);
                            } catch (Exception e) {

                                logger.error("Country codes for ITS old format not found: " + origCountry);
                            }
//                        }


                        //convert old purposes to new purposes in ITS survey
                        try {
                            tripPurpose = (int) itsPurposes.getIndexedValueAt(tripPurpose, 3);
                        } catch (Exception e) {
                            logger.error("Purpose codes for ITS old format not found: " + e);
                        }
/*
                        //convert entry modes to new modes in ITS survey
                        try {
                            entryMode = (int) itsEntryModes.getIndexedValueAt(entryMode, 3);
                        } catch (Exception e) {
                            logger.error("Entry mode codes for ITS old format not found: " + e);

                        }
*/
                        //clean multiple US states visits to 1 country visit in the nightsByPlace and Country matrices
                        //actually it does not clean the matrix nightsByPlace but sums
                        // the number of nights tyo the first country repeated and puts 999 nights in the rest
/*
                        for (int i = 1; i < country.length; i++) {
                            for (int j = 1; j < country.length & j != i; j++) {
                                if (country[i] == country[j] & nightsByPlace[i] != 999 & nightsByPlace[j] != 999) {
                                    nightsByPlace[i] = nightsByPlace[i] + nightsByPlace[j];
                                    nightsByPlace[j] = 999;
                                }
                            }
                        }
*/

                        //store as a new surveyIntTravel object
                        new surveyIntVisitorTravel(pumfId, refQuarter, refYear, tripPurpose, entryPort,
                                0, origCountry, province, nightsByPlace, weight, travelParty, entryRoute,0);

                        //comment next line to deactivate ONTARIO filter
                        //}
                    }
                } catch (Exception e) {
                    logger.error("Could not read Visitors ITS data: " + e);
                }


            }

            //second: loop for US data files
            for (int i=4; i<8; i++) {
                String txt = fileNames[i];
                String fileName = workDirectory + rb.getString("its.data.dir") + "/" + txt;
                String recString;
                try {
                    BufferedReader in = new BufferedReader(new FileReader(fileName));
                    while ((recString = in.readLine()) != null) {
                        recCount++;
                        String entryPort= recString.substring(50,54);
                        int travelParty     = convertToInteger(recString.substring(74,76));
                        int tripPurpose     = convertToInteger(recString.substring(121,123));
                        int origCountry= 11840;
                        int entryRoute= convertToInteger(recString.substring(517,518));


                        int entryMonth      = convertToInteger(recString.substring(56,58));
                        int refYear         = convertToInteger(recString.substring(58,62));

                        int refQuarter;
                        if (entryMonth < 4)
                            refQuarter = 1;
                        else if (entryMonth < 7)
                            refQuarter = 2;
                        else if (entryMonth < 10)
                            refQuarter = 3;
                        else
                            refQuarter = 4;



                        int province[] = new int[16];
                        int nightsByPlace[] = new int[16];
                        int pumfId = recCount * 100 + refYear % 100;

                        province[0]         = convertToInteger(recString.substring(231,233));
                        province[1]         = convertToInteger(recString.substring(231,233));
                        nightsByPlace[1]    = convertToInteger(recString.substring(241,244));
                        province[2]         = convertToInteger(recString.substring(250,252));
                        nightsByPlace[2]    = convertToInteger(recString.substring(260,263));
                        province[3]         = convertToInteger(recString.substring(269,271));
                        nightsByPlace[3]    = convertToInteger(recString.substring(279,282));
                        province[4]         = convertToInteger(recString.substring(288,290));
                        nightsByPlace[4]    = convertToInteger(recString.substring(298,301));
                        province[5]         = convertToInteger(recString.substring(307,309));
                        nightsByPlace[5]    = convertToInteger(recString.substring(317,320));
                        province[6]         = convertToInteger(recString.substring(326,328));
                        nightsByPlace[6]    = convertToInteger(recString.substring(336,339));
                        province[7]         = convertToInteger(recString.substring(345,347));
                        nightsByPlace[7]    = convertToInteger(recString.substring(355,358));
                        province[8]         = convertToInteger(recString.substring(364,366));
                        nightsByPlace[8]    = convertToInteger(recString.substring(374,377));
                        province[9]         = convertToInteger(recString.substring(383,385));
                        nightsByPlace[9]    = convertToInteger(recString.substring(393,396));
                        province[10]        = convertToInteger(recString.substring(402,404));
                        nightsByPlace[10]   = convertToInteger(recString.substring(412,415));
                        province[11]        = convertToInteger(recString.substring(421,423));
                        nightsByPlace[11]   = convertToInteger(recString.substring(431,434));
                        province[12]        = convertToInteger(recString.substring(440,442));
                        nightsByPlace[12]   = convertToInteger(recString.substring(450,453));
                        province[13]        = convertToInteger(recString.substring(459,461));
                        nightsByPlace[13]   = convertToInteger(recString.substring(469,472));
                        province[14]        = convertToInteger(recString.substring(478,480));
                        nightsByPlace[14]   = convertToInteger(recString.substring(488,491));
                        province[15]        = convertToInteger(recString.substring(497,499));
                        nightsByPlace[15]   = convertToInteger(recString.substring(507,510));
                        nightsByPlace[0]    = convertToInteger(recString.substring(926,929));
                        float weight        = convertToFloat(recString.substring(962,978));





                        //convert old codes to new codes in ITS survey
                        //when the country is not in the list is does not anything
//                        for (int j = 0; j < 16; j++) {
//                            try {
//                                if(origCountry > 0)
//                                    origCountry = (int) itsCountryCodes.getIndexedValueAt(origCountry, 2);
//                            } catch (Exception e) {
//                                logger.error("Country codes for ITS old format not found: " + origCountry);
//                            }
//                        }


                        //convert old purposes to new purposes in ITS survey
                        try {
                            tripPurpose = (int) itsPurposes.getIndexedValueAt(tripPurpose, 3);
                        } catch (Exception e) {
                            logger.error("Purpose codes for ITS old format not found: " + e);
                        }

                        //convert entry modes to new modes in ITS survey
//                        try {
//                            entryMode = (int) itsEntryModes.getIndexedValueAt(entryMode, 3);
//                        } catch (Exception e) {
//                            logger.error("Entry mode codes for ITS old format not found: " + e);
//
//                        }



                        //store as a new surveyIntVisitorTravel object
                        new surveyIntVisitorTravel(pumfId, refQuarter, refYear, tripPurpose, entryPort,
                                0, origCountry, province, nightsByPlace, weight, travelParty, entryRoute , 0);

                        //comment next line to deactivate ONTARIO filter
                        //}
                    }
                } catch (Exception e) {
                    logger.error("Could not read Visitors ITS data: " + e);
                }


            }

            logger.info("  Read " + recCount + " ITS visitor records ");
        }
    }


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
                    int cd = convertToInteger(recString.substring(39, 42));  //ascii position in file: 40-42
                    int cma = convertToInteger(recString.substring(42, 46));  // ascii position in file: 43-46
                    int ageGroup = convertToInteger(recString.substring(46, 47));  // ascii position in file: 47-47
                    int gender = convertToInteger(recString.substring(47, 48));  // ascii position in file: 48-48
                    int education = convertToInteger(recString.substring(48, 49));  // ascii position in file: 49-49
                    int laborStat = convertToInteger(recString.substring(49, 50));  // ascii position in file: 50-50
                    int hhIncome = convertToInteger(recString.substring(50, 51));  // ascii position in file: 51-51
                    int adultsInHh = convertToInteger(recString.substring(51, 53));  // ascii position in file: 52-53
                    int kidsInHh = convertToInteger(recString.substring(53, 55));  // ascii position in file: 54-55
                    new surveyPerson(refYear, refMonth, pumfId, weight, weight2, prov, cd, cma, ageGroup, gender, education,
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
                int travelPartyKids = convertToInteger(recString.substring(69, 71)); // ascii position in file: 068-069
                int mainMode = convertToInteger(recString.substring(79, 81));  // ascii position in file: 080-081
                int homeCma = convertToInteger(recString.substring(21, 25));  // ascii position in file: 022-025
                int tripPurp = convertToInteger(recString.substring(72, 74));  // ascii position in file: 073-074
                int numberNights = convertToInteger(recString.substring(120, 123));  // ascii position in file: 121-123
                int numIdentical = convertToInteger(recString.substring(173, 175));  // ascii position in file: 174-175
                float hhWeight = convertToFloat(recString.substring(34, 46));      // ascii position in file: 035-046
                float tripWeight = convertToFloat(recString.substring(46, 58));      // ascii position in file: 047-058

                //next line modified on 30 June by Carlos Llorca to extend the number of variables of the surveyTour object
                new surveyTour(tripId, pumfId, refYear, origProvince, destProvince, travelParty, travelPartyAdult, travelPartyKids, mainMode, homeCma, tripPurp, numberNights,
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




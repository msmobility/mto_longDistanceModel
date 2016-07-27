package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.DataDictionary;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.Survey;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Created by Joe on 26/07/2016.
 */
public class SurveyDataImporter {
    private Logger logger = Logger.getLogger(this.getClass());
    private ResourceBundle rb;
    private String workDirectory;

    private TableDataSet provinceList;
    private TableDataSet mainModeList;
    private TableDataSet cmaList;
    private TableDataSet tripPurposes;

    private DataDictionary dataDictionary;
    HashMap<Integer, surveyPerson> personMap;

    private SurveyDataImporter() {

    }

    public static mtoSurveyData importData(ResourceBundle rb) {
        SurveyDataImporter sdi = new SurveyDataImporter();
        sdi.rb = rb;
        sdi.readInput();
        mtoSurveyData mtoSurveyData2 = sdi.buildDataModel();

        return mtoSurveyData2;
    }

    private mtoSurveyData buildDataModel() {
        return  new mtoSurveyData(rb, personMap, dataDictionary, provinceList, mainModeList, cmaList, tripPurposes);
    }

    private void readInput() {
        // read input data
        workDirectory = rb.getString("work.directory");

        dataDictionary = new DataDictionary(rb.getString("data.dictionary"));
        personMap = new HashMap<>();

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


    private void readITSdata(int year) {
        // read ITS data
        logger.info ("  Reading ITS data for " + year);
        String fileName = workDirectory + rb.getString("its.data.dir") + "/" + ResourceUtil.getProperty(rb, "its.data");
        String recString;
        int recCount = 0;
        PrintWriter out = util.openFileForSequentialWriting(rb.getString("its.out.file") + ".csv", false);
        out.println("province,cma,weight");
        Survey survey = dataDictionary.getSurvey("ITS", "Canadians");
//        float[][] purp = new float[5][365];
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            while ((recString = in.readLine()) != null) {
                recCount++;
                String origProvince = survey.read(recString, "RSPROV");  // ascii position in file: 009-010
                String origCMA =      survey.read(recString, "RSCMA");  // ascii position in file: 011-014
                if (origProvince.equals("35")){
                    // origin == ontario
                    recCount++;
                    int purpose =      survey.readInt(recString, "RSNP");  // ascii position in file: 019-019
                    int entryMode =    survey.readInt(recString, "MODENTP");  // ascii position in file: 026-026
                    int country =      survey.readInt(recString, "PLVS01C");  // ascii position in file: 039-043
                    int nightsByPlace[] = new int[11];
                    nightsByPlace[1] = survey.readInt(recString, "NTSVS01");  // ascii position in file: 049-051
                    nightsByPlace[2] = survey.readInt(recString, "NTSVS02");  // ascii position in file: 076-078
                    nightsByPlace[3] = survey.readInt(recString, "NTSVS03");  // ascii position in file: 103-105
                    nightsByPlace[4] = survey.readInt(recString, "NTSVS04");  // ascii position in file: 130-132
                    nightsByPlace[5] = survey.readInt(recString, "NTSVS05");  // ascii position in file: 157-159
                    nightsByPlace[6] = survey.readInt(recString, "NTSVS06");  // ascii position in file: 184-186
                    nightsByPlace[7] = survey.readInt(recString, "NTSVS07");  // ascii position in file: 211-213
                    nightsByPlace[8] = survey.readInt(recString, "NTSVS08");  // ascii position in file: 238-240
                    nightsByPlace[9] = survey.readInt(recString, "NTSVS09");  // ascii position in file: 265-267
                    nightsByPlace[10]= survey.readInt(recString, "NTSVS10");  // ascii position in file: 292-294
                    nightsByPlace[0] = survey.readInt(recString, "TOTNIGHT");  // ascii position in file: 346-348
//                    purp[purpose][nightsByPlace[0]] += weight;
                }
                float weight =  survey.readFloat(recString, "WEIGHTP");    // ascii position in file: 476-492
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


    private void readTSRCdata(int year) {
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
        Survey survey = dataDictionary.getSurvey("TSRC", "Person");


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
                    int refYear = survey.readInt(recString, "REFYEARP");  // ascii position in file: 01-04
                    int refMonth = survey.readInt(recString, "REFMTHP");  // ascii position in file: 05-06
                    int origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 07-13
                    int pumfId = origPumfId * 100 + refYear%100;
                    float weight = survey.readFloat(recString, "WTPM");  // ascii position in file: 14-25
                    float weight2 = survey.readFloat(recString, "WTPM2");  // ascii position in file: 26-37
                    int prov = survey.readInt(recString, "RESPROV");  // ascii position in file: 38-39
                    int cma = survey.readInt(recString, "RESCMA2");  // ascii position in file: 43-46
                    int ageGroup = survey.readInt(recString, "AGE_GR2");  // ascii position in file: 47-47
                    int gender = survey.readInt(recString, "SEX");  // ascii position in file: 48-48
                    int education = survey.readInt(recString, "EDLEVGR");  // ascii position in file: 49-49
                    int laborStat = survey.readInt(recString, "LFSSTATG");  // ascii position in file: 50-50
                    int hhIncome = survey.readInt(recString, "INCOMGR2");  // ascii position in file: 51-51
                    int adultsInHh = survey.readInt(recString, "G_ADULTS");  // ascii position in file: 52-53
                    int kidsInHh = survey.readInt(recString, "G_KIDS");  // ascii position in file: 54-55

                    surveyPerson person = buildPerson(survey, refYear, refMonth, pumfId, weight, weight2, prov, cma, ageGroup, gender, education,
                            laborStat, hhIncome, adultsInHh, kidsInHh);

                    personMap.put(pumfId, person);
                }
            } catch (Exception e) {
                logger.error("Could not read TSRC person data: ",e);
            }
            // logger.info("  Read " + recCount + " person records for the month " + month);
            totRecCount += recCount;
        }
        logger.info("  Read " + totRecCount + " person records");
    }

    private surveyPerson buildPerson(Survey survey, int refYear, int refMonth, int pumfId, float weight, float weight2,
                                     int prov, int cma, int ageGroup, int gender, int education,
                                     int laborStat, int hhIncome, int adultsInHh, int kidsInHh) {

        Gender gender2 = Gender.getGender(gender);
        LaborStatus laborStat2 = LaborStatus.getStatus(laborStat);

        String ageGroup2 = survey.decodeValue("AGE_GR2", ageGroup);
        String education2 = survey.decodeValue("EDLEVGR", education);

        surveyPerson person = new surveyPerson(refYear, refMonth, pumfId, weight, weight2, prov, cma, ageGroup2, gender2, education2,
                laborStat2, hhIncome, adultsInHh, kidsInHh);

        return person;
    }


    private void readTSRCtripData (String dirName, int year) {
        // read trip file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.trips"));
        String recString;
        int recCount = 0;
        Survey survey = dataDictionary.getSurvey("TSRC", "Trip");
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_pumf.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                recCount++;
                int refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
                int origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
                int pumfId = origPumfId * 100 + refYear%100;
                int tripId =       survey.readInt(recString, "TRIPID");  // ascii position in file: 014-015
                int origProvince = survey.readInt(recString, "ORCPROVT");  // ascii position in file: 017-018
                int destProvince = survey.readInt(recString, "MDDPLFL");  // ascii position in file: 026-027
                int mainMode =     survey.readInt(recString, "TMDTYPE2");  // ascii position in file: 080-081
                int homeCma =      survey.readInt(recString, "ORCCMAT2");  // ascii position in file: 022-025
                int tripPurp =     survey.readInt(recString, "MRDTRIP3");  // ascii position in file: 073-074
                int numberNights = survey.readInt(recString, "CANNITE");  // ascii position in file: 121-123
                int numIdentical = survey.readInt(recString, "TR_D11");  // ascii position in file: 174-175
                surveyTour tour = new surveyTour(tripId, pumfId, origProvince, destProvince, mainMode, homeCma, tripPurp, numberNights,
                        numIdentical);
                surveyPerson sp = personMap.get(pumfId);
                if (numIdentical < 30) {
                    for (int i = 1; i <= numIdentical; i++) sp.addTour(tour);
                } else {
                    sp.addTour(tour); //TODO: why?
                }
                recCount++;
            }
        } catch (Exception e) {
            logger.error("Could not read TSRC trip data: ",e);
        }
        logger.info("  Read " + recCount + " tour records.");
    }


    private void readTSRCvisitData (String dirName, int year) {
        // read visit location file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.visits"));
        String recString;
        int recCount = 0;
        Survey survey = dataDictionary.getSurvey("TSRC", "Visit");
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_PUMF.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                recCount++;
                int refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
                int origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
                int pumfId = origPumfId * 100 + refYear%100;
                int tripId = survey.readInt(recString, "TRIPID");  // ascii position in file: 014-015
                int cmarea = survey.readInt(recString, "VCMA2");  // ascii position in file: 023-026
                int nights = survey.readInt(recString, "AC_Q04");  // ascii position in file: 027-029
                surveyPerson person = personMap.get(pumfId);
                surveyTour st = person.getTourFromId(tripId);
                st.addTripDestinations (cmarea, nights);
                recCount++;
            }
        } catch (Exception e) {
            logger.error("Could not read TSRC visit data: ", e);
        }
        logger.info("  Read " + recCount + " visit records.");
    }



}

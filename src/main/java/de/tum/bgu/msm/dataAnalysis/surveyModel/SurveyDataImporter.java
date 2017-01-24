package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.DataDictionary;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.Survey;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.tripGeneration.ExtCanToIntTripGeneration;
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


    private TableDataSet itsTripPurposesConversion;
    private TableDataSet itsCountryConversion;
    private TableDataSet itsModeConversion;

    private DataDictionary dataDictionary;
    HashMap<Long, SurveyPerson> personMap;

    private SurveyDataImporter() {

    }

    public static MtoSurveyData importData(ResourceBundle rb) {
        SurveyDataImporter sdi = new SurveyDataImporter();
        sdi.rb = rb;
        sdi.readInput();
        MtoSurveyData mtoSurveyData2 = sdi.buildDataModel();

        //generate all geometries
        mtoSurveyData2.getPersons().stream().forEach(p ->
                p.getTours().stream().forEach(t -> t.generateTourLineString(mtoSurveyData2))
        );

        return mtoSurveyData2;
    }

    private MtoSurveyData buildDataModel() {
        return new MtoSurveyData(rb, personMap, dataDictionary);
    }

    private void readInput() {
        // read input data
        workDirectory = rb.getString("work.directory");
        dataDictionary = new DataDictionary(rb);
        personMap = new HashMap<>();

        // read all TSRC data
        for (int year : ResourceUtil.getIntegerArray(rb, "tsrc.years")) readTSRCdata(year);
        // read all ITS data

        readItsConversionTables();
        for (int year : ResourceUtil.getIntegerArray(rb, "its.years")) readITSCanData(year);

    }


    private void readITSCanData(int year) {

        // read ITS data
        logger.info("  Reading ITS data for " + year);
        String fileName = workDirectory + rb.getString("its.data.dir") + "/" + year + "/" + ResourceUtil.getProperty(rb, "its.data") + year + "_PUMF.txt";
        String recString;
        int recCount = 0;
        String its_out_location = rb.getString("output.folder") + "/" + rb.getString("its.out.file");
        PrintWriter out = Util.openFileForSequentialWriting(its_out_location + year + ".csv", false);
        //System.out.println(fileName);
        out.println("recCount,year,date,origProv,origCD,origCMA,partySize,purpose,entryMode,nights,country,state,region,stopSeq,weight");
        Survey survey = dataDictionary.getSurvey("ITS", year, "Canadians");
//        float[][] purp = new float[5][365];
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            while ((recString = in.readLine()) != null) {
                recCount++;

                int nightsByPlace[] = new int[16];
                int destCountry[] = new int[16];
                int destUsState[] = new int[16];
                int destUsRegion[] = new int[16];

                if (year < 2013) {

                    int origProvince = survey.readInt(recString, "RSPROV");  // ascii position in file: 009-010
                    int origCD = survey.readInt(recString, "RSPRCD");
                    int origCMA = survey.readInt(recString, "RSCMA");  // ascii position in file: 011-014
                    int date = survey.readInt(recString, "DATEX");
                    int travelPartySize = survey.readInt(recString, "TPSZE");  // ascii position in file: 019-019
                    int purpose = convertPurpose(survey.readInt(recString, "RSN"));  // ascii position in file: 019-019
                    int entryMode = convertMode(survey.readInt(recString, "TRNEN"));  // ascii position in file: 026-026
                    nightsByPlace[0] = survey.readInt(recString, "TOTNIGHT");  // ascii position in file: 346-348
                    String place = survey.read(recString, "PLVSC1");
                    destCountry[0] = convertCountry(Integer.parseInt(place.substring(0, 5)));
                    if (destCountry[0] == 11840) destUsState[0] = Integer.parseInt(place.substring(5, 7));
                    else destUsState[0] = 99;
                    if (destCountry[0] == 11840) destUsRegion[0] = survey.readInt(recString, "USREGC1");
                    else destUsRegion[0] = 99;
                    float weight = survey.readFloat(recString, "PERSFAC");    // ascii position in file: 476-492

                    //prints total number of nights and first place
                    out.println(recCount + "," + year + "," + date + "," + origProvince + "," + origCD + "," + origCMA + "," + travelPartySize + ","
                            + purpose + "," + entryMode + "," + nightsByPlace[0] + "," + destCountry[0] + "," + destUsState[0] + "," + destUsRegion[0] + ",0," + weight);

                    for (int i = 1; i < 16; i++) {
                        nightsByPlace[i] = survey.readInt(recString, "NTSVS" + i);
                        place = survey.read(recString, "PLVSC" + i);
                        if (!place.substring(0, 5).trim().isEmpty()) {
                            destCountry[i] = convertCountry(Integer.parseInt(place.substring(0, 5)));
                            if (destCountry[i] == 11840) Integer.parseInt(place.substring(5, 7));
                            else destUsState[i] = 99;
                            if (destCountry[i] == 11840) destUsRegion[i] = survey.readInt(recString, "USREGC" + i);
                            else destUsRegion[i] = 99;

                            //prints all the places visited
                            out.println(recCount + "," + year + "," + date + "," + origProvince + "," + origCD + "," + origCMA + "," + travelPartySize + ","
                                    + purpose + "," + entryMode + "," + nightsByPlace[i] + "," + destCountry[i] + "," + destUsState[i] + "," + destUsRegion[i] +
                                    "," + i + "," + weight);
                        }
                    }

                } else {

                    int origProvince = survey.readInt(recString, "RSPROV");  // ascii position in file: 009-010
                    //int origCD = survey.readInt(recString, "RSPRCD");
                    int origCMA = survey.readInt(recString, "RSCMA");  // ascii position in file: 011-014
                    //date is here quarter
                    int date = survey.readInt(recString, "QUARTER");
                    int travelPartySize = survey.readInt(recString, "TPSZEP");  // ascii position in file: 019-019
                    int purpose = survey.readInt(recString, "RSNP");  // ascii position in file: 019-019
                    int entryMode = survey.readInt(recString, "MODENTP");  // ascii position in file: 026-026
                    nightsByPlace[0] = survey.readInt(recString, "TOTNIGHT");  // ascii position in file: 346-348
                    destCountry[0] = Integer.parseInt(survey.read(recString, "PLVS01C"));
                    destUsState[0] = survey.readInt(recString, "USSTAT01");
                    destUsRegion[0] = survey.readInt(recString, "USREGC01");
                    float weight = survey.readFloat(recString, "WEIGHTP");    // ascii position in file: 476-492

                    //prints total number of nights and first place
                    out.println(recCount + "," + year + "," + date + "," + origProvince + "," + "-999" + "," + origCMA + "," + travelPartySize + ","
                            + purpose + "," + entryMode + "," + nightsByPlace[0] + "," + destCountry[0] + "," + destUsState[0] + "," + destUsRegion[0]
                            + ",0," + weight);

                    for (int i = 1; i < 11; i++) {
                        String index = Integer.toString(i);
                        if (index.length() == 1) index = "0" + index;
                        nightsByPlace[i] = survey.readInt(recString, "NTSVS" + index);
                        destCountry[i] = Integer.parseInt(survey.read(recString, "PLVS" + index + "C"));
                        destUsState[i] = survey.readInt(recString, "USSTAT" + index);
                        destUsRegion[i] = survey.readInt(recString, "USREGC" + index);

                        if (destCountry[i] != 99996) {
                            //prints all the places visited
                            out.println(recCount + "," + year + "," + date + "," + origProvince + "," + "-999" + "," + origCMA + "," + travelPartySize + ","
                                    + purpose + "," + entryMode + "," + nightsByPlace[i] + "," + destCountry[i] + "," + destUsState[i] +
                                    "," + destUsRegion[i] + "," + i + "," + weight);
                        }
                    }

                }
            }
            out.close();
        } catch (Exception e) {
            logger.error("Could not read ITS data: " + e);
        }
        logger.info("  Read " + recCount + " ITS records in " + year);

//        for (int days=0;days<365;days++) logger.info("Days " + days + ": " + (purp[1][days]+purp[3][days]) + "," +
//                purp[2][days] + "," + purp[4][days]);
    }


    private void readTSRCdata(int year) {
        // read TSRC data
        logger.info("  Reading TSRC data for " + year);

        String dirName = workDirectory + ResourceUtil.getProperty(rb, ("tsrc.data.dir"));

        readTSRCpersonData(dirName, year);
        readTSRCtripData(dirName, year);
        readTSRCvisitData(dirName, year);

    }

    private void readTSRCpersonData(String dirName, int year) {
        // read person file

        String personFileName = ResourceUtil.getProperty(rb, ("tsrc.persons"));
        String recString;
        int totRecCount = 0;
        Survey survey = dataDictionary.getSurvey("TSRC", year, "Person");


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

                    SurveyPerson person = new SurveyPerson(survey, recString);
                    personMap.put(person.getPumfId(), person);
                    recCount++;
                }
            } catch (Exception e) {
                logger.error("Could not read TSRC person data: ", e);
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
        Survey survey = dataDictionary.getSurvey("TSRC", year, "Trip");
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_pumf.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {

                int numIdentical = survey.readInt(recString, "TR_D11");  // ascii position in file: 174-175

                long origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
                int refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
                long pumfId = origPumfId * 100 + refYear % 100;

                SurveyPerson sp = personMap.get(pumfId);
                SurveyTour tour = new SurveyTour(survey, sp, recString);

                if (numIdentical < 30) {
                    for (int i = 1; i <= numIdentical; i++) sp.addTour(tour);
                } else {
                    sp.addTour(tour); //TODO: why?
                }
                recCount++;
            }
        } catch (Exception e) {
            logger.error("Could not read TSRC trip data: ", e);
        }
        logger.info("  Read " + recCount + " tour records.");
    }


    private void readTSRCvisitData(String dirName, int year) {
        // read visit location file

        String tripFileName = ResourceUtil.getProperty(rb, ("tsrc.visits"));
        String recString;
        int recCount = 0;
        Survey survey = dataDictionary.getSurvey("TSRC", year, "Visit");
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_PUMF.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                int refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
                int origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
                long pumfId = origPumfId * 100 + refYear % 100;
                int tripId = survey.readInt(recString, "TRIPID");  // ascii position in file: 014-015

                SurveyPerson person = personMap.get(pumfId);
                SurveyTour tour = person.getTourFromId(tripId);
                if (tour == null) {
                    logger.error(Integer.toString(year) + " - " + Integer.toString(origPumfId) + " - " + tripId + " - ?");
                } else {
                    tour.addTripDestinations(new SurveyVisit(survey, tour, recString));
                }
                recCount++;
            }
            //sort all the visits in order
            personMap.values().parallelStream().forEach(p -> p.getTours().stream().forEach(SurveyTour::sortVisits));

        } catch (Exception e) {
            logger.error("Could not read TSRC visit data: ", e);
        }
        logger.info("  Read " + recCount + " visit records.");
    }

    public void readItsConversionTables() {
        itsCountryConversion = Util.readCSVfile(rb.getString("its.country.conversion"));
        itsCountryConversion.buildIndex(3);

        itsModeConversion = Util.readCSVfile(rb.getString("its.mode.conversion"));
        itsModeConversion.buildIndex(3);

        itsTripPurposesConversion = Util.readCSVfile(rb.getString("its.purpose.conversion"));
        itsTripPurposesConversion.buildIndex(3);

    }

    private int convertCountry(int oldCode) {
        try {
            int newCode = (int) itsCountryConversion.getIndexedValueAt(oldCode, 2);
            return newCode;
        } catch (Exception e) {
            return oldCode;
        }

    }

    private int convertMode(int oldCode) {
        try {
            int newCode = (int) itsModeConversion.getIndexedValueAt(oldCode, 2);
            return newCode;
        } catch (Exception e) {
            return oldCode;
        }
    }

    private int convertPurpose(int oldCode) {

        try {
            int newCode = (int) itsTripPurposesConversion.getIndexedValueAt(oldCode, 2);
            return newCode;
        } catch (Exception e) {
            return oldCode;
        }
    }

}

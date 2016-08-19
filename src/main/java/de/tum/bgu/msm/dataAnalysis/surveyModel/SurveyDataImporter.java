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
    HashMap<Long, surveyPerson> personMap;

    private SurveyDataImporter() {

    }

    public static mtoSurveyData importData(ResourceBundle rb) {
        SurveyDataImporter sdi = new SurveyDataImporter();
        sdi.rb = rb;
        sdi.readInput();
        mtoSurveyData mtoSurveyData2 = sdi.buildDataModel();

        //generate all geometries
        mtoSurveyData2.getPersons().stream().forEach(p ->
                p.getTours().stream().forEach(t -> t.generateTourLineString(mtoSurveyData2))
        );

        return mtoSurveyData2;
    }

    private mtoSurveyData buildDataModel() {
        return  new mtoSurveyData(rb, personMap, dataDictionary);
    }

    private void readInput() {
        // read input data
        workDirectory = rb.getString("work.directory");

        dataDictionary = new DataDictionary(rb);
        personMap = new HashMap<>();



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
        String its_out_location = rb.getString("output.folder") + File.separator + rb.getString("its.out.file");
        PrintWriter out = util.openFileForSequentialWriting(its_out_location + ".csv", false);
        out.println("province,cma,weight");
        Survey survey = dataDictionary.getSurvey("ITS", year, "Canadians");
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
        logger.info("  Reading TSRC data for " + year);

        String dirName = workDirectory + ResourceUtil.getProperty(rb, ("tsrc.data.dir"));

        readTSRCpersonData(dirName, year);
        readTSRCtripData(dirName, year);
        readTSRCvisitData(dirName, year);

    }

    private void readTSRCpersonData (String dirName, int year) {
        // read person file

        String personFileName = ResourceUtil.getProperty(rb, ("tsrc.persons"));
        String recString;
        int totRecCount = 0;
        Survey survey = dataDictionary.getSurvey("TSRC", year, "Person");


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

                    surveyPerson person = new surveyPerson(survey, recString);
                    personMap.put(person.getPumfId(), person);
                    recCount++;
                }
            } catch (Exception e) {
                logger.error("Could not read TSRC person data: ",e);
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
        Survey survey = dataDictionary.getSurvey("TSRC", year, "Trip");
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_pumf.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {

                int numIdentical = survey.readInt(recString, "TR_D11");  // ascii position in file: 174-175

                long origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
                int refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
                long pumfId = origPumfId * 100 + refYear%100;

                surveyPerson sp = personMap.get(pumfId);
                surveyTour tour = new surveyTour(survey, sp, recString);

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
        Survey survey = dataDictionary.getSurvey("TSRC", year, "Visit");
        try {
            String fullFileName = dirName + File.separator + year + File.separator + tripFileName + year + "_PUMF.txt";
            BufferedReader in = new BufferedReader(new FileReader(fullFileName));
            while ((recString = in.readLine()) != null) {
                int refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
                int origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
                long pumfId = origPumfId * 100 + refYear%100;
                int tripId = survey.readInt(recString, "TRIPID");  // ascii position in file: 014-015

                surveyPerson person = personMap.get(pumfId);
                surveyTour tour = person.getTourFromId(tripId);
                if (tour == null) {
                    logger.error(Integer.toString(year) + " - " + Integer.toString(origPumfId) + " - " + tripId + " - ?");
                } else {
                    tour.addTripDestinations(new SurveyVisit(survey, tour, recString));
                    recCount++;
                }
            }
            //sort all the visits in order
            personMap.values().parallelStream().forEach(p -> p.getTours().stream().forEach(surveyTour::sortVisits));

        } catch (Exception e) {
            logger.error("Could not read TSRC visit data: ", e);
        }
        logger.info("  Read " + recCount + " visit records.");
    }



}

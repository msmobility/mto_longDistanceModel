package de.tum.bgu.msm.syntheticPopulation;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Class to read synthetic population
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 *
 */

public class readSP {

    private static Logger logger = Logger.getLogger(readSP.class);
    private ResourceBundle rb;


    public readSP(ResourceBundle rb) {
        // Constructor
        this.rb = rb;
    }


    public void readSyntheticPopulation() {
        // method to read in synthetic population
        logger.info("  Reading synthetic population");
        readSyntheticHouseholds();
        readSyntheticPersons();
        examSyntheticPopulation();
    }


    private void readSyntheticHouseholds() {

        String fileName = ResourceUtil.getProperty(rb, "syn.pop.hh");

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");


            int posId     = util.findPositionInArray("hhid", header);
//            int posSize   = util.findPositionInArray("hhsize",header);
            int posInc    = util.findPositionInArray("hhinc",header);
            int posDdType = util.findPositionInArray("dtype",header);
//            int posWrkrs  = util.findPositionInArray("nworkers",header);
//            int posKids   = util.findPositionInArray("kidspr",header);
            int posTaz    = util.findPositionInArray("taz",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id      = Integer.parseInt(lineElements[posId]);
//                int hhSize  = Integer.parseInt(lineElements[posSize]);
                int hhInc   = Integer.parseInt(lineElements[posInc]);
                int ddType  = Integer.parseInt(lineElements[posDdType]);
//                int numWrks = Integer.parseInt(lineElements[posWrkrs]);
//                int numKids = Integer.parseInt(lineElements[posKids]);
                int taz     = Integer.parseInt(lineElements[posTaz]);

                new Household(id, hhInc, ddType, taz);  // this automatically puts it in id->household map in Household class
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " households.");
    }


    private void readSyntheticPersons() {

        String fileName = ResourceUtil.getProperty(rb, "syn.pop.pp");

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");

            int posHhId             = util.findPositionInArray("hhid", header);
            int posId               = util.findPositionInArray("uid", header);
            int posAge              = util.findPositionInArray("taz",header);
            int posGender           = util.findPositionInArray("sex",header);
            int posOccupation       = util.findPositionInArray("nocs",header);
            int posAttSchool        = util.findPositionInArray("attsch",header);
            int posHighestDegree    = util.findPositionInArray("hdgree",header);
            int posTaz              = util.findPositionInArray("taz",header);
//            int posEmploymentStatus = util.findPositionInArray("cow",header);
//            int posFullOrPartTime   = util.findPositionInArray("fptwk",header);
//            int posHoursWorked      = util.findPositionInArray("hrswrk",header);
//            int posIndustrySector   = util.findPositionInArray("naics",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id             = Integer.parseInt(lineElements[posId]);
                int hhId           = Integer.parseInt(lineElements[posHhId]);
                int taz            = Integer.parseInt(lineElements[posTaz]);
                if (taz != Household.getHouseholdFromId(hhId).getTaz()) {
                    logger.error("Household " + hhId + " has different TAZ in household file than in person file at person ID " + id);
                }
                int age            = Integer.parseInt(lineElements[posAge]);
                String gender      = lineElements[posGender];
                int occupation     = Integer.parseInt(lineElements[posOccupation]);
                int education      = Integer.parseInt(lineElements[posHighestDegree]);
//                int employment     = Integer.parseInt(lineElements[posEmploymentStatus]);
//                int fullOrPartTime = Integer.parseInt(lineElements[posFullOrPartTime]);
//                int hoursWorked    = Integer.parseInt(lineElements[posHoursWorked]);
//                int industrySector = Integer.parseInt(lineElements[posIndustrySector]);
                new Person(id, hhId, age, gender, occupation, education);  // this automatically puts it in id->household map in Household class
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop person file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " persons.");

    }


    private void examSyntheticPopulation () {
        // run selected tests on synthetic population to ensure consistency

        // Test 1: Were all persons created? The person read method checks whether all households mentioned in the person
        // file exist. Here, check if all persons mentioned in the household file exist

        for (Household hh: Household.getHouseholdArray()) {
            for (Person pp: hh.getPersonsOfThisHousehold()) {
                if (pp == null) {
                    logger.error("Inconsistent synthetic population. Household " + hh.getId() + " is supposed to have " +
                            hh.getHhSize() + " persons, but at least one of them is missing in the person file. Program terminated.");
                    System.exit(9);

                }
            }
        }
    }
}

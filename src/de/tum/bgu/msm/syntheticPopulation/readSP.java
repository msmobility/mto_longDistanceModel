package de.tum.bgu.msm.syntheticPopulation;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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


            int posId     = util.findPositionInArray("hh_id", header);
            int posSize   = util.findPositionInArray("hhsize",header);
            int posInc    = util.findPositionInArray("hhinc",header);
            int posDdType = util.findPositionInArray("dtype",header);
            int posWrkrs  = util.findPositionInArray("nworkers",header);
            int posKids   = util.findPositionInArray("kidspr",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id      = Integer.parseInt(lineElements[posId]);
                int hhSize  = Integer.parseInt(lineElements[posSize]);
                int hhInc   = Integer.parseInt(lineElements[posInc]);
                int ddType  = Integer.parseInt(lineElements[posDdType]);
                int numWrks = Integer.parseInt(lineElements[posWrkrs]);
                int numKids = Integer.parseInt(lineElements[posKids]);

                new Household(id, hhSize, hhInc, ddType, numWrks, numKids);  // this automatically puts it in id->household map in Household class
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

            int posId               = util.findPositionInArray("pp_id", header);
            int posAgeGroup         = util.findPositionInArray("agegrp",header);
            int posGender           = util.findPositionInArray("sex",header);
            int posOccupation       = util.findPositionInArray("nocs",header);
            int posHighestDegree    = util.findPositionInArray("hdgree",header);
            int posEmploymentStatus = util.findPositionInArray("cow",header);
            int posFullOrPartTime   = util.findPositionInArray("fptwk",header);
            int posHoursWorked      = util.findPositionInArray("hrswrk",header);
            int posIndustrySector   = util.findPositionInArray("naics",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id             = Integer.parseInt(lineElements[posId]);
                int ageGroup       = Integer.parseInt(lineElements[posAgeGroup]);
                int gender         = Integer.parseInt(lineElements[posGender]);
                int occupation     = Integer.parseInt(lineElements[posOccupation]);
                int education      = Integer.parseInt(lineElements[posHighestDegree]);
                int employment     = Integer.parseInt(lineElements[posEmploymentStatus]);
                int fullOrPartTime = Integer.parseInt(lineElements[posFullOrPartTime]);
                int hoursWorked    = Integer.parseInt(lineElements[posHoursWorked]);
                int industrySector = Integer.parseInt(lineElements[posIndustrySector]);
                new Person(id, ageGroup, gender, occupation, education, employment, fullOrPartTime, hoursWorked, industrySector);  // this automatically puts it in id->household map in Household class
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop person file: " + fileName);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " persons.");

    }
}

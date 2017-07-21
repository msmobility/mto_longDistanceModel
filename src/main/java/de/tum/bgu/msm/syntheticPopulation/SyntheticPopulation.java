package de.tum.bgu.msm.syntheticPopulation;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Mto;
import de.tum.bgu.msm.longDistance.MtoLongDistance;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import org.apache.log4j.Logger;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 *
 * Ontario Provincial Model
 * Class to read synthetic population
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 *
 * Added read CD data to the method read zonal data (Carlos Llorca 20.07.16
 *
 */

public class SyntheticPopulation {

    private static Logger logger = Logger.getLogger(SyntheticPopulation.class);
    private ResourceBundle rb;
    private JsonUtilMto prop;

    private Map<Integer, Zone> zoneLookup;
    private MtoLongDistData mtoLongDistData;

    private String hhFilename;
    private String ppFilename;
    private String travellersFilename;


    private static final Map<Integer, Person> personMap = new Int2ObjectAVLTreeMap();

    private static final Map<Integer, Household> householdMap = new Int2ObjectAVLTreeMap<>();



    public SyntheticPopulation(ResourceBundle rb, JsonUtilMto prop, MtoLongDistData mtoLongDistData) {
        // Constructor
        this.rb = rb;
        this.prop = prop;
        this.mtoLongDistData=mtoLongDistData;

        //hhFilename = ResourceUtil.getProperty(rb, "syn.pop.hh");
        hhFilename = prop.getStringProp("sp.hh_file");
        //ppFilename = ResourceUtil.getProperty(rb, "syn.pop.pp");
        ppFilename = prop.getStringProp("sp.pp_file");
        travellersFilename = prop.getStringProp("out.travellers_file");


        logger.info("Synthetic population reader set up");



    }

    public void loadSyntheticPopulation(){
        this.zoneLookup = mtoLongDistData.getZoneLookup();

        readSyntheticPopulation();

        logger.info("Synthetic population loaded");


    }


    public void readSyntheticPopulation() {

        // method to read in synthetic population
        logger.info("  Reading synthetic population");
        //readZonalData();
        //ArrayList<Zone> internalZoneList = readInternalZones();
        readSyntheticHouseholds();
        readSyntheticPersons();
        examSyntheticPopulation();
        //summarizePopulationData();


    }

    /*public void readZonalData () {
        // Read in zonal data

        zoneTable = util.importTable(rb.getString("int.can"));
        zones = zoneTable.getColumnAsInt("ID");
        zoneIndex = new int[util.getHighestVal(zones) + 1];
        for (int i = 0; i < zones.length; i++) {
            zoneIndex[zones[i]] = i;
        }



        cdTable = util.importTable("input/listOfCd.csv");
        cds = cdTable.getColumnAsInt("CD");
        cdsIndex = new int[util.getHighestVal(cds) + 1];
        for (int i = 0; i < cds.length; i++) cdsIndex[cds[i]] = cds[i];

        cdTable.buildIndex(cdTable.getColumnPosition("ID"));
    }*/

    private void readSyntheticHouseholds() {



        String recString = "";
        int recCount = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(hhFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posId     = Util.findPositionInArray("hhid", header);
//            int posSize   = util.findPositionInArray("hhsize",header);
            int posInc    = Util.findPositionInArray("hhinc",header);
            int posDdType = Util.findPositionInArray("dtype",header);
//            int posWrkrs  = util.findPositionInArray("nworkers",header);
//            int posKids   = util.findPositionInArray("kidspr",header);
            int posTaz    = Util.findPositionInArray("ID",header);

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

                Zone zone = zoneLookup.get(taz);

                Household hh = new Household(id, hhInc, ddType, taz, zone);  // this automatically puts it in id->household map in Household class

                householdMap.put(id,hh);
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " households.");
    }


    private void readSyntheticPersons() {



        String recString = "";
        int recCount = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(ppFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posHhId             = Util.findPositionInArray("hhid", header);
            int posId               = Util.findPositionInArray("uid", header);
            int posAge              = Util.findPositionInArray("age",header);
            int posGender           = Util.findPositionInArray("sex",header);
            int posOccupation       = Util.findPositionInArray("nocs",header);
            int posAttSchool        = Util.findPositionInArray("attsch",header);
            int posHighestDegree    = Util.findPositionInArray("hdgree",header);
            int posEmploymentStatus = Util.findPositionInArray("work_status",header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id         = Integer.parseInt(lineElements[posId]);
                int hhId       = Integer.parseInt(lineElements[posHhId]);
                int age        = Integer.parseInt(lineElements[posAge]);
                char gender  = lineElements[posGender].charAt(0);
                int occupation = Integer.parseInt(lineElements[posOccupation]);
                int education  = Integer.parseInt(lineElements[posHighestDegree]);
                int workStatus = Integer.parseInt(lineElements[posEmploymentStatus]);
                Household hh = getHouseholdFromId(hhId);
                Person pp = new Person(id, hhId, age, gender, occupation, education, workStatus, hh);  // this automatically puts it in id->household map in Household class

                personMap.put(id,pp);
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop person file: " + ppFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " persons.");

    }


    private void examSyntheticPopulation () {
        // run selected tests on synthetic population to ensure consistency

        // Test 1: Were all persons created? The person read method checks whether all households mentioned in the person
        // file exist. Here, check if all persons mentioned in the household file exist

        for (Household hh: getHouseholds()) {
            for (Person pp: hh.getPersonsOfThisHousehold()) {
                if (pp == null) {
                    logger.error("Inconsistent synthetic population. Household " + hh.getId() + " is supposed to have " +
                            hh.getHhSize() + " persons, but at least one of them is missing in the person file. Program terminated.");
                    System.exit(9);

                }
            }
        }
    }

    public Person getPersonFromId(int personId) {
        return personMap.get(personId);
    }


    public int getPersonCount() {
        return personMap.size();
    }


    public Collection<Person> getPersons() {
        return personMap.values();
    }

    public Collection<Household> getHouseholds() {
        return householdMap.values();
    }


    public Household getHouseholdFromId(int householdId) {
        return householdMap.get(householdId);
    }


    public int getHouseholdCount() {
        return householdMap.size();
    }

    public void writeSyntheticPopulation() {
        logger.info("Writing out data for trip generation (travellers)");


        PrintWriter pw2 = Util.openFileForSequentialWriting(travellersFilename, false);

        pw2.println("personId, away, daytrip, inOutTrip");
        for (Person trav : getPersons()) {
            //takes only persons travelling
            if (trav.isAway() | trav.isDaytrip() | trav.isInOutTrip()) {
                pw2.println(trav.getPersonId() + "," + trav.isAway() + "," + trav.isDaytrip() + "," + trav.isInOutTrip());
            }
        }
        pw2.close();
    }

}

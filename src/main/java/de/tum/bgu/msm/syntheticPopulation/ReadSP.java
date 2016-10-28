package de.tum.bgu.msm.syntheticPopulation;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;

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

public class ReadSP {

    private static Logger logger = Logger.getLogger(ReadSP.class);
    private ResourceBundle rb;
    private final Map<Integer, Zone> zoneLookup;

    private TableDataSet cdTable;
    int [] cds;
    int [] cdsIndex;
    private int[] hhByCd;
    private int[] ppByCd;


    public ReadSP(ResourceBundle rb, MtoLongDistData mtoLongDistData) {
        // Constructor
        this.rb = rb;
        this.zoneLookup = mtoLongDistData.getZoneLookup();
        readSyntheticPopulation(mtoLongDistData.getInternalZoneList());

    }

    public void readSyntheticPopulation(ArrayList<Zone> internalZoneList) {
        // method to read in synthetic population
        logger.info("  Reading synthetic population");
        //readZonalData();
        //ArrayList<Zone> internalZoneList = readInternalZones();
        readSyntheticHouseholds();
        readSyntheticPersons();
        examSyntheticPopulation();
        //summarizePopulationData();
        summarizePopulationByZone(internalZoneList);
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



        String fileName = ResourceUtil.getProperty(rb, "syn.pop.hh");

        String recString = "";
        int recCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
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

                new Household(id, hhInc, ddType, taz, zone);  // this automatically puts it in id->household map in Household class
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
                String gender  = lineElements[posGender];
                int occupation = Integer.parseInt(lineElements[posOccupation]);
                int education  = Integer.parseInt(lineElements[posHighestDegree]);
                int workStatus = Integer.parseInt(lineElements[posEmploymentStatus]);
                new Person(id, hhId, age, gender, occupation, education, workStatus);  // this automatically puts it in id->household map in Household class
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


    /*private void summarizePopulationData () {
        // calculate households and persons by zone

        hhByZone = new int[zones.length];
        ppByZone = new int[zones.length];

        for (Household hh: Household.getHouseholdArray()) {
            hhByZone[zoneIndex[hh.getTaz()]]++;
            ppByZone[zoneIndex[hh.getTaz()]] += hh.getHhSize();
        }

        PrintWriter pw = util.openFileForSequentialWriting("output/popByZone.csv", false);
        pw.println("zone,hh,pp");
        for (int zone: zones) pw.println(zone+","+hhByZone[zoneIndex[zone]]+","+ppByZone[zoneIndex[zone]]);
        pw.close();



        hhByCd = new int[cdsIndex.length];
        ppByCd = new int[cdsIndex.length];

        for (Household hh: Household.getHouseholdArray()) {
            hhByCd[cdsIndex[(int)cdTable.getIndexedValueAt(hh.getTaz(),"CD")]]++;
            ppByCd[cdsIndex[(int)cdTable.getIndexedValueAt(hh.getTaz(),"CD")]] += hh.getHhSize();
        }

        PrintWriter pw2 = util.openFileForSequentialWriting("output/popByCd.csv", false);
        pw2.println("cd,hh,pp");
        for (int cds: cdsIndex) {
            if (cds!=0) pw2.println(cds+","+hhByCd[cdsIndex[cds]]+","+ppByCd[cdsIndex[cds]]);
        }
        pw2.close();
        logger.info("Synthetic population summary written");

    }*/

    private void summarizePopulationByZone(ArrayList<Zone> internalZoneList){
        for (Household hh : Household.getHouseholdArray()){
            Zone zone = hh.getZone();
            zone.addHouseholds(1);
            zone.addPopulation(hh.getHhSize());
            //add employees in their zone, discarded because employments are needed, instead employees:
            /*Person[] personsInHousehold = hh.getPersonsOfThisHousehold();
            for (Person pers:personsInHousehold){
                if (pers.getWorkStatus()==1|pers.getWorkStatus()==2){
                    zone.addEmployment(1);
                }
            }*/
        }

        PrintWriter pw = Util.openFileForSequentialWriting("output/popByZone.csv", false);
        pw.println("zone,hh,pp");
        for (Zone zone: internalZoneList){
            pw.println(zone.getId() +","+ zone.getHouseholds()+"," + zone.getPopulation());
        }
        pw.close();
    }

}

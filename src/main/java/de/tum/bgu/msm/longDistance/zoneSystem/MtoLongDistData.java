package de.tum.bgu.msm.longDistance.zoneSystem;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.Mto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Ontario Provincial Model
 * Class to store data for long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 21 April 2016
 * Version 1
 *
 */

public class MtoLongDistData {
    private static Logger logger = Logger.getLogger(MtoLongDistData.class);
    private ResourceBundle rb;
    private Matrix autoTravelTime;

    private double autoAccessibility;

    private ArrayList<Zone> zoneList;
    private ArrayList<Zone> internalZones;
    private ArrayList<Zone> externalZones;
    private final Map<Integer, Zone> zoneLookup;

    public static final List<String> tripPurposes = Arrays.asList("visit","business","leisure");
    public static final List<String> tripStates = Arrays.asList("away","daytrip","inout");


    public MtoLongDistData(ResourceBundle rb) {

        this.rb = rb;
        this.internalZones = readInternalZones();
        this.externalZones = readExternalZones();
        this.zoneList = new ArrayList<>();
        this.zoneList.addAll(internalZones);
        this.zoneList.addAll(externalZones);
        this.zoneLookup = zoneList.stream().collect(Collectors.toMap(Zone::getId, x -> x));
        ;
    }

    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }

    public void readSkim(String mode) {
        // read skim file
        logger.info("  Reading skims files");

        String matrixName = mode + ".skim." + Mto.getYear();
        String hwyFileName = rb.getString(matrixName);
        // Read highway hwySkim
        logger.info("Opening omx file: " + hwyFileName);
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.time"));
        autoTravelTime = Util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup("zone_number");
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoTravelTime.setExternalNumbersZeroBased(externalNumbers);
    }

    public float getAutoTravelTime(int orig, int dest) {
        try {
            return autoTravelTime.getValueAt(orig, dest);
        } catch (Exception e) {
            logger.error("*** Could not find zone pair " + orig + "/" + dest + " ***");
            return -999;
        }
    }


    public ArrayList<Zone> readInternalZones(){
        //create zones objects (empty) and a map to find them in hh zone assignment

        TableDataSet zoneTable;
        int[] zones;

        ArrayList<Zone> internalZoneList = new ArrayList<>();
        zoneTable = Util.readCSVfile(rb.getString("int.can"));
        zoneTable.buildIndex(1);
        zones = zoneTable.getColumnAsInt("ID");
        for (int zone : zones) {
            int combinedZone = (int) zoneTable.getIndexedValueAt(zone, "CombinedZone");
            int employment = (int) zoneTable.getIndexedValueAt(zone, "Employment");
            Zone internalZone = new Zone (zone, 0, employment, ZoneType.ONTARIO, combinedZone);
            internalZoneList.add(internalZone);
        }
        return internalZoneList;

    }

    public ArrayList<Zone> readExternalZones(){

        ArrayList<Zone> externalZonesArray = new ArrayList<>();

        boolean externalCanada = ResourceUtil.getBooleanProperty(rb, "ext.can", false);
        boolean externalUs = ResourceUtil.getBooleanProperty(rb, "ext.us", false);
        boolean externalOverseas = ResourceUtil.getBooleanProperty(rb, "ext.os", false);

        TableDataSet externalCanadaTable;
        TableDataSet externalUsTable;
        TableDataSet externalOverseasTable;
        int[] externalZonesCanada;
        int[] externalZonesUs;
        int[] externalZonesOverseas;

        //second, read the external zones from files

        if (externalCanada) {
            externalCanadaTable = Util.readCSVfile(rb.getString("ext.can.file"));
            externalZonesCanada = externalCanadaTable.getColumnAsInt("ID");
            externalCanadaTable.buildIndex(externalCanadaTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesCanada){
                int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
                Zone zone = new Zone (externalZone, (int)externalCanadaTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalCanadaTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTCANADA, combinedZone);
                externalZonesArray.add(zone);
            }
        }
        if (externalUs) {
            externalUsTable = Util.readCSVfile(rb.getString("ext.us.file"));
            externalZonesUs = externalUsTable.getColumnAsInt("ID");
            externalUsTable.buildIndex(externalUsTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesUs){
                //int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
                Zone zone = new Zone (externalZone, (int)externalUsTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalUsTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTUS, -1);
                externalZonesArray.add(zone);
            }
        }
        if (externalOverseas){
            externalOverseasTable = Util.readCSVfile(rb.getString("ext.os.file"));
            externalZonesOverseas = externalOverseasTable.getColumnAsInt("ID");
            externalOverseasTable.buildIndex(externalOverseasTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesOverseas){
                //int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
                Zone zone = new Zone (externalZone, (int)externalOverseasTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalOverseasTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTOVERSEAS, -1);
                externalZonesArray.add(zone);
            }
        }

        return externalZonesArray;
    }

    public void calculateAccessibility(ArrayList<Zone> zoneList, List<String> fromZones, List<String> toZones, float alphaAuto, float betaAuto) {

        //read alpha and beta parameters
        logger.info("Calculating accessibilities");

        //create lists of origin and destination zones

        ArrayList<Zone> origZoneList = new ArrayList<>();
        for (String stringZoneType:fromZones) {
            for (Zone zone : zoneList) {
                if(zone.getZoneType().equals(ZoneType.getZoneType(stringZoneType))) origZoneList.add(zone);
            }
        }

        ArrayList<Zone> destZoneList = new ArrayList<>();
        for (String stringZoneType:toZones) {
            for (Zone zone : zoneList) {
                if(zone.getZoneType().equals(ZoneType.getZoneType(stringZoneType))) destZoneList.add(zone);
            }
        }
        //calculate accessibilities
        for (Zone origZone : origZoneList) {
            autoAccessibility = 0;
            for (Zone destZone : destZoneList) {
                double autoImpedance;
                //limit the minimum travel time for accessibility calculations (namely long distance accessibility)
                //if (getAutoTravelTime(origZone.getId(), destZone.getId()) > 90) {
                if (getAutoTravelTime(origZone.getId(), destZone.getId()) <= 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                    //todo this value should be 0 or 1??
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(origZone.getId(), destZone.getId()));
                }
                //comment the variable that it is not desired (population or employment)
                autoAccessibility += Math.pow(destZone.getPopulation(), alphaAuto) * autoImpedance;
               //autoAccessibility += Math.pow(destZone.getEmployment(), alphaAuto) * autoImpedance;
            }

            //set accessibilities in the Zone objects
            origZone.setAccessibility(autoAccessibility);


        }
        logger.info("Accessibility calculated using alpha= " + alphaAuto + " and beta= " + betaAuto);
        //scaling accessibility (only for Ontario zones --> 100 is assigned to the highest value in Ontario)
        double[] autoAccessibilityArray = new double[zoneList.size()];

        int i = 0;
        double highestVal = 0;
        for (Zone zone : zoneList) {
            autoAccessibilityArray[i] = zone.getAccessibility();
            if (autoAccessibilityArray[i] > highestVal & zone.getZoneType().equals(ZoneType.ONTARIO)) {
                highestVal = autoAccessibilityArray[i];
            }
            i++;
        }
        i = 0;
        for (Zone zone : zoneList) {
            zone.setAccessibility(autoAccessibilityArray[i] / highestVal * 100);
            i++;
        }

    }

    public void writeOutAccessibilities(ArrayList<Zone> zoneList) {
        //print out accessibilities

        String fileName = rb.getString("access.out.file") + ".csv";
        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
        pw.println("Zone,Accessibility,Population,Employments");

        logger.info("Print out data of accessibility");

        for (Zone zone : zoneList) {
            //to print only ontario zones activate commented lines below
            //if (zone.getZoneType().equals(ZoneType.ONTARIO)){
            pw.println(zone.getId() + "," + zone.getAccessibility() + "," + zone.getPopulation() + "," + zone.getEmployment());
            //}
        }
        pw.close();
    }

    public Map<Integer, Zone> getZoneLookup() {
        return zoneLookup;
    }

    public ArrayList<Zone> getZoneList() {
        return zoneList;
    }

    public ArrayList<Zone> getExternalZoneList() {
        return externalZones;
    }

    public ArrayList<Zone> getInternalZoneList() {
        return internalZones;
    }

    public void populateZones(SyntheticPopulation syntheticPopulationReader) {
        for (Household hh : syntheticPopulationReader.getHouseholds()){
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

    }

    //original Rolf version below
        /*autoAccessibility = new double[zones.length];
        for (int orig: zones) {
            autoAccessibility[rsp.getIndexOfZone(orig)] = 0;
            for (int dest: zones) {
                double autoImpedance;
                if (getAutoTravelTime(orig, dest) == 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(orig, dest));
                }
                autoAccessibility[rsp.getIndexOfZone(orig)] += Math.pow(pop[rsp.getIndexOfZone(dest)], alphaAuto) *
                        autoImpedance;
            }
        }
        autoAccessibility = util.scaleArray(autoAccessibility, 100);*/


//        String fileName = "accessibility" + "OnToOn" + alphaAuto + betaAuto + ".csv";
//        PrintWriter pw = util.openFileForSequentialWriting(fileName, false);
//        pw.println("Zone,Accessibility");
//
//        logger.info("Accessibility parameters: alpha = " + alphaAuto + " and beta = "+ betaAuto);
//
//        for (int zone: zones) pw.println(zone+","+autoAccessibility[rsp.getIndexOfZone(zone)]);
//        pw.close();
    }


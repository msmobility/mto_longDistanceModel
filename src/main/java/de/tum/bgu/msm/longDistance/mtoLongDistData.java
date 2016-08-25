package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;
import sun.security.tools.keytool.Resources_sv;

import java.io.PrintWriter;
import java.util.*;

/**
 *
 * Ontario Provincial Model
 * Class to store data for long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 21 April 2016
 * Version 1
 *
 */

public class mtoLongDistData {
    private static Logger logger = Logger.getLogger(mtoLongDistData.class);
    private ResourceBundle rb;
    private Matrix autoTravelTime;
    private double autoAccessibility;

    private TableDataSet internalZonesTable;
    private TableDataSet externalCanadaTable;
    private TableDataSet externalUsTable;
    private TableDataSet externalOverseasTable;
    private int[] internalZones;
    private int[] externalZonesCanada;
    private int[] externalZonesUs;
    private int[] externalZonesOverseas;



    public mtoLongDistData(ResourceBundle rb) {
        this.rb = rb;
    }


    public void readSkim() {
        // read skim file
        logger.info("  Reading skims files");

        String hwyFileName = rb.getString("auto.skim." + mto.getYear());
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.time"));
        autoTravelTime = util.convertOmxToMatrix(timeOmxSkimAutos);
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

    ArrayList<Zone> readInternalAndExternalZones(ArrayList<Zone> internalZoneList){


        boolean externalCanada = ResourceUtil.getBooleanProperty(rb,"ext.can",false);
        boolean externalUs = ResourceUtil.getBooleanProperty(rb,"ext.us",false);;
        boolean externalOverseas = ResourceUtil.getBooleanProperty(rb,"ext.os",false);

        ArrayList<Zone> zonesArray = new ArrayList<>();

        //first read the internal zones already created and add them the employmeny from an external file

        internalZonesTable = util.importTable(rb.getString("int.employment.file"));
        internalZones = internalZonesTable.getColumnAsInt("ID");
        internalZonesTable.buildIndex(internalZonesTable.getColumnPosition("ID"));
        int emptyZoneCount = 0;
        for (Zone zone : internalZoneList){
            try {
                zone.setEmployment((int)internalZonesTable.getIndexedValueAt(zone.getId(), "Employment"));
            } catch (Exception e){
                emptyZoneCount++;
              logger.info(zone.getId() + " has 0 employments / is not located in internalZones.csv, total of empty zones = " + emptyZoneCount);

            }

        }

        zonesArray.addAll(internalZoneList);

       /* //first read the internal zones from RSP //this part won't be required
        int[] zones = rsp.getZones();
        int[] pop = rsp.getPpByZone();

        for (int i=0; i< zones.length; i++){
            Zone zone = new Zone (zones[i], pop[i], ZoneType.ONTARIO);
            zonesArray.add(zone);
        }*/

        //second, read the external zones from files

        if (externalCanada) {
            externalCanadaTable = util.importTable(rb.getString("ext.can.file"));
            externalZonesCanada = externalCanadaTable.getColumnAsInt("ID");
            externalCanadaTable.buildIndex(externalCanadaTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesCanada){
                Zone zone = new Zone (externalZone, (int)externalCanadaTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalCanadaTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTCANADA);
                zonesArray.add(zone);
            }
        }
        if (externalUs) {
            externalUsTable = util.importTable(rb.getString("ext.us.file"));
            externalZonesUs = externalUsTable.getColumnAsInt("ID");
            externalUsTable.buildIndex(externalUsTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesUs){
                Zone zone = new Zone (externalZone, (int)externalUsTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalUsTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTUS);
                zonesArray.add(zone);
            }
        }
        if (externalOverseas){
            externalOverseasTable = util.importTable(rb.getString("ext.ov.file"));
            externalZonesOverseas = externalOverseasTable.getColumnAsInt("ID");
            externalOverseasTable.buildIndex(externalOverseasTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesOverseas){
                Zone zone = new Zone (externalZone, (int)externalOverseasTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalOverseasTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTOVERSEAS);
                zonesArray.add(zone);
            }
        }

        return zonesArray;
    }

    public void calculateAccessibility(ArrayList<Zone> zoneList) {
        // calculate accessibility

        logger.info("Calculating accessibilities");
        float alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
        float betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");


        //calculate accessibilities

        for (Zone origZone : zoneList) {
            autoAccessibility = 0;
            for (Zone destZone : zoneList){
                double autoImpedance;
//                limit the minimum travel time for accessibility calculations
//                if (getAutoTravelTime(origZone.getId(), destZone.getId()) > 90) {
                if (getAutoTravelTime(origZone.getId(), destZone.getId()) == 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                    //todo this value should be 0 or 1??
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(origZone.getId(), destZone.getId()));
                }
                //comment the variable that it is not desired (population or employment)
                //autoAccessibility += Math.pow(destZone.getPopulation(), alphaAuto) * autoImpedance;
                autoAccessibility += Math.pow(destZone.getEmployment(), alphaAuto) * autoImpedance;
            }
            origZone.setAccessibility(autoAccessibility);

        }

        //scaling accessibilities (only for Ontario zones --> 100 is assigned to the highest value in Ontario)
        double[] autoAccessibilityArray = new double[zoneList.size()];

        int i = 0;
        double highestVal = 0;
        for (Zone zone : zoneList){
            autoAccessibilityArray[i] = zone.getAccessibility();
            if (autoAccessibilityArray[i] > highestVal & zone.getZoneType().equals(ZoneType.ONTARIO)){
                highestVal = autoAccessibilityArray[i];
            }
            i++;
        }
        //autoAccessibilityArray = util.scaleArray(autoAccessibilityArray, 100);
        i = 0;
        for (Zone zone : zoneList){
            zone.setAccessibility(autoAccessibilityArray[i]/highestVal*100);
            i++;
        }

    //print out accessibilities

        boolean externalCanada = ResourceUtil.getBooleanProperty(rb,"ext.can");
        boolean externalUs = ResourceUtil.getBooleanProperty(rb,"ext.us");
        boolean externalOverseas = ResourceUtil.getBooleanProperty(rb,"ext.os");

        String destArea = "ON";
        if (externalCanada){
            destArea = "Canada";
        }
        if (externalUs){
            destArea = "NorthAmerica";
        }
        if (externalOverseas){
            destArea = "World";
        }


        String fileName = rb.getString("access.out.file") + destArea + alphaAuto + betaAuto + ".csv";
        PrintWriter pw = util.openFileForSequentialWriting(fileName, false);
        pw.println("Zone,Accessibility,Population,Employments");

        logger.info("Accessibility parameters: alpha = " + alphaAuto + " and beta = "+ betaAuto);

        for (Zone zone: zoneList){
            if (zone.getZoneType().equals(ZoneType.ONTARIO)){
                pw.println(zone.getId() + "," + zone.getAccessibility()+ "," + zone.getPopulation() + "," + zone.getEmployment());
            }
        }
        pw.close();

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
}

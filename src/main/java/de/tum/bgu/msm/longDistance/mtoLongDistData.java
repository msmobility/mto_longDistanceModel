package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

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

    private TableDataSet externalCanadaTable;
    private TableDataSet externalUsTable;
    private TableDataSet externalOverseasTable;
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


    public void calculateAccessibility(readSP rsp) {
        // calculate accessibility

        logger.info("Calculating accessibilities");
        float alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
        float betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");

        //todo define as a property
        boolean externalCanada = false;
        boolean externalUs = true;
        boolean externalOverseas = true;

        ArrayList<Zone> zonesArray = new ArrayList<>();

        //first read the internal zones from RSP
        int[] zones = rsp.getZones();
        int[] pop = rsp.getPpByZone();

        for (int i=0; i< zones.length; i++){
            Zone zone = new Zone (zones[i], pop[i], ZoneType.ONTARIO);
            zonesArray.add(zone);
        }

        //second, read the external zones from files

        if (externalCanada) {
            externalCanadaTable = util.importTable("input/external/externalCanada.csv");
            externalZonesCanada = externalCanadaTable.getColumnAsInt("ID");
            externalCanadaTable.buildIndex(externalCanadaTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesCanada){
                Zone zone = new Zone (externalZone, (int)externalCanadaTable.getIndexedValueAt(externalZone, "Population"), ZoneType.EXTCANADA);
                zonesArray.add(zone);
            }
        }else if (externalUs) {
            externalUsTable = util.importTable("input/external/externalUs.csv");
            externalZonesUs = externalUsTable.getColumnAsInt("ID");
            externalUsTable.buildIndex(externalUsTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesUs){
                Zone zone = new Zone (externalZone, (int)externalUsTable.getIndexedValueAt(externalZone, "Population"), ZoneType.EXTUS);
                zonesArray.add(zone);
            }
        }else if (externalOverseas){
            externalOverseasTable = util.importTable("input/external/externalOvserseas.csv");
            externalZonesOverseas = externalOverseasTable.getColumnAsInt("ID");
            externalOverseasTable.buildIndex(externalOverseasTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesOverseas){
                Zone zone = new Zone (externalZone, (int)externalOverseasTable.getIndexedValueAt(externalZone, "Population"), ZoneType.EXTOVERSEAS);
                zonesArray.add(zone);
            }
        }

        //calculate accessibilities

        for (Zone origZone : zonesArray) {
            autoAccessibility = 0;
            for (Zone destZone : zonesArray){
                double autoImpedance;
                if (getAutoTravelTime(origZone.getId(), destZone.getId()) == 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                    //todo this value should be 0 or 1??
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(origZone.getId(), destZone.getId()));
                }
                autoAccessibility += Math.pow(destZone.getPopulation(), alphaAuto) * autoImpedance;
            }
            origZone.setAccessibility(autoAccessibility);

        }

        //scaling accessibilities
        double[] autoAccessibilityArray = new double[zonesArray.size()];

        int i = 0;
        for (Zone zone : zonesArray){
            autoAccessibilityArray[i] = zone.getAccessibility();
            i++;
        }
        autoAccessibilityArray = util.scaleArray(autoAccessibilityArray, 100);
        i = 0;
        for (Zone zone : zonesArray){
            zone.setAccessibility(autoAccessibilityArray[i]);
            i++;
        }



        String destArea = new String();
        if (externalCanada){
            destArea = "ONandCAN";
        }else if (externalUs){
            destArea = "ONandCANandUS";
        } else if (externalOverseas){
            destArea = "ONandCANandUSandOS";
        } else {
            destArea = "ON";
        }

        String fileName = "output/accessibility" + "OnTo" + destArea + alphaAuto + betaAuto + ".csv";
        PrintWriter pw = util.openFileForSequentialWriting(fileName, false);
        pw.println("Zone,Accessibility");

        logger.info("Accessibility parameters: alpha = " + alphaAuto + " and beta = "+ betaAuto);

        for (Zone zone: zonesArray) pw.println(zone.getId() + "," + zone.getAccessibility());
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

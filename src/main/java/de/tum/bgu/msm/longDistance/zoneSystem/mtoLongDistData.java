package de.tum.bgu.msm.longDistance.zoneSystem;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.mto;
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

    public void readSkim(String mode) {
        // read skim file
        logger.info("  Reading skims files");

        String matrixName = mode + ".skim." + mto.getYear();
        String hwyFileName = rb.getString(matrixName);
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

    public void readInternalZonesEmployment(ArrayList<Zone> internalZoneList) {

        //read the internal zones already created and add them the employment from an external file

        internalZonesTable = util.importTable(rb.getString("int.employment.file"));
        internalZones = internalZonesTable.getColumnAsInt("ID");
        internalZonesTable.buildIndex(internalZonesTable.getColumnPosition("ID"));
        int emptyZoneCount = 0;

        for (Zone zone : internalZoneList) {
            try {
                zone.setEmployment((int) internalZonesTable.getIndexedValueAt(zone.getId(), "Employment"));
            } catch (Exception e) {
                emptyZoneCount++ ;
            }

        }
    logger.warn(emptyZoneCount+ " zones were found with employment equal to 0");

               /* //first read the internal zones from RSP //this part won't be required
        int[] zones = rsp.getZones();
        int[] pop = rsp.getPpByZone();

        for (int i=0; i< zones.length; i++){
            Zone zone = new Zone (zones[i], pop[i], ZoneType.ONTARIO);
            zonesArray.add(zone);
        }*/

    }

    public ArrayList<Zone> readExternalZones(){

        ArrayList<Zone> externalZonesArray = new ArrayList<>();

        boolean externalCanada = ResourceUtil.getBooleanProperty(rb, "ext.can", false);
        boolean externalUs = ResourceUtil.getBooleanProperty(rb, "ext.us", false);
        boolean externalOverseas = ResourceUtil.getBooleanProperty(rb, "ext.os", false);



        //second, read the external zones from files

        if (externalCanada) {
            externalCanadaTable = util.importTable(rb.getString("ext.can.file"));
            externalZonesCanada = externalCanadaTable.getColumnAsInt("ID");
            externalCanadaTable.buildIndex(externalCanadaTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesCanada){
                Zone zone = new Zone (externalZone, (int)externalCanadaTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalCanadaTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTCANADA);
                externalZonesArray.add(zone);
            }
        }
        if (externalUs) {
            externalUsTable = util.importTable(rb.getString("ext.us.file"));
            externalZonesUs = externalUsTable.getColumnAsInt("ID");
            externalUsTable.buildIndex(externalUsTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesUs){
                Zone zone = new Zone (externalZone, (int)externalUsTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalUsTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTUS);
                externalZonesArray.add(zone);
            }
        }
        if (externalOverseas){
            externalOverseasTable = util.importTable(rb.getString("ext.os.file"));
            externalZonesOverseas = externalOverseasTable.getColumnAsInt("ID");
            externalOverseasTable.buildIndex(externalOverseasTable.getColumnPosition("ID"));
            for (int externalZone : externalZonesOverseas){
                Zone zone = new Zone (externalZone, (int)externalOverseasTable.getIndexedValueAt(externalZone, "Population"),
                        (int)externalOverseasTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTOVERSEAS);
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
        PrintWriter pw = util.openFileForSequentialWriting(fileName, false);
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


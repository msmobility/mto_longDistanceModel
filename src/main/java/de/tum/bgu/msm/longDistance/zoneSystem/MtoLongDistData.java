package de.tum.bgu.msm.longDistance.zoneSystem;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Mto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.syntheticPopulation.Household;
import de.tum.bgu.msm.syntheticPopulation.SyntheticPopulation;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ontario Provincial Model
 * Class to store data for long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 21 April 2016
 * Version 1
 */

public class MtoLongDistData {
    private static Logger logger = Logger.getLogger(MtoLongDistData.class);
    private ResourceBundle rb;
    private JSONObject prop;
    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;

    private ArrayList<Zone> zoneList;

    private ArrayList<Zone> internalZones;
    private ArrayList<Zone> externalZones;
    private Map<Integer, Zone> zoneLookup;

    public static final List<String> tripPurposes = Arrays.asList("visit", "business", "leisure");
    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");

    private String[] autoFileMatrixLookup;
    private String[] distanceFileMatrixLookup;
    ;

    private TableDataSet zoneTable;
    private TableDataSet externalCanadaTable;
    private TableDataSet externalUsTable;
    private TableDataSet externalOverseasTable;


    public MtoLongDistData(ResourceBundle rb, JSONObject prop) {
        this.rb = rb;
        this.prop = prop;
        //autoFileMatrixLookup = new String[]{rb.getString("auto.skim.file"), rb.getString("auto.skim.matrix"), rb.getString("auto.skim.lookup")};
        //distanceFileMatrixLookup = new String[]{rb.getString("dist.skim.file"), rb.getString("dist.skim.matrix"), rb.getString("dist.skim.lookup")};

        autoFileMatrixLookup = new String[]{JsonUtilMto.getStringProp(prop,"zone.skim.time.file"),
                JsonUtilMto.getStringProp(prop, "zone.skim.time.matrix"),
                JsonUtilMto.getStringProp(prop,"zone.skim.time.lookup")};
        distanceFileMatrixLookup = new String[]{JsonUtilMto.getStringProp(prop,"zone.skim.distance.file"),
                JsonUtilMto.getStringProp(prop,"zone.skim.distance.matrix"),
                JsonUtilMto.getStringProp(prop,"zone.skim.distance.lookup")};

        //externalCanadaTable = Util.readCSVfile(rb.getString("ext.can.file"));
        externalCanadaTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone.external.canada_file"));
        externalCanadaTable.buildIndex(externalCanadaTable.getColumnPosition("ID"));

        //externalUsTable = Util.readCSVfile(rb.getString("ext.us.file"));
        externalUsTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone.external.us_file"));
        externalUsTable.buildIndex(externalUsTable.getColumnPosition("ID"));

        //externalOverseasTable = Util.readCSVfile(rb.getString("ext.os.file"));
        externalOverseasTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone.external.os_file"));
        externalOverseasTable.buildIndex(externalOverseasTable.getColumnPosition("ID"));

        //zoneTable = Util.readCSVfile(rb.getString("int.can"));
        zoneTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone.internal_file"));
        zoneTable.buildIndex(1);

        logger.info("Zonal data manager set up");
    }

    public void loadZonalData() {
        this.internalZones = readInternalZones();
        this.externalZones = readExternalZones();
        this.zoneList = new ArrayList<>();
        this.zoneList.addAll(internalZones);
        this.zoneList.addAll(externalZones);
        //convert the arraylist of zones into a map of zones accessible by id:
        this.zoneLookup = zoneList.stream().collect(Collectors.toMap(Zone::getId, x -> x));

        readSkims();

        logger.info("Zonal data loaded");
    }

    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }

    public void readSkims() {
        autoTravelTime = convertSkimToMatrix(autoFileMatrixLookup);
        autoTravelDistance = convertSkimToMatrix(distanceFileMatrixLookup);
    }

    public Matrix convertSkimToMatrix(String[] fileMatrixLookupName) {

        OmxFile skim = new OmxFile(fileMatrixLookupName[0]);
        skim.openReadOnly();
        OmxMatrix skimMatrix = skim.getMatrix(fileMatrixLookupName[1]);
        Matrix matrix = Util.convertOmxToMatrix(skimMatrix);
        OmxLookup omxLookUp = skim.getLookup(fileMatrixLookupName[2]);
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        matrix.setExternalNumbersZeroBased(externalNumbers);
        logger.info("  Skim matrix was read: " + fileMatrixLookupName[0]);
        return matrix;
    }

    public float getAutoTravelTime(int orig, int dest) {
        try {
            return autoTravelTime.getValueAt(orig, dest);
        } catch (Exception e) {
            logger.error("*** Could not find zone pair " + orig + "/" + dest + " ***");
            return -999;
        }
    }

    public float getAutoTravelDistance(int orig, int dest) {
        try {
            return autoTravelDistance.getValueAt(orig, dest);
        } catch (Exception e) {
            logger.error("*** Could not find zone pair " + orig + "/" + dest + " ***");
            return -999;
        }
    }


    public ArrayList<Zone> readInternalZones() {
        //create zones objects (empty) and a map to find them in hh zone assignment

        int[] zones;
        ArrayList<Zone> internalZoneList = new ArrayList<>();

        zones = zoneTable.getColumnAsInt("ID");
        for (int zone : zones) {
            int combinedZone = (int) zoneTable.getIndexedValueAt(zone, "CombinedZone");
            int employment = (int) zoneTable.getIndexedValueAt(zone, "Employment");
            //zones are created as empty as they are filled out using sp
            Zone internalZone = new Zone(zone, 0, employment, ZoneType.ONTARIO, combinedZone);
            internalZoneList.add(internalZone);
        }

        return internalZoneList;

    }

    public ArrayList<Zone> readExternalZones() {

        ArrayList<Zone> externalZonesArray = new ArrayList<>();

        int[] externalZonesCanada;
        int[] externalZonesUs;
        int[] externalZonesOverseas;

        //read the external zones from files

        externalZonesCanada = externalCanadaTable.getColumnAsInt("ID");
        for (int externalZone : externalZonesCanada) {
            int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
            Zone zone = new Zone(externalZone, (int) externalCanadaTable.getIndexedValueAt(externalZone, "Population"),
                    (int) externalCanadaTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTCANADA, combinedZone);
            externalZonesArray.add(zone);
        }


        externalZonesUs = externalUsTable.getColumnAsInt("ID");
        for (int externalZone : externalZonesUs) {
            //int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
            Zone zone = new Zone(externalZone, (int) externalUsTable.getIndexedValueAt(externalZone, "Population"),
                    (int) externalUsTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTUS, (int) externalUsTable.getIndexedValueAt(externalZone, "CombinedZone"));

            externalZonesArray.add(zone);
        }


        externalZonesOverseas = externalOverseasTable.getColumnAsInt("ID");
        for (int externalZone : externalZonesOverseas) {
            //int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
            long staticAttraction = (long) externalOverseasTable.getIndexedValueAt(externalZone, "staticAttraction");
            Zone zone = new Zone(externalZone, (int) externalOverseasTable.getIndexedValueAt(externalZone, "Population"),
                    (int) externalOverseasTable.getIndexedValueAt(externalZone, "Employment"), ZoneType.EXTOVERSEAS, (int) externalOverseasTable.getIndexedValueAt(externalZone, "CombinedZone"));
            zone.setStaticAttraction(staticAttraction);
            externalZonesArray.add(zone);
        }

        return externalZonesArray;
    }

    public void calculateAccessibility(ArrayList<Zone> zoneList, List<String> fromZones, List<String> toZones, float alphaAuto, float betaAuto) {

        //read alpha and beta parameters
        logger.info("   Calculating accessibilities");

        //create lists of origin and destination zones

        ArrayList<Zone> origZoneList = new ArrayList<>();
        for (String stringZoneType : fromZones) {
            for (Zone zone : zoneList) {
                if (zone.getZoneType().equals(ZoneType.getZoneType(stringZoneType))) origZoneList.add(zone);
            }
        }

        ArrayList<Zone> destZoneList = new ArrayList<>();
        for (String stringZoneType : toZones) {
            for (Zone zone : zoneList) {
                if (zone.getZoneType().equals(ZoneType.getZoneType(stringZoneType))) destZoneList.add(zone);
            }
        }

        double autoAccessibility;
        //calculate accessibilities
        for (Zone origZone : origZoneList) {
            autoAccessibility = 0;
            for (Zone destZone : destZoneList) {
                double autoImpedance;
                //limit the minimum travel time for accessibility calculations (namely long distance accessibility)
                //if (getAutoTravelTime(origZone.getId(), destZone.getId()) > 90) {
                if (getAutoTravelTime(origZone.getId(), destZone.getId()) <= 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(origZone.getId(), destZone.getId()));
                }

                autoAccessibility += Math.pow(destZone.getPopulation(), alphaAuto) * autoImpedance;

            }
            origZone.setAccessibility(autoAccessibility);


        }
        logger.info("Accessibility (raster zone level) calculated using alpha= " + alphaAuto + " and beta= " + betaAuto);
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
        //print out accessibilities - no longer used

        String fileName = rb.getString("access.out.file") + ".csv";
        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
        pw.println("Zone,Accessibility,Population,Employments");

        logger.info("Print out data of accessibility");

        for (Zone zone : zoneList) {
            pw.println(zone.getId() + "," + zone.getAccessibility() + "," + zone.getPopulation() + "," + zone.getEmployment());
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
        for (Household hh : syntheticPopulationReader.getHouseholds()) {
            Zone zone = hh.getZone();
            zone.addHouseholds(1);
            zone.addPopulation(hh.getHhSize());
        }

    }


}


package de.tum.bgu.msm.longDistance.zoneSystem;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.ModelComponent;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ontario Provincial Model
 * Class to store data for long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 21 April 2016
 * Version 1
 */

public class ZonalData implements ModelComponent {
    private static Logger logger = Logger.getLogger(ZonalData.class);
    private ResourceBundle rb;
    private JSONObject prop;
    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;

    private DataSet dataSet;

    private Map<Integer, Zone> zoneLookup;

    public static final List<String> tripPurposes = Arrays.asList("visit", "business", "leisure");
    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");

    private String[] autoFileMatrixLookup;
    private String[] distanceFileMatrixLookup;


    private TableDataSet zoneTable;
    private TableDataSet externalCanadaTable;
    private TableDataSet externalUsTable;
    private TableDataSet externalOverseasTable;


    public ZonalData() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder){

        this.prop = prop;
        //autoFileMatrixLookup = new String[]{rb.getString("auto.skim.file"), rb.getString("auto.skim.matrix"), rb.getString("auto.skim.lookup")};
        //distanceFileMatrixLookup = new String[]{rb.getString("dist.skim.file"), rb.getString("dist.skim.matrix"), rb.getString("dist.skim.lookup")};

        autoFileMatrixLookup = new String[]{JsonUtilMto.getStringProp(prop,"zone_system.skim.time.file"),
                JsonUtilMto.getStringProp(prop, "zone_system.skim.time.matrix"),
                JsonUtilMto.getStringProp(prop,"zone_system.skim.time.lookup")};
        distanceFileMatrixLookup = new String[]{JsonUtilMto.getStringProp(prop,"zone_system.skim.distance.file"),
                JsonUtilMto.getStringProp(prop,"zone_system.skim.distance.matrix"),
                JsonUtilMto.getStringProp(prop,"zone_system.skim.distance.lookup")};

        //externalCanadaTable = Util.readCSVfile(rb.getString("ext.can.file"));
        externalCanadaTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone_system.external.canada_file"));
        externalCanadaTable.buildIndex(externalCanadaTable.getColumnPosition("ID"));

        //externalUsTable = Util.readCSVfile(rb.getString("ext.us.file"));
        externalUsTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone_system.external.us_file"));
        externalUsTable.buildIndex(externalUsTable.getColumnPosition("ID"));

        //externalOverseasTable = Util.readCSVfile(rb.getString("ext.os.file"));
        externalOverseasTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone_system.external.overseas_file"));
        externalOverseasTable.buildIndex(externalOverseasTable.getColumnPosition("ID"));

        //zoneTable = Util.readCSVfile(rb.getString("int.can"));
        zoneTable = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"zone_system.internal_file"));
        zoneTable.buildIndex(1);

        logger.info("Zonal data manager set up");



    }

    public void load(DataSet dataset) {

        this.dataSet = dataset;
        ArrayList<Zone> zoneList;
        ArrayList<Zone> internalZones = readInternalZones();
        ArrayList<Zone> externalZones = readExternalZones();

        zoneList = new ArrayList<>();
        zoneList.addAll(internalZones);
        zoneList.addAll(externalZones);

        dataSet.setInternalZones(internalZones);
        dataSet.setExternalZones(externalZones);
        dataSet.setZones(zoneList.stream().collect(Collectors.toMap(Zone::getId, x -> x)));

        //convert the arraylist of zones into a map of zones accessible by id:
        this.zoneLookup = zoneList.stream().collect(Collectors.toMap(Zone::getId, x -> x));

        readSkims();

        logger.info("Zonal data loaded");
    }

    public void run(DataSet dataSet, int nThreads){

    }

    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }

    public void readSkims() {
        Matrix autoTravelTime = convertSkimToMatrix(autoFileMatrixLookup);
        //dataSet.setAutoTravelTime(autoTravelTime);
        dataSet.setAutoTravelTime(assignIntrazonalValues(autoTravelTime));

        //convertMatrixToSkim(autoFileMatrixLookup, autoTravelTime);

        Matrix autoTravelDistance = convertSkimToMatrix(distanceFileMatrixLookup);
        //dataSet.setAutoTravelDistance(autoTravelDistance);
        dataSet.setAutoTravelDistance(assignIntrazonalValues(autoTravelDistance));

        //convertMatrixToSkim(distanceFileMatrixLookup, autoTravelDistance);

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


    public void convertMatrixToSkim(String[] fileMatrixLookupName, Matrix matrix) {

        String fileName = "output/" +  fileMatrixLookupName[0];

        try (OmxFile omxFile = new OmxFile(fileName)) {

            int dim0 = matrix.getRowCount();

            int dim1 = dim0;

            int[] shape = {dim0,dim1};
            float mat1NA = -1;

            OmxMatrix.OmxFloatMatrix mat1 = new OmxMatrix.OmxFloatMatrix(fileMatrixLookupName[1], matrix.getValues(),mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(),"values");

            int lookup1NA = -1;
            int[] lookup1Data;

            lookup1Data = matrix.getExternalRowNumbersZeroBased();

            OmxLookup.OmxIntLookup lookup1 = new OmxLookup.OmxIntLookup(fileMatrixLookupName[2],lookup1Data,lookup1NA);

            omxFile.openNew(shape);
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup1);
            omxFile.save();
            System.out.println(omxFile.summary());

            omxFile.close();
            System.out.println(fileMatrixLookupName[0] + "matrix written");

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

    public Matrix assignIntrazonalValues(Matrix matrix){
        for (int i : matrix.getExternalRowNumbers()){
            float minDistance = 999;
            for (int j : matrix.getExternalRowNumbers()){
                if (i != j && minDistance > matrix.getValueAt(i,j) && matrix.getValueAt(i,j)!=0){
                    minDistance = matrix.getValueAt(i,j);
                }
            }
            matrix.setValueAt(i,i,minDistance/2);
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
    }









}


package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.dataAnalysis.gravityModel.GravityModel;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Joe on 11/08/2016.
 */
public class MtoGravityModel {

    private static final int MAX_NUM_ZONES = 10000;
    private final ResourceBundle rb;
    private final static Logger logger = Logger.getLogger(MtoGravityModel.class);
    private Matrix autoSkim;


    public MtoGravityModel(ResourceBundle rb) {

        this.rb = rb;
        readSkim();
    }

    public static void main (String[] args) throws ClassNotFoundException {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        MtoGravityModel tca = new MtoGravityModel(rb);
        tca.run("visit", 163);
        tca.run("leisure", 149);
        tca.run("business", 244);

    }


    private TableDataSet readTableFromDB(Connection conn, String sql) throws SQLException {
        TableDataSet tableDataSet = new TableDataSet();
        //need to run the query twice to get number of rows. bit hacky
        Statement stmt = conn.createStatement();

        stmt.execute(sql);
        ResultSet rs = stmt.getResultSet();
        int num_cols = rs.getMetaData().getColumnCount();
        int num_rows = 0;

        String[] headings = new String[num_cols];
        for (int i=0; i < num_cols; i++) {
            headings[i] = rs.getMetaData().getColumnLabel(i + 1);
        }

        double[][] data = new double[num_cols][MAX_NUM_ZONES];
        while (rs.next()) {
            for (int i=0; i < num_cols; i++) {
                data[i][rs.getRow()-1] = rs.getDouble(i+1);
            }
            num_rows++;
        }
        for (int i=0; i < num_cols; i++) {
            tableDataSet.appendColumn(Arrays.copyOf(data[i], num_rows), headings[i]);
        }
        tableDataSet.buildIndex(1);
        return tableDataSet;

    }

    private void run(String purpose, double etl) {
        TableDataSet zoneAttributes = null;
        //mtoSurveyData data = SurveyDataImporter.importData(rb);
        try (Connection conn = DatabaseInteractions.getPostgresConnection()) {

            String sql = "select alt as zone_id, coalesce(t.production, 0) as production, population + employment as attraction\n" +
                    "from destination_attributes\n" +
                    " left outer join \n" +
                    "\t(\n" +
                    "\t\tselect lvl2_orig as zone_id, \n" +
                    "\t\t\tpurpose, sum(wtep)  / (365 * 4) as production\n" + //4 years, want the daily trips
                    "\t\tfrom tsrc_trip_filtered\n";

            if (purpose != null) sql += "\t\twhere purpose = '" + purpose + "'\n";

            sql +=  "\t\tgroup by zone_id, purpose\n" +
                    "\t\t) as t \n" +
                    "on t.zone_id = alt\n" +
                    "order by alt, purpose";

            logger.debug(sql);

            zoneAttributes = readTableFromDB(conn, sql);

        } catch (SQLException ex) {
            logger.error("error connecting to db", ex);
        }

        //testZoneMapping();


        int max_zone_num = 10000;
        double[] productions = new double[max_zone_num];
        double[] attractions = new double[max_zone_num];
        Arrays.fill(productions, 0);
        Arrays.fill(attractions, 0);

        //TODO: build production and attractions arrays from TableDataSet (or just database)
        productions = zoneAttributes.getColumnAsDouble("production");
        attractions = zoneAttributes.getColumnAsDouble("attraction");


        int numZones = productions.length;
        productions = Arrays.copyOf(productions, numZones);
        attractions = Arrays.copyOf(attractions, numZones);

        //build gravity model
        //get all travel times between II and IE zones, but exlcude EE;
        double[][] skim = new double[numZones][numZones];
        //for every
        int[] zones = zoneAttributes.getColumnAsInt("zone_id");
        //int[] zone_types = zoneAttributes.getColumnAsInt("zone_type");
        logger.debug("highest zone:" + zones[numZones - 1] + " at " + (numZones - 1));
        filterExternalZones(skim, zones, numZones);

        logger.debug("number of valid zones: " + numZones);

        GravityModel gm = new GravityModel(zones, productions, attractions, skim, etl);
        double k = gm.calibrate();
        //output gravity model as a omx matrix

        gm.save("output/tripDist_" + purpose + ".omx");
        gm.outputToCsv("output/tripDist_" + purpose +".csv");

        /*try (Connection conn = DatabaseInteractions.getPostgresConnection()) {
            gm.outputToDb(conn);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());
        }*/
    }

    private void filterExternalZones(double[][] skim, int[] zones, int numZones) {
        Map<Integer, Integer> zoneLookup = IntStream.range(0,zones.length).boxed().collect(Collectors.toMap(Function.identity(), i -> zones[i]));

        //filter impedances, set to Inf for undesired EE zones
        Set<Integer> rightZones = new HashSet<>(Arrays.asList(70,76,77,78,79,80,81,83,97,98,101,104));
        Set<Integer>  leftZones = new HashSet<>(Arrays.asList(71,72,73,74,75,82,84,85,86,87,88,89,90,91,92,93,94,95,96,99,100,
                102,103,105,106,107,108,109,110,111,112,113,114,115,116,117));
        Set<Integer>  quebecZones = new HashSet<>(Arrays.asList(72,82,84,85,86,87,88,89,90,91,92,93,94,95,96,99,100,
                102,103,105,106,107,108,109,110,111,112,113,114,115,116,117));

        for (int i = 0; i < numZones; i++) {
            for (int j = 0; j < numZones; j++) {
                skim[i][j] = autoSkim.getValueAt(zones[i], zones[j]);
                int origZone = zoneLookup.get(i);
                int destZone = zoneLookup.get(j);
                boolean internal = origZone < 70 || destZone < 70;
                boolean ee = (rightZones.contains(origZone) && leftZones.contains(destZone)) || (leftZones.contains(origZone) && rightZones.contains(destZone));
                boolean quebecException = origZone != destZone && quebecZones.contains(origZone) && quebecZones.contains(destZone) &&
                        (origZone == 85 || origZone == 117 || destZone == 85 || destZone == 117);
                if (!(internal || ee || quebecException)) {
                    skim[i][i] = Double.POSITIVE_INFINITY;
                }
            }
        }
    }

    public void readSkim() {
        // read skim file
        logger.debug("  Reading skims files");

        String hwyFileName = rb.getString("skim.combinedzones");
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim"));
        autoSkim = util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoSkim.setExternalNumbersZeroBased(externalNumbers);
    }



}

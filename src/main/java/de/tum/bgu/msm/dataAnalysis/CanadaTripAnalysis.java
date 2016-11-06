package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.dataAnalysis.gravityModel.GravityModel;
import de.tum.bgu.msm.longDistance.mtoLongDistData;
import de.tum.bgu.msm.util;
import javafx.util.Pair;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created by Joe on 11/08/2016.
 */
public class CanadaTripAnalysis {

    private static final int MAX_NUM_ZONES = 10000;
    private final ResourceBundle rb;
    private final Logger logger = Logger.getLogger(this.getClass());
    private Matrix autoSkim;


    public CanadaTripAnalysis(ResourceBundle rb) {

        this.rb = rb;
        readSkim();
    }

    public static void main (String[] args) throws ClassNotFoundException {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        CanadaTripAnalysis tca = new CanadaTripAnalysis(rb);
        //tca.run("visit", -0.0047, 163);
        //tca.run("leisure", -0.0049, 149);
        tca.run("business", -0.0033, 244);

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

    private void run(String purpose, double g, double etl) {
        TableDataSet zoneAttributes = null;
        //mtoSurveyData data = SurveyDataImporter.importData(rb);
        try (Connection conn = DatabaseInteractions.getPostgresConnection()) {

            String sql = "select t.zone_id, sum(production) as production, sum(population + employment) as attraction\n" +
                    "from destination_attributes, \n" +
                    "\t(\n" +
                    "\t\tselect lvl2_orig as zone_id, \n" +
                    "\t\t\tcase when lvl2_orig < 70 then 1 else 0 end as zone_type,\n" +
                    "\t\t\tpurpose, wtep as production\n" +
                    "\t\tfrom tsrc_trip\n";
            if (purpose != null) sql += "\t\twhere purpose = '" + purpose + "'\n and dist2<9999 and refyear = 2014";
            sql +=  "\t\t) as t\n" +
                    "where t.zone_id = alt\n" +
                    "group by t.zone_id, zone_type, purpose\n" +
                    "order by t.zone_id, purpose\n";// where zone_id < 7060;";
            logger.info(sql);

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
        logger.info("highest zone:" + zones[numZones - 1] + " at " + (numZones - 1));
        for (int i = 0; i < numZones; i++) {
            for (int j = 0; j < numZones; j++) {
                skim[i][j] = autoSkim.getValueAt(zones[i], zones[j]);

                //if (skim[i][j] < 80) skim [i][j] =0.0;
            }
        }

        //set skim to 0/infinity when distance is less than 80km

        logger.info("number of valid zones: " + numZones);

        GravityModel gm = new GravityModel(zones, productions, attractions, skim, g, etl);
        gm.run();
        //output gravity model as a omx matrix

        //gm.save("output/tripDist_" + purpose + ".omx");
        gm.outputToCsv("output/tripDist_" + purpose +".csv");

        /*try (Connection conn = DatabaseInteractions.getPostgresConnection()) {
            gm.outputToDb(conn);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());
        }*/
    }

    public void readSkim() {
        // read skim file
        logger.info("  Reading skims files");

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

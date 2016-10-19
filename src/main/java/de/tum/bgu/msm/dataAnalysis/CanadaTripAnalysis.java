package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.dataAnalysis.gravityModel.GravityModel;
import de.tum.bgu.msm.longDistance.mtoLongDistData;
import de.tum.bgu.msm.util;
import javafx.util.Pair;
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

    public CanadaTripAnalysis(ResourceBundle rb) {
        this.rb = rb;
    }

    public static void main (String[] args) throws ClassNotFoundException {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        CanadaTripAnalysis tca = new CanadaTripAnalysis(rb);
        tca.run();

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

    private void run() {
        TableDataSet zoneAttributes = null;
        //mtoSurveyData data = SurveyDataImporter.importData(rb);
        try (Connection conn = DatabaseInteractions.getPostgresConnection()) {

            String sql = "select t.zone_id, sum(production) as production, sum(population + employment) as attraction\n" +
                    "from destination_attributes, \n" +
                    "\t(\n" +
                    "\t\tselect lvl2_orig as zone_id, \n" +
                    "\t\t\tcase when lvl2_orig < 70 then 1 else 0 end as zone_type,\n" +
                    "\t\t\tpurpose, wtep as production\n" +
                    "\t\tfrom tsrc_trip\n" +
                    "\t\twhere purpose = 'leisure'\n and dist2<9999" +
                    "\t\t) as t\n" +
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
        mtoLongDistData mtoLongDistData = new mtoLongDistData(rb);

        //get all travel times between II and IE zones, but exlcude EE;
        double[][] skim = new double[numZones][numZones];
        //for every
        int[] zones = zoneAttributes.getColumnAsInt("zone_id");
        //int[] zone_types = zoneAttributes.getColumnAsInt("zone_type");
        logger.info("highest zone:" + zones[numZones - 1] + " at " + (numZones - 1));
        for (int i = 0; i < numZones; i++) {
            for (int j = 0; j < numZones; j++) {
                skim[i][j] = mtoLongDistData.getAutoTravelTime(zones[i], zones[j]);

                //if (skim[i][j] < 80) skim [i][j] =0.0;
            }
        }

        //set skim to 0/infinity when distance is less than 80km

        logger.info("number of valid zones: " + numZones);

        GravityModel gm = new GravityModel(zones, productions, attractions, skim, 1);
        gm.run();
        //output gravity model as a omx matrix
        gm.save("output/tripDist.omx");
        try (Connection conn = DatabaseInteractions.getPostgresConnection()) {
            gm.outputToDb(conn);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());
        }
    }
/*
        try (Connection conn = DatabaseInteractions.getPostgresConnection()){
            TableDataSet zone_to_cds = readTableFromDB(conn, "select * from zone_to_lvl2_mapping");
            Map<Pair<Integer, Integer>, Double> agg_zones = gm.aggregate_zones(zone_to_cds);

            conn.prepareStatement("DELETE FROM gravity_model_results; ").execute();
            conn.setAutoCommit(false);
            PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO gravity_model_results VALUES (?,?,?)");

            for (Pair<Integer, Integer> k : agg_zones.keySet()) {
                preparedStatement.setInt(1, k.getKey());
                preparedStatement.setInt(2, k.getValue());
                preparedStatement.setDouble(3, agg_zones.get(k));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            conn.commit();

            //gm.outputToDb(conn);
        } catch (SQLException ex) {
            logger.error(ex.getMessage(), ex.getNextException());
        }

    }
*/
    //get zone for location
    private int getZone (TableDataSet zoneMapping, int prov, int cd, int cma) {

        int zone = 0;
        if (cd % 100 == 99) {
            zone = 0;
        }
        else if (prov == 35) {
            //get cd zone
            //logger.info("" + prov + " - " + cd);
            zone = (int) zoneMapping.getIndexedValueAt(cd, "zone");
        } else {
            try {
                zone = (int) zoneMapping.getIndexedValueAt(cma, "zone");
            } catch (ArrayIndexOutOfBoundsException e) {
                zone = (int) zoneMapping.getIndexedValueAt(prov, "zone");
            };
        }
        return zone;
    }

    private void testZoneMapping() {
        TableDataSet zoneMapping = null;
        try (Connection conn = DatabaseInteractions.getPostgresConnection()){

            String sql = "select zone, " +
                    "domesticVisit + domesticBusiness + domesticLeisure as production, " +
                    "population + employment as attraction " +
                    "from \"zone_counts\";";
            logger.info(sql);

            zoneMapping = readTableFromDB(conn, "select key, zone from pr_cd_cma_zone_mapping");

            Statement stmt = conn.createStatement();
            stmt.execute("select * from tsrc_trip where orcprovt = 35 or mddplfl = 35");
            ResultSet rs = stmt.getResultSet();

            PreparedStatement stmtUpdate = conn.prepareStatement("INSERT INTO trip_zones VALUES (?,?,?)");
            conn.setAutoCommit(false);

            while (rs.next()) {
                int origin_prov = rs.getInt("orcprovt");
                int origin_cd = rs.getInt("orccdt2");
                int origin_cma = rs.getInt("orccmat2");

                int dest_prov = rs.getInt("mddplfl");
                int dest_cd = rs.getInt("mdccd");
                int dest_cma = rs.getInt("mdccma2");

                int o_zone = getZone(zoneMapping, origin_prov, origin_cd, origin_cma);
                int d_zone = getZone(zoneMapping, dest_prov, dest_cd, dest_cma);
                logger.info(String.format("%d :  %d -> %d (%d %d %d) -> (%d, %d, %d)"
                        , rs.getInt("id"), o_zone,  d_zone, origin_prov, origin_cd, origin_cma, dest_prov, dest_cd, dest_cma));

                stmtUpdate.setInt(1, rs.getInt("id"));
                stmtUpdate.setInt(2, o_zone);
                stmtUpdate.setInt(3, d_zone);
                stmtUpdate.addBatch();

            }
            stmtUpdate.executeBatch();
            conn.commit();

        } catch (SQLException ex) {
            logger.error("error connecting to db", ex);
        }

    }



}

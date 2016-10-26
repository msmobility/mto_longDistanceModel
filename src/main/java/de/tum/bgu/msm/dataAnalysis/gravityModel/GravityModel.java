package de.tum.bgu.msm.dataAnalysis.gravityModel;

import com.pb.common.datafile.TableDataSet;
import javafx.util.Pair;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Joe on 27/07/2016.
 */
public class GravityModel {
    public static final double G = -0.0041;

    private Logger logger = Logger.getLogger(this.getClass());
    private AbstractMatrixMath matrixMath = new SerialMatrixMath();
    //zone labels
    private int[] zones;
    //array zone population
    private double[] productions;
    //array zone attraction
    private double[] attractions;
    //skim matrix
    private double[][] skim;

    double[][] impedances;

    //threshold
    private float solutionThreshold = 5;
    //max_iterations
    private int maxIterations = 0;
    //expected_trip_length
    private double expectedTripLength = 187.60; //make sure to exlude hte 99999 value

    public GravityModel(int[] zones, double[] productions, double[] attractions, double[][] skim, int maxIterations) {
        this.zones = zones;
        this.productions = productions;
        this.attractions = attractions;
        this.skim = skim;
        this.maxIterations = maxIterations;


    }

    public static void main(String[] args) {
              /*  double[][] skim = {{4.50, 4.00, 5.00},
                    {6.00, 5.50, 6.00},
                    {4.50, 6.50, 7.00},
                    {5.00, 7.50, 6.00}};*/
        int[] zones = new int[]{1, 2, 3};
        double[] productions = new double[]{400, 350, 250};
        double[] attractions = new double[]{300, 200, 500};
        double[][] orig_skim = {{5, 10, 18}, {13, 5, 15}, {20, 16, 6}};
        int iterations = 15;

        //apply impedances to matrix

        GravityModel gm = new GravityModel(zones, productions, attractions, orig_skim, iterations);
        gm.run();
    }


    public void run() {
        impedances =  new double[skim.length][skim[0].length];
        logger.info("Running gravity model estimation");

        logger.info("\tCalculating impedances");
        for (int i = 0; i < impedances.length; i++) {
            for (int j = 0; j < impedances[i].length; j++) {
                impedances[i][j] = f(skim[i][j]);
            }
        }
        //iteration
        boolean a = true;
        int i = 1;
        boolean found = false;

        double[] a_i = new double[attractions.length];
        double[] b_j = new double[productions.length];

        double[] TT_i = new double[attractions.length];
        double[] TT_j = new double[productions.length];

        double[][] newFactors = new double[productions.length][attractions.length];

        //logger.info(matrixMath.buildString(impedances));
        logger.info("\titerating...");
        while (i <= maxIterations) {
            matrixMath.sumReduceRows(TT_i, impedances);
            matrixMath.sumReduceColumns(TT_j, impedances);
            double o_diff = matrixMath.absoluteDifference(TT_i, productions);
            double d_diff = matrixMath.absoluteDifference(TT_j, attractions);
            logger.info(String.format("\t\tmissing origins: %.2f, missing destinations: %.2f", o_diff, d_diff));

            found = o_diff < solutionThreshold && d_diff < solutionThreshold;
            if (found) break;
            else {
                char round = a ? 'a' : 'b';
                logger.info("\t\titeration: " + i + ": " + round);
                if (a) {
                    matrixMath.setOnes(b_j);
                    matrixMath.divide(a_i, productions, TT_i);
                    a = false;
                } else { //b
                    matrixMath.divide(b_j, attractions, TT_j);
                    matrixMath.setOnes(a_i);
                    a = true;
                }
                logger.info("\t\t\tcreate new factors: " + i + ": " + round);
                matrixMath.outerProduct(newFactors, a_i, b_j);
                logger.info("\t\t\tadjust impedances: " + i + ": " + round);
                matrixMath.multiply(impedances, impedances, newFactors);
                i += 1;
                //logger.info(matrixMath.buildString(impedances));
            }

        }
        logger.info("\tsuitable solution found");
        double[][] test_result = new double[impedances.length][impedances[0].length];
        matrixMath.multiply(test_result, impedances, skim);
        //logger.info(matrixMath.buildString(test_result));
        double est = matrixMath.sum(test_result) / matrixMath.sum(impedances);
        logger.info(String.format("\tEstimated avg. trip length: %.2f of %s", est, expectedTripLength));
        logger.info(String.format("\tTotal number of trips: %.2f", matrixMath.sum(impedances)));

        matrixMath.sumReduceRows(TT_i, impedances);
        matrixMath.sumReduceColumns(TT_j, impedances);
        double o_diff = matrixMath.absoluteDifference(TT_i, productions);
        double d_diff = matrixMath.absoluteDifference(TT_j, attractions);
        logger.info(String.format("\t\tmissing origins: %.2f, missing destinations: %.2f", o_diff, d_diff));

    }

    private double f(double cost) {
        return Math.exp(G * cost);
    }


    public void save(String filename) {
        logger.info("skim shape: " + impedances.length);
        int[] shape = new int[]{impedances.length, impedances.length};
        OmxFile omxfile = new OmxFile(filename);
        omxfile.openNew(shape);
        OmxMatrix omxMatrix = new OmxMatrix.OmxDoubleMatrix("tripDistribution", impedances, -1.0);
        omxfile.addMatrix(omxMatrix);
        omxfile.addLookup(new OmxLookup.OmxIntLookup("zone", zones, null));
        omxfile.save();
    }

    public void outputToCsv(String filename) {
        String headers = "orig,dest,trips";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(headers);
            writer.write("\n");
            for (int i = 0; i < impedances.length; i++) {
                for (int j = 0; j < impedances[i].length; j++) {
                    writer.write(Integer.toString(zones[i]));
                    writer.write(",");
                    writer.write(Integer.toString(zones[j]));
                    writer.write(",");
                    writer.write(String.format("%.2f", impedances[i][j]));
                    writer.write("\n");
                }
            }

        } catch (IOException ex) {
            logger.error(ex);
        }
    }

    public void outputToDb(Connection conn) throws SQLException {
        conn.prepareStatement("DROP TABLE IF EXISTS gravity_model_results; ").execute();
        conn.prepareStatement("CREATE TABLE gravity_model_results(orig integer, dest integer, trips numeric);").execute();
        conn.setAutoCommit(false);
        PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO gravity_model_results VALUES (?,?,?)");
        for (int i = 0; i < impedances.length; i++) {
            for (int j = 0; j < impedances[i].length; j++) {
                preparedStatement.setInt(1, zones[i]);
                preparedStatement.setInt(2, zones[j]);
                preparedStatement.setDouble(3, impedances[i][j]);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        conn.commit();
    }

    public Map<Pair<Integer, Integer>, Double> aggregate_zones(TableDataSet mapping) {
        Map<Pair<Integer, Integer>, Double> aggregated_skim = new HashMap<>();

        for (int i = 0; i < impedances.length; i++) {
            for (int j = 0; j < impedances[i].length; j++) {
                //logger.info(String.format("lookup %d %d", zones[i], zones[j]));
                int o_cd = (int) mapping.getIndexedValueAt(zones[i], "cd");
                int d_cd = (int) mapping.getIndexedValueAt(zones[j], "cd");
                //logger.info(String.format("\tfound %d %d", o_cd, d_cd));
                Pair<Integer, Integer> pair = new Pair<>(o_cd, d_cd);
                aggregated_skim.putIfAbsent(pair, 0.0);

                aggregated_skim.put(pair, aggregated_skim.get(pair) + impedances[i][j]);
            }
        }
        return aggregated_skim;
    }
}

package de.tum.bgu.msm.dataAnalysis.gravityModel;

import com.pb.common.datafile.TableDataSet;
import javafx.util.Pair;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Joe on 27/07/2016.
 */
public class GravityModel {

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
    private int maxIterations = 1;
    //expected_trip_length
    private double expectedTripLength = 0; //make sure to exlude hte 99999 value

    public GravityModel(int[] zones, double[] productions, double[] attractions, double[][] skim, double expectedTripLength) {
        this.zones = zones;
        this.productions = productions;
        this.attractions = attractions;
        this.skim = skim;
        this.expectedTripLength = expectedTripLength;
        logger.setLevel(Level.INFO);


    }


 //   od.filter <- function( orig_pr, lvl2_orig, dest_pr, lvl2_dest ){
 //       quebec.exceptions = lvl2_orig != lvl2_dest &
 //               orig_pr == 24 & dest_pr == 24 &
 //               (lvl2_orig %in% c(85, 117) | lvl2_dest %in% c(85, 117))
 //       is.included = (orig_pr <= 35 & dest_pr >= 35) | (orig_pr >= 35 & dest_pr <= 35) | quebec.exceptions
 //       return (is.included)

    public double calibrate() {
        double est = 0;
        double max_k = -0.0001;
        double min_k = -0.03;
        double k = 0;
        int i = 0;
        while (i < 10 && Math.abs(est - expectedTripLength) > 2) {
            k = (max_k + min_k)/2;
            k = Math.round(k * 10000.0) / 10000.0; //4dp
            est = run(k);
            logger.debug(String.format(" %.4f : %.2f | %.2f", k, est, expectedTripLength));

            if (est < expectedTripLength) min_k = k;
            else max_k = k;
            i++;
        }
        logger.info(String.format("Solution: %.4f : %.2f // %.2f", k, est, expectedTripLength));
        return k;
    }

    public double run(double k) {
        impedances =  new double[skim.length][skim[0].length];
        logger.info("Running gravity model estimation: origins:" + Arrays.stream(productions).sum() );

        double[] a_i = new double[impedances.length];
        for (int i = 0; i < impedances.length; i++) {
            for (int j = 0; j < impedances[i].length; j++) {
                a_i[i] += attractions[j] * f(k, skim[i][j]);
            }
        }

        for (int i = 0; i < impedances.length; i++) {
            for (int j = 0; j < impedances[i].length; j++) {

                impedances[i][j] = productions[i] * attractions[j] * f(k, skim[i][j]);
                impedances[i][j] /= a_i[i];

            }
        }
        double[] TT_i = new double[attractions.length];
        double[] TT_j = new double[productions.length];

        matrixMath.sumReduceRows(TT_i, impedances);
        matrixMath.sumReduceColumns(TT_j, impedances);
        double o_diff = matrixMath.absoluteDifference(TT_i, productions);
        double d_diff = matrixMath.absoluteDifference(TT_j, attractions);

        logger.debug("\tsuitable solution found");
        double[][] test_result = new double[impedances.length][impedances[0].length];
        matrixMath.multiply(test_result, impedances, skim);
        //logger.info(matrixMath.buildString(test_result));
        double est = matrixMath.sum(test_result) / matrixMath.sum(impedances);
        logger.debug(String.format("\tEstimated avg. trip length: %.2f of %s", est, expectedTripLength));
        logger.debug(String.format("\tTotal number of trips: %.2f", matrixMath.sum(impedances)));

        logger.info(String.format("\t\tmissing origins: %.2f, missing destinations: %.2f", o_diff, d_diff));

        return est;

    }

    private double f(double k, double cost) {
        return Math.exp(k * cost);
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

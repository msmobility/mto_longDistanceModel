package de.tum.bgu.msm.dataAnalysis.gravityModel;

import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * Created by Joe on 27/07/2016.
 */
public class GravityModelSingleConstrained {
    private Logger logger = Logger.getLogger(this.getClass());
    private AbstractMatrixMath abstractMatrixMath = new SerialMatrixMath();
    //skim matrix
    //array zone population
    private double[] productions;
    //array zone attraction
    private double[] attractions;

    private double[][] skim;

    //threshold
    private float solutionThreshold = 5;
    //max_iterations
    private int maxIterations = 0;
    //expected_trip_length
    private double expectedTripLength = 11.5;

    public GravityModelSingleConstrained(double[] productions, double[] attractions, double[][] skim, int maxIterations) {
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
        double[] productions = new double[]{400,350,250};
        double[] attractions = new double[]{300,200,500};
        double[][] orig_skim = {{5,10,18},{13,5,15},{20,16,6}};
        int iterations = 15;

        //apply impedances to matrix

        GravityModelSingleConstrained gm = new GravityModelSingleConstrained(productions, attractions, orig_skim, iterations);
        gm.run();
    }


    public void run() {
        double[][]impedances = new double[skim.length][skim[0].length];
        logger.info("Running gravity model estimation");

        double[] a_i = new double[attractions.length];

        for (int i=0; i<impedances.length; i++) {
            for (int j=0; j < impedances[i].length; j++) {
                impedances[i][j] = productions[i] * attractions[j] * f(skim[i][j]);
            }
        }

        double k = Arrays.stream(productions).sum() / abstractMatrixMath.sum(impedances);

        for (int i=0; i<a_i.length; i++) {
            a_i[i] = 1.0;
            int d_sum = 0;
            for (int j=0; j < impedances[i].length; j++) {
                d_sum += attractions[j] * f(skim[i][j]);
            }
            a_i[i] /= d_sum;
            if (Double.isInfinite(a_i[i]))
                a_i[i] = 0;
        }

        double[] TT_i = new double[attractions.length];
        abstractMatrixMath.sumReduceRows(TT_i, impedances);
        abstractMatrixMath.divide(a_i, productions, TT_i);

        logger.info("\tCalculating impedances");
        for (int i=0; i<impedances.length; i++) {
            for (int j=0; j < impedances[i].length; j++) {
                if (Double.isInfinite(a_i[i]))
                    a_i[i] = 0;
                impedances[i][j] = k * (a_i[i] * productions[i] * attractions[j]) * f(skim[i][j]);
            }
        }


        logger.info("\tsuitable solution found");
        double[][] test_result = new double[impedances.length][impedances[0].length];
        abstractMatrixMath.multiply(test_result, impedances, skim);
        //logger.info(matrixMath.buildString(test_result));
        double est =  abstractMatrixMath.sum(test_result) / abstractMatrixMath.sum(impedances);
        logger.info(String.format("\tEstimated avg. trip length: %.2f of %s", est, expectedTripLength));

        abstractMatrixMath.sumReduceRows(TT_i, impedances);
        double o_diff = abstractMatrixMath.absoluteDifference(TT_i, productions);
        logger.info(String.format("\t\tmissing origins: %.2f, missing destinations: ???", o_diff));




    }

    private double f(double cost) {
        return Math.exp(-0.035 * cost);
    }


}

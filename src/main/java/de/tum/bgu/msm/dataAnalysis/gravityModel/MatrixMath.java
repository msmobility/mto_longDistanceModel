package de.tum.bgu.msm.dataAnalysis.gravityModel;

/**
 * Created by Joe on 27/07/2016.
 */
public interface MatrixMath {
    void sumReduceRows(double[] dest_array, double[][] skim);

    void sumReduceColumns(double[] dest_array, double[][] skim);

    double absoluteDifference(double[] a, double[] b);

    double absoluteDifference(double[] a, int[] b);

    void divide(double[] dest_array, double[] a, double[] b);

    void setOnes(double[] a);

    void outerProduct(double[][] dest_array, double[] a, double[] b);

    void multiply(double[][] dest_array, double[][] a, double[][] b);

    double sum(double[][] a);

    String buildString(double[][] skim);

    double[][] copy2d(double[][] original);
}

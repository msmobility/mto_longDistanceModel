package de.tum.bgu.msm.dataAnalysis.gravityModel;

/**
 * Created by Joe on 27/07/2016.
 */
public abstract class AbstractMatrixMath {
    abstract void sumReduceRows(double[] dest_array, double[][] skim);

    abstract void sumReduceColumns(double[] dest_array, double[][] skim);

    abstract double absoluteDifference(double[] a, double[] b);

    abstract double absoluteDifference(double[] a, int[] b);

    abstract void divide(double[] dest_array, double[] a, double[] b);

    abstract void setOnes(double[] a);

    abstract void outerProduct(double[][] dest_array, double[] a, double[] b);

    abstract void multiply(double[][] dest_array, double[][] a, double[][] b);

    abstract double sum(double[][] a);

    abstract String buildString(double[][] skim);

    abstract double[][] copy2d(double[][] original);

    public int nonZero(double[][] a) {
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (a[i][j] > 0) count++;
            }
        }
        return count;
    }
}

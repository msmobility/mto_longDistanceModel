package de.tum.bgu.msm.dataAnalysis.gravityModel;

import java.util.Arrays;

/**
 * Created by Joe on 27/07/2016.
 */
public class SerialMatrixMath extends AbstractMatrixMath {
    @Override
    public void sumReduceRows(double[] dest_array, double[][] skim) {
        assert dest_array.length == skim.length;
        for (int i=0; i<skim.length; i++) {
            dest_array[i] = Arrays.stream(skim[i]).sum();
        }

    }

    @Override
    public void sumReduceColumns(double[] dest_array, double[][] skim) {
        assert dest_array.length == skim[0].length;
        for (int j=0; j<dest_array.length; j++) {
            dest_array[j] = 0;
            for (double[] aSkim : skim) dest_array[j] += aSkim[j];
        }
    }

    @Override
    public double absoluteDifference(double[] a, double[] b) {
        double difference = 0;
        assert (a.length == b.length);
        for (int i=0; i<a.length; i++) {
            difference += Math.abs(a[i]-b[i]);
        }
        return difference;
    }

    @Override
    public double absoluteDifference(double[] a, int[] b) {
        double difference = 0;
        assert (a.length == b.length);
        for (int i=0; i<a.length; i++) {
            difference += Math.abs(a[i]-b[i]);
        }
        return difference;
    }

    @Override
    public void divide(double[] dest_array, double[] a, double[] b) {
        assert dest_array.length == a.length && dest_array.length == b.length;
        for (int i = 0; i < dest_array.length; i++) {
            dest_array[i] = a[i] / b[i];
        }
    }

    @Override
    public void setOnes(double[] a) {
        Arrays.fill(a, 1);
    }

    @Override
    public void outerProduct(double[][] dest_array, double[] a, double[] b) {
        assert dest_array.length == a.length && dest_array[0].length == b.length;

        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b.length; j++) {
                dest_array[i][j] = a[i] * b[j];
            }
        }
    }

    @Override
    public void multiply(double[][] dest_array, double[][] a, double[][] b) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (a[i][j] == 0 || b[i][j] == 0) {
                    dest_array[i][j] = 0;
                } else {
                    dest_array[i][j] = a[i][j] * b[i][j];
                }
            }
        }
    }

    @Override
    public double sum(double[][] a) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
               sum += a[i][j];
            }
        }
        return sum;
    }

    @Override
    public String buildString(double[][] skim) {
        String s = "\n";
        for (double[] row : skim)
        {
            s += '\t' + Arrays.toString(row) + "\n";
        }
        return s;
    }

    @Override
    public double[][] copy2d(double[][] original) {
        if (original == null) {
            return null;
        }

        final double[][] result = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            result[i] = Arrays.copyOf(original[i], original[i].length);
            // For Java versions prior to Java 6 use the next:
            // System.arraycopy(original[i], 0, result[i], 0, original[i].length);
        }
        return result;
    }

}
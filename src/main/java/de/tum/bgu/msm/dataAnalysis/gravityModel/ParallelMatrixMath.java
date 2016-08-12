package de.tum.bgu.msm.dataAnalysis.gravityModel;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Joe on 27/07/2016.
 */
public class ParallelMatrixMath implements MatrixMath {
    @Override
    public void sumReduceRows(double[] dest_array, double[][] skim) {
        assert dest_array.length == skim.length;
        IntStream.range(0, dest_array.length).parallel().forEach(i -> {
            dest_array[i] = Arrays.stream(skim[i]).sum();
        });

    }

    @Override
    public void sumReduceColumns(double[] dest_array, double[][] skim) {
        assert dest_array.length == skim[0].length;
        IntStream.range(0, dest_array.length).parallel().forEach(j -> {
            dest_array[j] = 0;
            for (double[] aSkim : skim) dest_array[j] += aSkim[j];
        });

    }

    @Override
    public double absoluteDifference(double[] a, double[] b) {
        assert (a.length == b.length);
        return IntStream.range(0, a.length).parallel().mapToDouble(i -> Math.abs(a[i]-b[i])).sum();
    }

    @Override
    public double absoluteDifference(double[] a, int[] b) {
        assert (a.length == b.length);
        return IntStream.range(0, a.length).parallel().mapToDouble(i -> Math.abs(a[i]-b[i])).sum();
    }

    @Override
    public void divide(double[] dest_array, double[] a, double[] b) {
        assert dest_array.length == a.length && dest_array.length == b.length;
        IntStream.range(0, a.length).parallel().forEach(i -> {
            dest_array[i] = a[i] / b[i];
        });
    }

    @Override
    public void setOnes(double[] a) {
        Arrays.parallelSetAll(a, x -> 1);
    }

    @Override
    public void outerProduct(double[][] dest_array, double[] a, double[] b) {
        assert dest_array.length == a.length && dest_array[0].length == b.length;
        IntStream.range(0, a.length).parallel().forEach(i -> {
            for (int j = 0; j < b.length; j++) {
                dest_array[i][j] = a[i] * b[j];
            }
        });
    }

    @Override
    public void multiply(double[][] dest_array, double[][] a, double[][] b) {
        IntStream.range(0, a.length).parallel().forEach(i -> {
            for (int j = 0; j < a[i].length; j++) {
                dest_array[i][j] = a[i][j] * b[i][j];
            }
        });
    }

    @Override
    public double sum(double[][] a) {
        return Arrays.stream(a).parallel().mapToDouble(a2 -> Arrays.stream(a2).sum()).sum();
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
        IntStream.range(0, original.length).parallel().forEach(i -> {
            result[i] = Arrays.copyOf(original[i], original[i].length);
            // For Java versions prior to Java 6 use the next:
            // System.arraycopy(original[i], 0, result[i], 0, original[i].length);
        });
        return result;
    }
}
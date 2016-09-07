package de.tum.bgu.msm.dataAnalysis.gravityModel;

/**
 * Created by Joe on 27/07/2016.
 */
public class CompareMatrixMathImplementations {
    AbstractMatrixMath seqMM = new SerialMatrixMath();
    AbstractMatrixMath parMM = new ParallelMatrixMath();

    public static void main(String[] args) {
        CompareMatrixMathImplementations  test = new CompareMatrixMathImplementations();

        test.run(test.seqMM);
        test.run(test.parMM);
    }
    private void run(AbstractMatrixMath mm) {

        int test_size = 20000;
        double[][] a = new double[test_size][test_size];

        for (int i=0; i<a.length; i++) {
            for (int j=0; j<a[i].length; j++) {
                a[i][j] = i+j;
            }
        }
        long startTime = System.currentTimeMillis();

        double result = mm.sum(a);
        System.out.println(result);
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);


    }
}

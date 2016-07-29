package de.tum.bgu.msm;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyVisit;
import de.tum.bgu.msm.dataAnalysis.surveyModel.mtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.surveyTour;
import omx.OmxMatrix;
import omx.hdf5.OmxHdf5Datatype;
import org.apache.log4j.Logger;


import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 *
 * Utility methods for Ontario Provincial Model
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 *
 */
public class util {
    static Logger logger = Logger.getLogger(util.class);


    public static ResourceBundle mtoInitialization(String resourceBundleName) {

        File propFile = new File(resourceBundleName);
        return ResourceUtil.getPropertyBundle(propFile);
    }


    public static TableDataSet readCSVfile (String fileName) {
        // read csv file and return as TableDataSet
        File dataFile = new File(fileName);
        TableDataSet dataTable;
        boolean exists = dataFile.exists();
        if (!exists) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            System.out.println("Current relative path is: " + s);
            logger.error("File not found: " + dataFile.getAbsolutePath());
            System.exit(1);
        }
        try {
            TableDataFileReader reader = TableDataFileReader.createReader(dataFile);
            dataTable = reader.readFile(dataFile);
            reader.close();
        } catch (Exception e) {
            logger.error("Error reading file " + dataFile);
            throw new RuntimeException(e);
        }
        return dataTable;
    }


    public static PrintWriter openFileForSequentialWriting(String fileName, boolean appendFile) {
        // open file and return PrintWriter object

        File outputFile = new File(fileName);
        try {
            FileWriter fw = new FileWriter(outputFile, appendFile);
            BufferedWriter bw = new BufferedWriter(fw);
            return new PrintWriter(bw);
        } catch (IOException e) {
            logger.error("Could not open file <" + fileName + ">.");
            return null;
        }
    }


    public static int getHighestVal(int[] array) {
        // return highest number in int array
        int high = Integer.MIN_VALUE;
        for (int num: array) high = Math.max(high, num);
        return high;
    }


    public static boolean containsElement (int[] array, int value) {
        // returns true if array contains value, otherwise false
        boolean found = false;
        for (int i: array) if (i == value) found = true;
        return found;
    }


    public static float rounder(float value, int digits) {
        // rounds value to digits behind the decimal point

        return Math.round(value * Math.pow(10, digits) + 0.5)/(float) Math.pow(10, digits);
    }


    public static long createTourId (long pumfId, int tripId) {
        return pumfId * 100 + tripId;
    }


    public static int getDaysOfMonth(int year, int month) {
        YearMonth yearMonthObject = YearMonth.of(year, month);
        return yearMonthObject.lengthOfMonth();
    }


    public static Matrix convertOmxToMatrix (OmxMatrix omxMatrix) {
        // convert OMX matrix into java matrix

        OmxHdf5Datatype.OmxJavaType type = omxMatrix.getOmxJavaType();
        String name = omxMatrix.getName();
        int[] dimensions = omxMatrix.getShape();

        if (type.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
            float[][] fArray = (float[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, fArray[i][j]);
            return mat;
        } else if (type.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
            double[][] dArray = (double[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, (float) dArray[i][j]);
            return mat;
        } else {
            logger.info("OMX Matrix type " + type.toString() + " not yet implemented. Program exits.");
            System.exit(1);
            return null;
        }
    }


    public static int findPositionInArray (String element, String[] arr){
        // return index position of element in array arr
        int ind = -1;
        for (int a = 0; a < arr.length; a++) if (arr[a].equalsIgnoreCase(element)) ind = a;
        if (ind == -1) logger.error ("Could not find element " + element +
                " in array (see method <findPositionInArray> in class <SiloUtil>");
        return ind;
    }


    public static TableDataSet importTable(String filePath) {

        // read a csv file into a TableDataSet
        TableDataSet table;
        CSVFileReader cfrReader = new CSVFileReader();
        try {
            table = cfrReader.readFile(new File( filePath ));
        } catch (Exception e) {
            throw new RuntimeException("File Not Found: <" + filePath + ">.", e);
        }
        return table;
    }


    public static double[] scaleArray (double[] array, double maxVal) {
        // scale float array so that largest value equals maxVal

        double highestVal = Double.MIN_VALUE;
        for (double val: array) highestVal = Math.max(val, highestVal);
        for (int i = 0; i < array.length; i++) array[i] = ((array[i] * maxVal * 1.) / (highestVal * 1.));
        return array;
    }


    public static <K,V> void outputCsv(String filename, String[] headers, Map<K,V> lines, BiFunction<K,V, Optional<String>> f) {
        try {
            FileWriter writer = new FileWriter(filename);
            //write headers
            writer.write(Arrays.stream(headers).collect(Collectors.joining(",")));
            writer.write("\n");
            for (K k : lines.keySet()) {
                Optional<String> toWrite = f.apply(k,lines.get(k));
                if (toWrite.isPresent()) {
                    writer.write(toWrite.get());
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }

    }

    public static void outputCsvTours(mtoSurveyData data, String filename, Map<surveyTour, SurveyVisit[]> tripStops) {
        String[] headers = new String[]{"id", "triplength", "path"};
        outputCsv(filename, headers, tripStops, (st, svs) -> {
            String ret = "";
            String wkt = buildTourWKT(data, svs, SurveyVisit::getUniqueCD);
            if (wkt.isEmpty()) {
                return Optional.empty();
            } else {
                ret += st.getUniqueId();
                ret += ',';
                ret += Integer.toString(tripStops.get(st).length);
                ret += ',';
                ret += wkt;
                return Optional.of(ret);
            }
        });

    }


    public static void outputTourCounts(mtoSurveyData data, String filename, Map<String, Long> tripStops) {
        String[] headers = new String[]{"path", "triplength", "count"};
        outputCsv(filename, headers, tripStops, (wkt, count) -> {
            String ret = "";
            if (wkt.isEmpty()) {
                return Optional.empty();
            } else {
                ret += wkt;
                ret += ',';
                ret += wkt.split(",").length; //hack to get the number of stops in the trip again
                ret += ',';
                ret += Long.toString(count);
                return Optional.of(ret);
            }
        });
    }

    public static String cmaToXY(mtoSurveyData data, int cma) {
        //logger.info(cma);
        TableDataSet cmaList = data.getCmaList();
        if (data.validCma(cma)) {
            return polygonIdToXY(cmaList, cma);
        } else return "-75.0 35.0";

    }

    public static String cdToXY(mtoSurveyData data, int cd) {
        //logger.info(cma);
        TableDataSet cdList = data.getCensusDivisionList();
        if (data.validCd(cd)) {
            return polygonIdToXY(cdList, cd);
        } else {
            logger.warn("Invalid census district: " + cd + ", trip will be ignored");
            return "";
        }

    }

    private static String polygonIdToXY(TableDataSet list, int id) {
        //logger.info(cma);
        float x = list.getIndexedValueAt(id, "X");
        float y = list.getIndexedValueAt(id, "Y");
        return Float.toString(x) + " " + Float.toString(y);
    }

    public static String buildTourWKT(mtoSurveyData data, SurveyVisit[] a, ToIntFunction<SurveyVisit> field) {
        String coordString = Arrays.stream(a)
                .map(v -> cdToXY(data, field.applyAsInt(v)))
                .collect(Collectors.joining(","));
        if (!coordString.isEmpty())
            return String.format("\"LINESTRING (%s)\"", coordString);
        else {
            return "";
        }
    }

    public static String buildTourWKT(List<String> coordinates) {
        String coordString = coordinates.stream().collect(Collectors.joining(","));
        if (!coordString.isEmpty())
            return String.format("\"LINESTRING (%s)\"", coordString);
        else {
            return "";
        }
    }
}

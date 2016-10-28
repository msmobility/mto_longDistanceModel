package de.tum.bgu.msm;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.tum.bgu.msm.dataAnalysis.surveyModel.MtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyTour;
import omx.OmxMatrix;
import omx.hdf5.OmxHdf5Datatype;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;


import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.lang.System.exit;

/**
 *
 * Utility methods for Ontario Provincial Model
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 *
 */
public class Util {
    static Logger logger = Logger.getLogger(Util.class);


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
            exit(1);
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
            exit(1);
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

    public static void outputTourCounts(MtoSurveyData data, String filename, Map<String, List<SurveyTour>> tourMap) {
        String[] headers = new String[]{"path", "mode", "trip_length", "tour_distance", "min_distance", "num_trips", "weighted_num_trips"};
        outputCsv(filename, headers, tourMap, (key, tours) -> {
            String ret = "";
            LineString line = tours.get(0).generateTourLineString(data);
            double weightedCount = tours.stream().mapToDouble(SurveyTour::getWeight).sum();
            if (line.isEmpty()) {
                return Optional.empty();
            } else {
                ret += String.format("\"%s\"", line.toText());
                ret += ',';
                ret += tours.get(0).getMainModeStr();
                ret += ',';
                ret += line.getNumPoints()-1; //hack to get the number of stops in the trip again
                ret += ',';
                ret += (int) getTourDistance(line) / 1000;
                ret += ',';
                ret += (tours.get(0).calculateFurthestDistance(data)) * 2;
                ret += ',';
                ret += tours.size();
                ret += ',';
                ret += weightedCount;
                return Optional.of(ret);
            }
        });
    }

    public static double getTourDistance(LineString ls) {
        //this could also be done using a distance matrix, or even the skim matrix
        double totalDistance = 0;
        try {
            Coordinate c0 = ls.getCoordinateN(0);
            for (int i=1; i<ls.getNumPoints(); i++) {
                Coordinate c1 = ls.getCoordinateN(i);
                //flip lat and long
                Coordinate c0flipped = new Coordinate(c0.y, c0.x);
                Coordinate c1flipped = new Coordinate(c1.y, c1.x);

                totalDistance += JTS.orthodromicDistance(c0flipped, c1flipped, CRS.decode("EPSG:4269"));
                c0 = c1;
            }
        } catch (TransformException | FactoryException | IllegalArgumentException e) {
            logger.error(ls, e);
            exit(1);
        }
        return totalDistance;
    }

}

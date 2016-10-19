package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.mtoLongDistData;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Created by Joe on 27/09/2016.
 */
public class TravelTimeAggregation {
    private final Logger logger = Logger.getLogger(this.getClass());
    private final ResourceBundle rb;
    double[][] cd_tt_small;
    int[] lookup = null;

    public TravelTimeAggregation(ResourceBundle rb) {
        this.rb = rb;
    }

    public static void main (String[] args) throws ClassNotFoundException {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        TravelTimeAggregation tca = new TravelTimeAggregation(rb);
        tca.run();
        tca.save("output/cd_travel_times.omx");

    }
    public void run() {
        mtoLongDistData mtoLongDistData = new mtoLongDistData(rb);

        TableDataSet cd_mapping = TableDataSet.readFile("input/zone_cd_mapping.csv");

        int[] cds = Arrays.stream(cd_mapping.getColumnAsInt("zone_lvl2")).distinct().sorted().toArray();
        lookup = cds;
        cd_tt_small = new double[cds.length][cds.length];


        long[][] cd_pp = new long[cds.length][cds.length];
        float[][] cd_tt = new float[cds.length][cds.length];

        for (int i = 1; i <= cd_mapping.getRowCount(); i++) {
            for (int j = 1; j <= cd_mapping.getRowCount(); j++) {
                int origin = (int) cd_mapping.getValueAt(i, "zone_id");
                int dest = (int) cd_mapping.getValueAt(j, "zone_id");

                int origin_cd = (int) cd_mapping.getValueAt(i, "zone_lvl2");
                int dest_cd = (int) cd_mapping.getValueAt(j, "zone_lvl2");

                long origin_pop = (long) cd_mapping.getValueAt(i, "population");
                long dest_pop = (long) cd_mapping.getValueAt(j, "population");

                //logger.info(String.format("%d %d: %f",  origin, dest, mtoLongDistData.getAutoTravelTime(origin, dest)));
                long pop_ij = origin_pop * dest_pop;

                cd_tt[origin_cd-1][dest_cd-1] += mtoLongDistData.getAutoTravelTime(origin, dest) * pop_ij;
                cd_pp[origin_cd-1][dest_cd-1] += pop_ij;


            }
        }
        File f = new File("output/cd_traveltimes2.csv");
        try (FileWriter writer = new FileWriter(f) ) {
            writer.write("origin_lvl2_zone, destination_lvl2_zone, travel_time\n");
            for (int i = 0; i < cd_pp.length; i++) {
                for (int j = 0; j < cd_pp.length; j++) {
                    if (cd_tt[i][j] > 0) {
                        float result = cd_tt[i][j] / cd_pp[i][j];
                        writer.write(String.format("%d,%d, %f\n", i+1, j+1, result));
                        //logger.info(String.format("%d %d: %d: %f", i, j, cd_pp[i][j], result));
                        cd_tt_small[i][j] = result;
                    }
                    else {
                        logger.warn("travel time is zero between " + (i+1) + " and " + (j+1));
                    }
                }
            }
        } catch (IOException ex) {
            logger.error(ex);
        }

    }

    public void save(String filename) {
        logger.info("skim shape: " + cd_tt_small.length);
        int[] shape = new int[]{cd_tt_small.length, cd_tt_small.length};
        OmxFile omxfile = new OmxFile(filename);
        omxfile.openNew(shape);
        OmxMatrix omxMatrix = new OmxMatrix.OmxDoubleMatrix("cd_traveltimes", cd_tt_small, -1.0);
        omxfile.addMatrix(omxMatrix);
        omxfile.addLookup(new OmxLookup.OmxIntLookup("cd", lookup, null));
        omxfile.save();
    }



}

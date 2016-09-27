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
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Created by Joe on 27/09/2016.
 */
public class TravelTimeAggregation {
    private final Logger logger = Logger.getLogger(this.getClass());
    private final ResourceBundle rb;

    public TravelTimeAggregation(ResourceBundle rb) {
        this.rb = rb;
    }

    public static void main (String[] args) throws ClassNotFoundException {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        TravelTimeAggregation tca = new TravelTimeAggregation(rb);
        tca.run();

    }
    public void run() {
        mtoLongDistData mtoLongDistData = new mtoLongDistData(rb);

        TableDataSet cd_mapping = TableDataSet.readFile("input/zone_cd_mapping.csv");


        long[][] cd_pp = new long[10000][10000];
        float[][] cd_tt = new float[10000][10000];

        for (int i = 1; i <= cd_mapping.getRowCount(); i++) {
            for (int j = 1; j <= cd_mapping.getRowCount(); j++) {
                int origin = (int) cd_mapping.getValueAt(i, "zone_id");
                int dest = (int) cd_mapping.getValueAt(j, "zone_id");

                int origin_cd = (int) cd_mapping.getValueAt(i, "cd");
                int dest_cd = (int) cd_mapping.getValueAt(j, "cd");

                int origin_pop = (int) cd_mapping.getValueAt(i, "population");
                int dest_pop = (int) cd_mapping.getValueAt(j, "population");
                if (origin_cd == 3501 && dest_cd == 3501) {
                    System.out.println(mtoLongDistData.getAutoTravelTime(origin, dest));
                }
                //logger.info(String.format("%d %d: %f",  origin, dest, mtoLongDistData.getAutoTravelTime(origin, dest)));
                long pop_ij = origin_pop * dest_pop;
                cd_tt[origin_cd][dest_cd] += mtoLongDistData.getAutoTravelTime(origin, dest) * pop_ij;
                cd_pp[origin_cd][dest_cd] += pop_ij;


            }
        }
        File f = new File("input/cd_traveltimes.csv");
        try (FileWriter writer = new FileWriter(f) ) {
            writer.write("origin_cd, destination_cd, connections, travel_time\n");
            for (int i = 0; i < cd_pp.length; i++) {
                for (int j = 0; j < cd_pp.length; j++) {
                    if (cd_tt[i][j] > 0) {
                        float result = cd_tt[i][j] / cd_pp[i][j];
                        writer.write(String.format("%d,%d,%d, %f\n", i, j, cd_pp[i][j], result));
                        //logger.info(String.format("%d %d: %d: %f", i, j, cd_pp[i][j], result));
                    }
                }
            }
        } catch (IOException ex) {
            logger.error(ex);
        }

    }
}

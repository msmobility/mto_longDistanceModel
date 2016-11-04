package de.tum.bgu.msm.dataAnalysis;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by Joe on 27/09/2016.
 */
public class TravelTimeAggregation {
    private final Logger logger = Logger.getLogger(this.getClass());
    private final ResourceBundle rb;
    private Matrix autoTravelTime;
    double[][] cd_tt_small;
    int[] lookup = null;

    public TravelTimeAggregation(ResourceBundle rb) {
        this.rb = rb;
    }

    public static void main (String[] args) throws ClassNotFoundException {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        TravelTimeAggregation tca = new TravelTimeAggregation(rb);
        tca.run();
        tca.save(rb.getString("aggregated.zones.output.filename"));

    }
    public void run() {
        readSkim();

        TableDataSet cd_mapping = TableDataSet.readFile(rb.getString("zone.aggregation.mapping.file"));

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

                cd_tt[origin_cd-1][dest_cd-1] += getAutoTravelTime(origin, dest) * pop_ij;
                cd_pp[origin_cd-1][dest_cd-1] += pop_ij;


            }
        }
        for (int i = 0; i < cd_pp.length; i++) {
            for (int j = 0; j < cd_pp.length; j++) {
                if (cd_tt[i][j] > 0) {
                    float result = cd_tt[i][j] / cd_pp[i][j];
                    //logger.info(String.format("%d %d: %d: %f", i, j, cd_pp[i][j], result));
                    cd_tt_small[i][j] = result;
                }
                else {
                    logger.warn("travel time is zero between " + (i+1) + " and " + (j+1));
                }
            }
        }


    }

    public void save(String filename) {
        logger.info("skim shape: " + cd_tt_small.length);
        int[] shape = new int[]{cd_tt_small.length, cd_tt_small.length};
        OmxFile omxfile = new OmxFile(filename + ".omx");
        omxfile.openNew(shape);
        OmxMatrix omxMatrix = new OmxMatrix.OmxDoubleMatrix("combined_zones_skim", cd_tt_small, -1.0);
        omxfile.addMatrix(omxMatrix);
        omxfile.addLookup(new OmxLookup.OmxIntLookup("combined_zones", lookup, null));
        omxfile.save();

        File f = new File(filename + ".csv");
        try (FileWriter writer = new FileWriter(f) ) {
            writer.write("origin_lvl2_zone, destination_lvl2_zone, travel_time\n");
            for (int i = 0; i < cd_tt_small.length; i++) {
                for (int j = 0; j < cd_tt_small.length; j++) {
                    writer.write(String.format("%d,%d, %f\n", i + 1, j + 1, cd_tt_small[i][j]));
                }
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
    }

    public void readSkim() {
        // read skim file
        logger.info("  Reading skims files");

        String hwyFileName = rb.getString("skim.combinedzones");
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim"));
        autoTravelTime = util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup(rb.getString("lookup"));
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        autoTravelTime.setExternalNumbersZeroBased(externalNumbers);
    }
    public float getAutoTravelTime(int orig, int dest) {
        try {
            return autoTravelTime.getValueAt(orig, dest);
        } catch (Exception e) {
            logger.error("*** Could not find zone pair " + orig + "/" + dest + " ***", e);
            return -999;
        }
    }



}

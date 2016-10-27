package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ResourceBundle;

/**
 *
 * Ontario Provincial Model
 * Class to store data for long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 21 April 2016
 * Version 1
 *
 */

public class mtoLongDistData {
    private static Logger logger = Logger.getLogger(mtoLongDistData.class);
    private ResourceBundle rb;
    private Matrix autoTravelTime;
    private double[] autoAccessibility;


    public mtoLongDistData(ResourceBundle rb) {

        this.rb = rb;
        this.readSkim(2013);
    }


    public void readSkim(int year) {
        // read skim file
        logger.info("  Reading skims files");

        String hwyFileName = rb.getString("auto.skim.combinedzones." + year);
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.combinedzones.time"));
        autoTravelTime = util.convertOmxToMatrix(timeOmxSkimAutos);
        OmxLookup omxLookUp = hSkim.getLookup("combinedZone");
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


    public void calculateAccessibility(readSP rsp) {
        // calculate accessibility

        logger.info("Calculating accessibilities");
        float alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
        float betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");

        int[] zones = rsp.getZones();
        int[] pop = rsp.getPpByZone();
        autoAccessibility = new double[zones.length];
        for (int orig: zones) {
            autoAccessibility[rsp.getIndexOfZone(orig)] = 0;
            for (int dest: zones) {
                double autoImpedance;
                if (getAutoTravelTime(orig, dest) == 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(orig, dest));
                }
                autoAccessibility[rsp.getIndexOfZone(orig)] += Math.pow(pop[rsp.getIndexOfZone(dest)], alphaAuto) *
                        autoImpedance;
            }
        }
        autoAccessibility = util.scaleArray(autoAccessibility, 100);


        PrintWriter pw = util.openFileForSequentialWriting("accessibility.csv", false);
        pw.println("Zone,Accessibility");

        for (int zone: zones) pw.println(zone+","+autoAccessibility[rsp.getIndexOfZone(zone)]);
        pw.close();
    }
}

package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.syntheticPopulation.readSP;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static Logger logger = LogManager.getLogger(mtoLongDistData.class);
    private ResourceBundle rb;
    private Matrix autoTravelTime;
    private double[] autoAccessibility;


    public mtoLongDistData(ResourceBundle rb) {
        this.rb = rb;
    }


    public void readSkim() {
        // read skim file
        logger.info("  Reading skims files");

        String hwyFileName = rb.getString("auto.skim." + mto.getYear());
        // Read highway hwySkim
        OmxFile hSkim = new OmxFile(hwyFileName);
        hSkim.openReadOnly();
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix(rb.getString("skim.time"));
        autoTravelTime = util.convertOmxToMatrix(timeOmxSkimAutos);

        System.out.println(autoTravelTime.getValueAt(1,1));
        System.out.println(autoTravelTime.getValueAt(1,2));
        System.out.println(autoTravelTime.getValueAt(2,1));
    }


    public float getAutoTravelTime(int orig, int dest) {
        return autoTravelTime.getValueAt(orig, dest);
    }


    public void calculateAccessibility(readSP rsp) {
        // calculate accessibility

        logger.info("  Calculating accessibilities");
        float alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
        float betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");

        int[] zones = rsp.getZones();
        int[] pop = rsp.getPpByZone();
        autoAccessibility = new double[zones.length];
        for (int orig: zones) {
            autoAccessibility[rsp.getIndexOfZone(orig)] = 0;
            for (int dest: zones) {
                double autoImpedance;
                System.out.println(orig+"-"+dest+":"+getAutoTravelTime(orig, dest));
                if (getAutoTravelTime(orig, dest) == 0) {      // should never happen for auto
                    autoImpedance = 0;
                } else {
                    autoImpedance = Math.exp(betaAuto * getAutoTravelTime(orig, dest));
                }
                autoAccessibility[rsp.getIndexOfZone(orig)] += Math.pow(pop[dest], alphaAuto) * autoImpedance;
            }
        }
        autoAccessibility = util.scaleArray(autoAccessibility, 100);


        PrintWriter pw = util.openFileForSequentialWriting("accessibility.csv", false);
        pw.println("Zone,Accessibility");

        for (int zone: zones) pw.println(zone+","+autoAccessibility[rsp.getIndexOfZone(zone)]);
    }
}

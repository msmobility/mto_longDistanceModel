package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.mto;
import de.tum.bgu.msm.util;
import omx.OmxFile;
import omx.OmxMatrix;
import org.apache.log4j.Logger;

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
        OmxMatrix timeOmxSkimAutos = hSkim.getMatrix("mf4_TT4");
        autoTravelTime = util.convertOmxToMatrix(timeOmxSkimAutos);
    }


    public float getAutoTravelTime(int orig, int dest) {
        return autoTravelTime.getValueAt(orig, dest);
    }


    public void calculateAccessibility() {
        // calculate accessibility

    }
}

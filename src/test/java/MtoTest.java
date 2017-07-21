import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.MtoLongDistance;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.ResourceBundle;

import static junit.framework.Assert.assertEquals;

import org.matsim.core.utils.misc.CRCChecksum;

/**
 * Created by carlloga on 7/18/2017.
 */
public class MtoTest {

    private static final Logger logger = Logger.getLogger(MtoTest.class);
    private ResourceBundle rb;


    @Test
     public final void testMain(){


        ResourceBundle rb = Util.mtoInitialization("src/test/resources/mto.properties");

        //todo prepare inpuot folders

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "0");

        logger.info("Started test for runLongDistModel");
        //MtoLongDistance ld = new MtoLongDistance(rb);
        //ld.loadLongDistanceModel();
        //ld.runLongDistanceModel();
        logger.info("Module runLongDistModel completed");

        {
            logger.info("Checking trips file ...");
//            long checksum_ref = CRCChecksum.getCRCFromFile("the other file");
//            final String filename = ".the original file";
//            long checksum_run = CRCChecksum.getCRCFromFile(filename);
//            assertEquals("Trip files are different", checksum_ref, checksum_run);
        }

    }





}

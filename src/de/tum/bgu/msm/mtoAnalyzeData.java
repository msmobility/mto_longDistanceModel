package de.tum.bgu.msm;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * Class to hold person object of travelers and non-travelers of the TSRC survey
 *
 * @author Rolf Moeckel
 * Created on 26 Feb. 2016 in Vienna, VA
 *
 **/

public class mtoAnalyzeData {
    static Logger logger = Logger.getLogger(mto.class);
    private ResourceBundle rb;


    public mtoAnalyzeData(ResourceBundle rb) {
        this.rb = rb;
        // Constructor
    }


    public void runAnalyses () {
        // run selected long-distance travel analyzes
        if (!ResourceUtil.getBooleanProperty(rb, "analyze.ld.data")) return;
        logger.info("Running selected analyses of long-distance data");

        int[] incomeCountTrips = new int[10];
        int[] incomeCountPersons = new int[10];
        surveyPerson[] spList = surveyPerson.getPersonArray();
        for (surveyPerson sp: spList) {
            incomeCountPersons[sp.getHhIncome()] += sp.getWeight();
            if (sp.getNumberOfTrips() > 0) {
                incomeCountTrips[sp.getHhIncome()] += sp.getWeight() * sp.getNumberOfTrips();
            }
        }

        for (int inc = 1; inc <= 4; inc++) System.out.println("IncomeGroupPersons" + inc + ": " + incomeCountPersons[inc]);
        System.out.println("IncomeNotStatedPersons: " + incomeCountPersons[9]);
        for (int inc = 1; inc <= 4; inc++) System.out.println("IncomeGroupTrips" + inc + ": " + incomeCountTrips[inc]);
        System.out.println("IncomeNotStatedTrips: " + incomeCountTrips[9]);
    }
}

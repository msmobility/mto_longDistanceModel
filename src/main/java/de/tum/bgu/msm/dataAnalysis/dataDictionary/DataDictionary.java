package de.tum.bgu.msm.dataAnalysis.dataDictionary;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Created by Joe on 25/07/2016.
 */
public class DataDictionary {

    static Logger logger = Logger.getLogger(DataDictionary.class);
    private Document dataDictionary;
    HashMap<String, Survey> surveys;

    public DataDictionary(ResourceBundle rb) {
        //get survey from models/mto/input
        surveys = new HashMap<>();
        try {
            //tsrc
            File tsrcLoc = new File(rb.getString("tsrc.data.dir"));
            String[] tsrcYears = rb.getString("tsrc.years").split(",");
            String[] sections = new String[]{"Person", "Trip", "Visit"};
            for (String year : tsrcYears) {
                for (String section : sections) {
                    String key = ("tsrc" + "_" + year + "_" + section).toLowerCase();
                    String fileName = rb.getString("tsrc.data.dir") + File.separator + year + File.separator;
                    fileName += String.format("datadict_tsrc_%s_%s.csv", section, year);
                    File surveyFile = new File(fileName);
                    surveys.put(key, new Survey(surveyFile));

                }
            }

            logger.info("dictionary creation successful");


        } catch (Exception pcex) {
            logger.error("error reading data dictionary", pcex);
            throw new RuntimeException(pcex);

        }
    }

    public Survey getSurvey(String survey, int year, String section) {
        String key = (survey + "_" + year + "_" + section).toLowerCase();
        return surveys.get(key);
    }

}


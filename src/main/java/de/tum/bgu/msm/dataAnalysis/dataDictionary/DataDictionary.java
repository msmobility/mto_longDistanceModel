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

    public DataDictionary(String data_dictionary_location) {
        surveys = new HashMap<>();
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            dataDictionary = dBuilder.parse(new File(data_dictionary_location));

            NodeList surveyNodeList = (NodeList) xPath.evaluate("/data_dictionary/*", dataDictionary, XPathConstants.NODESET);

            for (int i=0; i<surveyNodeList.getLength(); i++) {
                Node surveyNode = surveyNodeList.item(i);
                String surveyName = xPath.evaluate("@name", surveyNode);
                NodeList sectionsNodeList = (NodeList) xPath.evaluate("./*", surveyNode, XPathConstants.NODESET);;
                for (int j=0; j<sectionsNodeList.getLength(); j++) {
                    Node surveySectionNode = sectionsNodeList.item(j);
                    String sectionName = xPath.evaluate("@name", surveySectionNode);
                    String full_name = surveyName + "_" + sectionName;
                    Survey survey = new Survey(surveySectionNode);
                    surveys.put(full_name, survey);

                }


            }
            logger.info("dictionary creation successful");


        } catch (Exception pcex) {
            logger.error("error reading data dictionary", pcex);
            throw new RuntimeException(pcex);

        }
    }

    public Survey getSurvey(String survey, String section) {
        return surveys.get(survey + "_" + section);
    }

}


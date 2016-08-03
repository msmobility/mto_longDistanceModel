package de.tum.bgu.msm.dataAnalysis.dataDictionary;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.HashMap;

import static de.tum.bgu.msm.dataAnalysis.dataDictionary.DataDictionary.logger;

/**
 * Created by Joe on 25/07/2016.
 */
public class Survey {

    private HashMap<String, DictionaryVariable> variables;

    protected Survey(Node surveyNode) throws XPathException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        variables = new HashMap<>();

        NodeList vars = (NodeList) xPath.evaluate("./variables/*", surveyNode, XPathConstants.NODESET);
        for (int i=0; i<vars.getLength(); i++) {
            Node node = vars.item(i);
            String name = node.getAttributes().getNamedItem("name").getNodeValue();
            String question = "";
            int start = 0;
            int end = 0;

            NodeList children = node.getChildNodes();
            HashMap<Integer, String> answers = null;
            for (int j=0; j<children.getLength(); j++) {
                Node item = children.item(j);
                if (item.getNodeName().equals("question")) {
                    question = item.getTextContent();
                } else if (item.getNodeName().equals("start")) {
                    start =  Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equals("end")) {
                    end =  Integer.parseInt(item.getTextContent());
                } else if (item.getNodeName().equals("answers")) {
                    answers = parseAnswersFromVariableNode(item);
                }
            }
            variables.put(name, new DictionaryVariable(name, question, start, end, answers));

        }
    }

    private HashMap<Integer, String> parseAnswersFromVariableNode(Node item) {
        HashMap<Integer, String> answers;NodeList answerNodes = item.getChildNodes();
        answers = new HashMap<>();
        for (int k=0; k<answerNodes.getLength(); k++) {
            Node answerNode = answerNodes.item(k);
            Integer code = null;
            String answer = null;
            NodeList answerComponents = answerNode.getChildNodes();
            for (int l = 0; l < answerComponents.getLength(); l++) {
                Node answerComponent = answerComponents.item(l);
                if (answerComponent.getNodeName().equals("code")) {
                    try {
                        code = Integer.parseInt(answerComponent.getTextContent());
                    } catch (NumberFormatException e) {
                        logger.debug("incorrect format for answer code: " + answerComponent.getTextContent(), e);
                    }
                } else if (answerComponent.getNodeName().equals("answer")) {
                    answer = answerComponent.getTextContent();
                }
            }
            if (code != null) {
                answers.put(code, answer);
            }
        }
        return answers;
    }


    public String read(String recString, String variable) {
        int start = variables.get(variable).getStart();
        int end = variables.get(variable).getEnd();
        return recString.substring(start, end);
    }

    public int readInt(String recString, String variable) {
        int start = variables.get(variable).getStart();
        int end = variables.get(variable).getEnd();
        return convertToInteger(recString.substring(start, end));
    }

    public float readFloat(String recString, String variable) {
        int start = variables.get(variable).getStart();
        int end = variables.get(variable).getEnd();
        return convertToFloat(recString.substring(start, end));
    }

    public double readDouble(String recString, String variable) {
        int start = variables.get(variable).getStart();
        int end = variables.get(variable).getEnd();
        return convertToFloat(recString.substring(start, end));
    }

    public String decodeValue(String variable, int code) {
        return variables.get(variable).decodeAnswer(code);
    }

    public int convertToInteger(String s) {
        // converts s to an integer value, one or two leading spaces are allowed

        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            boolean spacesOnly = true;
            for (int pos = 0; pos < s.length(); pos++) {
                if (!s.substring(pos, pos+1).equals(" ")) spacesOnly = false;
            }
            if (spacesOnly) return -999;
            else {
                logger.fatal("String " + s + " cannot be converted into an integer.");
                return 0;
            }
        }
    }

    public float convertToFloat(String s) {
        // converts s to a float value

        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            if (s.contains(" . ")) return -999;
            boolean spacesOnly = true;
            for (int pos = 0; pos < s.length(); pos++) {
                if (!s.substring(pos, pos+1).equals(" ")) spacesOnly = false;
            }
            if (spacesOnly) return -999;
            else {
                logger.fatal("String " + s + " cannot be converted into a float.");
                return 0;
            }
        }
    }

}

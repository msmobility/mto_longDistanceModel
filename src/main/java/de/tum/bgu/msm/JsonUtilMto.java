package de.tum.bgu.msm;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by carlloga on 12-07-17.
 */
public class JsonUtilMto {

    static Logger logger = Logger.getLogger(JsonUtilMto.class);
    private org.json.simple.parser.JSONParser parser;
    private JSONObject jsonProperties;


    public JsonUtilMto(String jsonFile) {
        try {
            this.parser = new org.json.simple.parser.JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(jsonFile));

            jsonProperties = (JSONObject) obj.get("long_distance_model");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public JSONObject getJsonProperties() {
        return jsonProperties;
    }

    public static boolean getBooleanProp(JSONObject jsonProperties, String key) {

        try {
            return (boolean) getProperty(jsonProperties, key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }


    public static String getStringProp(JSONObject jsonProperties, String key) {

        try {
            return (String) getProperty(jsonProperties,key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }

    public static float getFloatProp(JSONObject jsonProperties, String key) {

        try {
            return (float)(double) getProperty(jsonProperties,key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }


    }

    public static long getLongProp(JSONObject jsonProperties, String key) {

        try {
            return (long) getProperty(jsonProperties,key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }


    }

    public static int getIntProp(JSONObject jsonProperties, String key) {

        try {
            return Math.toIntExact((long) getProperty(jsonProperties,key));
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }

    public static int[] getArrayIntProp(JSONObject jsonProperties, String key) {

        try {

            Object[] propertyArray  = ((JSONArray) getProperty(jsonProperties, key)).toArray();
            //logger.info(propertyArray[0].toString());
            int [] propertyArrayInt = new int[propertyArray.length];
            for (int i=0; i< propertyArray.length; i++){
                propertyArrayInt[i] = Integer.parseInt(propertyArray[i].toString());
                //logger.info(propertyArrayInt[i]);
            }
            return propertyArrayInt;
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }

    public static double[] getArrayDoubleProp(JSONObject jsonProperties, String key) {

        try {

            Object[] propertyArray  = ((JSONArray) getProperty(jsonProperties, key)).toArray();
            //logger.info(propertyArray[0].toString());
            double [] propertyArrayDouble = new double[propertyArray.length];
            for (int i=0; i< propertyArray.length; i++){
                propertyArrayDouble[i] = Float.parseFloat(propertyArray[i].toString());
                //logger.info(propertyArrayInt[i]);
            }
            return propertyArrayDouble;
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }


    public static Object getProperty(JSONObject jsonProperties, String key){

        String[] keys = key.split("[.]");
        JSONObject property = jsonProperties;

        try{
            for (int i = 0; i < keys.length-1; i++) {
                property = (JSONObject) property.get(keys[i]);
            }
            if((property.get(keys[keys.length - 1]))!= null){
                return property.get(keys[keys.length - 1]);
            } else {
                throw new Exception("Property key not found " + key);
            }

        } catch (Exception e){
            throw new RuntimeException("Property key not found: " + key);
        }

    }


}

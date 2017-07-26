package de.tum.bgu.msm;

import de.tum.bgu.msm.longDistance.MtoLongDistance;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.log4j.Logger;
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

    public boolean getBooleanProp(String key) {

        try {
            return (boolean) getProperty(key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }


    public String getStringProp(String key) {

        try {
            return (String) getProperty(key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }

    public float getFloatProp(String key) {

        try {
            return (float)(double) getProperty(key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }


    }

    public long getLongProp(String key) {

        try {
            return (long) getProperty(key);
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }


    }

    public int getIntProp(String key) {

        try {
            return Math.toIntExact((long) getProperty(key));
        } catch (Exception e) {
            throw new RuntimeException("Property key not found or invalid: " + key);
            //I guess this is impossible for json files
        }

    }


    public Object getProperty(String key){

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

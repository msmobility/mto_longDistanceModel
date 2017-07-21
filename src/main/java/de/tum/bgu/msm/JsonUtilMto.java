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

        String[] keys = key.split("[.]");



        JSONObject property = jsonProperties;

        try {
            for (int i = 0; i < keys.length-1; i++) {
                property = (JSONObject) property.get(keys[i]);
            }
            return (boolean) property.get(keys[keys.length - 1]);

        } catch (Exception e) {
            throw new RuntimeException("Property key not found: " + key);
        }

    }


    public String getStringProp(String key) {

        String[] keys = key.split("[.]");

        JSONObject property = jsonProperties;

        try{
            for (int i = 0; i < keys.length-1; i++) {
                property = (JSONObject) property.get(keys[i]);
            }

            return (String) property.get(keys[keys.length - 1]);
        } catch (Exception e){
            throw new RuntimeException("Property key not found: " + key);
        }

    }

    public float getFloatProp(String key) {

        String[] keys = key.split("[.]");

        JSONObject property = jsonProperties;

        try{
            for (int i = 0; i < keys.length-1; i++) {
                property = (JSONObject) property.get(keys[i]);
            }

            return (float)(double) property.get(keys[keys.length - 1]);

        } catch (Exception e){
            throw new RuntimeException("Property key not found: " + key);
        }


    }

    public long getLongProp(String key) {

        String[] keys = key.split("[.]");

        JSONObject property = jsonProperties;

        try{
            for (int i = 0; i < keys.length-1; i++) {
                property = (JSONObject) property.get(keys[i]);
            }

            return (long) property.get(keys[keys.length - 1]);
        } catch (Exception e){
            throw new RuntimeException("Property key not found: " + key);
        }


    }

    public int getIntProp(String key) {

        String[] keys = key.split("[.]");



        JSONObject property = jsonProperties;

        try{
            for (int i = 0; i < keys.length-1; i++) {
                property = (JSONObject) property.get(keys[i]);
            }

            return Math.toIntExact((long) property.get(keys[keys.length - 1]));

        } catch (ArithmeticException e){
            throw new RuntimeException("Cannot convert the property to an integer: " + key);
        } catch (Exception e){
            throw new RuntimeException("Property key not found: " + key);
        }

    }


}

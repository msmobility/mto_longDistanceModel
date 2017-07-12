package de.tum.bgu.msm;

import jdk.nashorn.internal.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by carlloga on 12-07-17.
 */
public class JsonUtilMto {

    private org.json.simple.parser.JSONParser parser;
    private JSONObject jsonProperties;


    public JsonUtilMto (String jsonFile){
        try {
        this.parser = new org.json.simple.parser.JSONParser();
        JSONObject obj = (JSONObject)parser.parse(new FileReader(jsonFile));

        jsonProperties = (JSONObject) obj.get("long_distance_model");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public boolean bool(String key){

            String[] keys = key.split("[.]");

            JSONObject property = jsonProperties;

            for (int i = 0; i < keys.length - 1; i++){
                property = (JSONObject) property.get(keys[i]);
            }

            return (boolean) property.get(keys[keys.length-1]);

    }


    public String stri(String key){

        String[] keys = key.split("[.]");

        JSONObject property = jsonProperties;

        for (int i = 0; i < keys.length - 1; i++){
            property = (JSONObject) property.get(keys[i]);
        }

        return  (String) property.get(keys[keys.length-1]);
    }

    public double dble(String key){

        String[] keys = key.split("[.]");

        JSONObject property = jsonProperties;

        for (int i = 0; i < keys.length - 1; i++){
            property = (JSONObject) property.get(keys[i]);
        }

        return  (double) property.get(keys[keys.length-1]);
    }

    public long lon(String key){

        String[] keys = key.split("[.]");

        JSONObject property = jsonProperties;

        for (int i = 0; i < keys.length - 1; i++){
            property = (JSONObject) property.get(keys[i]);
        }

        return  (long) property.get(keys[keys.length-1]);
    }


}

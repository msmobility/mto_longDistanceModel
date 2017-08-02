package de.tum.bgu.msm.longDistance;

import org.json.simple.JSONObject;

/**
 * Created by carlloga on 8/1/2017.
 * This interface is to be used in all the model components
 */
public interface ModelComponent {

    void setup(JSONObject submodelConfiguration, String inputFolder, String outputFolder);

    void load(DataSet dataSet);

    void run(DataSet dataSet, int nThreads);

}

package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import org.json.simple.JSONObject;

/**
 * Created by carlloga on 8/1/2017.
 */
public interface ModelComponent {

    void setup(JSONObject submodelConfiguration);

    void load(DataSet dataSet);

    void run(DataSet dataSet, int nThreads);

}

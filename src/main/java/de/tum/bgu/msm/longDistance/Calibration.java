package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;

import java.util.ArrayList;

/**
 * Created by carlloga on 12-07-17.
 */
public class Calibration {

    public Calibration(){



    }

    public double[][] getAverageTripDistances (ArrayList<LongDistanceTrip> allTrips){

        double[][] averageDistances = new double[3][3];
        double[][] counts = new double[3][3];

        for (LongDistanceTrip t : allTrips){
            if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)){
                if (!t.isLongDistanceInternational()){
                    //domestic from Ontario - row 0
                    averageDistances[0][t.getLongDistanceTripPurpose()]= t.getTravelDistanceLevel2();
                    counts[0][t.getLongDistanceTripPurpose()]++;
                } else if (t.getDestZoneType().equals(ZoneType.EXTUS)){
                    //international from ontario to us - row 1
                    averageDistances[1][t.getLongDistanceTripPurpose()]= t.getTravelDistanceLevel2();
                    counts[1][t.getLongDistanceTripPurpose()]++;
                }
            } else if (t.getDestZoneType().equals(ZoneType.EXTUS)){
                //international from US to ontario + row 2
                averageDistances[2][t.getLongDistanceTripPurpose()]= t.getTravelDistanceLevel2();
                counts[2][t.getLongDistanceTripPurpose()]++;

            }

            for (int i = 0; i< 3; i++){
                for (int j = 0; j< 3; j++){
                    if (counts[i][j]>0) averageDistances[i][j]= averageDistances[i][j] / counts[i][j];
                }
            }
        }

        return averageDistances;

    }

    public double[][] calculateCalibrationMatrix(ArrayList<LongDistanceTrip> allTrips){

        double[][] averageDistances = getAverageTripDistances(allTrips);

        double[][] calibrationMatrix = new double[3][3];

        calibrationMatrix[0][0] = averageDistances[0][0] /132.7427; //domestic visit
        calibrationMatrix[0][1] = averageDistances[0][1] /175.3925; //domestic business
        calibrationMatrix[0][2] = averageDistances[0][2] /134.314 ; //domestic leisure
        calibrationMatrix[1][0] = averageDistances[1][0];//to us visit
        calibrationMatrix[1][1] = averageDistances[1][1];//to us business
        calibrationMatrix[1][2] = averageDistances[1][2];//to us leisure
        calibrationMatrix[2][0] = averageDistances[2][0];//from us purp visit
        calibrationMatrix[2][1] = averageDistances[2][1];//from us purp business
        calibrationMatrix[2][2] = averageDistances[2][2];//from us leisure


        return calibrationMatrix;

    }

}

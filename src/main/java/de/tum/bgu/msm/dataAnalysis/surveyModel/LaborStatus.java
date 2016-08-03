package de.tum.bgu.msm.dataAnalysis.surveyModel;

/**
 * Created by Joe on 26/07/2016.
 */
public enum LaborStatus {
    EMPLOYED, UNEMPLOYED;

    public static LaborStatus getStatus(int laborStat) {
        if (laborStat == 1) return LaborStatus.EMPLOYED;
        else return LaborStatus.UNEMPLOYED;
    }
}

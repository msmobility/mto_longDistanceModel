package de.tum.bgu.msm.dataAnalysis.surveyModel;

/**
 * Created by Joe on 26/07/2016.
 */
public enum Gender {
    MALE, FEMALE;

    public static Gender getGender(int gender) {
        if (gender == 1) return Gender.MALE;
        else return Gender.FEMALE;
    }
}

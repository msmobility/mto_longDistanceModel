package de.tum.bgu.msm.dataAnalysis.surveyModel;

/**
 * Created by Joe on 27/07/2016.
 */
public class SurveyVisit {
    public final int visitId;
    public final int province;
    public final int cd;
    public final int cma;
    public final int nights;

    public SurveyVisit(int visitId, int province, int cd, int cmarea, int nights) {
        this.visitId = visitId;
        this.province = province;
        this.cd = cd;
        this.cma = cmarea;
        this.nights = nights;
    }

    public boolean stopInProvince(int provice) {
        return this.province == provice;
    }
}

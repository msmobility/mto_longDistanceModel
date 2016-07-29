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

    public int assignRandomCma(SurveyVisit sv) {
        //get possible cma's for census district
        return 0;
        //get cma's and weights (population?) for each
    }

    public int getCd() {
        return cd;
    }

    public int getCma() {
        return cma;
    }

    //4 digit code of combined province and census division is needed for boundary files
    public int getUniqueCD() {
        return province * 100 + cd;
    }

    public boolean cdStated() {
        return getCd() != 999;
    }
}

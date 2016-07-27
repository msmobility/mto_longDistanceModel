package de.tum.bgu.msm.dataAnalysis.surveyModel;

/**
 * Created by Joe on 27/07/2016.
 */
public class SurveyVisit {
    public final int visitId;
    public final int cma;
    public final int nights;

    public SurveyVisit(int visitId, int cmarea, int nights) {
        this.visitId = visitId;
        this.cma = cmarea;
        this.nights = nights;
    }
}

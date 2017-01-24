package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.Survey;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;

/**
 * Created by Joe on 27/07/2016.
 */
public class SurveyVisit {
    public static final Logger logger = Logger.getLogger(SurveyVisit.class);

    public final SurveyTour tour;

    public final int visitId;
    public final int province;
    public final int cd;
    public final int cma;
    public final int nights;
    public final boolean visitAirport;
    public final int visitIdentification;
    public final String airport;

    public SurveyVisit(Survey survey, SurveyTour tour, String recString) {

        this.visitId = survey.readInt(recString, "VISITID");  // ascii position in file: 016-017
        this.province = survey.readInt(recString, "VPROV");
        this.cd = survey.readInt(recString, "VCD2");
        this.cma = survey.readInt(recString, "VCMA2");  // ascii position in file: 023-026
        this.nights = survey.readInt(recString, "AC_Q04");  // ascii position in file: 027-029
        int airFlag = survey.readInt(recString, "AIRFLAG");  // ascii position in file: 027-029

        this.airport = survey.read(recString, "AIRCODE2");  // ascii position in file: 027-029
        this.visitIdentification = survey.readInt(recString, "VISRECFL");
        this.tour = tour;


        this.visitAirport = airFlag == 1;
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


    public Coordinate cdToCoords(MtoSurveyData data) {
        //logger.info(cma);
        try {
            TableDataSet cdList = data.getCensusDivisionList();
            float latitude = cdList.getIndexedValueAt(getUniqueCD(), "LATITUDE");
            float longitude = cdList.getIndexedValueAt(getUniqueCD(), "LONGITUDE");
            return new Coordinate(longitude, latitude);
        } catch (ArrayIndexOutOfBoundsException e) {
            //logger.warn(String.format("cd %d not found in record", getUniqueCD()));
            return new Coordinate(-90, 60);
        }

    }

    @Override
    public String toString() {
        return "SurveyVisit{" +
                "visitId=" + visitId +
                ", province=" + province +
                ", cd=" + cd +
                ", nights=" + nights +
                ", visitIdentification=" + visitIdentification +
                ", airport='" + airport + '\'' +
                '}';
    }
}

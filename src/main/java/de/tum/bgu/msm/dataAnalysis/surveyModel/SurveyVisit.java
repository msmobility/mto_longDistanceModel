package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;

/**
 * Created by Joe on 27/07/2016.
 */
public class SurveyVisit {
    public static final Logger logger = Logger.getLogger(SurveyVisit.class);
    public final int visitId;
    public final int province;
    public final int cd;
    public final int cma;
    public final int nights;
    public final boolean visitAirport;

    public SurveyVisit(int visitId, int province, int cd, int cmarea, int nights, int airFlag) {
        this.visitId = visitId;
        this.province = province;
        this.cd = cd;
        this.cma = cmarea;
        this.nights = nights;

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


    public Coordinate cdToCoords(mtoSurveyData data) {
        //logger.info(cma);
        try {
            TableDataSet cdList = data.getCensusDivisionList();
            float latitude = cdList.getIndexedValueAt(getUniqueCD(), "LATITUDE");
            float longitude = cdList.getIndexedValueAt(getUniqueCD(), "LONGITUDE");
            return new Coordinate(longitude, latitude);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.warn(String.format("cd %d not found in record", getUniqueCD()));
            return new Coordinate(-90, 60);
        }

    }

    public int distanceFromCd(mtoSurveyData data, int origCD) {
        //calculate origin location
        TableDataSet cdList = data.getCensusDivisionList();
        float latitude = cdList.getIndexedValueAt(origCD, "LATITUDE");
        float longitude = cdList.getIndexedValueAt(origCD, "LONGITUDE");
        Coordinate origin_coord = new Coordinate(longitude, latitude);

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(JTSFactoryFinder.EMPTY_HINTS);

        LineString ls = geometryFactory.createLineString(new Coordinate[]{origin_coord, cdToCoords(data)});

        return (int) util.getTourDistance(ls) / 1000;
    }
}

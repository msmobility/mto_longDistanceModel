package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.Survey;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to hold tour object of the TSRC survey
 *
 * @author Rolf Moeckel
 *         Created on 17 Mar. 2016 in Munich
 **/

public class surveyTour implements Serializable {

    static Logger logger = Logger.getLogger(surveyTour.class);
    private final int distance;
    private int refYear;
    private long pumfId;
    private int origProvince;
    private int destProvince;
    private int mainMode;
    private int homeCma;
    private int tripPurp;
    private int numberNights;
    private double weight;
    private ArrayList<SurveyVisit> tourStops;
    private int tripId;
    private surveyPerson person;
    private int origCD;
    private int destCD;
    private LineString tourGeometry = null;






    public surveyTour(Survey survey, surveyPerson person, String recString) {
        this.person = person;
        this.refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
        long origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
        this.pumfId = origPumfId * 100 + refYear % 100;
        this.tripId = survey.readInt(recString, "TRIPID");  // ascii position in file: 014-015
        this.origProvince = survey.readInt(recString, "ORCPROVT");  // ascii position in file: 017-018
        this.origCD = survey.readInt(recString, "ORCCDT2");  // ascii position in file: 017-018
        this.destProvince = survey.readInt(recString, "MDDPLFL");  // ascii position in file: 026-027
        this.destCD = survey.readInt(recString, "MDCCD");  // ascii position in file: 026-027
        this.mainMode = survey.readInt(recString, "TMDTYPE2");  // ascii position in file: 080-081
        this.homeCma = survey.readInt(recString, "ORCCMAT2");  // ascii position in file: 022-025
        this.tripPurp = survey.readInt(recString, "MRDTRIP3");  // ascii position in file: 073-074
        this.numberNights = survey.readInt(recString, "CANNITE");  // ascii position in file: 121-123
        this.weight = survey.readDouble(recString, "WTTP");
        this.distance = survey.readInt(recString, "DIST2");


        tourStops = new ArrayList<>();
    }


    public void addTripDestinations(SurveyVisit sv) {
        tourStops.add(sv);

    }

    public int getOrigProvince() {
        return origProvince;
    }

    public int getDestProvince() {
        return destProvince;
    }

    public int getMainMode() {
        return mainMode;
    }

    public int getHomeCma() {
        return homeCma;
    }

    public int getTripPurp() {
        return tripPurp;
    }

    public int getNumberNights() {
        return numberNights;
    }

    public int getNumberOfStop() {
        return tourStops.size();
    }

    public int getTripId() {
        return tripId;
    }

    public ArrayList<SurveyVisit> getStops() {
        return tourStops;
    }

    public long getDistinctNumRegions() {
        return getStops().stream().filter(v -> v.cma != homeCma).map(v -> v.cma).distinct().count();
    }

    public boolean isReturnTrip() {
        return homeCma == tourStops.get(tourStops.size() - 1).cma;
    }

    public void sortVisits() {
        tourStops.sort((o1, o2) -> Integer.compare(o2.visitId, o1.visitId)); //reverse order
    }

    public String getUniqueId() {
        return Long.toString(getPerson().getPumfId()) + Integer.toString(getTripId());
    }

    public surveyPerson getPerson() {
        return person;
    }

    public int getOrigCD() {
        return origCD;
    }

    public int getDestCD() { return destCD; }

    //4 digit code of combined province and census division is needed for boundary files
    public int getUniqueOrigCD() {
        return getOrigProvince() * 100 + getOrigCD();
    }

    public double getWeight() {
        return weight;
    }

    public int getDistance() {
        return distance;
    }

    public int calculateFurthestDistance(mtoSurveyData data) {
        return tourStops.stream().mapToInt(sv -> sv.distanceFromCd(data, getUniqueOrigCD())).max().getAsInt();
    }

    public LineString generateTourLineString(mtoSurveyData data) {
        //only greate the geometry once, as it's expensive to do. Can't be created at start as we need mtoSurveyData
        if (tourGeometry == null) {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(JTSFactoryFinder.EMPTY_HINTS);
            Coordinate[] coordinates = tourStops.stream()
                    .map(sv -> sv.cdToCoords(data))
                    .toArray(Coordinate[]::new);
            tourGeometry = geometryFactory.createLineString(coordinates);
        }
        return tourGeometry;
    }


    public String getMainModeStr() {
        switch (mainMode) {
            case 1:
                return "Auto";
            case 2:
                return "Air";
            case 3:
                return "Auto";
            case 4:
                return "Bus";
            case 5:
                return "Train";
            case 6:
                return "Sea";
            case 7:
                return "Sea";
        }
        return "Other";
    }

    public long getPumfId() {
        return pumfId;
    }

    @Override
    public String toString() {
        return "surveyTour{" +
                "\n\trefYear=" + refYear +
                "\n\tpumfId=" + pumfId +
                ", tripId=" + tripId +
                ", tourStops=" + tourStops.size() +
                ", origProvince=" + origProvince +
                ", destProvince=" + destProvince +
                ", origCD=" + origCD +
                ", destCD=" + destCD +
                ", mainMode=" + mainMode +
                ", tripPurp=" + tripPurp +
                ", numberNights=" + numberNights +
                ", weight=" + weight +
                "\n\tgeometry=" + tourGeometry +
                "\n}";
    }
}

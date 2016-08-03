package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
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
 * Created on 17 Mar. 2016 in Munich
 *
 **/

public class surveyTour implements Serializable {

    static Logger logger = Logger.getLogger(surveyTour.class);

    private int origProvince;
    private int destProvince;
    private int mainMode;
    private int homeCma;
    private int tripPurp;
    private final int distance;
    private int numberNights;
    private int numIdentical;
    private double weight;
    private ArrayList<SurveyVisit> tourStops;
    private int tripId;
    private surveyPerson person;
    private int origCD;
    private LineString tourGeometry = null;


    protected surveyTour(int tripId, surveyPerson person, int origProvince, int origCD, int destProvince, int mainMode, int homeCma,
               int tripPurp, int distance, int numberNights, int numIdentical, double weight) {
        // constructor of new survey tour

        this.person = person;
        this.tripId = tripId;
        this.origProvince = origProvince;
        this.origCD = origCD;
        this.destProvince = destProvince;
        this.mainMode = mainMode;
        this.homeCma = homeCma;
        this.tripPurp = tripPurp;
        this.distance = distance;
        this.numberNights = numberNights;
        this.numIdentical = numIdentical;
        this.weight = weight;
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

    public int getNumIdentical() {
        return numIdentical;
    }

    public int getNumberOfStop () {
        return tourStops.size();
    }

    public int getTripId() {
        return tripId;
    }

    public ArrayList<SurveyVisit> getStops() {
        return tourStops;
    }

    public long getDistinctNumRegions() {
        return  getStops().stream().filter(v -> v.cma != homeCma).map(v -> v.cma).distinct().count();
    }

    public boolean isReturnTrip() {
        return homeCma == tourStops.get(tourStops.size()-1).cma;
    }

    public SurveyVisit[] getTourStops() { //TODO: include homeCma
        SurveyVisit[] stops = new SurveyVisit[tourStops.size() + 1];
        stops[0] = new SurveyVisit(-1, getOrigProvince(), getOrigCD(), getHomeCma(), 0, -1, 0);
        for (int i = 0; i < getStops().size(); i++) {
            stops[i + 1] = getStops().get(i);
        }
        return stops;
    }

    public void sortVisits() {
        tourStops.sort((o1, o2) -> Integer.compare(o1.visitId,o2.visitId));
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

    //4 digit code of combined province and census division is needed for boundary files
    public int getUniqueOrigCD() {
        return getOrigProvince() * 100 + getOrigCD();
    }

    public double getWeight() { return weight; }

    public int getDistance() { return distance; }

    public int calculateFurthestDistance(mtoSurveyData data) {
        return tourStops.stream().mapToInt(sv -> sv.distanceFromCd(data, getUniqueOrigCD())).max().getAsInt();
    }

    public LineString generateTourLineString(mtoSurveyData data) {
        //only greate the geometry once, as it's expensive to do. Can't be created at start as we need mtoSurveyData
        if (tourGeometry == null) {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(JTSFactoryFinder.EMPTY_HINTS);
            Coordinate[] coordinates = Arrays.stream(getTourStops()).map(sv -> sv.cdToCoords(data)).toArray(Coordinate[]::new);
            tourGeometry = geometryFactory.createLineString(coordinates);
        }
        return tourGeometry;
    }


    public String getMainModeStr() {
        switch (mainMode) {
            case 1: return "Auto";
            case 2: return "Air";
            case 3: return "Auto";
            case 4: return "Bus";
            case 5: return "Train";
            case 6: return "Sea";
            case 7: return "Sea";
        }
        return "Other";
    }
}

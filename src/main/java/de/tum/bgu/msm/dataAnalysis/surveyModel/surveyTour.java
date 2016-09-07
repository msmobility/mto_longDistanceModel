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

import static java.lang.System.exit;

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
    private int origCD;
    private int origCma;
    private int destProvince;
    private int destCD;
    private int destCma;
    private int mainMode;
    private int tripPurp;
    private int numberNights;
    private double weight;
    private ArrayList<SurveyVisit> tourStops;
    private int tripId;
    private surveyPerson person;
    private LineString tourGeometry = null;

    private int tripType;
    private final int partySize;
    private final int numHhMembersOnTrip;
    private final int numHhAdultsOnTrip;
    private final int numHhKidsOnTrip;
    private final int numIdenticalTrips;
    private final int quarter;
    private double hHWeight;


    public surveyTour(Survey survey, surveyPerson person, String recString) {
        this.person = person;
        this.refYear = survey.readInt(recString, "REFYEAR");  // ascii position in file: 001-004
        long origPumfId = survey.readInt(recString, "PUMFID");  // ascii position in file: 007-013
        this.pumfId = origPumfId * 100 + refYear % 100;
        this.tripId = survey.readInt(recString, "TRIPID");  // ascii position in file: 014-015
        this.quarter = survey.readInt(recString, "QUARTER");  // ascii position in file: 014-015
        this.origProvince = survey.readInt(recString, "ORCPROVT");  // ascii position in file: 017-018
        this.origCD = survey.readInt(recString, "ORCCDT2");  // ascii position in file: 017-018
        this.origCma = survey.readInt(recString, "ORCCMAT2");  // ascii position in file: 017-018
        this.destProvince = survey.readInt(recString, "MDDPLFL");  // ascii position in file: 026-027
        this.destCD = survey.readInt(recString, "MDCCD");  // ascii position in file: 026-027
        this.destCma = survey.readInt(recString, "MDCCMA2");  // ascii position in file: 022-025
        this.mainMode = survey.readInt(recString, "TMDTYPE2");  // ascii position in file: 080-081
        this.tripPurp = survey.readInt(recString, "MRDTRIP3");  // ascii position in file: 073-074
        this.numberNights = survey.readInt(recString, "CANNITE");  // ascii position in file: 121-123
        this.hHWeight = survey.readDouble(recString, "WTEP");
        this.weight = survey.readDouble(recString, "WTTP");
        this.distance = survey.readInt(recString, "DIST2");
        this.tripType = survey.readInt(recString, "TRIPTYPE");
        this.partySize = survey.readInt(recString, "TP_D01");
        this.numHhMembersOnTrip = survey.readInt(recString, "T_G0802");
        this.numHhAdultsOnTrip = survey.readInt(recString, "TR_G08");
        this.numHhKidsOnTrip = survey.readInt(recString, "TP_G02");
        this.numIdenticalTrips = survey.readInt(recString, "TR_D11");


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
        return origCma;
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

    public void sortVisits() {
        tourStops.sort((o1, o2) -> Integer.compare(o2.visitId, o1.visitId)); //reverse order
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

    public int getUniqueDestCD() {
        return destProvince * 100 + destCD;
    }

    public int getDestCma() {
        return destCma;
    }

    public int getTripType() {
        return tripType;
    }

    public LineString generateTourLineString(mtoSurveyData data) {
        //only greate the geometry once, as it's expensive to do. Can't be created at start as we need mtoSurveyData
        if (tourGeometry == null) {
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(JTSFactoryFinder.EMPTY_HINTS);
            Coordinate[] coordinates = tourStops.stream()
                    .filter( sv -> !sv.visitAirport) //exclude airport visits for the moment, as they are not in order
                    .map(sv -> sv.cdToCoords(data))
                    .toArray(Coordinate[]::new);
            tourGeometry = geometryFactory.createLineString(coordinates);
        }
        return tourGeometry;
    }


    public int getTourDistance() {
        //this could also be done using a distance matrix, or even the skim matrix
        double totalDistance = 0;
        try {
            Coordinate c0 = tourGeometry.getCoordinateN(0);
             for (int i=1; i<tourGeometry.getNumPoints(); i++) {
                 Coordinate c1 = tourGeometry.getCoordinateN(i);
                 //flip lat and long
                 Coordinate c0flipped = new Coordinate(c0.y, c0.x);
                 Coordinate c1flipped = new Coordinate(c1.y, c1.x);

                 totalDistance += JTS.orthodromicDistance(c0flipped, c1flipped, CRS.decode("EPSG:4269"));
                     c0 = c1;
             }
        } catch (TransformException | FactoryException | IllegalArgumentException e) {
            logger.error(tourGeometry, e);
            exit(1);
        }
        return (int) (totalDistance / 1000);

    }

    public int getOrigCma() {
        return origCma;
    }

    public int getPartySize() {
        return partySize;
    }

    public int getNumHhMembersOnTrip() {
        return numHhMembersOnTrip;
    }

    public int getNumHhAdultsOnTrip() {
        return numHhAdultsOnTrip;
    }

    public int getNumHhKidsOnTrip() {
        return numHhKidsOnTrip;
    }

    public int getNumIdenticalTrips() {
        return numIdenticalTrips;
    }

    public LineString getLineString() {
        return tourGeometry;
    }

    public int getQuarter() {
        return quarter;
    }

    public double getHHWeight() {
        return hHWeight;
    }
}

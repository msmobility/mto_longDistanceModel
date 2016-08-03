package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.DataDictionary;
import de.tum.bgu.msm.util;
import javafx.scene.control.Tab;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;


import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Ontario Provincial Model
 * Class to hold data
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 14 December 2015
 * Version 1
 *
 */
public class mtoSurveyData {
    private Logger logger = Logger.getLogger(this.getClass());
    private ResourceBundle rb;
    private String workDirectory;
    private TableDataSet provinceList;
    private TableDataSet mainModeList;
    private TableDataSet cmaList;
    private TableDataSet censusDivisionList;
    private TableDataSet tripPurposes;

    private DataDictionary dataDictionary;
    private HashMap<Integer, surveyPerson> personMap;
    private int[] sortedCensusDivisions;
    private int[] sortedCMAList;

    mtoSurveyData(ResourceBundle rb, HashMap<Integer, surveyPerson> personMap, DataDictionary dd) {
        this.dataDictionary = dd;
        this.personMap = personMap;

        provinceList = util.readCSVfile(rb.getString("province.list"));
        provinceList.buildIndex(provinceList.getColumnPosition("Code"));

        mainModeList = util.readCSVfile(rb.getString("main.mode.list"));
        mainModeList.buildIndex(mainModeList.getColumnPosition("Code"));

        cmaList = util.readCSVfile(rb.getString("cma.list"));
        cmaList.buildIndex(cmaList.getColumnPosition("CMAUID"));

        censusDivisionList = util.readCSVfile(rb.getString("cd.list"));
        censusDivisionList.buildIndex(censusDivisionList.getColumnPosition("CDUID"));

        tripPurposes = util.readCSVfile(rb.getString("trip.purp"));
        tripPurposes.buildIndex(tripPurposes.getColumnPosition("Code"));

        //sorted cma and cd lists for searching cds
        int[] cduidCol = censusDivisionList.getColumnAsInt("CDUID");
        sortedCensusDivisions = Arrays.copyOf(cduidCol, cduidCol.length);
        Arrays.sort(sortedCensusDivisions);

        int[] cmauidCol = censusDivisionList.getColumnAsInt("CDUID");
        sortedCMAList = Arrays.copyOf(cmauidCol, cmauidCol.length);
        Arrays.sort(sortedCMAList);

    }


    public TableDataSet getProvinceList() {
        return provinceList;
    }

    public TableDataSet getMainModeList() {
        return mainModeList;
    }

    public TableDataSet getCmaList() {
        return cmaList;
    }

    public TableDataSet getTripPurposes() {
        return tripPurposes;
    }

    public surveyPerson getPersonFromId(int id) {
        return personMap.get(id);
    }

    public int getPersonCount() {
        return personMap.size();
    }

    public Collection<surveyPerson> getPersons() {
        return personMap.values();
    }

    public boolean validCma(int cma) {
        return Arrays.binarySearch(sortedCMAList, cma) > -1;
    }

    public boolean validCd(int cd) {
        boolean result = Arrays.binarySearch(sortedCensusDivisions, cd) > -1;
        return result;
    }

    public TableDataSet getCensusDivisionList() {
        return censusDivisionList;
    }

}




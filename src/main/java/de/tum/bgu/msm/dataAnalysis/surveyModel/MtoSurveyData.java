package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.DataDictionary;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;


import java.util.*;

/**
 *
 * Ontario Provincial Model
 * Class to hold data
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 14 December 2015
 * Version 1
 *
 */
public class MtoSurveyData {
    private Logger logger = Logger.getLogger(this.getClass());
    private ResourceBundle rb;
    private String workDirectory;
    private TableDataSet provinceList;
    private TableDataSet mainModeList;
    private TableDataSet cmaList;
    private TableDataSet censusDivisionList;
    private TableDataSet tripPurposes;

    private DataDictionary dataDictionary;
    private HashMap<Long, SurveyPerson> personMap;
    private int[] sortedCensusDivisions;
    private int[] sortedCMAList;

    MtoSurveyData(ResourceBundle rb, HashMap<Long, SurveyPerson> personMap, DataDictionary dd) {
        this.dataDictionary = dd;
        this.personMap = personMap;

        provinceList = Util.readCSVfile(rb.getString("province.list"));
        provinceList.buildIndex(provinceList.getColumnPosition("Code"));

        mainModeList = Util.readCSVfile(rb.getString("main.mode.list"));
        mainModeList.buildIndex(mainModeList.getColumnPosition("Code"));

        cmaList = Util.readCSVfile(rb.getString("cma.list"));
        cmaList.buildIndex(cmaList.getColumnPosition("CMAUID"));

        censusDivisionList = Util.readCSVfile(rb.getString("cd.list"));
        censusDivisionList.buildIndex(censusDivisionList.getColumnPosition("CDUID"));

        tripPurposes = Util.readCSVfile(rb.getString("trip.purp"));
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

    public SurveyPerson getPersonFromId(long id) {
        return personMap.get(id);
    }

    public int getPersonCount() {
        return personMap.size();
    }

    public Collection<SurveyPerson> getPersons() {
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




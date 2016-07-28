package de.tum.bgu.msm.dataAnalysis.surveyModel;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.dataAnalysis.dataDictionary.DataDictionary;
import org.apache.log4j.Logger;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.ResourceBundle;

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
    private TableDataSet tripPurposes;

    private DataDictionary dataDictionary;
    private HashMap<Integer, surveyPerson> personMap;

    mtoSurveyData(ResourceBundle rb, HashMap<Integer, surveyPerson> personMap, DataDictionary dd, TableDataSet provinceList,
                  TableDataSet mainModeList, TableDataSet cmaList, TableDataSet tripPurposes) {
        this.provinceList = provinceList;
        this.mainModeList = mainModeList;
        this.cmaList = cmaList;
        this.tripPurposes = tripPurposes;
        this.dataDictionary = dd;
        this.personMap = personMap;

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
        return Arrays.binarySearch(getCmaList().getColumnAsInt("CMAUID"), cma) > -1;
    }
}




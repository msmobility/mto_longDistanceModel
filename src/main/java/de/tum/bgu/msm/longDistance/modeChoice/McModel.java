package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Created by carlloga on 8/2/2017.
 */
public class McModel implements ModelComponent {

    static Logger logger = Logger.getLogger(McModel.class);

    //private DomesticModeChoice mcDomesticModel;
    private OntarianDomesticMC ontarianDomesticMC;
    private CanadianDomesticMC canadianDomesticMC;
    private IntModeChoice intModeChoice;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        //mcDomesticModel = new DomesticModeChoice(prop);
        ontarianDomesticMC = new OntarianDomesticMC(prop);
        canadianDomesticMC = new CanadianDomesticMC(prop);
        intModeChoice = new IntModeChoice(prop);
    }

    @Override
    public void load(DataSet dataSet) {
        //store the models
        //dataSet.setMcDomestic(mcDomesticModel);
        dataSet.setCanadianDomesticMC(canadianDomesticMC);
        dataSet.setOntarianDomesticMC(ontarianDomesticMC);
        dataSet.setMcInt(intModeChoice);

        //load submodels
//        mcDomesticModel.loadDomesticModeChoice(dataSet);
        ontarianDomesticMC.load(dataSet);
        canadianDomesticMC.load(dataSet);
        intModeChoice.loadIntModeChoice(dataSet);


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runModeChoice(dataSet.getAllTrips());
    }

    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!t.isInternational()) {
                if(t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)){
                    //domestic mode choice for synthetic persons in Ontario
                    int mode = ontarianDomesticMC.selectMode(t);
                    t.setMode(mode);
                    t.setTravelTimeLevel2(ontarianDomesticMC.getDomesticModalTravelTime(t));

                } else {
                    //domestic mode choice for synthetic persons in rest of Canada
                    int mode = canadianDomesticMC.selectMode(t);
                    t.setMode(mode);
                    t.setTravelTimeLevel2(canadianDomesticMC.getDomesticModalTravelTime(t));
                }

                // international mode choice
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getOrigZone().getZoneType().equals(ZoneType.EXTCANADA)) {
                //residents
                if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from Canada to US
                    int mode = intModeChoice.selectMode(t);
                    t.setMode(mode);
                } else {
                    //international from Canada to OS
                    t.setMode(1); //always by air
                }
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));
                //visitors
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS)) {
                //international visitors from US
                int mode = intModeChoice.selectMode(t);
                t.setMode(mode);
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));

            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTOVERSEAS)) {
                //international visitors from US
                t.setMode(1); //always by air
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));
            }

        });
    }


}

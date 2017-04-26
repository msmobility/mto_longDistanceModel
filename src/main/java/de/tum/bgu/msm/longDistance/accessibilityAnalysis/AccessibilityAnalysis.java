package de.tum.bgu.msm.longDistance.accessibilityAnalysis;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.tripGeneration.DomesticTripGeneration;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/26/2017.
 */
public class AccessibilityAnalysis {
    private ResourceBundle rb;
    private MtoLongDistData mtoLongDistData;
    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    private List<String> fromZones;
    private List<String> toZones;
    private ArrayList<Zone> zoneList;
    private float alphaAuto;
    private float betaAuto;

    public AccessibilityAnalysis (ResourceBundle rb, MtoLongDistData mtoLongDistData){
        this.rb = rb;
        this.mtoLongDistData= mtoLongDistData;


        zoneList = mtoLongDistData.getZoneList();

        mtoLongDistData.readSkim("auto");
        //mtoLongDistData.readSkim("transit");
        //input parameters for accessibility calculations from mto properties
        alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
        betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");
        fromZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "orig.zone.type", ",");
        toZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "dest.zone.type", ",");

    }

    public void calculateAccessibilityForAnalysis(){

        mtoLongDistData.calculateAccessibility(zoneList, fromZones, toZones, alphaAuto, betaAuto);
        mtoLongDistData.writeOutAccessibilities(zoneList);
        logger.info("Accessibility analysis completed using alpha = " + alphaAuto + " and beta = " + betaAuto);

    }

}

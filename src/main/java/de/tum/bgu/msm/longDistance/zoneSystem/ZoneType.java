package de.tum.bgu.msm.longDistance.zoneSystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by carlloga on 8/10/2016.
 */
public enum ZoneType {
    ONTARIO, EXTCANADA, EXTUS, EXTOVERSEAS;

    public static ZoneType getZoneType(String stringZoneType) {
        if (stringZoneType.equals("ONTARIO")) return ZoneType.ONTARIO;
        else if (stringZoneType.equals("EXTCANADA")) return ZoneType.EXTCANADA;
        else if (stringZoneType.equals("EXTUS")) return ZoneType.EXTUS;
        else return ZoneType.EXTOVERSEAS;

    }

    @Override
    public String toString(){
        ZoneType zt = this;
        if (zt.equals(ZoneType.ONTARIO)) return "ONTARIO";
        else if (zt.equals(ZoneType.EXTCANADA)) return "EXTCANADA";
        else if (zt.equals(ZoneType.EXTUS)) return "EXTUS";
        else return "EXTOVERSEAS";

    }

    public static Collection<ZoneType> ZoneTypes(){
        return Arrays.asList(ZoneType.ONTARIO, ZoneType.EXTCANADA, ZoneType.EXTUS, ZoneType.EXTOVERSEAS);
    }


}




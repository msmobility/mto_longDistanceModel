package de.tum.bgu.msm.longDistance.zoneSystem;

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




}




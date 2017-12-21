package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by carlloga on 8/2/2017.
 */
public enum Mode {

    AUTO, AIR, RAIL, BUS;

//    private int[] modes = {0, 1, 2, 3};
//    private String[] modeNames = {"auto", "air", "rail", "bus"};

    public String toString() {
        Mode m = this;
        if (m.equals(Mode.AUTO)) return "auto";
        else if (m.equals(Mode.RAIL)) return "rail";
        else if (m.equals(Mode.AIR)) return "air";
        else return "bus";

    }


    public static Mode getMode(int m) {
        if (m == 0) return Mode.AUTO;
        else if (m == 1) return Mode.AIR;
        else if (m == 2) return Mode.RAIL;
        else return Mode.BUS;
    }

    public static Collection<Mode> ListOfModes() {
        return Arrays.asList(Mode.AUTO, Mode.AIR, Mode.RAIL, Mode.BUS);
    }

}

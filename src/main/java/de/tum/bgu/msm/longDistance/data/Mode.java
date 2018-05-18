package de.tum.bgu.msm.longDistance.data;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by carlloga on 8/2/2017.
 */
public enum Mode implements ModeI {

    AUTO(0), AIR(1), RAIL(2), BUS(3);

    private final int code;
//    private int[] modes = {0, 1, 2, 3};
//    private String[] modeNames = {"auto", "air", "rail", "bus"};

    Mode(int modeCode) {this.code = modeCode;}


    public String toString() {
        Mode m = this;
        if (m.equals(Mode.AUTO)) return "auto";
        else if (m.equals(Mode.RAIL)) return "rail";
        else if (m.equals(Mode.AIR)) return "air";
        else return "bus";

    }

    public static Mode getModeFromCode(int m) {
        if (m == 0) return Mode.AUTO;
        else if (m == 1) return Mode.AIR;
        else if (m == 2) return Mode.RAIL;
        else return Mode.BUS;
    }

    public static int getModeCode(Mode mode) {
        return mode.code;
    }



}

package de.tum.bgu.msm.longDistance.data;

public enum IncrementalMode implements ModeI {

    AUTO(0), AIR(1), RAIL(2), BUS(3),HIGH_SPEED_RAIL(4);

    private final int code;

    IncrementalMode(int code) {
        this.code = code;
    }

    public String toString() {
        IncrementalMode m = this;
        if (m.equals(IncrementalMode.AUTO)) return "auto";
        else if (m.equals(IncrementalMode.RAIL)) return "rail";
        else if (m.equals(IncrementalMode.AIR)) return "air";
        else if (m.equals(IncrementalMode.BUS)) return "bus";
        else return "hsr";

    }

    public static IncrementalMode getModeFromCode(int m) {
        if (m == 0) return IncrementalMode.AUTO;
        else if (m == 1) return IncrementalMode.AIR;
        else if (m == 2) return IncrementalMode.RAIL;
        else if (m==3) return IncrementalMode.BUS;
        else return IncrementalMode.HIGH_SPEED_RAIL;
    }

    public static int getModeCode(IncrementalMode mode) {
        return mode.code;
    }

}

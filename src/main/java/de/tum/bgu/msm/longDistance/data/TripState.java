package de.tum.bgu.msm.longDistance.data;


import java.util.Arrays;
import java.util.Collection;

public enum TripState {

    AWAY, DAYTRIP, INOUT;

//    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");

    public String toString() {
        TripState ts = this;
        if (ts.equals(TripState.AWAY)) return "away";
        else if (ts.equals(TripState.DAYTRIP)) return "daytrip";
        else return "inout";

    }

   public static TripState getTripsState(int tripStateInt) {
        if (tripStateInt == 0) return TripState.AWAY;
        else if (tripStateInt == 1) return TripState.DAYTRIP;
        else return TripState.INOUT;
    }

    public static Collection<TripState> getListOfModes() {
        return Arrays.asList(TripState.AWAY, TripState.DAYTRIP, TripState.INOUT);
    }
}

package de.tum.bgu.msm.longDistance;

/**
 * Created by carlloga on 8/10/2016.
 */
public class Zone {
    private int id;
    private int population;
    private ZoneType zoneType;
    private double accessibility;

    public Zone(int id, int population, ZoneType zoneType) {
        this.id = id;
        this.population = population;
        this.zoneType = zoneType;
    }

    public int getId() {
        return id;
    }

    public int getPopulation() {
        return population;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    public double getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(double accessibility) {
        this.accessibility = accessibility;
    }
}


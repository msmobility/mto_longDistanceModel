package de.tum.bgu.msm.longDistance.zoneSystem;

/**
 * Created by carlloga on 8/10/2016.
 */
public class Zone {
    private int id;
    private int population = 0;
    private int employment = 0;
    private int households = 0;
    private ZoneType zoneType;
    private double accessibility;


    public Zone(int id, int population, int employment, ZoneType zoneType) {
        this.id = id;
        this.population = population;
        this.employment = employment;
        this.zoneType = zoneType;
    }

    public int getId() {
        return id;
    }

    public int getPopulation() {
        return population;
    }

    public int getEmployment() {
        return employment;
    }

    public int getHouseholds() {
        return households;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    public double getAccessibility() {
        return accessibility;
    }

    public void addPopulation (int population){
        this.population += population;
}

    public void addHouseholds (int households){
        this.households += households;
    }

    public void setEmployment(int employment) {
        this.employment = employment;
    }

    public void setAccessibility(double accessibility) {
        this.accessibility = accessibility;
    }
}


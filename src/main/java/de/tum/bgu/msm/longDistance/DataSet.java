package de.tum.bgu.msm.longDistance;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.sp.Household;
import de.tum.bgu.msm.longDistance.sp.Person;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by carlloga on 8/1/2017.
 */
public class DataSet {

    private static Logger logger = Logger.getLogger(DataSet.class);

    public static final List<String> tripPurposes = Arrays.asList("visit", "business", "leisure");
    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");

    private Map<Integer, Zone> zones = new HashMap<>();

    private ArrayList<Zone> internalZones = new ArrayList<>();
    private ArrayList<Zone> externalZones = new ArrayList<>();

    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;

    private Map<Integer, Person> persons = new HashMap<>();
    private Map<Integer, Household> households = new HashMap<>();

    //models to be passed between models
    private DomesticModeChoice mcDomestic;
    private DomesticDestinationChoice dcDomestic;
    private IntModeChoice mcInt;
    private IntOutboundDestinationChoice dcIntOutbound;




    //methods

    public static List<String> getTripPurposes() {
        return tripPurposes;
    }

    public static List<String> getTripStates() {
        return tripStates;
    }

    public Map<Integer, Zone> getZones() {
        return zones;
    }

    public void setZones(Map<Integer, Zone> zones) {
        this.zones = zones;
    }

    public ArrayList<Zone> getInternalZones() {
        return internalZones;
    }

    public void setInternalZones(ArrayList<Zone> internalZones) {
        this.internalZones = internalZones;
    }

    public ArrayList<Zone> getExternalZones() {
        return externalZones;
    }

    public void setExternalZones(ArrayList<Zone> externalZones) {
        this.externalZones = externalZones;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        DataSet.logger = logger;
    }

    public Matrix getAutoTravelTime() {
        return autoTravelTime;
    }

    public void setAutoTravelTime(Matrix autoTravelTime) {
        this.autoTravelTime = autoTravelTime;
    }

    public Matrix getAutoTravelDistance() {
        return autoTravelDistance;
    }

    public void setAutoTravelDistance(Matrix autoTravelDistance) {
        this.autoTravelDistance = autoTravelDistance;
    }

    public Map<Integer, Person> getPersons() {
        return persons;
    }

    public void setPersons(Map<Integer, Person> persons) {
        this.persons = persons;
    }

    public Map<Integer, Household> getHouseholds() {
        return households;
    }

    public void setHouseholds(Map<Integer, Household> households) {
        this.households = households;
    }

    public float getAutoTravelTime(int orig, int dest) {
        try {
            return autoTravelTime.getValueAt(orig, dest);
        } catch (Exception e) {
            logger.error("*** Could not find zone pair " + orig + "/" + dest + " ***");
            return -999;
        }
    }

    public float getAutoTravelDistance(int orig, int dest) {
        try {
            return autoTravelDistance.getValueAt(orig, dest);
        } catch (Exception e) {
            logger.error("*** Could not find zone pair " + orig + "/" + dest + " ***");
            return -999;
        }
    }

    public Person getPersonFromId(int personId) {
        return persons.get(personId);
    }

    public Household getHouseholdFromId(int hhId) {
        return households.get(hhId);
    }

    public DomesticModeChoice getMcDomestic() {
        return mcDomestic;
    }

    public void setMcDomestic(DomesticModeChoice mcDomestic) {
        this.mcDomestic = mcDomestic;
    }

    public DomesticDestinationChoice getDcDomestic() {
        return dcDomestic;
    }

    public void setDcDomestic(DomesticDestinationChoice dcDomestic) {
        this.dcDomestic = dcDomestic;
    }

    public IntModeChoice getMcInt() {
        return mcInt;
    }

    public void setMcInt(IntModeChoice mcInt) {
        this.mcInt = mcInt;
    }

    public IntOutboundDestinationChoice getDcIntOutbound() {
        return dcIntOutbound;
    }

    public void setDcIntOutbound(IntOutboundDestinationChoice dcIntOutbound) {
        this.dcIntOutbound = dcIntOutbound;
    }
}

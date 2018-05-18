package de.tum.bgu.msm.longDistance;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.data.IncrementalMode;
import de.tum.bgu.msm.longDistance.data.Mode;
import de.tum.bgu.msm.longDistance.data.ModeI;
import de.tum.bgu.msm.longDistance.destinationChoice.DcModel;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.*;
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

    //GENERAL
    public static final List<String> tripPurposes = Arrays.asList("visit", "business", "leisure");
    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");

    //todo this should be placed in other class or better be an enum
    public static final int[] modes = {0, 1, 2, 3};
    public static final String[] modeNames = {"auto", "air", "rail", "bus"};

    public static Set<ModeI> modesSet = new HashSet<>();

    //ZONAL DATA
    private Map<Integer, Zone> zones = new HashMap<>();
    private ArrayList<Zone> internalZones = new ArrayList<>();
    private ArrayList<Zone> externalZones = new ArrayList<>();

    //SKIMS GA-zones
    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;

    //SYNYHETIC POPULATION
    private Map<Integer, Person> persons = new HashMap<>();
    private Map<Integer, Household> households = new HashMap<>();

    //TRIPS
    private ArrayList<LongDistanceTrip> allTrips;

    //MODELS
    //todo probably the data to be interchanged between models should be here instead
    private DcModel destinationChoiceModel;
    private DomesticDestinationChoice dcDomestic;
    private IntOutboundDestinationChoice dcIntOutbound;
    private IntInboundDestinationChoice dcIntInbound;


    private McModel modeChoiceModel;
    private CanadianDomesticMC canadianDomesticMC;
    private OntarianDomesticMC ontarianDomesticMC;
    private IntModeChoice mcInt;

    //Skims for mode choice
    private Matrix[] travelTimeMatrix = new Matrix[4];
    private Matrix[] priceMatrix = new Matrix[4];
    private Matrix[] transferMatrix = new Matrix[4];
    private Matrix[] frequencyMatrix = new Matrix[4];


    //geters and setters


    public void setModes(){
        modesSet.addAll(EnumSet.allOf(Mode.class));
        modesSet.addAll(EnumSet.allOf(IncrementalMode.class));

    }

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

    public void setAutoTravelTime(Matrix autoTravelTime) {
        this.autoTravelTime = autoTravelTime;
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
            return autoTravelTime.getValueAt(orig, dest);
    }

    public float getAutoTravelDistance(int orig, int dest) {
            return autoTravelDistance.getValueAt(orig, dest);
    }

    public Person getPersonFromId(int personId) {
        return persons.get(personId);
    }

    public Household getHouseholdFromId(int hhId) {
        return households.get(hhId);
    }

    public CanadianDomesticMC getCanadianDomesticMC() {
        return canadianDomesticMC;
    }

    public void setCanadianDomesticMC(CanadianDomesticMC mcDomestic) {
        this.canadianDomesticMC = mcDomestic;
    }

    public OntarianDomesticMC getOntarianDomesticMC() {
        return ontarianDomesticMC;
    }

    public void setOntarianDomesticMC(OntarianDomesticMC ontarianDomesticMC) {
        this.ontarianDomesticMC = ontarianDomesticMC;
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

    public ArrayList<LongDistanceTrip> getAllTrips() {
        return allTrips;
    }

    public void setAllTrips(ArrayList<LongDistanceTrip> allTrips) {
        this.allTrips = allTrips;
    }

    public DcModel getDestinationChoiceModel() {
        return destinationChoiceModel;
    }

    public void setDestinationChoiceModel(DcModel destinationChoiceModel) {
        this.destinationChoiceModel = destinationChoiceModel;
    }

    public McModel getModeChoiceModel() {
        return modeChoiceModel;
    }

    public void setModeChoiceModel(McModel modeChoiceModel) {
        this.modeChoiceModel = modeChoiceModel;
    }

    public IntInboundDestinationChoice getDcIntInbound() {
        return dcIntInbound;
    }

    public void setDcIntInbound(IntInboundDestinationChoice dcIntInbound) {
        this.dcIntInbound = dcIntInbound;
    }

    public Matrix[] getTravelTimeMatrix() {
        return travelTimeMatrix;
    }

    public void setTravelTimeMatrix(Matrix[] travelTimeMatrix) {
        this.travelTimeMatrix = travelTimeMatrix;
    }

    public Matrix[] getPriceMatrix() {
        return priceMatrix;
    }

    public void setPriceMatrix(Matrix[] priceMatrix) {
        this.priceMatrix = priceMatrix;
    }

    public Matrix[] getTransferMatrix() {
        return transferMatrix;
    }

    public void setTransferMatrix(Matrix[] transferMatrix) {
        this.transferMatrix = transferMatrix;
    }

    public Matrix[] getFrequencyMatrix() {
        return frequencyMatrix;
    }

    public void setFrequencyMatrix(Matrix[] frequencyMatrix) {
        this.frequencyMatrix = frequencyMatrix;
    }
}

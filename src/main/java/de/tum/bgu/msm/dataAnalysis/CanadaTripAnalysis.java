package de.tum.bgu.msm.dataAnalysis;

import de.tum.bgu.msm.dataAnalysis.gravityModel.GravityModel;
import de.tum.bgu.msm.dataAnalysis.surveyModel.SurveyDataImporter;
import de.tum.bgu.msm.dataAnalysis.surveyModel.mtoSurveyData;
import de.tum.bgu.msm.dataAnalysis.surveyModel.surveyTour;
import de.tum.bgu.msm.longDistance.mtoLongDistData;
import de.tum.bgu.msm.util;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Joe on 11/08/2016.
 */
public class CanadaTripAnalysis {

    private final ResourceBundle rb;
    private final Logger logger = Logger.getLogger(this.getClass());

    public CanadaTripAnalysis(ResourceBundle rb) {
        this.rb = rb;
    }

    public static void main (String[] args) {

        ResourceBundle rb = util.mtoInitialization(args[0]);

        CanadaTripAnalysis tca = new CanadaTripAnalysis(rb);
        tca.run();

    }

    private void run() {

        mtoSurveyData data = SurveyDataImporter.importData(rb);

        //Stream<surveyTour> allTours = data.getPersons().parallelStream().flatMap(p -> p.getTours().stream());
        //Map<Long, List<surveyTour>> cmaHistogram = allTours.filter(t -> t.getHomeCma() > 0).collect(Collectors.groupingBy(surveyTour::getDistinctNumRegions));

        List<surveyTour> allTours = data.getPersons().stream().flatMap(p -> p.getTours().stream())
                .filter(t -> t.getMainModeStr().equals("Auto")) //only want car trips for the moment
                .collect(Collectors.toList());

        int max_zone_num = 10000;
        double[] productions = new double[max_zone_num];
        double[] attractions = new double[max_zone_num];
        int[] zone_index = new int[max_zone_num];
        int[] zone_index_reverse = new int[max_zone_num];
        int next_zone_index = 0;
        Arrays.fill(productions, 0);
        Arrays.fill(attractions, 0);
        Arrays.fill(zone_index, -1);
        Arrays.fill(zone_index, -1);

        for (surveyTour t : allTours) {
            //filter here to only add certain trips?
            ArrayList<Integer> affectedZones = new ArrayList<Integer>();

            //only for internal-external trips
            if (t.getDestProvince() == 35) {
                int dest_zone = data.getZoneForCd(t.getUniqueDestCD());
                if (dest_zone != -1)  affectedZones.add(dest_zone);

                affectedZones.add(data.getZoneIdForProvince(t.getOrigProvince()));
                affectedZones.add(data.getZoneIdForCMA(t.getHomeCma()));
            }
            if (t.getOrigProvince() == 35) {
                int orig_zone = data.getZoneForCd(t.getUniqueOrigCD());
                if (orig_zone != -1) affectedZones.add(orig_zone);

                affectedZones.add(data.getZoneIdForProvince(t.getDestProvince()));
                affectedZones.add(data.getZoneIdForCMA(t.getDestCma()));
            }
            //zone 0 is null
            for (int z : affectedZones) {
                if (z > 0) {
                    if (zone_index[z] == -1) {
                        zone_index[z] = next_zone_index;
                        zone_index_reverse[next_zone_index] = z;
                        next_zone_index++;
                    }
                    productions[zone_index[z]] += t.getWeight();
                    attractions[zone_index[z]] += t.getWeight();
                }
            };
        };
        int numZones = next_zone_index;
        productions = Arrays.copyOf(productions, numZones);
        attractions = Arrays.copyOf(attractions, numZones);


        String output_filename = rb.getString("output.folder") + File.separator + "zone_pa.csv";
        try (FileWriter writer = new FileWriter(output_filename)) {
            //write headers
            writer.write("zone,in_trips,out_trips\n");
            for (int i=0; i<productions.length; i++) {
                if (productions[i] > 0 || attractions[i] > 0) {
                    writer.write(Integer.toString(i) + "," + productions[i] + "," + attractions[i]);
                    writer.write("\n");
                }
            }

        } catch (IOException e) {

        }

        //build gravity model
        mtoLongDistData mtoLongDistData = new mtoLongDistData(rb);
        int[] cd_zones = data.getCensusDivisionList().getColumnAsInt("ID").clone();
        Arrays.sort(cd_zones);
        int validZoneCount = 0;

        //get all travel times between II and IE zones, but exlcude EE;
        double[][] skim = new double[numZones][numZones];
        //for every
        for (int i=1001; i<max_zone_num; i++) {
            for (int j=1001; j < max_zone_num; j++) {
                if (zone_index[i] > -1 && zone_index[j] > -1) {
                    boolean i_in_cd = Arrays.binarySearch(cd_zones, i) >= 0;
                    boolean j_in_cd = Arrays.binarySearch(cd_zones, j) >= 0;
                    boolean isConnection = false;
                    if (i_in_cd && j >= 9750 && j <= 9797) {
                        isConnection = true;
                    } else if (i >= 9750 && i <= 9797 && j_in_cd) {
                        isConnection = true;
                    } else if (i != j && i_in_cd && j_in_cd) {
                        isConnection = true;
                    } else {
                        isConnection = false;
                    }
                    if (isConnection) {
                        skim[zone_index[i]][zone_index[j]] = mtoLongDistData.getAutoTravelTime(i, j);
                        validZoneCount++;
                    } else {
                        skim[zone_index[i]][zone_index[j]] = Double.POSITIVE_INFINITY;
                    }
                }
            }
        }
        logger.info("number of valid zones: " + validZoneCount);

        //mask travel times for non connections


        GravityModel gm = new GravityModel(productions, attractions, skim, 1);
        gm.run();








    }


}

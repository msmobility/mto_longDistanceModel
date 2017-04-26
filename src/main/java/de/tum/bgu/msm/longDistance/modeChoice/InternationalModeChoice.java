package de.tum.bgu.msm.longDistance.modeChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.zoneSystem.MtoLongDistData;

import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/26/2017.
 */
public class InternationalModeChoice {

    ResourceBundle rb;

    private int[] modes = {0, 1, 2, 3};
    private String[] modeNames = {"auto", "air", "rail", "bus"};
    // 0 is auto, 1 is plane, 2 is train, 3 is rail

    //the arrays of matrices are stored in the order of modes
    private Matrix[] travelTimeMatrix = new Matrix[4];
    private Matrix[] priceMatrix = new Matrix[4];
    private Matrix[] transferMatrix = new Matrix[4];
    private Matrix[] frequencyMatrix = new Matrix[4];

    String[] tripPurposeArray;
    String[] tripStateArray;



    public InternationalModeChoice(ResourceBundle rb, MtoLongDistData ldData, DomesticModeChoice dmChoice) {
        this.rb = rb;



        tripPurposeArray = ldData.tripPurposes.toArray(new String[ldData.tripPurposes.size()]);
        tripStateArray = ldData.tripStates.toArray(new String[ldData.tripStates.size()]);

        //todo check if this can get the already read skims by mode
        dmChoice.readSkimByMode(rb);
    }

    public int selectMode(LongDistanceTrip trip){

        int destination = trip.getDestZoneId();



        return 1;
    }

}

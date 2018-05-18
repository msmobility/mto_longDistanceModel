package de.tum.bgu.msm.longDistance.modeChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LongDistanceTrip;

public interface ModeChoiceModelI {

    void load(DataSet dataSet);
    int selectMode(LongDistanceTrip trip);


    default Matrix[] readTravelTimeSkim(DataSet dataset) {
        return dataset.getTravelTimeMatrix();
    }

    default Matrix[] readPriceSkim(DataSet dataset) {
        return dataset.getPriceMatrix();
    }

    default Matrix[] readTrasferSkim(DataSet dataset) {
        return dataset.getTransferMatrix();
    }

    default Matrix[] readFrequencySkim(DataSet dataset) {
        return dataset.getFrequencyMatrix();
    }


    double calculateUtility(LongDistanceTrip trip, int m, int destination);

}

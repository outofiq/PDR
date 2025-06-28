package com.example.pdr_2;

import android.util.Log;
import java.util.LinkedList;


/**
 * 对外接口：GetStepLength(double data, long time)
 * data：滤波结果
 * time：传感器事件发生时间的时间戳
 */
public class StepDetect {
    private final LinkedList<Double> dataQueue, maxQueue, minQueue, peakQueue, valleyQueue;
    protected double alpha = 0.2;
    private long timestamp, lastTimestamp, beforeLastTimestamp;
    private double stepLength;
    private boolean trueStep;
    protected double a = 0.371, b = 0.227, c = 1, H = 1.72;
    private double backupDeltaT;




    public StepDetect() {
        dataQueue = new LinkedList<>();
        maxQueue = new LinkedList<>();
        minQueue = new LinkedList<>();
        peakQueue = new LinkedList<>();
        valleyQueue = new LinkedList<>();


        timestamp = 0;
        lastTimestamp = 0;
        backupDeltaT = 0.8;
        beforeLastTimestamp = 0;
        trueStep = true;
        stepLength = 0;
    }

    public double GetStepLength(double data, long time) {
        timestamp = time;
        if (lastTimestamp == 0) lastTimestamp = timestamp;
        if (beforeLastTimestamp == 0) beforeLastTimestamp = timestamp;

        stepLength = 0;

        if (dataQueue.size() == 3) dataQueue.removeFirst();
        dataQueue.addLast(data);

        if (dataQueue.size() == 3) getExtreme();

        return stepLength;
    }

    private void getExtreme() {
        double data0 = dataQueue.get(0);
        double data1 = dataQueue.get(1);
        double data2 = dataQueue.get(2);

        if (data1 > data0 && data1 > data2) {

            if (maxQueue.size() == 2) maxQueue.removeFirst();
            maxQueue.addLast(data1);

            if (maxQueue.size() == 2) RemoveOutliers(true);
        } else if (data1 < data0 && data1 < data2) {
            if (minQueue.size() == 2) minQueue.removeFirst();
            minQueue.addLast(data1);

            if (minQueue.size() == 2) RemoveOutliers(false);
        }
    }

    private void RemoveOutliers(boolean type) {
        if (type) {
            double max0 = maxQueue.get(0);
            double max1 = maxQueue.get(1);

            if (max0 + max1 > 0.1) {

                if (peakQueue.size() == 50) peakQueue.removeFirst();
                peakQueue.addLast(max1);

            }
        } else {
            double min0 = minQueue.get(0);
            double min1 = minQueue.get(1);

            if (min0 + min1 < -0.1) {

                if (valleyQueue.size() == 50) valleyQueue.removeFirst();
                valleyQueue.addLast(min1);
            }
        }

        if (!peakQueue.isEmpty() && !valleyQueue.isEmpty()) DynamicThreshold();
    }

    private void DynamicThreshold() {
        double peakMean = 0, valleyMean = 0;
        double peakMSD = 0, valleyMSD = 0;

        for (double value : peakQueue) peakMean += value;
        peakMean /= peakQueue.size();
        for (double value : peakQueue) peakMSD += Math.pow(value - peakMean, 2.0);
        peakMSD = Math.sqrt(peakMSD / (peakQueue.size() - 1));

        for (double value : valleyQueue) valleyMean += value;
        valleyMean /= valleyQueue.size();
        for (double value : valleyQueue) valleyMSD += Math.pow(value - valleyMean, 2.0);
        valleyMSD = Math.sqrt(valleyMSD / (valleyQueue.size() - 1));



        boolean peakDetected = false, valleyDetected = false;

        if (peakMSD - valleyMSD > alpha) {
            if (peakQueue.getLast() >= peakMSD * peakMSD) {
                peakDetected = true;
            }
            if (valleyQueue.getLast() <= -valleyMSD * valleyMSD) {
                valleyDetected = true;
            }
        } else {
            if (peakQueue.getLast() >= peakMSD) {
                peakDetected = true;
            }
            if (valleyQueue.getLast() <= -valleyMSD) {
                valleyDetected = true;
            }
        }
        if (peakDetected && valleyDetected) {
            if (trueStep) CalcStepLength();
            trueStep = !trueStep;
        }
    }

    private void CalcStepLength() {
        double deltaT = (double) (timestamp - lastTimestamp) / 1000000000.0;
        double lastDeltaT = (double) (lastTimestamp - beforeLastTimestamp) / 1000000000.0;
        Log.d("StepDeltaT", String.valueOf(deltaT));
        Log.d("LastStepDeltaT", String.valueOf(lastDeltaT));

        if (deltaT <= 1) backupDeltaT = deltaT;
        Log.d("BackupDeltaT", String.valueOf(backupDeltaT));

        if (deltaT > 1) deltaT = backupDeltaT;
        if (lastDeltaT > 1) lastDeltaT = backupDeltaT;
        Log.d("JudgeStepDeltaT", String.valueOf(deltaT));
        Log.d("JudgeLastStepDeltaT", String.valueOf(lastDeltaT));



        double SF = 1 / (0.8 * deltaT + 0.2 * lastDeltaT);
        stepLength = (0.7 + a * (H - 1.6) + b * (SF - 1.79) * H / 1.6) * c;

        beforeLastTimestamp = lastTimestamp;
        lastTimestamp = timestamp;
    }
}

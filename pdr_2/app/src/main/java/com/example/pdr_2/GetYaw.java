package com.example.pdr_2;

import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedList;

/**
 * 获取设备的初始航向角（yaw angle）
 * 通过融合加速度计（TYPE_ACCELEROMETER）和磁力计（TYPE_MAGNETIC_FIELD）的数据
 * 并进行滤波处理，最终得到较为准确的初始航向角值，并在界面上展示
 * 对外接口：getYaw()
 * 构造函数：Yaw(DataLib dataLib, Context context, TextView yawText)
 */
public class GetYaw {
    private final DataLib dataLib;
    private Dialog progressDialog;
    private final TextView yawText;
    private final Context context;
    private final double[] accData, magData;
    private LinkedList<Double> yawQueue;
    private final SensorManager sensorManager;
    private final Sensor accSensor, magSensor;
    private final Filter filter;

    public GetYaw(DataLib dataLib, Context context, TextView yawText) {
        this.dataLib = dataLib;//将获取到的变量赋值给类的成员变量
        this.context = context;
        this.yawText = yawText;

        filter = new Filter();

        accData = new double[3];
        magData = new double[3];
        yawQueue = new LinkedList<>();

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private OnYawFinishedListener onYawFinishedListener;
    public interface OnYawFinishedListener {
        void onYawFinished();
    }
    public void setOnYawFinishedListener(OnYawFinishedListener listener) {
        this.onYawFinishedListener = listener;
    }
    private final SensorEventListener accListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            accData[0] = event.values[0];
            accData[1] = event.values[1];
            accData[2] = event.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private final SensorEventListener magListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            magData[0] = event.values[0];
            magData[1] = event.values[1];
            magData[2] = event.values[2];

            float[] accDataF = new float[3];
            float[] magDataF = new float[3];
            for (int i = 0; i < 3; i++) {
                accDataF[i] = (float) accData[i];
                magDataF[i] = (float) magData[i];
            }

            float[] R = new float[9];
            float[] values = new float[3];
            SensorManager.getRotationMatrix(R, null, accDataF, magDataF);
            SensorManager.getOrientation(R, values);

            double yawData = values[0];
            double yawFilterData = filter.getFilterData(yawData);

            yawQueue.addLast(yawFilterData);

            updateProgress(yawQueue.size());

            Log.d("yaw", "航向角数据" + yawQueue.size());

            if (yawQueue.size() == 200) {


                if (onYawFinishedListener != null) {
                    onYawFinishedListener.onYawFinished();
                }


                for (double value : yawQueue) dataLib.initAngle += value;
                dataLib.initAngle /= yawQueue.size();

                yawQueue = new LinkedList<>();

                yawText.setText(context.getString(com.example.pdr_2.R.string.yawText,Math.toDegrees(dataLib.initAngle)));
                Log.d("yaw", "初始航向角" + dataLib.initAngle);

                showMsg();

                sensorManager.unregisterListener(accListener, accSensor);
                sensorManager.unregisterListener(magListener, magSensor);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void getYaw() {
        showProgress(context);

        sensorManager.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(magListener, magSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void showProgress(Context context) {
        // 使用自定义布局创建对话框
        progressDialog = new Dialog(context);
        progressDialog.setContentView(R.layout.get_yaw);
        progressDialog.setCancelable(false); // 禁止取消

        // 显示对话框
        progressDialog.show();
    }

    private void updateProgress(int progress) {
        if (progressDialog != null && progressDialog.isShowing()) {
            ProgressBar progressBar = progressDialog.findViewById(R.id.pb_progress);
            progressBar.setProgress(progress);

            // 如果进度达到最大值，关闭对话框
            if (progress >= 200) {
                dismissProgress();
            }
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showMsg() {
        Toast.makeText(context, "获取初始航向角成功", Toast.LENGTH_SHORT).show();
    }
}

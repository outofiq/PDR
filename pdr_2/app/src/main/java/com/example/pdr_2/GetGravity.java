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
import java.util.Locale;

/**
 * 通过注册传感器监听器，对获取到的原始数据进行滤波处理，计算平均重力值
 * 并在界面上显示相关信息，同时使用进度对话框和提示信息来增强用户体验。
 * 构造函数：Gravity(DataLib dataLib, Context context, TextView gravText)
 * 外部接口：getGravity()
 */
public class GetGravity {
    private final DataLib dataLib;
    private Dialog progressDialog;
    private final TextView gravText;
    private final Context context;
    private final double[] gravData;
    private LinkedList<Double> gravQueue;
    private final SensorManager sensorManager;
    private final Sensor gravSensor;
    private final Filter filter;

    /**
     * 构造函数
     * @param dataLib 数据存储
     * @param context context
     * @param gravText 获取的平均重力显示位置
     */
    public GetGravity(DataLib dataLib, Context context, TextView gravText){
        this.dataLib = dataLib;
        this.context = context;
        this.gravText = gravText;

        filter = new Filter();
        gravData = new double[3];
        gravQueue = new LinkedList<>();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gravSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    //回调接口
    private OnGravityFinishedListener onGravityFinishedListener;
    public interface OnGravityFinishedListener {
        void onGravityFinished();
    }
    public void setOnGravityFinishedListener(OnGravityFinishedListener listener) {
        this.onGravityFinishedListener = listener;
    }



    private final SensorEventListener gravListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            gravData[0] = event.values[0];
            gravData[1] = event.values[1];
            gravData[2] = event.values[2];

            double gravModData = Math.sqrt(Math.pow(gravData[0], 2.0) + Math.pow(gravData[1], 2.0) + Math.pow(gravData[2], 2.0));
            double gravFilterModData = filter.getFilterData(gravModData);

            gravQueue.addLast(gravFilterModData);

            updateProgress(gravQueue.size());

            Log.d("gyroSensor", "重力数据" + gravQueue.size());
            if (gravQueue.size() == 200) {

                //回调接口
                if (onGravityFinishedListener != null) {
                    onGravityFinishedListener.onGravityFinished();
                }


                for (double value : gravQueue) dataLib.grav += value;
                dataLib.grav /= gravQueue.size();

                gravQueue = new LinkedList<>();

                String formattedGrav = String.format(Locale.US, "%.6f", dataLib.grav);
                gravText.setText(context.getString(R.string.average_gravity_value, formattedGrav));
                Log.d("gyroSensor", "平均重力值" + dataLib.grav);

                showMsg();

                sensorManager.unregisterListener(gravListener, gravSensor);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    public void getGravity() {
        showProgress(context);

        sensorManager.registerListener(gravListener, gravSensor, SensorManager.SENSOR_DELAY_GAME);


    }


    //=============================================================
    // 对外显示
    //=============================================================
    private void showProgress(Context context) {
        // 使用自定义布局创建对话框
        progressDialog = new Dialog(context);
        progressDialog.setContentView(R.layout.get_gravity);
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
        Toast.makeText(context, "获取平均重力值成功", Toast.LENGTH_SHORT).show();
    }
}

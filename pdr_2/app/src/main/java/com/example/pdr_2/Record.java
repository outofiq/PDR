package com.example.pdr_2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * 开始、暂停、停止按钮对应的类
 * 对外接口：
 * StartRecord（）
 * StopRecord（）
 */
public class Record {
    private final DataLib dataLib;
    private final Context context;
    private final android.widget.Button startBtn, stopBtn;
    private final TextView  ENUText;
    private final TextView Yaw;
    private final TextView stepNumText, stepLengthText;

    private final SensorManager sensorManager;
    private final Sensor accSensor, gyroSensor;

    private final double[] accData;
    private final double[] gyroData;
    private double[] attAngle;
    private long timestamp, lastTimestamp;
    private int stepNum;
    private double N, E;

    private TrackingView canvas;

    private final AttitudeAngle attitudeAngle;
    private final Filter filter;
    private final StepDetect stepDetect;


    public Record(
                  DataLib dataLib,
                  Context context,
                  android.widget.Button startBtn, android.widget.Button stopBtn,
                  TextView ENUText,
                  TrackingView canvas,
                  TextView Yaw,
                  TextView stepNumText, TextView stepLengthText) {
        this.dataLib = dataLib;
        this.context = context;
        this.startBtn = startBtn;
        this.stopBtn = stopBtn;
        this.ENUText = ENUText;
        this.canvas=canvas;
        this.Yaw = Yaw;
        this.stepNumText = stepNumText;
        this.stepLengthText = stepLengthText;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        accData = new double[3];
        gyroData = new double[3];
        attAngle = new double[3];
        timestamp = 0;
        lastTimestamp = 0;
        stepNum = 0;
        N = 0;
        E = 0;

        attitudeAngle = new AttitudeAngle();
        filter = new Filter();
        stepDetect = new StepDetect();
    }

    /**
     * 定义加速度计监听实例
     * 监听对象？？？？？？？？？？？
     * accListener
     */
    private final SensorEventListener accListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            //获取加速度传感器数据变化的时间

            //获取传感器的三轴加速度数据、滤波值
            // 对外显示：加速度计Z轴值
            // 对外显示  滤波值
            accData[0] = event.values[0];
            accData[1] = event.values[1];
            accData[2] = event.values[2];
            double accZData = accData[2] - dataLib.grav;
            double HMAFilterData = filter.HMAFilter(accZData);
            double kalmanFilterData = filter.KalmanFilter(HMAFilterData);
            double BWLPFilterData = filter.BWLPFilter(kalmanFilterData);


            //获取步长
            double stepLength = stepDetect.GetStepLength(BWLPFilterData, event.timestamp);

            if (stepLength != 0) {

                double angle = dataLib.angle;

                //北东方向位置更新：步长×修正后的航向角
                E += stepLength * Math.sin(angle);
                N += stepLength * Math.cos(angle);
                double[] ENU = new double[]{E, N, 0};

                //对外显示：ENU坐标
                ENUText.setText(context.getString(R.string.ENUText,ENU[0],ENU[1],Math.toDegrees(angle)));


                // 获取 SurfaceView 的宽度和高度
                int width = canvas.getWidth();
                int height = canvas.getHeight();
                Log.d("Record", "画布尺寸: width=" + width + ", height=" + height);

                // 定义缩放因子，用于将坐标缩放到画布范围内
                float scaleFactor = 50.0f;  // 增加缩放因子，使轨迹更明显
                // 根据 SurfaceView 的宽度和高度动态计算初始偏移量
                float offsetX = width / 2;
                float offsetY = height / 2;
                
                Log.d("Record", "原始坐标: E=" + E + ", N=" + N);
                Log.d("Record", "偏移量: offsetX=" + offsetX + ", offsetY=" + offsetY);

                // 对坐标进行缩放和偏移处理
                float scaledX = (float) (E * scaleFactor) + offsetX;
                float scaledY = (float) (N * scaleFactor) + offsetY;
                
                // 确保坐标在画布范围内
                scaledX = Math.max(0, Math.min(scaledX, width));
                scaledY = Math.max(0, Math.min(scaledY, height));

                Log.d("Record", "最终坐标: x=" + scaledX + ", y=" + scaledY);
                Log.d("Record", "坐标是否在范围内: " + 
                    (scaledX >= 0 && scaledX <= width && scaledY >= 0 && scaledY <= height));

                canvas.addTrajectoryPoint(scaledX, scaledY);

                //步数自增
                //对外显示  步数：  步长：
                stepNum++;
                stepNumText.setText(context.getString(R.string.stepNumText,stepNum));
                stepLengthText.setText(context.getString(R.string.stepLengthText,stepLength));


            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //加速度计精度发生变化时不做处理
            //没有能力处理
        }
    };

    /**
     * 定义陀螺仪监听实例
     * 监听对象？？？？？？？？？？？？？？
     * gyroListener
     */
    private final SensorEventListener gyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            gyroData[0] = event.values[0];
            gyroData[1] = event.values[1];
            gyroData[2] = event.values[2];

            timestamp = event.timestamp;
            if (lastTimestamp == 0) lastTimestamp = timestamp;
            double deltaT = (double) (timestamp - lastTimestamp) / 1000000000.0;//前面没有赋值，只有构造函数，满足上一个if判断，deltaT===0
            lastTimestamp = timestamp;//这是对其赋值，上一次的时间戳，此后deltaT不为0

            if (deltaT != 0) {
                attAngle = attitudeAngle.getAttAngle(accData, gyroData, deltaT);
                dataLib.angle = attAngle[2] + dataLib.initAngle;
            }

            //对外显示
            //陀螺仪航向角
            Yaw.setText(context.getString(R.string.yawGyroText,Math.toDegrees(dataLib.angle)));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    public void StartRecord() {
        sensorManager.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME);

        StartRecordAnimate();
    }


    public void StopRecord() {
        sensorManager.unregisterListener(accListener, accSensor);
        sensorManager.unregisterListener(gyroListener, gyroSensor);

        StopRecordAnimate();
    }

    //点击开始按钮后的动画
    //开始按钮变为不可见
    private void StartRecordAnimate() {

        startBtn.setVisibility(View.INVISIBLE);
        startBtn.setEnabled(false);
        stopBtn.setVisibility(View.VISIBLE);
        stopBtn.setEnabled(true);

    }

    //点击停止按钮后的动画
    //暂停按钮和停止按钮分别向右左移动，回到开始按钮的位置上，并变为不可见
    //开始按钮变为可见
    private void StopRecordAnimate() {


        stopBtn.setVisibility(View.INVISIBLE);
        stopBtn.setEnabled(false);

        startBtn.setVisibility(View.VISIBLE);
        startBtn.setEnabled(true);

    }



}

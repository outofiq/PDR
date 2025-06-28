package com.example.pdr_2;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity{

    //======================================
    //成员变量
    //======================================

    /**
     * 数据处理相关
     */
    private DataLib dataLib;
    private Record record;
    private GetGravity getGravity;
    private GetYaw getYaw;


    /**
     * 状态相关
     * btmSheetStatus 底部面板状态
     * gravStatus     是否获取重力数据
     * posStatus      位置数据
     * yawStatus      航向角数据
     * startBtnStatus 记录是否开始
     * canSave        是否保存
     * canClear       是否清除数据
     */
    private boolean btmSheetStatus;
    private boolean startBtnStatus;
    Context context = this;
    //获取当前对象的引用
    //获取当前活动的上下文


    /**
     * 权限相关
     * REQUEST_PERMISSIONS  权限码
     */
    private static final int REQUEST_PERMISSIONS = 9527;

    /**
     * UI组件
     * 按钮
     * 文本框
     */
    private LinearLayout btmSheet,mainBtmSheet;
    private android.widget.Button startBtn;
    private android.widget.Button stopBtn;
    private TextView  yawText, ENUText;
    private TextView gravText;
    private TextView yawGyroText;
    private TextView stepNumText, stepLengthText;
    private TrackingView canvas;
    private LinearLayout layout;



    //=======================================
    // 初始化函数
    //=======================================


    /**
     * 视图初始化函数
     * 找到ui中的组件、赋值给成员变量
     */
    private void initViews() {
        //UI布局
        EdgeToEdge.enable(this);//边缘到边缘显示（全屏显示）
        setContentView(R.layout.activity_main);//通过activity_main文件设置UI布局

        //视图在系统栏下方正确显示，避免被系统栏遮挡，确保适用不同设备
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //初始化按钮状态量
        btmSheetStatus = false;
        startBtnStatus = false;

        //初始化画板
        canvas = findViewById(R.id.canvas_view);

        // 初始化动态展示面板
        btmSheet = findViewById(R.id.btmSheet);
        mainBtmSheet = findViewById(R.id.mainBtmSheet);

        // 初始化按钮
        startBtn = findViewById(R.id.startBtn);
        stopBtn = findViewById(R.id.stopBtn);

        // 初始化文本视图
        gravText = findViewById(R.id.gravText);
        yawText = findViewById(R.id.yawText);
        ENUText = findViewById(R.id.ENUText);
        yawGyroText = findViewById(R.id.yawGyroText);
        stepNumText = findViewById(R.id.stepNumText);
        stepLengthText = findViewById(R.id.stepLengthText);
    }



    /**
     * 各个按钮初始化
     */
    private void initRecord() {
        dataLib = new DataLib();
        getGravity = new GetGravity(dataLib, this, gravText);
        getYaw = new GetYaw(dataLib, this, yawText);
        record = new Record(
                dataLib,
                this,
                startBtn,
                stopBtn,
                ENUText,
                canvas,
                yawGyroText,
                stepNumText,
                stepLengthText
        );
    }

    private void GetPosition()
    {

        ENUText.setText(getString(R.string.ENU_location,dataLib.ENU[0],dataLib.ENU[1],dataLib.ENU[2]));

        showMsg("获取初始位置成功");
    }
    /**
     * 监听事件初始化函数
     */
    private void initBtnClickListener() {

        //为开始按钮添加一个监听事件，如果点击按钮就会。。。。。。
        startBtn.setOnClickListener(v-> {

            // 设置 Gravity 完成回调
            getGravity.setOnGravityFinishedListener(() -> {
                // 设置 Yaw 完成回调
                getYaw.setOnYawFinishedListener(() -> {
                    GetPosition();
                    // 设置位置获取完成后的回调
                    new Handler(Looper.getMainLooper()).post(() -> {
                        startBtnStatus = true;
                        record.StartRecord();
                        showMsg("开始记录");

                    });
                });
                getYaw.getYaw();
            });



            getGravity.getGravity();

        });

        //为停止按钮添加一个监听事件，如果点击就会。。。。。。
        stopBtn.setOnClickListener(v-> {
            startBtnStatus = false;

            getGravity = new GetGravity(dataLib, context, gravText);
            getYaw = new GetYaw(dataLib, context, yawText);

            record.StopRecord();
            showMsg("停止记录");

            //清理面板
            if(startBtnStatus) showMsg("请先停止记录");
            else {
                gravText.setText(R.string.averageGravity_clean);
                yawText.setText(R.string.StartYaw_clean);
                ENUText.setText(R.string.ENU_Location_clean);
                yawGyroText.setText(R.string.GYR_Yaw_clean);
                stepNumText.setText(R.string.step_num_clean);
                stepLengthText.setText(R.string.step_length_clean);
                //清理画板
                canvas.clearTrajectory();

            }


        });




        //为屏幕底部的详细数据面板（布局容器）添加监听事件，如果点击就会。。。。。。
        mainBtmSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //面板动画对象创建
                ObjectAnimator moveUp = ObjectAnimator.ofFloat(btmSheet, "translationY", 0f, -600f);
                ObjectAnimator moveDown = ObjectAnimator.ofFloat(btmSheet, "translationY", -600f, 0f);
                //地图动画对象创建
                ObjectAnimator moveMapUp = ObjectAnimator.ofFloat(canvas, "translationY", 0f, -700f);
                ObjectAnimator moveMapDown = ObjectAnimator.ofFloat(canvas, "translationY", -700f, 0f);

                //设置动画时长、为动画对象添加监听器
                setAnimationDuration(moveUp, moveDown, moveMapUp, moveMapDown);
                setAnimationListeners(moveUp, moveDown, moveMapUp, moveMapDown);

                //判断面板是否被打开
                if (!btmSheetStatus) {
                    AnimatorSet animatorSetUp = new AnimatorSet();
                    animatorSetUp.playTogether(moveUp, moveMapUp);

                    Log.d("BottomSheet", "向上移动, status=" + false);
                    animatorSetUp.start();
                    btmSheetStatus = true;
                    Log.d("BottomSheet", "status改为" + true);
                } else {
                    AnimatorSet animatorSetDown = new AnimatorSet();
                    animatorSetDown.playTogether(moveDown, moveMapDown);

                    Log.d("BottomSheet", "向下移动, status=" + true);
                    animatorSetDown.start();
                    btmSheetStatus = false;
                    Log.d("BottomSheet", "status改为" + false);
                }
            }

            //设置动画时长
            private void setAnimationDuration(ObjectAnimator... animators) {
                for (ObjectAnimator animator : animators) {
                    animator.setDuration(250);
                }
            }

            //设置动画监听器
            private void setAnimationListeners(ObjectAnimator... animators) {
                AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
                    //动画开始时被调用，将 startBtn 设置为不可用状态，并禁用地图 aMap 的所有手势操作。
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        startBtn.setEnabled(false);
                    }

                    //动画结束时被调用，将 startBtn 设置为可用状态，并启用地图 aMap 的所有手势操作。
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startBtn.setEnabled(true);
                    }
                };

                //为每个动画添加监听器
                for (ObjectAnimator animator : animators) {
                    animator.addListener(listener);
                }
            }
        });




    }


    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (EasyPermissions.hasPermissions(this, permissions)) {
            //true 有权限 开始定位

        } else {
            //false 无权限
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    /**
     * 检查Android版本便于申请权限
     */
    private void checkingAndroidVersion() {
        //Android6.0及以上先获取权限再定位
        requestPermission();
    }


    /**
     * 请求权限结果
     * @param requestCode 请求码
     * @param permissions 请求的权限列表
     * @param grantResults 权限请求的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //用于简化权限请求和处理的流程
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 提示函数
     * @param msg 提示内容
     */
    private void showMsg(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // 检查Android版本
            checkingAndroidVersion();
            // 请求权限
            requestPermission();
            // 初始化视图
            initViews();
            // 初始化记录器
            initRecord();
            // 初始化按钮监听器
            initBtnClickListener();
        } catch (Exception e) {
            Log.e("MainActivity", "初始化失败: " + e.getMessage());
            e.printStackTrace();
            // 显示错误信息给用户
            Toast.makeText(this, "应用初始化失败，请重启应用", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
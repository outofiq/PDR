package com.example.pdr_2;

/**
 * 作为一个数据容器，用于存储与位置、角度、重力
 * !!只作为容器，没有对外的接口
 *
 */
public class DataLib {

    public double[] ENU;
    public double grav;
    public double initAngle;
    public double angle;

    public DataLib() {
        ENU = new double[3];  // 初始化ENU数组
        ENU[0]=ENU[1]=ENU[2]=0.0;
        grav = 0;
        initAngle = 0;
        angle = 0;
    }
}

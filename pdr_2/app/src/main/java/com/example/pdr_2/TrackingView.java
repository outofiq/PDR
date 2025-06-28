package com.example.pdr_2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class TrackingView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Path path;
    private List<float[]> trajectoryPoints;

    public TrackingView(Context context) {
        super(context);
        init();
    }

    public TrackingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrackingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        path = new Path();
        trajectoryPoints = new ArrayList<>();
        
        // 设置背景色为透明
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void addTrajectoryPoint(float x, float y) {
        Log.d("TrackingView", "添加轨迹点: x=" + x + ", y=" + y);
        trajectoryPoints.add(new float[]{x, y});
        updatePath();
        drawTrajectory();
    }

    private void updatePath() {
        path.reset();
        if (trajectoryPoints.size() > 0) {
            float[] firstPoint = trajectoryPoints.get(0);
            path.moveTo(firstPoint[0], firstPoint[1]);
            for (int i = 1; i < trajectoryPoints.size(); i++) {
                float[] point = trajectoryPoints.get(i);
                path.lineTo(point[0], point[1]);
                Log.d("TrackingView", "绘制线段到: x=" + point[0] + ", y=" + point[1]);
            }
        }
    }

    private void drawTrajectory() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            try {
                // 清除画布为白色
                canvas.drawColor(Color.WHITE);
                
                // 绘制网格
                Paint gridPaint = new Paint();
                gridPaint.setColor(Color.LTGRAY);
                gridPaint.setStrokeWidth(1);
                
                int width = canvas.getWidth();
                int height = canvas.getHeight();
                int centerX = width / 2;
                int centerY = height / 2;
                
                // 绘制网格线
                for (int i = 0; i < width; i += 50) {
                    canvas.drawLine(i, 0, i, height, gridPaint);
                }
                for (int i = 0; i < height; i += 50) {
                    canvas.drawLine(0, i, width, i, gridPaint);
                }
                
                // 绘制中心十字
                gridPaint.setColor(Color.BLUE);
                gridPaint.setStrokeWidth(2);
                canvas.drawLine(0, centerY, width, centerY, gridPaint);
                canvas.drawLine(centerX, 0, centerX, height, gridPaint);
                
                // 绘制轨迹
                if (!trajectoryPoints.isEmpty()) {
                    canvas.drawPath(path, paint);
                    
                    // 绘制所有点
                    Paint pointPaint = new Paint();
                    pointPaint.setColor(Color.GREEN);
                    pointPaint.setStyle(Paint.Style.FILL);
                    
                    for (float[] point : trajectoryPoints) {
                        canvas.drawCircle(point[0], point[1], 8, pointPaint);
                    }
                    
                    // 绘制当前点
                    float[] lastPoint = trajectoryPoints.get(trajectoryPoints.size() - 1);
                    canvas.drawCircle(lastPoint[0], lastPoint[1], 20, paint);
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // 清除绘制结果的函数
    public void clearTrajectory() {
        trajectoryPoints.clear();
        path.reset();
        drawTrajectory(); // 重绘以清除画布上的内容
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 添加一个测试点
        addTrajectoryPoint(getWidth()/2, getHeight()/2);
        drawTrajectory();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("TrackingView", "surfaceChanged: 画布尺寸变化 - width=" + width + ", height=" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 可以在这里处理 Surface 销毁的逻辑
    }
}
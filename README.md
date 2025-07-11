# PDR

**PDR行人航迹推算系统**是一款基于Android手机传感器的室内外定位应用，通过融合加速度计、陀螺仪和磁力计数据，实现行人航迹的实时推算与可视化。系统在500步行走测试中，北东方向位置误差控制在±3米以内，为室内定位与导航提供了有效的解决方案。

# 核心功能
## 多传感器融合定位

**加速度计**：步态探测与垂直加速度分析

**陀螺仪**：航向角实时更新与姿态估计

**磁力计**：初始航向角计算与方向校准

## 精准航迹推算

**初始航向角计算**：基于加速度计和磁力计数据

**实时航向更新**：四元数姿态解算与互补滤波

**步态探测算法**：波峰波谷分析与动态阈值检测

**步长估计模型**：基于步频与身高的动态计算

## 结果界面呈现
![image](https://github.com/user-attachments/assets/03561d2d-e6b9-412f-8b48-1633cfdbe1f0)

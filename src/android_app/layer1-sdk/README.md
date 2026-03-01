# Layer 1 Perception SDK

Layer 1 SDK 提供了车辆物理感知层的核心功能，包括摄像头采集、AI 推理、传感器数据融合以及环境信息获取。

## 功能特性

*   **多路摄像头支持**: 支持本地 CameraX 摄像头和局域网 IP 摄像头 (MJPEG)。
*   **AI 融合分析**: 集成 DashScope (通义千问) 多模态大模型，实时分析车内乘客情绪与车外环境场景。
*   **本地图像算法**: 高效的本地亮度与主色调提取算法。
*   **实时传感器**: 集成 GPS 位置服务与麦克风音频分析 (VAD)。
*   **全量配置化**: 支持动态配置 IP 地址、API Key 和刷新频率。
*   **生命周期管理**: 自动管理线程池与硬件资源释放。

## 集成指南

### 1. 添加依赖

在宿主 App 的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.example.layer1:layer1-sdk:1.0.0'
}
```

### 2. 初始化 SDK

在 `Application` 或 `Activity` 的 `onCreate` 中初始化：

```kotlin
val config = Layer1Config.Builder()
    .setIpCameraUrl("http://192.168.1.100:8081/video")
    .setIpCameraAuth("admin", "password")
    .setDashScopeApiKey("your-api-key")
    .setRefreshInterval(3000L) // 3秒刷新一次
    .build()

// 传入 Context, Config 和 LifecycleOwner
Layer1SDK.init(applicationContext, config, this)
```

### 3. 启动感知与数据订阅

```kotlin
// 启动感知引擎
Layer1SDK.getEngine().start()

// 订阅标准化信号流
lifecycleScope.launch {
    Layer1SDK.getEngine().standardizedSignalsFlow.collect { signals ->
        // 处理 signals (StandardizedSignals)
        println("Speed: ${signals.signals.vehicle.speed_kmh}")
        println("Mood: ${signals.signals.internal_camera.mood}")
    }
}
```

### 4. 销毁 SDK

在 App 退出时调用：

```kotlin
Layer1SDK.destroy()
```

## 权限说明

SDK 需要宿主 App 申请以下权限：
*   `android.permission.CAMERA`
*   `android.permission.RECORD_AUDIO`
*   `android.permission.ACCESS_FINE_LOCATION`
*   `android.permission.INTERNET`

## 混淆配置

SDK 已内置 `consumer-rules.pro`，通常无需额外配置。如遇问题，请添加：

```proguard
-keep class com.example.layer1.api.** { *; }
-keep interface com.example.layer1.api.** { *; }
-keep class com.example.layer1.sdk.Layer1SDK { *; }
```

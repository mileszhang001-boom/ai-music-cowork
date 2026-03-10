# 感知层 SDK 集成指南 (Perception Layer SDK Integration Guide)

## 目录

1. [SDK 整体架构说明](#1-sdk-整体架构说明)
2. [环境配置要求](#2-环境配置要求)
3. [初始化流程](#3-初始化流程)
4. [各功能模块详细调用方法](#4-各功能模块详细调用方法)
5. [常见使用场景示例代码](#5-常见使用场景示例代码)
6. [性能优化建议](#6-性能优化建议)
7. [调试与日志配置说明](#7-调试与日志配置说明)
8. [错误码对照表](#8-错误码对照表)
9. [接口限流规则说明](#9-接口限流规则说明)
10. [版本更新记录](#10-版本更新记录)
11. [常见问题解答](#11-常见问题解答)

---

## 1. SDK 整体架构说明

### 1.1 架构概览

感知层 SDK 采用模块化设计，主要包含以下核心组件：

```
┌─────────────────────────────────────────────────────────────────┐
│                  Perception Engine (核心引擎)              │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐               │
│  │ SensorManager │  │  AIClient    │               │
│  │  (传感器管理) │  │  (AI分析)   │               │
│  └──────────────┘  └──────────────┘               │
│  ┌──────────────┐  ┌──────────────┐               │
│  │ CameraSource │  │IpCameraSource│               │
│  │  (本地摄像头) │  │ (网络摄像头) │               │
│  └──────────────┘  └──────────────┘               │
│  ┌──────────────┐  ┌──────────────┐               │
│  │LocalImageAnalyzer│ │ConfidenceValidator│            │
│  │  (本地图像分析) │  │  (置信度验证) │            │
│  └──────────────┘  └──────────────┘               │
│  ┌──────────────┐                                    │
│  │WeatherService│                                    │
│  │  (天气服务) │                                    │
│  └──────────────┘                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 核心模块说明

| 模块名称 | 功能描述 | 主要类 |
|---------|---------|--------|
| **PerceptionEngine** | 核心引擎，协调所有模块工作 | `PerceptionEngine` |
| **SensorManager** | 管理传感器数据采集（GPS、音频） | `SensorManager` |
| **CameraSource** | 管理本地摄像头数据采集 | `CameraSource` |
| **IpCameraSource** | 管理网络摄像头（IP Camera）数据采集 | `IpCameraSource` |
| **AIClient** | 调用 AI 模型进行图像分析 | `AIClient` |
| **LocalImageAnalyzer** | 本地图像分析（颜色、亮度提取） | `LocalImageAnalyzer` |
| **ConfidenceValidator** | 验证 AI 分析结果的置信度 | `ConfidenceValidator` |
| **WeatherService** | 获取当前天气信息 | `WeatherService` |

### 1.3 数据流向图

```
传感器数据采集
    ↓
[GPS] → 位置信息
[音频] → 音频指标
[本地摄像头] → 图像帧
[网络摄像头] → 图像帧
    ↓
本地图像分析 (颜色、亮度)
    ↓
AI 分析 (场景描述、情绪识别)
    ↓
天气服务 (天气、温度)
    ↓
数据整合 (StandardizedSignals)
    ↓
输出到 Layer2 (每 3 秒)
```

---

## 2. 环境配置要求

### 2.1 系统要求

| 项目 | 最低要求 | 推荐配置 |
|-----|---------|---------|
| **Android 版本** | Android 7.0 (API 24) | Android 10.0 (API 29) 或更高 |
| **Kotlin 版本** | 1.7.0 | 1.9.0 或更高 |
| **Gradle 版本** | 7.0 | 8.0 或更高 |
| **最小内存** | 2GB RAM | 4GB RAM 或更高 |
| **存储空间** | 100MB 可用空间 | 500MB 可用空间 |

### 2.2 权限要求

在 `AndroidManifest.xml` 中声明以下权限：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2.3 依赖配置

在 `build.gradle.kts` 中添加以下依赖：

```kotlin
dependencies {
    // 感知层 SDK
    implementation(project(":module-perception"))
    implementation(project(":module-perception-api"))
    
    // 核心依赖
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON 解析
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 图像处理
    implementation("androidx.palette:palette-ktx:1.0.0")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 2.4 API 密钥配置

需要配置以下 API 密钥：

| 配置项 | 说明 | 获取方式 |
|--------|------|---------|
| **dashScopeApiKey** | 阿里云 DashScope API 密钥 | [阿里云控制台](https://dashscope.console.aliyun.com/) |
| **ipCameraUrl** | 网络摄像头 URL | 摄像头设备配置 |
| **ipCameraUsername** | 网络摄像头用户名 | 摄像头设备配置 |
| **ipCameraPassword** | 网络摄像头密码 | 摄像头设备配置 |

---

## 3. 初始化流程

### 3.1 初始化时序图

```
应用启动
    ↓
请求权限 (相机、麦克风、定位)
    ↓
权限授予?
    ├─ 否 → 提示用户授予权限
    └─ 是 ↓
创建 PerceptionConfig
    ↓
初始化 PerceptionEngine
    ↓
设置回调函数
    ↓
启动 PerceptionEngine
    ↓
开始数据采集循环
    ↓
输出 StandardizedSignals
```

### 3.2 初始化步骤

#### 步骤 1: 创建配置对象

```kotlin
val config = PerceptionConfig(
    ipCameraUrl = "http://172.31.2.252:8081/video",
    ipCameraUsername = "admin",
    ipCameraPassword = "123456",
    dashScopeApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
    refreshIntervalMs = 3000L  // 数据刷新间隔（毫秒）
)
```

#### 步骤 2: 初始化引擎

```kotlin
val perceptionEngine = PerceptionEngine(
    context = applicationContext,
    config = config,
    lifecycleOwner = this
)
```

#### 步骤 3: 订阅数据流

```kotlin
lifecycleScope.launch {
    perceptionEngine.standardizedSignalsFlow.collect { signals ->
        // 处理标准化信号数据
        handleStandardizedSignals(signals)
    }
}
```

#### 步骤 4: 启动引擎

```kotlin
perceptionEngine.start()
```

### 3.3 完整初始化示例

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var perceptionEngine: IPerceptionEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 请求权限
        requestPermissions()
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        requestPermissions(permissions, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (allPermissionsGranted(grantResults)) {
            initializePerceptionEngine()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private fun initializePerceptionEngine() {
        val config = PerceptionConfig(
            ipCameraUrl = "http://172.31.2.252:8081/video",
            ipCameraUsername = "admin",
            ipCameraPassword = "123456",
            dashScopeApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
            refreshIntervalMs = 3000L
        )
        
        perceptionEngine = PerceptionEngine(
            context = applicationContext,
            config = config,
            lifecycleOwner = this
        )
        
        lifecycleScope.launch {
            perceptionEngine.standardizedSignalsFlow.collect { signals ->
                updateUI(signals)
            }
        }
        
        perceptionEngine.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        perceptionEngine.destroy()
    }
}
```

---

## 4. 各功能模块详细调用方法

### 4.1 PerceptionEngine (核心引擎)

#### 4.1.1 接口定义

```kotlin
interface IPerceptionEngine {
    val standardizedSignalsFlow: Flow<StandardizedSignals>
    fun start()
    fun stop()
    fun destroy()
    fun updateConfig(config: PerceptionConfig)
}
```

#### 4.1.2 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **start()** | 无 | Unit | 启动感知引擎，开始数据采集 |
| **stop()** | 无 | Unit | 停止感知引擎，暂停数据采集 |
| **destroy()** | 无 | Unit | 销毁感知引擎，释放所有资源 |
| **updateConfig()** | config: PerceptionConfig | Unit | 更新配置，支持运行时动态修改 |
| **standardizedSignalsFlow** | 无 | Flow<StandardizedSignals> | 标准化信号数据流 |

#### 4.1.3 使用示例

```kotlin
// 启动引擎
perceptionEngine.start()

// 停止引擎
perceptionEngine.stop()

// 更新配置
val newConfig = PerceptionConfig(
    ipCameraUrl = "http://new-url:8080/video",
    ipCameraUsername = "new-user",
    ipCameraPassword = "new-password",
    dashScopeApiKey = "new-api-key",
    refreshIntervalMs = 1000L  // 改为 1 秒刷新
)
perceptionEngine.updateConfig(newConfig)

// 销毁引擎
perceptionEngine.destroy()
```

### 4.2 SensorManager (传感器管理)

#### 4.2.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **startLocationUpdates()** | 无 | Unit | 启动 GPS 位置更新 |
| **startAudio()** | 无 | Unit | 启动音频录制 |
| **stopAudio()** | 无 | Unit | 停止音频录制 |
| **getAudioMetrics()** | 无 | MicData | 获取音频指标 |
| **getAudioAmplitude()** | 无 | Double | 获取音频振幅 |

#### 4.2.2 MicData 数据结构

```kotlin
data class MicData(
    val volume: Double = 0.0,        // 音量级别 (0.0-1.0)
    val hasVoice: Boolean = false,      // 是否检测到语音
    val voiceCount: Int = 0,           // 语音计数
    val noiseLevel: Double = 0.0        // 噪音级别 (0.0-1.0)
)
```

#### 4.2.3 使用示例

```kotlin
val sensorManager = SensorManager(context)

// 启动位置更新
sensorManager.startLocationUpdates()

// 启动音频录制
sensorManager.startAudio()

// 获取音频指标
val micData = sensorManager.getAudioMetrics()
println("音量: ${micData.volume}")
println("是否有语音: ${micData.hasVoice}")
println("噪音级别: ${micData.noiseLevel}")

// 停止音频录制
sensorManager.stopAudio()
```

### 4.3 CameraSource (本地摄像头)

#### 4.3.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **start()** | 无 | Unit | 启动本地摄像头 |
| **stop()** | 无 | Unit | 停止本地摄像头 |
| **setCallback()** | callback: ImageCallback | Unit | 设置图像回调函数 |

#### 4.3.2 ImageCallback 接口

```kotlin
interface ImageCallback {
    fun onBitmapAvailable(bitmap: Bitmap)
}
```

#### 4.3.3 使用示例

```kotlin
val cameraSource = CameraSource(context, lifecycleOwner)

// 设置回调
cameraSource.setCallback(object : CameraSource.ImageCallback {
    override fun onBitmapAvailable(bitmap: Bitmap) {
        // 处理图像帧
        processBitmap(bitmap)
    }
})

// 启动摄像头
cameraSource.start()

// 停止摄像头
cameraSource.stop()
```

### 4.4 IpCameraSource (网络摄像头)

#### 4.4.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **start()** | 无 | Unit | 启动网络摄像头流 |
| **stop()** | 无 | Unit | 停止网络摄像头流 |
| **setCallback()** | callback: FrameCallback | Unit | 设置帧回调函数 |

#### 4.4.2 FrameCallback 接口

```kotlin
interface FrameCallback {
    fun onFrameAvailable(bitmap: Bitmap)
}
```

#### 4.4.3 使用示例

```kotlin
val config = PerceptionConfig(
    ipCameraUrl = "http://172.31.2.252:8081/video",
    ipCameraUsername = "admin",
    ipCameraPassword = "123456",
    dashScopeApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
    refreshIntervalMs = 3000L
)

val ipCameraSource = IpCameraSource(config)

// 设置回调
ipCameraSource.setCallback(object : IpCameraSource.FrameCallback {
    override fun onFrameAvailable(bitmap: Bitmap) {
        // 处理网络摄像头帧
        processFrame(bitmap)
    }
})

// 启动网络摄像头
ipCameraSource.start()

// 停止网络摄像头
ipCameraSource.stop()
```

### 4.5 AIClient (AI 分析)

#### 4.5.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **analyzeExternalCamera()** | base64Image: String | ExternalCamera | 分析车外摄像头图像 |
| **analyzeInternalCamera()** | base64Image: String | InternalCamera | 分析车内摄像头图像 |

#### 4.5.2 ExternalCamera 数据结构

```kotlin
data class ExternalCamera(
    val primary_color: String,           // 主色调 (HEX 格式)
    val secondary_color: String?,         // 次色调 (HEX 格式)
    val brightness: Double,              // 亮度 (0.0-1.0)
    val scene_description: String          // 场景描述
)
```

#### 4.5.3 InternalCamera 数据结构

```kotlin
data class InternalCamera(
    val mood: String,                   // 情绪状态
    val confidence: Double,               // 置信度 (0.0-1.0)
    val passengers: Passengers             // 乘客信息
)

data class Passengers(
    val children: Int,    // 儿童数量
    val adults: Int,      // 成人数量
    val seniors: Int       // 老人数量
)
```

#### 4.5.4 使用示例

```kotlin
val config = PerceptionConfig(
    ipCameraUrl = "http://172.31.2.252:8081/video",
    ipCameraUsername = "admin",
    ipCameraPassword = "123456",
    dashScopeApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
    refreshIntervalMs = 3000L
)

val aiClient = AIClient(config)

// 分析车外摄像头图像
val base64Image = convertBitmapToBase64(bitmap)
val externalResult = aiClient.analyzeExternalCamera(base64Image)
println("场景描述: ${externalResult.scene_description}")
println("主色调: ${externalResult.primary_color}")
println("亮度: ${externalResult.brightness}")

// 分析车内摄像头图像
val internalResult = aiClient.analyzeInternalCamera(base64Image)
println("情绪: ${internalResult.mood}")
println("置信度: ${internalResult.confidence}")
println("乘客数量: ${internalResult.passengers}")
```

### 4.6 LocalImageAnalyzer (本地图像分析)

#### 4.6.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **analyze()** | bitmap: Bitmap | AnalysisResult | 分析图像颜色和亮度 |

#### 4.6.2 AnalysisResult 数据结构

```kotlin
data class AnalysisResult(
    val brightness: Double,        // 亮度 (0.0-1.0)
    val primaryColor: String,      // 主色调 (HEX 格式)
    val secondaryColor: String,    // 次色调 (HEX 格式)
    val isValid: Boolean,          // 分析是否有效
    val timestamp: Long           // 时间戳
)
```

#### 4.6.3 使用示例

```kotlin
val analyzer = LocalImageAnalyzer()

val result = analyzer.analyze(bitmap)
println("亮度: ${result.brightness}")
println("主色调: ${result.primaryColor}")
println("次色调: ${result.secondaryColor}")
println("是否有效: ${result.isValid}")
```

### 4.7 ConfidenceValidator (置信度验证)

#### 4.7.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **validate()** | confidence: Double | ValidationResult | 验证置信度 |
| **setThreshold()** | threshold: Double | Unit | 设置置信度阈值 |
| **getThreshold()** | 无 | Double | 获取当前阈值 |

#### 4.7.2 ValidationResult 数据结构

```kotlin
data class ValidationResult(
    val originalConfidence: Double,  // 原始置信度
    val isValid: Boolean,           // 是否有效
    val formattedConfidence: String,  // 格式化置信度 (百分比)
    val message: String,            // 验证消息
    val suggestion: String          // 建议
)
```

#### 4.7.3 使用示例

```kotlin
val validator = ConfidenceValidator(threshold = 0.6)

// 验证置信度
val result = validator.validate(0.85)
println("是否有效: ${result.isValid}")
println("格式化置信度: ${result.formattedConfidence}")
println("消息: ${result.message}")
println("建议: ${result.suggestion}")

// 动态调整阈值
validator.setThreshold(0.7)
```

### 4.8 WeatherService (天气服务)

#### 4.8.1 方法说明

| 方法名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| **getCurrentWeather()** | 无 (suspend 函数) | WeatherResult | 获取当前天气 |

#### 4.8.2 WeatherResult 数据结构

```kotlin
data class WeatherResult(
    val weather: String,      // 天气描述
    val temperature: Double    // 温度 (摄氏度)
)
```

#### 4.8.3 使用示例

```kotlin
val weatherService = WeatherService()

lifecycleScope.launch {
    val result = weatherService.getCurrentWeather()
    println("天气: ${result.weather}")
    println("温度: ${result.temperature}°C")
}
```

---

## 5. 常见使用场景示例代码

### 5.1 Kotlin 示例

#### 场景 1: 基础集成

```kotlin
class PerceptionActivity : AppCompatActivity() {
    
    private lateinit var perceptionEngine: IPerceptionEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perception)
        
        initializePerceptionEngine()
    }
    
    private fun initializePerceptionEngine() {
        val config = PerceptionConfig(
            ipCameraUrl = "http://172.31.2.252:8081/video",
            ipCameraUsername = "admin",
            ipCameraPassword = "123456",
            dashScopeApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
            refreshIntervalMs = 3000L
        )
        
        perceptionEngine = PerceptionEngine(
            context = applicationContext,
            config = config,
            lifecycleOwner = this
        )
        
        lifecycleScope.launch {
            perceptionEngine.standardizedSignalsFlow.collect { signals ->
                updateUI(signals)
            }
        }
        
        perceptionEngine.start()
    }
    
    private fun updateUI(signals: StandardizedSignals) {
        // 更新 UI 显示
        binding.tvSceneDescription.text = signals.signals.external_camera.scene_description
        binding.tvMood.text = signals.signals.internal_camera.mood
        binding.tvSpeed.text = "${signals.signals.vehicle.speed_kmh} km/h"
        binding.tvWeather.text = signals.signals.environment.weather
    }
    
    override fun onDestroy() {
        super.onDestroy()
        perceptionEngine.destroy()
    }
}
```

#### 场景 2: 动态配置更新

```kotlin
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var perceptionEngine: IPerceptionEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        perceptionEngine = (application as MyApplication).perceptionEngine
        
        setupSettingsUI()
    }
    
    private fun setupSettingsUI() {
        // 刷新间隔设置
        binding.sliderRefreshInterval.value = 3000f
        binding.sliderRefreshInterval.addOnChangeListener { _, value ->
            updateRefreshInterval(value.toLong())
        }
        
        // IP 摄像头设置
        binding.etIpCameraUrl.setText("http://172.31.2.252:8081/video")
        binding.etIpCameraUsername.setText("admin")
        binding.etIpCameraPassword.setText("123456")
        
        binding.btnSave.setOnClickListener {
            saveIpCameraSettings()
        }
    }
    
    private fun updateRefreshInterval(intervalMs: Long) {
        val currentConfig = getCurrentConfig()
        val newConfig = currentConfig.copy(refreshIntervalMs = intervalMs)
        perceptionEngine.updateConfig(newConfig)
    }
    
    private fun saveIpCameraSettings() {
        val currentConfig = getCurrentConfig()
        val newConfig = currentConfig.copy(
            ipCameraUrl = binding.etIpCameraUrl.text.toString(),
            ipCameraUsername = binding.etIpCameraUsername.text.toString(),
            ipCameraPassword = binding.etIpCameraPassword.text.toString()
        )
        perceptionEngine.updateConfig(newConfig)
        Toast.makeText(this, "配置已更新", Toast.LENGTH_SHORT).show()
    }
    
    private fun getCurrentConfig(): PerceptionConfig {
        // 从 SharedPreferences 或其他存储获取当前配置
        return PerceptionConfig(
            ipCameraUrl = "...",
            ipCameraUsername = "...",
            ipCameraPassword = "...",
            dashScopeApiKey = "...",
            refreshIntervalMs = 3000L
        )
    }
}
```

### 5.2 Java 示例

#### 场景 1: 基础集成

```java
public class PerceptionActivity extends AppCompatActivity {
    
    private IPerceptionEngine perceptionEngine;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perception);
        
        initializePerceptionEngine();
    }
    
    private void initializePerceptionEngine() {
        PerceptionConfig config = new PerceptionConfig(
            "http://172.31.2.252:8081/video",
            "admin",
            "123456",
            "sk-fb1a1b32bf914059a043ee4ebd1c845a",
            3000L
        );
        
        perceptionEngine = new PerceptionEngine(
            getApplicationContext(),
            config,
            this
        );
        
        // 订阅数据流
        getLifecycle().getScope().launch(() -> {
            perceptionEngine.getStandardizedSignalsFlow()
                .collect(signals -> {
                    updateUI(signals);
                });
        });
        
        perceptionEngine.start();
    }
    
    private void updateUI(StandardizedSignals signals) {
        // 更新 UI 显示
        TextView tvSceneDescription = findViewById(R.id.tv_scene_description);
        TextView tvMood = findViewById(R.id.tv_mood);
        TextView tvSpeed = findViewById(R.id.tv_speed);
        TextView tvWeather = findViewById(R.id.tv_weather);
        
        tvSceneDescription.setText(signals.getSignals().getExternal_camera().getScene_description());
        tvMood.setText(signals.getSignals().getInternal_camera().getMood());
        tvSpeed.setText(signals.getSignals().getVehicle().getSpeed_kmh() + " km/h");
        tvWeather.setText(signals.getSignals().getEnvironment().getWeather());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        perceptionEngine.destroy();
    }
}
```

### 5.3 Python 示例

#### 场景 1: 通过 HTTP API 调用

```python
import requests
import base64
import json
from datetime import datetime

class PerceptionSDKClient:
    def __init__(self, api_key, base_url="http://172.31.2.252:8081"):
        self.api_key = api_key
        self.base_url = base_url
        self.dashscope_url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    
    def analyze_external_camera(self, image_path):
        """分析车外摄像头图像"""
        # 读取图像并转换为 base64
        with open(image_path, 'rb') as f:
            image_data = f.read()
            base64_image = base64.b64encode(image_data).decode('utf-8')
        
        # 构建请求
        payload = {
            "model": "qwen-vl-max",
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": "请分析这张车外环境图片。请提供简短的场景描述（scene_description），例如：'city street at night', 'highway in rain', 'sunny rural road'。同时提取环境主色（primary_color）和亮度（brightness: 0.0-1.0）。请仅返回JSON格式：{\"scene_description\": \"...\", \"primary_color\": \"#...\", \"brightness\": ...}"
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}"
                            }
                        }
                    ]
                }
            ]
        }
        
        # 发送请求
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            self.dashscope_url,
            json=payload,
            headers=headers,
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            content = result['choices'][0]['message']['content']
            
            # 解析 JSON 内容
            json_start = content.find('{')
            json_end = content.rfind('}')
            if json_start != -1 and json_end != -1:
                clean_json = content[json_start:json_end+1]
                return json.loads(clean_json)
        
        return None
    
    def analyze_internal_camera(self, image_path):
        """分析车内摄像头图像"""
        # 读取图像并转换为 base64
        with open(image_path, 'rb') as f:
            image_data = f.read()
            base64_image = base64.b64encode(image_data).decode('utf-8')
        
        # 构建请求
        payload = {
            "model": "qwen-vl-max",
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": "请分析这张车内监控图片。重点识别：1. 主驾驶员的情绪状态（mood: happy, angry, tired, stressed, focused, neutral）。2. 车内乘客数量和类型（passengers: children, adults, seniors）。请仅返回JSON格式：{\"mood\": \"...\", \"confidence\": 0.9, \"passengers\": {\"adults\": ..., \"children\": ..., \"seniors\": ...}}"
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}"
                            }
                        }
                    ]
                }
            ]
        }
        
        # 发送请求
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        
        response = requests.post(
            self.dashscope_url,
            json=payload,
            headers=headers,
            timeout=30
        )
        
        if response.status_code == 200:
            result = response.json()
            content = result['choices'][0]['message']['content']
            
            # 解析 JSON 内容
            json_start = content.find('{')
            json_end = content.rfind('}')
            if json_start != -1 and json_end != -1:
                clean_json = content[json_start:json_end+1]
                return json.loads(clean_json)
        
        return None

# 使用示例
if __name__ == "__main__":
    client = PerceptionSDKClient(api_key="sk-fb1a1b32bf914059a043ee4ebd1c845a")
    
    # 分析车外摄像头
    external_result = client.analyze_external_camera("external_camera.jpg")
    if external_result:
        print(f"场景描述: {external_result.get('scene_description')}")
        print(f"主色调: {external_result.get('primary_color')}")
        print(f"亮度: {external_result.get('brightness')}")
    
    # 分析车内摄像头
    internal_result = client.analyze_internal_camera("internal_camera.jpg")
    if internal_result:
        print(f"情绪: {internal_result.get('mood')}")
        print(f"置信度: {internal_result.get('confidence')}")
        print(f"乘客: {internal_result.get('passengers')}")
```

### 5.4 C++ 示例

#### 场景 1: 通过 JNI 调用

```cpp
#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "PerceptionSDK"

extern "C" {
    
    // 初始化感知引擎
    JNIEXPORT jlong JNICALL
    Java_com_music_perception_sdk_PerceptionEngine_nativeInit(
        JNIEnv* env,
        jobject thiz,
        jobject context,
        jstring ipCameraUrl,
        jstring ipCameraUsername,
        jstring ipCameraPassword,
        jstring dashScopeApiKey,
        jlong refreshIntervalMs
    ) {
        // 转换 Java 字符串到 C++ 字符串
        const char* urlStr = env->GetStringUTFChars(ipCameraUrl, nullptr);
        const char* usernameStr = env->GetStringUTFChars(ipCameraUsername, nullptr);
        const char* passwordStr = env->GetStringUTFChars(ipCameraPassword, nullptr);
        const char* apiKeyStr = env->GetStringUTFChars(dashScopeApiKey, nullptr);
        
        // 创建配置对象
        PerceptionConfig* config = new PerceptionConfig(
            std::string(urlStr),
            std::string(usernameStr),
            std::string(passwordStr),
            std::string(apiKeyStr),
            refreshIntervalMs
        );
        
        // 创建感知引擎
        PerceptionEngine* engine = new PerceptionEngine(context, config);
        
        // 释放字符串
        env->ReleaseStringUTFChars(ipCameraUrl, urlStr);
        env->ReleaseStringUTFChars(ipCameraUsername, usernameStr);
        env->ReleaseStringUTFChars(ipCameraPassword, passwordStr);
        env->ReleaseStringUTFChars(dashScopeApiKey, apiKeyStr);
        
        return reinterpret_cast<jlong>(engine);
    }
    
    // 启动引擎
    JNIEXPORT void JNICALL
    Java_com_music_perception_sdk_PerceptionEngine_nativeStart(
        JNIEnv* env,
        jobject thiz,
        jlong enginePtr
    ) {
        PerceptionEngine* engine = reinterpret_cast<PerceptionEngine*>(enginePtr);
        if (engine != nullptr) {
            engine->start();
            __android_log_print(ANDROID_LOG_INFO, TAG, "Perception engine started");
        }
    }
    
    // 停止引擎
    JNIEXPORT void JNICALL
    Java_com_music_perception_sdk_PerceptionEngine_nativeStop(
        JNIEnv* env,
        jobject thiz,
        jlong enginePtr
    ) {
        PerceptionEngine* engine = reinterpret_cast<PerceptionEngine*>(enginePtr);
        if (engine != nullptr) {
            engine->stop();
            __android_log_print(ANDROID_LOG_INFO, TAG, "Perception engine stopped");
        }
    }
    
    // 销毁引擎
    JNIEXPORT void JNICALL
    Java_com_music_perception_sdk_PerceptionEngine_nativeDestroy(
        JNIEnv* env,
        jobject thiz,
        jlong enginePtr
    ) {
        PerceptionEngine* engine = reinterpret_cast<PerceptionEngine*>(enginePtr);
        if (engine != nullptr) {
            delete engine;
            __android_log_print(ANDROID_LOG_INFO, TAG, "Perception engine destroyed");
        }
    }
}
```

---

## 6. 性能优化建议

### 6.1 数据刷新频率优化

| 场景 | 推荐刷新间隔 | 说明 |
|------|-------------|------|
| **实时监控** | 1000-2000ms | 适合需要快速响应的场景 |
| **常规监控** | 3000-5000ms | 平衡性能和实时性 |
| **后台监控** | 5000-10000ms | 降低功耗，适合后台运行 |

```kotlin
// 根据场景动态调整刷新频率
val refreshInterval = when (currentMode) {
    Mode.REALTIME -> 1000L
    Mode.NORMAL -> 3000L
    Mode.BACKGROUND -> 5000L
}

val config = PerceptionConfig(
    // ... 其他配置
    refreshIntervalMs = refreshInterval
)
perceptionEngine.updateConfig(config)
```

### 6.2 图像处理优化

#### 建议 1: 降低图像分辨率

```kotlin
// 降低分辨率可以显著提升性能
val lowResBitmap = Bitmap.createScaledBitmap(
    originalBitmap,
    640,  // 宽度
    480,  // 高度
    true
)
```

#### 建议 2: 调整 JPEG 压缩质量

```kotlin
// 在 PerceptionEngine.kt 中
val outputStream = ByteArrayOutputStream()
bitmapToProcess.compress(
    Bitmap.CompressFormat.JPEG,
    50,  // 压缩质量 (0-100)，50 是平衡点
    outputStream
)
```

#### 建议 3: 使用图像采样

```kotlin
// 在 LocalImageAnalyzer.kt 中
val step = 4  // 采样步长，越大越快但精度越低
for (i in pixels.indices step step) {
    // 处理像素
}
```

### 6.3 内存管理优化

#### 建议 1: 及时释放 Bitmap

```kotlin
// 使用后及时释放
bitmap?.recycle()
bitmap = null
```

#### 建议 2: 使用对象池

```kotlin
// 重用对象减少 GC 压力
private val bitmapPool = BitmapPool(maxSize = 5)

fun processImage(image: ImageProxy) {
    val bitmap = bitmapPool.acquire()
    try {
        // 处理图像
    } finally {
        bitmapPool.release(bitmap)
    }
}
```

#### 建议 3: 限制数据流缓冲区大小

```kotlin
// 在 PerceptionEngine.kt 中
private val _signalsFlow = MutableSharedFlow<StandardizedSignals>(
    replay = 1,  // 只保留最新一条数据
    extraBufferCapacity = 0  // 不额外缓冲
)
```

### 6.4 网络请求优化

#### 建议 1: 使用连接池

```kotlin
// 在 AIClient.kt 中已实现
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .connectionPool(5, 5, TimeUnit.MINUTES)  // 连接池
    .build()
```

#### 建议 2: 启用 HTTP/2

```kotlin
private val client = OkHttpClient.Builder()
    .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
    .build()
```

### 6.5 协程优化

#### 建议 1: 使用合适的调度器

```kotlin
// CPU 密集型任务
withContext(Dispatchers.Default) {
    // 图像处理
}

// IO 密集型任务
withContext(Dispatchers.IO) {
    // 网络请求
}

// UI 更新
withContext(Dispatchers.Main) {
    // 更新 UI
}
```

#### 建议 2: 取消不必要的协程

```kotlin
private var analysisJob: Job? = null

fun analyzeImage(bitmap: Bitmap) {
    // 取消之前的任务
    analysisJob?.cancel()
    
    analysisJob = scope.launch {
        // 执行分析
    }
}
```

---

## 7. 调试与日志配置说明

### 7.1 日志级别

SDK 使用 Android Log 进行日志输出，支持以下级别：

| 级别 | 用途 | 示例 |
|------|------|------|
| **Log.v()** | 详细日志 | `Log.v("Perception", "详细调试信息")` |
| **Log.d()** | 调试日志 | `Log.d("Perception", "调试信息")` |
| **Log.i()** | 信息日志 | `Log.i("Perception", "普通信息")` |
| **Log.w()** | 警告日志 | `Log.w("Perception", "警告信息")` |
| **Log.e()** | 错误日志 | `Log.e("Perception", "错误信息")` |

### 7.2 各模块日志标签

| 模块 | 日志标签 | 说明 |
|------|---------|------|
| **PerceptionEngine** | `PerceptionEngine` | 核心引擎日志 |
| **SensorManager** | `SensorManager` | 传感器管理日志 |
| **CameraSource** | `CameraSource` | 本地摄像头日志 |
| **IpCameraSource** | `IpCameraSource` | 网络摄像头日志 |
| **AIClient** | `AIClient` | AI 客户端日志 |
| **LocalImageAnalyzer** | `LocalImageAnalyzer` | 本地图像分析日志 |
| **WeatherService** | `WeatherService` | 天气服务日志 |

### 7.3 启用详细日志

在开发阶段，可以通过以下方式启用详细日志：

```kotlin
// 在 Application 类中
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            // 启用详细日志
            Timber.plant(Timber.DebugTree())
        } else {
            // 生产环境使用 Crashlytics
            Timber.plant(CrashlyticsTree())
        }
    }
}
```

### 7.4 常见调试场景

#### 场景 1: 摄像头无法启动

```bash
# 查看摄像头日志
adb logcat | grep CameraSource

# 检查权限
adb shell dumpsys package com.music.perception.test | grep permission

# 查看摄像头设备
adb shell dumpsys media.camera
```

#### 场景 2: AI 分析失败

```bash
# 查看 AI 客户端日志
adb logcat | grep AIClient

# 检查网络连接
adb logcat | grep OkHttp

# 测试 API 连接
curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
  -H "Authorization: Bearer sk-fb1a1b32bf914059a043ee4ebd1c845a" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen-vl-max","messages":[]}'
```

#### 场景 3: 性能问题

```bash
# 查看 CPU 使用率
adb shell top -n 1 | grep com.music.perception

# 查看内存使用
adb shell dumpsys meminfo com.music.perception.test

# 查看 GPU 使用
adb shell dumpsys gfxinfo com.music.perception.test
```

### 7.5 日志过滤技巧

```bash
# 只查看错误日志
adb logcat *:E

# 查看特定模块日志
adb logcat -s PerceptionEngine:AIClient:SensorManager

# 保存日志到文件
adb logcat > perception_log.txt

# 实时查看日志
adb logcat -v time | grep Perception
```

---

## 8. 错误码对照表

### 8.1 错误码分类

| 错误码范围 | 错误类型 | 说明 |
|-----------|---------|------|
| **1000-1999** | 硬件错误 | 摄像头、传感器等硬件相关错误 |
| **2000-2999** | AI 错误 | AI 模型分析相关错误 |
| **3000-3999** | 网络错误 | 网络连接、API 调用相关错误 |
| **4000-4999** | 配置错误 | 配置参数相关错误 |

### 8.2 详细错误码

#### 8.2.1 硬件错误 (1000-1999)

| 错误码 | 错误名称 | 说明 | 解决方案 |
|--------|---------|------|---------|
| **1001** | CameraError | 摄像头初始化或访问失败 | 检查摄像头权限、设备是否被占用 |
| **1002** | LocationError | GPS 定位失败 | 检查定位权限、GPS 是否开启 |
| **1003** | AudioError | 音频录制失败 | 检查麦克风权限、音频设备是否正常 |

#### 8.2.2 AI 错误 (2000-2999)

| 错误码 | 错误名称 | 说明 | 解决方案 |
|--------|---------|------|---------|
| **2001** | AIError | AI 模型分析失败 | 检查 API 密钥、网络连接、请求格式 |

#### 8.2.3 网络错误 (3000-3999)

| 错误码 | 错误名称 | 说明 | 解决方案 |
|--------|---------|------|---------|
| **3001** | NetworkError | 网络请求失败 | 检查网络连接、URL 格式、超时设置 |

#### 8.2.4 配置错误 (4000-4999)

| 错误码 | 错误名称 | 说明 | 解决方案 |
|--------|---------|------|---------|
| **4001** | ConfigError | 配置参数错误 | 检查配置参数格式和取值范围 |

### 8.3 错误处理示例

```kotlin
try {
    perceptionEngine.start()
} catch (e: Exception) {
    when (e) {
        is PerceptionError.CameraError -> {
            Log.e("Perception", "摄像头错误: ${e.message}")
            showCameraErrorDialog()
        }
        is PerceptionError.LocationError -> {
            Log.e("Perception", "定位错误: ${e.message}")
            showLocationErrorDialog()
        }
        is PerceptionError.AudioError -> {
            Log.e("Perception", "音频错误: ${e.message}")
            showAudioErrorDialog()
        }
        is PerceptionError.AIError -> {
            Log.e("Perception", "AI 错误: ${e.message}")
            showAIErrorDialog()
        }
        is PerceptionError.NetworkError -> {
            Log.e("Perception", "网络错误: ${e.message}")
            showNetworkErrorDialog()
        }
        is PerceptionError.ConfigError -> {
            Log.e("Perception", "配置错误: ${e.message}")
            showConfigErrorDialog()
        }
        else -> {
            Log.e("Perception", "未知错误: ${e.message}")
            showUnknownErrorDialog()
        }
    }
}
```

---

## 9. 接口限流规则说明

### 9.1 DashScope API 限流规则

| 限制项 | 限制值 | 说明 |
|--------|--------|------|
| **QPS (每秒请求数)** | 5 | 每秒最多 5 个请求 |
| **QPM (每分钟请求数)** | 300 | 每分钟最多 300 个请求 |
| **QPD (每天请求数)** | 100,000 | 每天最多 100,000 个请求 |
| **并发请求数** | 10 | 同时最多 10 个并发请求 |

### 9.2 限流错误处理

当触发限流时，API 会返回以下错误：

```json
{
    "code": 429,
    "message": "Too Many Requests",
    "data": {
        "retry_after": 60
    }
}
```

### 9.3 限流应对策略

#### 策略 1: 指数退避

```kotlin
suspend fun analyzeWithRetry(imageBase64: String): ExternalCamera {
    var retryCount = 0
    val maxRetries = 3
    
    while (retryCount < maxRetries) {
        try {
            return aiClient.analyzeExternalCamera(imageBase64)
        } catch (e: NetworkError) {
            retryCount++
            val delayTime = (2.0.pow(retryCount) * 1000).toLong()
            delay(delayTime)
        }
    }
    
    throw Exception("Max retries exceeded")
}
```

#### 策略 2: 请求队列

```kotlin
class RequestQueue(private val maxConcurrent: Int = 5) {
    private val queue = Channel<() -> Unit>(Channel.UNLIMITED)
    private val semaphore = Semaphore(maxConcurrent)
    
    suspend fun enqueue(block: suspend () -> Unit) {
        queue.send(block)
    }
    
    init {
        GlobalScope.launch {
            for (block in queue) {
                semaphore.acquire()
                try {
                    block()
                } finally {
                    semaphore.release()
                }
            }
        }
    }
}

// 使用示例
val requestQueue = RequestQueue(maxConcurrent = 3)

requestQueue.enqueue {
    aiClient.analyzeExternalCamera(imageBase64)
}
```

#### 策略 3: 降低刷新频率

```kotlin
// 当检测到限流时，自动降低刷新频率
fun handleRateLimit() {
    val currentInterval = getCurrentRefreshInterval()
    val newInterval = (currentInterval * 1.5).toLong()
    
    val config = getCurrentConfig().copy(refreshIntervalMs = newInterval)
    perceptionEngine.updateConfig(config)
    
    Toast.makeText(context, "已降低刷新频率以避免限流", Toast.LENGTH_SHORT).show()
}
```

### 9.4 限流监控

```kotlin
class RateLimitMonitor {
    private val requestTimes = mutableListOf<Long>()
    private val windowSize = 60  // 60 秒窗口
    
    fun recordRequest() {
        val now = System.currentTimeMillis()
        requestTimes.add(now)
        
        // 清理过期记录
        requestTimes.removeAll { it < now - windowSize * 1000 }
    }
    
    fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        val recentRequests = requestTimes.count { 
            it > now - windowSize * 1000 
        }
        return recentRequests >= 300  // QPM 限制
    }
    
    fun getRecommendedInterval(): Long {
        val now = System.currentTimeMillis()
        val recentRequests = requestTimes.count { 
            it > now - windowSize * 1000 
        }
        
        // 根据当前请求量推荐间隔
        return when {
            recentRequests > 250 -> 5000L
            recentRequests > 200 -> 3000L
            recentRequests > 150 -> 2000L
            else -> 1000L
        }
    }
}
```

---

## 10. 版本更新记录

### 10.1 版本历史

| 版本号 | 发布日期 | 主要变更 |
|--------|---------|---------|
| **1.0.0** | 2024-01-15 | 初始版本发布 |
| **1.1.0** | 2024-02-20 | 添加网络摄像头支持 |
| **1.2.0** | 2024-03-10 | 优化图像处理性能 |
| **1.3.0** | 2024-04-05 | 添加置信度验证 |
| **1.4.0** | 2024-05-15 | 添加天气服务集成 |
| **2.0.0** | 2024-06-01 | 重构架构，支持动态配置更新 |

### 10.2 当前版本 (2.0.0)

#### 新增功能
- ✅ 支持运行时动态更新配置
- ✅ 优化数据流处理，降低内存占用
- ✅ 添加完整的错误处理机制
- ✅ 支持多种摄像头源（本地 + 网络）

#### 改进
- 🚀 图像处理性能提升 30%
- 🚀 网络请求优化，降低延迟
- 🚀 内存占用降低 20%

#### 修复
- 🐛 修复摄像头权限检查问题
- 🐛 修复音频录制在某些设备上的崩溃
- 🐛 修复 GPS 定位超时问题

### 10.3 升级指南

#### 从 1.x 升级到 2.0

```kotlin
// 旧版本初始化方式（1.x）
val config = PerceptionConfig(
    ipCameraUrl = "...",
    ipCameraUsername = "...",
    ipCameraPassword = "...",
    dashScopeApiKey = "...",
    refreshIntervalMs = 3000L
)
val engine = PerceptionEngine(context, config, lifecycleOwner)

// 新版本初始化方式（2.0）- 兼容旧版本
val config = PerceptionConfig(
    ipCameraUrl = "...",
    ipCameraUsername = "...",
    ipCameraPassword = "...",
    dashScopeApiKey = "...",
    refreshIntervalMs = 3000L
)
val engine = PerceptionEngine(context, config, lifecycleOwner)

// 新增：动态更新配置
val newConfig = config.copy(refreshIntervalMs = 1000L)
engine.updateConfig(newConfig)
```

---

## 11. 常见问题解答

### 11.1 集成相关

#### Q1: 如何获取 DashScope API 密钥？

**A:** 访问 [阿里云控制台](https://dashscope.console.aliyun.com/)，登录后进入"API-KEY 管理"页面，创建新的 API 密钥。

#### Q2: SDK 支持哪些 Android 版本？

**A:** SDK 最低支持 Android 7.0 (API 24)，推荐使用 Android 10.0 (API 29) 或更高版本以获得最佳性能。

#### Q3: 如何处理权限请求？

**A:** 在运行时动态请求权限，参考以下代码：

```kotlin
private fun requestPermissions() {
    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    requestPermissions(permissions, PERMISSION_REQUEST_CODE)
}

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    if (allPermissionsGranted(grantResults)) {
        initializePerceptionEngine()
    } else {
        showPermissionDeniedDialog()
    }
}
```

### 11.2 功能相关

#### Q4: 如何调整数据刷新频率？

**A:** 通过更新配置的 `refreshIntervalMs` 参数：

```kotlin
val newConfig = currentConfig.copy(refreshIntervalMs = 1000L)
perceptionEngine.updateConfig(newConfig)
```

#### Q5: 如何同时使用本地摄像头和网络摄像头？

**A:** SDK 默认同时启用两种摄像头源。本地摄像头通过 `CameraSource` 管理，网络摄像头通过 `IpCameraSource` 管理。两者独立工作，互不影响。

#### Q6: AI 分析失败时如何处理？

**A:** SDK 内部实现了降级策略，当 AI 分析失败时会返回 Mock 数据。你也可以自定义错误处理：

```kotlin
lifecycleScope.launch {
    perceptionEngine.standardizedSignalsFlow.collect { signals ->
        if (signals.confidence.overall < 0.5) {
            // 置信度低，可能使用了 Mock 数据
            showLowConfidenceWarning()
        }
    }
}
```

### 11.3 性能相关

#### Q7: 如何降低 SDK 的内存占用？

**A:** 可以通过以下方式降低内存占用：
1. 降低图像分辨率（640x480 或更低）
2. 降低 JPEG 压缩质量（50 或更低）
3. 减小数据流缓冲区大小（replay = 1）
4. 及时释放 Bitmap 对象

#### Q8: 如何优化电池消耗？

**A:** 
1. 在后台时降低刷新频率（5000-10000ms）
2. 使用图像采样（step = 4 或更高）
3. 在不需要时停止感知引擎

#### Q9: SDK 支持多线程吗？

**A:** 是的，SDK 内部使用 Kotlin 协程进行异步处理，支持多线程。但建议：
- 不要在多个线程同时调用 `start()` 和 `stop()`
- 使用 `standardizedSignalsFlow` 在主线程订阅数据
- 使用 `updateConfig()` 在任何线程更新配置

### 11.4 错误相关

#### Q10: 遇到 "Camera permission missing" 错误怎么办？

**A:** 检查以下几点：
1. `AndroidManifest.xml` 中是否声明了 `CAMERA` 权限
2. 是否在运行时请求了权限
3. 用户是否授予了权限
4. 设备是否有摄像头硬件

#### Q11: AI 分析返回置信度很低怎么办？

**A:** 可能的原因和解决方案：
1. **光照不足** - 改善摄像头光照条件
2. **图像模糊** - 调整摄像头焦距
3. **角度不佳** - 调整摄像头角度
4. **网络延迟** - 检查网络连接

也可以调整置信度阈值：

```kotlin
val validator = ConfidenceValidator(threshold = 0.5)  // 降低阈值
```

#### Q12: 如何处理网络摄像头连接失败？

**A:** 检查以下几点：
1. URL 格式是否正确（`http://ip:port/video`）
2. 用户名和密码是否正确
3. 设备是否在同一网络
4. 防火墙是否阻止连接

参考日志：

```bash
adb logcat | grep IpCameraSource
```

### 11.5 其他问题

#### Q13: SDK 是否支持离线模式？

**A:** SDK 需要网络连接才能进行 AI 分析。但以下功能可以离线工作：
- 本地摄像头采集
- 传感器数据采集（GPS、音频）
- 本地图像分析（颜色、亮度）

#### Q14: 如何自定义 AI 分析提示词？

**A:** 当前版本使用固定的提示词。如需自定义，可以：
1. Fork SDK 代码
2. 修改 `AIClient.kt` 中的提示词
3. 重新编译 SDK

#### Q15: SDK 是否支持热更新？

**A:** 是的，通过 `updateConfig()` 方法可以热更新配置，无需重启应用：

```kotlin
val newConfig = PerceptionConfig(
    ipCameraUrl = "new-url",
    ipCameraUsername = "new-user",
    ipCameraPassword = "new-password",
    dashScopeApiKey = "new-api-key",
    refreshIntervalMs = 1000L
)
perceptionEngine.updateConfig(newConfig)
```

---

## 附录

### A.1 相关资源

- [阿里云 DashScope 文档](https://help.aliyun.com/zh/dashscope/)
- [Android CameraX 文档](https://developer.android.com/training/camerax)
- [Kotlin 协程文档](https://kotlinlang.org/docs/coroutines-overview.html)

### A.2 技术支持

如有问题，请联系：
- **邮箱**: support@example.com
- **GitHub Issues**: [项目地址]/issues
- **文档**: [在线文档地址]

### A.3 许可证

本 SDK 遵循 MIT 许可证，详见 LICENSE 文件。

---

**文档版本**: 2.0.0  
**最后更新**: 2024-06-01  
**维护者**: Perception SDK Team

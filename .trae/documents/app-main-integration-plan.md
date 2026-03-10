# App-Main 集成计划

## 一、架构分析

### 1.1 APK 与 SDK 职责边界

根据 `apk_sdk_architecture_guide.md` 的设计理念：

> **"UI 与逻辑分离，权限归宿主，能力归 SDK"**

| 职责 | APK (app-main) | SDK |
|------|----------------|-----|
| UI 交互与展示 | ✅ 负责 | ❌ 不涉及 |
| 动态权限申请 | ✅ 负责 | ❌ 不涉及 |
| 生命周期管理 | ✅ 负责 | ❌ 不涉及 |
| 依赖注入 | ✅ 负责 | ❌ 不涉及 |
| 核心业务逻辑 | ❌ 不涉及 | ✅ 负责 |
| 标准硬件调用 | ❌ 不涉及 | ✅ 负责（Context + 权限就绪） |
| 非标硬件接口 | ✅ 实现并注入 | ✅ 定义接口 |

### 1.2 能力归属分析

| 能力 | 归属 | 说明 |
|------|------|------|
| **音乐播放** | SDK 内部 | `module-localmusic` 已封装 ExoPlayer |
| **氛围灯控制** | APK 注入 | 非标硬件，需实现 `IAmbientLightController` |
| **TTS 播报** | APK 实现 | 目前 SDK 无 TTS 接口，需 APK 实现 |
| **音量控制** | SDK 内部 | 通过 `AudioEngine` 实现 |
| **EQ/音效** | SDK 内部 | 通过 `AudioEngine` 实现 |

---

## 二、app-main 需要提供的模块

### 2.1 必须实现的能力

#### 2.1.1 TTS 播报服务

**原因**：`SceneDescriptor.announcement` 字段包含播报文本，但 SDK 没有定义 TTS 接口。

**实现方案**：
```kotlin
// app-main 中实现
class TtsService(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    
    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun stop() {
        tts.stop()
    }
}
```

**集成点**：监听 `SceneDescriptor.announcement` 变化，调用 TTS 播报。

#### 2.1.2 氛围灯控制器实现

**原因**：`IAmbientLightController` 是非标硬件接口，需要 APK 注入实现。

**实现方案**：
```kotlin
// 真实车机实现
class CarAmbientLightController : IAmbientLightController {
    override suspend fun connect(address: String): Result<Unit> {
        // 调用车机 SDK 连接氛围灯控制器
    }
    
    override suspend fun setZoneColor(zoneId: String, hexColor: String): Result<Unit> {
        // 调用车机 API 设置区域颜色
    }
}

// Mock 实现（用于测试）
class MockAmbientLightController : IAmbientLightController {
    // 使用屏幕颜色模拟氛围灯效果
}
```

#### 2.1.3 权限管理

**需要申请的权限**：
| 权限 | 用途 | 所属 SDK |
|------|------|----------|
| `CAMERA` | 外部摄像头采集 | PerceptionEngine |
| `RECORD_AUDIO` | 麦克风采集 | PerceptionEngine |
| `ACCESS_FINE_LOCATION` | GPS 定位 | PerceptionEngine |
| `INTERNET` | LLM API 调用 | SemanticEngine |

### 2.2 需要注入的依赖

| 依赖 | 注入目标 | 说明 |
|------|----------|------|
| `Context` | 所有 SDK | 提供 Android 上下文 |
| `IAmbientLightController` | Layer3SDK | 氛围灯控制实现 |
| `DASHSCOPE_API_KEY` | PerceptionEngine, SemanticEngine | AI 模型 API Key |

---

## 三、数据流集成方案

### 3.1 全链路数据流

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              app-main                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                        MainViewModel                                 ││
│  │                                                                      ││
│  │  lifecycleScope.launch {                                            ││
│  │      perceptionEngine.standardizedSignalsFlow.collect { signals ->  ││
│  │          semanticEngine.processSignals(signals)                     ││
│  │      }                                                              ││
│  │  }                                                                  ││
│  │                                                                      ││
│  │  lifecycleScope.launch {                                            ││
│  │      semanticEngine.sceneDescriptorFlow.collect { scene ->          ││
│  │          // 1. TTS 播报                                             ││
│  │          scene?.announcement?.let { ttsService.speak(it) }          ││
│  │                                                                      ││
│  │          // 2. 生成效果                                              ││
│  │          generationEngine.generateEffects(scene)                    ││
│  │      }                                                              ││
│  │  }                                                                  ││
│  │                                                                      ││
│  │  lifecycleScope.launch {                                            ││
│  │      generationEngine.effectCommandsFlow.collect { effects ->       ││
│  │          // 效果已由 SDK 内部执行                                    ││
│  │          updateUI(effects)                                          ││
│  │      }                                                              ││
│  │  }                                                                  ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 TTS 播报集成

```kotlin
// 在 MainViewModel 中
lifecycleScope.launch {
    semanticEngine.sceneDescriptorFlow.collect { scene ->
        scene?.announcement?.let { text ->
            if (text.isNotBlank()) {
                ttsService.speak(text)
            }
        }
    }
}
```

### 3.3 氛围灯注入

```kotlin
// 在 Application 或 MainActivity 中
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Layer3SDK
        val config = Layer3Config.Builder()
            .setAmbientLightController(CarAmbientLightController())
            .build()
        
        Layer3SDK.init(this, config)
    }
}
```

---

## 四、模块依赖关系

```
app-main
    │
    ├── core-api (数据契约)
    │
    ├── module-perception-api
    │       └── module-perception (感知层 SDK)
    │
    ├── module-semantic (语义层 SDK)
    │
    ├── layer3-api
    │       └── layer3-sdk (生成层 SDK)
    │
    ├── module-localmusic (本地音乐 SDK)
    │
    └── 自定义实现
            ├── TtsService (TTS 播报服务)
            ├── CarAmbientLightController (氛围灯控制)
            └── MockAmbientLightController (测试用 Mock)
```

---

## 五、实施步骤

### 阶段一：基础架构搭建

1. **创建 app-main 模块**
   - 配置 `build.gradle.kts`
   - 添加对三个 SDK 的依赖
   - 配置 Hilt 依赖注入

2. **实现权限管理**
   - 在 `AndroidManifest.xml` 声明权限
   - 实现动态权限申请流程

3. **实现 TTS 服务**
   - 创建 `TtsService` 类
   - 封装 Android `TextToSpeech` API

### 阶段二：SDK 集成

4. **集成感知层 SDK**
   - 初始化 `PerceptionEngine`
   - 监听 `standardizedSignalsFlow`

5. **集成语义层 SDK**
   - 初始化 `SemanticEngine`
   - 监听 `sceneDescriptorFlow`
   - 集成 TTS 播报

6. **集成生成层 SDK**
   - 初始化 `Layer3SDK`
   - 注入 `IAmbientLightController` 实现
   - 监听 `effectCommandsFlow`

### 阶段三：UI 实现

7. **实现主界面**
   - 显示当前场景名称和描述
   - 显示播放列表和当前曲目
   - 显示氛围灯状态

8. **实现错误处理**
   - 监听各 SDK 的错误状态
   - 显示错误提示 Dialog/Toast

---

## 六、需要确认的问题

### 6.1 氛围灯控制方案

**问题**：目前是真实车机环境还是测试环境？

**选项**：
- A. 真实车机 - 需要实现 `CarAmbientLightController` 调用车机 SDK
- B. 测试环境 - 使用 `MockAmbientLightController` 屏幕模拟
- C. 两者都需要 - 通过 BuildConfig 切换

### 6.2 TTS 播报时机

**问题**：TTS 播报是否需要打断当前音乐？

**选项**：
- A. 打断音乐 - 使用 `AudioManager.requestAudioFocus()`
- B. 降低音乐音量 - "闪避"模式
- C. 不打断 - 音乐继续播放，TTS 叠加

### 6.3 歌曲资源位置

**问题**：2000 首本地歌曲存储在哪里？

**选项**：
- A. 设备外部存储 - `/sdcard/Music/AiMusic/`
- B. 应用私有目录 - `context.getExternalFilesDir("Music")`
- C. Assets 预置 - 需要首次启动时复制到本地

---

## 七、总结

### app-main 需要实现的能力

| 能力 | 实现方式 | 优先级 |
|------|----------|--------|
| **TTS 播报** | APK 实现 `TtsService` | P0 |
| **氛围灯控制** | APK 实现 `IAmbientLightController` | P0 |
| **权限管理** | APK 申请动态权限 | P0 |
| **生命周期管理** | APK 管理 SDK 启停 | P0 |
| **UI 界面** | APK 实现 Compose UI | P1 |
| **数据流转** | APK 串联三个 SDK | P0 |

### SDK 已实现的能力（无需 APK 实现）

| 能力 | 所属 SDK |
|------|----------|
| 音乐播放 | `module-localmusic` + `layer3-sdk` |
| 音量控制 | `layer3-sdk.AudioEngine` |
| EQ/音效 | `layer3-sdk.AudioEngine` |
| 播放列表生成 | `layer3-sdk.ContentEngine` |
| 灯光配置生成 | `layer3-sdk.LightingEngine` |

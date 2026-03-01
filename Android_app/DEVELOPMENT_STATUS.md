# Android App 研发进度追踪

> 最后更新：2026-03-01

---

## 一、目录结构全貌

```
Android_app/
├── core/                              # 核心层
│   └── core-api/                      # ✅ 契约层（数据模型定义）
│       ├── Layer1Models.kt            # StandardizedSignals, Signals, Vehicle...
│       ├── Layer2Models.kt            # SceneDescriptor, Intent, Hints...
│       ├── Layer3Models.kt            # EffectCommands, ContentCommand...
│       └── SerializationTest.kt       # 序列化测试
│
├── features/                          # 功能模块层
│   ├── perception/                    # 感知层
│   │   ├── module-perception-api/     # ✅ 接口定义
│   │   │   ├── IPerceptionEngine.kt   # 引擎接口
│   │   │   ├── PerceptionConfig.kt    # 配置类
│   │   │   └── PerceptionError.kt     # 错误定义
│   │   └── module-perception/         # ✅ SDK 实现
│   │       ├── PerceptionEngine.kt    # 主引擎
│   │       ├── SensorManager.kt       # 传感器管理
│   │       ├── CameraSource.kt        # 外部摄像头
│   │       ├── IpCameraSource.kt      # IP 摄像头
│   │       ├── AIClient.kt            # AI 分析客户端
│   │       ├── LocalImageAnalyzer.kt  # 本地图像分析
│   │       ├── WeatherService.kt      # 天气服务
│   │       └── ConfidenceValidator.kt # 置信度验证
│   │
│   ├── semantic/                      # 语义层
│   │   ├── module-semantic/           # ✅ SDK 实现
│   │   │   ├── SemanticEngine.kt      # 主引擎
│   │   │   ├── RulesEngine.kt         # 规则引擎
│   │   │   ├── TemplateManager.kt     # 模板管理
│   │   │   ├── LlmClient.kt           # 大模型客户端
│   │   │   ├── PromptBuilder.kt       # Prompt 构建
│   │   │   └── assets/templates/      # 场景模板 JSON
│   │   └── app-demo-semantic/         # ✅ Demo APK
│   │       └── MainActivity.kt
│   │
│   ├── localmusic/                    # 本地音乐管理
│   │   ├── module-localmusic/         # ✅ SDK 实现
│   │   │   ├── LocalMusicIndex.kt     # SQLite 索引
│   │   │   ├── LocalMusicRepository.kt# 数据仓库
│   │   │   ├── MusicQueryBuilder.kt   # 查询构建器
│   │   │   ├── LocalMusicModule.kt    # 模块入口
│   │   │   ├── models/Track.kt        # 歌曲模型
│   │   │   └── player/
│   │   │       ├── MusicPlayer.kt     # 播放器
│   │   │       ├── PlaylistManager.kt # 播放列表
│   │   │       └── PlayerState.kt     # 播放状态
│   │   └── app-demo-localmusic/       # ✅ Demo APK
│   │       └── MainActivity.kt
│   │
│   └── generation/                    # 生成层
│       ├── module-generation/         # ❌ SDK 实现（待开发）
│       └── app-demo-generation/       # ❌ Demo APK（待开发）
│
├── app/                               # 主应用层
│   └── app-main/                      # ❌ 主 APK（待开发）
│
├── gradle/
│   └── libs.versions.toml             # 版本目录
├── build.gradle.kts                   # 根构建配置
├── settings.gradle.kts                # 模块注册
└── apk_sdk_architecture_guide.md      # 架构指南
```

---

## 二、模块完成状态

| 模块 | Kotlin 文件 | 状态 | 负责人 |
|------|-------------|------|--------|
| `core-api` | 4 | ✅ 完成 | - |
| `module-perception-api` | 3 | ✅ 完成 | 李翰铭 |
| `module-perception` | 8 | ✅ 完成 | 李翰铭 |
| `module-semantic` | 5 | ✅ 完成 | Trae AI |
| `module-localmusic` | 8 | ✅ 完成 | - |
| `app-demo-semantic` | 1 | ✅ 完成 | Trae AI |
| `app-demo-localmusic` | 1 | ✅ 完成 | - |
| `app-demo-perception` | 0 | ❌ 待开发 | - |
| `module-generation` | 0 | ❌ 待开发 | - |
| `app-demo-generation` | 0 | ❌ 待开发 | - |
| `app-main` | 0 | ❌ 待开发 | - |

**总计**：30 个 Kotlin 文件，7 个模块已完成，4 个模块待开发

---

## 三、数据流架构

```
┌─────────────────────────────────────────────────────────────────┐
│  Layer 1: 感知层 (module-perception) ✅                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ PerceptionEngine                                            ││
│  │ ├── SensorManager (GPS, 麦克风)                             ││
│  │ ├── CameraSource (外部摄像头)                               ││
│  │ ├── IpCameraSource (车内摄像头)                             ││
│  │ ├── AIClient (Qwen-VL-Max 图像分析)                         ││
│  │ ├── WeatherService (Open-Meteo 天气)                        ││
│  │ └── LocalImageAnalyzer (本地颜色提取)                       ││
│  └─────────────────────────────────────────────────────────────┘│
│                              ↓                                   │
│                    StandardizedSignals (core-api)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Layer 2: 语义层 (module-semantic) ✅                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ SemanticEngine                                              ││
│  │ ├── RulesEngine (规则匹配) → 快通道                         ││
│  │ ├── TemplateManager (模板库)                                ││
│  │ ├── LlmClient (Qwen-Plus 大模型) → 慢通道                   ││
│  │ └── PromptBuilder (Prompt 构建)                             ││
│  └─────────────────────────────────────────────────────────────┘│
│                              ↓                                   │
│                      SceneDescriptor (core-api)                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Layer 3: 生成层 (module-generation) ❌ 待开发                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ GenerationEngine                                            ││
│  │ ├── LocalMusicRepository (歌曲查询)                         ││
│  │ ├── MusicPlayer (ExoPlayer 播放)                            ││
│  │ ├── IAmbientLightController (氛围灯接口)                    ││
│  │ ├── IVolumeController (音量控制接口)                        ││
│  │ └── EffectExecutor (命令执行器)                             ││
│  └─────────────────────────────────────────────────────────────┘│
│                              ↓                                   │
│                      EffectCommands (core-api)                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、待实现功能清单

### 4.1 感知层 Demo APK (`app-demo-perception`)

| 功能 | 说明 | 状态 |
|------|------|------|
| 权限申请 | CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION | ❌ |
| UI 界面 | 展示传感器数据和 StandardizedSignals JSON | ❌ |
| SDK 集成 | 初始化 PerceptionEngine 并展示数据流 | ❌ |

### 4.2 生成层 SDK (`module-generation`)

| 功能 | 说明 | 状态 |
|------|------|------|
| GenerationEngine | 主引擎入口 | ❌ |
| MusicPlayer | 封装 Media3 ExoPlayer | ❌ |
| IAmbientLightController | 氛围灯控制接口 | ❌ |
| IVolumeController | 音量控制接口 | ❌ |
| EffectExecutor | 执行 EffectCommands | ❌ |

### 4.3 生成层 Demo APK (`app-demo-generation`)

| 功能 | 说明 | 状态 |
|------|------|------|
| Mock 硬件实现 | MockAmbientLightController（屏幕颜色模拟） | ❌ |
| UI 界面 | 展示 SceneDescriptor 输入和执行效果 | ❌ |
| SDK 集成 | 初始化 GenerationEngine 并执行命令 | ❌ |

### 4.4 主 APK (`app-main`)

| 功能 | 说明 | 状态 |
|------|------|------|
| 全链路集成 | 感知 → 语义 → 生成 | ❌ |
| Hilt 依赖注入 | 统一管理 SDK 实例 | ❌ |
| 权限管理 | 合并所有权限申请 | ❌ |
| 主界面 UI | 显示当前场景和播放状态 | ❌ |
| 生命周期管理 | 前后台切换时 SDK 暂停/恢复 | ❌ |

---

## 五、SDK 对外接口

### 5.1 感知层 SDK

```kotlin
interface IPerceptionEngine {
    val standardizedSignalsFlow: Flow<StandardizedSignals>  // 输出数据流
    fun start()                                              // 启动采集
    fun stop()                                               // 停止采集
    fun destroy()                                            // 销毁资源
    fun updateConfig(config: PerceptionConfig)               // 更新配置
}

// 配置类
data class PerceptionConfig(
    val ipCameraUrl: String,           // IP 摄像头地址
    val ipCameraUsername: String,      // 认证用户名
    val ipCameraPassword: String,      // 认证密码
    val dashScopeApiKey: String,       // AI API Key
    val refreshIntervalMs: Long = 3000L // 刷新间隔
)
```

### 5.2 语义层 SDK

```kotlin
class SemanticEngine(
    context: Context,
    llmApiKey: String = "",            // 可选，大模型 API Key
    llmBaseUrl: String = "...",        // 可选，API 地址
    llmModel: String = "qwen-plus"     // 可选，模型名称
) {
    fun initialize(): Boolean                              // 初始化
    suspend fun processSignals(signals: StandardizedSignals): Result<SceneDescriptor>
}
```

### 5.3 本地音乐 SDK

```kotlin
class LocalMusicRepository(private val musicIndex: LocalMusicIndex) {
    suspend fun queryTracks(hints: MusicHints): List<Track>
    suspend fun searchTracks(keyword: String): List<Track>
    suspend fun getTracksByGenre(genre: String): List<Track>
    suspend fun getTracksByMood(moodTag: String): List<Track>
    suspend fun getTracksByScene(sceneTag: String): List<Track>
    suspend fun getTracksByEnergyRange(min: Double, max: Double): List<Track>
    fun getTrackCount(): Int
    fun isReady(): Boolean
}
```

---

## 六、开发优先级

| 优先级 | 模块 | 工作量 | 依赖 | 状态 |
|--------|------|--------|------|------|
| P0 | `module-generation` | 3-5 天 | 无 | ❌ 待开发 |
| P1 | `app-demo-generation` | 1-2 天 | module-generation | ❌ 待开发 |
| P2 | `app-demo-perception` | 1-2 天 | module-perception | ❌ 待开发 |
| P3 | `app-main` | 3-5 天 | 所有 SDK | ❌ 待开发 |

---

## 七、变更日志

| 日期 | 变更内容 | 提交者 |
|------|----------|--------|
| 2026-03-01 | 对齐感知层数据模型到 core-api | Trae AI |
| 2026-03-01 | 整合李翰铭的感知层 SDK | Trae AI |
| 2026-03-01 | 完成语义层 SDK 和 Demo APK | Trae AI |
| 2026-03-01 | 初始化 Android 项目结构 | Trae AI |

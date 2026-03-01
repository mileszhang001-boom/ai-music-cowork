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
│   ├── layer3/                        # 生成层（Layer3）
│   │   ├── layer3-api/                # ✅ 接口定义
│   │   │   ├── IGenerationEngine.kt   # 生成引擎接口
│   │   │   ├── IContentEngine.kt      # 内容引擎接口
│   │   │   ├── ILightingEngine.kt     # 灯光引擎接口
│   │   │   ├── IAudioEngine.kt        # 音频引擎接口
│   │   │   ├── IAmbientLightController.kt # 氛围灯控制器接口
│   │   │   ├── Layer3Config.kt        # 配置类
│   │   │   └── Layer3Error.kt         # 错误定义
│   │   ├── layer3-sdk/                # ✅ SDK 实现
│   │   │   ├── Layer3SDK.kt           # SDK 主入口
│   │   │   ├── engine/
│   │   │   │   ├── ContentEngine.kt   # 音乐推荐引擎
│   │   │   │   ├── LightingEngine.kt  # 灯光控制引擎
│   │   │   │   ├── AudioEngine.kt     # 音频配置引擎
│   │   │   │   └── GenerationEngine.kt# 场景生成引擎
│   │   │   ├── data/
│   │   │   │   ├── MusicLibraryLoader.kt  # 音乐库加载器
│   │   │   │   ├── SceneTemplateLoader.kt # 场景模板加载器
│   │   │   │   └── CacheManager.kt    # 缓存管理器
│   │   │   ├── algorithm/
│   │   │   │   ├── MusicScorer.kt     # 音乐评分算法
│   │   │   │   ├── ArtistDiversityFilter.kt # 艺术家多样性过滤
│   │   │   │   ├── SceneKeywordMatcher.kt   # 场景关键词匹配
│   │   │   │   └── ColorAdjuster.kt   # 颜色调整器
│   │   │   └── assets/                # 测试数据
│   │   │       ├── music_library.json # 音乐库
│   │   │       ├── preset_templates.json # 场景模板
│   │   │       ├── scene_keyword_mapping.json # 场景关键词映射
│   │   │       └── template_analysis.json # 模板分析
│   │   └── app-demo-layer3/           # ✅ Demo APK
│   │       ├── MainActivity.kt        # 主界面
│   │       ├── ui/
│   │       │   ├── SceneSelectionScreen.kt # 场景选择界面
│   │       │   ├── ResultDisplayScreen.kt  # 结果展示界面
│   │       │   └── Theme.kt           # 主题配置
│   │       └── controller/
│   │           └── MockAmbientLightController.kt # Mock 氛围灯控制器
│   │
│   └── generation/                    # 生成层（旧架构，待整合）
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
| `layer3-api` | 6 | ✅ 完成 | Trae AI |
| `layer3-sdk` | 15+ | ✅ 完成 | Trae AI |
| `app-demo-semantic` | 1 | ✅ 完成 | Trae AI |
| `app-demo-localmusic` | 1 | ✅ 完成 | - |
| `app-demo-layer3` | 5 | ✅ 完成 | Trae AI |
| `app-demo-perception` | 0 | ❌ 待开发 | - |
| `module-generation` | 0 | ❌ 待整合 | - |
| `app-demo-generation` | 0 | ❌ 待整合 | - |
| `app-main` | 0 | ❌ 待开发 | - |

**总计**：50+ 个 Kotlin 文件，10 个模块已完成，4 个模块待开发/整合

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
│  Layer 3: 生成层 (layer3-sdk) ✅                                │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ GenerationEngine                                            ││
│  │ ├── ContentEngine (音乐推荐)                                ││
│  │ │   ├── MusicScorer (评分算法)                              ││
│  │ │   ├── ArtistDiversityFilter (多样性限制)                  ││
│  │ │   └── CacheManager (缓存管理)                             ││
│  │ ├── LightingEngine (灯光控制)                               ││
│  │ │   ├── SceneKeywordMatcher (场景匹配)                      ││
│  │ │   └── ColorAdjuster (颜色调整)                            ││
│  │ ├── AudioEngine (音频配置)                                  ││
│  │ └── IAmbientLightController (氛围灯接口，由 APK 注入)       ││
│  └─────────────────────────────────────────────────────────────┘│
│                              ↓                                   │
│                      EffectCommands (core-api)                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、SDK 对外接口

### 4.1 感知层 SDK

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

### 4.2 语义层 SDK

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

### 4.3 本地音乐 SDK

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

### 4.4 Layer3 生成层 SDK

```kotlin
// 初始化
val config = Layer3Config.Builder()
    .setContentProvider(ContentProviderConfig(providerType = "local"))
    .build()
Layer3SDK.init(context, config)

// 获取引擎
val generationEngine = Layer3SDK.getGenerationEngine()
val contentEngine = Layer3SDK.getContentEngine()
val lightingEngine = Layer3SDK.getLightingEngine()
val audioEngine = Layer3SDK.getAudioEngine()

// 生成场景效果
val sceneDescriptor = generationEngine.generateScene("morning_commute").getOrThrow()
val effects = generationEngine.generateEffects(sceneDescriptor).getOrThrow()

// 生成播放列表
val playlist = contentEngine.generatePlaylist(sceneDescriptor).getOrThrow()

// 生成灯光配置
val lighting = lightingEngine.generateLighting(sceneDescriptor).getOrThrow()

// 生成音频配置
val audio = audioEngine.generateAudioConfig(sceneDescriptor).getOrThrow()

// 销毁
Layer3SDK.destroy()
```

#### 接口定义

```kotlin
interface IGenerationEngine {
    val effectCommandsFlow: Flow<EffectCommands>
    suspend fun generateScene(sceneId: String): Result<SceneDescriptor>
    suspend fun generateEffects(scene: SceneDescriptor): Result<EffectCommands>
    fun start()
    fun stop()
    fun destroy()
    fun updateConfig(config: Layer3Config)
}

interface IContentEngine {
    val playlistFlow: Flow<List<Track>>
    suspend fun generatePlaylist(scene: SceneDescriptor): Result<List<Track>>
    fun play(playlist: List<Track>)
    fun pause()
    fun resume()
    fun stop()
    fun next()
    fun previous()
}

interface ILightingEngine {
    val lightingStateFlow: Flow<LightingCommand>
    suspend fun generateLighting(scene: SceneDescriptor): Result<LightingCommand>
    fun applyLighting(command: LightingCommand)
    fun setTheme(theme: String)
    fun setIntensity(intensity: Double)
}

interface IAudioEngine {
    val audioStateFlow: Flow<AudioCommand>
    suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioCommand>
    fun applyAudioConfig(command: AudioCommand)
    fun setPreset(preset: String)
    fun setVolume(volumeDb: Int)
}

// 氛围灯控制器接口（非标硬件，由 APK 注入）
interface IAmbientLightController {
    suspend fun connect(): Boolean
    suspend fun disconnect()
    fun setZoneColor(zoneId: Int, color: Int)
    fun setAllColors(colors: Map<Int, Int>)
    fun startMusicSync(bpm: Int)
    fun stopMusicSync()
}
```

---

## 五、模块依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                         core-api                            │
│  (Layer1Models, Layer2Models, Layer3Models)                 │
└─────────────────────────────────────────────────────────────┘
         ↓              ↓              ↓              ↓
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ perception  │ │  semantic   │ │ localmusic  │ │   layer3    │
│    -api     │ │             │ │             │ │    -api     │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
         ↓              ↓              ↓              ↓
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ perception  │ │  semantic   │ │ localmusic  │ │   layer3    │
│    -sdk     │ │    -sdk     │ │    -sdk     │ │    -sdk     │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
         ↓              ↓              ↓              ↓
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│    demo     │ │    demo     │ │    demo     │ │    demo     │
│ perception  │ │  semantic   │ │ localmusic  │ │   layer3    │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

---

## 六、开发优先级

| 优先级 | 模块 | 工作量 | 依赖 | 状态 |
|--------|------|--------|------|------|
| P0 | `layer3-sdk` | 3-5 天 | core-api | ✅ 完成 |
| P1 | `app-demo-layer3` | 1-2 天 | layer3-sdk | ✅ 完成 |
| P2 | `app-demo-perception` | 1-2 天 | module-perception | ❌ 待开发 |
| P3 | `app-main` | 3-5 天 | 所有 SDK | ❌ 待开发 |

---

## 七、变更日志

| 日期 | 变更内容 | 提交者 |
|------|----------|--------|
| 2026-03-01 | 完成 Layer3 SDK 和 Demo APK | Trae AI |
| 2026-03-01 | 对齐 Layer3 SDK 到 core-api 数据模型 | Trae AI |
| 2026-03-01 | 对齐感知层数据模型到 core-api | Trae AI |
| 2026-03-01 | 整合李翰铭的感知层 SDK | Trae AI |
| 2026-03-01 | 完成语义层 SDK 和 Demo APK | Trae AI |
| 2026-03-01 | 初始化 Android 项目结构 | Trae AI |

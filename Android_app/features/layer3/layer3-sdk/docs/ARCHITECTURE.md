# Layer3 SDK 架构说明

本文档详细介绍 Layer3 SDK 的架构设计和实现原理。

## 目录

- [概述](#概述)
- [模块结构](#模块结构)
- [引擎架构](#引擎架构)
- [数据流](#数据流)
- [缓存机制](#缓存机制)
- [设计原则](#设计原则)

---

## 概述

Layer3 SDK 是一个场景驱动的智能座舱体验引擎，通过统一的场景描述符协调音乐、灯光和音频三个子系统，为用户提供沉浸式的座舱体验。

### 核心设计理念

1. **场景驱动**: 以场景描述符为核心，统一协调各子系统
2. **响应式架构**: 使用 Kotlin Flow 实现状态管理和事件流
3. **模块化设计**: 各引擎独立运作，通过接口解耦
4. **缓存优化**: 多级缓存提升响应速度

---

## 模块结构

SDK 采用分层架构，分为三层：

```
┌─────────────────────────────────────────────────────────┐
│                      应用层                              │
│                   (Application)                          │
├─────────────────────────────────────────────────────────┤
│                      SDK 层                              │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Layer3SDK (入口)                    │   │
│  ├─────────────────────────────────────────────────┤   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐     │   │
│  │  │ Content   │ │ Lighting  │ │  Audio    │     │   │
│  │  │ Engine    │ │ Engine    │ │  Engine   │     │   │
│  │  └───────────┘ └───────────┘ └───────────┘     │   │
│  │  ┌───────────────────────────────────────────┐ │   │
│  │  │           Generation Engine               │ │   │
│  │  └───────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                      数据层                              │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐             │
│  │  Cache    │ │  Loader   │ │ Algorithm │             │
│  │  Manager  │ │           │ │           │             │
│  └───────────┘ └───────────┘ └───────────┘             │
├─────────────────────────────────────────────────────────┤
│                      API 层                              │
│              (layer3-api 模块)                          │
│  ┌───────────────────────────────────────────────────┐ │
│  │  Interfaces │ Data Models │ Constants │ Errors    │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 目录结构

```
layer3-sdk/
├── src/main/java/com/example/layer3/sdk/
│   ├── Layer3SDK.kt              # SDK 入口
│   ├── engine/                   # 引擎实现
│   │   ├── ContentEngine.kt      # 内容引擎
│   │   ├── LightingEngine.kt     # 灯光引擎
│   │   ├── AudioEngine.kt        # 音频引擎
│   │   └── GenerationEngine.kt   # 生成引擎
│   ├── algorithm/                # 算法模块
│   │   ├── MusicScorer.kt        # 音乐评分
│   │   ├── ArtistDiversityFilter.kt  # 艺术家多样性过滤
│   │   ├── ColorAdjuster.kt      # 颜色调整
│   │   └── SceneKeywordMatcher.kt    # 场景关键词匹配
│   ├── data/                     # 数据模块
│   │   ├── CacheManager.kt       # 缓存管理
│   │   ├── MusicLibraryLoader.kt # 音乐库加载
│   │   └── SceneTemplateLoader.kt    # 场景模板加载
│   └── util/                     # 工具模块
│       ├── JsonLoader.kt         # JSON 加载
│       └── Logger.kt             # 日志工具
└── src/main/assets/              # 资源文件
    ├── music_library.json        # 音乐库
    ├── preset_templates.json     # 预设模板
    └── scene_keyword_mapping.json # 场景关键词映射
```

---

## 引擎架构

### 整体架构图

```
                    ┌─────────────────┐
                    │   SceneDescriptor   │
                    │   (场景描述符)       │
                    └────────┬────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │      GenerationEngine        │
              │      (生成引擎)               │
              │  ┌────────────────────────┐  │
              │  │  - generateScene()     │  │
              │  │  - generateEffects()   │  │
              │  │  - Template Matching   │  │
              │  └────────────────────────┘  │
              └──────────┬───┬───┬───────────┘
                         │   │   │
         ┌───────────────┘   │   └───────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ ContentEngine   │ │ LightingEngine  │ │  AudioEngine    │
│ (内容引擎)       │ │ (灯光引擎)       │ │  (音频引擎)      │
├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│ - Playlist Gen  │ │ - Config Gen    │ │ - Config Gen    │
│ - Playback Ctrl │ │ - Zone Control  │ │ - EQ Control    │
│ - Scoring       │ │ - Color Adjust  │ │ - Spatial Audio │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  EffectCommands │
                    │  (效果命令)       │
                    └─────────────────┘
```

### ContentEngine (内容引擎)

内容引擎负责音乐内容的生成和播放控制。

```
┌─────────────────────────────────────────────────────┐
│                  ContentEngine                       │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────┐    ┌──────────────────────────┐  │
│  │ MusicLibrary │    │      MusicScorer         │  │
│  │    Loader    │───▶│  (场景匹配评分)           │  │
│  └──────────────┘    └──────────┬───────────────┘  │
│                                 │                    │
│                                 ▼                    │
│                      ┌──────────────────────────┐   │
│                      │  ArtistDiversityFilter   │   │
│                      │  (艺术家多样性过滤)        │   │
│                      └──────────┬───────────────┘   │
│                                 │                    │
│                                 ▼                    │
│                      ┌──────────────────────────┐   │
│                      │       Playlist           │   │
│                      │     (播放列表)            │   │
│                      └──────────────────────────┘   │
│                                                      │
│  Flow 输出:                                          │
│  ┌────────────────┐ ┌────────────────┐             │
│  │currentPlaylist │ │ currentTrack   │             │
│  │     Flow       │ │    Flow        │             │
│  └────────────────┘ └────────────────┘             │
└─────────────────────────────────────────────────────┘
```

**核心算法：**

1. **MusicScorer**: 根据场景特征对曲目评分
   - 匹配场景情绪（valence/arousal）
   - 匹配音乐特征（节奏、能量）
   - 匹配场景提示（流派、年代）

2. **ArtistDiversityFilter**: 保证播放列表的艺术家多样性
   - 限制同一艺术家的曲目数量
   - 保留评分排序

### LightingEngine (灯光引擎)

灯光引擎负责灯光配置的生成和控制。

```
┌─────────────────────────────────────────────────────┐
│                 LightingEngine                       │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │              Mood → Color Theme               │  │
│  │  ┌─────────┐    ┌─────────────────────────┐  │  │
│  │  │  Mood   │───▶│ inferColorTheme()       │  │  │
│  │  │(情绪)   │    │ happy/calm/energetic... │  │  │
│  │  └─────────┘    └──────────┬──────────────┘  │  │
│  └─────────────────────────────┼─────────────────┘  │
│                                │                     │
│                                ▼                     │
│  ┌──────────────────────────────────────────────┐  │
│  │              ColorAdjuster                    │  │
│  │  - 主题颜色管理                                │  │
│  │  - 颜色强度调整                                │  │
│  │  - 颜色格式转换                                │  │
│  └──────────────────────────────────────────────┘  │
│                                                      │
│  区域配置生成:                                       │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │Dashboard│ │DoorLeft │ │DoorRight│ │Footwell │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                      │
│  Flow 输出:                                          │
│  ┌────────────────┐ ┌────────────────┐             │
│  │ currentConfig  │ │ lightingState  │             │
│  │     Flow       │ │     Flow       │             │
│  └────────────────┘ └────────────────┘             │
└─────────────────────────────────────────────────────┘
```

**情绪到颜色主题映射：**

| 情绪区域 | 颜色主题 | 典型颜色 |
|----------|----------|----------|
| 高效价 + 高唤醒 | happy | 金色、橙色、红色 |
| 低效价 | melancholic | 冷色调 |
| 高唤醒 | energetic | 红色、粉色、紫色 |
| 低唤醒 | calm | 蓝色、浅蓝、绿色 |
| 高效价 + 低唤醒 | romantic | 粉色、浅粉、淡紫 |
| 其他 | focused | 白色、浅蓝、淡紫 |

### AudioEngine (音频引擎)

音频引擎负责音频参数的配置。

```
┌─────────────────────────────────────────────────────┐
│                  AudioEngine                         │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │         Scene → Audio Config                  │  │
│  │  ┌─────────────┐    ┌─────────────────────┐  │  │
│  │  │ Energy Level│───▶│ inferPreset()       │  │  │
│  │  │    Mood     │    │ electronic/pop/...  │  │  │
│  │  └─────────────┘    └──────────┬──────────┘  │  │
│  └───────────────────────────────┼───────────────┘  │
│                                  │                   │
│                                  ▼                   │
│  ┌──────────────────────────────────────────────┐  │
│  │           Equalizer Config                    │  │
│  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │  │
│  │  │ 60Hz│ │150Hz│ │400Hz│ │ 1kHz│ │ 6kHz│   │  │
│  │  │     │ │     │ │     │ │     │ │     │   │  │
│  │  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘   │  │
│  └──────────────────────────────────────────────┘  │
│                                                      │
│  空间音频:                                           │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐        │
│  │  Stereo   │ │Surround 5.1│ │ Dolby    │        │
│  │           │ │            │ │  Atmos   │        │
│  └───────────┘ └───────────┘ └───────────┘        │
│                                                      │
│  Flow 输出:                                          │
│  ┌────────────────┐ ┌────────────────┐             │
│  │ currentConfig  │ │  audioState    │             │
│  │     Flow       │ │     Flow       │             │
│  └────────────────┘ └────────────────┘             │
└─────────────────────────────────────────────────────┘
```

### GenerationEngine (生成引擎)

生成引擎是协调中心，负责场景生成和效果协调。

```
┌─────────────────────────────────────────────────────┐
│                GenerationEngine                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────────────────────────────────────┐  │
│  │           Scene Template Loader               │  │
│  │  - 加载预设场景模板                            │  │
│  │  - 模板 ID 查找                               │  │
│  │  - 关键词匹配                                  │  │
│  └──────────────────────────────────────────────┘  │
│                                                      │
│  效果生成流程:                                       │
│                                                      │
│  SceneDescriptor                                     │
│         │                                            │
│         ▼                                            │
│  ┌─────────────────┐                                │
│  │ ContentEngine   │──▶ EffectCommand (play_playlist)│
│  │ generatePlaylist│                                │
│  └─────────────────┘                                │
│         │                                            │
│         ▼                                            │
│  ┌─────────────────┐                                │
│  │LightingEngine   │──▶ EffectCommand (apply_config)│
│  │generateConfig   │                                │
│  └─────────────────┘                                │
│         │                                            │
│         ▼                                            │
│  ┌─────────────────┐                                │
│  │ AudioEngine     │──▶ EffectCommand (apply_config)│
│  │ generateConfig  │                                │
│  └─────────────────┘                                │
│         │                                            │
│         ▼                                            │
│  ┌─────────────────┐                                │
│  │EffectCommands   │                                │
│  │ (命令集合)       │                                │
│  └─────────────────┘                                │
│                                                      │
│  Flow 输出:                                          │
│  ┌────────────────────────────────────────────┐    │
│  │           effectCommandsFlow                │    │
│  └────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

---

## 数据流

### 场景生成数据流

```
用户请求 ──────────────────────────────────────────────────────────────────▶
    │
    ▼
┌─────────────────┐
│ generateScene() │
│   sceneId       │
└────────┬────────┘
         │
         ▼
    ┌────────────┐     缓存命中?
    │ Cache Get  │─────────────────┐
    └────────────┘                 │
         │ 否                      │ 是
         ▼                         │
    ┌────────────┐                 │
    │ Template   │                 │
    │ Loader     │                 │
    └────────────┘                 │
         │                         │
         ▼                         │
    ┌────────────┐                 │
    │ SceneDesc  │                 │
    │ (生成)     │                 │
    └────────────┘                 │
         │                         │
         ▼                         │
    ┌────────────┐                 │
    │ Cache Put  │                 │
    └────────────┘                 │
         │                         │
         ◀─────────────────────────┘
         │
         ▼
    返回 SceneDescriptor
```

### 效果生成数据流

```
SceneDescriptor
       │
       ▼
┌──────────────────────────────────────────────────────┐
│                   generateEffects()                   │
├──────────────────────────────────────────────────────┤
│                                                       │
│   ┌─────────────────────────────────────────────┐   │
│   │              并行生成配置                     │   │
│   │                                               │   │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐      │   │
│   │  │Content  │  │Lighting │  │ Audio   │      │   │
│   │  │Engine   │  │Engine   │  │ Engine  │      │   │
│   │  └────┬────┘  └────┬────┘  └────┬────┘      │   │
│   │       │            │            │            │   │
│   │       ▼            ▼            ▼            │   │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────┐      │   │
│   │  │Playlist │  │Lighting │  │ Audio   │      │   │
│   │  │         │  │ Config  │  │ Config  │      │   │
│   │  └────┬────┘  └────┬────┘  └────┬────┘      │   │
│   │       │            │            │            │   │
│   └───────┼────────────┼────────────┼───────────┘   │
│           │            │            │                │
│           ▼            ▼            ▼                │
│       EffectCommand EffectCommand EffectCommand      │
│           │            │            │                │
│           └────────────┼────────────┘                │
│                        │                             │
│                        ▼                             │
│              ┌─────────────────┐                     │
│              │ EffectCommands  │                     │
│              │   (命令集合)     │                     │
│              └─────────────────┘                     │
│                        │                             │
│                        ▼                             │
│              ┌─────────────────┐                     │
│              │  Flow Emit      │                     │
│              │ effectCommands  │                     │
│              └─────────────────┘                     │
└──────────────────────────────────────────────────────┘
```

### 状态订阅数据流

```
┌─────────────────────────────────────────────────────┐
│                    Engine                            │
│  ┌──────────────────────────────────────────────┐  │
│  │           MutableStateFlow                    │  │
│  │  ┌─────────────────────────────────────────┐ │  │
│  │  │ _currentState: MutableStateFlow<State>  │ │  │
│  │  └─────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────┘  │
│                         │                           │
│                         │ asStateFlow()             │
│                         ▼                           │
│  ┌──────────────────────────────────────────────┐  │
│  │              StateFlow                        │  │
│  │  ┌─────────────────────────────────────────┐ │  │
│  │  │ currentStateFlow: Flow<State>           │ │  │
│  │  └─────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                          │
                          │ collect()
                          ▼
┌─────────────────────────────────────────────────────┐
│                   Subscriber                         │
│  ┌──────────────────────────────────────────────┐  │
│  │  lifecycleScope.launch {                      │  │
│  │      engine.stateFlow.collect { state ->     │  │
│  │          updateUI(state)                     │  │
│  │      }                                       │  │
│  │  }                                           │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 缓存机制

SDK 实现了多级缓存机制，提升响应速度。

### CacheManager 实现

```kotlin
class CacheManager<T>(
    private val defaultTtlMs: Long = 300000L,  // 默认 TTL: 5 分钟
    private val maxSize: Int = 100              // 最大缓存数量
) {
    private val cache = ConcurrentHashMap<String, CacheEntry<T>>()
    private val accessOrder = mutableListOf<String>()
    
    data class CacheEntry<T>(
        val data: T,
        val createdAt: Long = System.currentTimeMillis(),
        val ttlMs: Long
    )
}
```

### 缓存策略

```
┌─────────────────────────────────────────────────────┐
│                  缓存策略                            │
├─────────────────────────────────────────────────────┤
│                                                      │
│  1. LRU 淘汰策略                                     │
│     ┌────────────────────────────────────────────┐ │
│     │ 当缓存满时，淘汰最久未访问的条目            │ │
│     │ accessOrder 列表维护访问顺序               │ │
│     └────────────────────────────────────────────┘ │
│                                                      │
│  2. TTL 过期策略                                     │
│     ┌────────────────────────────────────────────┐ │
│     │ 每个条目有独立的 TTL                        │ │
│     │ 读取时检查是否过期                          │ │
│     │ 过期条目自动移除                            │ │
│     └────────────────────────────────────────────┘ │
│                                                      │
│  3. 线程安全                                         │
│     ┌────────────────────────────────────────────┐ │
│     │ 使用 ConcurrentHashMap                     │ │
│     │ 操作使用 synchronized 保护                 │ │
│     └────────────────────────────────────────────┘ │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 各引擎缓存配置

| 引擎 | 缓存类型 | 最大数量 | 默认 TTL |
|------|----------|----------|----------|
| ContentEngine | Playlist | 20 | 5 分钟 |
| LightingEngine | LightingConfig | 20 | 5 分钟 |
| AudioEngine | AudioConfig | 20 | 5 分钟 |
| GenerationEngine | SceneDescriptor | 20 | 5 分钟 |

### 缓存使用示例

```kotlin
override suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist> {
    val cacheKey = "playlist_${scene.sceneId}"
    
    playlistCache.get(cacheKey)?.let {
        return Result.success(it)
    }
    
    val playlist = generatePlaylistInternal(scene)
    playlistCache.put(cacheKey, playlist)
    
    return Result.success(playlist)
}
```

---

## 设计原则

### 1. 单一职责原则

每个引擎只负责一个领域：
- ContentEngine: 音乐内容
- LightingEngine: 灯光控制
- AudioEngine: 音频配置
- GenerationEngine: 场景协调

### 2. 依赖倒置原则

引擎通过接口依赖，而非具体实现：

```kotlin
class GenerationEngine(
    private val contentEngine: ContentEngine,
    private val lightingEngine: LightingEngine,
    private val audioEngine: AudioEngine
) : IGenerationEngine {
    // ...
}
```

### 3. 开闭原则

通过配置和策略模式支持扩展：

```kotlin
data class Layer3Config(
    val contentProvider: ContentProviderConfig,
    val lightingController: LightingControllerConfig,
    val audioEngine: AudioEngineConfig,
    val generationEngine: GenerationEngineConfig
)
```

### 4. 接口隔离原则

接口按功能分离：

```kotlin
interface IContentEngine {
    suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist>
    suspend fun playPlaylist(playlist: Playlist): Result<Unit>
    // ...
}

interface ILightingEngine {
    suspend fun generateLightingConfig(scene: SceneDescriptor): Result<LightingConfig>
    suspend fun applyConfig(config: LightingConfig): Result<Unit>
    // ...
}
```

### 5. 响应式设计

使用 Kotlin Flow 实现响应式状态管理：

```kotlin
private val _currentState = MutableStateFlow<State>(InitialState)
val stateFlow: Flow<State> = _currentState.asStateFlow()
```

### 6. 错误处理

统一使用 Result 类型处理错误：

```kotlin
suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist> {
    return try {
        val playlist = generateInternal(scene)
        Result.success(playlist)
    } catch (e: Exception) {
        Result.failure(Layer3Error.ContentError(e.message))
    }
}
```

---

## 扩展指南

### 添加新的引擎

1. 在 `layer3-api` 模块定义接口：

```kotlin
interface INewEngine {
    val stateFlow: Flow<NewState>
    suspend fun doSomething(): Result<Unit>
}
```

2. 在 SDK 模块实现引擎：

```kotlin
class NewEngine(
    private val context: Context,
    private val config: NewEngineConfig
) : INewEngine {
    // 实现接口方法
}
```

3. 在 Layer3SDK 中注册：

```kotlin
object Layer3SDK {
    private var newEngine: NewEngine? = null
    
    fun init(context: Context, config: Layer3Config) {
        newEngine = NewEngine(context, config.newEngine)
    }
    
    fun getNewEngine(): INewEngine {
        return newEngine ?: throw IllegalStateException("SDK not initialized")
    }
}
```

### 添加新的算法

1. 在 `algorithm` 包创建算法类：

```kotlin
class NewAlgorithm {
    fun process(input: Input): Output {
        // 算法实现
    }
}
```

2. 在引擎中使用：

```kotlin
class ContentEngine(...) {
    private val newAlgorithm = NewAlgorithm()
    
    suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist> {
        val result = newAlgorithm.process(scene)
        // ...
    }
}
```

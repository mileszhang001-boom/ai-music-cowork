# Layer3 SDK API 参考

本文档提供 Layer3 SDK 的完整 API 参考。

## 目录

- [Layer3SDK 主类](#layer3sdk-主类)
- [ContentEngine API](#contentengine-api)
- [LightingEngine API](#lightingengine-api)
- [AudioEngine API](#audioengine-api)
- [GenerationEngine API](#generationengine-api)
- [数据模型说明](#数据模型说明)
- [错误类型](#错误类型)

---

## Layer3SDK 主类

Layer3SDK 是 SDK 的主入口点，采用单例模式实现。

### 方法

#### init

初始化 SDK。

```kotlin
fun init(context: Context, config: Layer3Config)
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `context` | Context | 应用上下文 |
| `config` | Layer3Config | SDK 配置 |

**示例：**
```kotlin
val config = Layer3Config.Builder()
    .setEnableAutoTransition(true)
    .build()

Layer3SDK.init(context, config)
```

---

#### getContentEngine

获取内容引擎实例。

```kotlin
fun getContentEngine(): IContentEngine
```

**返回：** 内容引擎接口实例

**异常：** 如果 SDK 未初始化，抛出 `IllegalStateException`

---

#### getLightingEngine

获取灯光引擎实例。

```kotlin
fun getLightingEngine(): ILightingEngine
```

**返回：** 灯光引擎接口实例

**异常：** 如果 SDK 未初始化，抛出 `IllegalStateException`

---

#### getAudioEngine

获取音频引擎实例。

```kotlin
fun getAudioEngine(): IAudioEngine
```

**返回：** 音频引擎接口实例

**异常：** 如果 SDK 未初始化，抛出 `IllegalStateException`

---

#### getGenerationEngine

获取生成引擎实例。

```kotlin
fun getGenerationEngine(): IGenerationEngine
```

**返回：** 生成引擎接口实例

**异常：** 如果 SDK 未初始化，抛出 `IllegalStateException`

---

#### isInitialized

检查 SDK 是否已初始化。

```kotlin
fun isInitialized(): Boolean
```

**返回：** 是否已初始化

---

#### getConfig

获取当前 SDK 配置。

```kotlin
fun getConfig(): Layer3Config?
```

**返回：** 当前配置，未初始化时返回 null

---

#### updateConfig

更新 SDK 配置。

```kotlin
fun updateConfig(newConfig: Layer3Config)
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `newConfig` | Layer3Config | 新的配置 |

---

#### destroy

销毁 SDK，释放所有资源。

```kotlin
fun destroy()
```

---

## ContentEngine API

内容引擎负责音乐播放列表生成和播放控制。

### 接口定义

```kotlin
interface IContentEngine {
    val currentPlaylistFlow: Flow<Playlist?>
    val currentTrackFlow: Flow<Track?>
    val playbackStateFlow: Flow<PlaybackState>
    
    suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist>
    suspend fun getRecommendations(basedOn: String, limit: Int = 10): Result<PlaylistRecommendation>
    suspend fun playPlaylist(playlist: Playlist): Result<Unit>
    suspend fun playTrack(track: Track): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun next(): Result<Unit>
    suspend fun previous(): Result<Unit>
    suspend fun seek(positionMs: Long): Result<Unit>
    suspend fun setVolume(level: Double): Result<Unit>
    fun getCurrentTrack(): Track?
    fun getCurrentPlaylist(): Playlist?
    fun getPlaybackState(): PlaybackState
}
```

### Flow 属性

#### currentPlaylistFlow

当前播放列表的状态流。

```kotlin
val currentPlaylistFlow: Flow<Playlist?>
```

**示例：**
```kotlin
lifecycleScope.launch {
    contentEngine.currentPlaylistFlow.collect { playlist ->
        playlist?.let { updatePlaylistUI(it) }
    }
}
```

---

#### currentTrackFlow

当前播放曲目的状态流。

```kotlin
val currentTrackFlow: Flow<Track?>
```

---

#### playbackStateFlow

播放状态的状态流。

```kotlin
val playbackStateFlow: Flow<PlaybackState>
```

---

### 方法

#### generatePlaylist

根据场景生成播放列表。

```kotlin
suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `scene` | SceneDescriptor | 场景描述符 |

**返回：** 包含播放列表的 Result

**示例：**
```kotlin
val result = contentEngine.generatePlaylist(scene)
result.onSuccess { playlist ->
    println("生成播放列表: ${playlist.name}, 共 ${playlist.track_count} 首")
}
```

---

#### getRecommendations

获取基于关键词的推荐。

```kotlin
suspend fun getRecommendations(basedOn: String, limit: Int = 10): Result<PlaylistRecommendation>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `basedOn` | String | 推荐依据（关键词） |
| `limit` | Int | 返回数量限制，默认 10 |

**返回：** 包含推荐结果的 Result

---

#### playPlaylist

开始播放播放列表。

```kotlin
suspend fun playPlaylist(playlist: Playlist): Result<Unit>
```

---

#### playTrack

播放指定曲目。

```kotlin
suspend fun playTrack(track: Track): Result<Unit>
```

---

#### pause / resume

暂停/恢复播放。

```kotlin
suspend fun pause(): Result<Unit>
suspend fun resume(): Result<Unit>
```

---

#### next / previous

切换到下一首/上一首。

```kotlin
suspend fun next(): Result<Unit>
suspend fun previous(): Result<Unit>
```

---

#### seek

跳转到指定位置。

```kotlin
suspend fun seek(positionMs: Long): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `positionMs` | Long | 目标位置（毫秒） |

---

#### setVolume

设置音量。

```kotlin
suspend fun setVolume(level: Double): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `level` | Double | 音量级别（0.0 - 1.0） |

---

## LightingEngine API

灯光引擎负责场景灯光配置生成和控制。

### 接口定义

```kotlin
interface ILightingEngine {
    val currentConfigFlow: Flow<LightingConfig?>
    val lightingStateFlow: Flow<LightingState>
    
    suspend fun generateLightingConfig(scene: SceneDescriptor): Result<LightingConfig>
    suspend fun applyConfig(config: LightingConfig): Result<Unit>
    suspend fun setZoneConfig(zoneId: String, zoneConfig: ZoneConfig): Result<Unit>
    suspend fun setBrightness(level: Double): Result<Unit>
    suspend fun setColor(hex: String): Result<Unit>
    suspend fun setPattern(pattern: String): Result<Unit>
    suspend fun enableMusicSync(enabled: Boolean): Result<Unit>
    suspend fun turnOff(): Result<Unit>
    suspend fun turnOn(): Result<Unit>
    fun getCurrentConfig(): LightingConfig?
    fun getLightingState(): LightingState
    fun getAvailableZones(): List<String>
}
```

### 方法

#### generateLightingConfig

根据场景生成灯光配置。

```kotlin
suspend fun generateLightingConfig(scene: SceneDescriptor): Result<LightingConfig>
```

**示例：**
```kotlin
val result = lightingEngine.generateLightingConfig(scene)
result.onSuccess { config ->
    println("灯光配置: ${config.scene.zones.size} 个区域")
}
```

---

#### applyConfig

应用灯光配置。

```kotlin
suspend fun applyConfig(config: LightingConfig): Result<Unit>
```

---

#### setZoneConfig

设置指定区域的灯光配置。

```kotlin
suspend fun setZoneConfig(zoneId: String, zoneConfig: ZoneConfig): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `zoneId` | String | 区域 ID |
| `zoneConfig` | ZoneConfig | 区域配置 |

**示例：**
```kotlin
val zoneConfig = ZoneConfig(
    zone_id = LightingZones.DASHBOARD,
    enabled = true,
    color = ColorConfig(hex = "#FF6B6B"),
    brightness = 0.8,
    pattern = LightingPatterns.PULSE
)
lightingEngine.setZoneConfig(LightingZones.DASHBOARD, zoneConfig)
```

---

#### setBrightness

设置整体亮度。

```kotlin
suspend fun setBrightness(level: Double): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `level` | Double | 亮度级别（0.0 - 1.0） |

---

#### setColor

设置整体颜色。

```kotlin
suspend fun setColor(hex: String): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `hex` | String | 十六进制颜色值（如 "#FF6B6B"） |

---

#### setPattern

设置灯光模式。

```kotlin
suspend fun setPattern(pattern: String): Result<Unit>
```

**可用模式：**
| 常量 | 值 | 说明 |
|------|-----|------|
| `LightingPatterns.STATIC` | "static" | 静态 |
| `LightingPatterns.BREATHING` | "breathing" | 呼吸灯 |
| `LightingPatterns.PULSE` | "pulse" | 脉冲 |
| `LightingPatterns.WAVE` | "wave" | 波浪 |
| `LightingPatterns.MUSIC_SYNC` | "music_sync" | 音乐同步 |
| `LightingPatterns.RAINBOW` | "rainbow" | 彩虹 |
| `LightingPatterns.FADE` | "fade" | 渐变 |

---

#### enableMusicSync

启用/禁用音乐同步。

```kotlin
suspend fun enableMusicSync(enabled: Boolean): Result<Unit>
```

---

#### turnOff / turnOn

关闭/开启灯光。

```kotlin
suspend fun turnOff(): Result<Unit>
suspend fun turnOn(): Result<Unit>
```

---

#### getAvailableZones

获取可用灯光区域列表。

```kotlin
fun getAvailableZones(): List<String>
```

**返回：** 区域 ID 列表

**可用区域：**
| 常量 | 值 | 说明 |
|------|-----|------|
| `LightingZones.DASHBOARD` | "dashboard" | 仪表盘 |
| `LightingZones.DOOR_LEFT` | "door_left" | 左车门 |
| `LightingZones.DOOR_RIGHT` | "door_right" | 右车门 |
| `LightingZones.FOOTWELL` | "footwell" | 脚窝 |
| `LightingZones.CEILING` | "ceiling" | 车顶 |
| `LightingZones.TRUNK` | "trunk" | 后备箱 |
| `LightingZones.EXTERIOR` | "exterior" | 外部 |

---

## AudioEngine API

音频引擎负责音频参数配置。

### 接口定义

```kotlin
interface IAudioEngine {
    val currentConfigFlow: Flow<AudioConfig?>
    val audioStateFlow: Flow<AudioState>
    
    suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioConfig>
    suspend fun applyConfig(config: AudioConfig): Result<Unit>
    suspend fun setEqualizerBand(band: EqualizerBand): Result<Unit>
    suspend fun setEqualizerPreset(presetName: String): Result<Unit>
    suspend fun setBassBoost(level: Double): Result<Unit>
    suspend fun setTrebleBoost(level: Double): Result<Unit>
    suspend fun enableSpatialAudio(enabled: Boolean): Result<Unit>
    suspend fun setSpatialMode(mode: String): Result<Unit>
    suspend fun resetToDefault(): Result<Unit>
    fun getCurrentConfig(): AudioConfig?
    fun getAudioState(): AudioState
    fun getAvailablePresets(): List<String>
}
```

### 方法

#### generateAudioConfig

根据场景生成音频配置。

```kotlin
suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioConfig>
```

**示例：**
```kotlin
val result = audioEngine.generateAudioConfig(scene)
result.onSuccess { config ->
    println("音频配置: 预设=${config.equalizer?.preset_name}")
}
```

---

#### setEqualizerPreset

设置均衡器预设。

```kotlin
suspend fun setEqualizerPreset(presetName: String): Result<Unit>
```

**可用预设：**
| 常量 | 值 | 说明 |
|------|-----|------|
| `AudioPresets.FLAT` | "flat" | 平坦 |
| `AudioPresets.BASS_BOOST` | "bass_boost" | 低音增强 |
| `AudioPresets.TREBLE_BOOST` | "treble_boost" | 高音增强 |
| `AudioPresets.VOCAL` | "vocal" | 人声 |
| `AudioPresets.ROCK` | "rock" | 摇滚 |
| `AudioPresets.POP` | "pop" | 流行 |
| `AudioPresets.CLASSICAL` | "classical" | 古典 |
| `AudioPresets.ELECTRONIC` | "electronic" | 电子 |
| `AudioPresets.JAZZ` | "jazz" | 爵士 |
| `AudioPresets.PODCAST` | "podcast" | 播客 |

---

#### setEqualizerBand

设置单个均衡器频段。

```kotlin
suspend fun setEqualizerBand(band: EqualizerBand): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `band` | EqualizerBand | 均衡器频段配置 |

**示例：**
```kotlin
val band = EqualizerBand(
    frequency_hz = 60,
    gain_db = 6.0
)
audioEngine.setEqualizerBand(band)
```

---

#### setBassBoost / setTrebleBoost

设置低音/高音增强。

```kotlin
suspend fun setBassBoost(level: Double): Result<Unit>
suspend fun setTrebleBoost(level: Double): Result<Unit>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `level` | Double | 增强级别（0.0 - maxBassBoost/maxTrebleBoost） |

---

#### enableSpatialAudio

启用/禁用空间音频。

```kotlin
suspend fun enableSpatialAudio(enabled: Boolean): Result<Unit>
```

---

#### setSpatialMode

设置空间音频模式。

```kotlin
suspend fun setSpatialMode(mode: String): Result<Unit>
```

**可用模式：**
| 常量 | 值 | 说明 |
|------|-----|------|
| `SpatialModes.STEREO` | "stereo" | 立体声 |
| `SpatialModes.SURROUND_5_1` | "surround_5_1" | 5.1 环绕声 |
| `SpatialModes.SURROUND_7_1` | "surround_7_1" | 7.1 环绕声 |
| `SpatialModes.DOLBY_ATMOS` | "dolby_atmos" | 杜比全景声 |
| `SpatialModes.BINAURAL` | "binaural" | 双耳 |

---

#### resetToDefault

重置到默认配置。

```kotlin
suspend fun resetToDefault(): Result<Unit>
```

---

## GenerationEngine API

生成引擎负责场景生成和效果协调。

### 接口定义

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
```

### 方法

#### generateScene

根据场景 ID 生成场景描述符。

```kotlin
suspend fun generateScene(sceneId: String): Result<SceneDescriptor>
```

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| `sceneId` | String | 场景 ID |

**返回：** 包含场景描述符的 Result

**示例：**
```kotlin
val result = generationEngine.generateScene("morning_commute")
result.onSuccess { scene ->
    println("场景: ${scene.scene_name}")
}
```

---

#### generateEffects

根据场景生成效果命令。

```kotlin
suspend fun generateEffects(scene: SceneDescriptor): Result<EffectCommands>
```

**返回：** 包含效果命令集合的 Result

---

#### start / stop

启动/停止生成引擎。

```kotlin
fun start()
fun stop()
```

---

## 数据模型说明

### SceneDescriptor

场景描述符，描述一个完整的场景配置。

```kotlin
data class SceneDescriptor(
    val version: String = "2.0",
    val scene_id: String,
    val scene_name: String? = null,
    val scene_narrative: String? = null,
    val intent: Intent = Intent(),
    val hints: Hints? = null,
    val announcement: Announcement? = null,
    val meta: SceneDescriptorMeta? = null
)
```

### Mood

情绪模型，使用效价-唤醒度模型。

```kotlin
data class Mood(
    val valence: Double = 0.5,    // 效价 (0.0-1.0)，正向情绪程度
    val arousal: Double = 0.5     // 唤醒度 (0.0-1.0)，情绪激活程度
)
```

### Intent

场景意图，包含情绪、能量等信息。

```kotlin
data class Intent(
    val mood: Mood = Mood(),
    val energy_level: Double = 0.5,
    val energy_curve: List<EnergyCurvePoint> = emptyList(),
    val atmosphere: String = "",
    val constraints: Constraints? = null,
    val user_overrides: UserOverrides? = null,
    val transition: Transition? = null
)
```

### Hints

场景提示，用于指导各引擎的生成。

```kotlin
data class Hints(
    val music: MusicHints? = null,
    val lighting: LightingHints? = null,
    val audio: AudioHints? = null
)
```

### Playlist

播放列表模型。

```kotlin
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val cover_url: String? = null,
    val owner: String? = null,
    val track_count: Int = 0,
    val total_duration_ms: Long = 0,
    val tracks: List<Track> = emptyList(),
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val is_public: Boolean = true,
    val source: String = "generated",
    val scene_id: String? = null,
    val tags: List<String> = emptyList()
)
```

### Track

音乐曲目模型。

```kotlin
data class Track(
    val id: String,
    val name: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val duration_ms: Long = 0,
    val popularity: Int = 0,
    val preview_url: String? = null,
    val external_urls: Map<String, String> = emptyMap()
)
```

### LightingConfig

灯光配置模型。

```kotlin
data class LightingConfig(
    val config_id: String,
    val scene: LightingScene,
    val transition_duration_ms: Long = 500,
    val active: Boolean = true,
    val created_at: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)
```

### ZoneConfig

区域灯光配置。

```kotlin
data class ZoneConfig(
    val zone_id: String,
    val enabled: Boolean = true,
    val color: ColorConfig = ColorConfig(),
    val brightness: Double = 1.0,
    val pattern: String = "static",
    val speed: Double = 1.0
)
```

### AudioConfig

音频配置模型。

```kotlin
data class AudioConfig(
    val config_id: String,
    val equalizer: EqualizerConfig? = null,
    val spatial: SpatialAudioConfig? = null,
    val volume: VolumeConfig = VolumeConfig(),
    val enhancements: AudioEnhancements = AudioEnhancements(),
    val active: Boolean = true,
    val created_at: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)
```

### EqualizerBand

均衡器频段。

```kotlin
data class EqualizerBand(
    val frequency_hz: Int,
    val gain_db: Double = 0.0
)
```

### EffectCommands

效果命令集合。

```kotlin
data class EffectCommands(
    val commands: List<EffectCommand> = emptyList(),
    val sequenceId: String,
    val sceneId: String? = null
)
```

### EffectCommand

单个效果命令。

```kotlin
data class EffectCommand(
    val commandId: String,
    val engineType: String,
    val action: String,
    val params: Map<String, Any> = emptyMap(),
    val priority: Int = 0
)
```

## 错误类型

SDK 使用 `Layer3Error` 封装所有错误。

```kotlin
sealed class Layer3Error : Exception {
    data class ContentError(override val message: String) : Layer3Error()
    data class LightingError(override val message: String) : Layer3Error()
    data class AudioError(override val message: String) : Layer3Error()
    data class GenerationError(
        override val message: String,
        val sceneId: String? = null
    ) : Layer3Error()
    data class NetworkError(override val message: String) : Layer3Error()
    data class ConfigurationError(override val message: String) : Layer3Error()
}
```

### 错误处理示例

```kotlin
val result = contentEngine.generatePlaylist(scene)

result.onFailure { error ->
    when (error) {
        is Layer3Error.ContentError -> {
            println("内容错误: ${error.message}")
        }
        is Layer3Error.NetworkError -> {
            println("网络错误: ${error.message}")
        }
        is Layer3Error.ConfigurationError -> {
            println("配置错误: ${error.message}")
        }
        else -> {
            println("未知错误: ${error.message}")
        }
    }
}
```

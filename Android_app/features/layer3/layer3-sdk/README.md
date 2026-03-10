# Layer3 SDK

Layer3 SDK 是一个场景生成引擎的 Android 实现，为智能座舱场景提供音乐内容推荐、灯光控制和音频配置功能。

## SDK 概述

Layer3 SDK 通过场景描述符（SceneDescriptor）来协调多个引擎，实现沉浸式的座舱体验。SDK 包含四个核心引擎：

- **ContentEngine**: 音乐播放列表生成和推荐
- **LightingEngine**: 场景灯光配置生成
- **AudioEngine**: 音频参数配置
- **GenerationEngine**: 场景效果协调执行

## 功能特性

### 音乐内容引擎
- 基于场景的智能播放列表生成
- 音乐评分与艺术家多样性过滤
- 播放状态管理与 Flow 响应式流
- 本地音乐库加载与缓存

### 灯光控制引擎
- 基于情绪的灯光主题推断
- 多区域灯光配置（仪表盘、车门、脚窝等）
- 多种灯光模式（静态、呼吸、脉冲、音乐同步）
- 颜色调整与主题管理

### 音频引擎
- 10 种预设均衡器模式
- 空间音频支持
- 低音/高音增强
- 自定义均衡器频段调节

### 场景生成引擎
- 场景模板加载与匹配
- 关键词场景匹配
- 效果命令协调生成
- LLM 集成支持

## 快速开始

### 环境要求

- Android SDK 26+ (Android 8.0)
- Kotlin 1.8+
- Java 8+

### 添加依赖

在模块的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.example.layer3:layer3-sdk:1.0.0'
}
```

### 初始化 SDK

```kotlin
import com.example.layer3.api.Layer3Config
import com.example.layer3.sdk.Layer3SDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = Layer3Config.Builder()
            .setEnableAutoTransition(true)
            .setTransitionDuration(500)
            .build()
        
        Layer3SDK.init(this, config)
    }
}
```

## 使用示例

### 生成场景效果

```kotlin
import com.example.layer3.sdk.Layer3SDK
import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.api.model.Mood
import com.example.layer3.api.model.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val generationEngine = Layer3SDK.getGenerationEngine()

CoroutineScope(Dispatchers.Main).launch {
    val sceneResult = generationEngine.generateScene("morning_commute")
    
    sceneResult.onSuccess { scene ->
        val effectsResult = generationEngine.generateEffects(scene)
        effectsResult.onSuccess { commands ->
            println("Generated ${commands.commands.size} effect commands")
        }
    }
}
```

### 创建自定义场景

```kotlin
import com.example.layer3.api.model.*

val customScene = SceneDescriptor(
    version = "2.0",
    scene_id = "custom_relax",
    scene_name = "放松时刻",
    scene_narrative = "适合下班后的轻松氛围",
    intent = Intent(
        mood = Mood(valence = 0.7, arousal = 0.3),
        energy_level = 0.3,
        atmosphere = "calm"
    ),
    hints = Hints(
        music = MusicHints(
            genres = listOf("jazz", "lo-fi"),
            tempo = "slow"
        ),
        lighting = LightingHints(
            color_theme = "calm",
            pattern = "breathing",
            intensity = 0.4
        ),
        audio = AudioHints(
            preset = "jazz"
        )
    )
)

val contentEngine = Layer3SDK.getContentEngine()
CoroutineScope(Dispatchers.Main).launch {
    val playlist = contentEngine.generatePlaylist(customScene)
    playlist.onSuccess {
        contentEngine.playPlaylist(it)
    }
}
```

### 监听状态变化

```kotlin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

lifecycleScope.launch {
    contentEngine.currentTrackFlow.collect { track ->
        track?.let { updateNowPlaying(it) }
    }
}

lifecycleScope.launch {
    Layer3SDK.getLightingEngine().lightingStateFlow.collect { state ->
        updateLightingUI(state)
    }
}

lifecycleScope.launch {
    Layer3SDK.getAudioEngine().audioStateFlow.collect { state ->
        updateAudioUI(state)
    }
}
```

### 灯光控制

```kotlin
val lightingEngine = Layer3SDK.getLightingEngine()

CoroutineScope(Dispatchers.Main).launch {
    lightingEngine.setBrightness(0.8)
    lightingEngine.setColor("#FF6B6B")
    lightingEngine.setPattern("pulse")
    lightingEngine.enableMusicSync(true)
}
```

### 音频配置

```kotlin
val audioEngine = Layer3SDK.getAudioEngine()

CoroutineScope(Dispatchers.Main).launch {
    audioEngine.setEqualizerPreset("electronic")
    audioEngine.setBassBoost(0.3)
    audioEngine.enableSpatialAudio(true)
    audioEngine.setSpatialMode("surround_5_1")
}
```

## API 参考

详细 API 文档请参阅 [API_REFERENCE.md](docs/API_REFERENCE.md)

### 核心类

| 类名 | 描述 |
|------|------|
| `Layer3SDK` | SDK 主入口，单例对象 |
| `Layer3Config` | SDK 配置类 |
| `SceneDescriptor` | 场景描述符数据模型 |

### 引擎接口

| 接口 | 描述 |
|------|------|
| `IContentEngine` | 音乐内容引擎接口 |
| `ILightingEngine` | 灯光控制引擎接口 |
| `IAudioEngine` | 音频引擎接口 |
| `IGenerationEngine` | 场景生成引擎接口 |

### 数据模型

| 模型 | 描述 |
|------|------|
| `Playlist` | 播放列表 |
| `Track` | 音乐曲目 |
| `LightingConfig` | 灯光配置 |
| `AudioConfig` | 音频配置 |
| `EffectCommands` | 效果命令集合 |

## 常见问题

### Q: SDK 初始化失败怎么办？

确保在 Application 的 `onCreate()` 中初始化 SDK，并传入正确的 Context：

```kotlin
Layer3SDK.init(context.applicationContext, config)
```

### Q: 如何检查 SDK 是否已初始化？

```kotlin
if (Layer3SDK.isInitialized()) {
    val contentEngine = Layer3SDK.getContentEngine()
}
```

### Q: 播放列表生成返回空怎么办？

检查 `assets/music_library.json` 文件是否存在且格式正确。音乐库文件应包含有效的曲目数据。

### Q: 如何自定义灯光区域？

通过 `LightingEngine.getAvailableZones()` 获取可用区域列表，然后使用 `setZoneConfig()` 配置：

```kotlin
val zones = lightingEngine.getAvailableZones()
zones.forEach { zone ->
    lightingEngine.setZoneConfig(zone, ZoneConfig(
        zone_id = zone,
        enabled = true,
        color = ColorConfig(hex = "#FF0000"),
        brightness = 0.8,
        pattern = "static"
    ))
}
```

### Q: 如何清理资源？

在不需要使用 SDK 时调用 `destroy()` 方法：

```kotlin
Layer3SDK.destroy()
```

### Q: 如何处理异步操作的错误？

所有异步方法返回 `Result<T>` 类型，使用 `onSuccess` 和 `onFailure` 处理：

```kotlin
val result = contentEngine.generatePlaylist(scene)
result.onSuccess { playlist ->
    // 处理成功
}.onFailure { error ->
    // 处理错误
    when (error) {
        is Layer3Error.ContentError -> println("内容错误: ${error.message}")
        else -> println("未知错误: ${error.message}")
    }
}
```

## 更多文档

- [集成指南](docs/INTEGRATION_GUIDE.md) - 详细的集成步骤
- [API 参考](docs/API_REFERENCE.md) - 完整的 API 文档
- [架构说明](docs/ARCHITECTURE.md) - SDK 架构设计

## 依赖

- `layer3-api`: 接口定义模块
- `kotlinx-coroutines-android`: Kotlin 协程
- `gson`: JSON 解析

## 版本历史

查看 [CHANGELOG.md](CHANGELOG.md) 了解版本更新历史。

## 许可证

Copyright © 2024 Example. All rights reserved.

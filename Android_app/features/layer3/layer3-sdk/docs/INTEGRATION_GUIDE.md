# Layer3 SDK 集成指南

本指南将帮助您在 Android 项目中集成 Layer3 SDK。

## 目录

- [环境要求](#环境要求)
- [添加依赖](#添加依赖)
- [初始化 SDK](#初始化-sdk)
- [配置选项](#配置选项)
- [使用示例](#使用示例)
- [注意事项](#注意事项)

## 环境要求

在集成 Layer3 SDK 之前，请确保您的开发环境满足以下要求：

| 要求 | 版本 |
|------|------|
| Android SDK | 26+ (Android 8.0) |
| Kotlin | 1.8+ |
| Java | 8+ |
| Gradle | 7.0+ |

## 添加依赖

### 方式一：本地模块依赖

如果 SDK 作为项目子模块存在，在 `settings.gradle` 中添加：

```groovy
include ':layer3-api'
include ':layer3-sdk'
project(':layer3-api').projectDir = new File('src/android_app/layer3-api')
project(':layer3-sdk').projectDir = new File('src/android_app/layer3-sdk')
```

然后在应用模块的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation project(':layer3-sdk')
}
```

### 方式二：Maven 依赖

在项目根目录的 `build.gradle` 中添加 Maven 仓库：

```groovy
allprojects {
    repositories {
        mavenLocal()
        // 或其他 Maven 仓库
    }
}
```

在应用模块的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.example.layer3:layer3-sdk:1.0.0'
}
```

## 初始化 SDK

### 基础初始化

在您的 `Application` 类中初始化 SDK：

```kotlin
import android.app.Application
import com.example.layer3.api.Layer3Config
import com.example.layer3.sdk.Layer3SDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = Layer3Config.Builder()
            .build()
        
        Layer3SDK.init(this, config)
    }
}
```

### 在 AndroidManifest.xml 中注册 Application

```xml
<application
    android:name=".MyApplication"
    ... >
    <!-- 其他配置 -->
</application>
```

## 配置选项

### Layer3Config 配置项

Layer3Config 提供了丰富的配置选项，您可以根据需要进行定制：

```kotlin
val config = Layer3Config.Builder()
    .setContentProvider(
        ContentProviderConfig(
            providerType = "local",
            cacheSizeMb = 100,
            enableOfflineMode = true
        )
    )
    .setLightingController(
        LightingControllerConfig(
            controllerType = "mock",
            autoReconnect = true,
            reconnectIntervalMs = 3000,
            timeoutMs = 5000
        )
    )
    .setAudioEngine(
        AudioEngineConfig(
            defaultPreset = "flat",
            enableSpatialAudio = false,
            defaultSpatialMode = "stereo",
            maxBassBoost = 0.5,
            maxTrebleBoost = 0.5
        )
    )
    .setGenerationEngine(
        GenerationEngineConfig(
            llmModel = "qwen-plus",
            maxTokens = 2000,
            temperature = 0.7
        )
    )
    .setEnableAutoTransition(true)
    .setTransitionDuration(500)
    .build()

Layer3SDK.init(this, config)
```

### 配置项说明

#### ContentProviderConfig

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `providerType` | String | "local" | 内容提供者类型 |
| `apiKey` | String | "" | API 密钥 |
| `apiSecret` | String | "" | API 密钥密文 |
| `baseUrl` | String | "" | API 基础 URL |
| `cacheSizeMb` | Int | 100 | 缓存大小（MB） |
| `enableOfflineMode` | Boolean | true | 是否启用离线模式 |

#### LightingControllerConfig

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `controllerType` | String | "mock" | 控制器类型 |
| `connectionAddress` | String | "" | 连接地址 |
| `connectionPort` | Int | 0 | 连接端口 |
| `autoReconnect` | Boolean | true | 是否自动重连 |
| `reconnectIntervalMs` | Long | 3000 | 重连间隔（毫秒） |
| `timeoutMs` | Long | 5000 | 超时时间（毫秒） |

#### AudioEngineConfig

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `defaultPreset` | String | "flat" | 默认均衡器预设 |
| `enableSpatialAudio` | Boolean | false | 是否启用空间音频 |
| `defaultSpatialMode` | String | "stereo" | 默认空间音频模式 |
| `maxBassBoost` | Double | 0.5 | 最大低音增强值 |
| `maxTrebleBoost` | Double | 0.5 | 最大高音增强值 |

#### GenerationEngineConfig

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `llmApiKey` | String | "" | LLM API 密钥 |
| `llmBaseUrl` | String | "" | LLM API 基础 URL |
| `llmModel` | String | "qwen-plus" | LLM 模型名称 |
| `maxTokens` | Int | 2000 | 最大 Token 数 |
| `temperature` | Double | 0.7 | 生成温度 |
| `templatePath` | String | "" | 模板路径 |

## 使用示例

### 完整集成示例

```kotlin
import android.app.Application
import com.example.layer3.api.*
import com.example.layer3.api.model.*
import com.example.layer3.sdk.Layer3SDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        initLayer3SDK()
    }
    
    private fun initLayer3SDK() {
        val config = Layer3Config.Builder()
            .setContentProvider(
                ContentProviderConfig(
                    providerType = "local",
                    enableOfflineMode = true
                )
            )
            .setAudioEngine(
                AudioEngineConfig(
                    defaultPreset = "flat",
                    enableSpatialAudio = true
                )
            )
            .setEnableAutoTransition(true)
            .setTransitionDuration(500)
            .build()
        
        Layer3SDK.init(this, config)
        
        observeEngineStates()
    }
    
    private fun observeEngineStates() {
        val scope = CoroutineScope(Dispatchers.Main)
        
        scope.launch {
            Layer3SDK.getContentEngine().currentTrackFlow.collect { track ->
                track?.let { 
                    println("当前播放: ${it.name}")
                }
            }
        }
        
        scope.launch {
            Layer3SDK.getLightingEngine().lightingStateFlow.collect { state ->
                println("灯光状态: 亮度=${state.brightness}, 模式=${state.current_pattern}")
            }
        }
        
        scope.launch {
            Layer3SDK.getAudioEngine().audioStateFlow.collect { state ->
                println("音频状态: 预设=${state.eq_preset}")
            }
        }
    }
}
```

### 在 Activity 中使用

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.layer3.api.model.*
import com.example.layer3.sdk.Layer3SDK
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupSceneButton()
    }
    
    private fun setupSceneButton() {
        findViewById<Button>(R.id.btnRelaxScene).setOnClickListener {
            applyRelaxScene()
        }
    }
    
    private fun applyRelaxScene() {
        lifecycleScope.launch {
            val scene = SceneDescriptor(
                version = "2.0",
                scene_id = "relax",
                scene_name = "放松模式",
                intent = Intent(
                    mood = Mood(valence = 0.7, arousal = 0.3),
                    energy_level = 0.3,
                    atmosphere = "calm"
                ),
                hints = Hints(
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
            
            val generationEngine = Layer3SDK.getGenerationEngine()
            val result = generationEngine.generateEffects(scene)
            
            result.onSuccess { commands ->
                println("场景应用成功，共 ${commands.commands.size} 个命令")
            }.onFailure { error ->
                println("场景应用失败: ${error.message}")
            }
        }
    }
}
```

### 动态更新配置

```kotlin
fun updateSDKConfig() {
    val newConfig = Layer3Config.Builder()
        .setEnableAutoTransition(false)
        .build()
    
    Layer3SDK.updateConfig(newConfig)
}
```

## 注意事项

### 1. 线程安全

SDK 的所有引擎方法都是线程安全的，但建议在主线程进行 UI 相关操作：

```kotlin
lifecycleScope.launch {
    val result = Layer3SDK.getContentEngine().generatePlaylist(scene)
    withContext(Dispatchers.Main) {
        result.onSuccess { updateUI(it) }
    }
}
```

### 2. 内存管理

- 在 Activity/Fragment 销毁时取消协程
- 避免在 Application 中持有 Activity 引用

```kotlin
class MainActivity : AppCompatActivity() {
    private var sceneJob: Job? = null
    
    private fun loadScene() {
        sceneJob = lifecycleScope.launch {
            // 场景加载逻辑
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sceneJob?.cancel()
    }
}
```

### 3. 资源释放

在应用退出时释放 SDK 资源：

```kotlin
override fun onTerminate() {
    super.onTerminate()
    Layer3SDK.destroy()
}
```

### 4. 错误处理

所有异步操作返回 `Result<T>`，请务必处理错误情况：

```kotlin
lifecycleScope.launch {
    val result = contentEngine.generatePlaylist(scene)
    
    result.onSuccess { playlist ->
        println("播放列表生成成功: ${playlist.name}")
    }.onFailure { error ->
        when (error) {
            is Layer3Error.ContentError -> {
                println("内容错误: ${error.message}")
            }
            is Layer3Error.NetworkError -> {
                println("网络错误: ${error.message}")
            }
            else -> {
                println("未知错误: ${error.message}")
            }
        }
    }
}
```

### 5. ProGuard 配置

如果使用 ProGuard 混淆，请添加以下规则：

```proguard
-keep class com.example.layer3.** { *; }
-keepclassmembers class com.example.layer3.** { *; }
```

### 6. 资源文件

确保以下资源文件存在于 `assets` 目录：

- `music_library.json` - 音乐库数据
- `preset_templates.json` - 预设模板
- `scene_keyword_mapping.json` - 场景关键词映射

### 7. 权限要求

SDK 本身不需要特殊权限，但如果您需要网络功能，请在 `AndroidManifest.xml` 中添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 8. 初始化时机

建议在 Application 的 `onCreate()` 中初始化 SDK，避免在其他组件中使用时 SDK 尚未初始化：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Layer3SDK.init(this, Layer3Config.Builder().build())
    }
}
```

### 9. 重复初始化

SDK 支持重复调用 `init()`，后续调用会更新配置而不是重新初始化：

```kotlin
Layer3SDK.init(context, config1)
Layer3SDK.init(context, config2)
```

### 10. 协程作用域

建议使用生命周期感知的协程作用域：

```kotlin
class MainActivity : AppCompatActivity() {
    private val mainScope = MainScope()
    
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}
```

## 下一步

- 查看 [API 参考](API_REFERENCE.md) 了解详细的 API 文档
- 查看 [架构说明](ARCHITECTURE.md) 了解 SDK 的设计架构

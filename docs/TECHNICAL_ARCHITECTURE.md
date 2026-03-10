# AI 音乐推荐系统 - 技术架构文档

## 一、系统整体架构

```mermaid
graph TB
    subgraph 用户层["👤 用户层"]
        DRIVER["驾驶员"]
        PASSENGER["乘客"]
    end

    subgraph 硬件层["🔧 硬件层"]
        CAMERA["车载摄像头"]
        MIC["麦克风"]
        GPS["GPS 定位"]
        AMBIENT_LIGHT["氛围灯"]
        SPEAKER["音响系统"]
    end

    subgraph Android_App["📱 Android 应用层"]
        MAIN_APP["app-main<br/>主应用"]
        
        subgraph SDK层["SDK 层"]
            L1["Layer 1: 感知层<br/>module-perception"]
            L2["Layer 2: 语义层<br/>module-semantic"]
            L3["Layer 3: 生成层<br/>layer3-sdk"]
            LM["本地音乐<br/>module-localmusic"]
        end
        
        subgraph 核心层["核心层"]
            CORE_API["core-api<br/>数据模型定义"]
        end
    end

    subgraph 外部服务["☁️ 外部服务"]
        AI_VISION["Qwen-VL-Max<br/>视觉分析"]
        AI_LLM["Qwen-Plus<br/>大语言模型"]
        WEATHER["Open-Meteo<br/>天气服务"]
    end

    DRIVER --> MAIN_APP
    PASSENGER --> MAIN_APP
    
    CAMERA --> L1
    MIC --> L1
    GPS --> L1
    
    L1 -->|"StandardizedSignals"| CORE_API
    CORE_API -->|"StandardizedSignals"| L2
    L2 -->|"SceneDescriptor"| CORE_API
    CORE_API -->|"SceneDescriptor"| L3
    LM -->|"Track 数据"| L3
    L3 -->|"EffectCommands"| CORE_API
    CORE_API -->|"EffectCommands"| MAIN_APP
    
    MAIN_APP --> AMBIENT_LIGHT
    MAIN_APP --> SPEAKER
    
    L1 --> AI_VISION
    L1 --> WEATHER
    L2 --> AI_LLM

    style DRIVER fill:#e1f5fe
    style PASSENGER fill:#e1f5fe
    style L1 fill:#fff3e0
    style L2 fill:#f3e5f5
    style L3 fill:#e8f5e9
    style CORE_API fill:#fce4ec
```

---

## 二、三层架构详解

```mermaid
graph LR
    subgraph Layer1["Layer 1: 感知层"]
        direction TB
        L1_INPUT["输入源"]
        L1_SENSOR["传感器管理<br/>SensorManager"]
        L1_CAMERA["摄像头<br/>CameraSource"]
        L1_AI["AI 分析<br/>AIClient"]
        L1_WEATHER["天气服务<br/>WeatherService"]
        L1_OUTPUT["StandardizedSignals<br/>标准化信号"]
        
        L1_INPUT --> L1_SENSOR
        L1_INPUT --> L1_CAMERA
        L1_SENSOR --> L1_AI
        L1_CAMERA --> L1_AI
        L1_AI --> L1_OUTPUT
        L1_WEATHER --> L1_OUTPUT
    end

    subgraph Layer2["Layer 2: 语义层"]
        direction TB
        L2_INPUT["StandardizedSignals"]
        L2_RULES["规则引擎<br/>RulesEngine<br/>⚡ 快通道"]
        L2_LLM["大模型<br/>LlmClient<br/>🧠 慢通道"]
        L2_TEMPLATE["模板管理<br/>TemplateManager"]
        L2_OUTPUT["SceneDescriptor<br/>场景描述"]
        
        L2_INPUT --> L2_RULES
        L2_INPUT --> L2_LLM
        L2_RULES --> L2_TEMPLATE
        L2_LLM --> L2_TEMPLATE
        L2_TEMPLATE --> L2_OUTPUT
    end

    subgraph Layer3["Layer 3: 生成层"]
        direction TB
        L3_INPUT["SceneDescriptor"]
        L3_CONTENT["内容引擎<br/>ContentEngine<br/>🎵 音乐推荐"]
        L3_LIGHT["灯光引擎<br/>LightingEngine<br/>💡 场景灯光"]
        L3_AUDIO["音频引擎<br/>AudioEngine<br/>🔊 音效配置"]
        L3_OUTPUT["EffectCommands<br/>效果命令"]
        
        L3_INPUT --> L3_CONTENT
        L3_INPUT --> L3_LIGHT
        L3_INPUT --> L3_AUDIO
        L3_CONTENT --> L3_OUTPUT
        L3_LIGHT --> L3_OUTPUT
        L3_AUDIO --> L3_OUTPUT
    end

    Layer1 -->|"数据流"| Layer2
    Layer2 -->|"数据流"| Layer3

    style Layer1 fill:#fff3e0
    style Layer2 fill:#f3e5f5
    style Layer3 fill:#e8f5e9
```

---

## 三、数据流架构

```mermaid
sequenceDiagram
    participant C as 车载传感器
    participant L1 as Layer1 感知层
    participant L2 as Layer2 语义层
    participant L3 as Layer3 生成层
    participant H as 硬件设备

    Note over C,H: 实时场景感知与响应流程

    C->>L1: 采集环境数据<br/>(图像、GPS、音频)
    L1->>L1: AI 分析 + 传感器融合
    L1->>L2: StandardizedSignals<br/>(标准化信号)
    
    L2->>L2: 规则匹配 / LLM 推理
    L2->>L3: SceneDescriptor<br/>(场景描述)
    
    L3->>L3: 音乐推荐 + 灯光配置 + 音效设置
    L3->>H: EffectCommands<br/>(效果命令)
    
    H->>H: 播放音乐 + 调整灯光 + 音效处理

    Note over C,H: 响应时间: 快通道 < 100ms, 慢通道 < 2s
```

---

## 四、Android 模块架构

```mermaid
graph TB
    subgraph App层["App 层"]
        APP_MAIN["app-main<br/>主应用入口"]
        APP_DEMO_L1["app-demo-perception<br/>感知层 Demo"]
        APP_DEMO_L2["app-demo-semantic<br/>语义层 Demo"]
        APP_DEMO_L3["app-demo-layer3<br/>生成层 Demo"]
        APP_DEMO_LM["app-demo-localmusic<br/>本地音乐 Demo"]
    end

    subgraph SDK层["SDK 层"]
        PERCEPTION_API["module-perception-api<br/>感知层接口"]
        PERCEPTION["module-perception<br/>感知层实现"]
        SEMANTIC["module-semantic<br/>语义层实现"]
        LAYER3_API["layer3-api<br/>生成层接口"]
        LAYER3_SDK["layer3-sdk<br/>生成层实现"]
        LOCAL_MUSIC["module-localmusic<br/>本地音乐实现"]
    end

    subgraph Core层["Core 层"]
        CORE["core-api<br/>数据模型定义"]
    end

    APP_MAIN --> PERCEPTION
    APP_MAIN --> SEMANTIC
    APP_MAIN --> LAYER3_SDK
    APP_MAIN --> LOCAL_MUSIC
    
    APP_DEMO_L1 --> PERCEPTION
    APP_DEMO_L2 --> SEMANTIC
    APP_DEMO_L3 --> LAYER3_SDK
    APP_DEMO_LM --> LOCAL_MUSIC

    PERCEPTION --> PERCEPTION_API
    PERCEPTION --> CORE
    SEMANTIC --> CORE
    LAYER3_SDK --> LAYER3_API
    LAYER3_SDK --> CORE
    LOCAL_MUSIC --> CORE
    LAYER3_API --> CORE

    style CORE fill:#fce4ec
    style PERCEPTION fill:#fff3e0
    style SEMANTIC fill:#f3e5f5
    style LAYER3_SDK fill:#e8f5e9
    style LOCAL_MUSIC fill:#e3f2fd
```

---

## 五、技术栈

```mermaid
mindmap
  root((AI 音乐推荐系统))
    前端
      Android App
        Kotlin
        Jetpack Compose
        Hilt 依赖注入
        Coroutines
        Flow
    后端服务
      Node.js
        Express
        音乐推荐引擎
        缓存系统
    AI 服务
      视觉分析
        Qwen-VL-Max
      语言模型
        Qwen-Plus
    数据存储
      本地存储
        SQLite
        JSON Assets
      缓存
        内存缓存
        文件缓存
    硬件接口
      传感器
        GPS
        麦克风
        摄像头
      输出设备
        氛围灯
        音响系统
```

---

## 六、核心数据模型

```mermaid
classDiagram
    class StandardizedSignals {
        +Signals signals
        +Vehicle vehicle
        +Environment environment
        +DriverState driver_state
        +Context context
    }

    class SceneDescriptor {
        +String scene_id
        +String scene_type
        +Intent intent
        +Hints hints
        +String announcement
    }

    class EffectCommands {
        +String scene_id
        +Commands commands
        +ExecutionReport execution_report
    }

    class Commands {
        +ContentCommand content
        +LightingCommand lighting
        +AudioCommand audio
    }

    class ContentCommand {
        +String action
        +List~Track~ playlist
        +String play_mode
    }

    class LightingCommand {
        +String action
        +String theme
        +List~String~ colors
        +Double intensity
    }

    class AudioCommand {
        +String action
        +String preset
        +AudioSettings settings
    }

    StandardizedSignals --> SceneDescriptor : Layer2 处理
    SceneDescriptor --> EffectCommands : Layer3 生成
    EffectCommands --> Commands : 包含
    Commands --> ContentCommand
    Commands --> LightingCommand
    Commands --> AudioCommand
```

---

## 七、场景化灯光主题

```mermaid
graph LR
    subgraph 场景输入["场景输入"]
        S1["早晨通勤"]
        S2["夜间驾驶"]
        S3["雨天行车"]
        S4["海边度假"]
        S5["夕阳驾驶"]
        S6["家庭出行"]
        S7["运动健身"]
        S8["冥想放松"]
    end

    subgraph 灯光主题["灯光主题"]
        T1["spring<br/>🌸 浅绿 + 浅粉"]
        T2["citynight<br/>🌃 午夜蓝 + 霓虹金"]
        T3["rainy<br/>🌧️ 钢蓝 + 板岩灰"]
        T4["ocean<br/>🌊 深青 + 浅海洋绿"]
        T5["sunset<br/>🌅 夕阳橙 + 金黄"]
        T6["warm<br/>☀️ 暖橙 + 柔黄"]
        T7["energetic<br/>⚡ 电光蓝 + 霓虹粉"]
        T8["meditation<br/>🧘 中紫 + 薰衣草"]
    end

    S1 --> T1
    S2 --> T2
    S3 --> T3
    S4 --> T4
    S5 --> T5
    S6 --> T6
    S7 --> T7
    S8 --> T8

    style T1 fill:#e8f5e9
    style T2 fill:#1a237e,color:#fff
    style T3 fill:#455a64,color:#fff
    style T4 fill:#00838f,color:#fff
    style T5 fill:#ff6f00,color:#fff
    style T6 fill:#ffb300
    style T7 fill:#e91e63,color:#fff
    style T8 fill:#7b1fa2,color:#fff
```

---

## 八、音乐推荐算法

```mermaid
flowchart TB
    subgraph 输入["输入"]
        SCENE["场景描述<br/>SceneDescriptor"]
        LIB["音乐库<br/>MusicLibrary"]
    end

    subgraph 评分["评分算法"]
        ENERGY["能量匹配<br/>Energy Score"]
        VALENCE["情绪匹配<br/>Valence Score"]
        GENRE["流派匹配<br/>Genre Score"]
        BPM["节拍匹配<br/>BPM Score"]
        DIVERSITY["艺术家多样性<br/>Artist Diversity"]
    end

    subgraph 输出["输出"]
        PLAYLIST["推荐歌单<br/>Playlist"]
    end

    SCENE --> ENERGY
    SCENE --> VALENCE
    SCENE --> GENRE
    SCENE --> BPM
    
    LIB --> ENERGY
    LIB --> VALENCE
    LIB --> GENRE
    LIB --> BPM
    
    ENERGY --> DIVERSITY
    VALENCE --> DIVERSITY
    GENRE --> DIVERSITY
    BPM --> DIVERSITY
    
    DIVERSITY --> PLAYLIST

    style PLAYLIST fill:#e8f5e9
```

---

## 九、系统部署架构

```mermaid
graph TB
    subgraph 车载端["🚗 车载端"]
        ANDROID["Android 设备"]
        
        subgraph 应用["应用"]
            MAIN["主应用"]
            SDK["SDK 模块"]
        end
        
        subgraph 数据["数据"]
            ASSETS["Assets 数据"]
            CACHE["本地缓存"]
        end
    end

    subgraph 云端["☁️ 云端"]
        AI_SERVICE["AI 服务<br/>Qwen"]
        WEATHER_SERVICE["天气服务<br/>Open-Meteo"]
    end

    subgraph 硬件["🔧 硬件"]
        CAMERA["摄像头"]
        SENSOR["传感器"]
        LIGHT["氛围灯"]
        AUDIO["音响"]
    end

    ANDROID --> 应用
    应用 --> 数据
    
    MAIN --> SDK
    SDK --> ASSETS
    SDK --> CACHE
    
    SDK --> AI_SERVICE
    SDK --> WEATHER_SERVICE
    
    SDK --> LIGHT
    SDK --> AUDIO
    
    CAMERA --> SDK
    SENSOR --> SDK

    style ANDROID fill:#e3f2fd
    style AI_SERVICE fill:#fff3e0
    style HARDWARE fill:#f5f5f5
```

---

## 十、关键性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 快通道响应 | < 100ms | 规则匹配场景 |
| 慢通道响应 | < 2s | LLM 推理场景 |
| 缓存命中率 | > 90% | 歌单缓存 |
| 内存占用 | < 200MB | 应用运行时 |
| 启动时间 | < 3s | 冷启动 |
| 音乐推荐准确率 | > 85% | 用户满意度 |

---

## 十一、开发状态

| 模块 | 状态 | 进度 |
|------|------|------|
| core-api | ✅ 完成 | 100% |
| module-perception | ✅ 完成 | 100% |
| module-semantic | ✅ 完成 | 100% |
| layer3-sdk | ✅ 完成 | 100% |
| module-localmusic | ✅ 完成 | 100% |
| app-main | ✅ 完成 | 100% |
| app-demo-* | ✅ 完成 | 100% |

---

## 十二、联系方式

- **项目仓库**: GitHub - ai-music-cowork
- **文档位置**: `/docs` 目录
- **开发状态**: `Android_app/DEVELOPMENT_STATUS.md`

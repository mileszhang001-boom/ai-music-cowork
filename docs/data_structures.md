# 关键数据结构规范

本文档定义了三层架构中每一层的输入/输出 JSON 数据结构，明确标注必填字段和可选字段。

---

## 字段标注说明

| 标记 | 含义 |
|------|------|
| `*` | **必填** - 该字段必须存在，否则校验失败 |
| `?` | **可选** - 该字段可以省略，系统会使用默认值 |
| `(条件)` | **条件必填** - 在特定条件下必须存在 |

---

## Layer 1: 物理感知层

### 输入: RawSignal (原始信号)

```json
{
  "source": "vhal",              // * 必填 - 信号源类型
  "type": "vehicle_speed",       // * 必填 - 信号类型
  "value": { "speed_kmh": 70 },  // * 必填 - 信号值
  "timestamp": 1709123456789,    // ? 可选 - 时间戳(默认当前时间)
  "confidence": 0.95,            // ? 可选 - 置信度(默认1.0)
  "metadata": {}                 // ? 可选 - 元数据
}
```

#### 信号源类型 (source)

| 值 | 说明 | 支持的 type |
|----|------|-------------|
| `vhal` | 车辆硬件抽象层 | `vehicle_speed`, `passenger_count`, `gear_position`, `fuel_level`, `door_status`, `window_status` |
| `environment` | 环境数据 | `time_of_day`, `weather`, `temperature`, `humidity`, `light_level` |
| `biometric` | 生物传感器 | `heart_rate`, `fatigue_level`, `stress_level` |
| `voice` | 语音输入 | `user_query` |
| `user_profile` | 用户画像 | `preferences`, `history` |
| `music_state` | 播放状态 | `current_track`, `playback_state` |

#### 各信号类型的 value 结构

**vhal 信号:**
```json
// vehicle_speed
{ "speed_kmh": 70 }

// passenger_count  
{ "passenger_count": 3 }

// gear_position
{ "gear": "D" }

// fuel_level
{ "fuel_percent": 65 }
```

**environment 信号:**
```json
// time_of_day (0.0=00:00, 1.0=24:00)
{ "time_of_day": 0.35 }

// weather
{ "weather": "clear" }  // clear | sunny | cloudy | rain | snow | fog | storm

// temperature
{ "temperature": 22 }
```

**biometric 信号:**
```json
// heart_rate
{ "heart_rate": 72 }

// fatigue_level (0-1)
{ "fatigue_level": 0.85 }

// stress_level (0-1)
{ "stress_level": 0.3 }
```

**voice 信号:**
```json
// user_query
{
  "text": "来点嗨歌",
  "intent": "creative"  // creative | navigation | control | info
}
```

---

### 输出: StandardizedSignals (标准化信号)

```json
{
  "version": "1.0",                    // * 必填 - Schema 版本
  "timestamp": "2026-02-28T08:30:05Z", // * 必填 - ISO 时间戳
  "signals": {                         // * 必填 - 结构化信号
    "vehicle": {                       // ? 可选 - 车辆状态
      "speed_kmh": 70,                 // ? 可选 - 车速 (km/h)
      "passenger_count": 1,            // ? 可选 - 乘客数
      "gear": "D"                      // ? 可选 - 档位
    },
    "environment": {                   // ? 可选 - 环境状态
      "time_of_day": 0.35,             // ? 可选 - 时间 (0-1)
      "weather": "clear",              // ? 可选 - 天气
      "temperature": 22                // ? 可选 - 温度
    },
    "biometric": {                     // ? 可选 - 生物特征
      "heart_rate": 72,                // ? 可选 - 心率
      "fatigue_level": 0.2,            // ? 可选 - 疲劳度 (0-1)
      "stress_level": 0.3              // ? 可选 - 压力值 (0-1)
    },
    "user_query": null                 // ? 可选 - 用户语音请求 (null 或 Object)
  },
  "confidence": {                      // * 必填 - 置信度
    "overall": 0.85,                   // * 必填 - 综合置信度 (0-1)
    "by_source": {                     // ? 可选 - 各信号源置信度
      "vhal": 0.95,
      "environment": 0.90,
      "biometric": 0.75
    }
  },
  "raw_signals": [],                   // ? 可选 - 原始信号数组
  "_meta": {                           // ? 可选 - 元数据
    "output_id": "perception_1709123456789",
    "total_count": 5,
    "high_confidence_count": 3,
    "active_sources": ["vhal", "environment", "biometric"]
  }
}
```

#### 字段详细说明

| 字段路径 | 必填 | 类型 | 范围/枚举 | 说明 |
|----------|------|------|-----------|------|
| `version` | * | string | `"1.0"` | Schema 版本号 |
| `timestamp` | * | string | ISO 8601 | 输出时间戳 |
| `signals.vehicle.speed_kmh` | ? | number | 0-300 | 车速 km/h |
| `signals.vehicle.passenger_count` | ? | integer | 0-8 | 乘客数量 |
| `signals.vehicle.gear` | ? | string | P/R/N/D | 档位 |
| `signals.environment.time_of_day` | ? | number | 0-1 | 一天中的时间比例 |
| `signals.environment.weather` | ? | string | clear/sunny/cloudy/rain/snow/fog/storm | 天气状况 |
| `signals.environment.temperature` | ? | number | -40~60 | 摄氏温度 |
| `signals.biometric.heart_rate` | ? | number | 40-200 | 心率 bpm |
| `signals.biometric.fatigue_level` | ? | number | 0-1 | 疲劳程度 |
| `signals.biometric.stress_level` | ? | number | 0-1 | 压力程度 |
| `signals.user_query` | ? | object/null | - | 用户语音请求 |
| `signals.user_query.text` | ? | string | - | 语音文本 |
| `signals.user_query.intent` | ? | string | creative/navigation/control/info | 意图类型 |
| `signals.user_query.confidence` | ? | number | 0-1 | 识别置信度 |
| `confidence.overall` | * | number | 0-1 | 综合置信度 |
| `confidence.by_source` | ? | object | - | 按信号源的置信度 |

---

## Layer 2: 语义推理层

### 输入: StandardizedSignals (来自 Layer 1)

即 Layer 1 的输出，参见上文。

---

### 输出: Scene Descriptor (场景描述符)

```json
{
  "version": "2.0",                    // * 必填 - Schema 版本
  "scene_id": "scene_1709123456789",   // * 必填 - 场景唯一ID
  "scene_type": "morning_commute",     // * 必填 - 场景类型
  "scene_name": "早晨通勤",             // ? 可选 - 场景名称
  "scene_narrative": "清新的早晨，独自驾车上班", // ? 可选 - 场景描述
  "timestamp": "2026-02-28T08:30:05Z", // ? 可选 - 时间戳
  
  "intent": {                          // * 必填 - 意图对象
    "mood": {                          // * 必填 - 情绪维度
      "valence": 0.6,                  // * 必填 - 效价 (负面0 → 正面1)
      "arousal": 0.4                   // * 必填 - 唤醒度 (平静0 → 兴奋1)
    },
    "energy_level": 0.4,               // * 必填 - 能量级别 (0-1)
    "energy_curve": [0.3, 0.4, 0.5],   // ? 可选 - 能量曲线 (时间序列)
    "curve_duration_minutes": 20,      // ? 可选 - 曲线时长
    "atmosphere": "fresh_morning",     // ? 可选 - 氛围描述
    "social_context": "solo",          // ? 可选 - 社交语境 (solo/couple/family/group)
    "trip_phase": "commute",           // ? 可选 - 行程阶段
    "trip_progress": 0.3,              // ? 可选 - 行程进度 (0-1)
    "constraints": {                   // ? 可选 - 硬约束 (引擎必须满足)
      "content_rating": "PG",          // ? 可选 - 内容分级 (G/PG/PG-13/R)
      "max_volume_db": 70,             // ? 可选 - 最大音量
      "avoid_sudden_changes": false,   // ? 可选 - 避免突变
      "brightness_max": 0.8            // ? 可选 - 最大亮度
    },
    "user_overrides": {                // ? 可选 - 用户覆盖
      "exclude_tags": ["heavy_metal"], // ? 可选 - 排除标签
      "preferred_language": ["zh"]     // ? 可选 - 偏好语言
    },
    "transition": {                    // ? 可选 - 过渡配置
      "type": "fade",                  // ? 可选 - 过渡类型 (fade/cut/crossfade)
      "style": "smooth",               // ? 可选 - 过渡风格
      "duration_sec": 5                // ? 可选 - 过渡时长(秒)
    }
  },
  
  "hints": {                           // * 必填 - 提示对象 (软建议)
    "music": {                         // ? 可选 - 音乐提示
      "genres": ["pop", "indie"],      // ? 可选 - 推荐流派
      "tempo": "moderate",             // ? 可选 - 节奏 (slow/moderate/fast)
      "suggested_tempo": [100, 120],   // ? 可选 - BPM 范围
      "suggested_artists": [],         // ? 可选 - 推荐艺术家
      "vocal_style": "bright"          // ? 可选 - 人声风格
    },
    "lighting": {                      // ? 可选 - 灯光提示
      "color_theme": "warm",           // ? 可选 - 颜色主题
      "pattern": "steady",             // ? 可选 - 动效模式
      "intensity": 0.4                 // ? 可选 - 亮度 (0-1)
    },
    "audio": {                         // ? 可选 - 音效提示
      "preset": "standard",            // ? 可选 - 预设名称
      "spatial": "front_stage",        // ? 可选 - 空间音频
      "bass_level": "moderate"         // ? 可选 - 低音级别
    }
  },
  
  "announcement": {                    // ? 可选 - 播报配置
    "text": "早安，为您准备了清新的晨间音乐", // ? 可选 - 播报文本
    "timing": "immediate",             // ? 可选 - 播报时机 (immediate/delayed/with_music)
    "voice_style": "warm_female"       // ? 可选 - 语音风格
  },
  
  "meta": {                            // ? 可选 - 元数据
    "confidence": 0.85,                // ? 可选 - 置信度
    "source": "template",              // ? 可选 - 来源 (template/llm/fallback)
    "reasoning": "基于时间和天气匹配",   // ? 可选 - 推理说明
    "template_id": "morning_commute",  // ? 可选 - 模板ID
    "expires_at": "2026-02-28T08:50:05Z", // ? 可选 - 过期时间
    "low_confidence_dimensions": []    // ? 可选 - 低置信度维度
  }
}
```

#### 场景类型 (scene_type) 枚举

| 值 | 说明 | 触发条件 |
|----|------|----------|
| `morning_commute` | 早晨通勤 | time_of_day: 0.1-0.4, 低乘客数 |
| `night_drive` | 深夜驾驶 | time_of_day < 0.2, 低能量 |
| `road_trip` | 公路旅行 | 高速度, 高能量 |
| `romantic_date` | 浪漫约会 | 2乘客, 晚间 |
| `family_outing` | 家庭出行 | 乘客数 ≥ 3, 含儿童 |
| `focus_work` | 专注工作 | 高专注度, 低能量 |
| `traffic_jam` | 交通拥堵 | 低速度, 长时间 |
| `fatigue_alert` | 疲劳提醒 | fatigue_level > 0.7 |
| `rainy_night` | 雨夜 | weather=rain, time < 0.3 |
| `party` | 派对 | 高社交, 高能量 |

#### 内容分级 (content_rating) 枚举

| 值 | 说明 | 适用场景 |
|----|------|----------|
| `G` | 大众级 | 所有场景 |
| `PG` | 建议家长指导 | 普通场景 |
| `PG-13` | 13岁以上 | 无儿童场景 |
| `R` | 限制级 | 仅成人场景 |

#### 字段详细说明

| 字段路径 | 必填 | 类型 | 范围 | 说明 |
|----------|------|------|------|------|
| `version` | * | string | `"2.0"` | Schema 版本 |
| `scene_id` | * | string | - | 唯一场景标识 |
| `scene_type` | * | string | 见枚举 | 场景类型 |
| `scene_name` | ? | string | - | 场景显示名称 |
| `intent.mood.valence` | * | number | 0-1 | 情绪效价 |
| `intent.mood.arousal` | * | number | 0-1 | 情绪唤醒度 |
| `intent.energy_level` | * | number | 0-1 | 能量级别 |
| `intent.constraints.content_rating` | ? | string | G/PG/PG-13/R | 内容分级约束 |
| `intent.constraints.max_volume_db` | ? | number | 0-100 | 最大音量约束 |
| `hints.music.genres` | ? | string[] | - | 推荐音乐流派 |
| `hints.music.tempo` | ? | string | slow/moderate/fast | 节奏建议 |
| `hints.lighting.color_theme` | ? | string | calm/warm/alert/night/party | 灯光主题 |
| `hints.lighting.intensity` | ? | number | 0-1 | 灯光亮度 |
| `hints.audio.preset` | ? | string | standard/night_mode/bass_boost/vocal_clarity | 音效预设 |

---

## Layer 3: 效果生成层

### 输入: Scene Descriptor (来自 Layer 2)

即 Layer 2 的输出，参见上文。

---

### 输出: EffectCommands (效果指令)

```json
{
  "version": "1.0",                    // * 必填 - Schema 版本
  "scene_id": "scene_1709123456789",   // * 必填 - 关联的场景ID
  "commands": {                        // * 必填 - 指令集合
    "content": {                       // ? 可选 - 内容引擎指令
      "action": "play_playlist",       // * (条件) 必填 - 动作类型
      "playlist": [                    // * (条件) 必填 - 播放列表
        {
          "track_id": "track_001",     // * 必填 - 曲目ID
          "title": "清晨",             // * 必填 - 曲名
          "artist": "艺术家A",          // * 必填 - 艺术家
          "duration_sec": 210,         // ? 可选 - 时长
          "energy": 0.4,               // ? 可选 - 能量值
          "genres": ["pop", "indie"],  // ? 可选 - 流派
          "album": "专辑名",            // ? 可选 - 专辑
          "cover_url": "https://..."   // ? 可选 - 封面URL
        }
      ],
      "total_duration": 3600,          // ? 可选 - 总时长(秒)
      "play_mode": "sequential"        // ? 可选 - 播放模式 (sequential/random/repeat)
    },
    "lighting": {                      // ? 可选 - 灯光引擎指令
      "action": "apply_theme",         // * (条件) 必填 - 动作类型
      "theme": "warm",                 // ? 可选 - 主题名称
      "colors": ["#FFA500", "#FFE4B5"],// ? 可选 - 颜色数组
      "pattern": "steady",             // ? 可选 - 动效模式
      "intensity": 0.4,                // ? 可选 - 亮度 (0-1)
      "transition_ms": 5000,           // ? 可选 - 过渡时间(ms)
      "zones": ["dashboard", "door"]   // ? 可选 - 应用区域
    },
    "audio": {                         // ? 可选 - 音效引擎指令
      "action": "apply_preset",        // * (条件) 必填 - 动作类型
      "preset": "standard",            // ? 可选 - 预设名称
      "settings": {                    // ? 可选 - 音效设置
        "eq": {                        // ? 可选 - 均衡器
          "bass": 0,                   // ? 可选 - 低音 (-10 ~ +10)
          "mid": 0,                    // ? 可选 - 中音 (-10 ~ +10)
          "treble": 0                  // ? 可选 - 高音 (-10 ~ +10)
        },
        "spatial": "front_stage",      // ? 可选 - 空间音频模式
        "volume_db": 65                // ? 可选 - 音量
      }
    }
  },
  "execution_report": {                // ? 可选 - 执行报告
    "status": "completed",             // * (条件) 必填 - 执行状态
    "timestamp": "2026-02-28T08:30:05Z", // ? 可选 - 执行时间
    "execution_time_ms": 150,          // ? 可选 - 执行耗时
    "details": {                       // ? 可选 - 各引擎状态
      "content": "success",            // ? 可选 - 内容引擎状态
      "lighting": "success",           // ? 可选 - 灯光引擎状态
      "audio": "success"               // ? 可选 - 音效引擎状态
    },
    "errors": []                       // ? 可选 - 错误列表
  },
  "announcement": {                    // ? 可选 - 播报指令
    "text": "早安，为您准备了清新的晨间音乐", // ? 可选 - 播报文本
    "timing": "immediate",             // ? 可选 - 播报时机
    "voice_style": "warm_female"       // ? 可选 - 语音风格
  },
  "_meta": {                           // ? 可选 - 元数据
    "source_descriptor": "template",   // ? 可选 - 来源
    "processing_time_ms": 150          // ? 可选 - 处理耗时
  }
}
```

#### 引擎动作类型

**内容引擎 (content) 动作:**

| action | 说明 | 必要参数 |
|--------|------|----------|
| `play_playlist` | 播放列表 | `playlist` |
| `curate_playlist` | 生成播放列表 | `hints` |
| `skip_track` | 跳过当前曲目 | - |
| `adjust_energy` | 调整能量级别 | `energy_level` |

**灯光引擎 (lighting) 动作:**

| action | 说明 | 必要参数 |
|--------|------|----------|
| `apply_theme` | 应用主题 | `theme` 或 `colors` |
| `set_color` | 设置颜色 | `colors` |
| `set_pattern` | 设置动效 | `pattern` |
| `set_intensity` | 设置亮度 | `intensity` |

**音效引擎 (audio) 动作:**

| action | 说明 | 必要参数 |
|--------|------|----------|
| `apply_preset` | 应用预设 | `preset` |
| `set_eq` | 设置均衡器 | `settings.eq` |
| `set_spatial` | 设置空间音频 | `settings.spatial` |
| `set_volume` | 设置音量 | `settings.volume_db` |

#### 灯光主题 (theme) 预设

| theme | 颜色 | 适用场景 |
|-------|------|----------|
| `calm` | #4A90A4, #87CEEB | 深夜驾驶、专注 |
| `warm` | #FFA500, #FFE4B5 | 早晨通勤 |
| `alert` | #FF0000, #FFFFFF | 疲劳提醒 |
| `night` | #1E3A5F, #2C5F7C | 深夜驾驶 |
| `party` | #FF00FF, #00FFFF, #FFFF00 | 派对 |

#### 灯光动效模式 (pattern)

| pattern | 说明 |
|---------|------|
| `steady` | 稳定常亮 |
| `breathing` | 呼吸渐变 |
| `pulse` | 脉冲闪烁 |
| `wave` | 波浪流动 |
| `flash` | 快速闪烁 |

#### 音效预设 (preset)

| preset | EQ 设置 | 空间音频 | 适用场景 |
|--------|---------|----------|----------|
| `standard` | bass:0, mid:0, treble:0 | front_stage | 默认 |
| `night_mode` | bass:-2, mid:-1, treble:0 | immersive_soft | 深夜 |
| `bass_boost` | bass:4, mid:1, treble:0 | front_stage | 提神 |
| `vocal_clarity` | bass:-1, mid:2, treble:1 | front_stage | 播报 |
| `electronic` | bass:3, mid:0, treble:2 | immersive_deep | 派对 |

#### 空间音频模式 (spatial)

| spatial | 说明 |
|---------|------|
| `front_stage` | 前排舞台感 |
| `immersive_soft` | 轻度沉浸 |
| `immersive_deep` | 深度沉浸 |
| `surround` | 环绕声 |

#### 执行状态 (status)

| status | 说明 |
|--------|------|
| `pending` | 等待执行 |
| `executing` | 执行中 |
| `completed` | 已完成 |
| `failed` | 执行失败 |

---

## 数据流转示意

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         外部信号源                                        │
│  VHAL | Voice | Biometric | Environment | UserProfile | MusicState       │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ RawSignal[]
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer 1: 物理感知层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │   Normalizer │→ │   Validator  │→ │  Structurer  │                   │
│  └──────────────┘  └──────────────┘  └──────────────┘                   │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ StandardizedSignals
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer 2: 语义推理层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │  Recognizer  │→ │TemplateMatch │→ │  Validator   │                   │
│  └──────────────┘  └──────────────┘  └──────────────┘                   │
│                    [LLM Reasoner] (可选)                                 │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ Scene Descriptor
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer 3: 效果生成层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │ Orchestrator │→ │   Engines    │→ │  Validator   │                   │
│  └──────────────┘  └──────────────┘  └──────────────┘                   │
│                    ├─ ContentEngine                                      │
│                    ├─ LightingEngine                                     │
│                    └─ AudioEngine                                        │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ EffectCommands
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         硬件执行层                                        │
│  音响播放 | 氛围灯控制 | DSP 音效 | TTS 播报                               │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 校验规则汇总

### Layer 1 校验规则

1. `version` 必须为 `"1.0"`
2. `timestamp` 必须为有效 ISO 8601 格式
3. `signals` 必须为对象
4. `confidence.overall` 必须在 0-1 范围内
5. `signals.vehicle.speed_kmh` 范围 0-300
6. `signals.biometric.fatigue_level` 范围 0-1

### Layer 2 校验规则

1. `version` 必须为 `"2.0"`
2. `scene_id` 不能为空
3. `intent.mood.valence` 和 `arousal` 必须在 0-1 范围内
4. `intent.energy_level` 必须在 0-1 范围内
5. `hints` 必须存在 (可以为空对象)
6. `intent.constraints.content_rating` 必须为有效枚举值

### Layer 3 校验规则

1. `version` 必须为 `"1.0"`
2. `scene_id` 必须与 Layer 2 输出一致
3. `commands` 必须为对象
4. `commands.lighting.intensity` 必须在 0-1 范围内
5. `commands.audio.settings.volume_db` 范围 0-100
6. `execution_report.status` 必须为有效枚举值

---

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0 | 2026-02-28 | 初始版本，定义三层数据结构 |

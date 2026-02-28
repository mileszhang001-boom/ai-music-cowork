# 关键数据结构规范

本文档定义了三层架构中每一层的输入/输出 JSON 数据结构，明确标注必填字段和可选字段。

---

## 一、数据流转全貌

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         外部信号源                                        │
│  VHAL | Voice | Environment | ExternalCamera | InternalCamera | Mic      │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ RawSignal[]
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer 1: 物理感知层                                  │
│                                                                          │
│  职责: 接收原始信号，标准化为结构化数据                                    │
│  输入: RawSignal[] (原始信号数组)                                         │
│  输出: StandardizedSignals (标准化信号)                                   │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ StandardizedSignals
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer 2: 语义推理层                                  │
│                                                                          │
│  职责: 分析信号，识别场景，生成播放意图                                    │
│  输入: StandardizedSignals (来自 Layer 1)                                │
│  输出: Scene Descriptor (场景描述符)                                      │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ Scene Descriptor
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer 3: 效果生成层                                  │
│                                                                          │
│  职责: 根据场景描述，生成具体执行指令                                      │
│  输入: Scene Descriptor (来自 Layer 2)                                   │
│  输出: EffectCommands (效果指令)                                          │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ EffectCommands
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         硬件执行层                                        │
│  音响播放 | 氛围灯控制 | DSP 音效 | TTS 播报                               │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、字段标注说明

| 标记 | 含义 |
|------|------|
| `*` | **必填** - 该字段必须存在，否则校验失败 |
| `?` | **可选** - 该字段可以省略，系统会使用默认值 |
| `(条件)` | **条件必填** - 在特定条件下必须存在 |

---

## 三、Layer 数据结构

---

### Layer 1: 物理感知层

#### 输入: RawSignal (原始信号)

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

#### 输出: StandardizedSignals (标准化信号)

```json
{
  "version": "1.0",
  "timestamp": "2026-02-28T08:30:05Z",
  "signals": {
    "vehicle": {
      "speed_kmh": 70,
      "passenger_count": 1,
      "gear": "D"
    },
    "environment": {
      "time_of_day": 0.35,
      "weather": "clear",
      "temperature": 22,
      "date_type": "weekday"
    },
    "external_camera": {
      "primary_color": "#87CEEB",
      "secondary_color": "#FFFFFF",
      "brightness": 0.6,
      "scene_description": "city"
    },
    "internal_camera": {
      "mood": "happy",
      "confidence": 0.85,
      "passengers": { "children": 1, "adults": 2, "seniors": 0 }
    },
    "internal_mic": {
      "volume_level": 0.4,
      "has_voice": true,
      "voice_count": 2,
      "noise_level": 0.15
    },
    "user_query": null
  },
  "confidence": {
    "overall": 0.85,
    "by_source": { "vhal": 0.95, "environment": 0.90 }
  }
}
```

#### 字段说明

| 字段路径 | 必填 | 类型 | 说明 |
|----------|------|------|------|
| `version` | * | string | Schema 版本号 `"1.0"` |
| `timestamp` | * | string | ISO 8601 时间戳 |
| `signals.vehicle.speed_kmh` | ? | number | 车速 (0-300 km/h) |
| `signals.vehicle.passenger_count` | ? | integer | 乘客数量 (0-8) |
| `signals.environment.time_of_day` | ? | number | 时间比例 (0-1, 0=00:00, 1=24:00) |
| `signals.environment.weather` | ? | string | 天气: clear/sunny/cloudy/rain/snow/fog/storm |
| `signals.environment.date_type` | ? | string | 日期: weekday/weekend/holiday |
| `signals.external_camera.scene_description` | ? | string | 环境: city/highway/suburban/rural/tunnel/bridge/parking |
| `signals.external_camera.brightness` | ? | number | 亮度 (0-1) |
| `signals.internal_camera.mood` | ? | string | 心情: happy/calm/tired/stressed/neutral/excited |
| `signals.internal_camera.passengers` | ? | object | 乘客分布: children/adults/seniors |
| `signals.internal_mic.volume_level` | ? | number | 声音大小 (0-1) |
| `signals.internal_mic.has_voice` | ? | boolean | 是否有人声 |
| `signals.user_query` | ? | object | 用户语音请求 |
| `confidence.overall` | * | number | 综合置信度 (0-1) |

---

### Layer 2: 语义推理层

#### 输入: StandardizedSignals

即 Layer 1 的输出。

#### 输出: Scene Descriptor (场景描述符)

```json
{
  "version": "2.0",
  "scene_id": "scene_1709123456789",
  "scene_type": "morning_commute",
  "scene_name": "早晨通勤",
  "scene_narrative": "清新的早晨，独自驾车上班",
  
  "intent": {
    "mood": { "valence": 0.6, "arousal": 0.4 },
    "energy_level": 0.4,
    "atmosphere": "fresh_morning",
    "social_context": "solo",
    "constraints": {
      "content_rating": "PG",
      "max_volume_db": 70
    },
    "user_overrides": {
      "exclude_tags": ["heavy_metal"]
    }
  },
  
  "hints": {
    "music": { "genres": ["pop", "indie"], "tempo": "moderate" },
    "lighting": { "color_theme": "warm", "pattern": "steady", "intensity": 0.4 },
    "audio": { "preset": "standard" }
  },
  
  "announcement": "早安，为您准备了清新的晨间音乐",
  
  "meta": {
    "confidence": 0.85,
    "source": "template",
    "template_id": "morning_commute"
  }
}
```

#### 字段说明

| 字段路径 | 必填 | 类型 | 说明 |
|----------|------|------|------|
| `version` | * | string | Schema 版本 `"2.0"` |
| `scene_id` | * | string | 场景唯一标识 |
| `scene_type` | * | string | 场景类型 (见枚举) |
| `scene_name` | ? | string | 场景显示名称 |
| `intent.mood.valence` | * | number | 情绪效价 (0=负面, 1=正面) |
| `intent.mood.arousal` | * | number | 情绪唤醒度 (0=平静, 1=兴奋) |
| `intent.energy_level` | * | number | 能量级别 (0-1) |
| `intent.constraints.content_rating` | ? | string | 内容分级: G/PG/PG-13/R |
| `intent.constraints.max_volume_db` | ? | number | 最大音量 (0-100) |
| `hints.music.genres` | ? | string[] | 推荐音乐流派 |
| `hints.music.tempo` | ? | string | 节奏: slow/moderate/fast |
| `hints.lighting.color_theme` | ? | string | 灯光主题: calm/warm/alert/night/party |
| `hints.lighting.intensity` | ? | number | 灯光亮度 (0-1) |
| `hints.audio.preset` | ? | string | 音效预设: standard/night_mode/bass_boost |
| `announcement` | ? | string | 播报文本 |
| `meta.source` | ? | string | 来源: template/llm/fallback |

---

### Layer 3: 效果生成层

#### 输入: Scene Descriptor

即 Layer 2 的输出。

#### 输出: EffectCommands (效果指令)

```json
{
  "version": "1.0",
  "scene_id": "scene_1709123456789",
  "commands": {
    "content": {
      "action": "play_playlist",
      "playlist": [
        {
          "track_id": "track_001",
          "title": "清晨",
          "artist": "艺术家A",
          "duration_sec": 210,
          "energy": 0.4
        }
      ],
      "play_mode": "sequential"
    },
    "lighting": {
      "action": "apply_theme",
      "theme": "warm",
      "colors": ["#FFA500", "#FFE4B5"],
      "pattern": "steady",
      "intensity": 0.4
    },
    "audio": {
      "action": "apply_preset",
      "preset": "standard",
      "settings": {
        "eq": { "bass": 0, "mid": 0, "treble": 0 },
        "volume_db": 65
      }
    }
  },
  "execution_report": {
    "status": "completed",
    "details": { "content": "success", "lighting": "success", "audio": "success" }
  }
}
```

#### 字段说明

| 字段路径 | 必填 | 类型 | 说明 |
|----------|------|------|------|
| `version` | * | string | Schema 版本 `"1.0"` |
| `scene_id` | * | string | 关联的场景ID |
| `commands.content.action` | * | string | 动作: play_playlist/curate_playlist/skip_track |
| `commands.content.playlist` | * | array | 播放列表 |
| `commands.lighting.action` | * | string | 动作: apply_theme/set_color/set_pattern |
| `commands.lighting.intensity` | ? | number | 亮度 (0-1) |
| `commands.audio.action` | * | string | 动作: apply_preset/set_eq/set_volume |
| `commands.audio.settings.volume_db` | ? | number | 音量 (0-100) |
| `execution_report.status` | * | string | 状态: pending/executing/completed/failed |

---

## 四、枚举值参考

### 场景类型 (scene_type)

| 值 | 说明 | 触发条件 |
|----|------|----------|
| `morning_commute` | 早晨通勤 | 时间 06:00-09:00, 独自驾驶 |
| `night_drive` | 深夜驾驶 | 时间 < 06:00, 低能量 |
| `road_trip` | 公路旅行 | 高速度, 高能量 |
| `family_outing` | 家庭出行 | 有儿童或乘客 ≥ 3 |
| `fatigue_alert` | 疲劳提醒 | mood = tired |
| `rainy_night` | 雨夜 | weather=rain, 夜间 |
| `party` | 派对 | 高社交, 高能量 |

### 心情类型 (mood)

| 值 | 说明 | 能量映射 |
|----|------|----------|
| `happy` | 开心 | 0.7 |
| `excited` | 兴奋 | 0.9 |
| `calm` | 平静 | 0.3 |
| `neutral` | 中性 | 0.5 |
| `tired` | 疲劳 | 0.2 (触发疲劳提醒) |
| `stressed` | 压力 | 0.6 |

### 环境描述 (scene_description)

| 值 | 说明 |
|----|------|
| `city` | 城市道路 |
| `highway` | 高速公路 |
| `suburban` | 郊区道路 |
| `rural` | 乡村道路 |
| `tunnel` | 隧道 |
| `bridge` | 桥梁 |
| `parking` | 停车场 |

### 内容分级 (content_rating)

| 值 | 说明 | 适用场景 |
|----|------|----------|
| `G` | 大众级 | 所有场景 |
| `PG` | 建议家长指导 | 普通场景 |
| `PG-13` | 13岁以上 | 无儿童场景 |
| `R` | 限制级 | 仅成人场景 |

### 灯光主题 (theme)

| theme | 颜色 | 适用场景 |
|-------|------|----------|
| `calm` | #4A90A4, #87CEEB | 深夜、专注 |
| `warm` | #FFA500, #FFE4B5 | 早晨通勤 |
| `alert` | #FF0000, #FFFFFF | 疲劳提醒 |
| `night` | #1E3A5F, #2C5F7C | 深夜驾驶 |
| `party` | #FF00FF, #00FFFF | 派对 |

### 音效预设 (preset)

| preset | EQ 设置 | 适用场景 |
|--------|---------|----------|
| `standard` | flat | 默认 |
| `night_mode` | bass:-2, mid:-1 | 深夜 |
| `bass_boost` | bass:+4 | 提神 |
| `vocal_clarity` | mid:+2 | 播报 |

---

## 五、校验规则

### Layer 1

- `version` 必须为 `"1.0"`
- `signals` 必须为对象
- `confidence.overall` 范围 0-1
- `signals.external_camera.brightness` 范围 0-1
- `signals.internal_mic.volume_level` 范围 0-1

### Layer 2

- `version` 必须为 `"2.0"`
- `scene_id` 不能为空
- `intent.mood.valence/arousal` 范围 0-1
- `intent.energy_level` 范围 0-1
- `hints` 必须存在

### Layer 3

- `version` 必须为 `"1.0"`
- `scene_id` 必须与 Layer 2 一致
- `commands` 必须为对象
- `commands.lighting.intensity` 范围 0-1
- `commands.audio.settings.volume_db` 范围 0-100

---

## 六、版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| 1.0 | 2026-02-28 | 初始版本 |
| 1.1 | 2026-02-28 | 新增 external_camera, internal_camera, internal_mic；移除 biometric |
| 1.2 | 2026-02-28 | 简化 Layer 2 输出结构，移除冗余字段 |

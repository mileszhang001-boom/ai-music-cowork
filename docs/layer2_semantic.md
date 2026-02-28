# Layer 2: 语义推理层 (Semantic Layer)

## 概述

语义推理层负责接收 Layer 1 的标准化信号，进行场景识别、意图理解，并生成 `Scene Descriptor` JSON，供 Layer 3 效果生成层使用。

## 核心职责

1. **场景识别**: 基于 Signal 维度计算场景向量
2. **模板匹配**: 从模板库匹配最合适的场景模板
3. **Query 路由**: 处理用户语音请求，生成 ACK 响应
4. **LLM 推理**: (可选) 调用大模型进行精细化推理
5. **输出生成**: 构建符合 Schema 的 Scene Descriptor

---

## 关键数据结构

### 场景维度 (SceneDimensions)

```javascript
const SceneDimensions = {
  SOCIAL: 'social',         // 社交维度 (独处/多人)
  ENERGY: 'energy',         // 能量维度 (低/高)
  FOCUS: 'focus',           // 专注维度 (放松/专注)
  TIME_CONTEXT: 'time_context', // 时间维度 (早晨/深夜)
  WEATHER: 'weather'        // 天气维度
};
```

### 场景类型 (SceneTypes)

```javascript
const SceneTypes = {
  MORNING_COMMUTE: 'morning_commute',  // 早晨通勤
  NIGHT_DRIVE: 'night_drive',          // 深夜驾驶
  ROAD_TRIP: 'road_trip',              // 公路旅行
  ROMANTIC_DATE: 'romantic_date',      // 浪漫约会
  FAMILY_OUTING: 'family_outing',      // 家庭出行
  FOCUS_WORK: 'focus_work',            // 专注工作
  TRAFFIC_JAM: 'traffic_jam',          // 交通拥堵
  FATIGUE_ALERT: 'fatigue_alert',      // 疲劳提醒
  RAINY_NIGHT: 'rainy_night',          // 雨夜
  PARTY: 'party'                       // 派对
};
```

### 意图类型 (IntentTypes)

```javascript
const IntentTypes = {
  CREATIVE: 'creative',     // 创意请求 (如"来点嗨歌")
  NAVIGATION: 'navigation', // 导航相关
  CONTROL: 'control',       // 控制指令
  INFO: 'info'              // 信息查询
};
```

### 输入: StandardizedSignals (来自 Layer 1)

```json
{
  "version": "1.0",
  "signals": {
    "vehicle": { "speed_kmh": 70, "passenger_count": 0 },
    "environment": { "time_of_day": 0.15, "weather": "clear" },
    "biometric": { "heart_rate": 72 },
    "user_query": null
  },
  "confidence": { "overall": 0.85 }
}
```

### 输出: Scene Descriptor JSON

```json
{
  "version": "2.0",
  "scene_id": "scene_1709123456789",
  "scene_type": "morning_commute",
  "scene_name": "早晨通勤",
  "scene_narrative": "清新的早晨，独自驾车上班",
  "timestamp": "2026-02-28T08:30:05Z",
  
  "intent": {
    "mood": {
      "valence": 0.6,
      "arousal": 0.4
    },
    "energy_level": 0.4,
    "energy_curve": [0.3, 0.4, 0.5, 0.4, 0.3],
    "curve_duration_minutes": 20,
    "atmosphere": "fresh_morning",
    "social_context": "solo",
    "trip_phase": "commute",
    "trip_progress": 0.3,
    "constraints": {
      "content_rating": "PG",
      "max_volume_db": 70,
      "avoid_sudden_changes": false,
      "brightness_max": 0.8
    },
    "user_overrides": {
      "exclude_tags": ["heavy_metal"],
      "preferred_language": ["zh", "en"]
    },
    "transition": {
      "type": "fade",
      "style": "smooth",
      "duration_sec": 5
    }
  },
  
  "hints": {
    "music": {
      "suggested_tags": ["pop", "indie", "upbeat"],
      "suggested_tempo": [100, 120],
      "suggested_artists": [],
      "suggested_vocal_style": "bright"
    },
    "lighting": {
      "suggested_palette": ["warm_orange", "soft_white"],
      "suggested_rhythm": "steady"
    },
    "audio": {
      "suggested_spatial": "front_stage",
      "suggested_bass_level": "moderate"
    }
  },
  
  "announcement": {
    "text": "早安，为您准备了清新的晨间音乐",
    "timing": "immediate",
    "voice_style": "warm_female"
  },
  
  "meta": {
    "confidence": 0.85,
    "source": "template",
    "reasoning": "基于时间和天气匹配到早晨通勤模板",
    "fallback_template": "morning_commute",
    "expires_at": "2026-02-28T08:50:05Z",
    "low_confidence_dimensions": []
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `intent.mood.valence` | Number (0-1) | 情绪效价 (负面→正面) |
| `intent.mood.arousal` | Number (0-1) | 情绪唤醒度 (平静→兴奋) |
| `intent.energy_level` | Number (0-1) | 整体能量级别 |
| `intent.energy_curve` | Array | 能量曲线 (时间序列) |
| `intent.constraints` | Object | 硬约束 (必须满足) |
| `hints` | Object | 软建议 (可被引擎采纳或忽略) |
| `announcement` | Object | TTS 播报内容 |
| `meta.source` | String | 来源 (template/llm/fallback) |

---

## 调试方式

### 1. 运行验证脚本

```bash
# 完整验证
npm run test:layer2

# 仅验证 Descriptor 结构
node scripts/layer2/demo.js --validate

# 测试场景识别
node scripts/layer2/demo.js --scenes

# 查看模板库
node scripts/layer2/demo.js --templates

# 生成 Mock 输入数据
node scripts/layer2/demo.js --mock
```

### 2. 单元测试

```bash
npm test -- tests/unit/layer2.test.js
```

### 3. 代码中调试

```javascript
const { semanticLayer } = require('./src/layers/semantic');

// 准备 Layer 1 输出
const perceptionOutput = {
  version: '1.0',
  signals: {
    vehicle: { speed_kmh: 70, passenger_count: 0 },
    environment: { time_of_day: 0.15, weather: 'clear' },
    biometric: { heart_rate: 72 },
    user_query: null
  },
  confidence: { overall: 0.85 }
};

// 处理并生成 Scene Descriptor
const result = await semanticLayer.process(perceptionOutput, {
  enableLLM: false  // 禁用 LLM，仅使用模板
});

console.log('Scene Descriptor:', result.scene_descriptor);
console.log('Meta:', result.meta);
```

### 4. 验证输出

```javascript
const { validator } = require('./src/layers/semantic/validator');

const validation = validator.validate(descriptor);
console.log('Valid:', validation.valid);
console.log('Errors:', validation.errors);
```

### 5. 测试模板匹配

```javascript
const { templateLibrary } = require('./src/layers/semantic/templates/library');

// 查看所有模板
const templates = templateLibrary.getAllTemplates();
console.log('Templates:', templates.map(t => t.template_id));

// 匹配模板
const sceneVector = {
  dimensions: { energy: 0.4, social: 0.1, time_context: 0.15 },
  scene_type: 'morning_commute'
};
const matched = templateLibrary.matchTemplate(sceneVector);
console.log('Matched:', matched);
```

---

## 功能设计

### 模块结构

```
src/layers/semantic/
├── index.js           # 主入口 (SemanticLayer 类)
├── recognizer/
│   └── index.js       # 场景识别器
├── router/
│   └── index.js       # Query 路由器
├── templates/
│   └── library.js     # 模板库
├── validator.js       # 输出验证器
└── types.js           # 常量定义
```

### 核心类: SemanticLayer

```javascript
class SemanticLayer {
  constructor(config = {}) {
    this.llmReasoner = null;        // LLM 推理器 (可选)
    this.currentDescriptor = null;  // 当前 Descriptor
  }

  // 设置 LLM 推理器
  setLLMReasoner(reasoner) { ... }

  // 处理 Layer 1 输出，生成 Scene Descriptor
  async process(perceptionOutput, options = {}) { ... }

  // 从模板构建 Descriptor
  buildDescriptorFromTemplate(template, sceneVector) { ... }

  // 构建默认 Descriptor (兜底)
  buildDefaultDescriptor(sceneVector, signals) { ... }
}
```

### 处理流程

```
StandardizedSignals → 场景识别 → 模板匹配 → [LLM推理] → 验证 → Scene Descriptor
                           ↓
                      Query Router → ACK 生成
```

### 快慢双通道

```
┌─────────────────────────────────────────────────────────┐
│                    Semantic Layer                        │
├─────────────────────────────────────────────────────────┤
│  快通道 (<2s):                                           │
│    Layer 1 Output → 场景识别 → 模板匹配 → 基础 Descriptor  │
├─────────────────────────────────────────────────────────┤
│  慢通道 (5-15s):                                         │
│    Layer 1 Output → LLM 推理 → 精细化 Descriptor          │
└─────────────────────────────────────────────────────────┘
```

---

## 需要的测试数据

### Mock Layer 1 输出

```javascript
// 早晨通勤
const morningCommuteInput = {
  version: '1.0',
  signals: {
    vehicle: { speed_kmh: 70, passenger_count: 0 },
    environment: { time_of_day: 0.15, weather: 'clear' },
    biometric: { heart_rate: 72 },
    user_query: null
  },
  confidence: { overall: 0.85 }
};

// 深夜驾驶 + 用户请求
const nightDriveWithQuery = {
  version: '1.0',
  signals: {
    vehicle: { speed_kmh: 50, passenger_count: 0 },
    environment: { time_of_day: 0.05, weather: 'clear' },
    biometric: { heart_rate: 65 },
    user_query: { text: '我想听点放松的音乐', intent: 'creative', confidence: 0.9 }
  },
  confidence: { overall: 0.80 }
};

// 疲劳提醒
const fatigueAlertInput = {
  version: '1.0',
  signals: {
    vehicle: { speed_kmh: 60, passenger_count: 0 },
    environment: { time_of_day: 0.4, weather: 'clear' },
    biometric: { heart_rate: 58, fatigue_level: 0.85 },
    user_query: null
  },
  confidence: { overall: 0.75 }
};

// 家庭出行
const familyOutingInput = {
  version: '1.0',
  signals: {
    vehicle: { speed_kmh: 80, passenger_count: 3 },
    environment: { time_of_day: 0.6, weather: 'sunny' },
    biometric: { heart_rate: 85 },
    user_query: null
  },
  confidence: { overall: 0.90 }
};
```

### 现有 Mock 数据文件

- `mock_data/scene_late_night_drive.json` - 深夜驾驶场景
- `mock_data/scene_morning_commute.json` - 早晨通勤场景
- `mock_data/scene_rainy_night.json` - 雨夜场景
- `mock_data/scene_fatigue_alert.json` - 疲劳提醒场景
- `mock_data/scene_family_beach.json` - 家庭海滩场景
- `mock_data/ack_creative.json` - ACK 消息示例

---

## Schema 文件

JSON Schema 定义位于: `schemas/scene_descriptor.schema.json`

---

## 与其他层的关系

```
┌─────────────────────────────────────────────────────────┐
│              Layer 1: 物理感知层                         │
│              输出: StandardizedSignals                   │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 2: 语义推理层                         │
│  场景识别 → 模板匹配 → [LLM推理] → Descriptor 生成       │
└────────────────────────┬────────────────────────────────┘
                         ▼
                Scene Descriptor JSON
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 3: 效果生成层                         │
└─────────────────────────────────────────────────────────┘
```

---

## 模板库说明

### 预置模板列表

| Template ID | Scene Type | 描述 |
|-------------|------------|------|
| `morning_commute` | morning_commute | 早晨通勤 |
| `night_drive` | night_drive | 深夜驾驶 |
| `road_trip` | road_trip | 公路旅行 |
| `focus_work` | focus_work | 专注工作 |
| `fatigue_alert` | fatigue_alert | 疲劳提醒 |

### 模板结构

```javascript
{
  template_id: 'morning_commute',
  scene_type: 'morning_commute',
  name: '早晨通勤',
  description: '清新的早晨，独自驾车上班',
  intent: {
    mood: { valence: 0.6, arousal: 0.4 },
    energy_level: 0.4,
    atmosphere: 'fresh_morning'
  },
  hints: {
    music: { genres: ['pop', 'indie'], tempo: 'moderate' },
    lighting: { color_theme: 'warm', pattern: 'steady' },
    audio: { preset: 'standard' }
  },
  announcement_templates: [
    { text: '早安，为您准备了清新的晨间音乐', voice_style: 'warm_female' }
  ],
  triggers: {
    time_range: [0.1, 0.3],
    weather: ['clear', 'cloudy'],
    passenger_count: { max: 1 }
  }
}
```

---

## 常见问题

### Q: 如何添加新的场景模板?

1. 在 `templates/library.js` 的 `presetTemplates` 中添加新模板
2. 定义 `template_id`, `scene_type`, `intent`, `hints`
3. 设置 `triggers` 条件用于自动匹配

### Q: LLM 推理何时启用?

当 `enableLLM: true` 且设置了 `llmReasoner` 时，会调用 LLM 进行精细化推理。LLM 结果会增量更新模板匹配的结果。

### Q: 如何处理低置信度场景?

当 `confidence < 0.5` 时:
1. 系统会回退到基于"时间+天气"的安全模板
2. `meta.low_confidence_dimensions` 会标记不确定的维度
3. 不会执行激进的场景切换

### Q: ACK 消息如何生成?

当 `signals.user_query` 存在时:
1. Query Router 分析意图类型
2. 根据意图类型选择 ACK 模板
3. 生成即时语音响应 (<1s)

# Layer 3: 效果生成层 (Effects Layer)

## 概述

效果生成层负责接收 Layer 2 的 Scene Descriptor，解析意图和提示，协调内容、灯光、音效三大引擎，生成最终的效果指令 JSON，驱动车载硬件执行。

## 核心职责

1. **Descriptor 解析**: 解析 Scene Descriptor 的 intent 和 hints
2. **引擎协调**: 协调内容、灯光、音效三大引擎
3. **指令生成**: 生成各引擎可执行的指令
4. **过渡控制**: 管理场景切换的过渡效果
5. **反馈生成**: 生成执行报告供上层使用

---

## 关键数据结构

### 引擎类型 (EngineTypes)

```javascript
const EngineTypes = {
  CONTENT: 'content',   // 内容引擎 (音乐播放)
  LIGHTING: 'lighting', // 灯光引擎 (氛围灯)
  AUDIO: 'audio'        // 音效引擎 (EQ、空间音频)
};
```

### 执行状态 (EffectStatus)

```javascript
const EffectStatus = {
  PENDING: 'pending',     // 等待执行
  EXECUTING: 'executing', // 执行中
  COMPLETED: 'completed', // 已完成
  FAILED: 'failed'        // 执行失败
};
```

### 输入: Scene Descriptor (来自 Layer 2)

```json
{
  "version": "2.0",
  "scene_id": "scene_1709123456789",
  "scene_type": "morning_commute",
  "intent": {
    "mood": { "valence": 0.6, "arousal": 0.4 },
    "energy_level": 0.4,
    "atmosphere": "fresh_morning",
    "constraints": {
      "content_rating": "PG",
      "max_volume_db": 70
    },
    "transition": {
      "type": "fade",
      "duration_sec": 5
    }
  },
  "hints": {
    "music": { "genres": ["pop", "indie"], "tempo": "moderate" },
    "lighting": { "color_theme": "warm", "pattern": "steady", "intensity": 0.4 },
    "audio": { "preset": "standard" }
  },
  "announcement": {
    "text": "早安，为您准备了清新的晨间音乐",
    "voice_style": "warm_female"
  }
}
```

### 输出: EffectCommands JSON

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
          "energy": 0.4,
          "genres": ["pop", "indie"]
        }
      ],
      "total_duration": 3600,
      "play_mode": "sequential"
    },
    "lighting": {
      "action": "apply_theme",
      "theme": "warm",
      "colors": ["#FFA500", "#FFE4B5"],
      "pattern": "steady",
      "intensity": 0.4,
      "transition_ms": 5000
    },
    "audio": {
      "action": "apply_preset",
      "preset": "standard",
      "settings": {
        "eq": { "bass": 0, "mid": 0, "treble": 0 },
        "spatial": "front_stage",
        "volume_db": 65
      }
    }
  },
  "execution_report": {
    "status": "completed",
    "timestamp": "2026-02-28T08:30:05Z",
    "execution_time_ms": 150,
    "details": {
      "content": "success",
      "lighting": "success",
      "audio": "success"
    }
  },
  "announcement": {
    "text": "早安，为您准备了清新的晨间音乐",
    "timing": "immediate",
    "voice_style": "warm_female"
  },
  "_meta": {
    "source_descriptor": "template",
    "processing_time_ms": 150
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `commands.content` | Object | 内容引擎指令 |
| `commands.lighting` | Object | 灯光引擎指令 |
| `commands.audio` | Object | 音效引擎指令 |
| `execution_report.status` | String | 执行状态 |
| `execution_report.details` | Object | 各引擎执行详情 |

---

## 调试方式

### 1. 运行验证脚本

```bash
# 完整验证
npm run test:layer3

# 仅验证效果指令结构
node scripts/layer3/demo.js --validate

# 测试各引擎
node scripts/layer3/demo.js --engines

# 模拟输出
node scripts/layer3/demo.js --simulate

# 生成 Mock 输入数据
node scripts/layer3/demo.js --mock
```

### 2. 代码中调试

```javascript
const { effectsLayer } = require('./src/layers/effects');

// 准备 Scene Descriptor
const descriptor = {
  version: '2.0',
  scene_id: 'scene_test',
  scene_type: 'morning_commute',
  intent: {
    mood: { valence: 0.6, arousal: 0.4 },
    energy_level: 0.4,
    atmosphere: 'fresh_morning'
  },
  hints: {
    music: { genres: ['pop'], tempo: 'moderate' },
    lighting: { color_theme: 'warm', pattern: 'steady', intensity: 0.4 },
    audio: { preset: 'standard' }
  }
};

// 处理并生成效果指令
const output = await effectsLayer.process(descriptor);
console.log('Commands:', output.commands);
console.log('Status:', output.execution_report.status);
```

### 3. 单独测试引擎

```javascript
const { contentEngine } = require('./src/layers/effects/engines/content');
const { lightingEngine } = require('./src/layers/effects/engines/lighting');
const { audioEngine } = require('./src/layers/effects/engines/audio');

// 测试内容引擎
const contentResult = await contentEngine.execute('curate_playlist', {
  hints: { genres: ['jazz', 'lo-fi'], tempo: 'slow' }
});
console.log('Playlist:', contentResult.playlist);

// 测试灯光引擎
const lightingResult = await lightingEngine.execute('apply_theme', {
  theme: 'calm',
  pattern: 'breathing',
  intensity: 0.3
});
console.log('Lighting:', lightingResult);

// 测试音效引擎
const audioResult = await audioEngine.execute('apply_preset', {
  preset: 'night_mode'
});
console.log('Audio:', audioResult);
```

### 4. 验证输出

```javascript
const { validator } = require('./src/layers/effects/validator');

const validation = validator.validate(output);
console.log('Valid:', validation.valid);
console.log('Errors:', validation.errors);
```

---

## 功能设计

### 模块结构

```
src/layers/effects/
├── index.js              # 主入口 (EffectsLayer 类)
├── orchestrator/
│   └── index.js          # 编排协调器
├── engines/
│   ├── content/
│   │   └── index.js      # 内容引擎
│   ├── lighting/
│   │   └── index.js      # 灯光引擎
│   └── audio/
│       └── index.js      # 音效引擎
├── validator.js          # 输出验证器
└── types.js              # 常量定义
```

### 核心类: EffectsLayer

```javascript
class EffectsLayer {
  constructor(config = {}) {
    this.config = { debug: config.debug || false };
  }

  // 处理 Scene Descriptor，生成效果指令
  async process(descriptor) { ... }

  // 获取指定引擎
  getEngine(type) { ... }

  // 获取执行历史
  getHistory(limit = 10) { ... }
}
```

### 编排协调器 (Orchestrator)

```javascript
class Orchestrator {
  // 注册引擎
  registerEngine(type, engine) { ... }

  // 执行所有引擎
  async execute(descriptor) { ... }

  // 获取引擎
  getEngine(type) { ... }
}
```

### 处理流程

```
Scene Descriptor → Orchestrator → 并行执行引擎 → 汇总结果 → 验证 → EffectCommands
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
     ContentEngine  LightingEngine  AudioEngine
```

---

## 三大引擎详解

### 1. 内容引擎 (Content Engine)

**职责**: 根据意图和提示生成播放列表

**支持的动作**:
- `curate_playlist`: 生成播放列表
- `play_playlist`: 播放指定列表
- `skip_track`: 跳过当前曲目
- `adjust_energy`: 调整能量级别

**输出结构**:
```json
{
  "action": "play_playlist",
  "playlist": [
    {
      "track_id": "track_001",
      "title": "歌曲名",
      "artist": "艺术家",
      "duration_sec": 210,
      "energy": 0.4,
      "genres": ["pop"]
    }
  ],
  "total_duration": 3600,
  "play_mode": "sequential"
}
```

### 2. 灯光引擎 (Lighting Engine)

**职责**: 根据意图和提示控制氛围灯

**支持的动作**:
- `apply_theme`: 应用灯光主题
- `set_color`: 设置颜色
- `set_pattern`: 设置动效模式
- `set_intensity`: 设置亮度

**预设主题**:
```javascript
const ColorThemes = {
  calm: ['#4A90A4', '#87CEEB'],
  warm: ['#FFA500', '#FFE4B5'],
  alert: ['#FF0000', '#FFFFFF'],
  night: ['#1E3A5F', '#2C5F7C'],
  party: ['#FF00FF', '#00FFFF', '#FFFF00']
};
```

**动效模式**:
- `steady`: 稳定
- `breathing`: 呼吸
- `pulse`: 脉冲
- `wave`: 波浪
- `flash`: 闪烁

**输出结构**:
```json
{
  "action": "apply_theme",
  "theme": "warm",
  "colors": ["#FFA500", "#FFE4B5"],
  "pattern": "steady",
  "intensity": 0.4,
  "transition_ms": 5000
}
```

### 3. 音效引擎 (Audio Engine)

**职责**: 根据意图和提示调整音效参数

**支持的动作**:
- `apply_preset`: 应用预设
- `set_eq`: 设置均衡器
- `set_spatial`: 设置空间音频
- `set_volume`: 设置音量

**预设配置**:
```javascript
const AudioPresets = {
  standard: { bass: 0, mid: 0, treble: 0, spatial: 'front_stage' },
  night_mode: { bass: -2, mid: -1, treble: 0, spatial: 'immersive_soft' },
  bass_boost: { bass: 4, mid: 1, treble: 0, spatial: 'front_stage' },
  vocal_clarity: { bass: -1, mid: 2, treble: 1, spatial: 'front_stage' },
  electronic: { bass: 3, mid: 0, treble: 2, spatial: 'immersive_deep' }
};
```

**输出结构**:
```json
{
  "action": "apply_preset",
  "preset": "standard",
  "settings": {
    "eq": { "bass": 0, "mid": 0, "treble": 0 },
    "spatial": "front_stage",
    "volume_db": 65
  }
}
```

---

## 需要的测试数据

### Mock Scene Descriptor 输入

```javascript
// 早晨通勤
const morningCommuteDescriptor = {
  version: '2.0',
  scene_id: 'scene_morning',
  scene_type: 'morning_commute',
  intent: {
    mood: { valence: 0.6, arousal: 0.4 },
    energy_level: 0.4,
    atmosphere: 'fresh_morning'
  },
  hints: {
    music: { genres: ['pop', 'indie'], tempo: 'moderate' },
    lighting: { color_theme: 'warm', pattern: 'steady', intensity: 0.4 },
    audio: { preset: 'standard' }
  },
  announcement: '早安，为您准备了清新的晨间音乐'
};

// 深夜驾驶
const nightDriveDescriptor = {
  version: '2.0',
  scene_id: 'scene_night',
  scene_type: 'night_drive',
  intent: {
    mood: { valence: 0.5, arousal: 0.2 },
    energy_level: 0.2,
    atmosphere: 'serene_night'
  },
  hints: {
    music: { genres: ['jazz', 'lo-fi'], tempo: 'slow' },
    lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.2 },
    audio: { preset: 'night_mode' }
  },
  announcement: '深夜路况良好，为您切换至静谧夜行模式'
};

// 疲劳提醒
const fatigueAlertDescriptor = {
  version: '2.0',
  scene_id: 'scene_fatigue',
  scene_type: 'fatigue_alert',
  intent: {
    mood: { valence: 0.4, arousal: 0.9 },
    energy_level: 0.9,
    atmosphere: 'alert_wake_up',
    constraints: { avoid_sudden_changes: false }
  },
  hints: {
    music: { genres: ['electronic', 'rock'], tempo: 'fast' },
    lighting: { color_theme: 'alert', pattern: 'flash', intensity: 0.8 },
    audio: { preset: 'bass_boost' }
  },
  announcement: '检测到您有点疲劳，为您切换到提神模式'
};
```

### 现有 Mock 数据文件

- `mock_data/scene_late_night_drive.json` - 深夜驾驶场景
- `mock_data/scene_morning_commute.json` - 早晨通勤场景
- `mock_data/scene_fatigue_alert.json` - 疲劳提醒场景
- `mock_data/feedback_report_001.json` - 反馈报告示例

---

## Schema 文件

JSON Schema 定义位于: `schemas/feedback_report.schema.json`

---

## 与其他层的关系

```
┌─────────────────────────────────────────────────────────┐
│              Layer 2: 语义推理层                         │
│              输出: Scene Descriptor                      │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 3: 效果生成层                         │
│  Orchestrator 协调三大引擎生成效果指令                   │
└────────────────────────┬────────────────────────────────┘
                         ▼
                EffectCommands JSON
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    硬件执行层                            │
│  音响播放 | 氛围灯控制 | DSP 音效                        │
└─────────────────────────────────────────────────────────┘
```

---

## Event Bus 通信

效果层通过 Event Bus 实现引擎间实时协作：

```javascript
// 内容引擎广播节拍事件
eventBus.emit('music.beat', { bpm: 120, intensity: 0.6 });

// 灯光引擎监听节拍
eventBus.on('music.beat', (data) => {
  lightingEngine.syncToBeat(data);
});

// 音效引擎监听节拍
eventBus.on('music.beat', (data) => {
  audioEngine.syncToBeat(data);
});
```

---

## 常见问题

### Q: 如何添加新的引擎?

1. 在 `engines/` 下创建新引擎目录
2. 实现 `execute(action, params)` 方法
3. 在 `index.js` 中注册引擎到 Orchestrator

### Q: 引擎执行失败如何处理?

- 单个引擎失败不会影响其他引擎
- `execution_report.details` 会标记失败的引擎
- 整体状态会根据失败情况设置

### Q: 如何处理硬约束 (constraints)?

引擎必须满足 `intent.constraints` 中的约束:
- `content_rating`: 内容分级过滤
- `max_volume_db`: 最大音量限制
- `brightness_max`: 最大亮度限制
- `avoid_sudden_changes`: 避免突变

### Q: hints 和 constraints 的区别?

- `constraints`: 硬约束，引擎**必须**满足
- `hints`: 软建议，引擎**可以**采纳或忽略

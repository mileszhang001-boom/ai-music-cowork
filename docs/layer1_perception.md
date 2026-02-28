# Layer 1: 物理感知层 (Perception Layer)

## 概述

物理感知层负责从各种车载传感器和外部数据源收集原始信号，进行标准化处理，并输出结构化的 `StandardizedSignals` JSON，供 Layer 2 语义推理层使用。

## 核心职责

1. **信号采集**: 从 6 种信号源采集原始数据
2. **信号标准化**: 将异构数据转换为统一格式
3. **置信度计算**: 评估每个信号的可靠程度
4. **输出验证**: 确保输出符合 Schema 规范

---

## 关键数据结构

### 信号源类型 (SignalSources)

```javascript
const SignalSources = {
  VHAL: 'vhal',           // 车辆硬件抽象层 (速度、档位、乘客数)
  VOICE: 'voice',         // 语音输入 (用户query)
  BIOMETRIC: 'biometric', // 生物传感器 (心率、疲劳度)
  ENVIRONMENT: 'environment', // 环境数据 (时间、天气、温度)
  USER_PROFILE: 'user_profile', // 用户画像
  MUSIC_STATE: 'music_state'    // 当前播放状态
};
```

### 信号分类 (SignalCategories)

```javascript
const SignalCategories = {
  CONTEXT: 'context',      // 上下文信号 (车辆状态、音乐状态)
  USER_STATE: 'user_state', // 用户状态 (生物特征、画像)
  USER_INTENT: 'user_intent', // 用户意图 (语音query)
  ENVIRONMENT: 'environment'  // 环境信号 (时间、天气)
};
```

### 输出 JSON 结构 (StandardizedSignals)

```json
{
  "version": "1.0",
  "timestamp": "2026-02-28T08:30:05Z",
  "signals": {
    "vehicle": {
      "speed_kmh": 70,
      "passenger_count": 0,
      "gear": "D"
    },
    "environment": {
      "time_of_day": 0.15,
      "weather": "clear",
      "temperature": 22
    },
    "biometric": {
      "heart_rate": 72,
      "fatigue_level": 0.2,
      "stress_level": 0.3
    },
    "user_query": {
      "text": "我想听点放松的音乐",
      "intent": "creative",
      "confidence": 0.9
    }
  },
  "confidence": {
    "overall": 0.85,
    "by_source": {
      "vhal": 0.95,
      "environment": 0.90,
      "biometric": 0.75
    }
  },
  "raw_signals": [],
  "_meta": {
    "output_id": "perception_1709123456789",
    "total_count": 5,
    "high_confidence_count": 3,
    "active_sources": ["vhal", "environment", "biometric"]
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `signals.vehicle` | Object | 车辆状态数据 |
| `signals.environment` | Object | 环境数据 |
| `signals.biometric` | Object | 生物特征数据 |
| `signals.user_query` | Object/null | 用户语音输入 |
| `confidence.overall` | Number (0-1) | 综合置信度 |
| `confidence.by_source` | Object | 各信号源置信度 |

---

## 调试方式

### 1. 运行验证脚本

```bash
# 完整验证
npm run test:layer1

# 仅验证信号结构
node scripts/layer1/demo.js --validate

# 测试传感器
node scripts/layer1/demo.js --sensors

# 生成 Mock 数据
node scripts/layer1/demo.js --mock
```

### 2. 单元测试

```bash
npm test -- tests/unit/layer1.test.js
```

### 3. 代码中调试

```javascript
const { perceptionLayer, SignalSources } = require('./src/layers/perception');

// 启用调试模式
const layer = new PerceptionLayer({ debug: true });

// 处理单个信号
const signal = {
  source: SignalSources.ENVIRONMENT,
  type: 'weather',
  value: { weather: 'rain' }
};
const normalized = layer.process(signal);

// 批量处理
const output = layer.processBatch([
  { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 70 } },
  { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.15 } }
]);

// 查看历史输出
console.log(layer.getHistory(5));
```

### 4. 验证输出

```javascript
const { validator } = require('./src/layers/perception/validator');

const validation = validator.validate(output);
console.log('Valid:', validation.valid);
console.log('Errors:', validation.errors);
```

---

## 功能设计

### 模块结构

```
src/layers/perception/
├── index.js        # 主入口 (PerceptionLayer 类)
├── normalizer.js   # 信号标准化逻辑
├── validator.js    # 输出验证器
├── types.js        # 常量定义
└── sensors/
    └── index.js    # 传感器适配器
```

### 核心类: PerceptionLayer

```javascript
class PerceptionLayer {
  constructor(config = {}) {
    this.signalBuffer = new Map();    // 信号缓冲区
    this.outputHistory = [];          // 输出历史
    this.debug = config.debug || false;
  }

  // 处理单个信号
  process(rawSignal) { ... }

  // 批量处理信号并生成输出
  processBatch(rawSignals) { ... }

  // 构建结构化输出
  buildOutput(normalizedSignals) { ... }

  // 验证输出
  validateOutput(output) { ... }

  // 获取活跃信号
  getActiveSignals(maxAge = 30000) { ... }
}
```

### 信号处理流程

```
原始信号 → normalizer.normalize() → 缓存 → buildOutput() → 验证 → 输出
```

---

## 需要的测试数据

### Mock 信号示例

```javascript
// 早晨通勤场景
const morningCommuteSignals = [
  { source: 'vhal', type: 'vehicle_speed', value: { speed_kmh: 70 } },
  { source: 'environment', type: 'time_of_day', value: { time_of_day: 0.15 } },
  { source: 'environment', type: 'weather', value: { weather: 'clear' } },
  { source: 'biometric', type: 'heart_rate', value: 72 }
];

// 深夜驾驶场景
const nightDriveSignals = [
  { source: 'vhal', type: 'vehicle_speed', value: { speed_kmh: 50 } },
  { source: 'environment', type: 'time_of_day', value: { time_of_day: 0.05 } },
  { source: 'biometric', type: 'heart_rate', value: 65 }
];

// 疲劳提醒场景
const fatigueAlertSignals = [
  { source: 'vhal', type: 'vehicle_speed', value: { speed_kmh: 60 } },
  { source: 'biometric', type: 'fatigue_level', value: 0.85 },
  { source: 'biometric', type: 'heart_rate', value: 58 }
];

// 用户语音请求场景
const voiceQuerySignals = [
  { source: 'vhal', type: 'vehicle_speed', value: { speed_kmh: 80 } },
  { source: 'voice', type: 'user_query', value: { text: '来点嗨歌', intent: 'creative' } }
];
```

### 现有 Mock 数据文件

- `mock_data/layer1_output_001.json` - Layer 1 输出示例
- `mock_data/layer1_output_002.json` - Layer 1 输出示例

---

## Schema 文件

JSON Schema 定义位于: `schemas/layer1_output.schema.json`

---

## 与其他层的关系

```
┌─────────────────────────────────────────────────────────┐
│                    外部信号源                             │
│  VHAL | 语音 | 生物传感器 | 环境 | 用户画像 | 音乐状态     │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 1: 物理感知层                         │
│  采集 → 标准化 → 置信度计算 → 结构化输出                  │
└────────────────────────┬────────────────────────────────┘
                         ▼
              StandardizedSignals JSON
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 2: 语义推理层                         │
└─────────────────────────────────────────────────────────┘
```

---

## 常见问题

### Q: 如何添加新的信号源?

1. 在 `types.js` 中添加新的 `SignalSources` 常量
2. 在 `SignalConfigs` 中配置信号属性
3. 在 `sensors/index.js` 中实现传感器适配器
4. 在 `index.js` 的 `structureSignals()` 中添加处理逻辑

### Q: 置信度如何计算?

置信度由以下因素决定:
- 信号源的默认置信度衰减率 (`confidenceDecay`)
- 信号的 TTL (Time To Live)
- 信号的新鲜度 (距上次更新的时间)

### Q: 如何处理低置信度信号?

当 `confidence.overall < 0.5` 时，Layer 2 会回退到安全模板，避免错误判断。

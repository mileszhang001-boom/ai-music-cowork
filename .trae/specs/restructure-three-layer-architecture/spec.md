# 三层架构团队协作体系重构 Spec

## Why
当前项目结构混合了语义处理和执行逻辑，三个团队（物理感知层、语义层、效果生成层）难以独立开发、测试和验证。需要重构项目结构，让每个团队能够：
1. 快速获取自己需要的数据
2. 独立验证自己产出的数据是否有效
3. 通过标准接口进行团队间协作

## What Changes
- **重构目录结构**：按三层架构重新组织代码
- **定义标准接口**：每层的输入/输出 JSON Schema
- **创建独立测试工具**：每层可独立验证
- **建立 Mock 数据体系**：支持团队独立开发

## Impact
- Affected specs: 整体项目架构
- Affected code: 全部 src 目录结构

---

## ADDED Requirements

### Requirement: 三层架构分离
系统 SHALL 采用三层架构设计，每层职责明确、接口标准：

```
┌─────────────────────────────────────────────────────────────┐
│                    Layer 1: 物理感知层                        │
│  职责: 采集原始数据 → 标准化信号输出                           │
│  输入: 摄像头、麦克风、VHAL、天气API、用户Query                │
│  输出: StandardizedSignals JSON                              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Layer 2: 语义推理层                        │
│  职责: 信号理解 → Scene Descriptor 生成                       │
│  输入: StandardizedSignals JSON                              │
│  输出: Scene Descriptor JSON                                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Layer 3: 效果生成层                        │
│  职责: Scene Descriptor → 真实车上效果                        │
│  输入: Scene Descriptor JSON                                 │
│  输出: 硬件控制指令 / 播放列表 / 灯光参数 / 音效参数            │
└─────────────────────────────────────────────────────────────┘
```

### Requirement: 标准化数据接口
每层 SHALL 提供标准化的输入/输出接口：

#### Layer 1 输出: StandardizedSignals Schema
```json
{
  "version": "1.0",
  "timestamp": "2026-02-28T10:00:00Z",
  "signals": {
    "vehicle": {
      "speed_kmh": 70,
      "passenger_count": 0,
      "gear": "D"
    },
    "environment": {
      "time_of_day": 0.35,
      "weather": "clear",
      "temperature": 22
    },
    "biometric": {
      "heart_rate": 72,
      "fatigue_level": 0.2,
      "stress_level": 0.3
    },
    "user_query": {
      "text": "帮我选一首适合现在的歌",
      "intent": "creative",
      "confidence": 0.95
    }
  },
  "confidence": {
    "overall": 0.85,
    "by_source": {
      "vehicle": 0.99,
      "environment": 0.95,
      "biometric": 0.80
    }
  }
}
```

#### Layer 2 输出: Scene Descriptor Schema (已有，保持兼容)
```json
{
  "version": "2.0",
  "scene_id": "scene_xxx",
  "scene_type": "morning_commute",
  "intent": { ... },
  "hints": { "music": {...}, "lighting": {...}, "audio": {...} },
  "announcement": "...",
  "meta": { ... }
}
```

#### Layer 3 输出: EffectCommands Schema
```json
{
  "version": "1.0",
  "scene_id": "scene_xxx",
  "commands": {
    "content": {
      "action": "play_playlist",
      "playlist": [...],
      "transition": { "type": "crossfade", "duration_ms": 3000 }
    },
    "lighting": {
      "action": "apply_theme",
      "theme": "calm",
      "colors": { "primary": "#1A237E", "secondary": "#4A148C" },
      "pattern": "breathing",
      "intensity": 0.3
    },
    "audio": {
      "action": "apply_preset",
      "preset": "night_mode",
      "eq": { "bass": -2, "mid": 0, "treble": 1 }
    }
  },
  "execution_report": {
    "status": "success",
    "timestamp": "2026-02-28T10:00:05Z"
  }
}
```

### Requirement: 独立验证工具
每层 SHALL 提供独立的验证工具：

#### Layer 1 验证工具
- `scripts/layer1/validate_signals.js` - 验证信号格式
- `scripts/layer1/test_sensors.js` - 测试各传感器数据采集
- `scripts/layer1/mock_data.js` - 生成 Mock 信号数据

#### Layer 2 验证工具
- `scripts/layer2/validate_descriptor.js` - 验证 Scene Descriptor 格式
- `scripts/layer2/test_scenes.js` - 测试场景识别
- `scripts/layer2/compare_models.js` - 对比不同模型输出

#### Layer 3 验证工具
- `scripts/layer3/validate_effects.js` - 验证效果指令格式
- `scripts/layer3/test_engines.js` - 测试各引擎执行
- `scripts/layer3/simulate_output.js` - 模拟硬件输出效果

### Requirement: 团队独立开发支持
每个团队 SHALL 能够独立开发和测试：

1. **Layer 1 团队**：
   - 可使用 Mock 用户 Query 测试信号采集
   - 输出可直接被 Layer 2 消费
   - 不依赖 Layer 2/3 的实现

2. **Layer 2 团队**：
   - 可使用 Layer 1 的 Mock 输出测试推理
   - 输出可直接被 Layer 3 消费
   - 不依赖 Layer 1/3 的实现

3. **Layer 3 团队**：
   - 可使用 Layer 2 的 Mock 输出测试效果生成
   - 可模拟硬件输出验证效果
   - 不依赖 Layer 1/2 的实现

---

## MODIFIED Requirements

### Requirement: 目录结构重组
项目目录 SHALL 按三层架构重新组织：

```
src/
├── layers/                      # 三层架构核心
│   ├── perception/              # Layer 1: 物理感知层
│   │   ├── index.js             # 统一入口
│   │   ├── sensors/             # 传感器适配器
│   │   │   ├── vhal.js          # 车辆信号
│   │   │   ├── environment.js   # 环境信号
│   │   │   ├── biometric.js     # 生物信号
│   │   │   ├── voice.js         # 语音信号
│   │   │   └── index.js
│   │   ├── normalizer.js        # 信号标准化
│   │   ├── validator.js         # 输出验证
│   │   └── types.js             # 类型定义
│   │
│   ├── semantic/                # Layer 2: 语义推理层
│   │   ├── index.js             # 统一入口
│   │   ├── router/              # 查询路由
│   │   │   └── index.js
│   │   ├── recognizer/          # 场景识别
│   │   │   └── index.js
│   │   ├── reasoner/            # LLM 推理
│   │   │   ├── llmClient.js
│   │   │   ├── promptBuilder.js
│   │   │   └── index.js
│   │   ├── templates/           # 模板库
│   │   │   ├── library.js
│   │   │   ├── learner.js
│   │   │   └── presets/
│   │   ├── validator.js         # 输出验证
│   │   └── types.js             # 类型定义
│   │
│   └── effects/                 # Layer 3: 效果生成层
│       ├── index.js             # 统一入口
│       ├── orchestrator/        # 编排协调
│       │   └── index.js
│       ├── engines/             # 专业引擎
│       │   ├── content/         # 内容引擎
│       │   ├── lighting/        # 灯光引擎
│       │   └── audio/           # 音效引擎
│       ├── validator.js         # 输出验证
│       └── types.js             # 类型定义
│
├── shared/                      # 共享模块
│   ├── eventBus/                # 事件总线
│   ├── feedback/                # 反馈系统
│   ├── schema/                  # JSON Schema 定义
│   │   ├── signals.schema.json
│   │   ├── descriptor.schema.json
│   │   └── effects.schema.json
│   └── utils/                   # 工具函数
│
├── config/                      # 配置
│   ├── index.js
│   └── llm.js
│
└── index.js                     # 统一入口 (兼容旧接口)
```

### Requirement: 脚本目录重组
脚本目录 SHALL 按团队分离：

```
scripts/
├── layer1/                      # 物理感知层团队
│   ├── validate_signals.js      # 验证信号格式
│   ├── test_sensors.js          # 测试传感器
│   ├── mock_data.js             # 生成 Mock 数据
│   └── demo.js                  # 演示脚本
│
├── layer2/                      # 语义推理层团队
│   ├── validate_descriptor.js   # 验证描述符
│   ├── test_scenes.js           # 测试场景识别
│   ├── compare_models.js        # 对比模型
│   ├── mock_input.js            # Mock 输入数据
│   └── demo.js                  # 演示脚本
│
├── layer3/                      # 效果生成层团队
│   ├── validate_effects.js      # 验证效果指令
│   ├── test_engines.js          # 测试引擎
│   ├── simulate_output.js       # 模拟输出
│   ├── mock_input.js            # Mock 输入数据
│   └── demo.js                  # 演示脚本
│
├── integration/                 # 集成测试
│   ├── e2e_test.js              # 端到端测试
│   └── demo_full.js             # 完整演示
│
└── tools/                       # 通用工具
    ├── schema_validator.js      # Schema 验证器
    └── data_generator.js        # 数据生成器
```

### Requirement: 测试目录重组
测试目录 SHALL 按团队分离：

```
tests/
├── layer1/                      # 物理感知层测试
│   ├── sensors.test.js
│   ├── normalizer.test.js
│   └── validator.test.js
│
├── layer2/                      # 语义推理层测试
│   ├── router.test.js
│   ├── recognizer.test.js
│   ├── reasoner.test.js
│   └── templates.test.js
│
├── layer3/                      # 效果生成层测试
│   ├── orchestrator.test.js
│   ├── content_engine.test.js
│   ├── lighting_engine.test.js
│   └── audio_engine.test.js
│
├── integration/                 # 集成测试
│   ├── layer1_to_layer2.test.js
│   ├── layer2_to_layer3.test.js
│   └── e2e.test.js
│
└── setup.js
```

---

## 数据流与团队边界

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           Team A: 物理感知层                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                   │
│  │  传感器适配  │ →  │  信号标准化  │ →  │  格式验证   │                   │
│  └─────────────┘    └─────────────┘    └─────────────┘                   │
│                                                ↓                         │
│                              StandardizedSignals JSON                     │
└──────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────┐
│                           Team B: 语义推理层                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                   │
│  │  场景识别   │ →  │  LLM推理    │ →  │  格式验证   │                   │
│  └─────────────┘    └─────────────┘    └─────────────┘                   │
│                                                ↓                         │
│                               Scene Descriptor JSON                       │
└──────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────┐
│                           Team C: 效果生成层                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                   │
│  │  编排协调   │ →  │  引擎执行   │ →  │  效果输出   │                   │
│  └─────────────┘    └─────────────┘    └─────────────┘                   │
│                                                ↓                         │
│                                EffectCommands JSON                        │
└──────────────────────────────────────────────────────────────────────────┘
```

## 验收标准

1. **Layer 1 团队** 可独立运行 `npm run test:layer1` 验证所有功能
2. **Layer 2 团队** 可独立运行 `npm run test:layer2` 验证所有功能
3. **Layer 3 团队** 可独立运行 `npm run test:layer3` 验证所有功能
4. 集成测试 `npm run test:integration` 验证三层协作
5. 所有 JSON Schema 验证通过
6. Mock 数据体系完整，支持独立开发

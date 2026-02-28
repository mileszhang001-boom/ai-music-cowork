# 车载座舱 AI 娱乐融合方案

## 项目简介

本项目旨在构建基于多模态输入的智能座舱娱乐系统，将"AI 帮我选歌"升级为"AI 为我编排一段声光电一体化的旅程体验"。通过双速架构（本地快通道+云端慢通道）和三层解耦设计，实现上车 2 秒内极速响应，并提供个性化的场景体验。

## 三层架构

```
┌─────────────────────────────────────────────────────────┐
│              Layer 1: 物理感知层                         │
│              信号采集 → 标准化 → 置信度计算               │
│              输出: StandardizedSignals JSON              │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 2: 语义推理层                         │
│              场景识别 → 模板匹配 → [LLM推理]              │
│              输出: Scene Descriptor JSON                 │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Layer 3: 效果生成层                         │
│              编排协调 → 引擎执行 → 指令下发               │
│              输出: EffectCommands JSON                   │
└─────────────────────────────────────────────────────────┘
```

## 快速开始

```bash
# 克隆仓库
git clone https://github.com/mileszhang001-boom/ai-music-cowork.git
cd ai-music-cowork

# 安装依赖
npm install

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入 API Key

# 运行测试
npm run test:layer1
npm run test:layer2
npm run test:layer3
npm run test:integration
```

## 核心文档

### 公共规范文档

| 文档 | 说明 |
|------|------|
| [spec-all.md](spec-all.md) | 技术规范文档 V2.2 |
| [plan.md](plan.md) | 研发实施计划 |
| [tasks.md](tasks.md) | 研发任务拆解 |
| [testing.md](testing.md) | 测试与验收路径 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献指南 |

### 模块文档

| 文档 | 说明 |
|------|------|
| [docs/layer1_perception.md](docs/layer1_perception.md) | Layer 1 物理感知层文档 |
| [docs/layer2_semantic.md](docs/layer2_semantic.md) | Layer 2 语义推理层文档 |
| [docs/layer3_effects.md](docs/layer3_effects.md) | Layer 3 效果生成层文档 |
| [docs/git_guide.md](docs/git_guide.md) | Git 协作指南 |

### Schema 文件

| 文件 | 说明 |
|------|------|
| [schemas/layer1_output.schema.json](schemas/layer1_output.schema.json) | Layer 1 输出 Schema |
| [schemas/scene_descriptor.schema.json](schemas/scene_descriptor.schema.json) | Scene Descriptor Schema |
| [schemas/ack.schema.json](schemas/ack.schema.json) | ACK 消息 Schema |
| [schemas/feedback_report.schema.json](schemas/feedback_report.schema.json) | Feedback Report Schema |

## 团队协作

### 团队职责

| 团队 | 负责层级 | 主要职责 |
|------|----------|----------|
| Team A | Layer 1 + Layer 2 | 信号处理、场景识别、LLM 推理 |
| Team B | Layer 3 (内容引擎) | 曲库管理、推荐算法、播放控制 |
| Team C | Layer 3 (灯光引擎) | 灯光语义映射、动效生成 |
| Team D | Layer 3 (音效引擎) | 音效预设、DSP 控制 |
| Team E | 平台/质量 | 集成测试、质量评估、性能监控 |

### 开发流程

1. 创建功能分支: `git checkout -b feature/layerX-your-feature`
2. 开发并测试: `npm run test:layerX`
3. 提交代码: `git commit -m "feat(layerX): 功能描述"`
4. 推送并创建 PR: `git push origin feature/layerX-your-feature`

详细协作规范请参考 [CONTRIBUTING.md](CONTRIBUTING.md) 和 [docs/git_guide.md](docs/git_guide.md)。

## 测试命令

```bash
# Layer 1 测试
npm run test:layer1

# Layer 2 测试
npm run test:layer2

# Layer 3 测试
npm run test:layer3

# 集成测试
npm run test:integration

# 完整演示
npm run demo:full
```

## 项目结构

```
ai-music-cowork/
├── src/
│   ├── layers/
│   │   ├── perception/     # Layer 1: 物理感知层
│   │   ├── semantic/       # Layer 2: 语义推理层
│   │   └── effects/        # Layer 3: 效果生成层
│   ├── shared/
│   │   └── eventBus/       # 事件总线
│   └── index.js            # 统一入口
├── docs/                   # 模块文档
├── schemas/                # JSON Schema
├── mock_data/              # Mock 数据
├── scripts/                # 测试脚本
├── tests/                  # 单元测试
├── spec-all.md             # 技术规范
├── plan.md                 # 研发计划
├── tasks.md                # 任务拆解
├── testing.md              # 测试验收
└── CONTRIBUTING.md         # 贡献指南
```

## GitHub 仓库

https://github.com/mileszhang001-boom/ai-music-cowork

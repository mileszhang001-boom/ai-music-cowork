## Why

当前 Layer 3 采用单一的编排协调器模式，难以满足复杂场景下的高效执行需求。主agent需要同时管理内容、灯光、音效三个子任务，缺乏任务分发、执行监控和质量检验的标准化流程。引入多Agent架构可以提升任务分析的准确性、执行的命中率和一次成功率，同时通过sub-agent的快速迭代和定期汇报机制，实现小步快跑的执行模式。

## What Changes

### 新增内容

- **主Agent (Orchestrator Agent)**:
  - 接收上游Scene Descriptor任务
  - 分析任务意图，决定是否需要调整
  - 向3个sub-agent分发任务并协调执行
  - 检验sub-agent执行结果是否达标
  - 核心指标：分析准确、命中率高、一次做对
  - 支持任务重试和失败恢复

- **Sub-Agent x3 (Content/Lighting/Audio)**:
  - Content Agent：负责歌单生成和内容推荐
  - Lighting Agent：负责氛围灯控制和动效生成
  - Audio Agent：负责音效预设和DSP控制
  - 采用小步快跑模式
  - 每分钟向主Agent汇报一次执行情况
  - 高效执行，确保任务完成

- **ReAct架构**:
  - 所有Agent采用ReAct (Reasoning + Acting) 架构
  - 支持Thought/Action/Observation循环
  - 具备反思和自检能力

### 核心指标

| 指标 | 目标值 |
|------|--------|
| 分析准确率 | ≥95% |
| 任务命中率 | ≥90% |
| 一次成功率 | ≥85% |
| 执行响应时间 | <500ms |

## Capabilities

### New Capabilities

- `layer3-main-agent`: Layer 3主Agent，负责任务分析、分发和质量检验
- `layer3-sub-agent`: Layer 3子Agent基类，包含内容/灯光/音效三个具体实现
- `react-agent-framework`: ReAct架构通用框架，支持Thought/Action/Observation循环
- `agent-communication-protocol`: Agent间通信协议，支持任务分发、状态汇报和结果检验
- `agent-metrics-collector`: Agent执行指标收集器，用于监控分析准确率、命中率和一次成功率

### Modified Capabilities

- `layer3-effects`: 扩展现有effects模块，接入多Agent协调机制
- `scene-descriptor-parser`: 增强Scene Descriptor解析能力，支持任务拆分和优先级判断

## Impact

### 代码影响

- 新增 `src/core/agents/` 目录
  - `src/core/agents/main/` - 主Agent实现
  - `src/core/agents/sub/` - 子Agent实现（content/lighting/audio）
  - `src/core/agents/core/` - ReAct框架核心
  - `src/core/agents/protocol/` - 通信协议

- 新增 `src/core/agents/metrics/` - 指标收集模块
- 修改 `src/layers/effects/` - 接入多Agent协调器
- 修改 `src/core/orchestrator/` - 与主Agent对接

### API影响

- Scene Descriptor输入格式保持不变
- Effect Commands输出格式保持不变
- 内部新增Agent通信协议（不对外暴露）

### 依赖影响

- 复用现有LLM客户端 (`src/core/llm/`)
- 复用现有事件总线 (`src/shared/eventBus/`)
- 复用现有记忆系统 (`src/core/memory/`)

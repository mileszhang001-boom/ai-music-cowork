# 贡献指南 (Contributing Guide)

## 项目概述

本项目为车载座舱 AI 娱乐融合方案，采用三层架构设计：

- **Layer 1 (物理感知层)**: 信号采集与标准化
- **Layer 2 (语义推理层)**: 场景识别与意图理解
- **Layer 3 (效果生成层)**: 效果指令生成与执行

---

## 团队职责

| 团队 | 负责层级 | 主要职责 |
|------|----------|----------|
| Team A | Layer 1 + Layer 2 | 信号处理、场景识别、LLM 推理 |
| Team B | Layer 3 (内容引擎) | 曲库管理、推荐算法、播放控制 |
| Team C | Layer 3 (灯光引擎) | 灯光语义映射、动效生成 |
| Team D | Layer 3 (音效引擎) | 音效预设、DSP 控制 |
| Team E | 平台/质量 | 集成测试、质量评估、性能监控 |

---

## 合作宪法

### 核心原则

1. **接口契约优先**: 各层之间的数据交换必须遵循约定的 JSON Schema
2. **独立开发验证**: 每层可使用 Mock 数据独立开发和测试
3. **变更通知机制**: 任何影响接口的变更必须通知相关团队
4. **文档同步更新**: 代码变更必须同步更新相关文档

### 接口契约

各层之间的数据交换格式已冻结，修改需经过团队评审：

| 接口 | Schema 文件 | 所属层 |
|------|-------------|--------|
| StandardizedSignals | `schemas/layer1_output.schema.json` | Layer 1 → Layer 2 |
| Scene Descriptor | `schemas/scene_descriptor.schema.json` | Layer 2 → Layer 3 |
| EffectCommands | (内置于代码) | Layer 3 输出 |
| Feedback Report | `schemas/feedback_report.schema.json` | Layer 3 → Layer 2 |

### 变更影响范围

当修改以下内容时，需要通知相关团队：

#### Layer 1 变更

| 修改类型 | 影响范围 | 需通知团队 |
|----------|----------|------------|
| 新增信号源 | Layer 2 需适配 | Team A, E |
| 修改输出结构 | Layer 2 解析逻辑 | Team A, E |
| 置信度计算逻辑 | 场景识别准确性 | Team A, E |

#### Layer 2 变更

| 修改类型 | 影响范围 | 需通知团队 |
|----------|----------|------------|
| Scene Descriptor 结构变化 | Layer 3 解析逻辑 | All Teams |
| 新增场景模板 | 无破坏性影响 | Team A, E |
| LLM Prompt 变化 | 输出质量 | Team A, E |

#### Layer 3 变更

| 修改类型 | 影响范围 | 需通知团队 |
|----------|----------|------------|
| 引擎接口变化 | 编排逻辑 | Team B, C, D |
| 新增引擎 | 需注册到 Orchestrator | Team B, C, D, E |
| 约束处理逻辑 | 安全底线 | All Teams |

---

## 公共文档

以下文档为团队共同知识库，所有成员应熟悉：

### 核心规范文档

| 文档 | 说明 | 维护责任 |
|------|------|----------|
| `spec-all.md` | 技术规范文档 V2.2 | 全团队 |
| `plan.md` | 研发实施计划 | Team E |
| `tasks.md` | 研发任务拆解 | Team E |
| `testing.md` | 测试与验收路径 | Team E |

### 模块文档

| 文档 | 说明 | 维护责任 |
|------|------|----------|
| `docs/layer1_perception.md` | Layer 1 模块文档 | Team A |
| `docs/layer2_semantic.md` | Layer 2 模块文档 | Team A |
| `docs/layer3_effects.md` | Layer 3 模块文档 | Team B, C, D |
| `docs/git_guide.md` | Git 协作指南 | 全团队 |

### Schema 文件

| 文件 | 说明 | 维护责任 |
|------|------|----------|
| `schemas/layer1_output.schema.json` | Layer 1 输出 Schema | Team A |
| `schemas/scene_descriptor.schema.json` | Scene Descriptor Schema | Team A |
| `schemas/ack.schema.json` | ACK 消息 Schema | Team A |
| `schemas/feedback_report.schema.json` | Feedback Report Schema | Team E |

---

## 开发流程

### 1. 功能开发

```bash
# 1. 创建功能分支
git checkout -b feature/layerX-your-feature

# 2. 开发并测试
npm run test:layerX

# 3. 提交代码
git commit -m "feat(layerX): 功能描述"

# 4. 推送并创建 PR
git push origin feature/layerX-your-feature
```

### 2. 代码审查

- PR 必须经过至少一人审查
- 审查者检查代码质量、测试覆盖、文档更新
- 审查通过后方可合并

### 3. 文档更新

代码变更后，必须同步更新：

- [ ] 模块文档 (`docs/layerX_xxx.md`)
- [ ] Schema 文件 (如涉及接口变更)
- [ ] 公共文档 (如影响其他层)

---

## 测试规范

### 各层独立测试

```bash
# Layer 1 测试
npm run test:layer1

# Layer 2 测试
npm run test:layer2

# Layer 3 测试
npm run test:layer3

# 集成测试
npm run test:integration
```

### Mock 数据使用

各层可使用 Mock 数据独立开发：

```javascript
// Layer 2 使用 Layer 1 的 Mock 输出
const mockLayer1Output = require('../mock_data/layer1_output_001.json');

// Layer 3 使用 Layer 2 的 Mock 输出
const mockDescriptor = require('../mock_data/scene_morning_commute.json');
```

---

## 质量标准

### 代码质量

- 遵循 ESLint 规范
- 函数注释完整
- 无 console.log 残留 (除调试模式)

### 测试覆盖

- 核心逻辑单元测试覆盖
- 接口变更需更新测试用例
- PR 必须通过所有测试

### 文档质量

- 文档与代码同步
- 示例代码可运行
- 结构清晰易读

---

## 沟通渠道

### 日常沟通

- GitHub Issues: 问题追踪
- GitHub PR: 代码审查
- 团队会议: 周例会

### 紧急问题

如遇以下情况，需立即通知相关团队：

1. 接口 Schema 破坏性变更
2. 核心功能 Bug
3. 安全漏洞

---

## 版本发布

### 版本号规范

遵循语义化版本 (SemVer):

- `MAJOR`: 破坏性变更
- `MINOR`: 新功能，向后兼容
- `PATCH`: Bug 修复

### 发布流程

1. 完成 Phase 阶段任务
2. 通过集成测试
3. 更新版本号和 CHANGELOG
4. 创建 Release Tag

---

## 附录

### 快速参考

```bash
# 安装依赖
npm install

# 运行测试
npm test

# 运行特定层测试
npm run test:layer1
npm run test:layer2
npm run test:layer3

# 运行集成测试
npm run test:integration

# 代码检查
npm run lint
```

### 相关链接

- [GitHub 仓库](https://github.com/mileszhang001-boom/ai-music-cowork)
- [Git 协作指南](docs/git_guide.md)
- [技术规范文档](spec-all.md)

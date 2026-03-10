# Merge Remaining Documents Spec

## Why
在之前的文档重构中，已经建立了五个核心文档体系（`constitution.md`, `spec-all.md`, `plan.md`, `tasks.md`, `testing.md`）。但目前项目根目录下仍遗留了 `tech.md`（全链路技术方案评估）和 `checklist.md`（核心检查项）。为了彻底贯彻“五个核心文档”的分类标准，保持项目结构的极简与统一，需要将这两个遗留文档的内容合并到对应的核心文档中，并删除原文件。

## What Changes
- **合并 `tech.md`**：
  - `tech.md` 包含了 SA8295P 平台基线、内存预算、各层技术选型（ExoPlayer, CarPropertyManager 等）以及核心风险评估。
  - 这些内容属于“技术实现方案、可行性调研报告和系统架构设计”，应合并至 **`plan.md`** 中。
- **合并 `checklist.md`**：
  - `checklist.md` 包含了项目各阶段的核心检查项（接口准备、响应速度指标、场景验收等）。
  - 这些内容属于“测试计划、测试用例和核心用户体验验证”，应合并至 **`testing.md`** 中（作为验收标准/Checklist 章节）。
- **清理冗余文件**：
  - 确认内容完整迁移后，删除根目录下的 `tech.md` 和 `checklist.md`。
- **更新索引**：
  - 更新 `README.md` 和 `doc_integration_guide.md` 中的相关描述，确保不再引用已删除的文件。

## Impact
- Affected specs: `plan.md`（新增技术评估与选型章节）、`testing.md`（新增核心检查项章节）、`README.md`、`doc_integration_guide.md`。
- Affected code: 删除 `tech.md` 和 `checklist.md`。

## ADDED Requirements
### Requirement: Complete Document Consolidation
系统应确保所有项目文档均归属于五个核心文档类别，不存在游离的规范或评估文档。

#### Scenario: Success case
- **WHEN** 开发者查阅技术选型或验收标准时
- **THEN** 能够直接在 `plan.md` 中找到 SA8295P 的硬件约束与技术选型，在 `testing.md` 中找到各阶段的 Checklist，而无需查找额外的零散文件。

# 数据流 LLM 调用链路验证 Spec

## Why
用户需要了解 Layer2 和 Layer3 在不同场景下的 LLM 调用行为，以及音乐推荐的数据来源和延迟情况。

## What Changes
- 创建测试脚本验证 LLM 调用链路
- 测试模板内场景和非模板场景的处理差异
- 记录各路径的延迟数据

## Impact
- Affected code: 测试脚本、数据流文档

## ADDED Requirements

### Requirement: LLM 调用链路验证
系统 SHALL 明确 Layer2 和 Layer3 的 LLM 调用时机：

#### 问题 1：Layer2 快通道触发后，Layer3 音乐推荐是否固定？
**代码分析结果：**
- Layer2 快通道只生成 `scene_descriptor`（场景描述），包含 `hints.music`（音乐风格提示）
- Layer3 ContentEngine 的歌单生成是**独立的**，有自己的三级策略：
  1. 预设音乐库 (`data/music_library.json`)
  2. LLM 生成（如果第一级无结果且 LLM 可用）
  3. Mock 数据（8 首硬编码曲目）

**结论：** 音乐不是固定的。即使 Layer2 走了快通道（模板），Layer3 仍可能调用 LLM 生成歌单——取决于 `sceneType` 是否在预设音乐库中。

#### 问题 2：Layer3 音乐推荐曲库来源？
**代码分析结果：**
- **预设音乐库**：`data/music_library.json`（本地 JSON 文件）
- **Mock 数据**：8 首硬编码曲目（仅用于兜底）
- **LLM 生成**：AI "模拟全网搜索"，返回虚构的歌曲信息（非真实音乐源）

**结论：** 当前是本地数据 + AI 虚构，**没有接入真实音乐源**（如网易云、QQ音乐 API）。

#### 问题 3：非模板场景下 Layer2 和 Layer3 是否都会调用 LLM？
**代码分析结果：**
- **Layer2 (SemanticLayer)**：如果模板不匹配且 `enableLLM=true`，会调用 LLM 生成 `scene_descriptor`
- **Layer3 (ContentEngine)**：如果 `sceneType` 不在预设音乐库且 LLM 可用，会调用 LLM 生成歌单

**结论：** 是的，**可能调用两次 LLM**，分别在 Layer2 和 Layer3。预计总延迟：6-16 秒。

### Requirement: 端到端测试验证
使用 `tests/data/scene_templates_test.json` 中 `gen_` 开头的场景进行测试验证。

#### Scenario: 测试非模板场景的 LLM 调用
- **GIVEN** 使用 `gen_028` (rural_drive) 场景作为输入
- **WHEN** 该场景不在预设模板库中
- **THEN** Layer2 应调用 LLM 生成 scene_descriptor
- **AND** Layer3 应调用 LLM 生成歌单（如果 sceneType 不在预设音乐库）
- **AND** 记录总延迟时间

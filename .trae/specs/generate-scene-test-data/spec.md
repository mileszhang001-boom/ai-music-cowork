# 场景模板测试数据生成 Spec

## Why
为语义推理层提供完整的测试数据集，包含预置模板和 LLM 生成的高频场景，便于开发调试和验证场景识别逻辑。

## What Changes
- 创建测试数据 JSON 文件，包含场景描述符完整结构
- 包含 6 个预置模板的测试数据
- 包含 50 条 LLM 生成的高频场景测试数据
- 数据覆盖各种时间、天气、乘客组合

## Impact
- Affected code: `tests/data/scene_templates_test.json` (新建)
- 用于调试工具和单元测试

## ADDED Requirements

### Requirement: 测试数据生成
系统 SHALL 提供完整的场景模板测试数据文件。

#### Scenario: 预置模板数据
- **WHEN** 加载测试数据文件
- **THEN** 包含所有 6 个预置模板的完整场景描述符

#### Scenario: 高频场景数据
- **WHEN** 加载测试数据文件
- **THEN** 包含 50 条覆盖高频场景的测试数据

### Requirement: 数据结构规范
测试数据 SHALL 遵循 Layer 2 输出的 Scene Descriptor 结构。

#### Scenario: 必填字段完整
- **WHEN** 检查每条测试数据
- **THEN** 包含 version、scene_id、scene_type、intent、hints 等必填字段

#### Scenario: 场景多样性
- **WHEN** 检查 50 条 LLM 生成数据
- **THEN** 覆盖早晨通勤、深夜驾驶、家庭出行、疲劳提醒等高频场景的多种变体

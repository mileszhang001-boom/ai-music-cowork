# Phase 2 云端模型接入 Spec

## Why
Phase 1 已完成快通道核心链路开发，现需接入阿里云百炼平台的 Qwen 系列大语言模型，实现慢通道精细化推理能力，支持复杂场景的智能推荐。

## What Changes
- **新增 Layer 3 模块**：接入云端大模型，实现 Scene Descriptor 的智能生成
- **新增 LLM 客户端**：封装阿里云百炼 API 调用，支持 qwen3.5-flash 和 qwen3.5-plus 模型
- **新增 Prompt 工程**：设计场景推理专用的 Prompt 结构
- **新增错误处理机制**：完善的异常捕获和降级策略
- **新增配置管理**：API Key、Base URL、模型参数的统一管理

## Impact
- Affected specs: `spec-all.md` 第 6.2 节（Layer 3 慢通道）
- Affected code: `src/core/layer3/`（新增）、`src/config/`（更新）、`src/core/orchestrator/`（更新）

## ADDED Requirements

### Requirement: LLM Client Integration
系统应当支持接入阿里云百炼平台的 Qwen 系列大语言模型：
- 支持 qwen3.5-flash（快速响应）和 qwen3.5-plus（高质量推理）两种模型
- 使用 OpenAI 兼容接口进行调用
- 支持流式和非流式两种输出模式

#### Scenario: Basic LLM Call
- **WHEN** 系统需要生成 Scene Descriptor
- **THEN** 调用 LLM 客户端，传入场景上下文和 Prompt
- **AND** 返回结构化的 Scene Descriptor JSON

### Requirement: Prompt Engineering
系统应当设计专用的 Prompt 结构用于场景推理：
- System Prompt：定义 AI 助手的角色和能力边界
- User Prompt：包含信号上下文、用户偏好、历史记忆等信息
- Output Format：要求输出符合 Scene Descriptor V2.0 Schema 的 JSON

#### Scenario: Prompt Construction
- **WHEN** 构建场景推理请求
- **THEN** 系统自动组装 System Prompt 和 User Prompt
- **AND** 包含必要的上下文信息（时间、天气、乘客、用户偏好等）

### Requirement: Error Handling and Fallback
系统应当实现完善的错误处理机制：
- 网络超时自动重试（最多 3 次）
- API 限流时降级到快通道模板
- 模型响应格式错误时进行修复尝试
- 记录所有错误日志用于问题排查

#### Scenario: API Error Handling
- **WHEN** LLM API 调用失败（超时、限流、错误响应）
- **THEN** 系统自动重试或降级到快通道
- **AND** 记录错误信息用于后续分析

### Requirement: Model Comparison Testing
系统应当支持两种模型的对比测试：
- qwen3.5-flash：适用于简单场景，响应速度快
- qwen3.5-plus：适用于复杂场景，推理质量高
- 支持配置切换和 A/B 测试

#### Scenario: Model Selection
- **WHEN** 配置指定使用特定模型
- **THEN** 系统使用对应的模型进行推理
- **AND** 记录响应时间和质量指标

### Requirement: Configuration Management
系统应当统一管理 LLM 相关配置：
- API Key：支持环境变量和配置文件两种方式
- Base URL：支持多地域配置（北京、新加坡、弗吉尼亚）
- 模型参数：temperature、max_tokens、top_p 等
- 超时配置：连接超时、读取超时

## MODIFIED Requirements

### Requirement: Orchestrator Integration
编排协调器需要支持快慢双通道融合：
- 快通道先返回模板结果
- 慢通道完成后进行增量更新
- 平滑替换，避免用户感知突变

## REMOVED Requirements
无移除项。

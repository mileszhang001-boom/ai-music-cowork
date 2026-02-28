## ADDED Requirements

### Requirement: ReAct Loop Execution
所有Agent SHALL 采用ReAct（Reasoning + Acting）架构执行任务，通过Thought/Action/Observation循环迭代。

#### Scenario: Single iteration
- **WHEN** Agent执行单次ReAct迭代
- **THEN** Agent SHALL 生成Thought（推理），执行Action（行动），获取Observation（观察结果）

#### Scenario: Multiple iterations
- **WHEN** 需要多次迭代才能完成任务
- **THEN** Agent SHALL 循环执行ReAct步骤，直到满足结束条件

### Requirement: ReAct Max Iterations
ReAct循环 SHALL 设置最大迭代次数，防止无限循环。

#### Scenario: Max iterations reached
- **WHEN** 迭代次数达到最大限制（默认3次）
- **THEN** Agent SHALL 停止迭代，返回当前最佳结果

### Requirement: ReAct Reflection
Agent SHALL 具备反思能力，能够基于历史推理链进行自我检查和修正。

#### Scenario: Self-correction
- **WHEN** Agent发现当前推理与预期不符
- **THEN** Agent SHALL 回溯上一步推理，尝试其他路径

### Requirement: ReAct Action Types
Agent SHALL 支持多种Action类型，包括LLM调用、规则引擎查询、工具执行和信息检索。

#### Scenario: LLM action
- **WHEN** Agent需要LLM进行推理
- **THEN** Agent SHALL 调用LLM客户端，获取推理结果

#### Scenario: Rule engine action
- **WHEN** Agent需要使用规则引擎验证
- **THEN** Agent SHALL 调用规则引擎，获取验证结果

#### Scenario: Tool execution
- **WHEN** Agent需要执行具体操作
- **THEN** Agent SHALL 调用相应工具（如内容引擎、灯光引擎）

### Requirement: ReAct Observation Processing
Agent SHALL 能够处理不同类型的Observation，并将其纳入推理过程。

#### Scenario: Successful observation
- **WHEN** Action返回成功结果
- **THEN** Agent SHALL 将结果加入推理上下文，继续下一步

#### Scenario: Failed observation
- **WHEN** Action返回失败结果
- **THEN** Agent SHALL 分析失败原因，调整策略后重试

## ADDED Requirements

### Requirement: Main Agent Task Analysis
主Agent SHALL 能够接收上游Scene Descriptor并进行深度分析，提取任务意图、约束条件和执行参数。

#### Scenario: Analyze valid scene descriptor
- **WHEN** 主Agent接收到有效的Scene Descriptor
- **THEN** 主Agent SHALL 解析出intent、hints、constraints字段，并生成任务分解方案

#### Scenario: Handle malformed input
- **WHEN** 主Agent接收到格式不正确的输入
- **THEN** 主Agent SHALL 返回错误码ERR_INVALID_INPUT，并记录错误日志

### Requirement: Main Agent Task Decision
主Agent SHALL 能够基于分析结果决定是否需要调整任务，包括任务优先级排序、依赖关系处理和执行策略选择。

#### Scenario: No modification needed
- **WHEN** 任务分析结果符合预期
- **THEN** 主Agent SHALL 直接分发任务，不做修改

#### Scenario: Task modification required
- **WHEN** 任务需要调整（如约束冲突、依赖缺失）
- **THEN** 主Agent SHALL 修改任务参数后分发，并记录修改原因

### Requirement: Main Agent Task Distribution
主Agent SHALL 能够将任务分发到3个Sub-Agent（Content/Lighting/Audio），并维护任务状态追踪。

#### Scenario: Distribute to all sub-agents
- **WHEN** 主Agent完成任务分析
- **THEN** 主Agent SHALL 向3个Sub-Agent分发对应子任务，并返回task_id列表

#### Scenario: Partial distribution
- **WHEN** 部分子Agent不可用
- **THEN** 主Agent SHALL 仅分发可用Agent的任务，并标记不可用Agent

### Requirement: Main Agent Result Verification
主Agent SHALL 能够检验Sub-Agent的执行结果是否符合预期，包括结果完整性、参数合法性和效果达标性。

#### Scenario: Verification passed
- **WHEN** Sub-Agent返回结果通过检验
- **THEN** 主Agent SHALL 标记该任务为完成，并汇总结果

#### Scenario: Verification failed
- **WHEN** Sub-Agent返回结果未通过检验
- **THEN** 主Agent SHALL 触发重试机制（最多3次），若仍失败则记录失败原因

### Requirement: Main Agent Core Metrics
主Agent SHALL 追踪并报告核心指标：分析准确率≥95%、任务命中率≥90%、一次成功率≥85%。

#### Scenario: Metrics collection
- **WHEN** 任务完成（成功或失败）
- **THEN** 主Agent SHALL 记录分析结果、命中状态和执行次数，用于指标计算

#### Scenario: Metrics reporting
- **WHEN** 请求获取核心指标
- **THEN** 主Agent SHALL 返回分析准确率、命中率和一次成功率

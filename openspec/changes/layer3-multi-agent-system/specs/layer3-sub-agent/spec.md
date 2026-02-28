## ADDED Requirements

### Requirement: Sub-Agent Task Execution
Sub-Agent SHALL 能够接收主Agent分发的子任务，并执行相应的娱乐效果（内容/灯光/音效）。

#### Scenario: Content Agent executes
- **WHEN** Content Agent接收到播放任务
- **THEN** Agent SHALL 生成歌单或推荐内容，并返回执行结果

#### Scenario: Lighting Agent executes
- **WHEN** Lighting Agent接收到氛围灯任务
- **THEN** Agent SHALL 生成灯光控制指令，并返回执行结果

#### Scenario: Audio Agent executes
- **WHEN** Audio Agent接收到音效任务
- **THEN** Agent SHALL 生成音效预设参数，并返回执行结果

### Requirement: Sub-Agent Reporting Mechanism
Sub-Agent SHALL 每分钟向主Agent汇报一次执行情况，包括任务进度、当前状态和遇到的问题。

#### Scenario: Periodic reporting
- **WHEN** 距离上次汇报已过60秒
- **THEN** Sub-Agent SHALL 向主Agent发送进度报告

#### Scenario: Event-driven reporting
- **WHEN** 任务完成或遇到错误
- **THEN** SubAgent SHALL 立即向主Agent发送状态更新

### Requirement: Sub-Agent Small-Step Execution
Sub-Agent SHALL 采用小步快跑模式执行任务，将大任务拆分为多个小步骤，每步完成后立即报告。

#### Scenario: Step completion
- **WHEN** Sub-Agent完成一个执行步骤
- **THEN** Agent SHALL 记录步骤结果，并准备下一步执行

#### Scenario: All steps completed
- **WHEN** Sub-Agent完成所有步骤
- **THEN** Agent SHALL 发送最终结果给主Agent

### Requirement: Sub-Agent Error Handling
Sub-Agent SHALL 能够处理执行过程中的错误，并进行重试或降级。

#### Scenario: Retryable error
- **WHEN** Sub-Agent遇到可重试错误（如临时资源不可用）
- **THEN** Agent SHALL 重试执行（最多3次），若仍失败则报告错误

#### Scenario: Non-retryable error
- **WHEN** Sub-Agent遇到不可恢复错误（如参数非法）
- **THEN** Agent SHALL 立即报告错误给主Agent，不再重试

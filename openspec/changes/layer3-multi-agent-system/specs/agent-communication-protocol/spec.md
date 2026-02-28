## ADDED Requirements

### Requirement: Task Distribution Protocol
主Agent SHALL 使用标准化协议向Sub-Agent分发任务。

#### Scenario: Distribute task to sub-agent
- **WHEN** 主Agent需要分发任务
- **THEN** 主Agent SHALL 发送包含task_id、任务类型、任务参数的消息

#### Scenario: Receive task
- **WHEN** Sub-Agent接收到任务消息
- **THEN** Sub-Agent SHALL 返回ACK确认，并开始执行

### Requirement: Status Reporting Protocol
Sub-Agent SHALL 使用标准化协议向主Agent汇报状态。

#### Scenario: Periodic status report
- **WHEN** Sub-Agent需要汇报状态
- **THEN** Sub-Agent SHALL 发送包含task_id、进度、状态的消息

#### Scenario: Status message format
- **WHEN** 主Agent接收到状态报告
- **THEN** 主Agent SHALL 解析消息格式，更新任务状态

### Requirement: Result Submission Protocol
Sub-Agent SHALL 使用标准化协议提交执行结果。

#### Scenario: Submit result
- **WHEN** Sub-Agent完成任务执行
- **THEN** Sub-Agent SHALL 发送包含task_id、结果数据、执行时间的消息

#### Scenario: Receive result
- **WHEN** 主Agent接收到结果
- **THEN** 主Agent SHALL 验证结果格式，进行结果检验

### Requirement: Error Reporting Protocol
Agent间 SHALL 使用标准化协议报告错误。

#### Scenario: Report error
- **WHEN** Agent遇到错误
- **THEN** Agent SHALL 发送包含error_code、error_message、context的消息

#### Scenario: Error response
- **WHEN** 主Agent接收到错误报告
- **THEN** 主Agent SHALL 记录错误，决定是否重试或降级

### Requirement: Heartbeat Protocol
Sub-Agent SHALL 定期发送心跳消息，证明自身可用性。

#### Scenario: Send heartbeat
- **WHEN** 距离上次心跳已过30秒
- **THEN** Sub-Agent SHALL 发送心跳消息，包含Agent ID和状态

#### Scenario: Missing heartbeat
- **WHEN** 主Agent超过60秒未收到某Sub-Agent心跳
- **THEN** 主Agent SHALL 标记该Agent为不可用，重新分配任务

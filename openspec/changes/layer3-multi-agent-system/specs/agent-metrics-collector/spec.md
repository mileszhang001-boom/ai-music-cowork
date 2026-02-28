## ADDED Requirements

### Requirement: Analysis Accuracy Metric
系统 SHALL 追踪主Agent的任务分析准确率，目标值≥95%。

#### Scenario: Track analysis accuracy
- **WHEN** 任务完成并获得最终验证结果
- **THEN** 系统 SHALL 对比分析结果与最终结果，计算准确率

#### Scenario: Report accuracy
- **WHEN** 请求获取分析准确率
- **THEN** 系统 SHALL 返回分析准确率（成功分析数/总任务数）

### Requirement: Task Hit Rate Metric
系统 SHALL 追踪任务命中率，目标值≥90%。

#### Scenario: Track hit rate
- **WHEN** 任务执行完成
- **THEN** 系统 SHALL 判断任务是否命中预期目标，记录命中状态

#### Scenario: Report hit rate
- **WHEN** 请求获取任务命中率
- **THEN** 系统 SHALL 返回命中率（命中数/总任务数）

### Requirement: First-Time Success Rate Metric
系统 SHALL 追踪一次成功率，目标值≥85%。

#### Scenario: Track first-time success
- **WHEN** 任务执行完成
- **THEN** 系统 SHALL 记录执行次数，判断是否一次成功

#### Scenario: Report success rate
- **WHEN** 请求获取一次成功率
- **THEN** 系统 SHALL 返回一次成功率（一次成功数/总任务数）

### Requirement: Response Time Metric
系统 SHALL 追踪Agent响应时间。

#### Scenario: Track response time
- **WHEN** 任务开始处理
- **THEN** 系统 SHALL 记录开始时间，计算响应时间

#### Scenario: Report response time
- **WHEN** 请求获取响应时间统计
- **THEN** 系统 SHALL 返回平均响应时间、最大响应时间、最小响应时间

### Requirement: Metrics Aggregation
系统 SHALL 支持按时间窗口聚合指标。

#### Scenario: Time window aggregation
- **WHEN** 请求指定时间窗口的指标
- **THEN** 系统 SHALL 仅统计该时间窗口内的任务数据

#### Scenario: Overall aggregation
- **WHEN** 请求总体指标
- **THEN** 系统 SHALL 统计所有历史任务的指标数据

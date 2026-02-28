## 1. 基础设施搭建

- [x] 1.1 创建 `src/core/agents/` 目录结构
- [x] 1.2 创建 `src/core/agents/core/` ReAct框架核心目录
- [x] 1.3 创建 `src/core/agents/main/` 主Agent目录
- [x] 1.4 创建 `src/core/agents/sub/` 子Agent目录
- [x] 1.5 创建 `src/core/agents/protocol/` 通信协议目录
- [x] 1.6 创建 `src/core/agents/metrics/` 指标收集器目录
- [x] 1.7 配置 package.json 依赖（若需要新增）

## 2. ReAct框架核心实现

- [x] 2.1 实现 ReActAgent 基类 (src/core/agents/core/ReActAgent.js)
- [x] 2.2 实现 Thought/Action/Observation 数据结构
- [x] 2.3 实现 ReAct 循环执行器
- [x] 2.4 实现最大迭代次数控制
- [x] 2.5 实现反思和自检机制
- [x] 2.6 实现 Action 类型路由器（LLM/Rule/Tool）
- [ ] 2.7 单元测试：ReActAgent 基类

## 3. Agent通信协议实现

- [x] 3.1 定义消息格式标准 (src/core/agents/protocol/messages.js)
- [x] 3.2 实现任务分发协议
- [x] 3.3 实现状态汇报协议
- [x] 3.4 实现结果提交协议
- [x] 3.5 实现错误报告协议
- [x] 3.6 实现心跳协议
- [ ] 3.7 单元测试：通信协议

## 4. 指标收集器实现

- [x] 4.1 实现分析准确率追踪 (src/core/agents/metrics/collector.js)
- [x] 4.2 实现任务命中率追踪
- [x] 4.3 实现一次成功率追踪
- [x] 4.4 实现响应时间统计
- [x] 4.5 实现时间窗口聚合
- [ ] 4.6 单元测试：指标收集器

## 5. 主Agent实现

- [x] 5.1 实现 MainAgent 类 (src/core/agents/main/MainAgent.js)
- [x] 5.2 实现任务分析模块
- [x] 5.3 实现任务决策模块（是否修改任务）
- [x] 5.4 实现任务分发模块
- [x] 5.5 实现结果检验模块
- [x] 5.6 实现核心指标报告接口
- [x] 5.7 集成 LLM 客户端
- [ ] 5.8 单元测试：MainAgent

## 6. Sub-Agent基类实现

- [x] 6.1 实现 SubAgent 基类 (src/core/agents/sub/SubAgent.js)
- [x] 6.2 实现任务执行接口
- [x] 6.3 实现汇报机制（定时+事件触发）
- [x] 6.4 实现小步快跑执行模式
- [x] 6.5 实现错误处理和重试
- [ ] 6.6 单元测试：SubAgent 基类

## 7. Content Agent实现

- [x] 7.1 实现 ContentAgent 类 (src/core/agents/sub/ContentAgent.js)
- [x] 7.2 继承 SubAgent 基类
- [x] 7.3 实现歌单生成逻辑
- [x] 7.4 实现内容推荐逻辑
- [x] 7.5 对接现有内容引擎 (src/engines/content/)
- [ ] 7.6 单元测试：ContentAgent

## 8. Lighting Agent实现

- [x] 8.1 实现 LightingAgent 类 (src/core/agents/sub/LightingAgent.js)
- [x] 8.2 继承 SubAgent 基类
- [x] 8.3 实现灯光控制指令生成
- [x] 8.4 实现动效生成逻辑
- [x] 8.5 对接现有灯光引擎 (src/engines/lighting/)
- [ ] 8.6 单元测试：LightingAgent

## 9. Audio Agent实现

- [x] 9.1 实现 AudioAgent 类 (src/core/agents/sub/AudioAgent.js)
- [x] 9.2 继承 SubAgent 基类
- [x] 9.3 实现音效预设参数生成
- [x] 9.4 实现 DSP 控制逻辑
- [x] 9.5 对接现有音效引擎 (src/engines/audio/)
- [ ] 9.6 单元测试：AudioAgent

## 10. Layer 3 集成

- [x] 10.1 创建 MultiAgentOrchestrator (src/core/agents/MultiAgentOrchestrator.js)
- [x] 10.2 修改现有 effects/index.js 接入多Agent系统
- [x] 10.3 集成事件总线通信
- [x] 10.4 集成记忆系统
- [x] 10.5 集成规则引擎（兜底）
- [ ] 10.6 集成测试：主流程

## 11. 端到端测试

- [ ] 11.1 主Agent与3个Sub-Agent联调测试
- [ ] 11.2 异常场景测试（Agent失败、超时）
- [ ] 11.3 性能测试（响应时间）
- [ ] 11.4 指标收集验证
- [ ] 11.5 与上游 Layer 2 集成测试
- [ ] 11.6 完整端到端测试

## 12. 调试工具

- [ ] 12.1 扩展 debug.js 支持多Agent调试
- [ ] 12.2 添加 Agent 状态查看功能
- [ ] 12.3 添加消息追踪功能
- [ ] 12.4 添加指标仪表盘

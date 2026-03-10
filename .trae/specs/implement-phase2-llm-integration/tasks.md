# Tasks

- [x] Task 1: 创建 LLM 客户端基础模块
  - [x] SubTask 1.1: 创建 `src/core/llm/` 目录结构
  - [x] SubTask 1.2: 实现 `llmClient.js` - 封装 OpenAI 兼容接口调用
  - [x] SubTask 1.3: 实现配置管理（API Key、Base URL、模型选择）
  - [x] SubTask 1.4: 实现请求参数配置（temperature、max_tokens 等）

- [x] Task 2: 实现错误处理和重试机制
  - [x] SubTask 2.1: 实现网络超时自动重试（最多 3 次）
  - [x] SubTask 2.2: 实现 API 限流检测和降级策略
  - [x] SubTask 2.3: 实现响应格式错误修复尝试
  - [x] SubTask 2.4: 实现错误日志记录

- [x] Task 3: 设计和实现 Prompt 工程
  - [x] SubTask 3.1: 设计 System Prompt 模板
  - [x] SubTask 3.2: 设计 User Prompt 模板（包含信号上下文）
  - [x] SubTask 3.3: 实现 Prompt 构建器 `promptBuilder.js`
  - [x] SubTask 3.4: 实现 JSON 输出格式约束

- [x] Task 4: 实现 Layer 3 慢通道模块
  - [x] SubTask 4.1: 创建 `src/core/layer3/index.js`
  - [x] SubTask 4.2: 实现场景推理入口函数
  - [x] SubTask 4.3: 实现响应解析和 Scene Descriptor 生成
  - [x] SubTask 4.4: 实现与 Layer 1/2 的数据对接

- [x] Task 5: 实现快慢双通道融合
  - [x] SubTask 5.1: 更新 Orchestrator 支持双通道调度
  - [x] SubTask 5.2: 实现增量更新（Diff）逻辑
  - [x] SubTask 5.3: 实现平滑替换策略
  - [x] SubTask 5.4: 实现模板学习触发（调用 TemplateLearner）

- [x] Task 6: 编写测试用例
  - [x] SubTask 6.1: 编写 LLM 客户端单元测试
  - [x] SubTask 6.2: 编写 Prompt 构建器单元测试
  - [x] SubTask 6.3: 编写 Layer 3 集成测试
  - [x] SubTask 6.4: 编写模型对比测试脚本

- [x] Task 7: 编写文档和演示
  - [x] SubTask 7.1: 编写 LLM 接口使用文档
  - [x] SubTask 7.2: 更新演示脚本支持慢通道展示
  - [x] SubTask 7.3: 编写模型性能对比报告

# Task Dependencies
- Task 2 depends on Task 1（需要先有客户端才能处理错误）
- Task 3 depends on Task 1（需要客户端支持）
- Task 4 depends on Task 1, Task 2, Task 3（需要完整的 LLM 能力）
- Task 5 depends on Task 4（需要 Layer 3 完成）
- Task 6 depends on Task 1, Task 2, Task 3, Task 4, Task 5
- Task 7 depends on Task 6（测试通过后编写文档）

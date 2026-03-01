# Tasks

- [x] Task 1: 创建 LLM 调用链路测试脚本
  - [x] 1.1 创建 `scripts/test_llm_call_chain.js`
  - [x] 1.2 读取 `gen_` 开头的测试场景数据
  - [x] 1.3 测试模板内场景的处理流程
  - [x] 1.4 测试非模板场景的处理流程
  - [x] 1.5 记录各路径的延迟数据

- [x] Task 2: 验证 Layer2 LLM 调用时机
  - [x] 2.1 确认模板匹配成功时不调用 LLM
  - [x] 2.2 确认模板匹配失败时调用 LLM（如果启用）
  - [x] 2.3 记录 Layer2 LLM 调用延迟

- [x] Task 3: 验证 Layer3 ContentEngine 歌单生成策略
  - [x] 3.1 确认预设音乐库场景使用本地数据
  - [x] 3.2 确认非预设场景调用 LLM 生成
  - [x] 3.3 确认 LLM 不可用时使用 Mock 数据
  - [x] 3.4 记录 Layer3 LLM 调用延迟

- [x] Task 4: 输出测试报告
  - [x] 4.1 汇总各场景的处理路径
  - [x] 4.2 汇总各路径的延迟数据
  - [x] 4.3 回答用户的三个问题

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 2, Task 3]

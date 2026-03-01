# Tasks

- [x] Task 1: 设计标签体系和提示词模板
  - [x] SubTask 1.1: 整理主流音乐平台的标签分类标准
  - [x] SubTask 1.2: 设计 LLM 提示词模板
  - [x] SubTask 1.3: 定义标签字段的数据结构和约束

- [x] Task 2: 增强音乐扫描工具
  - [x] SubTask 2.1: 扫描所有音乐文件，提取基础元数据
  - [x] SubTask 2.2: 提取音频文件的技术信息（时长、比特率、采样率）
  - [x] SubTask 2.3: 生成基础索引文件（未分析状态）

- [x] Task 3: 实现 LLM 批量分析功能
  - [x] SubTask 3.1: 创建 Qwen API 调用模块
  - [x] SubTask 3.2: 实现批量处理逻辑（支持并发）
  - [x] SubTask 3.3: 添加进度跟踪和断点续传
  - [x] SubTask 3.4: 实现错误处理和重试机制

- [x] Task 4: 生成完整索引文件
  - [x] SubTask 4.1: 合并基础元数据和 LLM 分析结果
  - [x] SubTask 4.2: 验证标签完整性和有效性
  - [x] SubTask 4.3: 生成最终的 index.json

- [x] Task 5: 测试和验证
  - [x] SubTask 5.1: 测试小批量歌曲分析（10首）
  - [x] SubTask 5.2: 验证标签质量
  - [x] SubTask 5.3: 测试完整流程（635首）
  - [x] SubTask 5.4: 验证索引文件在 Layer3 中的使用

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 2, Task 3]
- [Task 5] depends on [Task 4]

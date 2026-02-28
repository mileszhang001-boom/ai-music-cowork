# Tasks

- [x] Task 1: 扩展预置模板库至 50 个场景
  - [x] SubTask 1.1: 设计 50 个场景的分类体系（时间/天气/社交/状态/特殊）
  - [x] SubTask 1.2: 编写 30 个新增场景模板的 JSON 定义
  - [x] SubTask 1.3: 更新 `templates/preset_templates.json` 文件

- [x] Task 2: 实现模板学习机制
  - [x] SubTask 2.1: 创建 `src/core/templateLibrary/templateLearner.js` 模块
  - [x] SubTask 2.2: 实现从 Scene Descriptor 提取模板特征的函数
  - [x] SubTask 2.3: 实现用户反馈检测逻辑（负面操作检测）
  - [x] SubTask 2.4: 实现模板优先级动态调整（常用模板优先）

- [x] Task 3: 实现模板持久化
  - [x] SubTask 3.1: 创建 `templates/preset_templates.json`（预置模板）
  - [x] SubTask 3.2: 创建 `templates/learned_templates.json`（学习模板）
  - [x] SubTask 3.3: 创建 `templates/custom_templates.json`（用户自定义模板）
  - [x] SubTask 3.4: 更新 TemplateLibrary 支持多源模板加载

- [x] Task 4: 完善 TTS 引导词机制
  - [x] SubTask 4.1: 在模板中添加 `announcement_templates` 字段（多条播报文案）
  - [x] SubTask 4.2: 实现 ACK 与 Announcement 的协调播报逻辑
  - [x] SubTask 4.3: 更新 `src/core/queryRouter/index.js` 支持 ACK 队列管理

- [x] Task 5: 编写机制说明文档
  - [x] SubTask 5.1: 创建 `docs/template_matching_mechanism.md` 文档
  - [x] SubTask 5.2: 说明模板匹配算法（触发条件、维度相似度、优先级）
  - [x] SubTask 5.3: 说明 TTS 引导词生成规则
  - [x] SubTask 5.4: 添加使用示例和最佳实践

- [x] Task 6: 更新单元测试和集成测试
  - [x] SubTask 6.1: 添加模板学习机制的单元测试
  - [x] SubTask 6.2: 添加多源模板加载的测试
  - [x] SubTask 6.3: 更新演示脚本支持新功能展示

# Task Dependencies
- Task 2 depends on Task 1（需要先有完整的模板库才能测试学习机制）
- Task 4 depends on Task 1（需要模板支持 announcement_templates）
- Task 6 depends on Task 1, 2, 3, 4, 5

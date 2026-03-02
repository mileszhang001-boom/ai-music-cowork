# Tasks

- [x] Task 1: 分析当前 Layer3 匹配问题并生成评测报告
  - [x] SubTask 1.1: 运行场景匹配评测，输出各场景得分
  - [x] SubTask 1.2: 识别低分场景的问题根因
  - [x] SubTask 1.3: 生成优化建议

- [x] Task 2: 优化 ContentEngine 编排逻辑
  - [x] SubTask 2.1: 实现动态中英文比例（基于场景类型）
  - [x] SubTask 2.2: 实现场景类型过滤（排除不合适内容）
  - [x] SubTask 2.3: 优化匹配权重（提升 valence/energy 匹配精度）

- [x] Task 3: 优化歌曲特征数据
  - [x] SubTask 3.1: 为中文歌曲添加 scene_tags
  - [x] SubTask 3.2: 优化高能量场景的歌曲 energy 值
  - [x] SubTask 3.3: 优化低能量场景的歌曲 energy 值

- [x] Task 4: 优化场景模板
  - [x] SubTask 4.1: 为疲劳提醒场景添加排除规则
  - [x] SubTask 4.2: 为儿童模式添加内容过滤规则
  - [x] SubTask 4.3: 调整各场景的中英文比例偏好

- [x] Task 5: 验证优化效果
  - [x] SubTask 5.1: 重新运行场景匹配评测
  - [x] SubTask 5.2: 确保所有预置场景评分 ≥ 80
  - [x] SubTask 5.3: 确保中文占比在 40-70% 范围内

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 2, Task 3, Task 4]

# Tasks

- [x] Task 1: 分析远程分支的参数命名和标签系统
  - [x] SubTask 1.1: 读取远程分支的 data/index.json，提取所有字段定义
  - [x] SubTask 1.2: 对比当前代码中的参数名，列出需要修改的参数
  - [x] SubTask 1.3: 整理 mood_tags 和 scene_tags 的完整标签列表

- [x] Task 2: 实现多维度音乐特征评分系统
  - [x] SubTask 2.1: 实现 _calculateEnergyScore 方法（能量匹配评分）
  - [x] SubTask 2.2: 实现 _calculateTempoScore 方法（节奏匹配评分）
  - [x] SubTask 2.3: 实现 _calculateValenceScore 方法（情绪匹配评分）
  - [x] SubTask 2.4: 实现 _calculateTagScore 方法（标签匹配评分）
  - [x] SubTask 2.5: 实现 _calculateOverallScore 方法（综合评分）

- [x] Task 3: 优化推荐策略逻辑
  - [x] SubTask 3.1: 优化 queryLocalMusic 方法，使用评分系统替代简单过滤
  - [x] SubTask 3.2: 更新 curatePlaylist 方法，集成评分排序
  - [x] SubTask 3.3: 添加约束条件处理（max_tracks, min_energy, max_duration）

- [x] Task 4: 更新参数命名和标签系统
  - [x] SubTask 4.1: 添加 valence 参数支持（如果当前没有）
  - [x] 4.2: 统一 mood_tags 和 scene_tags 的解析逻辑
  - [x] SubTask 4.3: 更新 FALLBACK_PLAYLIST，添加 valence 和标签

- [x] Task 5: 测试和验证
  - [x] SubTask 5.1: 创建测试脚本验证评分算法
  - [x] SubTask 5.2: 测试模板场景推荐效果
  - [x] SubTask 5.3: 测试非模板场景推荐效果
  - [x] SubTask 5.4: 对比优化前后的推荐质量

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 3, Task 4]

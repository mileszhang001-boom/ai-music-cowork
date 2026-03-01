# Tasks

- [x] Task 1: 设计并实现歌曲标签数据结构
  - [x] 定义 Spotify 风格音频特征字段（valence, danceability, acousticness, instrumentalness）
  - [x] 定义场景匹配标签字段（mood_tags, activity_tags, weather_tags, time_tags, scene_tags）
  - [x] 创建标签生成工具脚本

- [x] Task 2: 为曲库歌曲补充标签数据
  - [x] 编写标签自动生成脚本，从现有字段推导新标签
  - [x] 为 1000 首歌曲生成完整标签
  - [x] 验证标签数据格式正确

- [x] Task 3: 优化非模板场景标签匹配推荐
  - [x] 新增 `_matchByTags` 方法
  - [x] 更新 `_searchLibraryByHints` 方法，集成标签匹配
  - [x] 添加标签权重配置

- [x] Task 4: 验证优化效果
  - [x] 测试非模板场景推荐准确性
  - [x] 对比优化前后的推荐结果
  - [x] 确认标签匹配逻辑正确

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 3

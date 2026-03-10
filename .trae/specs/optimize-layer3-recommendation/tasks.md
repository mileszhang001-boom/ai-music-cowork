# Tasks

- [x] Task 1: 新增 `_searchLibraryByHints` 方法，实现曲库智能检索
  - [x] 遍历所有场景的歌曲
  - [x] 根据 genres 筛选匹配歌曲
  - [x] 根据 tempo/bpm 筛选
  - [x] 根据 energy_level 筛选
  - [x] 根据 language 筛选（中英文、纯音乐）
  - [x] 计算匹配度并排序

- [x] Task 2: 优化 `curatePlaylist` 方法的检索策略
  - [x] 模板场景直接从曲库获取，跳过 LLM
  - [x] 非模板场景先调用 `_searchLibraryByHints`
  - [x] 曲库匹配不足时才调用 LLM
  - [x] 添加 `_buildResult` 辅助方法

- [x] Task 3: 验证优化效果
  - [x] 测试模板场景（morning_commute）- 应直接返回曲库歌曲
  - [x] 测试非模板场景 - 应先检索曲库再调用 LLM
  - [x] 确认中英文和纯音乐都能正确匹配

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2

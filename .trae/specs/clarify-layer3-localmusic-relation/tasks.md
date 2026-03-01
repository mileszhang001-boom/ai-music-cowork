# Tasks

- [x] Task 1: 重构 ContentEngine 推荐策略为三层
  - [x] 1.1 修改 `curatePlaylist()` 方法实现三层策略
  - [x] 1.2 模板场景：SQL 直接匹配 (source: 'sql')
  - [x] 1.3 非模板场景：LLM 智能选取 (source: 'llm')
  - [x] 1.4 兜底场景：预设固定清单 (source: 'fallback')
  - [x] 1.5 定义 FALLBACK_PLAYLIST 常量

- [x] Task 2: 定义 Layer3 与本地音乐模块接口
  - [x] 2.1 定义 `LocalMusicModule` 接口 (TypeScript 类型)
  - [x] 2.2 定义 `ContentEngine` 接口 (TypeScript 类型)
  - [x] 2.3 修改 ContentEngine 构造函数接收 LocalMusicModule

- [x] Task 3: 更新 Android 本地音乐模块
  - [x] 3.1 确保 LocalMusicRepository 提供查询接口
  - [x] 3.2 确保 MusicPlayer 提供播放接口
  - [x] 3.3 添加接口文档注释

- [x] Task 4: 更新文档
  - [x] 4.1 更新数据流说明文档
  - [x] 4.2 更新本地音乐资源部署指南

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]
- [Task 4] depends on [Task 1, Task 3]

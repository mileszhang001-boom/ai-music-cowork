# Tasks

## Phase 1: PC 端工具开发

- [x] Task 1: 创建音乐元数据提取工具
  - [x] 1.1 创建 `tools/music-organizer/` 目录结构
  - [x] 1.2 实现 `organize_music.py` 主脚本
  - [x] 1.3 实现音频元数据提取 (支持 MP3/FLAC/WAV/M4A)
  - [x] 1.4 实现 SQLite 数据库生成 (含 FTS5)
  - [x] 1.5 实现拼音转换 (用于中文检索)
  - [x] 1.6 添加 CLI 参数支持

- [ ] Task 2: 创建音乐特征分析工具 (可选)
  - [ ] 2.1 集成 librosa 进行 BPM 检测
  - [ ] 2.2 实现能量值估算
  - [ ] 2.3 实现情绪标签自动生成

## Phase 2: Android 端模块开发

- [x] Task 3: 创建 Android 本地音乐检索模块
  - [x] 3.1 创建 `module-localmusic` 模块
  - [x] 3.2 实现 `LocalMusicIndex` 类 (加载 SQLite)
  - [x] 3.3 实现 `MusicQueryBuilder` 类 (构建 SQL)
  - [x] 3.4 实现 `LocalMusicRepository` 类 (检索接口)
  - [x] 3.5 编写单元测试

- [x] Task 4: 集成 ExoPlayer 播放器
  - [x] 4.1 添加 ExoPlayer 依赖
  - [x] 4.2 实现 `MusicPlayer` 类
  - [x] 4.3 实现播放列表管理
  - [x] 4.4 实现播放状态回调

- [x] Task 5: 修改 ContentEngine 集成本地音乐
  - [x] 5.1 添加本地音乐检索作为第一级策略
  - [x] 5.2 实现 `queryLocalMusic(hints)` 方法
  - [x] 5.3 修改 `curatePlaylist()` 返回本地音乐路径

## Phase 3: 集成测试

- [x] Task 6: 端到端测试
  - [x] 6.1 准备测试音乐数据 (10-20 首)
  - [x] 6.2 运行 PC 端工具生成索引
  - [x] 6.3 ADB 推送到 Android 设备
  - [x] 6.4 测试检索功能
  - [x] 6.5 测试播放功能

- [x] Task 7: 文档更新
  - [x] 7.1 更新数据流说明文档
  - [x] 7.2 编写音乐资源部署指南

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 5] depends on [Task 3]
- [Task 6] depends on [Task 1, Task 3, Task 4, Task 5]
- [Task 7] depends on [Task 6]

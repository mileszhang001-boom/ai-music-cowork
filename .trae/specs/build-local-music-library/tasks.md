# Tasks

## Phase 1: 场景音乐需求分析
- [x] Task 1.1: 分析 20 个场景模板的音乐需求，整理每个场景需要的流派、节奏、能量级别。
- [x] Task 1.2: 汇总所有场景需要的流派类型，统计每种流派需要的歌曲数量。

## Phase 2: 音乐资源调研
- [x] Task 2.1: 调研 Jamendo (jamendo.com) 的音乐分类和元数据结构。
- [x] Task 2.2: 调研 Free Music Archive (freemusicarchive.org) 的音乐分类和元数据结构。
- [x] Task 2.3: 调研 Pixabay Music、Audionautix 等其他免版权音乐网站。
- [x] Task 2.4: 确定最终使用的音乐元数据来源和格式。

## Phase 3: 本地曲库构建
- [x] Task 3.1: 设计曲库 JSON Schema，定义歌曲元数据字段。
- [x] Task 3.2: 创建 `data/music_library.json` 文件结构。
- [x] Task 3.3: 为每个场景收集/生成至少 50 首歌曲的元数据（可使用 LLM 辅助生成）。
- [x] Task 3.4: 验证曲库数据的完整性和一致性。

## Phase 4: Content Engine 集成
- [x] Task 4.1: 修改 Content Engine，支持从本地曲库加载歌曲。
- [x] Task 4.2: 实现场景匹配算法，根据场景类型筛选歌曲。
- [x] Task 4.3: 编写测试脚本验证曲库加载和场景匹配功能。

# Task Dependencies
- [Task 2.x] depends on [Task 1.2]
- [Task 3.x] depends on [Task 2.4]
- [Task 4.x] depends on [Task 3.4]

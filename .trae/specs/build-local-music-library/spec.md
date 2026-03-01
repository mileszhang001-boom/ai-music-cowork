# Build Local Music Library Spec

## Why
当前内容引擎仅使用 8 首硬编码的 Mock 歌曲，无法满足 20 个场景模板的多样化音乐推荐需求。需要构建一个包含真实歌曲元数据的本地曲库，每个场景至少 50 首歌曲，总计约 1000 首歌曲。

## What Changes
- 分析 `scene_templates.json` 中 20 个场景的音乐需求（流派、节奏、能量等）。
- 调研可用的免版权音乐资源网站（Jamendo、Free Music Archive、Pixabay Music 等）。
- 创建本地曲库 JSON 文件 `data/music_library.json`，包含歌曲元数据（不含音频文件）。
- 元数据字段包括：id、title、artist、genre、bpm、energy、duration、language、expression、suitable_scenes 等。
- 更新 Content Engine 以支持从本地曲库加载歌曲。

## Impact
- Affected specs: Layer 3 Content Engine
- Affected code:
  - `src/layers/effects/engines/content/index.js`
  - 新增 `data/music_library.json`

## ADDED Requirements
### Requirement: Local Music Library
系统需要提供一个本地音乐曲库，包含至少 1000 首歌曲的元数据。

#### Scenario: Success case
- **WHEN** Content Engine 需要推荐歌曲时
- **THEN** 能够从本地曲库中根据场景类型筛选出至少 50 首匹配的歌曲。

### Requirement: Scene-Based Music Categorization
每首歌曲需要标注适合的场景类型。

#### Scenario: Success case
- **WHEN** 查询特定场景的歌曲时
- **THEN** 返回所有标注了该场景的歌曲列表。

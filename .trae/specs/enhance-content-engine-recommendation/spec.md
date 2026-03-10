# Enhance Content Engine Recommendation Spec

## Why
当前的内容引擎（Content Engine）仅使用硬编码的 `MockTracks` 数组进行歌曲推荐，无法真正根据输入的 `Scene Descriptor` 和 `orchestrator` 的要求进行智能推荐和编排。为了实现“全网搜索中英文和纯音乐，了解每首歌的表达内容、用户反馈并能和场景描述对应”的需求，需要引入 LLM 来动态生成和编排播放列表。

## What Changes
- 修改 `src/layers/effects/engines/content/index.js`，引入 `llmClient`。
- 在 `curatePlaylist` 方法中，构建 Prompt，将 `Scene Descriptor` 的 `intent` 和 `hints` 传递给 LLM。
- 要求 LLM 扮演音乐推荐专家的角色，模拟全网搜索，推荐符合场景的中英文歌曲和纯音乐。
- LLM 的输出需包含每首歌的表达内容、用户反馈模拟，以及与场景的对应关系，并严格按照 JSON 格式返回。
- 解析 LLM 的返回结果，将其转换为符合 `EffectCommands` 规范的 `playlist` 结构。
- 保留原有的 Mock 逻辑作为降级方案（当 LLM 调用失败或未配置 API Key 时使用）。

## Impact
- Affected specs: Layer 3 Content Engine 的推荐逻辑。
- Affected code: 
  - `src/layers/effects/engines/content/index.js`

## ADDED Requirements
### Requirement: LLM-based Smart Music Recommendation
系统需要能够根据场景描述，智能推荐并编排歌曲。

#### Scenario: Success case
- **WHEN** Orchestrator 调用 Content Engine 的 `curate_playlist` 动作，并传入 `hints` 和 `constraints`
- **THEN** Content Engine 调用 LLM，生成包含中英文和纯音乐的播放列表，每首歌曲包含表达内容和场景匹配度分析，并返回符合规范的 JSON 结构。

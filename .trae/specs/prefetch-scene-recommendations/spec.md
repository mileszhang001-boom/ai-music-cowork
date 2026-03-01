# 场景模板预推荐系统 Spec

## Why
当前项目包含 102 个预设场景模板，但每次请求时都需要实时计算推荐，响应较慢。通过提前分析场景模板并预生成推荐歌单，可以大幅提升用户体验。

## What Changes
- 分析所有 102 个场景模板的音乐需求
- 为每个模板预生成推荐歌单并缓存
- 优化场景匹配逻辑，支持模板 ID 和场景类型匹配
- 添加场景模板预热机制

## Impact
- Affected code:
  - `src/layers/effects/engines/content/index.js`
  - `templates/preset_templates.json`
  - `data/cached_playlists/` (新增缓存目录)

## ADDED Requirements

### Requirement: 场景模板分析
系统 SHALL 分析所有场景模板的音乐需求：

#### Scenario: 提取音乐提示
- **WHEN** 系统启动时
- **THEN** 从 `preset_templates.json` 提取每个模板的 `hints.music` 配置
- **AND** 解析 `intent.mood` (valence, arousal) 映射到音乐特征
- **AND** 解析 `intent.energy_level` 映射到歌曲能量值

#### Scenario: 模板分类统计
- **GIVEN** 102 个场景模板
- **WHEN** 分析模板分布
- **THEN** 按类别统计：
  - time: 20 个（时间相关）
  - weather: 15 个（天气相关）
  - social: 20 个（社交相关）
  - state: 15 个（状态相关）
  - special: 15 个（特殊场景）
  - environment: 15 个（环境相关）

### Requirement: 预生成推荐歌单
系统 SHALL 为每个模板预生成推荐歌单：

#### Scenario: 生成缓存
- **WHEN** 系统启动或音乐库更新时
- **THEN** 为每个模板生成 20-30 首推荐歌曲
- **AND** 保存到 `data/cached_playlists/{template_id}.json`
- **AND** 包含歌曲 ID、评分、匹配度等信息

#### Scenario: 缓存格式
- **GIVEN** 预生成的推荐歌单
- **WHEN** 保存缓存文件
- **THEN** 使用以下格式：
```json
{
  "template_id": "TPL_001",
  "scene_type": "morning_commute",
  "generated_at": "2026-03-01T20:00:00Z",
  "playlist": [
    {
      "id": "song_001",
      "title": "晴天",
      "artist": "周杰伦",
      "score": 85,
      "match_reasons": ["genre:pop", "energy:0.6", "valence:0.7"]
    }
  ],
  "avg_score": 78,
  "total_duration_ms": 5340000
}
```

### Requirement: 场景匹配优化
系统 SHALL 优化场景匹配逻辑：

#### Scenario: 模板 ID 匹配
- **WHEN** 请求包含 `template_id`
- **THEN** 直接使用预生成的缓存歌单
- **AND** 响应时间 < 50ms

#### Scenario: 场景类型匹配
- **WHEN** 请求包含 `scene_type` 但无 `template_id`
- **THEN** 查找该场景类型的所有模板
- **AND** 合并多个模板的推荐结果
- **AND** 去重并按评分排序

#### Scenario: 自定义场景
- **WHEN** 场景不在预设模板中
- **THEN** 使用 LLM 实时生成推荐
- **AND** 缓存生成结果供后续使用

### Requirement: 缓存更新机制
系统 SHALL 维护缓存有效性：

#### Scenario: 音乐库更新
- **WHEN** `index.json` 文件修改时间变化
- **THEN** 重新生成所有预推荐缓存
- **AND** 记录更新日志

#### Scenario: 定期刷新
- **WHEN** 缓存超过 7 天未更新
- **THEN** 自动触发刷新
- **AND** 在后台异步执行

## MODIFIED Requirements

### Requirement: curatePlaylist 方法增强
原有的 curatePlaylist 方法 SHALL 支持缓存优先：

```javascript
async curatePlaylist(hints = {}, constraints = {}, sceneType = null, templateId = null) {
  // 1. 检查模板缓存
  if (templateId && await this._hasCachedPlaylist(templateId)) {
    return this._getCachedPlaylist(templateId, constraints);
  }
  
  // 2. 检查场景类型缓存
  if (sceneType && await this._hasSceneCache(sceneType)) {
    return this._getSceneCachedPlaylist(sceneType, constraints);
  }
  
  // 3. 实时生成（原有逻辑）
  // ...
}
```

## REMOVED Requirements
无

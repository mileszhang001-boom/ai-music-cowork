# 优化 Layer3 歌曲推荐策略 Spec

## Why
当前 Content Engine 的歌曲推荐策略需要优化：
- 模板场景应该直接从曲库快速检索，无需 LLM 调用
- 非模板场景应该先从曲库检索匹配内容，只有找不到时才调用 LLM 联网搜索

## What Changes
- 优化 `curatePlaylist` 方法的检索策略
- 新增 `_searchLibraryByHints` 方法，根据 hints 从曲库智能检索
- 区分模板场景和非模板场景的处理逻辑

## Impact
- Affected code: `src/layers/effects/engines/content/index.js`
- 影响歌曲推荐性能和准确性

## ADDED Requirements

### Requirement: 模板场景快速检索
当 scene_type 匹配预设场景时，系统 SHALL 直接从曲库获取歌曲，无需 LLM 调用。

#### Scenario: 模板场景快速启播
- **WHEN** scene_type 为预设场景（如 morning_commute, night_drive 等）
- **THEN** 直接从 music_library.json 获取对应场景的歌曲
- **AND** 不调用 LLM

### Requirement: 非模板场景智能检索
当 scene_type 不匹配预设场景时，系统 SHALL 按以下顺序检索：

#### Scenario: 非模板场景检索流程
- **WHEN** scene_type 不在预设场景列表中
- **THEN** 执行以下步骤：
  1. 根据 hints（genres, tempo, energy）从曲库智能检索匹配歌曲
  2. 如果曲库有匹配歌曲，返回结果
  3. 如果曲库无匹配或匹配不足，调用 LLM 联网搜索

### Requirement: 曲库智能检索
新增 `_searchLibraryByHints(hints, constraints)` 方法：

```javascript
_searchLibraryByHints(hints, constraints) {
  // 1. 遍历所有场景的歌曲
  // 2. 根据 genres 筛选
  // 3. 根据 tempo/bpm 筛选
  // 4. 根据 energy_level 筛选
  // 5. 根据 language 筛选（中英文、纯音乐）
  // 6. 返回匹配度最高的歌曲
}
```

## MODIFIED Requirements

### Requirement: curatePlaylist 策略优化

```javascript
async curatePlaylist(hints = {}, constraints = {}, sceneType = null) {
  let playlist = [];
  let source = 'mock';

  // 步骤1: 模板场景 - 直接从曲库获取
  if (sceneType && this.getAvailableScenes().includes(sceneType)) {
    const sceneTracks = this.getTracksByScene(sceneType);
    if (sceneTracks.length > 0) {
      playlist = this._filterTracksByHints(sceneTracks, hints, constraints);
      source = 'library';
      return this._buildResult(playlist, source);
    }
  }

  // 步骤2: 非模板场景 - 先从曲库智能检索
  const libraryMatches = this._searchLibraryByHints(hints, constraints);
  if (libraryMatches.length >= (constraints.min_tracks || 5)) {
    playlist = libraryMatches;
    source = 'library_search';
    return this._buildResult(playlist, source);
  }

  // 步骤3: 曲库匹配不足 - 调用 LLM 联网搜索
  if (this.llmClient && this.config.enableLLM) {
    playlist = await this._generatePlaylistWithLLM(hints, constraints);
    source = 'llm';
  }

  // 步骤4: 兜底 - 使用 mock 数据
  if (playlist.length === 0) {
    playlist = this._filterTracksByHints(this.trackLibrary, hints, constraints);
    source = 'mock';
  }

  return this._buildResult(playlist, source);
}
```

# Layer3 推荐算法优化 Spec

## Why
远程分支已经对 Layer3 进行了重大重构（三层推荐策略、本地音乐集成），需要根据其要求优化参数值、参数名称和标签系统，同时结合业界音乐推荐算法最佳实践进一步提升推荐质量。

## What Changes
- 对齐远程分支的参数命名规范（如 `valence` 情绪值参数）
- 优化音乐特征标签系统（mood_tags、scene_tags）
- 引入多维度评分算法（能量匹配、节奏匹配、情绪匹配、标签匹配）
- 优化推荐策略的权重配置
- 添加音乐推荐算法最佳实践（协同过滤思想、基于内容的推荐）

## Impact
- Affected specs: Layer3 推荐系统
- Affected code: 
  - `src/layers/effects/engines/content/index.js`
  - `data/index.json`
  - `templates/preset_templates.json`

## ADDED Requirements

### Requirement: 多维度音乐特征评分系统
系统 SHALL 实现多维度音乐特征评分系统，综合考虑以下因素：

#### Scenario: 能量匹配评分
- **WHEN** 用户请求特定能量等级的音乐
- **THEN** 系统计算每首歌曲的能量差值并评分
- **AND** 能量差值 ≤ 0.1 得 30 分
- **AND** 能量差值 ≤ 0.2 得 20 分
- **AND** 能量差值 ≤ 0.3 得 10 分

#### Scenario: 节奏匹配评分
- **WHEN** 用户请求特定节奏（tempo）的音乐
- **THEN** 系统根据 BPM 范围匹配并评分
- **AND** BPM 在目标范围内得 25 分
- **AND** BPM 偏离范围 ±10 得 15 分

#### Scenario: 情绪匹配评分
- **WHEN** 用户请求特定情绪的音乐
- **THEN** 系统使用 valence 参数进行情绪匹配
- **AND** valence 差值 ≤ 0.15 得 25 分
- **AND** valence 差值 ≤ 0.3 得 15 分

#### Scenario: 标签匹配评分
- **WHEN** 用户请求包含特定标签的音乐
- **THEN** 系统匹配 mood_tags 和 scene_tags
- **AND** 每匹配一个 mood_tag 得 10 分
- **AND** 每匹配一个 scene_tag 得 15 分

### Requirement: 智能推荐策略优化
系统 SHALL 实现三层推荐策略，按优先级执行：

#### Scenario: 模板场景快速匹配
- **WHEN** 场景类型在预设模板列表中
- **THEN** 优先从本地 JSON 数据库精确匹配
- **AND** 匹配条件：scene_tags 包含场景类型
- **AND** 返回前 10-20 首最高分歌曲

#### Scenario: 非模板场景智能推荐
- **WHEN** 场景类型不在预设模板列表中
- **THEN** 使用 LLM 从本地音乐库智能选取
- **AND** LLM 考虑所有音频特征（BPM、energy、valence、tags）
- **AND** 返回 LLM 推荐的歌曲列表

#### Scenario: 兜底推荐
- **WHEN** 以上策略都无法返回结果
- **THEN** 使用预设的 FALLBACK_PLAYLIST
- **AND** 根据 hints 参数过滤兜底列表

### Requirement: 参数命名规范化
系统 SHALL 使用统一的参数命名规范：

#### Scenario: 音频特征参数
- **GIVEN** 远程分支使用 valence 表示情绪值
- **WHEN** 处理音乐特征数据
- **THEN** 使用以下标准参数名：
  - `bpm`: 节奏速度
  - `energy`: 能量值 (0-1)
  - `valence`: 情绪值 (0-1，正向/负向)
  - `mood_tags`: 情绪标签数组
  - `scene_tags`: 场景标签数组

### Requirement: 标签系统扩展
系统 SHALL 支持丰富的标签体系：

#### Scenario: 情绪标签 (mood_tags)
- **WHEN** 分析歌曲情绪特征
- **THEN** 支持以下标准情绪标签：
  - happy, sad, energetic, calm, romantic, melancholy
  - nostalgic, epic, warm, dreamy, peaceful, uplifting

#### Scenario: 场景标签 (scene_tags)
- **WHEN** 分析歌曲适用场景
- **THEN** 支持以下标准场景标签：
  - morning_commute, night_drive, road_trip, party
  - romantic_date, family_trip, focus_work, relax
  - workout, rainy_day, traffic_jam, fatigue_alert

## MODIFIED Requirements

### Requirement: curatePlaylist 方法优化
原有的 curatePlaylist 方法 SHALL 增强为：

```javascript
async curatePlaylist(hints = {}, constraints = {}, sceneType = null) {
  // 1. 计算每首歌曲的综合评分
  // 2. 按评分排序
  // 3. 应用约束条件（max_tracks, min_energy, max_duration）
  // 4. 返回最优推荐列表
}
```

#### Scenario: 综合评分计算
- **WHEN** 评估歌曲匹配度
- **THEN** 综合评分 = 能量分 + 节奏分 + 情绪分 + 标签分
- **AND** 总分范围 0-100
- **AND** 只返回评分 ≥ 40 的歌曲

## REMOVED Requirements
无

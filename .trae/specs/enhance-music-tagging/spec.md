# 曲库歌曲标签系统优化 Spec

## Why
当前曲库歌曲只有基础字段（genre, bpm, energy），缺少丰富的标签体系，导致非模板场景推荐匹配不够精准。需要参考 Spotify 等音乐平台的标签体系，为每首歌曲添加多维度的标签，提升推荐准确性。

## What Changes
- 扩展歌曲元数据结构，新增多维度标签字段
- 为曲库中 1000 首歌曲补充完整标签
- 优化非模板场景的标签匹配推荐逻辑

## Impact
- Affected code: `data/music_library.json`, `src/layers/effects/engines/content/index.js`
- 影响歌曲推荐的准确性和丰富度

## ADDED Requirements

### Requirement: 歌曲标签体系设计
参考 Spotify 音频特征和 Valence-Arousal 情绪模型，每首歌曲 SHALL 包含以下标签维度：

#### 客观属性标签
| 标签 | 类型 | 说明 | 示例值 |
|------|------|------|--------|
| `bpm` | number | 节拍速度 | 120 |
| `duration` | number | 时长（秒） | 215 |
| `key` | string | 调性 | "C_major", "A_minor" |
| `time_signature` | number | 拍号 | 4 |

#### Spotify 风格音频特征
| 标签 | 类型 | 范围 | 说明 |
|------|------|------|------|
| `energy` | float | 0.0-1.0 | 能量强度 |
| `valence` | float | 0.0-1.0 | 情绪正向（快乐/悲伤） |
| `danceability` | float | 0.0-1.0 | 可舞性 |
| `acousticness` | float | 0.0-1.0 | 原声程度 |
| `instrumentalness` | float | 0.0-1.0 | 纯音乐程度 |
| `speechiness` | float | 0.0-1.0 | 人声程度 |

#### 场景匹配标签
| 标签 | 类型 | 说明 |
|------|------|------|
| `mood_tags` | string[] | 情绪标签：["happy", "sad", "calm", "energetic", "romantic", "melancholy"] |
| `activity_tags` | string[] | 活动标签：["driving", "working", "exercise", "relax", "party", "sleep"] |
| `weather_tags` | string[] | 天气标签：["sunny", "rainy", "cloudy", "night"] |
| `time_tags` | string[] | 时间标签：["morning", "afternoon", "evening", "night", "late_night"] |
| `scene_tags` | string[] | 场景标签：["commute", "highway", "city", "home", "office"] |

### Requirement: 标签生成规则

#### 从现有字段推导
```javascript
// valence 从 expression 和 genre 推导
if (genre.includes('jazz', 'classical', 'lo-fi')) valence = 0.3-0.5;
if (genre.includes('pop', 'rock', 'electronic')) valence = 0.5-0.8;

// danceability 从 bpm 推导
danceability = Math.min(1, bpm / 150);

// instrumentalness 从 language 推导
instrumentalness = language === 'instrumental' ? 0.9 : 0.1;

// mood_tags 从 energy 和 valence 推导
if (energy > 0.7 && valence > 0.6) mood_tags.push('energetic', 'happy');
if (energy < 0.3 && valence < 0.4) mood_tags.push('calm', 'melancholy');
```

### Requirement: 非模板场景标签匹配

新增 `_matchByTags(hints, tracks)` 方法：

```javascript
_matchByTags(hints, tracks) {
  return tracks.map(track => {
    let score = 0;
    
    // 情绪匹配
    if (hints.mood && track.mood_tags?.includes(hints.mood)) score += 25;
    
    // 活动匹配
    if (hints.activity && track.activity_tags?.includes(hints.activity)) score += 20;
    
    // 天气匹配
    if (hints.weather && track.weather_tags?.includes(hints.weather)) score += 15;
    
    // 时间匹配
    if (hints.time && track.time_tags?.includes(hints.time)) score += 15;
    
    // 场景匹配
    if (hints.scene && track.scene_tags?.includes(hints.scene)) score += 20;
    
    // Valence-Arousal 匹配
    if (hints.valence !== undefined) {
      score += (1 - Math.abs(track.valence - hints.valence)) * 15;
    }
    if (hints.arousal !== undefined) {
      score += (1 - Math.abs(track.energy - hints.arousal)) * 15;
    }
    
    return { ...track, _tagScore: score };
  })
  .filter(t => t._tagScore > 0)
  .sort((a, b) => b._tagScore - a._tagScore);
}
```

## MODIFIED Requirements

### Requirement: 歌曲数据结构扩展

```json
{
  "id": "mc_001",
  "title": "Good Morning",
  "artist": "Max Frost",
  "genre": "pop",
  "bpm": 110,
  "energy": 0.45,
  "duration": 215,
  "language": "en",
  "expression": "清晨的阳光，充满希望",
  "user_feedback": "很适合早晨听",
  "scene_match": "早晨通勤，正能量",
  
  "新增标签字段": {
    "valence": 0.7,
    "danceability": 0.6,
    "acousticness": 0.3,
    "instrumentalness": 0.0,
    "mood_tags": ["happy", "energetic", "hopeful"],
    "activity_tags": ["driving", "morning", "commute"],
    "weather_tags": ["sunny"],
    "time_tags": ["morning"],
    "scene_tags": ["commute", "city"]
  }
}
```

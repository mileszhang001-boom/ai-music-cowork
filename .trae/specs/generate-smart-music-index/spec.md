# 基于下载音乐生成智能标签索引 Spec

## Why
用户已下载 635 首真实音乐文件（7.4GB），需要调用 Qwen 大模型为每首歌曲生成完整的元数据标签，参考主流音乐软件（Spotify、网易云音乐、QQ音乐）的曲库建设标准，构建专业的音乐索引库。

## What Changes
- 扫描所有音乐文件，提取基础信息（文件名、时长、格式等）
- 调用 Qwen 大模型分析每首歌曲，生成多维度标签
- 参考 Spotify 音频特征和网易云音乐标签体系设计标签结构
- 生成完整的 `index.json` 索引文件
- 支持批量处理和进度跟踪

## Impact
- Affected code: 
  - `data/index.json` (新生成)
  - `tools/music-organizer/` (增强标签生成工具)
  - `src/layers/effects/engines/content/index.js` (使用新索引)
- 影响 Layer3 推荐系统的歌曲库质量

## ADDED Requirements

### Requirement: 音乐标签体系设计
参考主流音乐平台，每首歌曲 SHALL 包含以下标签维度：

#### 基础元数据
| 字段 | 类型 | 说明 | 来源 |
|------|------|------|------|
| `id` | number | 唯一标识符 | 自动生成 |
| `title` | string | 歌曲标题 | 文件名/LLM |
| `artist` | string | 艺术家 | 文件名/LLM |
| `album` | string | 专辑名称 | LLM |
| `genre` | string | 音乐流派 | LLM |
| `year` | number | 发行年份 | LLM |
| `duration_ms` | number | 时长（毫秒） | 音频文件 |
| `file_path` | string | 文件绝对路径 | 扫描 |
| `file_size` | number | 文件大小（字节） | 扫描 |
| `format` | string | 音频格式 | 扫描 |
| `bitrate` | number | 比特率 | 音频文件 |
| `sample_rate` | number | 采样率 | 音频文件 |

#### Spotify 风格音频特征
| 字段 | 类型 | 范围 | 说明 |
|------|------|------|------|
| `bpm` | number | 60-200 | 节拍速度 |
| `energy` | float | 0.0-1.0 | 能量强度（活跃程度） |
| `valence` | float | 0.0-1.0 | 情绪正向（快乐/悲伤） |
| `danceability` | float | 0.0-1.0 | 可舞性 |
| `acousticness` | float | 0.0-1.0 | 原声程度 |
| `instrumentalness` | float | 0.0-1.0 | 纯音乐程度 |

#### 网易云音乐风格标签
| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `mood_tags` | string[] | 情绪标签 | ["快乐", "治愈", "励志"] |
| `scene_tags` | string[] | 场景标签 | ["夜晚", "独处", "驾驶"] |
| `language` | string | 语言 | "zh", "en", "ja" |
| `era` | string | 年代 | "90s", "2000s", "2010s" |

#### 扩展标签（参考QQ音乐）
| 字段 | 类型 | 说明 |
|------|------|------|
| `theme_tags` | string[] | 主题标签：["爱情", "友情", "青春", "回忆"] |
| `instrument_tags` | string[] | 乐器标签：["钢琴", "吉他", "电子合成器"] |
| `tempo` | string | 节奏类型：["slow", "medium", "fast"] |

### Requirement: LLM 标签生成规则

#### Scenario: 歌曲分析提示词
- **WHEN** 调用 Qwen 分析歌曲
- **THEN** 使用以下提示词模板：

```
分析以下歌曲并生成音乐标签：

歌曲信息：
- 标题：{title}
- 艺术家：{artist}
- 文件名：{filename}

请以 JSON 格式返回以下信息：
{
  "title": "标准化后的歌曲标题",
  "artist": "标准化后的艺术家名",
  "album": "专辑名称（如果知道）",
  "genre": "流派（pop/rock/jazz/classical/electronic/folk/r&b/hip-hop/metal/country/blues）",
  "year": 发行年份,
  "bpm": 节拍速度（60-200）,
  "energy": 能量值（0.0-1.0）,
  "valence": 情绪正向值（0.0-1.0，1=非常快乐，0=非常悲伤）,
  "danceability": 可舞性（0.0-1.0）,
  "acousticness": 原声程度（0.0-1.0）,
  "instrumentalness": 纯音乐程度（0.0-1.0）,
  "mood_tags": ["情绪标签1", "情绪标签2"],
  "scene_tags": ["场景标签1", "场景标签2"],
  "theme_tags": ["主题标签1"],
  "instrument_tags": ["主要乐器"],
  "language": "语言代码",
  "era": "年代",
  "tempo": "slow/medium/fast",
  "description": "简短描述（20字以内）"
}

参考标准：
- mood_tags 可选值：快乐、悲伤、平静、激昂、浪漫、忧郁、治愈、励志、怀旧、神秘、温暖、清新、伤感、甜蜜、孤独、希望、愤怒、恐惧、放松、兴奋
- scene_tags 可选值：早晨、夜晚、驾驶、工作、运动、派对、约会、旅行、学习、睡眠、冥想、雨天、晴天、夏天、冬天、独处、聚会
- theme_tags 可选值：爱情、友情、亲情、青春、回忆、梦想、自然、城市、季节、节日
```

### Requirement: 批量处理机制

#### Scenario: 进度跟踪
- **WHEN** 处理大量歌曲时
- **THEN** 系统显示进度条和预估时间
- **AND** 支持断点续传（记录已处理的歌曲）
- **AND** 每处理 50 首歌曲保存一次中间结果

#### Scenario: 错误处理
- **WHEN** LLM 调用失败时
- **THEN** 记录错误日志
- **AND** 使用默认值填充标签
- **AND** 标记为 `llm_analyzed: 0`

### Requirement: 标签质量验证

#### Scenario: 标签完整性检查
- **WHEN** 生成标签后
- **THEN** 验证必填字段不为空
- **AND** 数值字段在有效范围内
- **AND** 数组字段至少包含一个元素

## MODIFIED Requirements

### Requirement: 索引文件格式
生成的 `index.json` SHALL 使用以下格式：

```json
[
  {
    "id": 1,
    "title": "晴天",
    "artist": "周杰伦",
    "album": "叶惠美",
    "genre": "pop",
    "year": 2003,
    "bpm": 95,
    "energy": 0.65,
    "valence": 0.7,
    "danceability": 0.6,
    "acousticness": 0.4,
    "instrumentalness": 0.0,
    "mood_tags": ["怀旧", "温暖", "治愈"],
    "scene_tags": ["校园", "青春", "回忆"],
    "theme_tags": ["青春", "爱情"],
    "instrument_tags": ["吉他", "钢琴"],
    "language": "zh",
    "era": "2000s",
    "tempo": "medium",
    "duration_ms": 269000,
    "file_path": "/path/to/file.mp3",
    "file_size": 8234567,
    "format": "MP3",
    "bitrate": 320,
    "sample_rate": 44100,
    "play_count": 0,
    "llm_analyzed": 1,
    "description": "校园民谣经典"
  }
]
```

## REMOVED Requirements
无

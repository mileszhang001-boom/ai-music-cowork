# 本地音乐资源集成方案 Spec

## Why
用户有 5GB 音乐资源在电脑上，需要通过 Android App 在车机上进行检索、生成歌单、播放。当前 Layer3 的 ContentEngine 只能通过 LLM 生成虚构歌曲，无法播放真实音乐。

## What Changes
- 创建 PC 端音乐元数据提取工具
- 创建 Android 端本地音乐检索模块
- 修改 ContentEngine 支持本地音乐库检索
- 集成 ExoPlayer 实现本地音乐播放

## Impact
- Affected specs: Layer3 ContentEngine
- Affected code: 
  - `src/layers/effects/engines/content/index.js` (Node.js 端)
  - `Android_app/features/generation/` (Android 端)
  - 新增：`tools/music-organizer/` (PC 端工具)

---

## 整体方案架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          PC 端（准备工作）                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 音乐资源整理                                                         │
│     /music_raw/                                                          │
│     ├── pop/         ← 流派分类                                          │
│     ├── rock/                                                           │
│     ├── jazz/                                                           │
│     └── ...                                                             │
│                                                                          │
│  2. 元数据提取工具 (Python 脚本)                                         │
│     python organize_music.py                                            │
│       --input ./music_raw/                                              │
│       --output ./music_package/                                         │
│                                                                          │
│  3. 输出产物                                                             │
│     /music_package/                                                      │
│     ├── music/           ← 整理后的音乐文件                              │
│     │   ├── pop/                                                        │
│     │   └── ...                                                         │
│     └── index.db         ← SQLite 索引数据库 (含 FTS5 全文检索)          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
                         USB / SD 卡 / ADB 推送
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                        车机端（Android App）                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  存储位置: /sdcard/Music/AiMusic/                                        │
│  ├── music/              ← 音乐文件                                      │
│  └── index.db            ← 索引数据库                                    │
│                                                                          │
│  数据流:                                                                 │
│                                                                          │
│  Layer2 (SemanticEngine)                                                 │
│      ↓                                                                   │
│  SceneDescriptor { hints: { genres: ["pop"], energy: 0.6 } }           │
│      ↓                                                                   │
│  Layer3 (ContentEngine)                                                  │
│      ↓                                                                   │
│  ┌─────────────────────────────────────────┐                            │
│  │ 本地音乐检索 (新增)                       │                            │
│  │ SELECT * FROM tracks                     │                            │
│  │ WHERE genre IN ('pop')                   │                            │
│  │ AND energy BETWEEN 0.5 AND 0.7           │                            │
│  │ LIMIT 20                                 │                            │
│  └─────────────────────────────────────────┘                            │
│      ↓                                                                   │
│  Playlist: [                                                             │
│    { title: "Song A", file_path: "/sdcard/Music/AiMusic/pop/song.mp3" } │
│  ]                                                                       │
│      ↓                                                                   │
│  ExoPlayer 播放                                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ADDED Requirements

### Requirement: PC 端音乐元数据提取工具

系统 SHALL 提供 Python 脚本用于提取音乐元数据并生成 SQLite 索引数据库。

#### Scenario: 提取音乐元数据
- **GIVEN** 用户有音乐文件目录
- **WHEN** 运行 `python organize_music.py --input ./music/ --output ./output/`
- **THEN** 生成 `music/` 目录和 `index.db` 数据库文件
- **AND** 数据库包含 FTS5 全文检索表

### Requirement: Android 本地音乐检索模块

系统 SHALL 提供 Android 端的本地音乐检索能力。

#### Scenario: 加载音乐索引
- **GIVEN** 音乐文件和索引已存储在 `/sdcard/Music/AiMusic/`
- **WHEN** App 启动时
- **THEN** 加载 `index.db` 到内存
- **AND** 验证音乐文件完整性

#### Scenario: 按条件检索音乐
- **GIVEN** 音乐索引已加载
- **WHEN** 调用 `queryTracks(hints)` 方法
- **THEN** 返回匹配的音乐列表
- **AND** 列表按匹配度排序

### Requirement: ContentEngine 本地音乐集成

系统 SHALL 修改 ContentEngine 优先使用本地音乐库。

#### Scenario: 本地音乐库命中
- **GIVEN** 本地音乐库有匹配的歌曲
- **WHEN** ContentEngine 生成歌单
- **THEN** 返回本地音乐列表
- **AND** source = 'local'

#### Scenario: 本地音乐库未命中
- **GIVEN** 本地音乐库无匹配歌曲
- **WHEN** ContentEngine 生成歌单
- **THEN** 回退到 LLM 生成（现有逻辑）

### Requirement: ExoPlayer 音乐播放

系统 SHALL 使用 ExoPlayer 播放本地音乐文件。

#### Scenario: 播放音乐
- **GIVEN** 歌单已生成
- **WHEN** 调用 `play(playlist)` 方法
- **THEN** ExoPlayer 开始播放
- **AND** 支持上一首/下一首/暂停操作

---

## 数据库设计

### SQLite 表结构

```sql
-- 主表：曲目信息
CREATE TABLE tracks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 基本信息
    title TEXT NOT NULL,
    title_pinyin TEXT,
    artist TEXT NOT NULL,
    artist_pinyin TEXT,
    album TEXT,
    year INTEGER,
    
    -- 分类标签
    genre TEXT,
    sub_genres TEXT,        -- JSON 数组
    language TEXT,          -- zh, en, ja, instrumental
    era TEXT,               -- 80s, 90s, 2000s, 2010s, 2020s
    
    -- 音乐特征 (AI 匹配用)
    bpm INTEGER,
    energy REAL,            -- 0.0 - 1.0
    danceability REAL,
    valence REAL,           -- 情绪正向度
    acousticness REAL,
    instrumentalness REAL,
    
    -- 情绪标签 (JSON 数组)
    mood_tags TEXT,         -- ["happy", "energetic"]
    scene_tags TEXT,        -- ["morning", "road_trip"]
    weather_tags TEXT,
    
    -- 播放信息
    duration_ms INTEGER,
    file_path TEXT NOT NULL,
    file_size INTEGER,
    format TEXT,
    bitrate INTEGER,
    
    -- 元数据
    play_count INTEGER DEFAULT 0,
    user_rating INTEGER,
    is_favorite INTEGER DEFAULT 0
);

-- 全文检索虚拟表
CREATE VIRTUAL TABLE tracks_fts USING fts5(
    title, title_pinyin, artist, artist_pinyin, 
    album, genre, mood_tags, scene_tags,
    content='tracks', content_rowid='id'
);

-- 索引
CREATE INDEX idx_tracks_genre ON tracks(genre);
CREATE INDEX idx_tracks_energy ON tracks(energy);
CREATE INDEX idx_tracks_bpm ON tracks(bpm);
```

---

## 文件传输方案

### 开发阶段：ADB 推送

```bash
# 1. 推送音乐文件
adb push ./music_package/music/ /sdcard/Music/AiMusic/

# 2. 推送索引数据库
adb push ./music_package/index.db /sdcard/Music/AiMusic/

# 3. 验证
adb shell ls -la /sdcard/Music/AiMusic/
```

### Demo 阶段：USB/SD 卡

1. 将 `music_package/` 目录复制到 USB 或 SD 卡
2. 在车机上插入 USB/SD 卡
3. 使用文件管理器复制到 `/sdcard/Music/AiMusic/`

### 生产阶段：应用内下载（可选）

1. 首次启动时检测音乐库
2. 如不存在，提示用户下载
3. 后台下载并解压到指定目录

---

## 检索策略

### AI 意图 → SQL 查询映射

```javascript
// Layer2 输出
const hints = {
  genres: ["pop", "rock"],
  energy: 0.6,
  tempo: "fast",
  mood: ["happy", "energetic"]
};

// 转换为 SQL
function buildQuery(hints) {
  let sql = "SELECT * FROM tracks WHERE 1=1";
  const params = [];
  
  // 流派
  if (hints.genres?.length) {
    sql += ` AND genre IN (${hints.genres.map(() => '?').join(',')})`;
    params.push(...hints.genres);
  }
  
  // 能量范围 (±0.2 容差)
  if (hints.energy) {
    sql += " AND energy BETWEEN ? AND ?";
    params.push(hints.energy - 0.2, hints.energy + 0.2);
  }
  
  // BPM 范围
  if (hints.tempo === 'slow') {
    sql += " AND bpm BETWEEN 60 AND 90";
  } else if (hints.tempo === 'fast') {
    sql += " AND bpm BETWEEN 120 AND 160";
  }
  
  // 情绪标签
  if (hints.mood?.length) {
    sql += ` AND (${hints.mood.map(() => 'mood_tags LIKE ?').join(' OR ')})`;
    params.push(...hints.mood.map(m => `%${m}%`));
  }
  
  sql += " ORDER BY (energy * 0.3 + valence * 0.3) DESC LIMIT 20";
  
  return { sql, params };
}
```

---

## 延迟预估

| 操作 | 延迟 |
|------|------|
| 加载索引数据库 (一次性) | ~100ms |
| 本地检索 (SQLite) | **< 10ms** |
| ExoPlayer 开始播放 | ~50ms |
| **总延迟** | **< 200ms** |

对比：LLM 生成歌单需要 10 秒，本地检索仅需 10ms，**快 1000 倍**。

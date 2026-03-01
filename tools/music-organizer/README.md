# PC端音乐元数据提取工具

用于提取音乐文件的元数据并生成 SQLite 索引数据库的命令行工具。

## 功能特性

- 支持多种音频格式：MP3、FLAC、WAV、M4A
- 提取完整元数据：标题、艺术家、专辑、流派、时长、比特率、采样率
- 生成 SQLite 数据库，支持 FTS5 全文检索
- 支持中文拼音转换，便于中文歌曲检索
- 支持多种导出格式：SQLite、JSON、CSV

## 安装

```bash
# 进入工具目录
cd tools/music-organizer

# 安装依赖
pip install -r requirements.txt
```

## 使用方法

### 基本用法

```bash
python organize_music.py --input /path/to/music --output /path/to/index.db
```

### 参数说明

| 参数 | 简写 | 说明 | 必需 |
|------|------|------|------|
| `--input` | `-i` | 输入音乐目录路径 | 是 |
| `--output` | `-o` | 输出数据库文件路径 | 是 |
| `--format` | `-f` | 输出格式：db/json/csv/all | 否（默认：db） |

### 使用示例

```bash
# 仅生成 SQLite 数据库
python organize_music.py -i ~/Music -o ./music_index.db

# 同时生成 JSON 和 CSV 文件
python organize_music.py -i ~/Music -o ./index.db --format all

# 仅导出 JSON 格式
python organize_music.py -i /path/to/music -o ./output.json --format json
```

## 数据库结构

### tracks 表（主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER | 主键 |
| title | TEXT | 歌曲标题 |
| title_pinyin | TEXT | 标题拼音 |
| artist | TEXT | 艺术家 |
| artist_pinyin | TEXT | 艺术家拼音 |
| album | TEXT | 专辑名 |
| genre | TEXT | 流派 |
| bpm | INTEGER | 节拍 |
| energy | REAL | 能量值 |
| valence | REAL | 情感值 |
| mood_tags | TEXT | 情绪标签 |
| scene_tags | TEXT | 场景标签 |
| duration_ms | INTEGER | 时长（毫秒） |
| file_path | TEXT | 文件路径 |
| file_size | INTEGER | 文件大小 |
| format | TEXT | 格式 |
| bitrate | INTEGER | 比特率 |
| sample_rate | INTEGER | 采样率 |
| play_count | INTEGER | 播放次数 |

### tracks_fts 表（FTS5 全文检索虚拟表）

用于对歌曲标题、艺术家、专辑等字段进行全文检索，支持中文拼音搜索。

## 全文检索示例

```sql
-- 搜索标题或艺术家包含 "love" 的歌曲
SELECT * FROM tracks_fts WHERE tracks_fts MATCH 'love';

-- 搜索拼音包含 "zhoujielun" 的歌曲（周杰伦）
SELECT * FROM tracks_fts WHERE tracks_fts MATCH 'artist_pinyin:zhoujielun';

-- 组合搜索
SELECT t.* FROM tracks t
JOIN tracks_fts fts ON t.id = fts.rowid
WHERE tracks_fts MATCH 'album:叶惠美';
```

## 依赖说明

- **mutagen**: 音频元数据提取库，支持多种音频格式
- **pypinyin**: 中文拼音转换库（可选，用于中文检索支持）

# PC 端音乐元数据提取工具

用于提取音乐文件的元数据并生成 SQLite 索引数据库，支持 LLM 分析音乐标签。

## 安装依赖

```bash
pip install mutagen pypinyin requests
```

## 使用方法

### 基本用法（只提取元数据）

```bash
python organize_music.py -i /path/to/music -o ./index.db
```

### 使用 LLM 分析标签（推荐）

```bash
# 方式1：命令行传入 API Key
python organize_music.py -i /path/to/music -o ./index.db --llm --api-key sk-xxx

# 方式2：使用环境变量
export DASHSCOPE_API_KEY=sk-xxx
python organize_music.py -i /path/to/music -o ./index.db --llm
```

## 参数说明

| 参数 | 说明 |
|------|------|
| `-i, --input` | 输入音乐目录路径（必需） |
| `-o, --output` | 输出数据库文件路径（必需） |
| `-f, --format` | 输出格式：db / json / all（默认：db） |
| `--llm` | 使用 LLM 分析音乐标签 |
| `--api-key` | 阿里云百炼 API Key |
| `--api-base` | API 地址（默认：阿里云百炼） |
| `--batch-size` | LLM 批量处理大小（默认：10） |

## LLM 分析说明

使用 LLM 分析每首音乐的以下标签：

| 标签 | 说明 |
|------|------|
| `genre` | 主要流派（pop/rock/jazz 等） |
| `bpm` | 节拍数（60-200） |
| `energy` | 能量值（0.0-1.0） |
| `valence` | 情绪正向度（0.0-1.0） |
| `mood_tags` | 情绪标签（happy/sad/energetic 等） |
| `scene_tags` | 场景标签（morning_commute/night_drive 等） |

## 输出产物

```
index.db          # SQLite 数据库（含 FTS5 全文检索）
index.json        # JSON 格式（可选）
```

## 数据库结构

```sql
CREATE TABLE tracks (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    genre TEXT,
    bpm INTEGER,
    energy REAL,
    valence REAL,
    mood_tags TEXT,      -- JSON 数组
    scene_tags TEXT,     -- JSON 数组
    file_path TEXT NOT NULL,
    llm_analyzed INTEGER -- 是否经过 LLM 分析
);
```

## 示例输出

```
扫描目录: /Users/mi/Music
发现 1500 个音乐文件

处理 [1/1500]: 周杰伦 - 晴天.mp3
  LLM 分析中...
  ✓ 流派: pop, 能量: 0.6, 情绪: ["happy", "nostalgic"]

处理 [2/1500]: Linkin Park - In The End.mp3
  LLM 分析中...
  ✓ 流派: rock, 能量: 0.8, 情绪: ["energetic", "dark"]

处理完成:
  成功: 1500
  LLM 分析: 1480
  失败: 20
  数据库: ./index.db
```

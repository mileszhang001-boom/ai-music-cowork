# PC 端音乐元数据提取工具

用于提取音乐文件的元数据并生成 JSON 索引文件，支持 LLM 分析音乐标签。

## 安装依赖

```bash
pip install mutagen pypinyin requests
```

## 使用方法

```bash
# 基本用法
python organize_music.py -i /path/to/music -o ./index.json

# 使用 LLM 分析标签（推荐）
python organize_music.py -i /path/to/music -o ./index.json --llm --api-key sk-xxx
```

## 参数说明

| 参数 | 说明 |
|------|------|
| `-i, --input` | 输入音乐目录路径（必需） |
| `-o, --output` | 输出 JSON 文件路径（必需） |
| `--llm` | 使用 LLM 分析音乐标签 |
| `--api-key` | 阿里云百炼 API Key |

---

## 读取 index.json 配置

### Android 设备

```bash
adb push ./index.json /sdcard/Music/AiMusic/
```

代码已写好，默认读取 `/sdcard/Music/AiMusic/index.json`

### Node.js 调试

修改 `.env.local`：

```bash
LOCAL_MUSIC_DB=/path/to/your/index.json
```

然后运行 `npm run debug`

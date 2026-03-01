# 导出歌曲库表格规范

## Why
当前歌曲库包含1000首歌曲信息，存储在JSON格式中。为了方便其他AI系统识别和阅读这些歌曲信息，需要将歌曲库导出为结构化的表格格式（如CSV或Markdown表格），便于导入和分析。

## What Changes
- 创建一个脚本，读取 `data/music_library.json` 文件
- 提取所有场景下的所有歌曲信息
- 生成结构化的表格文件（支持CSV和Markdown两种格式）
- 表格包含关键字段：ID、歌曲名称、歌手、流派、BPM、能量值、时长、语言、情感表达、场景匹配等

## Impact
- Affected specs: 无
- Affected code: 新增脚本文件，不影响现有代码

## ADDED Requirements

### Requirement: 歌曲库表格导出功能
系统 SHALL 提供将音乐库JSON数据导出为表格格式的功能。

#### Scenario: 导出CSV格式
- **WHEN** 用户运行导出脚本并选择CSV格式
- **THEN** 系统应生成包含所有歌曲信息的CSV文件，每行代表一首歌曲，包含所有关键字段

#### Scenario: 导出Markdown格式
- **WHEN** 用户运行导出脚本并选择Markdown格式
- **THEN** 系统应生成包含所有歌曲信息的Markdown表格文件，便于在文档中展示

#### Scenario: 导出所有场景歌曲
- **WHEN** 执行导出操作
- **THEN** 系统应遍历所有场景（morning_commute、night_drive等），提取所有歌曲信息，并合并到一个表格中

### Requirement: 表格字段定义
导出的表格 SHALL 包含以下字段：

| 字段名 | 说明 | 示例 |
|--------|------|------|
| ID | 歌曲唯一标识 | mc_001 |
| 歌曲名称 | 歌曲标题 | Good Morning |
| 歌手 | 演唱者/创作者 | Max Frost |
| 流派 | 音乐流派 | pop |
| BPM | 节拍数 | 110 |
| 能量值 | 能量级别(0-1) | 0.45 |
| 时长(秒) | 歌曲时长 | 215 |
| 语言 | 歌曲语言 | en/zh/instrumental |
| 情感表达 | 歌曲情感描述 | 充满活力的早晨唤醒曲 |
| 用户反馈 | 模拟用户评价 | 非常适合早起的歌曲 |
| 场景匹配 | 场景适配说明 | 轻松的流行节奏适合早晨通勤 |
| 所属场景 | 歌曲所属场景 | morning_commute |

### Requirement: 输出文件位置
导出的表格文件 SHALL 保存在 `data/` 目录下：
- CSV格式: `data/music_library_export.csv`
- Markdown格式: `data/music_library_export.md`

## MODIFIED Requirements
无

## REMOVED Requirements
无

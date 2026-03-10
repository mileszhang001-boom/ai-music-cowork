# Layer3 推荐层与本地音乐管理模块关系澄清 Spec

## Why
当前架构中 Layer3 推荐层与本地音乐管理模块的职责边界不清晰，需要明确定义两者之间的数据流和职责分工。

## What Changes
- 明确本地音乐管理模块的职责范围
- 明确 Layer3 推荐层的职责范围
- 定义两者之间的数据接口
- 重构 Layer3 推荐策略为三层实现

## Impact
- Affected specs: local-music-integration
- Affected code: 
  - `src/layers/effects/engines/content/index.js` (Node.js Layer3)
  - `Android_app/features/localmusic/` (Android 本地音乐模块)

---

## 架构定义

### 模块职责划分

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    本地音乐管理模块 (LocalMusicModule)                    │
├─────────────────────────────────────────────────────────────────────────┤
│ 职责：                                                                   │
│ 1. 读取预先生成的 index.db 数据库文件                                    │
│ 2. 将 index.db 数据传输至 Layer3 推荐层                                  │
│ 3. 接收并处理 Layer3 生成的音乐歌单数据                                  │
│ 4. 负责播放本地音乐资源文件                                              │
│                                                                          │
│ 不负责：                                                                 │
│ - 场景分析、推荐逻辑（由 Layer3 负责）                                   │
│ - 歌单生成策略（由 Layer3 负责）                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓↑ 数据交互
┌─────────────────────────────────────────────────────────────────────────┐
│                      Layer3 推荐层 (ContentEngine)                       │
├─────────────────────────────────────────────────────────────────────────┤
│ 职责：                                                                   │
│ 基于 Layer2 输入的描述信息（模板或大模型生成）与本地音乐管理模块传入的    │
│ index.db 数据，生成符合场景需求的音乐歌单                                │
│                                                                          │
│ 推荐策略（三层）：                                                       │
│ 1. 模板场景：SQL 查询直接匹配 → 快速启播                                 │
│ 2. 非模板场景：调用大模型智能选取 → 个性化歌单                           │
│ 3. 兜底场景：预设固定音乐清单 → 备选方案                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

### 数据流定义

```
Layer2 输出 (SceneDescriptor)
    │
    │  { scene_type, hints: { genres, energy, tempo, mood } }
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ Layer3 推荐层                                                            │
│                                                                          │
│  1. 接收 Layer2 输出                                                     │
│  2. 从本地音乐管理模块获取 index.db 数据                                  │
│  3. 执行推荐策略                                                         │
│  4. 输出歌单数据                                                         │
│                                                                          │
│  输出格式：                                                              │
│  {                                                                       │
│    playlist: [                                                           │
│      { id, title, artist, file_path, ... }                               │
│    ],                                                                    │
│    source: 'sql' | 'llm' | 'fallback',                                   │
│    scene_match: { ... }                                                  │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘
    │
    │  歌单数据
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│ 本地音乐管理模块                                                         │
│                                                                          │
│  1. 接收歌单数据                                                         │
│  2. 加载音乐文件                                                         │
│  3. ExoPlayer 播放                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ADDED Requirements

### Requirement: 本地音乐管理模块职责

系统 SHALL 将本地音乐管理模块定义为独立模块，负责：

#### Scenario: 读取 index.db
- **GIVEN** index.db 文件存在于 `/sdcard/Music/AiMusic/index.db`
- **WHEN** 本地音乐管理模块初始化
- **THEN** 加载数据库并提供查询接口

#### Scenario: 传输数据给 Layer3
- **GIVEN** Layer3 请求数据
- **WHEN** 本地音乐管理模块收到请求
- **THEN** 返回可查询的数据库连接或数据集

#### Scenario: 接收歌单并播放
- **GIVEN** Layer3 生成歌单
- **WHEN** 本地音乐管理模块收到歌单数据
- **THEN** 使用 ExoPlayer 播放音乐文件

### Requirement: Layer3 推荐策略分层

系统 SHALL 实现三层推荐策略：

#### Scenario: 模板场景 - SQL 直接匹配
- **GIVEN** Layer2 输出的 scene_type 在预设模板中
- **WHEN** Layer3 执行推荐
- **THEN** 通过 SQL 查询直接匹配音乐
- **AND** 延迟 < 10ms
- **AND** source = 'sql'

#### Scenario: 非模板场景 - LLM 智能选取
- **GIVEN** Layer2 输出的 scene_type 不在预设模板中
- **OR** SQL 查询无结果
- **WHEN** Layer3 执行推荐
- **THEN** 调用大模型基于 index.db 数据智能选取
- **AND** 延迟 3-10s
- **AND** source = 'llm'

#### Scenario: 兜底场景 - 预设固定清单
- **GIVEN** SQL 和 LLM 都无法正常执行
- **WHEN** Layer3 执行推荐
- **THEN** 播放预设的固定音乐清单
- **AND** source = 'fallback'

### Requirement: Layer3 与本地音乐模块接口

系统 SHALL 定义清晰的接口：

```typescript
// 本地音乐管理模块提供给 Layer3 的接口
interface LocalMusicModule {
  // 获取数据库连接/数据集
  getDatabase(): MusicDatabase;
  
  // 执行 SQL 查询
  queryTracks(sql: string, params: any[]): Track[];
  
  // 播放歌单
  playPlaylist(playlist: Track[]): void;
}

// Layer3 提供给本地音乐模块的接口
interface ContentEngine {
  // 生成歌单
  curatePlaylist(
    sceneDescriptor: SceneDescriptor,
    musicDatabase: MusicDatabase
  ): PlaylistResult;
}
```

---

## MODIFIED Requirements

### Requirement: ContentEngine 推荐策略

原有四级策略修改为三层策略：

| 原策略 | 新策略 |
|--------|--------|
| 第一级：本地 SQLite 检索 | 模板场景：SQL 直接匹配 |
| 第二级：预设音乐库 | (合并到 SQL 匹配) |
| 第三级：LLM 生成 | 非模板场景：LLM 智能选取 |
| 第四级：Mock 数据 | 兜底场景：预设固定清单 |

---

## 关键设计决策

### 1. 数据库访问方式

**决策**：Layer3 通过本地音乐管理模块访问 index.db，而非直接访问。

**原因**：
- 解耦：Layer3 不关心数据来源（本地/远程）
- 可测试：本地音乐模块可 Mock
- 安全：数据库访问逻辑集中管理

### 2. LLM 场景的数据传递

**决策**：LLM 场景下，将 index.db 的摘要信息传递给 LLM。

**格式**：
```json
{
  "total_tracks": 1500,
  "genres": ["pop", "rock", "jazz"],
  "sample_tracks": [
    { "title": "晴天", "artist": "周杰伦", "genre": "pop" },
    ...
  ]
}
```

### 3. 兜底清单定义

**决策**：兜底清单为硬编码的 10 首歌曲，覆盖多种场景。

```javascript
const FALLBACK_PLAYLIST = [
  { title: "晴天", artist: "周杰伦", genre: "pop" },
  { title: "夜曲", artist: "周杰伦", genre: "ballad" },
  // ...
];
```

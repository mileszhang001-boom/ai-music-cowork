# Layer 3 LLM 集成优化

## 更新日期
2026-03-01

## 更新概述
优化了 Layer 3（效果生成层）的歌单推荐逻辑，实现了预设场景快速匹配和非预设场景 LLM 智能推荐的混合策略。

## 问题背景
之前当用户输入非预设模板的 JSON 场景时，系统只能使用 mock 数据生成歌单，无法利用 LLM 进行智能推荐。

## 解决方案

### 1. 智能场景判断
系统现在能够自动识别输入的场景是否为预设场景：
- **预设场景**（如 `morning_commute`）：从音乐库快速匹配歌曲
- **非预设场景**（如 `holiday_travel`）：调用 LLM 智能生成歌单

### 2. 降级保护机制
```
预设场景 → 音乐库数据（快速）
    ↓
非预设场景 → LLM 生成（智能）
    ↓ (失败时)
Mock 数据（兜底）
```

## 代码修改

### 修改文件列表

| 文件 | 修改内容 |
|------|----------|
| `src/layers/effects/engines/content/index.js` | 自动检测 API Key、智能场景判断、LLM 调用逻辑 |
| `scripts/debug.js` | 加载 dotenv 环境变量 |
| `.env` | 新增环境变量配置文件 |
| `package.json` | 新增 dotenv 依赖 |

### 核心代码变更

#### 1. ContentEngine 初始化优化
```javascript
// 文件: src/layers/effects/engines/content/index.js

class ContentEngine {
  constructor(config = {}) {
    // ... 其他初始化代码
    
    // 自动检测 API Key 并启用 LLM
    const apiKey = this.config.apiKey || process.env.DASHSCOPE_API_KEY;
    if (apiKey) {
      this.llmClient = new LLMClient({
        apiKey: apiKey,
        model: this.config.model || Models.QWEN_PLUS
      });
      this.config.enableLLM = true;
    }
  }
}
```

#### 2. 歌单生成逻辑优化
```javascript
async curatePlaylist(hints = {}, constraints = {}, sceneType = null) {
  let playlist = [];
  let source = 'mock';

  // 判断是否为预设场景
  const availableScenes = this.getAvailableScenes();
  const isPresetScene = sceneType && availableScenes.includes(sceneType);

  // 预设场景：使用音乐库数据
  if (isPresetScene && this.musicLibrary) {
    const sceneTracks = this.getTracksByScene(sceneType);
    if (sceneTracks.length > 0) {
      playlist = this._filterTracksByHints(sceneTracks, hints, constraints);
      source = 'library';
    }
  }

  // 非预设场景或音乐库无数据：调用 LLM
  if (playlist.length === 0 && this.llmClient && this.config.enableLLM) {
    try {
      playlist = await this._generatePlaylistWithLLM(hints, constraints);
      source = 'llm';
    } catch (error) {
      // LLM 失败，降级到 mock
    }
  }

  // 最终降级：使用 mock 数据
  if (playlist.length === 0) {
    playlist = this._filterTracksByHints(this.trackLibrary, hints, constraints);
    source = 'mock';
  }

  return { playlist, source, ... };
}
```

#### 3. 单例初始化修复
```javascript
// 文件: src/layers/effects/engines/content/index.js

// 修复前
const contentEngine = new ContentEngine();

// 修复后
const contentEngine = new ContentEngine({
  apiKey: process.env.DASHSCOPE_API_KEY
});
```

#### 4. 环境变量加载
```javascript
// 文件: scripts/debug.js

require('dotenv').config();
```

## 环境配置

### 新增 .env 文件
```env
# 阿里云百炼 API 配置
DASHSCOPE_API_KEY=your-api-key-here

# 地域配置
LLM_REGION=beijing

# 默认模型
LLM_MODEL=qwen3.5-plus
```

### 新增依赖
```json
{
  "dependencies": {
    "dotenv": "^17.3.1"
  }
}
```

## 使用说明

### 1. 配置 API Key
```bash
# 方式一：创建 .env 文件
cp .env.example .env
# 编辑 .env 文件，填入真实的 API Key

# 方式二：设置环境变量
export DASHSCOPE_API_KEY="your-api-key-here"
```

### 2. 运行调试工具
```bash
npm run debug
```

### 3. 测试场景
- 选择 Layer 3 或全链路测试
- 选择预设场景：使用音乐库数据（快速）
- 输入自定义 JSON：调用 LLM 智能推荐

## 验证结果

```
✅ 预设场景 (morning_commute)
   - 数据来源: library
   - 示例: Good Morning - Max Frost

✅ 非预设场景 (holiday_travel)
   - 数据来源: llm
   - 示例: Country Roads - John Denver
```

## 预设场景列表

音乐库中包含以下预设场景：
- morning_commute (早晨通勤)
- night_drive (深夜驾驶)
- road_trip (公路旅行)
- romantic_date (浪漫约会)
- family_outing (家庭出行)
- focus_work (专注工作)
- traffic_jam (交通拥堵)
- rainy_night (雨夜驾驶)
- fatigue_alert (疲劳提醒)
- party (派对)
- kids_mode (儿童模式)
- highway_cruise (高速巡航)
- sunset_drive (日落驾驶)
- rainy_day (雨天)
- workout (运动)
- meditation (冥想)
- morning_energy (早晨活力)
- weekend_leisure (周末休闲)
- late_night_solo (深夜独处)
- default (默认)

## 注意事项

1. **API Key 安全**：不要将真实的 API Key 提交到代码仓库
2. **降级机制**：即使 LLM 不可用，系统也能正常工作
3. **性能考虑**：预设场景响应更快，非预设场景需要 LLM 调用时间

## 后续优化方向

1. 增加 LLM 响应缓存，减少重复调用
2. 支持用户反馈学习，优化推荐质量
3. 扩展音乐库预设场景覆盖范围

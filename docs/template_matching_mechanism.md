# 模板匹配机制说明文档

## 概述

本文档详细说明了场景模板匹配机制，包括模板结构、匹配算法、TTS 引导词生成规则以及模板学习机制。

## 1. 模板结构

### 1.1 模板字段说明

```json
{
  "template_id": "TPL_001",
  "scene_type": "morning_commute",
  "name": "早晨通勤",
  "description": "工作日早晨通勤场景，轻松唤醒状态",
  "category": "time",
  "intent": {
    "mood": { "valence": 0.6, "arousal": 0.4 },
    "energy_level": 0.4,
    "atmosphere": "fresh_morning",
    "constraints": { "max_volume_db": -5 }
  },
  "hints": {
    "music": { "genres": ["pop", "indie"], "tempo": "medium", "vocal_style": "any" },
    "lighting": { "color_theme": "cool", "pattern": "breathing", "intensity": 0.4 },
    "audio": { "preset": "vocal_clarity" }
  },
  "triggers": {
    "time_range": ["06:00", "09:00"],
    "weekdays": [1, 2, 3, 4, 5],
    "min_speed": 20
  },
  "priority": 1,
  "announcement_templates": [
    "早上好，为你准备了清新的音乐开启新的一天",
    "早安，今天也要元气满满哦"
  ]
}
```

### 1.2 字段详解

| 字段 | 类型 | 说明 |
|------|------|------|
| `template_id` | string | 模板唯一标识，格式：`TPL_XXX`（预置）、`LEARNED_XXXX`（学习）、`CUSTOM_XXXX`（自定义） |
| `scene_type` | string | 场景类型标识符 |
| `name` | string | 模板显示名称 |
| `description` | string | 模板描述 |
| `category` | string | 分类：`time`、`weather`、`social`、`state`、`special`、`default` |
| `intent` | object | 场景意图，包含 mood、energy_level、atmosphere、constraints |
| `hints` | object | 引擎提示，包含 music、lighting、audio 配置 |
| `triggers` | object | 触发条件，用于匹配判断 |
| `priority` | number | 优先级，数值越小优先级越高（0 最高） |
| `announcement_templates` | array | TTS 播报文案列表，随机选择一条播报 |
| `source` | string | 模板来源：`preset`、`learned`、`custom` |

## 2. 模板匹配算法

### 2.1 匹配流程

```
输入: sceneVector, context
  ↓
遍历所有模板
  ↓
计算每个模板的匹配分数
  ↓
过滤分数 > 0 的模板
  ↓
按 priority 升序、score 降序排序
  ↓
返回最高优先级模板
```

### 2.2 匹配分数计算

匹配分数由触发条件匹配和维度相似度两部分组成：

#### 2.2.1 触发条件匹配

| 触发条件 | 匹配规则 | 分数贡献 |
|----------|----------|----------|
| `fatigue_level.min` | context.fatigueLevel >= min | +50 |
| `time_range` | 当前时间在范围内 | +30 |
| `weather` | context.weather 在列表中 | +20 |
| `min_passengers` | context.passengerCount >= min | +15 |
| `scene_type` | 与 sceneVector.scene_type 匹配 | +40 |

**硬性条件（不满足则分数为 0）**：
- `fatigue_level.min`：疲劳等级必须达到阈值
- `time_range`：时间必须在范围内
- `weekdays`：必须是指定的工作日
- `weather`：天气必须匹配
- `min_speed` / `max_speed`：速度必须在范围内
- `vehicle_speed`：车速必须符合条件
- `min_passengers` / `max_passengers`：乘客数量必须符合
- `has_children`：必须有儿童乘客
- `manual_activation`：必须手动激活

#### 2.2.2 维度相似度计算

```javascript
similarity = 0;
count = 0;

// 能量级别相似度
if (template.intent.energy_level && sceneVector.dimensions.energy) {
  similarity += 1 - Math.abs(template.intent.energy_level - sceneVector.dimensions.energy);
  count++;
}

// 情绪唤醒度相似度
if (template.intent.mood?.arousal && sceneVector.dimensions.energy) {
  similarity += 1 - Math.abs(template.intent.mood.arousal - sceneVector.dimensions.energy);
  count++;
}

// 情绪效价相似度
if (template.intent.mood?.valence && sceneVector.dimensions.social) {
  similarity += 1 - Math.abs(template.intent.mood.valence - sceneVector.dimensions.social) * 0.5;
  count++;
}

finalSimilarity = count > 0 ? similarity / count : 0;
score += finalSimilarity * 20;
```

### 2.3 优先级排序规则

1. **优先级数值**：数值越小，优先级越高
2. **同优先级时**：按匹配分数降序排列
3. **特殊规则**：
   - 疲劳提醒模板（priority = 0）优先级最高
   - 默认模板（priority = 10）作为兜底

## 3. TTS 引导词机制

### 3.1 ACK 与 Announcement 的区别

| 特性 | ACK | Announcement |
|------|-----|--------------|
| 触发时机 | 用户语音 Query 后立即 | 场景执行后 |
| 播报延迟 | < 1 秒 | ACK 播报完成后 |
| 内容类型 | 即时确认 | 场景解释 |
| 示例 | "好的，让我想想..." | "外面在下雨，为你准备了安静的音乐" |

### 3.2 播报协调规则

```
用户语音输入
  ↓
生成 ACK → 加入 ACK 队列
  ↓
播报 ACK（isAckPlaying = true）
  ↓
ACK 播报完成（emit ACK_COMPLETED）
  ↓
isAckPlaying = false
  ↓
处理 Announcement 队列
  ↓
播报 Announcement
```

### 3.3 Announcement 生成

从模板的 `announcement_templates` 字段随机选择一条：

```javascript
const templates = template.announcement_templates || [];
const randomIndex = Math.floor(Math.random() * templates.length);
const text = templates[randomIndex];
```

### 3.4 语音风格选择

根据场景特征自动选择语音风格：

| 条件 | 语音风格 |
|------|----------|
| 社交场景 + 多人 | energetic_female |
| energy_level > 0.7 | energetic_female |
| energy_level < 0.3 或夜间场景 | calm_male |
| 默认 | warm_female |

## 4. 模板学习机制

### 4.1 学习触发条件

当 LLM 生成的 Scene Descriptor 被执行后，系统进入 **30 秒反馈检测窗口**：

- **正面反馈**：用户无任何负面操作 → 学习新模板
- **负面反馈**：用户执行切歌、调音量、暂停等操作 → 不学习，并可能降低相关模板优先级

### 4.2 负面操作列表

```javascript
const negativeActions = [
  'skip',           // 切歌
  'volume_change',  // 调音量
  'pause',          // 暂停
  'dislike',        // 不喜欢
  'switch_scene'    // 切换场景
];
```

### 4.3 模板特征提取

从 Scene Descriptor 提取以下特征：

1. **scene_type**：从 intent.scene_type 或推断得出
2. **intent**：mood、energy_level、atmosphere、constraints
3. **hints**：music、lighting、audio 配置
4. **triggers**：根据上下文推断（时间、天气、乘客数等）
5. **announcement_templates**：从 announcement 字段提取

### 4.4 优先级动态调整

```javascript
basePriority = 5;
boost = Math.min(acceptCount * 0.5, 4);
finalPriority = Math.max(1, basePriority - boost);
```

- 每次正面反馈，优先级提升 0.5
- 最高可提升到 priority = 1
- 连续 3 次负面反馈且正面反馈少于负面反馈时，删除模板

## 5. 模板持久化

### 5.1 模板源

| 来源 | 文件路径 | 说明 |
|------|----------|------|
| preset | `templates/preset_templates.json` | 预置模板，不可删除 |
| learned | `templates/learned_templates.json` | 从 LLM 学习的模板 |
| custom | `templates/custom_templates.json` | 用户手动创建的模板 |

### 5.2 加载顺序

1. 加载 preset 模板
2. 加载 learned 模板
3. 加载 custom 模板
4. 同步 TemplateLearner 中的模板

### 5.3 模板 ID 生成规则

- **预置模板**：`TPL_001` ~ `TPL_050`
- **学习模板**：`LEARNED_` + MD5(scene_type + atmosphere + genres + mood).substring(0, 8)
- **自定义模板**：`CUSTOM_` + Date.now()

## 6. 使用示例

### 6.1 基本匹配

```javascript
const { templateLibrary } = require('./src/core/templateLibrary');

const sceneVector = {
  scene_type: 'morning_commute',
  dimensions: { energy: 0.4, social: 0.3, focus: 0.5 }
};

const context = {
  hour: 7,
  speed: 40,
  passengerCount: 0,
  weather: 'sunny'
};

const matchedTemplate = templateLibrary.matchTemplate(sceneVector, context);
console.log(matchedTemplate.name); // "早晨通勤"
```

### 6.2 模板学习

```javascript
const executionId = 'exec_001';
const sceneDescriptor = {
  intent: {
    scene_type: 'custom_relax',
    mood: { valence: 0.6, arousal: 0.2 },
    energy_level: 0.2,
    atmosphere: 'peaceful'
  },
  hints: {
    music: { genres: ['ambient'], tempo: 'slow' }
  }
};

templateLibrary.recordExecution(executionId, sceneDescriptor, { hour: 22 });

// 30 秒后，如果没有负面反馈，自动学习新模板
```

### 6.3 生成 Announcement

```javascript
const { queryRouter } = require('./src/core/queryRouter');

const announcement = queryRouter.generateAnnouncementFromTemplate(matchedTemplate);
console.log(announcement.text); // "早上好，为你准备了清新的音乐开启新的一天"
```

## 7. 最佳实践

### 7.1 模板设计

1. **触发条件要具体**：避免过于宽泛的触发条件导致误匹配
2. **优先级设置合理**：紧急场景（如疲劳提醒）使用 priority = 0
3. **播报文案多样化**：每个模板至少提供 2-3 条播报文案

### 7.2 学习机制调优

1. **反馈窗口**：默认 30 秒，可根据用户习惯调整
2. **最小接受次数**：默认 2 次，避免单次误操作学习
3. **最大学习模板数**：默认 100，避免模板库膨胀

### 7.3 性能优化

1. **模板预加载**：系统启动时加载所有模板到内存
2. **索引优化**：按 scene_type 和 source 建立索引
3. **定期清理**：删除长期未使用的学习模板

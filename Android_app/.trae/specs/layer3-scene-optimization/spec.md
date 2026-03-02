# Layer3 场景匹配优化 Spec

## Why

当前 Layer3 场景匹配存在两个核心问题：
1. **中文内容过多且匹配度差** - 中文歌曲占 53.5%，但编排策略强制 70% 中文，导致低质量匹配
2. **场景内容不合适** - 如「疲劳提醒」匹配到「儿童歌曲」，「雨天emo」匹配不到合适的悲伤歌曲

## What Changes

### 1. ContentEngine 编排逻辑优化
- 移除强制 70% 中文比例限制
- 基于场景需求动态调整中英文比例
- 增加场景类型过滤（如儿童模式排除成人内容）

### 2. 歌曲特征优化
- 增加中文歌曲的低能量选项（当前仅 6.9%）
- 增加中文歌曲的高能量选项（当前仅 13%）
- 为歌曲添加场景标签（scene_tags）

### 3. 场景模板优化
- 疲劳提醒：排除儿童歌曲，只匹配高能量摇滚/电子
- 雨天emo：优先匹配悲伤/低能量歌曲
- 儿童模式：只匹配儿童友好内容

## Impact

- Affected code: 
  - `ContentEngine.kt` - 编排逻辑
  - `index.json` - 歌曲特征
  - `scene_templates.json` - 场景模板

## 问题诊断

### 数据分析结果

| 指标 | 中文歌曲 | 问题 |
|------|----------|------|
| Energy 低(0-0.4) | 6.9% | ❌ 太少，无法满足低能量场景 |
| Energy 高(0.7-1.0) | 13% | ⚠️ 不足，疲劳提醒场景匹配困难 |
| Valence 消极(0-0.4) | 24.9% | ✅ 足够 |
| BPM 慢速(<90) | 0.2% | ❌ 极度缺乏 |
| BPM 极快(140+) | 0% | ❌ 缺乏 |

### 当前编排问题

```kotlin
// 问题代码：强制 70% 中文
val chineseRatio = 0.7  // 固定比例，不考虑场景需求
```

### 场景匹配问题示例

| 场景 | 问题 | 原因 |
|------|------|------|
| 疲劳提醒 | 匹配儿童歌曲 | 缺乏场景类型过滤 |
| 雨天emo | 中文占比过高 | 强制 70% 中文比例 |
| 浪漫约会 | 匹配不相关歌曲 | valence/energy 匹配权重不足 |

## ADDED Requirements

### Requirement: 动态中英文比例
系统 SHALL 根据场景类型动态调整中英文歌曲比例，而非固定 70%。

#### Scenario: 疲劳提醒场景
- **WHEN** 场景为「疲劳提醒」
- **THEN** 中文比例应为 30-50%，优先高能量英文摇滚/电子

#### Scenario: 儿童模式场景
- **WHEN** 场景为「儿童模式」
- **THEN** 中文比例应为 80-100%，且必须过滤成人内容

### Requirement: 场景类型过滤
系统 SHALL 根据场景类型过滤不合适的歌曲。

#### Scenario: 疲劳提醒排除儿童歌曲
- **WHEN** 场景为「疲劳提醒」
- **THEN** 排除 genre 为 children 的歌曲

#### Scenario: 儿童模式内容过滤
- **WHEN** 场景为「儿童模式」或「家庭出行」
- **THEN** 只匹配儿童友好内容，排除 explicit 内容

### Requirement: 场景评分达标
系统 SHALL 确保每个预置场景的匹配评分达到 80 分。

#### Scenario: 场景评分
- **WHEN** 生成播放列表
- **THEN** 每个场景的匹配分 ≥ 80，中文占比 40-70%，Valence 偏差 < 0.15

## MODIFIED Requirements

### Requirement: ContentEngine 编排算法
修改编排算法，支持动态比例和场景过滤：

```kotlin
// 动态比例：基于场景类型
val chineseRatio = when (scene.scene_type) {
    "fatigue_alert" -> 0.4  // 疲劳提醒：更多英文高能量
    "kids_mode" -> 0.9      // 儿童模式：更多中文
    "romantic_date" -> 0.6  // 浪漫约会：平衡
    else -> 0.6             // 默认：60%
}

// 场景过滤
val excludedGenres = when (scene.scene_type) {
    "fatigue_alert" -> listOf("children", "disney")
    "kids_mode" -> listOf("explicit")
    else -> emptyList()
}
```

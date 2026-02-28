# Enhance Template Library Spec

## Why
当前模板库存在两个关键问题：
1. 模板数量有限（仅 20 个预置模板），无法覆盖用户个性化需求，且缺乏从 LLM 生成结果中学习的能力
2. 模板匹配逻辑与 TTS 引导词（announcement）的关系不明确，缺乏清晰的机制说明文档

## What Changes
- **新增模板学习机制**：支持从 LLM 生成的 Scene Descriptor 中提取模板，自动学习并记忆用户常用场景
- **扩展预置模板库**：将预置模板从 20 个扩展到 50 个，覆盖更多典型场景
- **明确 TTS 引导词机制**：清晰定义 announcement 与模板的关系，以及 ACK 与 announcement 的配合规则
- **新增模板持久化**：支持用户自定义模板的保存和加载

## Impact
- Affected specs: `spec-all.md` 第 6.1 节、`templates/scene_templates.json`
- Affected code: `src/core/templateLibrary/index.js`、`src/core/memory/`（新增模板记忆模块）

## ADDED Requirements

### Requirement: Template Learning from LLM
系统应当支持从 LLM 生成的 Scene Descriptor 中自动学习新模板：
- 当 LLM 生成的 Scene Descriptor 被用户接受（无负面反馈）时，系统应提取关键特征并生成新模板
- 学习的模板应包含：scene_type、intent、hints、典型触发条件
- 用户常用模板应获得更高的匹配优先级

#### Scenario: LLM Template Learning
- **WHEN** LLM 生成的 Scene Descriptor 被执行且用户未在 30 秒内进行负面操作（切歌、调音量等）
- **THEN** 系统提取该 Scene Descriptor 的特征，生成或更新用户自定义模板

### Requirement: Template Persistence
系统应当支持模板的持久化存储：
- 预置模板存储在 `templates/preset_templates.json`
- 用户学习模板存储在 `templates/learned_templates.json`
- 用户手动创建的模板存储在 `templates/custom_templates.json`
- 系统启动时自动加载所有模板源

### Requirement: TTS Announcement Mechanism
系统应当明确 TTS 引导词的生成和播报规则：
- **ACK**：用户语音 Query 后 < 1 秒内播报，内容为即时确认（如"好的，让我想想"）
- **Announcement**：场景切换时播报，内容为场景解释（如"外面在下雨，为你准备了安静的音乐"）
- **配合规则**：ACK 播报完成后才允许 Announcement 排队播报

#### Scenario: ACK and Announcement Coordination
- **WHEN** 用户发起语音 Query 触发场景切换
- **THEN** 系统先播报 ACK（< 1 秒），场景执行后播报 Announcement（解释新场景）

### Requirement: Template Matching Documentation
系统应当提供清晰的模板匹配机制文档：
- 匹配算法说明（触发条件优先级、维度相似度计算）
- TTS 引导词生成规则
- 模板优先级排序规则

## MODIFIED Requirements

### Requirement: Extended Preset Templates
预置模板数量应从 20 个扩展到 50 个，覆盖以下场景类别：
- 时间相关：早晨、上午、中午、下午、傍晚、深夜、凌晨
- 天气相关：晴天、雨天、雪天、雾天、暴风
- 社交相关：独自、情侣、家庭、朋友、多人派对
- 状态相关：疲劳、专注、放松、兴奋、通勤
- 特殊场景：节假日、纪念日、长途旅行

## REMOVED Requirements
无移除项。

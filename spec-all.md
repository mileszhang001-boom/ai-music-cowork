# 车载座舱 AI 娱乐融合方案 — 技术规范文档 V2

## 目录
- [1. 项目概述](#1-项目概述)
- [2. 产品定义](#2-产品定义)
- [3. 信号可用性 — Layer 1 输出结构](#3-信号可用性--layer-1-输出结构)
- [4. 技术架构](#4-技术架构)
- [5. 核心体验设计](#5-核心体验设计)
- [6. 数据结构设计](#6-数据结构设计)
- [7. 记忆与成长系统](#7-记忆与成长系统)
- [8. 质量评估体系 — 三层闭环](#8-质量评估体系--三层闭环)
- [9. 端到端场景走查](#9-端到端场景走查)
- [10. 实现计划](#10-实现计划)
- [11. 团队结构](#11-团队结构)
- [12. 风险管理](#12-风险管理)
- [13. KPI 体系](#13-kpi-体系)
- [14. 附录](#14-附录)

---

## 修订记录

| 版本 | 日期 | 修订内容 |
|------|------|----------|
| V1 | 2026-02-27 | 初版，基础架构与功能需求 |
| V2 | 2026-02-27 | 新增产品定义、核心体验设计、记忆系统、质量评估闭环、端到端场景走查；深化技术架构与实现计划 |
| V2.1 | 2026-02-27 | 重构 Scene Descriptor 为 intent/hints 架构；新增信号可用性分析与降级策略；明确执行层自主决策权 |
| V2.2 | 2026-02-28 | 新增用户 Query 即时应答（ACK）机制；补全端到端场景走查、实现计划、团队结构、风险管理、KPI 指标 |

---

## 1. 项目概述

### 1.1 项目背景

随着智能座舱技术的发展，用户对车载娱乐体验的需求已从"能听歌"升级为"懂我此刻需要什么"。传统的音乐推荐系统基于历史偏好做静态推荐，无法感知用户所处的物理环境、情绪状态和社交场景。本项目旨在通过 AI 大模型的强泛化、推理能力，结合车内娱乐能力（内容、音效、氛围灯），打造让用户有惊喜感的全场景体验。

### 1.2 核心理念

> **车不再只是交通工具，而是一个移动的、有感知的、会讲故事的空间。关键是把"AI 能力"转化为"用户能感知到的体验质量"，而不是停留在技术 demo 层面。**

### 1.3 项目目标

- 构建一个基于多模态输入的智能座舱娱乐系统
- 实现场景感知与理解，提供个性化的声光电一体化体验
- 保证系统响应速度，实现上车 2 秒内即有氛围响应
- 建立一个可扩展、可维护、可度量的系统架构

### 1.4 核心价值

| 维度 | 价值 |
|------|------|
| 用户体验 | 从"AI 帮我选了几首歌"升级为"AI 为我编排了一段旅程体验" |
| 品牌竞争力 | 打造差异化的智能座舱功能，提升产品溢价能力 |
| 技术创新 | 探索 AI 大模型在车载场景下的多模态融合应用 |
| 商业价值 | 通过高级场景包/主题包实现增值变现 |

---

## 2. 产品定义

### 2.1 用户体验流程

1. **上车** → 检测多模态信息 → 分析此刻状态（深夜、下雨、心情不好、驾驶员一个人） → 从曲库中选取适合的歌曲，形成歌单 → 匹配相应的播放效果（音效、氛围灯效果）
2. **信息发生变化**（如儿童上车、多人聊天、天气转晴） → 多模态信息变化 → 形成新的此刻状态（儿童、合家欢） → 更新歌单 → 更新播放效果

### 2.2 信号可用性分析

#### 2.2.1 已确认可用的信号源

| 信号源 | 可获取的原始数据 | 可直接提取的信息 |
|--------|----------------|-----------------|
| 车外摄像头 | 车外实时画面 | 道路类型（高速/城市/乡村）、光照条件、大致天气（晴/阴/雨/雪） |
| 天气接口 | 结构化天气数据 | 精确天气、温度、湿度、日出日落时间 |
| 时间接口 | 系统时间 | 时段（凌晨/早晨/上午/下午/傍晚/夜晚）、工作日/周末、节假日 |
| 行程信息 | 导航目的地、预计时间 | 出行目的（可推断）、行程时长、行程进度、预计到达时间 |
| 车内 OMS | 车内摄像头画面 | 乘客数量、乘客位置（主驾/副驾/后排）、大致年龄段（成人/儿童）、面部表情 |
| 车内 Mic | 车内音频流 | 车内声音状态（静默/说话/打电话/笑声/哭声/唱歌） |

#### 2.2.2 需要推断的信息及策略

以下信息无法直接从硬件获取，需要通过已有信号推断或采用降级策略：

| 需要的信息 | 推断方式 | 置信度 | 降级策略 |
|-----------|---------|--------|---------|
| **驾驶员情绪** | OMS 面部表情分析 + Mic 语音语调分析 + 综合推断 | 中（0.5-0.7） | 置信度 < 0.4 时忽略情绪维度，仅用环境+行程信息做场景识别 |
| **出行目的** | 导航目的地类型（公司=通勤、商场=购物、景区=出游）+ 时间规律（工作日早晚=通勤）+ 历史记忆 | 中-高（0.6-0.9） | 无法推断时标记为 `unknown`，使用通用场景模板 |
| **车速类别** | 导航数据中的道路类型 + 车外摄像头场景识别（高速/城市） | 中（0.5-0.7） | 无法获取时默认 `cruising`，不做速度相关的安全约束调整 |
| **乘客关系** | OMS 年龄段 + 座位分布 + 历史记忆（常见组合） | 低-中（0.3-0.6） | 无法推断时仅用"成人数+儿童数"做基础判断 |
| **疲劳状态** | OMS 眼睛状态（闭眼频率、揉眼动作）+ 时间（深夜/凌晨）+ 行程时长 | 中（0.5-0.7） | 置信度低时不主动切换提神模式，但深夜+长途时默认提高警觉 |
| **车窗状态** | 无法直接获取 | 不可用 | 从设计中移除此字段，不影响核心体验 |

#### 2.2.3 信号融合置信度模型

核心原则：**宁可少判断，不可误判断。低置信度的信号降权处理，而非忽略。**

```
最终场景置信度 = Σ(各信号置信度 × 信号权重)

权重分配：
  天气接口（API 数据，高可靠）     → 权重 1.0
  时间接口（系统时间，完全可靠）    → 权重 1.0
  行程信息（导航数据，高可靠）      → 权重 0.9
  车外摄像头（视觉识别，中可靠）    → 权重 0.7
  车内 OMS（人脸/人体识别，中可靠） → 权重 0.6
  车内 Mic（音频分析，中可靠）      → 权重 0.5
  推断信息（情绪/目的，低可靠）     → 权重 0.3
```

当最终场景置信度 < 0.5 时，系统回退到基于"时间+天气"的安全模板，不做复杂推理。

### 2.3 关键体验点

| 编号 | 体验点 | 说明 |
|------|--------|------|
| E1 | 响应速度 | 上车 2 秒内即有氛围响应，不能让用户等 |
| E2 | 惊喜感 | 基于多模态信息，大模型分析并生成新的场景响应 |
| E3 | 声光电配套 | 歌单、播放效果、氛围灯高度匹配，形成一体化体验 |
| E4 | 自然语言交互 | 支持用户自然语言修改（如"我不喜欢 xxx 的音乐"） |
| E5 | 可解释性 | 在适当时机提供轻量解释，消除"黑箱感" |
| E6 | 优雅过渡 | 场景切换的体验质量 ≥ 场景本身的体验质量 |
| E7 | 知道何时不打扰 | "知道什么时候不打扰"是高级智能的体现 |
| E8 | 即时语音确认 | 用户说话后 < 1 秒内收到语音确认，消除"没听到"的焦虑 |

### 2.4 核心用户故事

| ID | 角色 | 故事 | 验收标准 |
|----|------|------|----------|
| US-01 | 独行通勤者 | 深夜下雨独自开车回家，上车后系统自动播放安静的音乐，配上暖色灯光，并告诉我"外面在下雨，为你准备了一些安静的音乐" | 上车 2 秒内灯光变暖、音乐响起；语音解释自然 |
| US-02 | 家庭出行者 | 带小孩出门，孩子上车后系统自动切换到合家欢模式，过滤不适宜内容 | 检测到儿童后 3 秒内完成模式切换；content_rating 为 G |
| US-03 | 偏好表达者 | 说"我不喜欢听流行乐"，系统立即确认"好的，马上调整"，然后从歌单中移除流行乐，并记住这个偏好 | **语音确认 < 1 秒**；约束生效 < 3 秒；下次上车仍记住 |
| US-04 | 长途旅行者 | 3 小时长途驾驶，系统根据行程进度编排情绪曲线，到达前音乐逐渐收束 | 歌单有明确的情绪弧线；到达前 10 分钟进入收束阶段 |
| US-05 | 疲劳驾驶者 | 驾驶员频繁揉眼打哈欠，系统自动切换到提神模式 | 检测到疲劳信号后切换到高能量音乐和明亮灯光 |
| US-06 | 创意请求者 | 说"给我一个海边度假的感觉"，系统先说"好主意，让我想想怎么编排"，几秒后切换到全新的海边氛围 | **语音确认 < 1 秒**；场景切换 < 15 秒；announcement 解释新场景 |

### 2.5 体验设计原则

| 原则 | 说明 |
|------|------|
| 先快后优 | 模板秒级响应，大模型后台优化，用户感知到的是"越来越懂我" |
| 叙事性编排 | 一段旅程是一个故事弧线，不是随机歌曲列表 |
| 优雅过渡 | 场景切换永远不能是"突然切歌 + 灯光突变" |
| 用户指令最高优先 | 任何时候用户的明确指令都覆盖 AI 的推理结果 |
| 知道何时沉默 | 不是所有时刻都需要音乐和灯效 |
| 越用越懂你 | 个性化飞轮：用得越多 → 越精准 → 越离不开 |
| 信号不足时保守 | 宁可用安全模板，不可基于低置信度信号做激进推理 |
| 说话必有回应 | 用户主动说话后，系统必须在 1 秒内给出语音确认 |

---

## 3. 信号可用性 — Layer 1 输出结构

### 3.1 信号推断置信度体系

每个信号都附带 `confidence` 和 `source` 标记，下游根据置信度决定权重：

| 置信度等级 | 数值范围 | 含义 | 下游处理 |
|-----------|---------|------|---------|
| high | 0.8-1.0 | 硬件直接获取，数据可靠 | 正常权重 |
| medium | 0.5-0.8 | 推断得出，有一定可信度 | 降权使用 |
| low | 0.2-0.5 | 推断不确定，仅作参考 | 大幅降权，不作为关键决策依据 |
| unavailable | 0 | 无法获取也无法推断 | 使用默认值，该维度不参与场景分类 |

### 3.2 Layer 1 输出结构

基于实际可用信号，Layer 1 的输出结构如下：

```json
{
  "environment": {
    "time_of_day": "night",
    "weather": "rainy",
    "light_level": 0.2,
    "outside_scene": "highway",
    "season_context": "winter",
    "_source": {
      "time_of_day": {"from": "system_clock", "confidence": 1.0},
      "weather": {"from": "weather_api", "confidence": 0.95},
      "light_level": {"from": "exterior_camera", "confidence": 0.85},
      "outside_scene": {"from": "exterior_camera", "confidence": 0.8},
      "season_context": {"from": "time+weather_inferred", "confidence": 0.9}
    }
  },
  "passengers": {
    "count": 2,
    "composition": ["adult_driver", "child_rear"],
    "mood_signals": {
      "driver": {"valence": 0.5, "arousal": 0.3, "confidence": 0.4}
    },
    "cabin_audio_state": "talking",
    "_source": {
      "count": {"from": "oms", "confidence": 0.9},
      "composition": {"from": "oms", "confidence": 0.85},
      "mood_signals": {"from": "oms_face+mic_tone", "confidence": 0.4},
      "cabin_audio_state": {"from": "mic", "confidence": 0.8}
    }
  },
  "trip": {
    "trip_purpose": "commute",
    "trip_progress": 0.3,
    "estimated_remaining_min": 25,
    "_source": {
      "trip_purpose": {"from": "nav+time_pattern_inferred", "confidence": 0.6},
      "trip_progress": {"from": "navigation", "confidence": 0.95},
      "estimated_remaining_min": {"from": "navigation", "confidence": 0.9}
    }
  },
  "driver_state": {
    "fatigue_level": 0.2,
    "attention_level": 0.8,
    "_source": {
      "fatigue_level": {"from": "oms_face", "confidence": 0.7},
      "attention_level": {"from": "oms_face", "confidence": 0.7}
    }
  }
}
```

**关键设计决策**：
- 每个字段都附带 `_source` 元数据，标明数据来源和置信度
- Layer 2 在做场景分类时，根据 confidence 对各维度加权
- 低置信度的信号不会主导场景判断，避免"猜错了还强行执行"

### 3.3 信号不可用时的降级链

```
完整信号 → 推断信号（降权） → 默认值（最小化影响） → 安全模板（兜底）

示例：情绪信号降级链
OMS 面部表情（confidence: 0.6）
  → 如果 OMS 不可用：MIC 语音情绪（confidence: 0.4）
    → 如果 MIC 也不可用：默认 neutral（confidence: 0）
      → Layer 2 忽略情绪维度，仅基于环境+行程做场景分类
```

## 4. 技术架构

### 4.1 整体架构

```
┌──────────────────────┐          ┌──────────────────────────────────┐
│                      │          │                                  │
│   场景语义层系统       │          │         执行层系统                 │
│   (Team A)           │          │         (Team B/C/D)             │
│                      │          │                                  │
│  多模态感知           │          │  ┌──────────┐                    │
│  场景识别             │  Scene   │  │ 内容引擎   │  Team B           │
│  大模型推理           │Descriptor│  │ (歌单生成) │                    │
│  用户指令处理    ─────────────→  │  ├──────────┤                    │
│  记忆系统             │  (JSON)  │  │ 灯光引擎   │  Team C           │
│                      │          │  │ (氛围灯控制)│                    │
│                      │    ←─────────│ 音效引擎   │  Team D           │
│                      │ Feedback │  │ (空间音频) │                    │
│                      │  (JSON)  │  └──────────┘                    │
│                      │          │       │                          │
│                      │          │  ┌────┴─────┐                    │
│                      │          │  │ 编排协调器  │  统一调度           │
│                      │          │  └──────────┘                    │
└──────────────────────┘          └──────────────────────────────────┘
```

**核心设计决策：引入「场景语义层」— 不让大模型直接控制硬件**

在多模态输入和硬件执行之间加入一层结构化的中间表达（Scene Descriptor JSON），实现：

| 收益 | 说明 |
|------|------|
| 解耦 | 大模型只负责"理解和决策"，不需要知道灯有几个区域、音效有哪些参数 |
| 可调试 | 场景语义是可读的 JSON，产品经理可以直接审核和调优 |
| 可缓存 | 相似场景可以复用，大幅提升响应速度 |
| 可回退 | 用户说"不喜欢"时，可以在语义层微调，而不是重新跑一遍大模型 |
| 可扩展 | 新增引擎（如触觉反馈）只需在 JSON 中加字段，其他引擎无感 |

**语义层与执行层的职责边界**：

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  语义层的职责（导演）：                                    │
│  ✅ 理解场景 → 输出情绪、能量、氛围                        │
│  ✅ 理解用户 → 输出约束、偏好覆盖                          │
│  ✅ 编排叙事 → 输出 energy_curve、过渡策略                 │
│  ✅ 提供建议 → hints（可选，仅供参考）                     │
│  ❌ 不指定具体的 tempo_range、RGB 值、DSP 参数             │
│                                                         │
│  执行层的职责（专业演员）：                                 │
│  ✅ 基于 intent 自主决策专业参数                           │
│  ✅ 参考 hints，但有权忽略                                │
│  ✅ 遵守 constraints 和 user_overrides（硬约束）           │
│  ✅ 通过 Feedback Report 告诉语义层"我做了什么"            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**为什么这样设计**：语义层（LLM）不是音乐专家、灯光设计师或音效工程师。如果让它直接指定 `tempo_range: [80, 110]` 或 `rhythm: "gentle_pulse"`，就是"外行指导内行"。正确的做法是：语义层说"这个场景应该是温馨、安静、低能量的"，内容引擎用自己的专业知识库决定什么音乐最合适，灯光引擎用自己的美学映射表决定什么颜色和动效最合适。

### 4.2 双速架构

响应速度是第一关键体验点，采用快慢双通道：

| 通道 | 触发时机 | 响应时间 | 实现方式 |
|------|---------|---------|---------|
| **快通道** | 上车/场景突变 | < 2 秒 | 本地轻量模型 + 预计算场景模板库（50-100 个典型场景） |
| **慢通道** | 快通道启动后 | 5-15 秒 | 云端大模型精细化推理，生成个性化方案 |

**用户体验**：上车瞬间就有氛围响应（快通道），几秒后体验悄然升级变得更精准（慢通道结果渐变替换），用户感知到的是"越来越懂我"，而不是"等了好久才出来"。

### 4.3 场景语义层 — 三层处理管线

```
┌─────────────────────────────────────────────────────────┐
│                    Layer 3: 场景推理层                     │
│            (大模型 / 云端，5-15秒，按需触发)                  │
│  输入：场景特征向量 + 用户画像 + 特殊 Query                   │
│  输出：Scene Descriptor（intent + hints）                  │
├─────────────────────────────────────────────────────────┤
│                    Layer 2: 场景识别层                     │
│           (本地轻量模型，200ms-1秒，持续运行)                  │
│  输入：多模态信号（含 confidence）→ 场景特征向量               │
│  输出：场景标签 + 变化事件 + 是否需要触发 Layer 3              │
├─────────────────────────────────────────────────────────┤
│                    Layer 1: 信号预处理层                    │
│              (规则引擎，<100ms，实时运行)                     │
│  输入：原始传感器数据（6 个确认信号源）                        │
│  输出：标准化信号 + 推断信号 + 信号质量评估（confidence）       │
└─────────────────────────────────────────────────────────┘
```

#### 4.3.1 Layer 1：信号预处理层（实时，< 100ms）

不做任何"理解"，只做信号标准化、推断补全和异常过滤。

**信号处理规则**：

| 目标字段 | 主信号源 | 推断策略 | 降级默认值 |
|---------|---------|---------|-----------|
| `time_of_day` | 时间接口 | 直接获取 | — |
| `weather` | 天气接口 | 无网时用车外摄像头推断 | `unknown` |
| `light_level` | 车外摄像头 | 时间+天气联合推断 | 按时间默认值 |
| `outside_scene` | 车外摄像头 | 行程信息（高速/城市）辅助 | `unknown` |
| `passenger_count` | OMS | — | `1`（仅驾驶员） |
| `passenger_composition` | OMS | — | `["adult_driver"]` |
| `mood_signals` | OMS 面部 + MIC 语调 | 两路信号融合 | `neutral, confidence: 0` |
| `cabin_audio_state` | MIC | — | `unknown` |
| `trip_purpose` | 行程信息 + 时间规律 + 历史记忆 | 多信号联合推断 | `unknown` |
| `trip_progress` | 导航系统 | — | `unknown`（未设导航时） |
| `fatigue_level` | OMS 面部 | — | `unknown` |
| `speed_category` | 行程信息 + 车外摄像头 | 道路类型推断 | `unknown` |

**输出结构**：见第 3.2 节。

#### 4.3.2 Layer 2：场景识别层（本地，200ms-1 秒）

做两件事：**场景分类** 和 **变化检测**。

**场景特征向量**（基于 confidence 加权计算）：

```json
{
  "scene_vector": {
    "energy_level": 0.25,
    "social_level": 0.1,
    "mood_valence": 0.3,
    "mood_arousal": 0.2,
    "safety_attention": 0.6,
    "child_friendly": 0.0,
    "intimacy_level": 0.7
  },
  "scene_label": "solo_night_commute_rainy",
  "scene_cluster": "melancholic_calm",
  "matched_template_id": "TPL_042",
  "template_similarity": 0.87,
  "vector_confidence": 0.75
}
```

`vector_confidence` 是综合置信度，反映输入信号的整体可靠程度。当 `vector_confidence` 较低时，Layer 2 倾向于使用更通用的场景簇，避免过度推断。

**场景簇（Scene Cluster）**：预定义的 50-100 个典型场景原型，例如：
- `energetic_group_outing`（活力合家出游）
- `melancholic_calm`（忧郁平静）
- `focused_commute`（专注通勤）
- `family_joy`（家庭欢乐）
- `late_night_solo`（深夜独行）

每个场景簇预先绑定一套默认的 Scene Descriptor，这是"快通道"的基础。

**变化检测输出**：

```json
{
  "change_detected": true,
  "change_type": "event",
  "change_magnitude": 0.35,
  "changed_dimensions": ["social_level", "child_friendly"],
  "trigger_layer3": true,
  "trigger_reason": "passenger_composition_changed"
}
```

**触发 Layer 3 的条件**（不是每次变化都触发）：
- 场景簇发生切换（如从 `solo_commute` 变为 `family_joy`）
- 变化幅度超过阈值（magnitude > 0.4）
- 用户发出了 Query
- 模板匹配度过低（similarity < 0.6，说明是罕见场景）

#### 4.3.3 Layer 3：场景推理层（云端大模型，按需触发）

只有在 Layer 2 判断"需要"时才触发，输入是精炼过的结构化信息。

**Prompt 结构**：

```
[系统角色] 你是车载场景编排师。你的职责是理解当前场景，输出场景意图描述。
注意：你只需要描述"这个场景应该是什么氛围"，不需要指定具体的音乐参数或灯光参数——
那是各专业引擎的工作。你可以在 hints 中提供建议，但引擎有权忽略。

[用户画像]（来自记忆系统）
- 音乐偏好：爵士、独立民谣，不喜欢流行乐
- 通勤习惯：偏好安静氛围
- 灯光偏好：暖色调

[当前场景特征]
- 场景向量：{energy: 0.25, social: 0.1, mood: 0.3, ...}
- 场景标签：solo_night_commute_rainy
- 信号置信度：vector_confidence: 0.75
- 低置信度维度：mood（0.4）— 情绪判断不确定，请勿过度依赖

[当前播放状态]
- 正在播放：Norah Jones - Don't Know Why
- 歌单进度：第 3/12 首

[变化事件]（如果有）
- 事件：后排检测到儿童上车
- 变化维度：social_level 0.1→0.6, child_friendly 0→1.0

[用户指令]（如果有）
- "我不喜欢听流行乐"

[输出要求] 生成 Scene Descriptor JSON（intent + hints 格式）...
```

#### 4.3.4 快速响应关键机制

**模板预热 + 增量更新**：

```
                    ┌──────────────┐
                    │  预计算模板库   │  50-100个场景模板
                    │  (本地缓存)    │  每个模板 = 完整的 Scene Descriptor
                    └──────┬───────┘
                           │
  场景变化 ──→ Layer 2 匹配最近模板 ──→ 直接使用（< 1秒）
                           │
                           ├──→ 同时触发 Layer 3 精细化推理
                           │
                    Layer 3 返回 ──→ 增量 Diff 更新（渐变过渡）
```

增量更新是关键：Layer 3 返回后，不是整体替换，而是计算与当前模板的 Diff，只渐变更新差异部分。

**用户 Query 的快慢分流**：

| Query 类型 | 示例 | ACK 应答 | 执行路径 | 响应时间 |
|-----------|------|---------|---------|---------|
| 约束型 | "不要 xxx"、"声音小点" | "好的，马上调整" | 规则引擎直接修改 Descriptor 字段 | ACK < 1s，生效 < 1s |
| 偏好型 | "来点爵士"、"换个心情" | "收到，正在为你切换" | 快路径修改 + 慢路径优化 | ACK < 1s，初步生效 < 1s，优化 5-15s |
| 创意型 | "给我一个海边度假的感觉" | "好主意，让我想想怎么编排" | 必须走 Layer 3 | ACK < 1s，场景切换 5-15s |

**防抖机制**：

```
信号变化 ──→ 防抖窗口（5秒）──→ 确认变化稳定 ──→ 触发场景更新
                  │
                  └─→ 短暂波动（如路过隧道）──→ 忽略
```

#### 4.3.5 用户 Query 即时应答（ACK）机制

**问题**：当用户主动说话触发 Layer 3 推理时（如创意型 Query），需要等待 5-15 秒才能收到 Scene Descriptor 中的 announcement。这段沉默会让用户觉得"系统没听到我"。

**解决方案**：在 Query Router 阶段（Layer 2 之前）增加一条独立的 ACK 旁路通道，不等 Layer 3，直接通过 TTS 给用户即时确认。

```
用户说话
    │
    ▼
┌─ Query Router（< 500ms）──────────────────────────────┐
│                                                        │
│  1. ASR 语音识别                                        │
│  2. 意图分类（约束/偏好/创意/情绪）                       │
│  3. 生成 ACK 文本 ──→ 直接发送 TTS 播报 ──→ 用户听到确认  │
│  4. 分流到快路径 / 慢路径                                │
│                                                        │
└────────────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
    快路径（< 1s）        慢路径（Layer 3, 5-15s）
    规则引擎修改           云端推理
    当前 Descriptor        生成新 Descriptor
         │                    │
         ▼                    ▼
    立即执行              Descriptor 到达后执行
                          （可能含 announcement 解释新场景）
```

**ACK 与 Announcement 的职责分离**：

| | ACK（即时确认） | Announcement（场景解释） |
|---|---|---|
| **目的** | 让用户知道"系统听到了" | 解释新场景的内容和原因 |
| **时效** | < 1 秒 | 跟随 Scene Descriptor，可以等 |
| **生成方** | Query Router（规则/模板） | Layer 3（LLM 生成） |
| **通道** | 独立旁路，直接到 TTS | Scene Descriptor → 编排协调器 → TTS |
| **内容** | 简短确认（"好的，马上调整"） | 丰富解释（"为你切换到海边度假模式，选了轻松的 Bossa Nova"） |
| **适用场景** | 仅用户主动 Query（MVP） | 所有场景切换（事件触发 + Query 触发） |

**ACK 消息结构**（独立通道，不经过 Scene Descriptor）：

```json
{
  "type": "ack",
  "text": "好主意，让我想想怎么编排",
  "voice_style": "warm_female",
  "timestamp": "2026-02-27T22:30:01+08:00",
  "query_intent": "creative",
  "related_query": "给我一个海边度假的感觉"
}
```

**ACK 模板库**（MVP 阶段使用规则模板，无需 LLM）：

| 意图类别 | ACK 模板（随机选一） |
|---------|-------------------|
| 约束型 | "好的，马上调整" / "收到，正在处理" |
| 偏好型 | "收到，正在为你切换" / "好的，换一种风格" |
| 创意型 | "好主意，让我想想怎么编排" / "有意思，给我几秒钟" |
| 情绪型 | "我听到了，为你调整氛围" / "好的，让我来帮你" |

**完整的用户体验时间线**（以创意型 Query 为例）：

```
T+0.0s  用户："给我一个海边度假的感觉"
T+0.3s  ASR 识别完成
T+0.5s  Query Router 分类为"创意型"，生成 ACK
T+0.8s  TTS 播报："好主意，让我想想怎么编排"     ← 用户感知到被响应
T+1.0s  Layer 3 开始推理（后台）
T+8.0s  Layer 3 返回 Scene Descriptor
T+8.2s  编排协调器收到 Descriptor
T+8.5s  TTS 播报 announcement："为你切换到海边度假模式，选了一些轻松的 Bossa Nova"
T+9.0s  音乐开始渐变、灯光开始过渡、音效切换
```

**设计决策：MVP 阶段 ACK 仅用于用户主动 Query 场景**

被动事件（如儿童上车、天气变化）不使用 ACK，原因：
- 用户没有主动发起交互，没有"等待回应"的心理预期
- 被动事件的 5-15 秒延迟用户不会感知到
- 减少 MVP 的实现复杂度

未来可扩展：被动事件也加入视觉微反馈（如灯光做一个微妙的"收到"动效）。

### 4.4 执行层架构

#### 4.4.1 编排协调器（Orchestrator）

执行层内部的核心组件，负责接收 Scene Descriptor，分发给各引擎，协调执行顺序：

```
Scene Descriptor 到达
        │
        ▼
┌─ 编排协调器 ─────────────────────────────────┐
│                                              │
│  1. 解析 JSON，校验 version 和字段完整性        │
│  2. 读取 intent.transition 确定过渡策略         │
│  3. 按时序编排各引擎的执行：                     │
│                                              │
│     ┌─ 如果有 announcement ──────────────┐    │
│     │                                    │    │
│     │  检查 ACK 是否已播报过：              │    │
│     │  - 如果是 Query 触发且 ACK 已播报     │    │
│     │    → announcement 在引擎执行开始后播报 │    │
│     │    （timing: after_transition_start） │    │
│     │  - 如果是事件触发（无 ACK）           │    │
│     │    → announcement 在引擎执行前播报    │    │
│     │    （timing: before_transition）     │    │
│     │                                    │    │
│     └────────────────────────────────────┘    │
│              │                               │
│              ▼                               │
│     ┌─ 并行执行 ──────────────────────┐      │
│     │  内容引擎：基于 intent 自主选歌    │      │
│     │  灯光引擎：基于 intent 自主配光    │      │
│     │  音效引擎：基于 intent 自主调音    │      │
│     └────────────────────────────────┘      │
│              │                               │
│              ▼                               │
│  4. 收集各引擎的执行反馈                       │
│  5. 汇总为 Feedback Report 回传语义层          │
│                                              │
└──────────────────────────────────────────────┘
```

**协调器的关键职责**：
- **时序控制**：区分 ACK 已播报（Query 触发）和未播报（事件触发）两种情况
- **异常处理**：某个引擎失败时，其他引擎继续工作（降级策略）
- **节奏同步**：灯光引擎可从内容引擎获取当前 BPM 做音乐同步

#### 4.4.2 内容引擎（Team B）— 自主决策模式

```
输入：Scene Descriptor 中的 intent + hints.music（参考）
输出：有序歌单 + 播放控制

┌─ 内容引擎 ──────────────────────────────────────┐
│                                                 │
│  决策流程：                                       │
│  1. 读取 intent.mood → 内部"情绪-音乐"知识库映射   │
│  2. 读取 intent.energy_level/curve → 确定能量分布  │
│  3. 读取 intent.constraints → 硬约束（必须遵守）    │
│  4. 读取 intent.user_overrides → 用户指令（必须遵守）│
│  5. 参考 hints.music → 可采纳可忽略               │
│  6. 用内容引擎自己的推荐算法生成歌单                 │
│                                                 │
│  对外暴露：                                       │
│  - current_track_info (当前播放信息)               │
│  - current_bpm (实时 BPM，供灯光引擎同步)           │
│  - playback_events (跳过/完成/暂停 事件)           │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### 4.4.3 灯光引擎（Team C）— 自主决策模式

```
输入：Scene Descriptor 中的 intent + hints.lighting（参考）
      + 内容引擎的 current_bpm（实时同步）
输出：灯光硬件控制指令

决策流程：
1. 读取 intent.mood + intent.atmosphere
   → 查询内部"情绪-色彩"映射表（灯光设计师建的）
   → 确定基础色调和动效

2. 读取 intent.energy_curve
   → 灯光亮度和动效节奏跟随全局能量曲线

3. 读取 intent.constraints
   → 夜间驾驶亮度上限、儿童场景避免闪烁等

4. 参考 hints.lighting
   → 如果和自己的判断一致，采纳
   → 如果冲突，以自己的专业判断为准

5. 从 Event Bus 获取 current_bpm
   → 决定是否做音乐同步

内部维护的映射表（灯光设计师负责）：
  情绪映射：melancholic_calm → warm_amber + slow_breathing
  情绪映射：family_joy → soft_pink + gentle_pulse
  情绪映射：energetic_drive → vivid_blue + music_sync
```

#### 4.4.4 音效引擎（Team D）— 自主决策模式

```
输入：Scene Descriptor 中的 intent + hints.audio（参考）
输出：DSP 参数控制

决策流程：
1. 读取 intent.mood + intent.atmosphere
   → 查询内部"情绪-音效"预设表（音效工程师建的）

2. 读取 intent.constraints.max_volume_db
   → 硬约束，必须遵守

3. 参考 hints.audio
   → 可采纳可忽略

内部维护的预设表（音效工程师负责）：
  intimate_calm → 近场+低混响+温暖EQ
  family_warm → 环绕+中混响+均衡EQ
  energetic_drive → 宽场+高混响+增强低音
```

#### 4.4.5 引擎间实时通信 — Event Bus

```
┌──────────────────────────────────────────────┐
│              Event Bus (事件总线)              │
│                                              │
│  事件类型：                                    │
│  music.track_changed  → {bpm, energy, mood}  │
│  music.beat           → {timestamp, strong}  │
│  music.playback_state → {playing/paused}     │
│  lighting.transition_done → {}               │
│  audio.volume_changed → {db}                 │
│                                              │
└──────────────────────────────────────────────┘
         ↑              ↑              ↑
    内容引擎发布     灯光引擎订阅     音效引擎订阅
    beat 事件       beat → 灯光闪烁   beat → 低音增强
```

**典型协作场景**：

| 场景 | 发布者 | 事件 | 订阅者 | 动作 |
|------|--------|------|--------|------|
| 灯光跟随节拍 | 内容引擎 | `music.beat` | 灯光引擎 | 强拍时亮度+20% |
| 切歌时灯光变化 | 内容引擎 | `music.track_changed` | 灯光引擎 | 根据新歌情绪微调颜色 |
| 暂停时灯光静态 | 内容引擎 | `music.playback_state` | 灯光引擎 | 停止动效，保持静态 |
| 音量变化时灯光响应 | 音效引擎 | `audio.volume_changed` | 灯光引擎 | 音量降低时亮度也降低 |

事件总线是执行层内部的通信机制，语义层完全不感知。

## 5. 核心体验设计

### 5.1 声光叙事 — Playlist Arc

不是简单地"这首歌配蓝色灯"，而是将一段旅程编排为一个故事弧线：

```
开场（唤醒/过渡） → 沉浸（主体情绪） → 转折（适度变化防疲劳） → 收束（到达前的情绪着陆）
```

**实现方式**：通过 Scene Descriptor 中的 `intent.energy_curve` 字段控制全局叙事节奏：

```json
"energy_curve": [0.4, 0.5, 0.6, 0.5, 0.3]
```

- `energy_curve` 是全局叙事曲线，不只是音乐的事
- 内容引擎基于此曲线编排歌单能量分布
- 灯光引擎基于此曲线调整亮度和动效节奏
- 音效引擎基于此曲线调整空间感和低音强度
- 各引擎独立解读同一条曲线，用各自的专业方式表达

**效果**：用户感受到的不是"AI 帮我选了几首歌"，而是"AI 为我编排了一段旅程体验"——这才是真正的惊喜感来源。

### 5.2 过渡编排策略

场景变化时的处理是体验的关键。核心原则：**场景切换的体验质量 ≥ 场景本身的体验质量。**

| 变化类型 | 过渡策略 | 示例 |
|---------|---------|------|
| 渐变型（天气转晴） | 在当前歌曲结束后自然过渡 | 灯光在 30 秒内渐变，下一首歌切换风格 |
| 事件型（儿童上车） | 当前歌曲淡出 + 语音过渡 | "检测到小朋友上车了，为你们切换到合家欢模式~" |
| 紧急型（安全相关） | 立即响应 | 灯光变亮、音乐降低音量 |
| 用户主动型（语音指令） | ACK 即时确认 + 场景切换 | ACK："好的，马上调整" → 执行 → announcement 解释 |

**过渡参数由 Scene Descriptor 的 `intent.transition` 字段控制**：

```json
"transition": {
  "type": "event",
  "style": "crossfade",
  "duration_sec": 15
}
```

### 5.3 可解释性设计

在适当时机提供轻量解释，增加信任感并教育用户功能的能力边界：

| 时机 | 解释方式 | 示例 |
|------|---------|------|
| 上车时 | announcement | "外面在下雨，为你准备了一些安静的音乐，配上暖色灯光" |
| 场景切换时 | announcement | "小朋友上车了，切换到欢乐模式" |
| 用户 Query 后 | ACK + announcement | ACK："好主意" → announcement："为你切换到海边度假模式" |
| 用户询问时 | announcement | "因为现在是晚高峰通勤时段，我选了一些帮你放松的音乐" |

**ACK 和 announcement 的配合**：用户主动 Query 时，ACK 负责即时确认（< 1 秒），announcement 负责事后解释（跟随 Descriptor）。两者不重复，ACK 是"收到"，announcement 是"我做了什么"。

### 5.4 静默场景设计

不是所有时刻都需要音乐和灯效。系统应该有能力判断：

| 场景 | 检测方式 | 系统行为 |
|------|---------|---------|
| 车内正在打电话 | Mic 检测到 `phone_call` 状态 | 自动降低音量或暂停 |
| 多人热烈聊天 | Mic 检测到 `talking_loud` + OMS 多人 | 音乐退为极低背景音 |
| 驾驶员疲劳 | OMS 闭眼频率高 + 深夜时段 + 长途 | 切换到提神模式 |
| 车内安静且乘客在休息 | Mic 检测到 `silent` + OMS 闭眼 | 保持极低音量或静默 |

### 5.5 乘客冲突处理策略

多乘客场景下，偏好可能冲突。

**优先级规则**：

```
安全 > 儿童适宜 > 驾驶员偏好 > 多数人偏好
```

**处理方案**：

| 策略 | 说明 |
|------|------|
| 优先级规则 | 有儿童时强制 `content_rating: "G"`，安全场景强制灯光变亮 |
| 分区能力 | 如果硬件支持，前排/后排不同音频区域 |
| 协商机制 | 语音播报："车上有小朋友，先播放适合全家的音乐，到达后为您恢复个人偏好" |

---

## 6. 数据结构设计

### 6.1 Scene Descriptor（场景语义描述）— 系统的"合同"

Scene Descriptor 是语义层和执行层之间的接口协议。

**核心设计原则：语义层是"导演"，只描述意图；执行层是"专业演员"，自主决策实现。**

Scene Descriptor 分为两部分：
- **intent**：场景意图（硬约束 + 语义描述），各引擎**必须**基于此做决策
- **hints**：领域建议（可选），各引擎**可以采纳也可以忽略**

```json
{
  "version": "2.0",
  "scene_id": "uuid-xxx",
  "timestamp": "2026-02-27T22:30:00+08:00",
  "scene_name": "雨夜归途·合家欢",
  "scene_narrative": "雨夜带着小朋友回家，温馨而安心",

  "intent": {
    "mood": {"valence": 0.6, "arousal": 0.3},
    "energy_level": 0.4,
    "energy_curve": [0.4, 0.5, 0.6, 0.5, 0.3],
    "curve_duration_minutes": 30,
    "atmosphere": "warm_intimate",
    "social_context": "family_with_child",
    "trip_phase": "midway",
    "trip_progress": 0.3,

    "constraints": {
      "content_rating": "G",
      "max_volume_db": -10,
      "avoid_sudden_changes": true,
      "brightness_max": 0.6
    },

    "user_overrides": {
      "exclude_tags": ["流行乐", "摇滚"],
      "preferred_language": ["中文", "英文"]
    },

    "transition": {
      "type": "event",
      "style": "crossfade",
      "duration_sec": 15
    }
  },

  "hints": {
    "music": {
      "suggested_tags": ["儿童友好", "轻快民谣", "温馨"],
      "suggested_tempo": [80, 110],
      "suggested_artists": ["陈绮贞", "Norah Jones"],
      "suggested_vocal_style": "soft"
    },
    "lighting": {
      "suggested_palette": ["warm_amber", "soft_pink"],
      "suggested_rhythm": "gentle_pulse"
    },
    "audio": {
      "suggested_spatial": "surround_warm",
      "suggested_bass_level": "low"
    }
  },

  "announcement": {
    "text": "小朋友上车了，切换到温馨模式",
    "timing": "before_transition",
    "voice_style": "warm_female"
  },

  "meta": {
    "confidence": 0.72,
    "source": "layer3_llm",
    "reasoning": "检测到儿童上车，结合用户不喜欢流行乐的偏好，判断场景为温馨家庭氛围",
    "fallback_template": "TPL_067",
    "expires_at": "2026-02-27T23:15:00+08:00",
    "low_confidence_dimensions": ["mood_valence"]
  }
}
```

**字段职责划分**：

| 字段区域 | 谁生成 | 谁消费 | 约束力 |
|---------|--------|--------|--------|
| `intent.mood/energy/atmosphere` | 语义层 | 所有引擎 | 必须基于此做决策 |
| `intent.constraints` | 语义层 | 所有引擎 | **硬约束，必须遵守** |
| `intent.user_overrides` | 语义层（来自用户指令） | 所有引擎 | **硬约束，必须遵守** |
| `intent.energy_curve` | 语义层 | 所有引擎 | 全局叙事节奏，各引擎独立解读 |
| `hints.music` | 语义层 | 内容引擎 | **仅供参考，可忽略** |
| `hints.lighting` | 语义层 | 灯光引擎 | **仅供参考，可忽略** |
| `hints.audio` | 语义层 | 音效引擎 | **仅供参考，可忽略** |

**什么时候 hints 有价值**：

| 场景 | hints 的价值 |
|------|-------------|
| 用户明确说了"来点爵士" | `hints.music.suggested_tags: ["爵士"]` 传递用户意图 |
| LLM 识别到特殊场景 | "海边日落" → `hints.lighting.suggested_palette: ["sunset_orange"]` 有启发 |
| 记忆系统知道用户偏好 | 用户历史上喜欢 Norah Jones → `hints.music.suggested_artists` 有价值 |

### 6.2 ACK 消息结构

ACK 是独立于 Scene Descriptor 的旁路消息，仅在用户主动 Query 时由 Query Router 生成，直接发送到 TTS 引擎：

```json
{
  "type": "ack",
  "text": "好主意，让我想想怎么编排",
  "voice_style": "warm_female",
  "timestamp": "2026-02-27T22:30:01+08:00",
  "query_intent": "creative",
  "estimated_wait_sec": 8
}
```

**ACK 与 Announcement 的关系**：

| 维度 | ACK | Announcement |
|------|-----|-------------|
| 职责 | 确认用户被听到，管理等待预期 | 解释新场景的内容和原因 |
| 时效 | < 1 秒 | 跟随 Scene Descriptor（可能延迟 5-15 秒） |
| 生成者 | Query Router（规则/模板，不需要 LLM） | Layer 3 或 Layer 2 |
| 通道 | 独立旁路 → TTS | Scene Descriptor → 编排协调器 → TTS |
| 触发条件 | 仅用户主动 Query | 任何场景切换（用户 Query 或被动事件） |

### 6.3 Feedback Report（反馈报告）— 执行层回传给语义层

```json
{
  "scene_id": "uuid-xxx",
  "timestamp": "2026-02-27T22:45:00+08:00",

  "music_feedback": {
    "playlist_generated": true,
    "tracks_count": 12,
    "hints_adopted": {
      "suggested_tags": true,
      "suggested_tempo": false,
      "reason": "引擎判断 tempo [70, 100] 更适合当前 energy_level"
    },
    "actual_parameters": {
      "tempo_range_used": [70, 100],
      "tags_used": ["儿童友好", "轻快民谣", "温馨", "摇篮曲"]
    },
    "current_track": {"title": "小星星", "artist": "儿歌", "bpm": 95},
    "user_actions": [
      {"type": "skip", "track": "xxx", "at_sec": 30},
      {"type": "completed", "track": "yyy"}
    ]
  },

  "lighting_feedback": {
    "transition_completed": true,
    "actual_duration_sec": 14,
    "hints_adopted": {
      "suggested_palette": true,
      "suggested_rhythm": true
    },
    "actual_parameters": {
      "base_color": "warm_amber",
      "rhythm": "gentle_pulse"
    },
    "hardware_errors": []
  },

  "audio_feedback": {
    "preset_applied": "family_warm",
    "hints_adopted": {
      "suggested_spatial": false,
      "reason": "引擎判断 family_warm 比 surround_warm 更适合有儿童的场景"
    },
    "actual_volume_db": -12,
    "dsp_errors": []
  },

  "user_interactions": [
    {"type": "voice_command", "text": "声音再小一点", "timestamp": "..."},
    {"type": "manual_adjust", "target": "volume", "value": -3}
  ]
}
```

**Feedback Report 的核心价值**：
- `hints_adopted` 字段让语义层知道哪些建议被采纳、哪些被忽略及原因
- `actual_parameters` 字段让语义层知道引擎实际使用了什么参数
- 这些信息可以帮助语义层在后续推理中给出更好的 hints

### 6.4 Scene Descriptor 保鲜期

Scene Descriptor 不是生成一次就永远有效的，需要设计过期和刷新机制：

| 场景 | 保鲜期 | 刷新策略 |
|------|--------|---------|
| 通勤（路线固定） | 较长（整个行程） | 除非有事件触发，否则不刷新 |
| 长途旅行 | 30-45 分钟 | 定期刷新，跟随行程进度调整情绪曲线 |
| 城市内短途 | 整个行程 | 一般不需要刷新 |
| 多人场景 | 较短（10-15 分钟） | 人员变化频繁，需要更敏感 |

通过 `meta.expires_at` 字段控制。

---

## 7. 记忆与成长系统

单次体验好不够，要让系统越用越懂用户，构成个性化飞轮：**用得越多 → 越精准 → 越离不开**。

### 7.1 三层记忆架构

| 记忆层级 | 时间跨度 | 内容 | 用途 |
|---------|---------|------|------|
| 短期记忆 | 当前旅程 | 这次旅程中跳过了哪些歌、对哪个灯效说了"太亮了" | 实时调整当前体验 |
| 中期记忆 | 周/月级别 | 每周通勤的偏好模式、周末出游的偏好模式 | 场景模板个性化 |
| 长期画像 | 季度/年级别 | 音乐品味演变、季节性偏好变化 | Layer 3 Prompt 中的用户画像 |

### 7.2 记忆更新机制

```
Feedback Report 到达
        │
        ▼
┌─ 记忆更新引擎 ──────────────────────────┐
│                                         │
│  短期记忆：                               │
│  - 用户跳过了歌 → 记录 exclude 偏好       │
│  - 用户手动调音量 → 记录音量偏好           │
│  - 用户说"太亮了" → 记录亮度偏好           │
│  - ACK 后用户追加指令 → 更新约束           │
│                                         │
│  中期记忆（每日聚合）：                     │
│  - 通勤时段偏好模式                        │
│  - 周末出游偏好模式                        │
│  - 不同乘客组合的偏好                      │
│  - hints 采纳率统计（哪些 hints 有效）      │
│                                         │
│  长期画像（每月更新）：                     │
│  - 音乐品味标签权重                        │
│  - 灯光/音效偏好基线                       │
│  - 季节性偏好变化趋势                      │
│                                         │
└─────────────────────────────────────────┘
```

### 7.3 旅程回忆功能

每次旅程结束后，系统自动生成旅程回忆卡片：

```
🌧️ 雨夜归途 · 2026.02.27
━━━━━━━━━━━━━━━━━━
🎵 播放了 12 首歌
💙 主色调：深蓝 + 暖琥珀
⏱️ 旅程 45 分钟
🎯 情绪曲线：疲惫 → 平静 → 温暖

"在雨声和爵士乐中，送你安全到家"
━━━━━━━━━━━━━━━━━━
[分享] [收藏这个场景] [再来一次]
```

- 用户可以收藏场景，下次手动触发
- 可以分享到社交媒体，形成传播
- 积累的旅程回忆形成情感连接，提升品牌忠诚度

---

## 8. 质量评估体系 — 三层闭环

### 8.1 第一层：规则校验（自动化，实时）

底线检查，确保生成的 Scene Descriptor 不会出错：

| 规则 | 说明 |
|------|------|
| child_safety | 有儿童时 `content_rating` 必须为 `G` |
| volume_safety | `max_volume_db` 不超过安全阈值 |
| constraint_consistency | `user_overrides.exclude_tags` 不能出现在 `hints.music.suggested_tags` 中 |
| transition_safety | 紧急场景不允许 `slow_fade` |
| preference_respect | 用户明确排除的标签不能出现在推荐中 |
| ack_required | 用户主动 Query 时必须生成 ACK |

**通过率目标：100%。** 不通过直接回退到安全模板。

### 8.2 第二层：语义一致性评分（自动化，离线批量）

| 评估维度 | 评估方法 | 权重 |
|---------|---------|------|
| 情绪匹配度 | intent.mood 与各引擎实际输出的情绪标签是否一致 | 30% |
| 能量匹配度 | intent.energy_level 与实际歌单能量分布是否对应 | 20% |
| 约束满足度 | constraints 和 user_overrides 是否被正确执行 | 25% |
| 叙事连贯性 | 与上一个场景描述的过渡是否合理 | 15% |
| 多维协调性 | 音乐/灯光/音效三者之间是否风格统一 | 10% |

可以构建评估数据集：

```
输入：{场景特征向量, 用户画像, 变化事件}
输出：{Scene Descriptor}
标注：产品专家对输出打分 (1-5)
```

积累 500-1000 条标注数据后，可以训练自动评分模型，用于线上实时质量监控。

### 8.3 第三层：用户行为反馈（线上，长期）

| 行为信号 | 含义 | 权重 |
|---------|------|------|
| 歌曲跳过 | 音乐推荐不匹配 | 高负面 |
| 手动调整灯光/音量 | 效果参数不合适 | 中负面 |
| 语音说"不喜欢/换一个" | 整体不满意 | 高负面 |
| 完整听完一首歌 | 音乐匹配 | 中正面 |
| 主动说"这个不错" | 惊喜感 | 高正面 |
| 长时间不干预 | 整体满意 | 低正面 |
| 下次上车功能仍开启 | 长期认可 | 高正面 |

**反馈闭环**：

```
Scene Descriptor 生成 → 执行 → 收集用户行为 → 计算质量分
                                                    │
                    ┌───────────────────────────────┘
                    ↓
        质量分回传给 Layer 3 的 Prompt（few-shot 示例）
        质量分用于优化 Layer 2 的模板库
        质量分用于更新用户记忆
        hints 采纳率用于优化 Layer 3 的建议质量
```

### 8.4 A/B 测试框架

在同一场景下，可以让 Layer 3 生成两个候选 Descriptor，随机选一个执行，然后对比用户行为反馈：

```
Layer 3 生成 Descriptor A（保守策略）
Layer 3 生成 Descriptor B（激进策略）
        │
        ▼
  随机选择一个执行
        │
        ▼
  收集用户行为反馈
        │
        ▼
  对比 A/B 的质量分
        │
        ▼
  优胜策略权重提升
```

**A/B 测试维度**：

| 维度 | A 组 | B 组 |
|------|------|------|
| hints 详细度 | 只给 intent，不给 hints | 给完整 hints |
| 模板 vs LLM | 使用模板 Descriptor | 使用 Layer 3 生成 |
| energy_curve 长度 | 3 段 | 7 段 |
| announcement 频率 | 每次场景切换都播报 | 仅重大变化时播报 |

---

## 9. 端到端场景走查

### 9.1 场景一：深夜雨天独自通勤回家

**前置条件**：用户已使用系统 2 周，有通勤偏好记忆。

```
T+0s    驾驶员上车，点火
        Layer 1：时间=22:30（夜晚）、天气=雨、OMS=1人成人、Mic=静默
        Layer 2：场景向量 → melancholic_calm，匹配 TPL_042（similarity: 0.87）

T+1.5s  快通道响应
        编排协调器收到 TPL_042 的 Scene Descriptor
        announcement："晚上好，外面在下雨，为你准备了安静的音乐"
        灯光渐变为 warm_amber + slow_breathing
        音乐开始播放（低能量爵士）
        音效切换为 intimate_cocoon

T+2s    Layer 3 开始后台推理
        输入：场景向量 + 用户画像（偏好爵士、不喜欢流行乐、通勤偏好安静）

T+10s   Layer 3 返回个性化 Descriptor
        与 TPL_042 计算 Diff：
        - energy_curve 更精细（从 3 段变 5 段）
        - hints 中加入用户偏好的 artist
        增量更新：在当前歌曲结束后，下一首歌开始体现个性化

T+25min 行程过半
        Layer 2 检测到 trip_progress: 0.5
        energy_curve 进入中段，能量微升
        灯光亮度微增，动效从 slow_breathing 变为 gentle_pulse

T+40min 接近目的地（trip_progress: 0.9）
        energy_curve 进入收束段
        音乐切换到更安静的曲目
        灯光亮度降低，准备"到家"氛围

T+45min 到达目的地
        announcement："到家了，今晚听了 12 首歌，晚安"
        生成旅程回忆卡片
        更新中期记忆：本次通勤偏好数据
```

### 9.2 场景二：家庭出游 + 儿童上车 + 用户创意请求

**前置条件**：驾驶员已在车上，系统正在播放个人偏好音乐。

```
T+0s    当前状态：solo_commute 场景，播放爵士乐

T+5s    后排检测到儿童上车
        Layer 1：OMS 更新 → count: 2, composition: [adult_driver, child_rear]
        Layer 2：child_friendly 0→1.0, social_level 0.1→0.6
                 场景簇切换：melancholic_calm → family_joy
                 trigger_layer3: true（场景簇切换）

T+6s    快通道响应
        匹配 TPL_067（family_joy 模板）
        编排协调器：
          announcement："小朋友上车了，切换到温馨模式"
          内容引擎：切换到儿童友好歌单（content_rating: G）
          灯光引擎：warm_amber → soft_pink + gentle_pulse
          音效引擎：intimate_cocoon → family_warm

T+15s   Layer 3 返回个性化 family_joy Descriptor
        增量更新：更精准的儿童友好歌单

T+30min 用户说："给我一个海边度假的感觉"

T+30.3s ASR 识别完成

T+30.5s Query Router：
        - 意图分类 → 创意型
        - 生成 ACK → "好主意，让我想想怎么编排"
        - ACK 直接发送到 TTS 播报                    ← 用户 < 1 秒收到回应

T+31s   Layer 3 开始推理
        输入：当前场景 + "海边度假" 创意请求 + 仍有儿童（constraints 保留）

T+38s   Layer 3 返回新 Descriptor
        scene_name: "海边度假·合家欢"
        intent.atmosphere: "beach_vacation"
        intent.constraints.content_rating: "G"（仍保留儿童约束）
        hints.music.suggested_tags: ["Bossa Nova", "夏日", "轻快"]
        hints.lighting.suggested_palette: ["ocean_blue", "sunset_orange"]
        announcement: "为你们切换到海边度假模式，选了一些轻松的音乐"

T+38.5s 编排协调器：
        播报 announcement（场景解释）
        内容引擎：自主决策，基于 beach_vacation 氛围选歌
        灯光引擎：自主决策，ocean_blue 渐变
        音效引擎：自主决策，切换到宽场模式
```

### 9.3 场景三：疲劳驾驶检测

```
T+0s    当前状态：late_night_solo，已驾驶 2 小时

T+5s    OMS 检测到驾驶员频繁揉眼、闭眼时间变长
        Layer 1：fatigue_level: 0.7（confidence: 0.65）
        Layer 2：safety_attention 急升
                 change_type: "critical"
                 trigger_layer3: false（紧急场景不等 Layer 3）

T+6s    快通道紧急响应
        匹配 TPL_099（fatigue_alert 模板）
        编排协调器（紧急模式，跳过 announcement 等待）：
          灯光立即变亮（brightness: 0.9）
          音乐切换到高能量曲目
          音效增强低音
          announcement（与灯光同时）："检测到你有点疲劳，为你切换到提神模式，建议找个服务区休息一下"

T+10s   Layer 3 后台生成更精细的提神方案
        增量更新：更合适的提神歌单
```

---

## 10. 实现计划

### 10.1 开发策略：Mock 驱动 + 接口先行

由于语义层和执行层完全通过 JSON 解耦，可以并行开发：

```
Phase 0（第 1-2 周）：接口定义
├─ 确定 Scene Descriptor JSON Schema（V2.0）
├─ 确定 Feedback Report JSON Schema
├─ 确定 ACK 消息结构
├─ 编写 Mock 数据集（覆盖 10+ 典型场景）
└─ 各团队基于 Mock 数据独立开发

Phase 1（第 3-6 周）：核心链路
├─ 语义层：Layer 1 + Layer 2 + 模板库（快通道）
├─ 语义层：Query Router + ACK 生成
├─ 执行层：编排协调器 + 内容引擎基础版
├─ 执行层：灯光引擎基础版 + 音效引擎基础版
└─ 集成测试：快通道端到端跑通

Phase 2（第 7-10 周）：智能化
├─ 语义层：Layer 3 接入（云端大模型）
├─ 语义层：慢通道 + 增量更新
├─ 执行层：引擎自主决策优化 + hints 采纳逻辑
├─ 执行层：Event Bus + 引擎间协作
└─ 集成测试：快慢双通道端到端跑通

Phase 3（第 11-14 周）：体验打磨
├─ 记忆系统：短期 + 中期记忆
├─ 质量评估：规则校验 + 行为反馈收集
├─ 过渡编排优化：各类过渡策略调优
├─ ACK 模板优化：基于用户反馈调整 ACK 话术
└─ 场景模板扩充：从 10 个扩展到 50 个

Phase 4（第 15-18 周）：规模化
├─ 场景模板扩展到 100 个
├─ 长期记忆 + 用户画像
├─ A/B 测试框架
├─ 旅程回忆功能
└─ 性能优化 + 稳定性加固
```

### 10.2 里程碑

| 里程碑 | 时间 | 交付物 | 验收标准 |
|--------|------|--------|---------|
| M1：接口冻结 | 第 2 周末 | JSON Schema + Mock 数据集 + ACK 结构 | 各团队确认接口可用 |
| M2：快通道 Demo | 第 6 周末 | 上车 → 模板响应 → 声光电联动 + Query ACK | 上车 2 秒内有响应；用户说话 1 秒内有 ACK |
| M3：智能化 Demo | 第 10 周末 | 快慢双通道 + Layer 3 个性化 | 慢通道结果明显优于模板 |
| M4：体验版 | 第 14 周末 | 完整体验链路 + 记忆系统 | 连续使用 1 周后推荐精准度提升 |
| M5：发布版 | 第 18 周末 | 全功能 + 稳定性 | 通过车规级测试 |

### 10.3 Mock 驱动开发详解

Mock 数据是并行开发的关键。各团队在接口冻结后，基于 Mock 数据独立开发和测试：

**语义层 Mock**（供执行层使用）：

```json
// mock_descriptors/rainy_night_solo.json
{
  "version": "2.0",
  "scene_name": "深夜雨天独行",
  "intent": {
    "mood": {"valence": 0.3, "arousal": 0.2},
    "energy_level": 0.25,
    "atmosphere": "melancholic_calm",
    "constraints": {"max_volume_db": -8}
  },
  "hints": {
    "music": {"suggested_tags": ["爵士", "安静", "独立民谣"]}
  },
  "announcement": {"text": "外面在下雨，为你准备了安静的音乐"}
}
```

**执行层 Mock**（供语义层使用）：

```json
// mock_feedbacks/rainy_night_solo_feedback.json
{
  "scene_id": "mock-001",
  "music_feedback": {
    "playlist_generated": true,
    "tracks_count": 10,
    "hints_adopted": {"suggested_tags": true}
  },
  "user_interactions": []
}
```

**Event Bus Mocker**：
为了让灯光和音效团队在内容引擎未就绪时也能测试声光同步，需要开发一个轻量级的 `EventBus Mocker` 工具。该工具读取预设的节拍文件（如 `.csv`），按真实时间间隔向 Event Bus 注入带有 `presentation_time` 的 `music.beat` 和 `music.track_changed` 事件。

---

## 11. 团队结构

### 11.1 团队划分

| 团队 | 职责 | 核心交付 |
|------|------|---------|
| **Team A：语义层** | 多模态感知、场景识别、大模型推理、Query Router + ACK、记忆系统 | Scene Descriptor + ACK 消息 |
| **Team B：内容引擎** | 曲库管理、推荐算法、歌单编排、播放控制 | 有序歌单 + 播放事件 |
| **Team C：灯光引擎** | 灯光语义映射、动效生成、硬件驱动适配 | 灯光控制指令 |
| **Team D：音效引擎** | 音效预设管理、DSP 参数控制、空间音频 | DSP 控制指令 |
| **Team E：平台/质量** | 集成测试、质量评估、A/B 测试、性能监控 | 质量报告 + 测试框架 |

### 11.2 团队间协作接口

```
Team A ──Scene Descriptor JSON──→ Team B/C/D
Team B/C/D ──Feedback Report JSON──→ Team A
Team B ──Event Bus──→ Team C/D（引擎间实时通信）
Team A ──ACK JSON──→ TTS（独立旁路，不经过 Team B/C/D）
Team E ──质量报告──→ All Teams
```

**关键协作规则**：
- 团队间只通过 JSON 接口通信，不直接调用对方代码
- 接口变更需要所有消费方确认
- 每周一次跨团队集成测试

---

## 12. 风险管理

| 风险类别 | 风险点 | 发生概率 | 影响程度 | 应对策略 (Mitigation) |
|---------|--------|---------|---------|----------------------|
| **硬件与平台** | 车厂 VHAL 接口不支持高频灯光动效 | 高 | 高 | 早期（Phase 0）必须与车厂 BSP 团队确认接口能力。若不支持，降级为仅支持静态颜色切换，放弃呼吸/脉冲动效。 |
| **硬件与平台** | 端侧 LLM 占用过多内存导致系统杀后台 | 中 | 致命 | 采用 Qwen-0.5B 极小模型；使用 mmap 方式加载模型；在 AndroidManifest 中申请 `persistent` 属性（需系统签名）。 |
| **硬件与平台** | 高通 QNN SDK 适配 llama.cpp 失败 | 中 | 高 | 备选方案：放弃 DSP 加速，直接使用 CPU 多线程推理（0.5B 模型在 SA8295P CPU 上仍可达到 ~10 token/s，勉强可用）。 |
| **硬件与平台** | 音频焦点冲突（如导航播报打断音乐） | 必现 | 中 | 严格遵守 Android Audio Focus 机制；导航播报时触发 `duck`（降低音量）而非 `pause`，播报结束后平滑恢复。 |
| **模型与算法** | 云端 LLM 延迟波动大（>15秒） | 高 | 中 | 强化 Layer 2 快通道的模板丰富度；在 ACK 话术中加入预期管理（"这个想法很有创意，我需要多想一会儿"）。 |
| **模型与算法** | 云端 LLM 不可用 | 低 | 高 | 快通道模板完全可独立运行，作为兜底方案。 |
| **模型与算法** | OMS 识别准确率低 | 中 | 中 | confidence 降权 + 降级到默认值。 |
| **模型与算法** | 情绪推断不准确 | 高 | 中 | 情绪维度默认降权 + 不作为主要决策依据。 |
| **系统与体验** | 用户对 AI 推荐不信任 | 中 | 高 | 可解释性设计 + ACK 即时反馈 + 渐进式引导。 |
| **系统与体验** | 场景切换过于频繁 | 中 | 中 | 引入防抖机制 + 设定保鲜期 + 提高变化阈值。 |
| **系统与体验** | TTS 通道冲突（ACK 和 announcement 重叠） | 低 | 中 | ACK 播报完成后才允许 announcement 排队。 |
| **系统与体验** | 引擎间协作延迟 | 低 | 中 | Event Bus 本地通信 + 超时降级，确保声光同步。 |

---

## 13. KPI 体系

### 13.1 系统健康指标

| 指标 | 目标 | 测量方式 |
|------|------|---------|
| 快通道响应时间 | < 2 秒 | 从上车检测到首次灯光/音乐响应 |
| ACK 响应时间 | < 1 秒 | 从用户说话结束到 TTS 播报 ACK |
| Layer 3 推理时间 | < 15 秒 | 从触发到 Descriptor 返回 |
| 规则校验通过率 | 100% | 自动化检测 |
| 系统可用率 | > 99.5% | 快通道独立可用 |

### 13.2 用户体验指标

| 指标 | 目标 | 测量方式 |
|------|------|---------|
| 歌曲跳过率 | < 30% | Feedback Report 中的 skip 事件 |
| 手动调整率 | < 20% | Feedback Report 中的 manual_adjust 事件 |
| 语音负面反馈率 | < 5% | "不喜欢"、"换一个"等语音指令频率 |
| 功能持续使用率 | > 70% | 连续 7 天上车后功能仍开启 |
| 场景切换满意度 | > 80% | 切换后 30 秒内无负面操作 |

### 13.3 产品成功指标

| 指标 | 目标 | 测量方式 |
|------|------|---------|
| 功能激活率 | > 60% | 新车用户首月内开启功能的比例 |
| 月活跃率 | > 50% | 每月至少使用 5 次的用户比例 |
| NPS 净推荐值 | > 40 | 用户调研 |
| 付费转化率（场景包） | > 10% | 购买高级场景包的用户比例 |

---

## 14. 附录

### 14.1 术语表

| 术语 | 说明 |
|------|------|
| Scene Descriptor | 场景语义描述 JSON，语义层和执行层之间的接口协议 |
| intent | Scene Descriptor 中的场景意图部分，各引擎必须基于此决策 |
| hints | Scene Descriptor 中的领域建议部分，各引擎可采纳可忽略 |
| ACK | 即时应答消息，用户主动 Query 后 < 1 秒的语音确认 |
| announcement | 场景解释播报，跟随 Scene Descriptor 的语音说明 |
| Feedback Report | 执行层回传给语义层的反馈报告 JSON |
| Scene Cluster | 场景簇，预定义的 50-100 个典型场景原型 |
| energy_curve | 能量曲线，控制一段旅程的叙事节奏 |
| Event Bus | 执行层内部的引擎间实时通信机制 |
| Query Router | 语义层中负责用户语音指令分类和 ACK 生成的组件 |

### 14.2 Scene Cluster 目录（示例）

| ID | 场景簇名称 | 典型场景 | 默认 energy_level | 默认 atmosphere |
|----|-----------|---------|-------------------|----------------|
| TPL_001 | energetic_morning_commute | 早晨通勤，精神饱满 | 0.6 | fresh_energetic |
| TPL_010 | focused_highway | 高速巡航，专注驾驶 | 0.4 | calm_focused |
| TPL_020 | family_joy | 合家欢出游 | 0.7 | warm_joyful |
| TPL_030 | late_night_solo | 深夜独行 | 0.2 | melancholic_calm |
| TPL_042 | rainy_night_commute | 雨夜通勤 | 0.25 | melancholic_calm |
| TPL_050 | weekend_road_trip | 周末自驾游 | 0.7 | adventurous |
| TPL_060 | romantic_evening | 浪漫夜驾 | 0.4 | intimate_warm |
| TPL_067 | family_with_child | 带儿童出行 | 0.5 | warm_intimate |
| TPL_080 | party_mode | 多人欢乐 | 0.85 | energetic_social |
| TPL_099 | fatigue_alert | 疲劳提醒 | 0.8 | alert_energetic |

### 14.3 ACK 模板库（MVP 版本）

| 意图类别 | ACK 模板 | 变体 |
|---------|---------|------|
| 约束型 | "好的，马上调整" | "收到，已经帮你调整了" |
| 偏好型 | "收到，正在为你切换" | "好的，为你换一种风格" |
| 创意型 | "好主意，让我想想怎么编排" | "有意思，给我几秒钟" |
| 情绪型 | "我听到了，为你调整氛围" | "好的，为你换个心情" |
| 音量型 | "好的" | （直接执行，ACK 极简） |

---

*文档结束*
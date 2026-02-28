### 不一致 1：Mock 数据 JSON 结构与 spec-all.md 定义的 Scene Descriptor 结构不一致（严重）
spec-all.md 第 6.1 节定义的 Scene Descriptor V2.0 结构为：

```
{
  "version": "2.0",
  "scene_id": "...",
  "intent": {
    "mood": {"valence": 0.6, "arousal": 0.3},
    "energy_level": 0.4,
    "energy_curve": [...],
    "atmosphere": "...",
    "constraints": {...},
    "user_overrides": {...},
    "transition": {...}
  },
  "hints": {
    "music": {...},
    "lighting": {...},
    "audio": {...}
  },
  "announcement": {...},
  "meta": {...}
}
```
但 mock_data 中的 3 个 Scene Descriptor 文件（如 scene_rainy_night.json ）使用的是完全不同的扁平化结构：

```
{
  "scene_id": "...",
  "intent": "melancholic_calm",  // 字符串而非对象
  "confidence": 0.92,
  "orchestration": {             // 不存在于规范中
    "content_hints": {...},
    "light_hints": {...},
    "audio_hints": {...}
  }
}
```
具体差异 ：

- intent 在规范中是一个复杂对象（含 mood/energy_level/constraints 等），在 Mock 中是一个简单字符串
- Mock 中使用 orchestration 作为顶层字段，规范中不存在此字段
- Mock 中缺少 version 、 scene_name 、 scene_narrative 、 meta 等规范要求的字段
- Mock 中的 hints 被嵌套在 orchestration 下（如 content_hints 、 light_hints ），而规范中 hints 是顶层字段
- energy_curve 格式不同：规范中是简单数组 [0.4, 0.5, 0.6, 0.5, 0.3] ，Mock 中是对象数组 [{"time_offset_s": 0, "energy_level": 0.3}, ...]
- Mock 中 constraints.max_volume_db 使用正数（如 75），规范中使用负数（如 -10）

### 不一致 2：ACK 消息结构与 spec-all.md 定义不一致（中等）
spec-all.md 第 6.2 节定义的 ACK 结构为：

```
{
  "type": "ack",
  "text": "...",
  "voice_style": "warm_female",
  "timestamp": "...",
  "query_intent": "creative",
  "estimated_wait_sec": 8
}
```
但 ack_creative.json 使用的结构为：

```
{
  "message_id": "ack_creative_001",
  "type": "voice_ack",           // 不同于规范的 "ack"
  "source": "query_router",      // 规范中无此字段
  "trigger": {...},              // 规范中无此字段
  "response": {
    "tts_text": "...",           // 规范中为 "text"
    "voice_type": "warm_female", // 规范中为 "voice_style"
    "latency_ms": 850            // 规范中无此字段
  }
}
```
具体差异 ：

- type 值不同（ "ack" vs "voice_ack" ）
- 字段命名不同（ text vs tts_text ， voice_style vs voice_type ）
- Mock 中缺少 query_intent 和 estimated_wait_sec 字段
- Mock 中多了 message_id 、 source 、 trigger 、 latency_ms 等规范中未定义的字段

### 不一致 3：spec-all.md 内部存在 Markdown 格式错误（轻微）
在 spec-all.md 第 235-242 行附近，存在未正确关闭的代码块和重复的章节标题：

```
```  ← 第 236 行，多余的代码块结束标记

---

# 第二部分：第 4 章（技术架构）— 含 ACK 机制

```markdown  ← 第 242 行，多余的代码块开始标记
```

类似的问题在第 729-731 行也出现了（"第三部分"的分隔标记）。这些看起
来是文档拼接时遗留的格式问题，会导致 Markdown 渲染异常。此外，第 
831 行出现了一段错误文本 `"服务端异常，请稍后重试 (-1)"`，疑似是 
API 调用失败时的错误信息被误写入文档。

### 不一致 4：spec-all.md 中 Scene Descriptor 定义重复（轻微）

第 6.1 节的标题和开头内容在文档中出现了两次（第 824-831 行和第 
832-838 行），第一次出现时被截断（出现了"服务端异常"的错误信息），
第二次才是完整内容。

### 差距 5：Mock 数据覆盖不足

`spec-all.md` 第 10.3 节和 `tasks.md` T0-4 任务要求"预置 
3-5 个典型用户画像 (Persona) 及记忆数据"，并"编写 Mock 数据集
（覆盖 10+ 典型场景）"。当前 mock_data 目录仅有 4 个文件（3 个 
Scene Descriptor + 1 个 ACK），远未达到 10+ 典型场景的要求。缺
少的 Mock 数据包括：
- Feedback Report 的 Mock 数据（规范中有定义但无 Mock 文件）
- Layer 1 输出的 Mock 数据
- Layer 2 场景向量和变化检测的 Mock 数据
- 更多场景的 Scene Descriptor（如早晨通勤、高速巡航、浪漫夜驾、多
人欢乐等）
- 用户画像/记忆数据的 Mock 文件

### 差距 6：缺少 Event Bus Mocker 工具 (已解决)

`tasks.md` T0-5 任务要求"开发 Event Bus Mocker 工具，支持节拍
注入"，`spec-all.md` 第 10.3 节也详细描述了该工具的需求（读取预设
节拍文件，按真实时间间隔注入 `music.beat` 和 `music.
track_changed` 事件）。目前已在 `src/mocker/eventBusMocker.js` 中实现。

### 差距 7：缺少 JSON Schema 正式定义文件

多个文档（`constitution.md`、`tasks.md` T0-1/T0-2/T0-3）都强
调需要"冻结 JSON Schema"，但项目中没有独立的 JSON Schema 定义文
件（如 `.schema.json` 文件）。目前 Schema 仅以 Markdown 中的 
JSON 示例形式存在于 `spec-all.md` 中，这不利于自动化校验和代码生
成。

### 差距 8：plan.md 中的风险管理与 spec-all.md 部分重叠但不完全
一致

`plan.md` 第 8.5 节列出了 5 项核心风险，`spec-all.md` 第 12 节
列出了 8 项风险。两者有重叠（如云端延迟、VHAL 接口），但也有差异：
- `plan.md` 独有：端侧 LLM 内存导致系统杀后台、QNN SDK 适配失
败、音频焦点冲突
- `spec-all.md` 独有：OMS 识别准确率低、情绪推断不准确、用户对 
AI 不信任、场景切换过于频繁、TTS 通道冲突、引擎间协作延迟

这违反了 `doc_integration_guide.md` 中提出的"单一事实来源"原
则。

### 差距 9：testing.md 中的场景与 spec-all.md 的场景走查高度重
复

`testing.md` 的 3 个测试场景与 `spec-all.md` 第 9 节的 3 个端
到端场景走查内容几乎完全相同（包括前置条件、时间线、预期效果）。虽然 
`testing.md` 引用了 `spec-all.md`，但实际上是复制了全部内容而非
仅做引用，这增加了维护成本，且未来修改时容易出现两处不同步的风险。

### 差距 10：缺少实际代码和项目工程结构 (已解决)

整个项目目前已搭建基础的工程脚手架，包含 `package.json`、`src/`、`tests/`、`docs/` 等目录结构，并实现了基础的 Mocker 工具。

---

## 四、文档体系整体评价

### 优点
1. **文档体系完整**：从愿景（constitution）到规范（spec-all）到
计划（plan）到任务（tasks）到测试（testing），形成了完整的文档链条
2. **技术设计深入**：双速架构、ACK 机制、intent/hints 分离、
Event Bus 等设计考虑周全
3. **交叉引用清晰**：文档间通过 Markdown 链接相互引用，导航方便
4. **技术选型务实**：基于 SA8295P 的实际约束进行了详细的内存预算估
算和可行性分析
5. **场景走查详尽**：3 个核心场景的时间线精确到秒级，验收标准明确

### 需要改进的方面
1. **Mock 数据与规范严重不一致**：这是最紧迫的问题，如果团队基于当
前 Mock 数据开发，将与规范定义的接口产生冲突
2. **文档内部格式问题**：`spec-all.md` 存在拼接痕迹和格式错误，需
要清理
3. **风险管理应统一**：应遵循"单一事实来源"原则，将风险管理统一到一
个文档中
4. **Mock 数据量不足**：需要补充更多场景和数据类型的 Mock 文件
5. **缺少正式的 JSON Schema 文件**：应产出机器可读的 Schema 定义文件（如 `.schema.json`），以支持自动校验和代码生成

```
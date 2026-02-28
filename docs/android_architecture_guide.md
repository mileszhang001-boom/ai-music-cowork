# 车载座舱 AI 娱乐融合方案 — Android 架构设计指南

## 1. 架构选型背景与核心建议

基于项目在 Node.js 环境下 Phase 3（核心模块集成测试）的成功验证，以及《技术规范文档 V2.2》中对高通 SA8295P 平台（Android 14 Automotive）的硬件约束（可用内存 4-6GB）和极高的响应速度要求（<2秒响应，<1秒 ACK，毫秒级声光同步），特制定本 Android 架构设计指南。

### 1.1 核心建议：单 APK + 多 SDK (AAR) 模块化架构

为了实现**快速在实车上跑通全链路 MVP**，同时保证**极低的 Bug 率和高稳定性**，强烈推荐采用“单 APK + 多 SDK (AAR) 模块化”架构。

**为什么选择单 APK？**
1. **性能与同步保障**：生成层（灯光、音效）需要监听音乐节拍（`music.beat`）来实现毫秒级的声光电同步。单进程内通过内存（如 Kotlin `SharedFlow`）传递事件几乎零开销，彻底避免了跨进程（IPC）带来的延迟和抖动（Jitter）。
2. **内存开销极低**：Android 系统中每个独立的进程都会有基础的内存开销（ART 虚拟机等）。在只有 4-6GB 预算的情况下，单进程能最大化利用内存，降低被系统 LMK (Low Memory Killer) 杀死的风险。
3. **开发与调试极快**：没有复杂的 AIDL 接口定义，没有 `Parcelable` 序列化开销，没有跨进程生命周期管理的烦恼。对象直接通过引用传递，Bug 极易定位。

---

## 2. 模块划分与职责定义

系统严格划分为三个核心层次，每个层次由不同的团队开发和调优。为了保证解耦，所有层级之间的通信**必须且只能**通过 `core-api` 模块中定义的数据类（Data Class）进行。

### 2.1 感知层 (Perception Layer)
*   **职责**：负责收集车内外的原始硬件数据，进行清洗、降噪和标准化。
*   **输入**：硬件底层数据（如 OMS 摄像头画面、Mic 音频流、车机时间、天气 API、车辆状态）。
*   **输出**：`StandardizedSignals`（标准化信号对象，包含置信度评分）。

### 2.2 语义层 (Semantic Layer)
*   **职责**：系统的“大脑”，负责理解标准化信号，结合用户画像和记忆系统，进行场景识别（快通道）和大模型推理（慢通道）。
*   **输入**：`StandardizedSignals`。
*   **输出**：`SceneDescriptor`（场景描述符，严格遵守 JSON Schema 契约）。

### 2.3 生成层 (Generation Layer / Execution)
*   **职责**：系统的“手脚”，负责将抽象的场景描述转化为具体的物理表现。
*   **输入**：`SceneDescriptor`。
*   **输出**：调用 Android 底层能力（播放音乐 ExoPlayer、显示 UI、调节氛围灯 CarPropertyManager、调节音量/音效 AudioEffect）。

---

## 3. Android 工程结构设计 (支持独立调试)

为了解决“不同团队如何独立调试效果、快速组合跑通”的痛点，Android Studio 工程（Gradle Multi-module）应采用**“1 个主 App + N 个 Demo App + N 个 SDK”**的结构：

```text
AiCabinEntertainment/
├── core-api/                   # 【核心契约层】定义 StandardizedSignals, SceneDescriptor 等数据类和接口 (所有模块依赖)
├── tools-mock/                 # 【Mock 工具库】提供各种场景的假数据生成器 (用于独立调试)
│
├── module-perception/          # 【感知层 SDK】(AAR) 内部实现传感器接入和信号处理
├── module-semantic/            # 【语义层 SDK】(AAR) 内部实现规则树和 LLM 推理
├── module-generation/          # 【生成层 SDK】(AAR) 内部实现声光电控制
│
├── app-demo-perception/        # 【感知层独立调试 App】(APK) UI 显示实时传感器数据和输出的 StandardizedSignals
├── app-demo-semantic/          # 【语义层独立调试 App】(APK) UI 提供按钮注入 Mock 信号，屏幕显示生成的 SceneDescriptor
├── app-demo-generation/        # 【生成层独立调试 App】(APK) UI 提供按钮下发 Mock SceneDescriptor，直接观察车内声光电效果
│
└── app-main/                   # 【全链路集成 App】(宿主 APK) 负责将三个 SDK 组装在一起，跑通真实全链路
```

---

## 4. 独立调试与快速组合策略

### 4.1 如何独立调试效果？ (Demo App 机制)
每个团队不需要等待其他团队的代码，可以直接编译运行属于自己那一层的 `app-demo-xxx` APK 到实车上进行调试。

*   **感知层团队**：运行 `app-demo-perception`。开车上路，观察屏幕上打印的 `StandardizedSignals` 是否准确反映了当前的环境和乘客状态。
*   **语义层团队**：运行 `app-demo-semantic`。在屏幕上点击“模拟深夜雨天通勤”按钮（由 `tools-mock` 提供假信号），观察大模型推理耗时，以及输出的 `SceneDescriptor` JSON 是否符合预期。
*   **生成层团队**：运行 `app-demo-generation`。在屏幕上点击“播放派对模式”按钮（直接下发预置的 `SceneDescriptor`），观察氛围灯是否闪烁、音乐是否劲爆、音量是否合适。

### 4.2 如何快速组合跑通？ (依赖注入与响应式流)
在 `app-main`（宿主 App）中，使用 Kotlin 的协程流（Flow）将三个模块像流水线一样串联起来。

**连接代码示例 (伪代码)：**

```kotlin
// 在 app-main 的 Application 或主 Service 中组装：

val perceptionEngine = PerceptionEngine()
val semanticEngine = SemanticEngine()
val generationEngine = GenerationEngine()

// 1. 感知层 -> 语义层
lifecycleScope.launch {
    perceptionEngine.standardizedSignalsFlow
        .collect { signals ->
            // 将标准化信号喂给语义层
            semanticEngine.processSignals(signals)
        }
}

// 2. 语义层 -> 生成层
lifecycleScope.launch {
    semanticEngine.sceneDescriptorFlow
        .collect { descriptor ->
            // 将场景描述符喂给生成层执行
            generationEngine.executeScene(descriptor)
        }
}
```

**优势**：
*   **极度解耦**：`module-perception` 完全不知道 `module-semantic` 的存在，它们只认识 `core-api` 里的数据类。
*   **一键切换 Mock/Real**：在 `app-main` 中，如果感知层硬件还没 ready，只需把 `perceptionEngine.standardizedSignalsFlow` 替换为 `MockPerceptionEngine().flow`，全链路依然可以跑通测试。

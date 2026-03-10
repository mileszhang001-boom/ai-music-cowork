# Android App 架构：APK 与 SDK 的分工、协作与通信指南

本文档专门用于描述“音乐推荐项目” Android 端架构中，**宿主 APK** 与 **业务 SDK** 之间的职责边界、合作关系以及通信方式。这是整个项目多团队并行开发、解耦和独立测试的核心基石。

---

## 1. 核心设计理念

在我们的架构中，遵循一个绝对的铁律：**“UI 与逻辑分离，权限归宿主，能力归 SDK”**。

*   **SDK (Software Development Kit)**：纯粹的业务逻辑黑盒。它不知道自己运行在什么界面下，也不知道用户是否授权了权限。它只负责接收输入数据，执行核心算法或硬件操作，然后输出结果。
*   **APK (Application Package)**：运行环境的提供者和调度者。它负责与用户交互（UI）、向系统申请权限、管理生命周期，并将不同的 SDK 像乐高积木一样串联起来。

---

## 2. 职责分工 (Responsibilities)

### 2.1 宿主 APK 的职责
无论是用于独立测试的 Demo APK (`app-demo-xxx`) 还是最终集成的全链路主 APK (`app-main`)，都必须承担以下职责：

1.  **UI 交互与展示**：提供所有的 Activity、Fragment 和 Compose 界面。
2.  **动态权限申请**：在 `AndroidManifest.xml` 中声明权限，并在运行时向用户弹窗申请权限（如 `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`）。
3.  **生命周期管理**：监听 App 的前后台切换，适时启动、暂停或销毁 SDK 的工作流。
4.  **依赖注入 (DI)**：
    *   为 SDK 提供 Android `Context`（通常是 `ApplicationContext`）。
    *   为 SDK 提供非标硬件接口的具体实现（例如，将真实车机的氛围灯控制类注入给生成层 SDK）。
5.  **模块串联 (仅限 `app-main`)**：将上一个 SDK 的输出数据，作为下一个 SDK 的输入数据进行传递（感知 -> 语义 -> 生成）。

### 2.2 业务 SDK 的职责
包括感知层 (`module-perception`)、语义层 (`module-semantic`) 和生成层 (`module-generation`)，必须承担以下职责：

1.  **核心业务逻辑**：执行数据清洗、大模型推理、音频播放等核心任务。
2.  **标准硬件调用**：在接收到 APK 传入的 `Context` 且权限就绪的前提下，调用 Android 标准 API（如 CameraX, AudioRecord, ExoPlayer）。
3.  **定义外部依赖接口**：对于无法标准化的硬件（如车机 VHAL、氛围灯），SDK 内部**禁止硬编码**，必须定义 Interface（如 `IAmbientLightController`），等待 APK 注入实现。
4.  **异常抛出**：当遇到权限被收回、硬件被占用或网络断开等异常时，SDK 不得自行弹窗（Toast/Dialog），必须通过回调或 Flow 将异常抛给 APK 处理。

---

## 3. 合作关系与通信方式 (Collaboration & Communication)

### 3.1 数据通信契约 (Data Contract)
SDK 之间**绝对禁止**直接互相依赖。所有的输入输出数据必须严格使用 `core/core-api` 模块中定义的 Kotlin Data Class（这些类 1:1 映射自 `docs/data_structures.md`）。

*   **感知层 SDK**：输入 `RawSignal` -> 输出 `StandardizedSignals`
*   **语义层 SDK**：输入 `StandardizedSignals` -> 输出 `SceneDescriptor`
*   **生成层 SDK**：输入 `SceneDescriptor` -> 输出 `EffectCommands`

### 3.2 初始化与依赖注入 (Initialization & DI)
APK 与 SDK 的第一次“握手”发生在初始化阶段。推荐使用 **Hilt** 进行依赖注入，或者手动传入。

**示例流程**：
1.  APK 启动，检查并申请麦克风权限。
2.  权限通过后，APK 实例化感知层 SDK，并传入 `Context`。
    ```kotlin
    // 在 APK 中
    val perceptionEngine = PerceptionEngine(applicationContext)
    ```
3.  对于生成层 SDK，APK 还需要注入非标硬件的实现。
    ```kotlin
    // 在 APK 中实现接口
    class CarVhalLightImpl : IAmbientLightController {
        override fun setLightColor(hexColor: String) { /* 调用真实车机 API */ }
    }
    
    // 注入给 SDK
    val generationEngine = GenerationEngine(applicationContext, CarVhalLightImpl())
    ```

### 3.3 运行时数据流转 (Runtime Data Flow)
在运行时，APK 负责监听 SDK 的输出，并将其传递给下一个 SDK。推荐使用 Kotlin `SharedFlow` 或 `StateFlow` 实现响应式通信。

**示例流程 (在 `app-main` 中)**：
```kotlin
// 1. 启动感知层，监听标准化信号
lifecycleScope.launch {
    perceptionEngine.standardizedSignalsFlow.collect { signals ->
        // 2. 将感知层的输出，作为语义层的输入
        semanticEngine.processSignals(signals)
    }
}

// 3. 监听语义层的输出
lifecycleScope.launch {
    semanticEngine.sceneDescriptorFlow.collect { scene ->
        // 4. 将语义层的输出，作为生成层的输入
        generationEngine.executeScene(scene)
    }
}
```

### 3.4 异常通信 (Error Handling)
SDK 内部发生错误时，通过密封类 (Sealed Class) 或特定的 Error Flow 通知 APK。

```kotlin
// 在 SDK 中定义状态
sealed class EngineState {
    object Running : EngineState()
    data class Error(val code: Int, val message: String) : EngineState()
}

// 在 APK 中监听并处理 UI
lifecycleScope.launch {
    perceptionEngine.stateFlow.collect { state ->
        when (state) {
            is EngineState.Error -> {
                // APK 负责弹出 Dialog 或 Toast 提示用户
                showErrorDialog(state.message)
            }
            is EngineState.Running -> { /* 更新 UI 状态 */ }
        }
    }
}
```

---

## 4. 总结

通过这种设计，我们实现了：
1.  **极高的可测试性**：每个 SDK 都可以脱离真实硬件，通过注入 Mock 实现（如 `MockLightImpl`）在 Demo APK 中独立运行和测试。
2.  **团队并行开发**：感知、语义、生成三个团队互不干扰，只要遵守 `core-api` 的数据契约，就可以独立推进进度。
3.  **灵活的部署能力**：未来如果需要将某个 SDK 替换为云端实现，或者适配不同的车型，只需要在 APK 层修改注入的实现类，SDK 内部代码无需任何改动。
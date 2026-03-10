# 音乐推荐项目 Android 端多团队协作与 AI 辅助开发指南

本文档旨在指导参与“音乐推荐项目” Android 端开发的各个团队（感知、语义、生成、集成）及其 AI 辅助工具（如 Trae, Cursor, Copilot 等），如何在一个统一的架构下高效协作、快速集成已有能力，并确保最终交付的质量。

---

## 1. 架构核心原则 (AI 必须遵守的铁律)

所有团队的 AI 助手在生成代码或修改架构时，**必须**遵守以下原则：

1.  **契约至上**：所有模块间的通信数据结构**必须**严格遵循 `docs/data_structures.md`。在 Android 端，这些结构被 1:1 映射为 `core/core-api` 模块中的 Kotlin Data Class。**禁止**任何团队私自修改 `core-api` 的数据结构。
2.  **绝对解耦**：
    *   SDK 模块（如 `module-perception`）**禁止**包含任何 UI 代码（Activity/Fragment）。
    *   SDK 模块**禁止**直接互相依赖。
    *   SDK 模块**禁止**在内部申请 Android 动态权限。
3.  **依赖反转 (DI)**：SDK 需要的 `Context` 或非标硬件能力（如车机氛围灯），必须通过接口定义，由宿主 APK（Demo 或 Main）在初始化时注入。
4.  **统一版本管理**：**禁止**在 `build.gradle.kts` 中硬编码依赖版本。所有依赖必须通过 `gradle/libs.versions.toml` (Version Catalog) 引入。

---

## 2. 团队工作区与职责划分

项目采用按特性分包 (Feature-based modularization) 的结构。请确保你的 AI 助手只在分配给你的目录下工作。

```text
Android_app/
├── core/core-api/                   # [公共] 契约层 (Data Classes & Interfaces)
├── features/perception/             # [感知团队] 负责收集传感器数据 -> StandardizedSignals
├── features/semantic/               # [语义团队] 负责 StandardizedSignals -> SceneDescriptor
├── features/generation/             # [生成团队] 负责 SceneDescriptor -> 硬件执行指令
└── app/app-main/                    # [集成团队] 负责串联 SDK、权限申请、真实硬件注入
```

---

## 3. 如何快速集成已有的能力代码？

如果你的团队在之前的原型阶段或独立项目中，已经跑通了某些能力（例如：读取车机 VHAL 信息、ExoPlayer 播放逻辑、调节氛围灯），请按照以下步骤将其“移植”到当前架构中：

### 场景 A：标准 Android 能力 (如 ExoPlayer, CameraX, AudioRecord)
这些能力通常只需要标准的 Android `Context`。
1.  **迁移代码**：将已有的核心逻辑类（如 `AudioPlayerManager`）直接拷贝到你的 SDK 模块（如 `module-generation`）中。
2.  **改造初始化**：确保该类的初始化方法接收 `Context` 作为参数。
3.  **对接契约**：在 SDK 的入口类中，将接收到的契约数据（如 `SceneDescriptor`）解析后，调用你的 `AudioPlayerManager` 进行播放。
4.  **Demo 验证**：在你的 `app-demo-xxx` 中，传入 `ApplicationContext` 初始化 SDK，验证功能是否正常。

### 场景 B：非标硬件能力 (如车机氛围灯、特定 VHAL 信号)
这些能力通常依赖于特定车型的私有 API 或 AIDL，**绝对不能**直接写死在 SDK 中。
1.  **定义接口 (SDK 端)**：在你的 SDK 模块或 `core-api` 中定义一个标准的 Kotlin Interface。
    ```kotlin
    // 在 module-generation 中定义
    interface IAmbientLightController {
        fun setLightColor(hexColor: String)
        fun setLightPattern(pattern: String)
    }
    ```
2.  **SDK 内部调用**：SDK 内部只针对这个 Interface 编程，不关心具体实现。
3.  **迁移实现 (集成端)**：将你已有的、调用真实车机 API 的代码（如 `CarVhalLightImpl`）拷贝到 **`app/app-main/`** 模块中，并让它实现 `IAmbientLightController` 接口。
4.  **依赖注入 (集成端)**：在 `app-main` 初始化 SDK 时，将 `CarVhalLightImpl` 的实例注入给 SDK。
5.  **Mock 测试 (Demo 端)**：在你的 `app-demo-xxx` 中，实现一个 `MockLightImpl`（例如只打印 Log），注入给 SDK 用于独立调试。

---

## 4. AI 辅助开发 Prompt 建议

当团队成员使用 AI 助手开发时，建议在 Prompt 中加入以下上下文，以确保 AI 生成的代码符合架构规范：

> **通用 Prompt 前缀：**
> "你现在正在参与『音乐推荐项目』的 Android 端开发。请严格遵守以下架构规范：
> 1. 我们使用 Kotlin 和 Coroutines。
> 2. 依赖管理使用 `gradle/libs.versions.toml`。
> 3. 模块间通信必须使用 `core-api` 中定义的 Data Class。
> 4. SDK 模块不能包含 UI 和权限申请，必须通过依赖注入获取 Context 和硬件接口。
> 
> 当前任务是：[描述你的具体任务，例如：在 module-perception 中实现 AudioRecord 录音逻辑，并将音量转化为 StandardizedSignals 中的 volume_level]"

---

## 5. 提交代码前的 Check 节点

在向主分支提交代码前，团队必须（或要求 AI 助手）完成以下检查：

1.  **边界清晰**：确认没有修改其他团队的目录。
2.  **依赖合规**：确认没有在 `build.gradle.kts` 中硬编码版本号。
3.  **契约对齐**：确认 SDK 的输入输出严格使用了 `core-api` 的 Data Class。
4.  **Demo 可运行**：确认本团队的 `app-demo-xxx` 可以独立编译并运行，且能通过 Mock 数据验证核心逻辑。
5.  **无 UI 污染**：确认 SDK 模块中没有引入 `Activity`, `Fragment` 或权限请求弹窗。
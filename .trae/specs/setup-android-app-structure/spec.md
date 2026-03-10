# Android App 架构与模块拆分规范 (Setup Android App Structure Spec)

## 1. 目标与背景 (Why)
基于 Node.js 原型的成功验证，项目正式进入 Android 端开发阶段。为了满足多团队并行开发、独立调试、快速集成的需求，我们需要在 `Android_app/` 目录下搭建一个标准的“单主 APK + 多 SDK + 多 Demo APK”的 Gradle 多模块工程。
最核心的诉求是：**严格遵循现有的数据结构契约，实现模块间的绝对解耦，并确保每个模块都能独立验证其效果。**

## 2. 核心通信参数与契约 (Communication Parameters & Contracts)
为了保证多个 SDK 和 APK 之间的通信解耦，所有模块间的通信**必须**基于 `/Users/mi/Desktop/音乐推荐项目/docs/data_structures.md` 中定义的 JSON 数据结构。
我们将建立一个独立的契约层 (`core-api` 模块)，将 JSON 结构 1:1 映射为 Kotlin Data Class。

*   **Layer 1 输出 / Layer 2 输入**: `StandardizedSignals` (标准化信号)
*   **Layer 2 输出 / Layer 3 输入**: `SceneDescriptor` (场景描述符)
*   **Layer 3 输出**: `EffectCommands` (效果指令)

**通信原则**：
1.  SDK 之间**禁止**直接互相依赖。
2.  所有 SDK 仅依赖 `core-api` 模块。
3.  APK 负责将上一个 SDK 的输出（Data Class）作为下一个 SDK 的输入进行传递。

## 3. 模块职责划分 (Module Responsibilities)

### 3.1 契约层 (core-api)
*   **任务**：作为全系统的“通用语言”。**绝对禁止**包含任何业务逻辑，只包含数据模型（Data Class）和接口定义（Interface）。
*   **规范**：必须严格遵循 `docs/data_structures.md`。定义 `RawSignal`, `StandardizedSignals`, `SceneDescriptor`, `EffectCommands` 等类。

### 3.2 SDK 模块 (业务逻辑层)
各个 SDK 负责核心的业务逻辑处理，不包含任何 UI 界面。

*   **`module-perception` (感知层 SDK)**
    *   **任务**：对接车机底层 API（VHAL、摄像头、麦克风），收集原始数据，进行清洗、降噪和置信度计算。
    *   **输入**：`RawSignal` (原始信号数组)
    *   **输出**：`StandardizedSignals` (标准化信号)
*   **`module-semantic` (语义层 SDK)**
    *   **任务**：系统的“大脑”。包含规则引擎、大模型推理逻辑、记忆系统。负责将标准化信号转化为场景意图。
    *   **输入**：`StandardizedSignals` (来自感知层)
    *   **输出**：`SceneDescriptor` (场景描述符)
*   **`module-generation` (生成层 SDK)**
    *   **任务**：系统的“手脚”。包含编排协调器、音乐播放器 (ExoPlayer)、灯光控制、音效控制。根据场景描述，生成具体执行指令并调用 Android 底层硬件 API。
    *   **输入**：`SceneDescriptor` (来自语义层)
    *   **输出**：`EffectCommands` (效果指令)

### 3.3 APK 模块 (运行与测试层)
APK 负责提供运行环境、UI 界面、权限申请以及模块间的串联。为了保证独立调试，每个 SDK 都配备一个专属的 Demo APK。

*   **`app-demo-perception` (感知层独立测试 APK)**
    *   **任务**：提供 UI 界面，实时显示当前收集到的传感器数据，以及转换后的 `StandardizedSignals` JSON 字符串。用于实车路测感知准确率。
*   **`app-demo-semantic` (语义层独立测试 APK)**
    *   **任务**：提供 UI 面板，允许开发者手动构造或加载预设的 `StandardizedSignals`（Mock 数据），点击“推理”按钮后，展示生成的 `SceneDescriptor`。用于脱离硬件测试大模型和规则树。
*   **`app-demo-generation` (生成层独立测试 APK)**
    *   **任务**：提供 UI 面板，内置多种典型的 `SceneDescriptor` JSON 文件。点击对应场景，直接观察车内的音乐、灯光、音效变化。用于调优声光电同步效果。
*   **`app-main` (全链路集成主 APK)**
    *   **任务**：宿主程序。负责将三个 SDK 实例化，并使用 Kotlin `SharedFlow` 或 `LiveData` 将它们串联起来（感知 -> 语义 -> 生成）。处理全局的生命周期和权限申请。

## 4. 权限与硬件能力管理 (Permissions & Hardware Capabilities)
在涉及 Android 底层硬件（如摄像头、麦克风、音频播放、车机氛围灯）时，必须严格遵循“**UI 与逻辑分离，权限归宿主，能力归 SDK**”的原则。

### 4.1 职责划分
*   **APK 职责 (宿主)**：
    *   **权限声明与申请**：在 `AndroidManifest.xml` 中声明所需权限，并在 UI 层（Activity/Fragment）处理动态运行时权限的申请（如 `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`）。
    *   **生命周期管理**：根据前后台状态，控制 SDK 的启动与暂停。
    *   **依赖注入 (DI)**：为 SDK 提供 `Context`，或提供特定硬件接口的实现。
*   **SDK 职责 (能力实现)**：
    *   **标准 Android 能力**：SDK 内部实现具体的硬件调用逻辑（如使用 CameraX 捕获图像、AudioRecord 录音、ExoPlayer 播放音频）。SDK 必须在 APK 传入 `Context` 且权限就绪后才能工作。
    *   **非标车机能力 (如氛围灯、VHAL)**：SDK 内部**不应**硬编码特定车型的 API。SDK 应在 `core-api` 或自身模块中定义接口（如 `IAmbientLightController`），由 APK 根据具体运行环境（实车或 Demo）提供具体实现并注入给 SDK（依赖反转）。

### 4.2 快速集成已有能力 (Integrating Existing Capabilities)
如果团队在之前的原型阶段或独立项目中，已经跑通了某些能力（例如：读取设备信息、播放内容、调节灯光），请按照以下步骤将其“移植”到当前架构中：

1.  **标准 Android 能力 (如 ExoPlayer, CameraX, AudioRecord)**：
    *   **迁移代码**：将已有的核心逻辑类（如 `AudioPlayerManager`）直接拷贝到对应的 SDK 模块（如 `module-generation`）中。
    *   **改造初始化**：确保该类的初始化方法接收 `Context` 作为参数。
    *   **对接契约**：在 SDK 的入口类中，将接收到的契约数据（如 `SceneDescriptor`）解析后，调用你的 `AudioPlayerManager` 进行播放。
    *   **Demo 验证**：在对应的 `app-demo-xxx` 中，传入 `ApplicationContext` 初始化 SDK，验证功能是否正常。

2.  **非标硬件能力 (如车机氛围灯、特定 VHAL 信号)**：
    *   **定义接口 (SDK 端)**：在 SDK 模块或 `core-api` 中定义一个标准的 Kotlin Interface（例如 `IAmbientLightController`）。
    *   **SDK 内部调用**：SDK 内部只针对这个 Interface 编程，不关心具体实现。
    *   **迁移实现 (集成端)**：将已有的、调用真实车机 API 的代码（如 `CarVhalLightImpl`）拷贝到 **`app/app-main/`** 模块中，并让它实现 `IAmbientLightController` 接口。
    *   **依赖注入 (集成端)**：在 `app-main` 初始化 SDK 时，将 `CarVhalLightImpl` 的实例注入给 SDK。
    *   **Mock 测试 (Demo 端)**：在 `app-demo-xxx` 中，实现一个 `MockLightImpl`（例如只打印 Log），注入给 SDK 用于独立调试。

### 4.3 通信与调用流程
1.  **初始化阶段**：APK 启动 -> 检查并申请权限 -> 权限通过 -> 实例化 SDK 并传入 `Context` 和必要的接口实现（如 `val perceptionSdk = PerceptionEngine(context)`）。
2.  **运行阶段**：SDK 使用传入的 `Context` 开启硬件流（如麦克风监听），并将数据转化为 `StandardizedSignals` 输出。
3.  **异常处理**：如果 SDK 运行中发现权限被收回或硬件被占用，通过回调或 Kotlin Flow 抛出异常事件，由 APK 捕获并展示错误 UI。

## 5. 依赖库体系与版本管理 (Library Ecosystem & Versioning)
为了保证多个团队在开发不同 SDK 时不会产生依赖冲突，并且保证整个项目的兼容性和稳定性，我们必须采用统一的依赖库体系和版本管理方案。

### 5.1 核心技术栈与库选择
*   **语言**：Kotlin (推荐使用最新稳定版，如 1.9.x 或 2.0.x)。
*   **异步与并发**：Kotlin Coroutines & Flow (用于处理传感器数据流、大模型推理回调等)。
*   **依赖注入**：Hilt (Google 官方推荐，用于在 `app-main` 中向各个 SDK 注入 `Context` 和硬件接口实现)。
*   **网络请求**：Retrofit + OkHttp (用于语义层调用云端大模型 API)。
*   **JSON 解析**：Kotlinx Serialization (轻量级，与 Kotlin Data Class 完美契合，用于解析 `docs/data_structures.md` 定义的结构)。
*   **多媒体**：Media3 (ExoPlayer) (用于生成层播放音频)。
*   **相机**：CameraX (用于感知层获取车内/车外图像)。

### 5.2 统一版本管理 (Version Catalog)
**绝对禁止**在各个模块的 `build.gradle.kts` 中硬编码依赖库的版本号。
必须在根目录下使用 Gradle Version Catalog (`gradle/libs.versions.toml`) 统一管理所有依赖的版本。
*   **优势**：任何团队想要升级某个库（例如 Retrofit），只需在 `libs.versions.toml` 中修改一处，所有依赖该库的模块都会同步升级，彻底杜绝版本冲突。

## 6. 目录结构与多团队协作 (Directory Structure & Collaboration)
为了方便多个团队（感知团队、语义团队、生成团队、集成团队）分别更新代码而不产生合并冲突，`Android_app/` 目录下的结构必须清晰隔离。

```text
Android_app/
├── gradle/libs.versions.toml       # [全局] 统一的依赖版本管理文件
├── build.gradle.kts                # [全局] 根构建脚本
├── settings.gradle.kts             # [全局] 模块注册表
│
├── core/                           # [公共基础层]
│   └── core-api/                   # 契约层 (Data Classes & Interfaces)
│
├── features/                       # [业务特性层 - 各团队独立工作区]
│   ├── perception/                 # 感知团队目录
│   │   ├── module-perception/      # 感知 SDK
│   │   └── app-demo-perception/    # 感知 Demo APK
│   │
│   ├── semantic/                   # 语义团队目录
│   │   ├── module-semantic/        # 语义 SDK
│   │   └── app-demo-semantic/      # 语义 Demo APK
│   │
│   └── generation/                 # 生成团队目录
│       ├── module-generation/      # 生成 SDK
│       └── app-demo-generation/    # 生成 Demo APK
│
└── app/                            # [集成层]
    └── app-main/                   # 宿主主 APK (集成团队负责)
```

**协作规范**：
1.  **代码隔离**：感知团队只允许修改 `features/perception/` 目录下的代码，语义团队只允许修改 `features/semantic/` 目录下的代码。
2.  **契约先行**：如果需要修改 `core-api` 中的数据结构，必须经过所有团队的 Review 和架构师的批准，并同步更新 `docs/data_structures.md`。
3.  **独立构建**：每个团队可以在自己的 Demo APK 目录下执行 `assembleDebug`，无需编译其他团队的代码，极大提升编译速度。

## 7. 测试与效果保证策略 (Testing Strategy)
为了保证测试效果和模块的独立性，采取以下策略：

1.  **契约测试 (Contract Testing)**：在 `core-api` 中编写单元测试，确保 Kotlin Data Class 序列化/反序列化后的 JSON 与 `docs/data_structures.md` 完全一致。任何字段的缺失或类型错误都会导致测试失败。
2.  **边界 Mock 测试 (Mocking)**：每个 SDK 必须提供一个 `Mock` 实现类。例如，在 `app-main` 中，如果感知层硬件未就绪，可以一键注入 `MockPerceptionEngine`，持续输出假信号，以保证下游模块的联调不被阻塞。
3.  **独立 Demo 验证 (Isolated Demo Apps)**：通过 `app-demo-*` 系列 APK，开发人员可以在不依赖其他团队进度的情况下，独立验证自己负责的 SDK 逻辑。
4.  **Node.js 资产复用 (Asset Reuse)**：将 Node.js 阶段沉淀的测试用例（输入 JSON -> 预期输出 JSON）直接转化为 Android 端的 JUnit 测试用例，确保 Android 端逻辑与原型一致。

### 7.1 集成测试完整方案与 Check 节点 (Integration Testing Plan)
在各个 SDK 独立开发完成后，集成团队需要在 `app-main` 中进行全链路集成测试。以下是完整的测试方案和 Check 节点：

**阶段一：契约与 Mock 联调 (Contract & Mock Integration)**
*   **目标**：验证三个 SDK 的数据流转是否畅通，不依赖真实硬件。
*   **操作**：在 `app-main` 中注入 `MockPerceptionEngine`（输出固定 `StandardizedSignals`），连接真实的 `module-semantic`，再连接 `MockGenerationEngine`（只打印 `EffectCommands`）。
*   **Check 节点 1**：语义层能否正确接收 Mock 信号并输出预期的 `SceneDescriptor`？
*   **Check 节点 2**：生成层能否正确接收 `SceneDescriptor` 并输出预期的 `EffectCommands`？

**阶段二：单点硬件接入 (Single Hardware Integration)**
*   **目标**：逐个替换 Mock 模块为真实硬件模块，隔离排查问题。
*   **操作 A (感知接入)**：注入真实的 `module-perception`，保持生成层为 Mock。
*   **Check 节点 3**：在真实车机/手机上，感知层能否正确申请权限并持续输出 `StandardizedSignals`？
*   **操作 B (生成接入)**：恢复感知层为 Mock，注入真实的 `module-generation` 和真实的硬件接口实现（如 `CarVhalLightImpl`）。
*   **Check 节点 4**：生成层能否根据 Mock 的 `SceneDescriptor` 正确播放音乐和调节氛围灯？

**阶段三：全链路实车路测 (Full-Chain Road Test)**
*   **目标**：在真实环境中验证端到端的效果。
*   **操作**：在 `app-main` 中注入所有真实的 SDK 和硬件实现。
*   **Check 节点 5 (性能与稳定性)**：系统连续运行 1 小时，是否存在内存泄漏？CPU 占用是否在合理范围内？
*   **Check 节点 6 (端到端延迟)**：从环境变化（如进入隧道）到音乐/灯光发生改变，整体延迟是否满足产品要求（例如 < 2秒）？
*   **Check 节点 7 (异常恢复)**：在运行中手动关闭麦克风权限或断开网络，系统能否优雅降级并给出提示，而不是直接崩溃？

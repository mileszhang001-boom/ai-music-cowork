# 任务列表 (Tasks)

- [x] Task 1: 初始化 Android 根工程与契约层 (`core-api`)
  - [x] SubTask 1.1: 在 `Android_app/` 目录下创建根工程，配置基础的 Android Gradle Plugin (AGP) 和 Kotlin 插件。
  - [x] SubTask 1.2: 创建 `gradle/libs.versions.toml` 文件，统一管理 Kotlin, Coroutines, Hilt, Retrofit, CameraX, Media3 等核心库的版本。
  - [x] SubTask 1.3: 按照多团队协作规范，创建 `core/`, `features/`, `app/` 目录结构。
  - [x] SubTask 1.4: 在 `core/` 目录下创建 `core-api` Android Library 模块。
  - [x] SubTask 1.5: 严格按照 `docs/data_structures.md`，在 `core-api` 中定义 `RawSignal`, `StandardizedSignals`, `SceneDescriptor`, `EffectCommands` 等 Kotlin Data Class。
  - [x] SubTask 1.6: 编写单元测试，验证 Data Class 与 JSON 字符串的序列化/反序列化是否完全符合文档规范。

- [ ] Task 2: 搭建感知层 (`features/perception/`)
  - [ ] SubTask 2.1: 在 `features/perception/` 下创建 `module-perception` SDK 模块（仅依赖 `core-api`），定义输入 `RawSignal` 输出 `StandardizedSignals` 的接口。
  - [ ] SubTask 2.2: 在 SDK 中实现 CameraX、AudioRecord 和 Location 的核心逻辑，要求必须传入 `Context` 才能初始化。
  - [ ] SubTask 2.3: 在 `features/perception/` 下创建 `app-demo-perception` APK 模块（依赖 `module-perception`）。
  - [ ] SubTask 2.4: 在 Demo APK 中实现动态权限申请（相机、麦克风、定位），权限通过后将 `Context` 注入 SDK 并启动。
  - [ ] SubTask 2.5: 在 Demo APK 中实现 UI，用于展示传感器数据和输出的标准化信号，确保可独立路测。

- [ ] Task 3: 搭建语义层 (`features/semantic/`)
  - [ ] SubTask 3.1: 在 `features/semantic/` 下创建 `module-semantic` SDK 模块（仅依赖 `core-api`），定义输入 `StandardizedSignals` 输出 `SceneDescriptor` 的接口。
  - [ ] SubTask 3.2: 在 SDK 中集成 Retrofit 和 Kotlinx Serialization，实现与云端大模型的通信逻辑。
  - [ ] SubTask 3.3: 在 `features/semantic/` 下创建 `app-demo-semantic` APK 模块（依赖 `module-semantic`）。
  - [ ] SubTask 3.4: 在 Demo APK 中实现 UI，允许手动输入 Mock 的 `StandardizedSignals`，并展示生成的 `SceneDescriptor`，确保可独立验证大模型和规则逻辑。

- [ ] Task 4: 搭建生成层 (`features/generation/`)
  - [ ] SubTask 4.1: 在 `features/generation/` 下创建 `module-generation` SDK 模块（仅依赖 `core-api`），定义输入 `SceneDescriptor` 输出 `EffectCommands` 的接口。
  - [ ] SubTask 4.2: 在 SDK 中集成 Media3 (ExoPlayer)，实现音频播放和 AudioManager 音量控制逻辑（需传入 `Context`）。
  - [ ] SubTask 4.3: 在 SDK 中定义非标硬件接口（如 `IAmbientLightController` 氛围灯控制器），等待外部注入。
  - [ ] SubTask 4.4: 在 `features/generation/` 下创建 `app-demo-generation` APK 模块（依赖 `module-generation`）。
  - [ ] SubTask 4.5: 在 Demo APK 中实现 `IAmbientLightController`（可先用 Log 或屏幕颜色模拟），并注入给 SDK。
  - [ ] SubTask 4.6: 在 Demo APK 中实现 UI，提供预置的 `SceneDescriptor` 按钮，点击后直接触发声光电效果，确保可独立调优硬件表现。

- [ ] Task 5: 搭建全链路集成层 (`app/app-main/`)
  - [ ] SubTask 5.1: 在 `app/` 目录下创建 `app-main` APK 模块，依赖上述三个 SDK 模块。
  - [ ] SubTask 5.2: 在 `app-main` 中集成 Hilt，实现全局的依赖注入（注入 Context 和硬件接口）。
  - [ ] SubTask 5.2: 在 `app-main` 中实现全局的动态权限申请（合并感知层和生成层所需的所有权限）。
  - [ ] SubTask 5.3: 在 `app-main` 中实现真实的硬件接口（如对接真实车机 VHAL 的氛围灯控制），并注入给生成层 SDK。
  - [ ] SubTask 5.4: 在 `app-main` 中实现数据流转逻辑，将三个 SDK 串联（感知 -> 语义 -> 生成），确保通信参数严格遵循 `core-api` 的定义。

- [ ] Task 6: 建立测试与 Mock 机制
  - [ ] SubTask 6.1: 为每个 SDK 编写 Mock 实现类（如 `MockPerceptionEngine`），以便在集成测试时替换真实实现。
  - [ ] SubTask 6.2: 编写脚本或手动将 Node.js 项目中的 JSON 模板和测试用例同步到 Android 工程的 `assets` 目录中，用于单元测试。

# 任务依赖关系 (Task Dependencies)
- Task 2, 3, 4 依赖于 Task 1 (契约层完成后，各 SDK 可并行开发)
- Task 5 依赖于 Task 2, 3, 4
- Task 6 贯穿于整个开发周期，依赖于 Task 1

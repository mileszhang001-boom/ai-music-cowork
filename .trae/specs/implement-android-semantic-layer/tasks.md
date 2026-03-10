# 任务列表 (Tasks)

- [x] Task 1: 初始化语义层模块结构
  - [x] SubTask 1.1: 在 `Android_app/features/semantic/` 下创建 `module-semantic` (Android Library) 和 `app-demo-semantic` (Android App)。
  - [x] SubTask 1.2: 在 `settings.gradle.kts` 中确保这两个模块已被 include。
  - [x] SubTask 1.3: 配置 `module-semantic` 的 `build.gradle.kts`，添加对 `:core:core-api`、Retrofit、OkHttp、Kotlinx Serialization 和 Coroutines 的依赖。
  - [x] SubTask 1.4: 配置 `app-demo-semantic` 的 `build.gradle.kts`，添加对 `:features:semantic:module-semantic` 的依赖。

- [x] Task 2: 迁移模板库 (Template Library)
  - [x] SubTask 2.1: 将 Node.js 项目 `src/core/templateLibrary/templates/` 下的 JSON 文件拷贝到 `module-semantic/src/main/assets/templates/` 目录中。
  - [x] SubTask 2.2: 在 `module-semantic` 中实现 `TemplateManager` 类，负责从 assets 中读取并解析 JSON 为 `SceneDescriptor` 对象。

- [x] Task 3: 迁移规则引擎 (Rules Engine)
  - [x] SubTask 3.1: 在 `module-semantic` 中实现 `RulesEngine` 类。
  - [x] SubTask 3.2: 将 Node.js `src/core/rulesEngine/index.js` 中的硬编码规则（如早晨通勤、深夜驾驶、疲劳提醒等）用 Kotlin 重新实现。
  - [x] SubTask 3.3: 编写单元测试，验证给定特定的 `StandardizedSignals`，规则引擎能正确返回对应的 `template_id`。

- [x] Task 4: 迁移大模型客户端 (LLM Client)
  - [x] SubTask 4.1: 在 `module-semantic` 中定义 Retrofit 接口 `LlmApiService`，用于调用云端大模型（如 DeepSeek/OpenAI 兼容接口）。
  - [x] SubTask 4.2: 实现 `PromptBuilder` 类，将 `StandardizedSignals` 转化为大模型所需的 Prompt 字符串（参考 Node.js `src/core/llm/prompts.js`）。
  - [x] SubTask 4.3: 实现 `LlmClient` 类，封装网络请求、重试逻辑以及将大模型返回的 JSON 字符串解析为 `SceneDescriptor` 的逻辑。

- [x] Task 5: 封装语义引擎门面 (Semantic Engine Facade)
  - [x] SubTask 5.1: 创建 `SemanticEngine` 类作为 SDK 的唯一入口。
  - [x] SubTask 5.2: 实现核心工作流：接收 `StandardizedSignals` -> 优先过 `RulesEngine` -> 若命中则从 `TemplateManager` 取数据 -> 若未命中则调用 `LlmClient` -> 返回最终的 `SceneDescriptor`。

- [x] Task 6: 搭建 Demo APK 验证环境
  - [x] SubTask 6.1: 在 `app-demo-semantic` 中实现一个简单的 UI（如使用 Compose），包含一个下拉菜单选择预设的 Mock 信号（如"模拟早晨"、"模拟疲劳"）。
  - [x] SubTask 6.2: 实现一个"执行推理"按钮，点击后调用 `SemanticEngine`。
  - [x] SubTask 6.3: 在 UI 上展示输入的 JSON 和输出的 `SceneDescriptor` JSON，验证整个语义层链路是否畅通。

# 任务依赖关系 (Task Dependencies)
- Task 2, 3, 4 可以并行开发，均依赖于 Task 1。
- Task 5 依赖于 Task 2, 3, 4。
- Task 6 依赖于 Task 5。
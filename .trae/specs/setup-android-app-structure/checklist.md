# 检查清单 (Checklist)

- [x] Android 根工程已在 `Android_app/` 目录下成功初始化，Gradle 配置有效。
- [x] **版本管理验证**：已创建 `gradle/libs.versions.toml`，所有模块的 `build.gradle.kts` 均通过 Version Catalog 引用依赖，无硬编码版本号。
- [x] **目录结构验证**：工程目录已按照 `core/`, `features/`, `app/` 的规范进行拆分，各团队的工作区清晰隔离。
- [x] `core-api` 模块已在 `core/` 目录下创建，并且包含了严格遵循 `docs/data_structures.md` 的 `StandardizedSignals`, `SceneDescriptor`, `EffectCommands` 等数据类。
- [x] `core-api` 模块中包含了针对数据类 JSON 序列化/反序列化的单元测试，且测试通过。
- [ ] `module-perception`, `module-semantic`, `module-generation` 三个 SDK 模块已在各自的 `features/` 子目录下创建，并且**仅**依赖于 `core-api`，没有互相依赖。
- [ ] `app-demo-perception`, `app-demo-semantic`, `app-demo-generation` 三个独立的 Demo APK 模块已在各自的 `features/` 子目录下创建，并分别依赖对应的 SDK。
- [ ] 每个 Demo APK 都能独立运行，并能通过 Mock 数据或真实数据验证对应 SDK 的功能。
- [ ] `app-main` 主 APK 模块已在 `app/` 目录下创建，依赖了三个 SDK，并成功实现了数据流的串联（感知 -> 语义 -> 生成）。
- [ ] 已为各个 SDK 提供了 Mock 实现类，确保在部分模块未完成时，主 APK 依然可以进行联调测试。
- [ ] **权限管理验证**：所有动态权限申请逻辑均在 APK 模块中实现，SDK 模块内部不包含任何请求权限的 UI 代码。
- [ ] **硬件解耦验证**：SDK 模块通过接收 `Context` 或依赖注入的 Interface（如 `IAmbientLightController`）来调用硬件，未硬编码特定车型的非标 API。
- [ ] **集成测试验证 (阶段一)**：在 `app-main` 中使用 Mock 感知和 Mock 生成，验证语义层数据流转畅通（Check 节点 1, 2）。
- [ ] **集成测试验证 (阶段二)**：在 `app-main` 中分别单点接入真实感知层和真实生成层，验证硬件调用正常（Check 节点 3, 4）。
- [ ] **集成测试验证 (阶段三)**：在真实车机/手机上完成全链路路测，性能、延迟和异常恢复均符合要求（Check 节点 5, 6, 7）。
- [ ] 整个 Gradle 工程可以成功同步 (Sync) 且没有错误。

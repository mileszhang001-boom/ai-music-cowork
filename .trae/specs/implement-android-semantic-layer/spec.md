# Android 语义层 (Semantic Layer) 搭建与迁移 Spec

## 为什么做 (Why)
Android 基础架构和契约层 (`core-api`) 已经搭建完成。现在需要开始具体业务模块的开发。语义层作为系统的“大脑”，负责将感知层传来的标准化信号 (`StandardizedSignals`) 转化为具体的场景意图 (`SceneDescriptor`)。
我们需要在 Android 端搭建语义层的 SDK 和 Demo APK，并将 Node.js 原型中已经验证过的能力（规则引擎、大模型集成、模板库）同步迁移到 Android 代码中。

## 核心变更 (What Changes)
- 在 `Android_app/features/semantic/` 目录下创建 `module-semantic` (SDK) 和 `app-demo-semantic` (Demo APK)。
- 将 Node.js 的 `src/core/rulesEngine` 逻辑迁移为 Kotlin 的规则匹配引擎。
- 将 Node.js 的 `src/core/llm` 逻辑迁移为基于 Retrofit 的大模型 API 调用客户端。
- 将 Node.js 的 `src/core/templateLibrary` 逻辑迁移为 Android 的本地模板管理器（读取 assets 中的 JSON）。
- 在 Demo APK 中实现一个 UI，允许开发者手动输入或选择 Mock 的 `StandardizedSignals`，并展示生成的 `SceneDescriptor`。

## 影响范围 (Impact)
- 受影响的规范：遵循 `docs/data_structures.md` 和 `Android_app/apk_sdk_architecture_guide.md`。
- 受影响的代码：新增 `Android_app/features/semantic/` 目录下的所有代码。

## 新增需求 (ADDED Requirements)

### 需求 1: 语义层 SDK 核心接口
系统必须提供一个 `SemanticEngine` 类，作为语义层的唯一入口。
#### 场景: 成功处理信号
- **WHEN** 宿主 APK 调用 `SemanticEngine.processSignals(signals: StandardizedSignals)`
- **THEN** 引擎应结合规则、模板和大模型，返回一个符合契约的 `SceneDescriptor` 对象。

### 需求 2: 规则与模板引擎迁移
系统必须在 Android 端实现与 Node.js 原型一致的规则匹配逻辑。
#### 场景: 匹配到预设场景
- **WHEN** 传入的信号满足“早晨通勤”的规则（如时间在 6:00-9:00，单人驾驶）
- **THEN** 引擎应直接从本地模板库中读取并返回对应的 `SceneDescriptor`，无需调用大模型。

### 需求 3: 大模型 (LLM) 客户端迁移
系统必须在 Android 端实现与云端大模型的通信能力。
#### 场景: 规则未命中，触发大模型推理
- **WHEN** 传入的信号未命中任何本地规则
- **THEN** 引擎应使用 Retrofit 将信号组装为 Prompt 发送给大模型 API，并将返回的 JSON 解析为 `SceneDescriptor`。

### 需求 4: 独立 Demo 验证
系统必须提供一个独立的 Demo APK 用于测试语义层。
#### 场景: 开发者测试语义逻辑
- **WHEN** 开发者运行 `app-demo-semantic` 并点击“发送 Mock 信号”按钮
- **THEN** 界面应展示输入的 JSON 和引擎输出的 `SceneDescriptor` JSON，证明逻辑闭环。
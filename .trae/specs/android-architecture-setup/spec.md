# Android Architecture Setup Spec

## Why
基于当前项目的现状（Node.js 版本的全链路 Mock 和 Phase 3 核心模块集成测试已全部通过），**完全可以开始 Android 端的构建了**。
核心逻辑（如 Scene Descriptor 契约、Feedback Report 结构、ACK 机制、状态机流转）已经得到了验证，接口定义已冻结。

针对高通 SA8295P 平台（Android 14 Automotive）和 4-6GB 的内存预算，我们需要确定语义层（Team A）与执行层各引擎（Team B/C/D）之间的连接方式。

## What Changes
- **确立 Android 技术架构**：采用 **1个宿主 APK + 多个业务 SDK (AAR)** 的架构。
- **为什么不选 3 个独立 APK（进程间通信 IPC）？**
  1. **性能与延迟**：`plan.md` 中明确指定了引擎间通信使用 `Kotlin SharedFlow`。如果跨进程，必须使用 AIDL/Binder，这会带来极高的 IPC 开销，无法满足 `music.beat` 这种毫秒级（如 30fps）的声光同步需求。
  2. **内存限制**：系统留给本项目的内存预算仅 4-6GB。多个 APK 意味着启动多个 Android Runtime (ART) 虚拟机，会造成严重的内存浪费，增加被系统杀后台的风险。
- **为什么选 SDK (AAR) 方式？**
  1. **解耦开发**：Team A、B、C、D 可以各自维护独立的 Android Library Module（SDK），甚至在不同的 Git 仓库中开发，互不干扰。
  2. **零延迟通信**：所有 SDK 最终打包进同一个宿主 APK 运行在同一个进程中，可以完美使用 `Kotlin SharedFlow` 进行内存级的高频事件分发。
  3. **契约调用**：各 SDK 之间仅通过暴露的 Kotlin Interface 传递 JSON 字符串或 Data Class，严格遵守 `spec-all.md` 的设计边界。

## Impact
- Affected specs: Android 端的工程结构与模块划分。
- Affected code: 新建 Android Gradle 多模块工程。

## ADDED Requirements
### Requirement: Single Host APK with Multi-Module SDKs
系统 SHALL 作为一个单一的 Android 应用程序（APK）运行，内部划分为多个独立的 Gradle 模块（SDK）。

#### Scenario: High-frequency Event Sync
- **WHEN** 内容引擎（Content Engine）发出 `music.beat` 节拍事件
- **THEN** 灯光引擎（Lighting Engine）通过同进程的 `SharedFlow` 瞬间接收到该事件，无 IPC 延迟，实现完美的声光同步。

#### Scenario: Independent Engine Development
- **WHEN** Team C 需要测试灯光引擎的新动效
- **THEN** 他们只需编译 `lighting-engine` 模块，并在自己的测试 App 中运行，无需依赖完整的系统环境。

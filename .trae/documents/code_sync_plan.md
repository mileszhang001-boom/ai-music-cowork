# 代码同步与提交计划

## 1. 现状评估
*   **本地环境**: 包含 `src/android_app/layer1-sdk` (临时重构模块) 和 `src/android_app/app` (依赖临时模块的 Demo)。此环境已验证可运行。
*   **远程环境**: `Android_app/features/perception` (规范架构模块)，但 AI Prompt 是旧的。
*   **Git 状态**: `git pull` 后存在大量未提交的变更（包括本地临时模块和远程拉取的新模块）。

## 2. 目标
*   **首要目标**: 将本地验证通过的关键逻辑（AI Prompt 优化）同步到规范架构模块中。
*   **次要目标**: 提交所有代码，确保远程仓库拥有最新的逻辑和完整的（尽管暂时冗余）代码结构。
*   **非目标**: 暂时不强行删除本地临时模块或重构 `app` 依赖，以免破坏当前可运行的 Demo 环境。

## 3. 执行步骤

### 步骤一：同步 AI Prompt
*   读取 `src/android_app/layer1-sdk/.../AIClient.kt` 中的 `analyzeInternalCamera` 方法 Prompt。
*   将该 Prompt 及其 JSON 解析逻辑应用到 `Android_app/features/perception/.../AIClient.kt` 中。

### 步骤二：提交代码
*   执行 `git add .` 将所有变更（包括本地临时模块和更新后的规范模块）暂存。
*   执行 `git commit`，详细说明包含“本地临时 Demo 环境”和“规范架构模块同步”。
*   执行 `git push` 推送到远程仓库。

## 4. 验证
*   无需重新编译 `app`，因为我们只修改了备用的规范模块，未触及当前运行的依赖。
*   确保 Git 状态干净。

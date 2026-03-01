# Layer3 生成层 Android SDK Spec

## Why
当前 Layer3 (Effects Layer) 的代码是 Node.js 实现，需要将其移植到 Android 平台，生成一个符合 Android 标准的 SDK，以便集成到 Android_app 中。该 SDK 将负责音乐推荐、灯光控制和音频处理等核心功能。

## What Changes
- 创建 `layer3-api` 模块，定义公共接口和数据模型
- 创建 `layer3-sdk` 模块，实现核心业务逻辑
- 移植 ContentEngine（音乐推荐引擎）到 Kotlin
- 移植 LightingEngine（灯光引擎）到 Kotlin
- 移植 AudioEngine（音频引擎）到 Kotlin
- 创建 Demo APK 用于独立测试
- 集成到 Android_app 主项目

## Impact
- Affected code:
  - `src/android_app/layer3-api/` (新增)
  - `src/android_app/layer3-sdk/` (新增)
  - `src/android_app/app-demo-layer3/` (新增)
  - `src/android_app/settings.gradle` (修改)
  - `src/android_app/build.gradle` (修改)

## ADDED Requirements

### Requirement: Layer3 API 模块定义
系统 SHALL 创建 `layer3-api` 模块，定义公共接口和数据模型：

#### Scenario: 数据模型定义
- **WHEN** 定义数据模型时
- **THEN** 应包含以下核心数据类：
  - `SceneDescriptor` - 场景描述符（输入）
  - `EffectCommands` - 效果命令（输出）
  - `Playlist` - 歌单数据
  - `LightingConfig` - 灯光配置
  - `AudioConfig` - 音频配置

#### Scenario: 接口定义
- **WHEN** 定义接口时
- **THEN** 应包含以下核心接口：
  - `IGenerationEngine` - 生成引擎接口
  - IContentEngine - 内容引擎接口
  - `ILightingEngine` - 灯光引擎接口
  - `IAudioEngine` - 音频引擎接口
  - `IAmbientLightController` - 氛围灯控制器接口（非标硬件，由 APK 注入）

### Requirement: Layer3 SDK 模块实现
系统 SHALL 创建 `layer3-sdk` 模块，实现核心业务逻辑：

#### Scenario: SDK 初始化
- **WHEN** APK 调用 `Layer3SDK.init(context, config)`
- **THEN** SDK 应初始化所有引擎
- **AND** 接受 `ApplicationContext` 和 `Layer3Config`
- **AND** 不申请任何权限（权限由 APK 申请）

#### Scenario: 场景执行
- **WHEN** APK 调用 `engine.executeScene(descriptor)`
- **THEN** SDK 应执行以下流程：
  1. 调用 ContentEngine 生成歌单
  2. 调用 LightingEngine 生成灯光配置
  3. 调用 AudioEngine 生成音频配置
  4. 返回 `EffectCommands` 结果

#### Scenario: 音乐推荐
- **WHEN** ContentEngine 接收到场景描述
- **THEN** 应根据场景类型推荐音乐
- **AND** 支持本地 JSON 音乐库
- **AND** 支持缓存机制
- **AND** 支持艺术家多样性限制

#### Scenario: 灯光控制
- **WHEN** LightingEngine 接收到场景描述
- **THEN** 应根据场景类型生成灯光配置
- **AND** 支持场景化主题（ocean, sunset, rainy 等）
- **AND** 支持动态颜色调整

### Requirement: 音乐库加载
系统 SHALL 支持从本地 JSON 文件加载音乐库：

#### Scenario: 加载音乐库
- **WHEN** SDK 初始化时
- **THEN** 应从 `assets/music_library.json` 加载音乐库
- **OR** 从外部存储路径加载（可配置）

#### Scenario: 音乐库格式
- **GIVEN** 音乐库 JSON 文件
- **WHEN** 解析音乐库
- **THEN** 应包含以下字段：
  - `id` - 歌曲 ID
  - `title` - 歌曲标题
  - `artist` - 艺术家
  - `genre` - 流派
  - `energy` - 能量值 (0.0-1.0)
  - `valence` - 情绪值 (0.0-1.0)
  - `bpm` - 节拍
  - `duration_ms` - 时长（毫秒）

### Requirement: 缓存机制
系统 SHALL 支持歌单缓存：

#### Scenario: 缓存命中
- **WHEN** 相同场景再次请求
- **THEN** 应返回缓存的歌单
- **AND** 响应时间 < 50ms

#### Scenario: 缓存过期
- **WHEN** 缓存超过 7 天
- **THEN** 应自动刷新缓存

### Requirement: Demo APK
系统 SHALL 创建 Demo APK 用于独立测试：

#### Scenario: 测试场景选择
- **WHEN** 用户打开 Demo APK
- **THEN** 应显示场景列表
- **AND** 允许用户选择场景进行测试

#### Scenario: 测试结果显示
- **WHEN** 场景执行完成
- **THEN** 应显示推荐的歌单
- **AND** 应显示灯光配置
- **AND** 应显示音频配置

### Requirement: 异常处理
系统 SHALL 正确处理异常：

#### Scenario: 音乐库加载失败
- **WHEN** 音乐库文件不存在或格式错误
- **THEN** 应抛出 `MusicLibraryException`
- **AND** 通过 `StateFlow` 通知 APK

#### Scenario: 推荐失败
- **WHEN** 无法生成推荐
- **THEN** 应返回兜底歌单
- **AND** 记录错误日志

## MODIFIED Requirements

### Requirement: settings.gradle 更新
更新 `src/android_app/settings.gradle`，添加新模块：

```gradle
include ':layer3-api'
include ':layer3-sdk'
include ':app-demo-layer3'
```

### Requirement: build.gradle 更新
更新 `src/android_app/build.gradle`，添加依赖配置：

```gradle
dependencies {
    implementation project(':layer3-api')
    implementation project(':layer3-sdk')
}
```

## REMOVED Requirements
无

# App-Main 集成开发 Spec

## Why
需要将 Layer1（感知层）、Layer2（语义层）、Layer3（生成层）三个 SDK 集成到主 APK 中，实现全链路数据流转验证。

## What Changes
- 创建 `app-main` 模块，集成三个 SDK
- 实现 TTS 播报服务（使用 Android 标准 TTS）
- 实现音乐播放时的音频闪避（TTS 播报时压低音乐音量）
- 配置歌曲资源路径为设备外部存储
- 实现数据流调试界面（分模块打印各层输出）
- **暂不实现**氛围灯控制（权限被锁定）
- **暂不实现**完整 UI 设计

## Impact
- Affected specs: 无
- Affected code: `Android_app/app/app-main/`（新建）

## ADDED Requirements

### Requirement: App-Main 模块创建
系统应当创建 `app-main` 模块作为主 APK 入口。

#### Scenario: 模块初始化
- **WHEN** 用户启动应用
- **THEN** 系统初始化三个 SDK（PerceptionEngine, SemanticEngine, Layer3SDK）
- **AND** 系统申请必要权限（CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION）

### Requirement: TTS 播报服务
系统应当使用 Android 标准 TTS API 实现语音播报功能。

#### Scenario: TTS 播报触发
- **WHEN** SemanticEngine 输出 SceneDescriptor 包含 announcement 字段
- **THEN** 系统调用 TTS 服务播报文本
- **AND** 系统压低当前音乐播放音量

#### Scenario: 音频闪避
- **WHEN** TTS 开始播报
- **THEN** 音乐音量降低至 30%
- **WHEN** TTS 播报结束
- **THEN** 音乐音量恢复至原水平

### Requirement: 歌曲资源存储
系统应当从设备外部存储读取歌曲资源。

#### Scenario: 资源路径配置
- **GIVEN** 歌曲资源已推送到设备外部存储
- **WHEN** ContentEngine 请求播放歌曲
- **THEN** 系统从 `/sdcard/Music/AiMusic/` 路径读取音频文件

### Requirement: 数据流调试界面
系统应当提供调试界面展示各层 SDK 的输出数据。

#### Scenario: Layer1 数据展示
- **WHEN** PerceptionEngine 输出 StandardizedSignals
- **THEN** 界面实时显示 JSON 格式的信号数据

#### Scenario: Layer2 数据展示
- **WHEN** SemanticEngine 输出 SceneDescriptor
- **THEN** 界面显示场景类型、意图、提示信息

#### Scenario: Layer3 数据展示
- **WHEN** Layer3SDK 输出 EffectCommands
- **THEN** 界面显示播放列表、音频配置信息

### Requirement: 氛围灯控制暂缓
系统**暂不实现**氛围灯控制功能。

#### Scenario: 氛围灯跳过
- **WHEN** LightingEngine 生成灯光配置
- **THEN** 系统记录日志但不执行实际控制
- **AND** 界面显示"氛围灯控制已禁用"提示

## MODIFIED Requirements
无

## REMOVED Requirements
无

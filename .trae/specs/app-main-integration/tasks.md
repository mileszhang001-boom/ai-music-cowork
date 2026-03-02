# Tasks

## Task 1: 创建 app-main 模块基础结构
- [x] Task 1.1: 在 `Android_app/app/` 下创建 `app-main` 模块目录
- [x] Task 1.2: 配置 `build.gradle.kts`，添加对三个 SDK 的依赖
- [x] Task 1.3: 在 `settings.gradle.kts` 中注册模块
- [x] Task 1.4: 创建 `AndroidManifest.xml`，声明必要权限

## Task 2: 实现权限管理
- [x] Task 2.1: 创建 `PermissionManager` 类处理动态权限申请
- [x] Task 2.2: 实现 CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION 权限申请流程
- [x] Task 2.3: 处理权限拒绝情况，显示提示信息

## Task 3: 实现 TTS 播报服务
- [x] Task 3.1: 创建 `TtsService` 类，封装 Android TextToSpeech API
- [x] Task 3.2: 实现 `speak(text: String)` 方法
- [x] Task 3.3: 实现 `stop()` 方法
- [x] Task 3.4: 添加 TTS 初始化状态监听

## Task 4: 实现音频闪避功能
- [x] Task 4.1: 创建 `AudioDuckManager` 类
- [x] Task 4.2: 实现 TTS 开始时降低音乐音量至 30%
- [x] Task 4.3: 实现 TTS 结束后恢复音乐音量
- [x] Task 4.4: 使用 AudioManager 监听音频焦点变化

## Task 5: 配置歌曲资源路径
- [x] Task 5.1: 在 `LocalMusicIndex` 中配置外部存储路径 `/sdcard/Music/AiMusic/`
- [x] Task 5.2: 添加 READ_EXTERNAL_STORAGE 权限声明
- [x] Task 5.3: 实现存储权限申请流程

## Task 6: 创建 MainViewModel 集成三个 SDK
- [x] Task 6.1: 创建 `MainViewModel` 类
- [x] Task 6.2: 初始化 `PerceptionEngine`，监听 `standardizedSignalsFlow`
- [x] Task 6.3: 初始化 `SemanticEngine`，监听 `sceneDescriptorFlow`
- [x] Task 6.4: 初始化 `Layer3SDK`，监听 `effectCommandsFlow`
- [x] Task 6.5: 实现 SDK 生命周期管理（start/stop/destroy）

## Task 7: 实现数据流调试界面
- [x] Task 7.1: 创建 `MainActivity` 使用 Compose UI
- [x] Task 7.2: 实现 Layer1 数据展示区域（StandardizedSignals JSON）
- [x] Task 7.3: 实现 Layer2 数据展示区域（SceneDescriptor 关键字段）
- [x] Task 7.4: 实现 Layer3 数据展示区域（播放列表、音频配置）
- [x] Task 7.5: 实现数据实时刷新

## Task 8: 集成 TTS 播报到数据流
- [x] Task 8.1: 在 `MainViewModel` 中监听 `sceneDescriptorFlow`
- [x] Task 8.2: 当 `announcement` 字段非空时触发 TTS 播报
- [x] Task 8.3: TTS 播报前调用 `AudioDuckManager` 降低音量
- [x] Task 8.4: TTS 结束后恢复音量

## Task 9: 处理氛围灯控制禁用
- [x] Task 9.1: 创建 `DisabledAmbientLightController` 实现 `IAmbientLightController`
- [x] Task 9.2: 所有方法返回 "功能已禁用" 结果
- [x] Task 9.3: 在界面显示氛围灯禁用提示

## Task 10: 验证全链路数据流
- [x] Task 10.1: 启动应用，验证权限申请流程
- [x] Task 10.2: 验证 Layer1 数据采集并显示
- [x] Task 10.3: 验证 Layer2 场景推理并显示
- [x] Task 10.4: 验证 TTS 播报触发
- [x] Task 10.5: 验证 Layer3 播放列表生成
- [x] Task 10.6: 验证音乐播放功能

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 和 Task 4 可并行
- Task 6 依赖 Task 1, Task 2
- Task 7 依赖 Task 6
- Task 8 依赖 Task 3, Task 4, Task 6
- Task 9 依赖 Task 1
- Task 10 依赖所有前置任务

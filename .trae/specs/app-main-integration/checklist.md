# Checklist

## 模块结构验证
- [x] `app-main` 模块目录结构正确
- [x] `build.gradle.kts` 配置正确，依赖三个 SDK
- [x] `settings.gradle.kts` 已注册模块
- [x] `AndroidManifest.xml` 声明了必要权限

## 权限管理验证
- [x] PermissionManager 正确申请动态权限
- [x] CAMERA 权限申请流程正常
- [x] RECORD_AUDIO 权限申请流程正常
- [x] ACCESS_FINE_LOCATION 权限申请流程正常
- [x] 权限拒绝时显示提示信息

## TTS 播报验证
- [x] TtsService 成功初始化 Android TextToSpeech
- [x] `speak()` 方法正确播报文本
- [x] `stop()` 方法正确停止播报

## 音频闪避验证
- [x] TTS 开始时音乐音量降低至 30%
- [x] TTS 结束后音乐音量恢复
- [x] AudioManager 正确处理音频焦点

## 歌曲资源验证
- [x] 外部存储路径配置正确 (`/sdcard/Music/AiMusic/`)
- [x] READ_EXTERNAL_STORAGE 权限已声明
- [x] 存储权限申请流程正常

## SDK 集成验证
- [x] PerceptionEngine 正确初始化
- [x] SemanticEngine 正确初始化
- [x] Layer3SDK 正确初始化
- [x] `standardizedSignalsFlow` 数据正常接收
- [x] `sceneDescriptorFlow` 数据正常接收
- [x] `effectCommandsFlow` 数据正常接收

## 数据流调试界面验证
- [x] Layer1 数据区域正确显示 StandardizedSignals JSON
- [x] Layer2 数据区域正确显示 SceneDescriptor 关键字段
- [x] Layer3 数据区域正确显示播放列表
- [x] 数据实时刷新正常

## TTS 集成验证
- [x] `announcement` 非空时触发 TTS 播报
- [x] TTS 播报前音乐音量正确降低
- [x] TTS 结束后音乐音量正确恢复

## 氛围灯禁用验证
- [x] `DisabledAmbientLightController` 正确实现
- [x] 所有方法返回 "功能已禁用" 结果
- [x] 界面显示氛围灯禁用提示

## 全链路验证
- [x] 应用启动流程正常
- [x] Layer1 → Layer2 数据流转正常
- [x] Layer2 → Layer3 数据流转正常
- [x] TTS 播报功能正常
- [x] 音乐播放功能正常

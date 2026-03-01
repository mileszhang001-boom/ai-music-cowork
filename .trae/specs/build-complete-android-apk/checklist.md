# 检查清单 (Checklist)

## 感知层 (Perception Layer)
- [ ] `module-perception` 模块已创建并配置正确的依赖
- [ ] `PerceptionEngine` 入口类已实现，支持传入 `Context` 初始化
- [ ] 音量传感器能正确获取车内音量级别
- [ ] 位置传感器能正确获取车速
- [ ] 时间传感器能正确获取系统时间和日期类型
- [ ] `SignalAggregator` 能将各传感器数据聚合为 `StandardizedSignals`
- [ ] 使用 `StateFlow` 暴露实时数据流
- [ ] `app-demo-perception` 能独立运行并展示传感器数据

## 生成层 (Generation Layer)
- [ ] `module-generation` 模块已创建并配置 Media3 依赖
- [ ] `GenerationEngine` 入口类已实现
- [ ] `MusicLibrary` 能扫描本地存储的歌曲文件
- [ ] 歌曲索引和缓存机制正常工作
- [ ] `MusicPlayer` 能播放本地歌曲
- [ ] `IAmbientLightController` 接口已定义
- [ ] `AudioManagerVolumeController` 能控制音量
- [ ] `EffectExecutor` 能根据 `EffectCommands` 执行硬件操作
- [ ] `app-demo-generation` 能独立运行并展示执行效果

## 主 APK (Main App)
- [ ] `app-main` 模块已创建并依赖三个 SDK 模块
- [ ] Hilt 依赖注入已配置
- [ ] 全链路数据流转正常：感知 → 语义 → 生成
- [ ] 动态权限申请正常工作
- [ ] 前后台切换时 SDK 能正确暂停/恢复
- [ ] 主界面 UI 正常显示当前场景和播放状态
- [ ] 手动模式切换功能正常

## 集成测试
- [ ] 端到端测试用例通过
- [ ] 各场景下的数据流转正常
- [ ] 异常情况能正确处理并提示用户
- [ ] 内存占用在合理范围内
- [ ] 电池消耗在合理范围内

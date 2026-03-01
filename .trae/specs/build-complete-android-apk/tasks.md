# 任务列表 (Tasks)

## 阶段一：感知层 SDK 开发 (Perception Layer)

- [ ] Task 1: 创建感知层 SDK 模块结构
  - [ ] SubTask 1.1: 在 `features/perception/` 下创建 `module-perception` Android Library 模块
  - [ ] SubTask 1.2: 配置 `build.gradle.kts`，添加对 `:core:core-api` 的依赖
  - [ ] SubTask 1.3: 创建 `PerceptionEngine` 入口类，定义输入输出接口

- [ ] Task 2: 实现传感器数据采集
  - [ ] SubTask 2.1: 实现 `AudioSensorCollector` - 使用 `AudioManager` 获取车内音量级别
  - [ ] SubTask 2.2: 实现 `LocationSensorCollector` - 使用 `LocationManager` 获取车速
  - [ ] SubTask 2.3: 实现 `TimeSensorCollector` - 获取系统时间和日期类型
  - [ ] SubTask 2.4: 实现 `WeatherSensorCollector` - 获取天气信息（可 Mock 或调用天气 API）
  - [ ] SubTask 2.5: 实现 `CameraSensorCollector` - 使用 CameraX 获取图像（可选）

- [ ] Task 3: 实现数据标准化输出
  - [ ] SubTask 3.1: 实现 `SignalAggregator` - 将各传感器数据聚合为 `StandardizedSignals`
  - [ ] SubTask 3.2: 实现置信度计算逻辑
  - [ ] SubTask 3.3: 使用 Kotlin `StateFlow` 暴露实时数据流

- [ ] Task 4: 创建感知层 Demo APK
  - [ ] SubTask 4.1: 创建 `app-demo-perception` APK 模块
  - [ ] SubTask 4.2: 实现动态权限申请（定位、麦克风、相机）
  - [ ] SubTask 4.3: 实现 UI 展示传感器数据和 `StandardizedSignals` JSON

## 阶段二：生成层 SDK 开发 (Generation Layer)

- [ ] Task 5: 创建生成层 SDK 模块结构
  - [ ] SubTask 5.1: 在 `features/generation/` 下创建 `module-generation` Android Library 模块
  - [ ] SubTask 5.2: 配置 `build.gradle.kts`，添加 Media3 (ExoPlayer) 依赖
  - [ ] SubTask 5.3: 创建 `GenerationEngine` 入口类

- [ ] Task 6: 实现本地歌曲管理
  - [ ] SubTask 6.1: 实现 `MusicLibrary` - 扫描本地存储的歌曲文件
  - [ ] SubTask 6.2: 定义 `Track` 数据模型（包含 id, title, artist, path, duration, energy, genres）
  - [ ] SubTask 6.3: 实现歌曲索引和缓存机制
  - [ ] SubTask 6.4: 实现按流派、能量、节奏等维度的筛选功能

- [ ] Task 7: 实现音乐播放器
  - [ ] SubTask 7.1: 实现 `MusicPlayer` - 封装 Media3 ExoPlayer
  - [ ] SubTask 7.2: 实现播放列表管理
  - [ ] SubTask 7.3: 实现播放状态监听和回调

- [ ] Task 8: 实现硬件控制接口
  - [ ] SubTask 8.1: 定义 `IAmbientLightController` 接口（氛围灯控制）
  - [ ] SubTask 8.2: 定义 `IVolumeController` 接口（音量控制）
  - [ ] SubTask 8.3: 实现 `AudioManagerVolumeController` - 使用 Android AudioManager
  - [ ] SubTask 8.4: 实现 `EffectExecutor` - 根据 `EffectCommands` 执行硬件操作

- [ ] Task 9: 创建生成层 Demo APK
  - [ ] SubTask 9.1: 创建 `app-demo-generation` APK 模块
  - [ ] SubTask 9.2: 实现 Mock 的 `IAmbientLightController`（屏幕颜色模拟）
  - [ ] SubTask 9.3: 实现 UI 展示 `SceneDescriptor` 输入和执行效果

## 阶段三：主 APK 集成 (Main App Integration)

- [ ] Task 10: 创建主 APK 模块结构
  - [ ] SubTask 10.1: 在 `app/` 下创建 `app-main` APK 模块
  - [ ] SubTask 10.2: 配置依赖三个 SDK 模块
  - [ ] SubTask 10.3: 集成 Hilt 依赖注入框架

- [ ] Task 11: 实现全链路数据流转
  - [ ] SubTask 11.1: 创建 `MainViewModel` - 管理全局状态
  - [ ] SubTask 11.2: 实现数据流：`PerceptionEngine` → `SemanticEngine` → `GenerationEngine`
  - [ ] SubTask 11.3: 使用 `StateFlow` 或 `SharedFlow` 实现响应式数据传递

- [ ] Task 12: 实现权限和生命周期管理
  - [ ] SubTask 12.1: 实现动态权限申请（合并所有需要的权限）
  - [ ] SubTask 12.2: 实现前后台切换时的 SDK 暂停/恢复
  - [ ] SubTask 12.3: 实现异常处理和用户提示

- [ ] Task 13: 实现主界面 UI
  - [ ] SubTask 13.1: 实现主界面 - 显示当前场景和播放状态
  - [ ] SubTask 13.2: 实现场景切换动画
  - [ ] SubTask 13.3: 实现手动模式切换（自动/手动）

## 阶段四：测试与优化

- [ ] Task 14: 集成测试
  - [ ] SubTask 14.1: 编写端到端测试用例
  - [ ] SubTask 14.2: 测试各场景下的数据流转
  - [ ] SubTask 14.3: 测试异常情况处理

- [ ] Task 15: 性能优化
  - [ ] SubTask 15.1: 优化歌曲扫描性能
  - [ ] SubTask 15.2: 优化内存占用
  - [ ] SubTask 15.3: 优化电池消耗

# 任务依赖关系 (Task Dependencies)
- Task 2, 3 依赖于 Task 1
- Task 4 依赖于 Task 3
- Task 6, 7, 8 依赖于 Task 5
- Task 9 依赖于 Task 8
- Task 11, 12, 13 依赖于 Task 10
- Task 14, 15 依赖于 Task 13

# 并行开发建议
- **感知层团队**：负责 Task 1-4
- **生成层团队**：负责 Task 5-9
- **集成团队**：负责 Task 10-15
- 感知层和生成层可以并行开发，最后由集成团队整合

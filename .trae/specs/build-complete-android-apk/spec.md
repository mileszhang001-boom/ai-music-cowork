# 构建完整 Android APK 规范 (Build Complete Android APK Spec)

## 背景 (Why)
各子模块已完成可用性验证：
- **感知层**：已跑通，可从硬件获取数据并转化为 `StandardizedSignals`
- **语义层**：SDK 已完成，规则引擎 + 大模型推理可用，输出正确的 `SceneDescriptor`
- **内容生成层**：算法已验证，可生成正确的 `EffectCommands`
- **硬件接口层**：氛围灯、音量控制已调通（使用标准 Android API）

现在需要构建完整的 Android APK，将所有已验证的模块集成到一起，实现端到端的音乐推荐体验。

## 核心变更 (What Changes)
1. **感知层 SDK** (`module-perception`)：实现传感器数据采集，输出 `StandardizedSignals`
2. **生成层 SDK** (`module-generation`)：实现音乐播放、氛围灯控制、音量控制
3. **主 APK** (`app-main`)：集成所有 SDK，实现全链路数据流转
4. **歌曲资源管理**：扫描并管理本地 2000 首歌曲

## 前置依赖 (Prerequisites)
1. ✅ `core-api` 契约层已完成
2. ✅ `module-semantic` 语义层 SDK 已完成
3. ✅ `app-demo-semantic` 语义层 Demo APK 已完成
4. 📁 2000 首本地歌曲文件（需确认存储位置）
5. 📁 Node.js 原型代码（作为算法参考）

## 技术方案 (Technical Approach)

### 1. 感知层 SDK (`module-perception`)
**输入**：硬件传感器（麦克风、摄像头、定位、时间）
**输出**：`StandardizedSignals`

**实现要点**：
- 使用 Android `AudioManager` 获取车内音量级别
- 使用 Android `LocationManager` 获取车速和位置
- 使用 `CameraX` 获取车内/车外图像（可选）
- 使用 `SensorManager` 获取加速度等传感器数据
- 时间、天气等通过系统 API 获取

### 2. 生成层 SDK (`module-generation`)
**输入**：`SceneDescriptor`
**输出**：`EffectCommands`（并执行）

**实现要点**：
- **音乐播放**：使用 Media3 (ExoPlayer) 播放本地歌曲
- **歌曲管理**：扫描本地存储的 2000 首歌曲，建立索引
- **氛围灯控制**：定义 `IAmbientLightController` 接口，由 APK 注入实现
- **音量控制**：使用 Android `AudioManager` API

### 3. 主 APK (`app-main`)
**职责**：
- 集成三个 SDK（感知、语义、生成）
- 实现数据流转：感知 → 语义 → 生成
- 管理权限和生命周期
- 注入硬件接口实现

## 影响范围 (Impact)
- 新增 `features/perception/module-perception`
- 新增 `features/perception/app-demo-perception`
- 新增 `features/generation/module-generation`
- 新增 `features/generation/app-demo-generation`
- 新增 `app/app-main`

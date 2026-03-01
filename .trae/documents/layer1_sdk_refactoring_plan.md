# Layer 1 感知层 SDK 重构与封装计划

## 1. 架构设计与分析

### 1.1 现状分析

当前 Layer 1 功能紧耦合在 `src/android_app` 的 `app` 模块中，主要包含以下组件：

* **数据采集**: `SensorManager` (GPS, Mic), `CameraSource` (CameraX), `IpCameraSource` (MJPEG/HTTP).

* **数据处理**: `LocalImageAnalyzer` (本地算法), `AIClient` (云端推理).

* **服务集成**: `WeatherService` (Open-Meteo).

* **数据模型**: `Layer1Model.kt` (定义了 Signals, VehicleSignal 等).

* **业务逻辑**: `MainActivity` 中包含大量调度逻辑 (Loop, UI update).

### 1.2 目标架构

遵循 `apk_sdk_architecture_guide.md` 和 `vehicle_interface_api.md`，将 Layer 1 拆分为独立 SDK。

**模块划分**:

1. **`layer1-api`**: 定义公共接口、数据模型 (`StandardizedSignals` 等)、配置类 (`Layer1Config`)、回调接口 (`ILayer1EventListener`)。此模块无具体业务实现，保持轻量。
2. **`layer1-sdk`** **(impl)**: 实现 `layer1-api` 定义的接口。包含所有具体逻辑 (`IpCameraSource`, `AIClient` 等)。依赖 `layer1-api`。
3. **`app-demo`**: 宿主 APK，用于集成测试 SDK。负责权限申请、UI 展示、SDK 初始化与配置。

### 1.3 核心 API 设计

* **初始化**: `Layer1SDK.init(context, config)`

* **配置**: `Layer1Config` (包含 IP Camera 地址, API Key, 刷新频率等)

* **启动/停止**: `Layer1SDK.startPerception()`, `Layer1SDK.stopPerception()`

* **数据回调**: `Flow<StandardizedSignals>` 或 Listener。

* **动态配置**: `updateIpCameraConfig(url, auth)`

## 2. 实施步骤

### 阶段一：模块结构搭建与数据模型迁移

1. **创建 Module**:

   * 在 `src/android_app` 下新建 `layer1-api` (Android Library).

   * 在 `src/android_app` 下新建 `layer1-sdk` (Android Library).
2. **迁移数据模型**:

   * 将 `com.example.layer1.data` 下的所有数据类 (`StandardizedSignals`, `Signals`, etc.) 移动到 `layer1-api` 模块。

   * 确保数据类符合 `vehicle_interface_api.md` 规范。
3. **定义接口契约**:

   * 在 `layer1-api` 中定义 `IPerceptionEngine` 接口。

   * 定义 `Layer1Config` 数据类 (Builder 模式)。

   * 定义错误码 `Layer1Error`。

### 阶段二：核心功能迁移与解耦 (Refactoring)

1. **迁移工具类至** **`layer1-sdk`**:

   * 移动 `AIClient`, `CameraSource`, `IpCameraSource`, `LocalImageAnalyzer`, `SensorManager`, `WeatherService`, `ConfidenceValidator` 到 `layer1-sdk`。
2. **重构** **`SensorManager`**:

   * 移除对 Activity Context 的依赖，改为 Application Context。

   * 确保权限检查逻辑由外部（宿主）保证，内部仅做检查或抛出异常。
3. **重构** **`CameraSource`**:

   * 确保 `LifecycleOwner` 正确传递或内部管理。
4. **实现** **`PerceptionEngine`**:

   * 在 `layer1-sdk` 中实现 `IPerceptionEngine`。

   * 封装主循环逻辑 (原 `MainActivity` 中的 `startLoop`) 到 SDK 内部的 `CoroutineScope`。

   * 实现 `Flow` 数据发射。
5. **配置化改造**:

   * 改造 `IpCameraSource` 和 `AIClient`，使其支持从 `Layer1Config` 读取 IP 地址和 API Key，而不是硬编码。

### 阶段三：SDK 封装与统一入口

1. **单例封装**:

   * 创建 `Layer1SDK` 单例类（Facade 模式），持有 `IPerceptionEngine` 实例。

   * 实现 `init`, `destroy`, `updateConfig` 方法。
2. **生命周期管理**:

   * 确保 `destroy` 时释放所有资源 (Camera, AudioRecord, Thread Pools)。
3. **混淆规则**:

   * 在 `layer1-sdk/consumer-rules.pro` 中添加规则，保留 API 接口和数据模型不被混淆。

### 阶段四：构建配置与发布准备

1. **Gradle 配置**:

   * 配置 `maven-publish` 插件。

   * 配置 Dokka 生成 JavaDoc/KotlinDoc。

   * 设置语义化版本号 (e.g., `1.0.0`).
2. **依赖管理**:

   * 确保 `layer1-sdk` 通过 `api project(':layer1-api')` 暴露接口。

### 阶段五：集成验证 (Demo App)

1. **改造** **`app`** **模块**:

   * 移除原有的业务代码。

   * 添加对 `layer1-sdk` 的依赖。

   * 在 `MainActivity` 中实现权限申请。

   * 调用 `Layer1SDK.init` 和 `startPerception`。

   * 订阅 SDK 输出的数据并展示。
2. **功能验证**:

   * 验证 IP Camera 动态配置是否生效。

   * 验证 AI 推理是否正常。

   * 验证天气和传感器数据。

### 阶段六：文档与交付

1. **编写文档**:

   * `README.md`: 集成指南、配置说明、权限列表。

   * `CHANGELOG.md`: 版本变更记录。
2. **输出交付物**:

   * 生成 AAR, POM, Sources JAR, JavaDoc JAR。

## 3. 关键检查点

* [ ] **权限分离**: SDK 内部不直接请求权限，而是检查权限并在缺失时通过回调通知宿主或抛出异常。

* [ ] **动态配置**: IP Camera 地址必须支持运行时修改。

* [ ] **接口隔离**: 宿主 App 不应引用 `layer1-sdk` 中的具体实现类，只依赖 `layer1-api` 中的接口。

* [ ] **线程安全**: 确保所有硬件调用在后台线程，回调在主线程（或指定线程）。


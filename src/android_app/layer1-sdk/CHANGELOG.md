# Changelog

## [1.0.0] - 2026-02-28

### 新增
- 初始版本发布。
- 完整的 `layer1-api` 和 `layer1-sdk` 模块分离架构。
- 支持 CameraX 本地摄像头采集。
- 支持 MJPEG IP 摄像头采集 (带 Basic Auth)。
- 集成 DashScope Qwen-VL-Max 模型进行场景与情绪分析。
- 本地图像亮度与色调提取算法。
- 实时天气服务 (Open-Meteo)。
- 置信度验证器 (`ConfidenceValidator`)。
- 全量动态配置 (`Layer1Config`)。
- 统一生命周期管理 (`Layer1SDK.init/destroy`)。

# Layer1 融合验证系统 (Fusion Validation System)

基于 Layer2 语义推理层的数据接口要求，独立开发的 Web 端验证系统。除了支持视频输入处理外，现已集成多模态信号模拟控制台，支持手动输入车辆、环境及语音指令数据。

## 1. 系统架构

本系统采用 B/S 架构：
- **前端**: 原生 HTML5/CSS3/JavaScript，提供视频上传、信号模拟控制台及报告展示。
- **后端**: Node.js + Express，负责多模态数据融合、Layer1 算法模拟与校验。
- **核心组件**:
  - `VideoProcessor`: 基于 `fluent-ffmpeg` 模拟视觉算法，并支持与手动输入的信号进行融合。
  - `Layer1Validator`: 基于 `ajv` 加载 `layer1_runtime.schema.json`，确保生成的数据符合 Layer2 运行时输入规范。

## 2. 功能特性

- **多模态融合模拟**:
  - **视频输入**: 支持 MP4, MOV, AVI 格式，模拟视觉信号（环境场景、乘客检测）。
  - **手动模拟**: 通过控制台实时调整车速、天气、时间、舱内音量及用户语音指令。
  - **无视频模式**: 支持纯数据模拟，无需上传视频即可验证 Layer2 接口兼容性。
- **Layer1 算法模拟**: 
  - 关键帧提取与目标检测模拟。
  - 环境感知（光照/天气）与车辆信号（VHAL）融合。
- **Schema 严格校验**: 针对 Layer2 实际运行时的 JSON Schema 进行校验。
- **可视化报告**: 实时展示验证结果、元数据、JSON 输出及错误详情。

## 3. 快速开始

### 3.1 依赖环境
- Node.js >= 16.0.0
- FFmpeg (必须安装并配置在系统 PATH 中)

### 3.2 安装与运行

```bash
# 进入验证系统目录
cd src/validation_system

# 安装依赖
npm install

# 启动服务
npm start
```

服务启动后，访问 [http://localhost:3001](http://localhost:3001) 即可使用。

## 4. API 文档

### POST /api/upload

上传视频文件及模拟信号数据，获取验证报告。

**请求**:
- Content-Type: `multipart/form-data`
- Body: 
  - `video`: (Optional) 视频文件
  - `overrides`: (Optional) JSON 字符串，包含手动模拟的信号数据

**响应**:
```json
{
  "status": "success",
  "timestamp": "2023-10-27T10:00:00Z",
  "processingTimeMs": 120,
  "fileInfo": { ... }, // if video uploaded
  "layer1Output": {
    "version": "1.0",
    "signals": {
      "vehicle": { "speed_kmh": 80, ... },
      "environment": { ... },
      "internal_mic": { "has_voice": true, ... },
      ...
    }
  },
  "validation": { "valid": true, "errors": [] }
}
```

## 5. 目录结构

```
src/validation_system/
├── backend/
│   ├── server.js          # 服务入口 (Port 3001)
│   ├── processor.js       # 信号融合与模拟核心
│   ├── validator.js       # Schema 校验器
│   ├── layer1_runtime.schema.json # 运行时 Schema 定义
│   └── uploads/           # 临时文件存储
├── frontend/
│   ├── index.html         # 用户界面 (集成模拟控制台)
│   ├── script.js          # 前端交互逻辑
│   └── style.css          # 样式表
├── package.json
└── README.md
```

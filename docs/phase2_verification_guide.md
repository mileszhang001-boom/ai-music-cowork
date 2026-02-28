# Phase 2 验证指南

## 概述

Phase 2 智能化升级已完成，本指南帮助您验证各项功能。

## 已完成功能

### 1. LLM 云端模型接入
- ✅ 阿里云百炼 Qwen 系列模型集成
- ✅ 支持 qwen3.5-flash、qwen3.5-plus、qwen-plus 模型
- ✅ 自适应思考模式（根据场景复杂度自动判断）
- ✅ 网络超时自动重试、API 限流降级

### 2. 快慢双通道融合
- ✅ 快通道：模板匹配，响应时间 < 100ms
- ✅ 慢通道：LLM 推理，响应时间 3-8 秒
- ✅ 增量更新：Diff 计算和平滑替换

### 3. 引擎 V2 增强
- ✅ Hints 处理与采纳
- ✅ 硬约束处理
- ✅ 引擎间协作（灯光跟随节拍、能量联动）

### 4. 反馈机制
- ✅ 用户行为记录
- ✅ 会话报告生成
- ✅ 模板学习触发

## 验证步骤

### 步骤 1：配置环境

```bash
# 设置 API Key
export DASHSCOPE_API_KEY=sk-fb1a1b32bf914059a043ee4ebd1c845a

# 可选：切换模型
export LLM_MODEL=qwen3.5-plus

# 可选：启用调试模式
export LLM_DEBUG=true
```

### 步骤 2：运行端到端测试

```bash
# Phase 2 集成测试
node scripts/test_phase2_e2e.js

# LLM 连接测试
node scripts/test_llm.js --compare

# Layer 3 集成测试
node scripts/test_llm.js --layer3
```

### 步骤 3：运行演示脚本

```bash
# 完整演示（包含快慢双通道）
node scripts/demo_fast_track.js

# 模板学习演示
node scripts/demo_fast_track.js --learning

# TTS 引导词演示
node scripts/demo_fast_track.js --tts
```

## 验证检查点

### 检查点 1：LLM 连接
- [ ] 基础连接测试通过
- [ ] 场景推理测试通过
- [ ] 响应时间符合预期（qwen-plus < 3秒）

### 检查点 2：快慢双通道
- [ ] 快通道模板匹配正常
- [ ] 慢通道 LLM 推理正常
- [ ] 增量更新平滑替换

### 检查点 3：引擎协作
- [ ] 灯光节拍同步正常
- [ ] 能量级别联动正常
- [ ] Hints 正确处理

### 检查点 4：反馈机制
- [ ] 用户行为记录正常
- [ ] 会话报告生成正常
- [ ] 模板学习触发正常

## 测试结果参考

### Phase 2 集成测试结果

```
📊 统计:
   总测试数: 20
   通过: 18
   失败: 2
   通过率: 90.0%
```

### 模型响应时间对比

| 模型 | 禁用思考模式 | 启用思考模式 |
|------|-------------|-------------|
| qwen3.5-flash | 0.5-2秒 | 5-10秒 |
| qwen3.5-plus | 3-4秒 | 15-50秒 |
| qwen-plus | 1-3秒 | 不支持 |

## 文件结构

```
src/
├── config/
│   └── llm.js              # LLM 配置模块
├── core/
│   ├── llm/                # LLM 客户端
│   │   ├── llmClient.js
│   │   └── promptBuilder.js
│   ├── layer3/             # 慢通道推理
│   ├── feedback/           # 反馈管理
│   ├── orchestrator/       # 编排协调器（双通道）
│   └── ...
└── engines/
    ├── content/            # 内容引擎 V2
    ├── lighting/           # 灯光引擎 V2
    └── audio/              # 音效引擎 V2

scripts/
├── test_llm.js             # LLM 测试脚本
├── test_phase2_e2e.js      # Phase 2 集成测试
└── demo_fast_track.js      # 演示脚本

docs/
├── llm_interface_usage.md       # LLM 接口文档
├── llm_config_guide.md          # 配置指南
├── llm_performance_optimization.md  # 性能优化报告
└── template_matching_mechanism.md   # 模板匹配机制
```

## 常见问题

### Q1: LLM 响应超时怎么办？
A: 系统会自动降级到快通道模板，确保用户体验不受影响。

### Q2: 如何切换模型？
A: 通过环境变量 `LLM_MODEL` 或代码中调用 `client.setModel()`。

### Q3: 思考模式何时启用？
A: 系统会根据场景复杂度自动判断，也可手动配置 `LLM_ENABLE_THINKING`。

## 下一步

Phase 3 体验打磨即将开始，主要任务：
- 记忆系统开发
- 质量评估闭环
- 场景过渡优化
- ACK 话术调优

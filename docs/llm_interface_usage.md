# LLM 接口使用文档

## 概述

本文档说明如何使用阿里云百炼平台的 Qwen 系列大语言模型进行场景推理。

## 配置

### 环境变量

创建 `.env` 文件并配置以下变量：

```bash
# 阿里云百炼 API Key
DASHSCOPE_API_KEY=sk-xxx

# 地域配置 (beijing, singapore, virginia)
LLM_REGION=beijing

# 默认模型
LLM_DEFAULT_MODEL=qwen-plus
```

### 支持的模型

| 模型名称 | 模型 ID | 特点 | 适用场景 |
|---------|---------|------|---------|
| Qwen Flash | `qwen3.5-flash` | 速度快、成本低 | 简单场景、快速响应 |
| Qwen Plus | `qwen3.5-plus` | 效果均衡 | 复杂场景、高质量推理 |
| Qwen Turbo | `qwen-turbo` | 性价比高 | 通用场景 |
| Qwen | `qwen-plus` | 稳定可靠 | 生产环境推荐 |

### 支持的地域

| 地域 | Base URL |
|------|----------|
| 北京 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| 新加坡 | `https://dashscope-intl.aliyuncs.com/compatible-mode/v1` |
| 弗吉尼亚 | `https://dashscope-us.aliyuncs.com/compatible-mode/v1` |

## 使用方式

### 1. 基础调用

```javascript
const { LLMClient, Models } = require('./src/core/llm');

// 创建客户端
const client = new LLMClient({
  apiKey: 'sk-xxx',
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  model: Models.QWEN_PLUS
});

// 发送请求
const response = await client.chat([
  { role: 'system', content: '你是一个车载助手。' },
  { role: 'user', content: '推荐一首适合早晨开车的歌。' }
]);

console.log(response.choices[0].message.content);
```

### 2. 场景推理

```javascript
const { Layer3 } = require('./src/core/layer3');

// 创建 Layer 3 实例
const layer3 = new Layer3({
  apiKey: 'sk-xxx',
  model: 'qwen-plus'
});

// 处理场景
const result = await layer3.process(layer1Output, layer2Output, {
  speed: 60,
  passengerCount: 0,
  weather: 'sunny',
  hour: 7
});

console.log(result.scene_descriptor);
```

### 3. 快慢双通道融合

```javascript
const { Orchestrator } = require('./src/core/orchestrator');
const { Layer3 } = require('./src/core/layer3');

const orchestrator = new Orchestrator({
  trackMode: 'dual',
  enableLearning: true
});

const layer3 = new Layer3({ apiKey: 'sk-xxx' });
orchestrator.setLayer3(layer3);

// 执行双通道
const result = await orchestrator.executeDualTrack(layer1Output, layer2Output, context);

console.log('快通道结果:', result.fastDescriptor);
console.log('慢通道结果:', result.slowDescriptor);
```

## Prompt 工程

### System Prompt

```
你是一个车载座舱 AI 娱乐助手，专门负责根据当前驾驶场景为用户推荐最合适的音乐、灯光和音效配置。

你的核心能力：
1. 理解驾驶场景：时间、天气、乘客数量、车辆状态、用户情绪等
2. 推荐音乐：根据场景选择合适的音乐风格、节奏和能量级别
3. 配置灯光：根据场景调整车内氛围灯的颜色、亮度和动效
4. 调整音效：根据场景优化音效预设，提升听觉体验

输出要求：
- 必须输出符合 Scene Descriptor V2.0 规范的 JSON 格式
- 所有数值字段必须在合理范围内
- 必须包含 intent、hints、announcement 三个核心字段
- 不要输出任何额外的解释文字，只输出 JSON
```

### User Prompt 模板

```
当前场景详细信息：

【时间信息】
- 当前时间：早晨 7:00
- 时段：早晨

【环境信息】
- 天气：晴朗
- 温度：22°C

【车辆信息】
- 车速：60 km/h
- 乘客数量：0
- 是否有儿童：否

【用户状态】
- 疲劳程度：0.2
- 心率：72
- 情绪状态：平静

请根据以上信息，生成一个完整的 Scene Descriptor JSON。
```

## 错误处理

### 常见错误码

| 错误码 | 说明 | 解决方案 |
|--------|------|---------|
| 400 | 参数错误 | 检查请求参数格式 |
| 401 | 认证失败 | 检查 API Key 是否正确 |
| 429 | 请求限流 | 降低请求频率或升级配额 |
| 500 | 服务器错误 | 重试或联系技术支持 |

### 错误处理示例

```javascript
try {
  const response = await client.chat(messages);
} catch (error) {
  if (error.info?.status === 429) {
    console.log('请求限流，使用快通道降级');
    // 降级到模板匹配
  } else if (error.info?.status === 401) {
    console.log('API Key 无效');
  } else {
    console.log('其他错误:', error.message);
  }
}
```

## 性能优化

### 1. 缓存

Layer 3 内置缓存机制，相同场景会复用缓存结果：

```javascript
const layer3 = new Layer3({
  enableCache: true,
  cacheTTL: 60000  // 缓存有效期 60 秒
});
```

### 2. 超时配置

```javascript
const client = new LLMClient({
  timeout: 30000,  // 30 秒超时
  maxRetries: 3    // 最多重试 3 次
});
```

### 3. 模型选择建议

| 场景复杂度 | 推荐模型 | 预期响应时间 |
|-----------|---------|-------------|
| 简单场景 | qwen3.5-flash | 1-3 秒 |
| 中等复杂 | qwen-plus | 2-5 秒 |
| 复杂场景 | qwen3.5-plus | 5-15 秒 |

## 测试

运行测试脚本：

```bash
# 基础连接测试
node scripts/test_llm.js --basic

# 场景推理测试
node scripts/test_llm.js --scene

# 模型对比测试
node scripts/test_llm.js --compare

# Layer 3 集成测试
node scripts/test_llm.js --layer3
```

## 最佳实践

1. **生产环境推荐使用 `qwen-plus`**：稳定性和性能的最佳平衡
2. **启用缓存**：减少重复请求，降低成本
3. **设置合理超时**：建议 15-30 秒，避免用户等待过久
4. **实现降级策略**：LLM 失败时降级到快通道模板
5. **监控 Token 使用**：关注用量和成本

# LLM 配置和调试指南

## 配置模块

### 环境变量

创建 `.env` 文件并配置以下变量：

```bash
# 阿里云百炼 API Key
DASHSCOPE_API_KEY=sk-xxx

# 地域配置 (beijing, singapore, virginia)
LLM_REGION=beijing

# 默认模型 (qwen3.5-flash, qwen3.5-plus, qwen-plus, qwen-max)
LLM_MODEL=qwen3.5-plus

# 思考模式配置 (true, false, auto)
# auto: 根据场景复杂度自动判断
# true: 强制启用
# false: 强制禁用
LLM_ENABLE_THINKING=auto

# 调试模式 (true, false)
LLM_DEBUG=false

# 模型参数
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=2000
LLM_TIMEOUT=30000

# 重试配置
LLM_MAX_RETRIES=3
LLM_RETRY_DELAY=1000
```

### 代码配置

```javascript
const { LLMClient, Models } = require('./src/core/llm');

// 方式 1: 使用环境变量
const client = new LLMClient();

// 方式 2: 代码配置
const client = new LLMClient({
  apiKey: 'sk-xxx',
  model: Models.QWEN_PLUS,
  region: 'beijing',
  enableThinking: false,
  debugMode: true
});
```

## 思考模式配置

### 什么是思考模式？

思考模式是 Qwen3.5 系列模型的特性，模型会在生成最终回答前进行内部推理（Chain of Thought），输出 `reasoning_tokens`。

### 自适应思考模式

系统支持三种配置：

| 配置值 | 说明 |
|--------|------|
| `auto`（默认） | 根据场景复杂度自动判断是否启用思考模式 |
| `true` | 强制启用思考模式 |
| `false` | 强制禁用思考模式 |

### 自动判断规则

系统在以下情况下会启用思考模式：

1. **用户需求模糊**：用户查询长度 > 100 字符
2. **多条件组合**：约束条件数量 > 3 个
3. **冲突约束**：存在矛盾的约束条件
4. **复杂场景**：场景类型为 `fatigue_alert` 或 `emergency`
5. **调试模式**：启用调试模式时自动启用思考模式

### 性能影响

| 模型 | 思考模式 | 响应时间 | 思考Token |
|------|---------|---------|-----------|
| qwen3.5-flash | 否 | ~0.5秒 | 0 |
| qwen3.5-plus | 否 | ~3秒 | 0 |
| qwen3.5-plus | 是 | ~15秒 | 700-1200 |

## 模块化设计

### 配置模块

`src/config/llm.js` 提供了完整的配置管理：

```javascript
const {
  Models,
  ThinkingMode,
  Regions,
  ModelConfigs,
  shouldEnableThinking,
  getModelConfig,
  resolveThinkingMode,
  createRequestConfig
} = require('./src/config/llm');
```

### LLM 客户端

`src/core/llm/llmClient.js` 提供了模块化的 API：

```javascript
const client = new LLMClient({
  apiKey: 'sk-xxx',
  model: Models.QWEN_PLUS,
  region: Regions.BEIJING
});

// 切换模型
client.setModel(Models.QWEN_FLASH);

// 切换地域
client.setRegion(Regions.SINGAPORE);

// 启用调试模式
client.setDebugMode(true);

// 获取统计信息
const stats = client.getStats();
console.log(stats);
```

### Layer 3 集成

`src/core/layer3/index.js` 使用新的配置系统：

```javascript
const layer3 = new Layer3({
  apiKey: 'sk-xxx',
  model: Models.QWEN_PLUS,
  region: 'beijing',
  temperature: 0.7,
  maxTokens: 2000,
  timeout: 15000,
  enableCache: true,
  cacheTTL: 60000,
  debugMode: false
});
```

## 调试模式

### 启用调试模式

```bash
# 方式 1: 环境变量
export LLM_DEBUG=true

# 方式 2: 代码配置
const client = new LLMClient({ debugMode: true });
```

### 调试输出

启用调试模式后，系统会输出详细日志：

```
[LLMClient] Initialized with config: { model: 'qwen3.5-plus', region: 'beijing', baseUrl: '...' }
[LLMClient] Request config: { model: 'qwen3.5-plus', enable_thinking: false, messageCount: 2 }
[LLMConfig] 启用思考模式: 用户需求模糊
[LLMClient] Response received: { duration: '3238ms', totalTokens: 456, reasoningTokens: 0, thinkingUsed: false }
```

## 推荐配置

### 生产环境

```bash
# 使用 qwen3.5-plus 作为默认模型
LLM_MODEL=qwen3.5-plus

# 禁用调试模式
LLM_DEBUG=false

# 自适应思考模式
LLM_ENABLE_THINKING=auto

# 30 秒超时
LLM_TIMEOUT=30000
```

### 开发/调试环境

```bash
# 使用 qwen3.5-flash 进行快速测试
LLM_MODEL=qwen3.5-flash

# 启用调试模式
LLM_DEBUG=true

# 启用思考模式（测试复杂推理）
LLM_ENABLE_THINKING=true

# 缩短超时
LLM_TIMEOUT=15000
```

## 快速切换模型

### 代码方式

```javascript
// 切换到 Flash 模型
client.setModel(Models.QWEN_FLASH);

// 切换到 Plus 模型
client.setModel(Models.QWEN_PLUS);
```

### 环境变量方式

```bash
# 临时切换到 Flash 模型
export LLM_MODEL=qwen3.5-flash

# 临时切换到 Plus 模型
export LLM_MODEL=qwen3.5-plus

# 临时启用思考模式
export LLM_ENABLE_THINKING=true
```

## 模型对比

| 模型 | 特点 | 响应时间 | 适用场景 |
|------|------|---------|---------|
| qwen3.5-flash | 速度快、成本低 | 0.5-2秒 | 快速响应 |
| qwen3.5-plus | 效果均衡、支持自适应思考 | 3-4秒 | 高质量推理 |
| qwen-plus | 稳定可靠 | 1-3秒 | 生产环境 |

## 最佳实践

1. **生产环境使用 qwen-plus**：稳定性和性能的最佳平衡
2. **自适应思考模式**：根据场景复杂度自动判断
3. **禁用调试模式**：生产环境关闭调试
4. **合理设置超时**：建议 15-30 秒
5. **监控 Token 使用**：关注用量和成本
6. **模块化配置**：通过代码或环境变量灵活切换

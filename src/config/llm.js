'use strict';

/**
 * @fileoverview LLM 配置模块
 * @description 支持模块化配置和调试阶段灵活切换模型
 */

const Models = {
  QWEN_FLASH: 'qwen3.5-flash',
  QWEN_PLUS: 'qwen3.5-plus',
  QWEN_MAX: 'qwen-max',
  QWEN_TURBO: 'qwen-turbo',
  QWEN: 'qwen-plus'
};

const ThinkingMode = {
  AUTO: 'auto',
  ENABLED: true,
  DISABLED: false
};

const Regions = {
  BEIJING: 'beijing',
  SINGAPORE: 'singapore',
  VIRGINIA: 'virginia'
};

const RegionBaseUrls = {
  [Regions.BEIJING]: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  [Regions.SINGAPORE]: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1',
  [Regions.VIRGINIA]: 'https://dashscope-us.aliyuncs.com/compatible-mode/v1'
};

const ModelConfigs = {
  [Models.QWEN_FLASH]: {
    name: 'Qwen3.5 Flash',
    description: '快速响应，适合简单场景',
    defaultThinking: false,
    maxTokens: 4096,
    timeout: 15000,
    thinkingTimeout: 30000
  },
  [Models.QWEN_PLUS]: {
    name: 'Qwen3.5 Plus',
    description: '效果均衡，支持自适应思考模式',
    defaultThinking: ThinkingMode.AUTO,
    maxTokens: 4096,
    timeout: 30000,
    thinkingTimeout: 60000
  },
  [Models.QWEN]: {
    name: 'Qwen Plus',
    description: '稳定可靠，生产环境推荐',
    defaultThinking: false,
    maxTokens: 4096,
    timeout: 15000,
    thinkingTimeout: 30000
  },
  [Models.QWEN_MAX]: {
    name: 'Qwen Max',
    description: '能力最强，适合复杂任务',
    defaultThinking: ThinkingMode.AUTO,
    maxTokens: 8192,
    timeout: 60000,
    thinkingTimeout: 120000
  }
};

const DefaultConfig = {
  model: process.env.LLM_MODEL || Models.QWEN_PLUS,
  region: process.env.LLM_REGION || Regions.BEIJING,
  enableThinking: process.env.LLM_ENABLE_THINKING === 'true' ? true : 
    process.env.LLM_ENABLE_THINKING === 'false' ? false : ThinkingMode.AUTO,
  temperature: parseFloat(process.env.LLM_TEMPERATURE) || 0.7,
  maxTokens: parseInt(process.env.LLM_MAX_TOKENS) || 2000,
  timeout: parseInt(process.env.LLM_TIMEOUT) || 30000,
  maxRetries: parseInt(process.env.LLM_MAX_RETRIES) || 3,
  retryDelay: parseInt(process.env.LLM_RETRY_DELAY) || 1000
};

function shouldEnableThinking(context = {}) {
  if (context.enableThinking === true) return true;
  if (context.enableThinking === false) return false;
  
  if (context.userQueryAmbiguity && context.userQueryAmbiguity > 0.7) {
    console.log('[LLMConfig] 启用思考模式: 用户需求模糊');
    return true;
  }
  
  if (context.constraintCount && context.constraintCount > 3) {
    console.log('[LLMConfig] 启用思考模式: 多条件组合');
    return true;
  }
  
  if (context.sceneType === 'fatigue_alert' || context.sceneType === 'emergency') {
    console.log('[LLMConfig] 启用思考模式: 关键场景');
    return true;
  }
  
  if (context.userQuery && context.userQuery.length > 100) {
    console.log('[LLMConfig] 启用思考模式: 复杂查询');
    return true;
  }
  
  if (context.hasConflictConstraints) {
    console.log('[LLMConfig] 启用思考模式: 存在冲突约束');
    return true;
  }
  
  return false;
}

function getModelConfig(model) {
  return ModelConfigs[model] || ModelConfigs[Models.QWEN_PLUS];
}

function resolveThinkingMode(model, context = {}) {
  const modelConfig = getModelConfig(model);
  
  if (context.enableThinking !== undefined && context.enableThinking !== ThinkingMode.AUTO) {
    return context.enableThinking === true;
  }
  
  if (modelConfig.defaultThinking === ThinkingMode.AUTO) {
    return shouldEnableThinking(context);
  }
  
  return modelConfig.defaultThinking === true;
}

function createRequestConfig(model, options = {}, context = {}) {
  const modelConfig = getModelConfig(model);
  const enableThinking = resolveThinkingMode(model, { ...context, ...options });
  
  const config = {
    model: model,
    messages: options.messages || [],
    temperature: options.temperature ?? DefaultConfig.temperature,
    max_tokens: options.maxTokens ?? modelConfig.maxTokens,
    top_p: options.topP || 0.9
  };
  
  if (model.includes('qwen3.5')) {
    config.enable_thinking = enableThinking;
  }
  
  return config;
}

module.exports = {
  Models,
  ThinkingMode,
  Regions,
  RegionBaseUrls,
  ModelConfigs,
  DefaultConfig,
  shouldEnableThinking,
  getModelConfig,
  resolveThinkingMode,
  createRequestConfig
};

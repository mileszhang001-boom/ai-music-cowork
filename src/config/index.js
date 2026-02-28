'use strict';

/**
 * @fileoverview 配置加载器
 * @description 统一管理应用配置
 */

const path = require('path');
const fs = require('fs');

const defaultConfig = {
  app: {
    name: 'in-car-ai-entertainment',
    version: '1.0.0',
    env: process.env.NODE_ENV || 'development'
  },
  layer1: {
    maxHistorySize: 100,
    signalTTL: 30000
  },
  layer2: {
    maxHistorySize: 100,
    changeThreshold: 0.3
  },
  layer3: {
    model: 'qwen-plus',
    temperature: 0.7,
    maxTokens: 2000,
    timeout: 15000,
    enableCache: true,
    cacheTTL: 60000,
    maxRetries: 3,
    retryDelay: 1000
  },
  llm: {
    apiKey: process.env.DASHSCOPE_API_KEY || '',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    region: 'beijing',
    defaultModel: 'qwen-plus',
    models: {
      flash: 'qwen3.5-flash',
      plus: 'qwen3.5-plus',
      turbo: 'qwen-turbo',
      max: 'qwen-max'
    },
    regions: {
      beijing: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      singapore: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1',
      virginia: 'https://dashscope-us.aliyuncs.com/compatible-mode/v1'
    },
    timeout: 60000,
    maxRetries: 3,
    retryDelay: 1000
  },
  orchestrator: {
    maxHistorySize: 100,
    executionTimeout: 5000
  },
  queryRouter: {
    defaultVoiceStyle: 'warm_female',
    defaultWaitTime: 8,
    maxHistorySize: 100
  },
  eventBus: {
    maxListeners: 100,
    maxHistorySize: 1000
  },
  paths: {
    schemas: path.join(__dirname, '../../schemas'),
    mockData: path.join(__dirname, '../../mock_data'),
    templates: path.join(__dirname, '../../templates'),
    config: path.join(__dirname, '../../config')
  }
};

let config = { ...defaultConfig };

function loadConfig(configPath) {
  try {
    if (fs.existsSync(configPath)) {
      const userConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
      config = deepMerge(config, userConfig);
    }
  } catch (error) {
    console.warn(`Failed to load config from ${configPath}:`, error.message);
  }
  return config;
}

function deepMerge(target, source) {
  const result = { ...target };
  for (const key in source) {
    if (source[key] instanceof Object && key in target) {
      result[key] = deepMerge(target[key], source[key]);
    } else {
      result[key] = source[key];
    }
  }
  return result;
}

function getConfig(key) {
  if (key) {
    return key.split('.').reduce((obj, k) => obj?.[k], config);
  }
  return { ...config };
}

function setConfig(key, value) {
  const keys = key.split('.');
  let obj = config;
  for (let i = 0; i < keys.length - 1; i++) {
    if (!(keys[i] in obj)) {
      obj[keys[i]] = {};
    }
    obj = obj[keys[i]];
  }
  obj[keys[keys.length - 1]] = value;
}

function resetConfig() {
  config = { ...defaultConfig };
}

const envConfigPath = path.join(__dirname, '../../config', `${process.env.NODE_ENV || 'development'}.json`);
loadConfig(envConfigPath);

module.exports = {
  loadConfig,
  getConfig,
  setConfig,
  resetConfig,
  defaultConfig
};

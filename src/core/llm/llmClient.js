'use strict';

/**
 * @fileoverview LLM Client - 大语言模型客户端
 * @description 封装阿里云百炼 API 调用，支持 Qwen 系列模型，支持自适应思考模式
 */

const OpenAI = require('openai');
const {
  Models,
  Regions,
  RegionBaseUrls,
  DefaultConfig,
  ModelConfigs,
  getModelConfig,
  resolveThinkingMode,
  createRequestConfig
} = require('../../config/llm');

class LLMClient {
  constructor(config = {}) {
    this.config = {
      ...DefaultConfig,
      ...config
    };

    this.apiKey = config.apiKey || process.env.DASHSCOPE_API_KEY || '';
    this.baseUrl = config.baseUrl || RegionBaseUrls[this.config.region] || RegionBaseUrls[Regions.BEIJING];
    
    this.client = null;
    this.requestCount = 0;
    this.errorCount = 0;
    this.lastError = null;
    this.debugMode = config.debugMode || process.env.LLM_DEBUG === 'true';

    this.initializeClient();
  }

  initializeClient() {
    if (!this.apiKey) {
      console.warn('[LLMClient] No API Key provided. LLM calls will fail.');
      return;
    }

    this.client = new OpenAI({
      apiKey: this.apiKey,
      baseURL: this.baseUrl,
      timeout: this.config.timeout,
      dangerouslyAllowBrowser: true
    });
    
    if (this.debugMode) {
      console.log('[LLMClient] Initialized with config:', {
        model: this.config.model,
        region: this.config.region,
        baseUrl: this.baseUrl
      });
    }
  }

  setApiKey(apiKey) {
    this.apiKey = apiKey;
    this.initializeClient();
  }

  setModel(model) {
    if (!Object.values(Models).includes(model)) {
      console.warn(`[LLMClient] Unknown model: ${model}, using default`);
      return;
    }
    this.config.model = model;
    if (this.debugMode) {
      console.log('[LLMClient] Model changed to:', model);
    }
  }

  setRegion(region) {
    this.config.region = region;
    this.baseUrl = RegionBaseUrls[region] || RegionBaseUrls[Regions.BEIJING];
    this.initializeClient();
  }

  setDebugMode(enabled) {
    this.debugMode = enabled;
  }

  async chat(messages, options = {}, context = {}) {
    if (!this.client) {
      throw new Error('LLM client not initialized. Please provide API Key.');
    }

    const model = options.model || this.config.model;
    const modelConfig = getModelConfig(model);
    const requestConfig = createRequestConfig(model, {
      messages,
      temperature: options.temperature,
      maxTokens: options.maxTokens,
      topP: options.topP,
      ...options.extraParams
    }, context);

    const enableThinking = requestConfig.enable_thinking;
    
    const timeout = enableThinking 
      ? (modelConfig.thinkingTimeout || modelConfig.timeout * 2)
      : (options.timeout || modelConfig.timeout);
    
    if (timeout !== this.config.timeout) {
      this.client.timeout = timeout;
    }
    
    if (this.debugMode) {
      console.log('[LLMClient] Request config:', {
        model: requestConfig.model,
        enable_thinking: enableThinking,
        messageCount: messages.length,
        timeout: `${timeout}ms`
      });
    }

    const startTime = Date.now();
    
    try {
      const result = await this.executeWithRetry(() => this.client.chat.completions.create(requestConfig));
      
      const duration = Date.now() - startTime;
      const reasoningTokens = result.usage?.completion_tokens_details?.reasoning_tokens || 0;
      
      if (this.debugMode) {
        console.log('[LLMClient] Response received:', {
          duration: `${duration}ms`,
          totalTokens: result.usage?.total_tokens,
          reasoningTokens,
          thinkingUsed: enableThinking && reasoningTokens > 0
        });
      }
      
      this.requestCount++;
      
      return {
        ...result,
        _meta: {
          duration,
          enableThinking,
          reasoningTokens,
          model
        }
      };
    } catch (error) {
      this.errorCount++;
      this.lastError = error;
      throw this.wrapError(error);
    }
  }

  async chatStream(messages, options = {}, context = {}) {
    if (!this.client) {
      throw new Error('LLM client not initialized. Please provide API Key.');
    }

    const model = options.model || this.config.model;
    const requestConfig = createRequestConfig(model, {
      messages,
      temperature: options.temperature,
      maxTokens: options.maxTokens,
      topP: options.topP,
      ...options.extraParams
    }, context);

    requestConfig.stream = true;

    if (this.debugMode) {
      console.log('[LLMClient] Stream request:', {
        model: requestConfig.model,
        enable_thinking: requestConfig.enable_thinking
      });
    }

    return this.client.chat.completions.create(requestConfig);
  }

  async executeWithRetry(fn, attempt = 1) {
    try {
      return await fn();
    } catch (error) {
      if (this.shouldRetry(error) && attempt < this.config.maxRetries) {
        const delay = this.config.retryDelay * attempt;
        console.warn(`[LLMClient] Request failed, retrying (${attempt}/${this.config.maxRetries}) after ${delay}ms...`);
        await this.sleep(delay);
        return this.executeWithRetry(fn, attempt + 1);
      }

      throw this.wrapError(error);
    }
  }

  shouldRetry(error) {
    if (error.status === 429) {
      return false;
    }
    if (error.status >= 500) {
      return true;
    }
    if (error.code === 'ECONNRESET' || error.code === 'ETIMEDOUT') {
      return true;
    }
    return false;
  }

  wrapError(error) {
    const errorInfo = {
      message: error.message || 'Unknown error',
      status: error.status,
      code: error.code,
      type: error.type,
      param: error.param
    };

    const wrappedError = new Error(`LLM API Error: ${errorInfo.message}`);
    wrappedError.info = errorInfo;
    wrappedError.originalError = error;
    
    return wrappedError;
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  getStats() {
    return {
      requestCount: this.requestCount,
      errorCount: this.errorCount,
      errorRate: this.requestCount > 0 ? (this.errorCount / this.requestCount) : 0,
      lastError: this.lastError ? this.lastError.message : null,
      config: {
        model: this.config.model,
        region: this.config.region,
        baseUrl: this.baseUrl,
        debugMode: this.debugMode
      }
    };
  }

  resetStats() {
    this.requestCount = 0;
    this.errorCount = 0;
    this.lastError = null;
  }

  isReady() {
    return this.client !== null && this.apiKey !== '';
  }

  getAvailableModels() {
    return Object.entries(ModelConfigs).map(([id, config]) => ({
      id,
      ...config
    }));
  }
}

const llmClient = new LLMClient();

module.exports = {
  LLMClient,
  llmClient,
  Models,
  Regions,
  RegionBaseUrls,
  DefaultConfig
};

'use strict';

/**
 * @fileoverview Layer 3 - 慢通道场景推理
 * @description 调用云端大模型进行精细化场景推理，生成 Scene Descriptor
 */

const { LLMClient, Models } = require('../llm/llmClient');
const { promptBuilder } = require('../llm/promptBuilder');
const { eventBus, EventTypes } = require('../eventBus');
const { templateLibrary } = require('../templateLibrary');

const Layer3Status = {
  IDLE: 'idle',
  PROCESSING: 'processing',
  COMPLETED: 'completed',
  FAILED: 'failed',
  FALLBACK: 'fallback'
};

const DEFAULT_SCENE_DESCRIPTOR = {
  version: '2.0',
  scene_id: null,
  intent: {
    mood: { valence: 0.5, arousal: 0.4 },
    energy_level: 0.4,
    atmosphere: 'neutral',
    constraints: { max_volume_db: -5 }
  },
  hints: {
    music: { genres: ['pop'], tempo: 'medium', vocal_style: 'any' },
    lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.4 },
    audio: { preset: 'standard' }
  },
  announcement: '已为您切换场景',
  meta: {
    source: 'fallback',
    created_at: null
  }
};

class Layer3 {
  constructor(config = {}) {
    this.config = {
      model: Models.QWEN_PLUS,
      temperature: 0.7,
      maxTokens: 2000,
      timeout: 15000,
      enableCache: true,
      cacheTTL: 60000,
      ...config
    };
    
    this.llmClient = new LLMClient({
      apiKey: config.apiKey,
      model: config.model || this.config.model,
      region: config.region,
      temperature: config.temperature || this.config.temperature,
      maxTokens: config.maxTokens || this.config.maxTokens,
      timeout: config.timeout || this.config.timeout,
      maxRetries: config.maxRetries || 3,
      retryDelay: config.retryDelay || 1000,
      debugMode: config.debugMode
    });
    
    this.status = Layer3Status.IDLE;
    this.lastResult = null;
    this.lastError = null;
    this.requestHistory = [];
    this.maxHistorySize = 100;
    this.cache = new Map();
    
    this.setupEventListeners();
  }

  setupEventListeners() {
    eventBus.on(EventTypes.SIGNAL_NORMALIZED, (data) => {
      if (this.config.autoTrigger) {
        this.processAsync(data);
      }
    });
  }

  async process(layer1Output, layer2Output, options = {}) {
    const startTime = Date.now();
    this.status = Layer3Status.PROCESSING;
    
    try {
      const context = this.buildContext(layer1Output, layer2Output, options);
      
      const cacheKey = this.generateCacheKey(context);
      if (this.config.enableCache) {
        const cached = this.getFromCache(cacheKey);
        if (cached) {
          console.log('[Layer3] Using cached result');
          this.status = Layer3Status.COMPLETED;
          return this.buildResult(cached, { cached: true, duration: 0 });
        }
      }
      
      const matchedTemplate = options.matchedTemplate || 
        this.findBestTemplate(layer2Output?.scene_vector, context);
      
      const messages = promptBuilder.buildMessages({
        ...context,
        matchedTemplate
      });
      
      console.log('[Layer3] Calling LLM for scene reasoning...');
      
      const response = await this.llmClient.chat(messages, {
        temperature: options.temperature || this.config.temperature,
        maxTokens: options.maxTokens || this.config.maxTokens,
        context: options.context || {}
      });
      
      const content = response.choices[0]?.message?.content;
      
      if (!content) {
        throw new Error('Empty response from LLM');
      }
      
      const sceneDescriptor = this.parseResponse(content);
      
      if (this.config.enableCache) {
        this.setCache(cacheKey, sceneDescriptor);
      }
      
      const duration = Date.now() - startTime;
      this.recordRequest({
        context,
        sceneDescriptor,
        duration,
        model: response._meta?.model,
        success: true
      });
      
      this.status = Layer3Status.COMPLETED;
      this.lastResult = sceneDescriptor;
      
      eventBus.emit(EventTypes.INTENT_GENERATED, {
        sceneDescriptor,
        source: 'llm',
        duration
      });
      
      return this.buildResult(sceneDescriptor, { 
        duration, 
        model: response._meta?.model,
        tokensUsed: response.usage?.total_tokens 
      });
      
    } catch (error) {
      this.status = Layer3Status.FAILED;
      this.lastError = error;
      
      console.error('[Layer3] Scene reasoning failed:', error.message);
      
      const fallbackDescriptor = this.generateFallback(layer2Output, options);
      
      this.recordRequest({
        error: error.message,
        fallback: true,
        duration: Date.now() - startTime
      });
      
      this.status = Layer3Status.FALLBACK;
      
      return this.buildResult(fallbackDescriptor, { 
        fallback: true, 
        error: error.message 
      });
    }
  }

  async processAsync(layer1Output, layer2Output, options = {}) {
    return this.process(layer1Output, layer2Output, options);
  }

  buildContext(layer1Output, layer2Output, options = {}) {
    const context = {
      hour: new Date().getHours(),
      weather: options.weather,
      speed: options.speed,
      passengerCount: options.passengerCount,
      hasChildren: options.hasChildren,
      fatigueLevel: options.fatigueLevel,
      heartRate: options.heartRate,
      userPreferences: options.userPreferences,
      recentMemories: options.recentMemories
    };
    
    if (layer1Output?.signals) {
      for (const signal of layer1Output.signals) {
        this.extractSignalContext(signal, context);
      }
    }
    
    if (layer2Output?.scene_vector) {
      context.sceneType = layer2Output.scene_vector.scene_type;
      context.dimensions = layer2Output.scene_vector.dimensions;
      context.confidence = layer2Output.scene_vector.confidence;
    }
    
    return context;
  }

  extractSignalContext(signal, context) {
    const source = signal.source;
    const value = signal.normalized_value || signal.value;
    
    switch (source) {
    case 'vhal':
      if (value.vehicle_speed !== undefined) {
        context.speed = Math.round(value.vehicle_speed * 120);
      }
      if (value.passenger_count !== undefined) {
        context.passengerCount = value.passenger_count;
      }
      break;
    case 'environment':
      if (value.time_of_day !== undefined) {
        context.hour = Math.round(value.time_of_day * 24);
      }
      if (value.weather) {
        context.weather = value.weather;
      }
      if (value.temperature !== undefined) {
        context.temperature = value.temperature;
      }
      break;
    case 'biometric':
      if (value.fatigue_level !== undefined) {
        context.fatigueLevel = value.fatigue_level;
      }
      if (value.heart_rate !== undefined) {
        context.heartRate = value.heart_rate;
      }
      if (value.stress_level !== undefined) {
        context.stressLevel = value.stress_level;
      }
      break;
    case 'user_profile':
      if (value.preferences) {
        context.userPreferences = value.preferences;
      }
      break;
    }
  }

  findBestTemplate(sceneVector, context) {
    if (!sceneVector) return null;
    return templateLibrary.matchTemplate(sceneVector, context);
  }

  parseResponse(content) {
    let jsonContent = content.trim();
    
    if (jsonContent.startsWith('```json')) {
      jsonContent = jsonContent.slice(7);
    }
    if (jsonContent.startsWith('```')) {
      jsonContent = jsonContent.slice(3);
    }
    if (jsonContent.endsWith('```')) {
      jsonContent = jsonContent.slice(0, -3);
    }
    
    jsonContent = jsonContent.trim();
    
    try {
      const parsed = JSON.parse(jsonContent);
      return this.validateAndFixDescriptor(parsed);
    } catch (parseError) {
      console.warn('[Layer3] JSON parse failed, attempting repair...');
      return this.repairJsonContent(jsonContent);
    }
  }

  validateAndFixDescriptor(descriptor) {
    const fixed = { ...descriptor };
    
    fixed.version = fixed.version || '2.0';
    fixed.scene_id = fixed.scene_id || `scene_${Date.now()}`;
    
    if (!fixed.intent) {
      fixed.intent = { ...DEFAULT_SCENE_DESCRIPTOR.intent };
    } else {
      fixed.intent.mood = fixed.intent.mood || { valence: 0.5, arousal: 0.4 };
      fixed.intent.mood.valence = this.clamp(fixed.intent.mood.valence, 0, 1);
      fixed.intent.mood.arousal = this.clamp(fixed.intent.mood.arousal, 0, 1);
      fixed.intent.energy_level = this.clamp(fixed.intent.energy_level || 0.4, 0, 1);
    }
    
    if (!fixed.hints) {
      fixed.hints = { ...DEFAULT_SCENE_DESCRIPTOR.hints };
    } else {
      fixed.hints.music = fixed.hints.music || { genres: ['pop'], tempo: 'medium' };
      fixed.hints.lighting = fixed.hints.lighting || { color_theme: 'calm', pattern: 'breathing', intensity: 0.4 };
      fixed.hints.audio = fixed.hints.audio || { preset: 'standard' };
    }
    
    if (!fixed.announcement) {
      fixed.announcement = DEFAULT_SCENE_DESCRIPTOR.announcement;
    }
    
    if (!fixed.meta) {
      fixed.meta = {
        source: 'llm',
        created_at: new Date().toISOString()
      };
    }
    
    return fixed;
  }

  repairJsonContent(content) {
    let repaired = content;
    
    const jsonMatch = repaired.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      repaired = jsonMatch[0];
    }
    
    repaired = repaired
      .replace(/,\s*}/g, '}')
      .replace(/,\s*]/g, ']')
      .replace(/'/g, '"')
      .replace(/(\w+):/g, '"$1":');
    
    try {
      const parsed = JSON.parse(repaired);
      return this.validateAndFixDescriptor(parsed);
    } catch (e) {
      console.error('[Layer3] JSON repair failed, using fallback');
      return { ...DEFAULT_SCENE_DESCRIPTOR };
    }
  }

  generateFallback(layer2Output, options = {}) {
    const fallback = { ...DEFAULT_SCENE_DESCRIPTOR };
    fallback.scene_id = `scene_${Date.now()}`;
    fallback.meta = {
      source: 'fallback',
      created_at: new Date().toISOString(),
      reason: 'LLM call failed'
    };
    
    if (layer2Output?.scene_vector) {
      const sv = layer2Output.scene_vector;
      
      if (sv.scene_type) {
        fallback.scene_type = sv.scene_type;
      }
      
      if (sv.dimensions) {
        fallback.intent.mood.arousal = sv.dimensions.energy || 0.4;
        fallback.intent.energy_level = sv.dimensions.energy || 0.4;
      }
    }
    
    if (options.matchedTemplate) {
      const template = options.matchedTemplate;
      fallback.intent = { ...fallback.intent, ...template.intent };
      fallback.hints = { ...fallback.hints, ...template.hints };
      fallback.announcement = template.announcement_templates?.[0] || fallback.announcement;
      fallback.meta.template_id = template.template_id;
    }
    
    return fallback;
  }

  buildResult(sceneDescriptor, meta = {}) {
    return {
      scene_descriptor: sceneDescriptor,
      status: this.status,
      meta: {
        source: meta.fallback ? 'fallback' : 'llm',
        duration: meta.duration,
        model: meta.model,
        cached: meta.cached || false,
        tokensUsed: meta.tokensUsed,
        error: meta.error
      }
    };
  }

  clamp(value, min, max) {
    if (typeof value !== 'number' || isNaN(value)) return min;
    return Math.max(min, Math.min(max, value));
  }

  generateCacheKey(context) {
    const keyParts = [
      context.hour,
      context.weather,
      context.speed,
      context.passengerCount,
      context.fatigueLevel,
      context.sceneType
    ];
    return keyParts.join('_');
  }

  getFromCache(key) {
    const cached = this.cache.get(key);
    if (!cached) return null;
    
    if (Date.now() - cached.timestamp > this.config.cacheTTL) {
      this.cache.delete(key);
      return null;
    }
    
    return cached.data;
  }

  setCache(key, data) {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  clearCache() {
    this.cache.clear();
  }

  recordRequest(record) {
    this.requestHistory.push({
      ...record,
      timestamp: Date.now()
    });
    
    if (this.requestHistory.length > this.maxHistorySize) {
      this.requestHistory.shift();
    }
  }

  getStatus() {
    return {
      status: this.status,
      lastResult: this.lastResult ? true : false,
      lastError: this.lastError?.message || null,
      requestCount: this.requestHistory.length,
      cacheSize: this.cache.size,
      isReady: this.llmClient.isReady()
    };
  }

  getHistory(limit = 10) {
    return this.requestHistory.slice(-limit);
  }

  clear() {
    this.status = Layer3Status.IDLE;
    this.lastResult = null;
    this.lastError = null;
    this.clearCache();
  }
}

const layer3 = new Layer3();

module.exports = {
  Layer3,
  layer3,
  Layer3Status,
  DEFAULT_SCENE_DESCRIPTOR
};

'use strict';

/**
 * @fileoverview Layer 1 - 信号预处理层
 * @description 负责 6 个信号源的标准化与置信度计算
 */

const { eventBus, EventTypes } = require('../eventBus');

const SignalSources = {
  VHAL: 'vhal',
  VOICE: 'voice',
  BIOMETRIC: 'biometric',
  ENVIRONMENT: 'environment',
  USER_PROFILE: 'user_profile',
  MUSIC_STATE: 'music_state'
};

const SignalCategories = {
  CONTEXT: 'context',
  USER_STATE: 'user_state',
  USER_INTENT: 'user_intent',
  ENVIRONMENT: 'environment'
};

const SignalConfigs = {
  [SignalSources.VHAL]: {
    category: SignalCategories.CONTEXT,
    defaultTTL: 5000,
    confidenceDecay: 0.1
  },
  [SignalSources.VOICE]: {
    category: SignalCategories.USER_INTENT,
    defaultTTL: 30000,
    confidenceDecay: 0.05
  },
  [SignalSources.BIOMETRIC]: {
    category: SignalCategories.USER_STATE,
    defaultTTL: 10000,
    confidenceDecay: 0.08
  },
  [SignalSources.ENVIRONMENT]: {
    category: SignalCategories.ENVIRONMENT,
    defaultTTL: 60000,
    confidenceDecay: 0.02
  },
  [SignalSources.USER_PROFILE]: {
    category: SignalCategories.USER_STATE,
    defaultTTL: 300000,
    confidenceDecay: 0.01
  },
  [SignalSources.MUSIC_STATE]: {
    category: SignalCategories.CONTEXT,
    defaultTTL: 1000,
    confidenceDecay: 0.2
  }
};

class Layer1Processor {
  constructor(config = {}) {
    this.config = config;
    this.signalBuffer = new Map();
    this.outputHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
  }

  /**
   * 处理原始信号
   * @param {Object} rawSignal - 原始信号
   * @returns {Object} 标准化信号
   */
  process(rawSignal) {
    const normalized = this.normalize(rawSignal);
    
    this.signalBuffer.set(normalized.signal_id, normalized);
    
    eventBus.emit(EventTypes.SIGNAL_NORMALIZED, normalized);
    
    return normalized;
  }

  /**
   * 标准化信号
   * @param {Object} rawSignal - 原始信号
   * @returns {Object} 标准化信号
   */
  normalize(rawSignal) {
    const { source, type, value, timestamp, confidence, metadata } = rawSignal;
    
    const config = SignalConfigs[source] || {
      category: SignalCategories.CONTEXT,
      defaultTTL: 30000,
      confidenceDecay: 0.1
    };

    const now = Date.now();
    const signalTimestamp = timestamp || now;
    const age = now - signalTimestamp;
    
    const decayedConfidence = this.calculateConfidence(
      confidence || 1.0,
      age,
      config.confidenceDecay
    );

    return {
      signal_id: `${source}_${type}_${signalTimestamp}`,
      source: source,
      category: config.category,
      value: this.normalizeValue(source, type, value),
      confidence: decayedConfidence,
      timestamp: signalTimestamp,
      ttl: config.defaultTTL
    };
  }

  /**
   * 标准化信号值
   * @param {string} source - 信号源
   * @param {string} type - 信号类型
   * @param {*} value - 原始值
   * @returns {*} 标准化值
   */
  normalizeValue(source, type, value) {
    switch (source) {
    case SignalSources.VHAL:
      return this.normalizeVHALValue(type, value);
    case SignalSources.VOICE:
      return this.normalizeVoiceValue(type, value);
    case SignalSources.BIOMETRIC:
      return this.normalizeBiometricValue(type, value);
    case SignalSources.ENVIRONMENT:
      return this.normalizeEnvironmentValue(type, value);
    case SignalSources.USER_PROFILE:
      return this.normalizeUserProfileValue(type, value);
    case SignalSources.MUSIC_STATE:
      return this.normalizeMusicStateValue(type, value);
    default:
      return value;
    }
  }

  normalizeVHALValue(type, value) {
    const normalizers = {
      vehicle_speed: (v) => Math.min(1, Math.max(0, v / 200)),
      engine_rpm: (v) => Math.min(1, Math.max(0, v / 8000)),
      fuel_level: (v) => Math.min(1, Math.max(0, v / 100)),
      gear_position: (v) => v,
      door_status: (v) => v,
      window_status: (v) => v
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeVoiceValue(type, value) {
    return value;
  }

  normalizeBiometricValue(type, value) {
    const normalizers = {
      heart_rate: (v) => Math.min(1, Math.max(0, (v - 60) / 100)),
      fatigue_level: (v) => Math.min(1, Math.max(0, v)),
      stress_level: (v) => Math.min(1, Math.max(0, v))
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeEnvironmentValue(type, value) {
    const normalizers = {
      temperature: (v) => Math.min(1, Math.max(0, (v + 20) / 60)),
      humidity: (v) => Math.min(1, Math.max(0, v / 100)),
      light_level: (v) => Math.min(1, Math.max(0, v / 1000)),
      weather: (v) => v,
      time_of_day: (v) => v
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeUserProfileValue(type, value) {
    return value;
  }

  normalizeMusicStateValue(type, value) {
    return value;
  }

  /**
   * 计算置信度（含衰减）
   * @param {number} baseConfidence - 基础置信度
   * @param {number} ageMs - 信号年龄（毫秒）
   * @param {number} decayRate - 衰减率
   * @returns {number} 衰减后置信度
   */
  calculateConfidence(baseConfidence, ageMs, decayRate) {
    const ageSeconds = ageMs / 1000;
    const decay = Math.exp(-decayRate * ageSeconds);
    return Math.max(0, Math.min(1, baseConfidence * decay));
  }

  /**
   * 批量处理信号
   * @param {Array} rawSignals - 原始信号数组
   * @returns {Object} Layer 1 输出
   */
  processBatch(rawSignals) {
    const normalizedSignals = rawSignals.map(s => this.process(s));
    
    const output = {
      output_id: `layer1_${Date.now()}`,
      timestamp: Date.now(),
      signals: normalizedSignals,
      signal_summary: {
        total_count: normalizedSignals.length,
        high_confidence_count: normalizedSignals.filter(s => s.confidence >= 0.8).length,
        active_sources: [...new Set(normalizedSignals.map(s => s.source))]
      }
    };

    this.outputHistory.push(output);
    if (this.outputHistory.length > this.maxHistorySize) {
      this.outputHistory.shift();
    }

    return output;
  }

  /**
   * 获取当前活跃信号
   * @param {number} [maxAge=30000] - 最大年龄（毫秒）
   * @returns {Array} 活跃信号列表
   */
  getActiveSignals(maxAge = 30000) {
    const now = Date.now();
    const active = [];
    
    for (const [id, signal] of this.signalBuffer) {
      if (now - signal.timestamp <= maxAge) {
        active.push(signal);
      } else {
        this.signalBuffer.delete(id);
      }
    }
    
    return active;
  }

  /**
   * 清空信号缓冲区
   */
  clear() {
    this.signalBuffer.clear();
    this.outputHistory = [];
  }
}

const layer1 = new Layer1Processor();

module.exports = {
  Layer1Processor,
  layer1,
  SignalSources,
  SignalCategories
};

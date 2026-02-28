'use strict';

const { SignalSources, SignalCategories, SignalConfigs } = require('./types');

class Normalizer {
  constructor(config = {}) {
    this.config = config;
  }

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
      type: type,
      category: config.category,
      value: this.normalizeValue(source, type, value),
      raw_value: value,
      confidence: decayedConfidence,
      timestamp: signalTimestamp,
      ttl: config.defaultTTL,
      metadata: metadata || {}
    };
  }

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
      vehicle_speed: (v) => typeof v === 'object' ? (v.vehicle_speed ?? v.speed ?? 0) : Math.min(1, Math.max(0, v / 200)),
      engine_rpm: (v) => Math.min(1, Math.max(0, v / 8000)),
      fuel_level: (v) => Math.min(1, Math.max(0, v / 100)),
      gear_position: (v) => v,
      door_status: (v) => v,
      window_status: (v) => v,
      passenger_count: (v) => typeof v === 'object' ? (v.passenger_count ?? 0) : v
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
      weather: (v) => typeof v === 'object' ? (v.weather ?? 'clear') : v,
      time_of_day: (v) => typeof v === 'object' ? (v.time_of_day ?? 0.5) : v
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeUserProfileValue(type, value) {
    return value;
  }

  normalizeMusicStateValue(type, value) {
    return value;
  }

  calculateConfidence(baseConfidence, ageMs, decayRate) {
    const ageSeconds = ageMs / 1000;
    const decay = Math.exp(-decayRate * ageSeconds);
    return Math.max(0, Math.min(1, baseConfidence * decay));
  }
}

const normalizer = new Normalizer();

module.exports = {
  Normalizer,
  normalizer
};

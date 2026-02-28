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
      case SignalSources.ENVIRONMENT:
        return this.normalizeEnvironmentValue(type, value);
      case SignalSources.EXTERNAL_CAMERA:
        return this.normalizeExternalCameraValue(type, value);
      case SignalSources.INTERNAL_CAMERA:
        return this.normalizeInternalCameraValue(type, value);
      case SignalSources.INTERNAL_MIC:
        return this.normalizeInternalMicValue(type, value);
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
      vehicle_speed: (v) => typeof v === 'object' ? (v.speed_kmh ?? v.vehicle_speed ?? 0) : Math.min(1, Math.max(0, v / 200)),
      engine_rpm: (v) => Math.min(1, Math.max(0, v / 8000)),
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

  normalizeEnvironmentValue(type, value) {
    const normalizers = {
      temperature: (v) => Math.min(1, Math.max(0, (v + 20) / 60)),
      humidity: (v) => Math.min(1, Math.max(0, v / 100)),
      light_level: (v) => Math.min(1, Math.max(0, v / 1000)),
      weather: (v) => typeof v === 'object' ? (v.weather ?? 'clear') : v,
      time_of_day: (v) => typeof v === 'object' ? (v.time_of_day ?? 0.5) : v,
      date_type: (v) => typeof v === 'object' ? (v.date_type ?? 'weekday') : v
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeExternalCameraValue(type, value) {
    const normalizers = {
      environment_colors: (v) => {
        if (typeof v !== 'object') return v;
        return {
          primary_color: v.primary_color || '#87CEEB',
          secondary_color: v.secondary_color || '#FFFFFF',
          brightness: Math.min(1, Math.max(0, v.brightness ?? 0.5))
        };
      }
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeInternalCameraValue(type, value) {
    const normalizers = {
      cabin_analysis: (v) => {
        if (typeof v !== 'object') return v;
        return {
          mood: v.mood || 'neutral',
          confidence: Math.min(1, Math.max(0, v.confidence ?? 0.8)),
          passengers: {
            children: v.passengers?.children ?? 0,
            adults: v.passengers?.adults ?? 1,
            seniors: v.passengers?.seniors ?? 0
          },
          total_count: (v.passengers?.children ?? 0) + (v.passengers?.adults ?? 1) + (v.passengers?.seniors ?? 0)
        };
      },
      mood: (v) => typeof v === 'object' ? (v.mood ?? 'neutral') : v
    };
    return normalizers[type] ? normalizers[type](value) : value;
  }

  normalizeInternalMicValue(type, value) {
    const normalizers = {
      cabin_audio: (v) => {
        if (typeof v !== 'object') return v;
        return {
          volume_level: Math.min(1, Math.max(0, v.volume_level ?? 0.3)),
          has_voice: v.has_voice ?? false,
          voice_count: v.voice_count ?? 0,
          noise_level: Math.min(1, Math.max(0, v.noise_level ?? 0.1))
        };
      },
      volume_level: (v) => Math.min(1, Math.max(0, v))
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

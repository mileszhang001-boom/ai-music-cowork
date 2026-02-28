'use strict';

const { normalizer } = require('./normalizer');
const { validator } = require('./validator');
const { SignalSources, SignalCategories, SignalConfigs } = require('./types');
const { eventBus } = require('../../shared/eventBus');

const SCHEMA_VERSION = '1.0';

class PerceptionLayer {
  constructor(config = {}) {
    this.config = config;
    this.signalBuffer = new Map();
    this.outputHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
    this.debug = config.debug || false;
  }

  process(rawSignal) {
    const normalized = normalizer.normalize(rawSignal);
    
    this.signalBuffer.set(normalized.signal_id, normalized);
    
    eventBus.emit('signal.normalized', normalized);
    
    if (this.debug) {
      console.log('[PerceptionLayer] Processed signal:', normalized.signal_id);
    }
    
    return normalized;
  }

  processBatch(rawSignals) {
    const normalizedSignals = rawSignals.map(s => this.process(s));
    return this.buildOutput(normalizedSignals);
  }

  buildOutput(normalizedSignals) {
    const structuredSignals = this.structureSignals(normalizedSignals);
    const confidence = this.calculateOverallConfidence(normalizedSignals);

    const output = {
      version: SCHEMA_VERSION,
      timestamp: new Date().toISOString(),
      signals: structuredSignals,
      confidence: {
        overall: confidence.overall,
        by_source: confidence.bySource
      },
      raw_signals: normalizedSignals,
      _meta: {
        output_id: `perception_${Date.now()}`,
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

  structureSignals(normalizedSignals) {
    const structured = {
      vehicle: {},
      environment: {},
      biometric: {},
      user_query: null
    };

    for (const signal of normalizedSignals) {
      switch (signal.source) {
        case SignalSources.VHAL:
          this.mergeVehicleSignal(structured.vehicle, signal);
          break;
        case SignalSources.ENVIRONMENT:
          this.mergeEnvironmentSignal(structured.environment, signal);
          break;
        case SignalSources.BIOMETRIC:
          this.mergeBiometricSignal(structured.biometric, signal);
          break;
        case SignalSources.VOICE:
          structured.user_query = {
            text: signal.value?.text || '',
            intent: signal.value?.intent || 'creative',
            confidence: signal.confidence
          };
          break;
      }
    }

    return structured;
  }

  mergeVehicleSignal(vehicle, signal) {
    if (signal.type === 'vehicle_speed') {
      vehicle.speed_kmh = signal.value?.speed_kmh || Math.round(signal.value?.vehicle_speed * 200);
    }
    if (signal.type === 'passenger_count') {
      vehicle.passenger_count = signal.value?.passenger_count || 0;
    }
    if (signal.type === 'gear_position') {
      vehicle.gear = signal.value;
    }
  }

  mergeEnvironmentSignal(environment, signal) {
    if (signal.type === 'time_of_day') {
      environment.time_of_day = signal.value?.time_of_day || 0.5;
    }
    if (signal.type === 'weather') {
      environment.weather = signal.value?.weather || 'clear';
    }
    if (signal.type === 'temperature') {
      environment.temperature = signal.value;
    }
  }

  mergeBiometricSignal(biometric, signal) {
    if (signal.type === 'heart_rate') {
      biometric.heart_rate = signal.raw_value || signal.value;
    }
    if (signal.type === 'fatigue_level') {
      biometric.fatigue_level = signal.raw_value || signal.value;
    }
    if (signal.type === 'stress_level') {
      biometric.stress_level = signal.raw_value || signal.value;
    }
  }

  calculateOverallConfidence(signals) {
    if (signals.length === 0) {
      return { overall: 0, bySource: {} };
    }

    const bySource = {};
    const sourceSignals = {};

    for (const signal of signals) {
      const source = signal.source;
      if (!sourceSignals[source]) {
        sourceSignals[source] = [];
      }
      sourceSignals[source].push(signal.confidence);
    }

    for (const [source, confidences] of Object.entries(sourceSignals)) {
      bySource[source] = confidences.reduce((a, b) => a + b, 0) / confidences.length;
    }

    const overall = Object.values(bySource).reduce((a, b) => a + b, 0) / Object.keys(bySource).length;

    return { overall, bySource };
  }

  validateOutput(output) {
    return validator.validate(output);
  }

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

  clear() {
    this.signalBuffer.clear();
    this.outputHistory = [];
  }

  getHistory(limit = 10) {
    return this.outputHistory.slice(-limit);
  }
}

const perceptionLayer = new PerceptionLayer();

module.exports = {
  PerceptionLayer,
  perceptionLayer,
  SignalSources,
  SignalCategories,
  SignalConfigs
};

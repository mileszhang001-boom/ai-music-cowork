'use strict';

const SCHEMA_VERSION = '1.0';

const SignalsSchema = {
  type: 'object',
  required: ['version', 'timestamp', 'signals', 'confidence'],
  properties: {
    version: { type: 'string', const: '1.0' },
    timestamp: { type: 'string', format: 'date-time' },
    signals: {
      type: 'object',
      properties: {
        vehicle: {
          type: 'object',
          properties: {
            speed_kmh: { type: 'number', minimum: 0, maximum: 300 },
            passenger_count: { type: 'integer', minimum: 0, maximum: 8 },
            gear: { type: 'string' }
          }
        },
        environment: {
          type: 'object',
          properties: {
            time_of_day: { type: 'number', minimum: 0, maximum: 1 },
            weather: { type: 'string' },
            temperature: { type: 'number' }
          }
        },
        biometric: {
          type: 'object',
          properties: {
            heart_rate: { type: 'number', minimum: 40, maximum: 200 },
            fatigue_level: { type: 'number', minimum: 0, maximum: 1 },
            stress_level: { type: 'number', minimum: 0, maximum: 1 }
          }
        },
        user_query: {
          type: 'object',
          properties: {
            text: { type: 'string' },
            intent: { type: 'string' },
            confidence: { type: 'number', minimum: 0, maximum: 1 }
          }
        }
      }
    },
    confidence: {
      type: 'object',
      required: ['overall'],
      properties: {
        overall: { type: 'number', minimum: 0, maximum: 1 },
        by_source: { type: 'object' }
      }
    },
    raw_signals: { type: 'array' }
  }
};

class Validator {
  validate(output) {
    const errors = [];

    if (!output.version) {
      errors.push('Missing version field');
    }

    if (!output.timestamp) {
      errors.push('Missing timestamp field');
    }

    if (!output.signals || typeof output.signals !== 'object') {
      errors.push('Missing or invalid signals object');
    }

    if (!output.confidence || typeof output.confidence.overall !== 'number') {
      errors.push('Missing or invalid confidence.overall');
    }

    if (output.confidence.overall < 0 || output.confidence.overall > 1) {
      errors.push('confidence.overall must be between 0 and 1');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  validateSignal(signal) {
    const errors = [];

    if (!signal.source) {
      errors.push('Signal missing source field');
    }

    if (!signal.type) {
      errors.push('Signal missing type field');
    }

    if (signal.value === undefined) {
      errors.push('Signal missing value field');
    }

    if (typeof signal.confidence !== 'number' || signal.confidence < 0 || signal.confidence > 1) {
      errors.push('Signal confidence must be a number between 0 and 1');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
}

const validator = new Validator();

module.exports = {
  Validator,
  validator,
  SignalsSchema,
  SCHEMA_VERSION
};

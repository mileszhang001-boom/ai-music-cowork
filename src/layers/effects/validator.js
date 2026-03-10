'use strict';

const EffectsSchema = {
  type: 'object',
  required: ['version', 'scene_id', 'commands'],
  properties: {
    version: { type: 'string' },
    scene_id: { type: 'string' },
    commands: {
      type: 'object',
      properties: {
        content: { type: 'object' },
        lighting: { type: 'object' },
        audio: { type: 'object' }
      }
    },
    execution_report: {
      type: 'object',
      properties: {
        status: { type: 'string' },
        timestamp: { type: 'string' },
        execution_time_ms: { type: 'number' }
      }
    }
  }
};

class Validator {
  validate(output) {
    const errors = [];

    if (!output.version) {
      errors.push('Missing version field');
    }

    if (!output.scene_id) {
      errors.push('Missing scene_id field');
    }

    if (!output.commands || typeof output.commands !== 'object') {
      errors.push('Missing or invalid commands object');
    }

    if (output.commands) {
      if (output.commands.lighting) {
        if (typeof output.commands.lighting.intensity === 'number') {
          if (output.commands.lighting.intensity < 0 || output.commands.lighting.intensity > 1) {
            errors.push('lighting.intensity must be between 0 and 1');
          }
        }
      }
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
  EffectsSchema
};

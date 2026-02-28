'use strict';

const DescriptorSchema = {
  type: 'object',
  required: ['version', 'scene_id', 'intent', 'hints'],
  properties: {
    version: { type: 'string' },
    scene_id: { type: 'string' },
    scene_type: { type: 'string' },
    scene_name: { type: 'string' },
    scene_narrative: { type: 'string' },
    intent: {
      type: 'object',
      required: ['mood', 'energy_level'],
      properties: {
        mood: {
          type: 'object',
          properties: {
            valence: { type: 'number', minimum: 0, maximum: 1 },
            arousal: { type: 'number', minimum: 0, maximum: 1 }
          }
        },
        energy_level: { type: 'number', minimum: 0, maximum: 1 },
        atmosphere: { type: 'string' },
        constraints: { type: 'object' }
      }
    },
    hints: {
      type: 'object',
      properties: {
        music: { type: 'object' },
        lighting: { type: 'object' },
        audio: { type: 'object' }
      }
    },
    announcement: { type: 'string' },
    meta: { type: 'object' }
  }
};

class Validator {
  validate(descriptor) {
    const errors = [];

    if (!descriptor.version) {
      errors.push('Missing version field');
    }

    if (!descriptor.scene_id) {
      errors.push('Missing scene_id field');
    }

    if (!descriptor.intent) {
      errors.push('Missing intent field');
    } else {
      if (!descriptor.intent.mood) {
        errors.push('Missing intent.mood field');
      }
      if (typeof descriptor.intent.energy_level !== 'number') {
        errors.push('Missing or invalid intent.energy_level');
      }
    }

    if (!descriptor.hints) {
      errors.push('Missing hints field');
    }

    if (descriptor.intent?.energy_level !== undefined) {
      if (descriptor.intent.energy_level < 0 || descriptor.intent.energy_level > 1) {
        errors.push('intent.energy_level must be between 0 and 1');
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
  DescriptorSchema
};

'use strict';

const EngineTypes = {
  CONTENT: 'content',
  LIGHTING: 'lighting',
  AUDIO: 'audio'
};

const EffectStatus = {
  PENDING: 'pending',
  EXECUTING: 'executing',
  COMPLETED: 'completed',
  FAILED: 'failed'
};

module.exports = {
  EngineTypes,
  EffectStatus
};

'use strict';

const assert = require('assert');
const { Orchestrator, EngineTypes, OrchestratorState } = require('../../src/core/orchestrator');

describe('Orchestrator', () => {
  let orchestrator;
  let mockContentEngine;
  let mockLightingEngine;
  let mockAudioEngine;

  beforeEach(() => {
    orchestrator = new Orchestrator();
    
    mockContentEngine = {
      execute: async (action, params) => ({ action, result: 'content_ok' })
    };
    
    mockLightingEngine = {
      execute: async (action, params) => ({ action, result: 'lighting_ok' })
    };
    
    mockAudioEngine = {
      execute: async (action, params) => ({ action, result: 'audio_ok' })
    };

    orchestrator.registerEngine(EngineTypes.CONTENT, mockContentEngine);
    orchestrator.registerEngine(EngineTypes.LIGHTING, mockLightingEngine);
    orchestrator.registerEngine(EngineTypes.AUDIO, mockAudioEngine);
  });

  afterEach(() => {
    orchestrator.reset();
  });

  describe('registerEngine', () => {
    it('should register engines', () => {
      assert.ok(orchestrator.getEngine(EngineTypes.CONTENT));
      assert.ok(orchestrator.getEngine(EngineTypes.LIGHTING));
      assert.ok(orchestrator.getEngine(EngineTypes.AUDIO));
    });
  });

  describe('planActions', () => {
    it('should plan actions from intent and hints', () => {
      const intent = {
        transition: { type: 'fade', duration_ms: 2000 }
      };
      
      const hints = {
        music: { genres: ['pop'] },
        lighting: { color_theme: 'calm' },
        audio: { preset: 'standard' }
      };

      const actions = orchestrator.planActions(intent, hints);

      assert.ok(actions.length === 3);
      assert.ok(actions.find(a => a.engine === EngineTypes.AUDIO));
      assert.ok(actions.find(a => a.engine === EngineTypes.LIGHTING));
      assert.ok(actions.find(a => a.engine === EngineTypes.CONTENT));
    });
  });

  describe('getStatus', () => {
    it('should return current status', () => {
      const status = orchestrator.getStatus();

      assert.strictEqual(status.state, OrchestratorState.IDLE);
      assert.ok(Array.isArray(status.registered_engines));
    });
  });
});

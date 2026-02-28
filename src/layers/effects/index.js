'use strict';

const { orchestrator } = require('./orchestrator');
const { contentEngine } = require('./engines/content');
const { lightingEngine } = require('./engines/lighting');
const { audioEngine } = require('./engines/audio');
const { validator } = require('./validator');
const { EngineTypes, EffectStatus } = require('./types');
const { eventBus } = require('../../shared/eventBus');

const SCHEMA_VERSION = '1.0';

class EffectsLayer {
  constructor(config = {}) {
    this.config = {
      debug: config.debug || false,
      ...config
    };
    
    this._initEngines();
  }

  _initEngines() {
    orchestrator.registerEngine(EngineTypes.CONTENT, contentEngine);
    orchestrator.registerEngine(EngineTypes.LIGHTING, lightingEngine);
    orchestrator.registerEngine(EngineTypes.AUDIO, audioEngine);
  }

  async process(descriptor) {
    const startTime = Date.now();

    if (!descriptor) {
      return this.buildErrorOutput('No scene descriptor provided');
    }

    try {
      const executionResult = await orchestrator.execute(descriptor);

      const output = {
        version: SCHEMA_VERSION,
        scene_id: descriptor.scene_id,
        commands: executionResult.commands,
        execution_report: {
          status: executionResult.execution_report?.status || EffectStatus.COMPLETED,
          timestamp: executionResult.execution_report?.timestamp || new Date().toISOString(),
          execution_time_ms: Date.now() - startTime,
          details: {
            content: executionResult.results?.content ? 'success' : 'skipped',
            lighting: executionResult.results?.lighting ? 'success' : 'skipped',
            audio: executionResult.results?.audio ? 'success' : 'skipped'
          }
        },
        announcement: descriptor.announcement,
        _meta: {
          source_descriptor: descriptor.meta?.source || 'unknown',
          processing_time_ms: Date.now() - startTime
        }
      };

      const validation = validator.validate(output);
      if (!validation.valid && this.config.debug) {
        console.warn('[EffectsLayer] Validation warnings:', validation.errors);
      }

      eventBus.emit('effects.output_generated', output);

      return output;

    } catch (error) {
      if (this.config.debug) {
        console.error('[EffectsLayer] Execution error:', error.message);
      }
      return this.buildErrorOutput(error.message, descriptor.scene_id);
    }
  }

  buildErrorOutput(error, sceneId = null) {
    return {
      version: SCHEMA_VERSION,
      scene_id: sceneId || `error_${Date.now()}`,
      commands: {},
      execution_report: {
        status: EffectStatus.FAILED,
        timestamp: new Date().toISOString(),
        error: error
      },
      _meta: {
        error: true
      }
    };
  }

  getEngine(type) {
    return orchestrator.getEngine(type);
  }

  getHistory(limit = 10) {
    return orchestrator.getHistory(limit);
  }

  clear() {
    orchestrator.clear();
  }
}

const effectsLayer = new EffectsLayer();

module.exports = {
  EffectsLayer,
  effectsLayer,
  EngineTypes,
  EffectStatus
};

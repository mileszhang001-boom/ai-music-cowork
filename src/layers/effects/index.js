'use strict';

const { orchestrator } = require('./orchestrator');
const { contentEngine } = require('./engines/content');
const { lightingEngine } = require('./engines/lighting');
const { audioEngine } = require('./engines/audio');
const { validator } = require('./validator');
const { EngineTypes, EffectStatus } = require('./types');
const { eventBus } = require('../../shared/eventBus');
const { MultiAgentOrchestrator } = require('../../core/agents');

const SCHEMA_VERSION = '2.0';

class EffectsLayer {
  constructor(config = {}) {
    this.config = {
      useMultiAgent: config.useMultiAgent || false,
      debug: config.debug || false,
      ...config
    };
    
    this.multiAgentOrchestrator = null;
    
    if (this.config.useMultiAgent) {
      this._initMultiAgent();
    } else {
      this._initEngines();
    }
  }

  _initMultiAgent() {
    this.multiAgentOrchestrator = new MultiAgentOrchestrator({
      debug: this.config.debug,
      enableLLM: this.config.enableLLM !== false
    });
    
    this.multiAgentOrchestrator.initialize({
      contentEngine,
      lightingEngine,
      audioEngine,
      llmClient: this.config.llmClient,
      ruleEngine: this.config.ruleEngine
    });
    
    if (this.config.debug) {
      console.log('[EffectsLayer] Multi-Agent Orchestrator initialized');
    }
  }

  _initEngines() {
    orchestrator.registerEngine(EngineTypes.CONTENT, contentEngine);
    orchestrator.registerEngine(EngineTypes.LIGHTING, lightingEngine);
    orchestrator.registerEngine(EngineTypes.AUDIO, audioEngine);
  }

  enableMultiAgent(config = {}) {
    if (!this.multiAgentOrchestrator) {
      this.config.useMultiAgent = true;
      this._initMultiAgent();
    }
    return this;
  }

  disableMultiAgent() {
    if (this.multiAgentOrchestrator) {
      this.multiAgentOrchestrator.clear();
      this.multiAgentOrchestrator = null;
    }
    this.config.useMultiAgent = false;
    this._initEngines();
    return this;
  }

  async process(descriptor) {
    const startTime = Date.now();

    if (!descriptor) {
      return this.buildErrorOutput('No scene descriptor provided');
    }

    if (this.config.useMultiAgent && this.multiAgentOrchestrator) {
      try {
        const result = await this.multiAgentOrchestrator.process(descriptor);
        
        const validation = validator.validate(result);
        if (!validation.valid && this.config.debug) {
          console.warn('[EffectsLayer] Validation warnings:', validation.errors);
        }

        eventBus.emit('effects.output_generated', result);
        
        return result;
        
      } catch (error) {
        if (this.config.debug) {
          console.error('[EffectsLayer] Multi-Agent error:', error.message);
        }
        return this.buildErrorOutput(error.message, descriptor.scene_id);
      }
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
    if (this.config.useMultiAgent && this.multiAgentOrchestrator) {
      return this.multiAgentOrchestrator.getStatus();
    }
    return orchestrator.getEngine(type);
  }

  getMetrics() {
    if (this.multiAgentOrchestrator) {
      return this.multiAgentOrchestrator.getMetrics();
    }
    return null;
  }

  getHistory(limit = 10) {
    return orchestrator.getHistory(limit);
  }

  clear() {
    if (this.multiAgentOrchestrator) {
      this.multiAgentOrchestrator.clear();
    }
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

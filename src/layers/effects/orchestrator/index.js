'use strict';

const { EngineTypes, EffectStatus } = require('../types');
const { eventBus } = require('../../../shared/eventBus');

class Orchestrator {
  constructor(config = {}) {
    this.config = config;
    this.engines = new Map();
    this.currentSceneId = null;
    this.executionHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
  }

  registerEngine(type, engine) {
    this.engines.set(type, engine);
  }

  getEngine(type) {
    return this.engines.get(type) || null;
  }

  async execute(descriptor) {
    if (!descriptor) {
      return { status: 'error', message: 'No descriptor provided' };
    }

    const startTime = Date.now();
    this.currentSceneId = descriptor.scene_id;

    const commands = {};
    const results = {};

    if (descriptor.hints?.music) {
      const contentEngine = this.getEngine(EngineTypes.CONTENT);
      if (contentEngine) {
        try {
          results.content = await contentEngine.execute('curate_playlist', {
            hints: descriptor.hints.music,
            constraints: descriptor.intent?.constraints,
            scene_type: descriptor.scene_type
          });
          commands.content = {
            action: 'play_playlist',
            playlist: results.content?.playlist || [],
            transition: { type: 'crossfade', duration_ms: 3000 }
          };
        } catch (err) {
          results.content = { error: err.message };
        }
      }
    }

    if (descriptor.hints?.lighting) {
      const lightingEngine = this.getEngine(EngineTypes.LIGHTING);
      if (lightingEngine) {
        try {
          results.lighting = await lightingEngine.execute('apply_theme', {
            theme: descriptor.hints.lighting.color_theme,
            pattern: descriptor.hints.lighting.pattern,
            intensity: descriptor.hints.lighting.intensity
          });
          commands.lighting = {
            action: 'apply_theme',
            theme: descriptor.hints.lighting.color_theme,
            colors: results.lighting?.colors || {},
            pattern: descriptor.hints.lighting.pattern,
            intensity: descriptor.hints.lighting.intensity
          };
        } catch (err) {
          results.lighting = { error: err.message };
        }
      }
    }

    if (descriptor.hints?.audio) {
      const audioEngine = this.getEngine(EngineTypes.AUDIO);
      if (audioEngine) {
        try {
          results.audio = await audioEngine.execute('apply_preset', {
            preset: descriptor.hints.audio.preset
          });
          commands.audio = {
            action: 'apply_preset',
            preset: descriptor.hints.audio.preset,
            eq: results.audio?.settings || {}
          };
        } catch (err) {
          results.audio = { error: err.message };
        }
      }
    }

    const executionTime = Date.now() - startTime;

    this.executionHistory.push({
      scene_id: descriptor.scene_id,
      timestamp: Date.now(),
      execution_time_ms: executionTime,
      success: !Object.values(results).some(r => r.error)
    });

    if (this.executionHistory.length > this.maxHistorySize) {
      this.executionHistory.shift();
    }

    eventBus.emit('effects.executed', {
      scene_id: descriptor.scene_id,
      commands,
      execution_time_ms: executionTime
    });

    return {
      scene_id: descriptor.scene_id,
      commands,
      results,
      execution_report: {
        status: EffectStatus.COMPLETED,
        timestamp: new Date().toISOString(),
        execution_time_ms: executionTime
      }
    };
  }

  getHistory(limit = 10) {
    return this.executionHistory.slice(-limit);
  }

  clear() {
    this.currentSceneId = null;
    this.executionHistory = [];
  }
}

const orchestrator = new Orchestrator();

module.exports = {
  Orchestrator,
  orchestrator
};

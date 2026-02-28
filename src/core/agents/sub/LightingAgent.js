'use strict';

const { SubAgent } = require('./SubAgent');

class LightingAgent extends SubAgent {
  constructor(config = {}) {
    super({
      name: config.name || 'LightingAgent',
      agentType: 'lighting',
      mainAgentId: config.mainAgentId || 'main',
      maxIterations: config.maxIterations || 3,
      maxSteps: config.maxSteps || 8,
      debug: config.debug
    });

    this.lightingEngine = config.lightingEngine || null;
    this.currentEffect = null;
    this.pendingCommands = [];
  }

  setLightingEngine(engine) {
    this.lightingEngine = engine;
  }

  async executeTask(task) {
    this.setContext({
      intent: task.params?.intent || {},
      hints: task.params?.hints || {},
      constraints: task.params?.constraints || {}
    });

    return super.executeTask(task);
  }

  async controlLighting(params) {
    const { brightness, color, zone } = params;

    if (this.lightingEngine) {
      try {
        const result = await this.lightingEngine.execute('set_lighting', {
          brightness,
          color,
          zone: zone || 'all'
        });
        return result;
      } catch (error) {
        return { error: error.message };
      }
    }

    return this.fallbackControlLighting(params);
  }

  fallbackControlLighting(params) {
    return {
      success: true,
      commands: [{
        action: 'set_brightness',
        value: params.brightness || 0.8,
        zone: params.zone || 'all'
      }, {
        action: 'set_color',
        value: params.color || '#FFFFFF',
        zone: params.zone || 'all'
      }],
      source: 'fallback'
    };
  }

  async generateLightingEffect(params) {
    const { intent, hints } = this.context;
    
    const effectParams = {
      energy: intent?.energy || 'medium',
      mood: intent?.mood || 'neutral',
      colors: hints?.lighting?.colors || ['#FFFFFF'],
      pattern: hints?.lighting?.pattern || 'solid',
      transition: hints?.lighting?.transition || 'smooth'
    };

    if (this.lightingEngine) {
      try {
        const result = await this.lightingEngine.execute('generate_effect', effectParams);
        this.currentEffect = result.effect;
        return result;
      } catch (error) {
        return { error: error.message };
      }
    }

    return this.fallbackGenerateEffect(effectParams);
  }

  fallbackGenerateEffect(params) {
    const colorMap = {
      high_energetic: ['#FF6B6B', '#FFE66D', '#4ECDC4'],
      medium_energetic: ['#A8E6CF', '#88D8B0', '#FFEAA7'],
      low_energetic: ['#74B9FF', '#A29BFE', '#DFE6E9'],
      joyful: ['#FFE66D', '#FF6B6B', '#4ECDC4'],
      melancholic: ['#6C5CE7', '#A29BFE', '#74B9FF'],
      calm: ['#00B894', '#55EFC4', '#81ECEC']
    };

    const key = `${params.energy}_${params.mood}`;
    const colors = colorMap[key] || colorMap.medium_energetic;

    const effect = {
      type: 'color_transition',
      colors,
      pattern: params.pattern,
      transition_duration_ms: params.transition === 'smooth' ? 2000 : 500,
      loop: true,
      brightness: 0.8
    };

    return {
      effect,
      commands: [{
        action: 'play_effect',
        effect
      }],
      source: 'fallback'
    };
  }

  async smallStepExecute(action, params) {
    switch (action) {
      case 'control_lighting':
        return await this.controlLighting(params);
      case 'generate_effect':
        return await this.generateLightingEffect(params);
      default:
        return await super.smallStepExecute(action, params);
    }
  }

  getCurrentEffect() {
    return this.currentEffect;
  }

  stopEffect() {
    this.currentEffect = null;
    this.pendingCommands = [];
    
    return {
      commands: [{
        action: 'stop_effect',
        zone: 'all'
      }]
    };
  }
}

module.exports = {
  LightingAgent
};

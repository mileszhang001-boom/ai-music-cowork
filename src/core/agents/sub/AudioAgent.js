'use strict';

const { SubAgent } = require('./SubAgent');

class AudioAgent extends SubAgent {
  constructor(config = {}) {
    super({
      name: config.name || 'AudioAgent',
      agentType: 'audio',
      mainAgentId: config.mainAgentId || 'main',
      maxIterations: config.maxIterations || 3,
      maxSteps: config.maxSteps || 6,
      debug: config.debug
    });

    this.audioEngine = config.audioEngine || null;
    this.currentPreset = null;
    this.currentDSP = null;
  }

  setAudioEngine(engine) {
    this.audioEngine = engine;
  }

  async executeTask(task) {
    this.setContext({
      intent: task.params?.intent || {},
      hints: task.params?.hints || {},
      constraints: task.params?.constraints || {}
    });

    return super.executeTask(task);
  }

  async applyAudioPreset(params) {
    const { preset_name, energy } = params;
    
    const presetMap = {
      high_energetic: 'bass_boost',
      medium_energetic: 'balanced',
      low_energetic: 'soft',
      default: 'balanced'
    };

    const preset = preset_name || presetMap[energy] || presetMap.default;

    if (this.audioEngine) {
      try {
        const result = await this.audioEngine.execute('apply_preset', { preset });
        this.currentPreset = result.preset;
        return result;
      } catch (error) {
        return { error: error.message };
      }
    }

    return this.fallbackApplyPreset(preset);
  }

  fallbackApplyPreset(preset) {
    const presets = {
      bass_boost: {
        name: 'bass_boost',
        eq: { low: 6, mid: 0, high: -2 },
        reverb: 0.2,
        surround: true
      },
      balanced: {
        name: 'balanced',
        eq: { low: 0, mid: 0, high: 0 },
        reverb: 0.3,
        surround: false
      },
      soft: {
        name: 'soft',
        eq: { low: -3, mid: 2, high: 1 },
        reverb: 0.5,
        surround: false
      },
      vocal: {
        name: 'vocal',
        eq: { low: -2, mid: 4, high: 2 },
        reverb: 0.4,
        surround: false
      }
    };

    const selectedPreset = presets[preset] || presets.balanced;
    this.currentPreset = selectedPreset;

    return {
      preset: selectedPreset,
      commands: [{
        action: 'apply_eq',
        params: selectedPreset.eq
      }, {
        action: 'set_reverb',
        value: selectedPreset.reverb
      }],
      source: 'fallback'
    };
  }

  async adjustDSP(params) {
    const { intent, hints } = this.context;
    
    const dspParams = {
      energy: intent?.energy || 'medium',
      mood: intent?.mood || 'neutral',
      spatial: hints?.audio?.spatial || 'stereo',
      bass_boost: hints?.audio?.bass_boost || false,
      clarity: hints?.audio?.clarity || 'normal'
    };

    if (this.audioEngine) {
      try {
        const result = await this.audioEngine.execute('adjust_dsp', dspParams);
        this.currentDSP = result.dsp_params;
        return result;
      } catch (error) {
        return { error: error.message };
      }
    }

    return this.fallbackAdjustDSP(dspParams);
  }

  fallbackAdjustDSP(params) {
    const dspConfigs = {
      high_energetic: {
        eq_bands: [5, 2, 0, -1, -2],
        compression: 'aggressive',
        surround: true,
        bass_boost: true,
        clarity: 'high'
      },
      medium_energetic: {
        eq_bands: [2, 1, 0, 0, -1],
        compression: 'moderate',
        surround: false,
        bass_boost: false,
        clarity: 'normal'
      },
      low_energetic: {
        eq_bands: [-2, 0, 2, 3, 2],
        compression: 'light',
        surround: false,
        bass_boost: false,
        clarity: 'enhanced'
      }
    };

    const key = params.energy;
    const config = dspConfigs[key] || dspConfigs.medium_energetic;
    
    this.currentDSP = config;

    return {
      dsp_params: config,
      commands: [{
        action: 'set_eq',
        bands: config.eq_bands
      }, {
        action: 'set_compression',
        mode: config.compression
      }, {
        action: 'set_surround',
        enabled: config.surround
      }],
      source: 'fallback'
    };
  }

  async smallStepExecute(action, params) {
    switch (action) {
      case 'apply_audio_preset':
        return await this.applyAudioPreset(params);
      case 'adjust_dsp':
        return await this.adjustDSP(params);
      default:
        return await super.smallStepExecute(action, params);
    }
  }

  getCurrentPreset() {
    return this.currentPreset;
  }

  getCurrentDSP() {
    return this.currentDSP;
  }

  resetAudio() {
    this.currentPreset = null;
    this.currentDSP = null;
    
    return {
      commands: [{
        action: 'reset_eq'
      }, {
        action: 'reset_reverb'
      }, {
        action: 'reset_compression'
      }]
    };
  }
}

module.exports = {
  AudioAgent
};

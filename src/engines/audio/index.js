'use strict';

/**
 * @fileoverview Audio Engine V2 - 音效引擎增强版
 * @description 支持引擎间协作、动态预设调整、Hints 处理
 */

const { eventBus, EventTypes } = require('../../core/eventBus');

const AudioPresets = {
  STANDARD: {
    name: 'standard',
    bass: 0,
    mid: 0,
    treble: 0,
    spatial: false,
    surround_level: 0
  },
  BASS_BOOST: {
    name: 'bass_boost',
    bass: 6,
    mid: 0,
    treble: -2,
    spatial: false,
    surround_level: 0
  },
  VOCAL_CLARITY: {
    name: 'vocal_clarity',
    bass: -2,
    mid: 4,
    treble: 2,
    spatial: false,
    surround_level: 0
  },
  CONCERT: {
    name: 'concert',
    bass: 4,
    mid: 2,
    treble: 3,
    spatial: true,
    surround_level: 0.7
  },
  NIGHT_MODE: {
    name: 'night_mode',
    bass: -4,
    mid: -2,
    treble: -4,
    spatial: false,
    surround_level: 0
  },
  FOCUS: {
    name: 'focus',
    bass: -2,
    mid: 0,
    treble: 1,
    spatial: false,
    surround_level: 0
  },
  PARTY: {
    name: 'party',
    bass: 8,
    mid: 2,
    treble: 4,
    spatial: true,
    surround_level: 1.0
  }
};

const HintPriority = {
  REQUIRED: 'required',
  PREFERRED: 'preferred',
  SUGGESTED: 'suggested'
};

class AudioEngine {
  constructor(config = {}) {
    this.config = config;
    this.name = 'audio';
    this.version = '2.0';
    this.currentPreset = AudioPresets.STANDARD;
    this.volume = 50;
    this.muted = false;
    this.spatialMode = null;
    this.collaborationState = {
      autoAdjustEnabled: true,
      lastAdjustment: null
    };
    this.energyMapping = {
      low: { preset: 'night_mode', volumeModifier: -10 },
      medium: { preset: 'standard', volumeModifier: 0 },
      high: { preset: 'concert', volumeModifier: 5 }
    };
    
    this.setupEventListeners();
  }

  setupEventListeners() {
    eventBus.on(EventTypes.ENGINE_ACTION, (data) => {
      if (data.engine === 'content' && data.action === 'track_changed') {
        this.onTrackChanged(data.data);
      }
      if (data.engine === 'content' && data.action === 'playlist_ready') {
        this.onPlaylistReady(data.data);
      }
    });
  }

  async execute(action, params) {
    switch (action) {
    case 'apply_preset':
      return this.applyPreset(params);
    case 'set_volume':
      return this.setVolume(params);
    case 'set_eq':
      return this.setEQ(params);
    case 'toggle_spatial':
      return this.toggleSpatial(params);
    case 'mute':
      return this.mute();
    case 'unmute':
      return this.unmute();
    case 'adjust_for_energy':
      return this.adjustForEnergy(params);
    case 'auto_adjust':
      return this.autoAdjust(params);
    default:
      throw new Error(`Unknown action: ${action}`);
    }
  }

  applyPreset(params) {
    const { preset, transition, hints, constraints } = params;
    
    const processedHints = this.processHints(hints);
    const processedConstraints = this.processConstraints(constraints);
    
    let presetConfig = this.resolvePreset(preset);
    
    if (processedHints.bass?.applied) {
      presetConfig = { ...presetConfig, bass: processedHints.bass.value };
    }
    if (processedHints.treble?.applied) {
      presetConfig = { ...presetConfig, treble: processedHints.treble.value };
    }
    if (processedHints.spatial?.applied) {
      presetConfig = { ...presetConfig, spatial: processedHints.spatial.value };
    }

    if (processedConstraints.maxVolume) {
      this.volume = Math.min(this.volume, processedConstraints.maxVolume.value);
    }

    this.currentPreset = presetConfig;

    const result = {
      preset: this.currentPreset.name,
      settings: this.currentPreset,
      transition: transition || { type: 'fade', duration_ms: 1000 },
      hints_applied: processedHints.applied,
      constraints_applied: Object.keys(processedConstraints).length
    };

    this.emitCollaborationEvent('preset_applied', {
      preset: this.currentPreset.name
    });

    return result;
  }

  processHints(hints) {
    if (!hints || !hints.audio) {
      return { applied: 0 };
    }

    const audioHints = hints.audio;
    const processed = {
      preset: audioHints.preset ? { applied: true, value: audioHints.preset } : null,
      bass: audioHints.bass !== undefined ? { applied: true, value: audioHints.bass } : null,
      treble: audioHints.treble !== undefined ? { applied: true, value: audioHints.treble } : null,
      spatial: audioHints.spatial !== undefined ? { applied: true, value: audioHints.spatial } : null
    };

    processed.applied = Object.values(processed).filter(v => v?.applied).length;
    return processed;
  }

  processConstraints(constraints) {
    if (!constraints) return {};

    const processed = {};

    if (constraints.max_volume_db !== undefined) {
      processed.maxVolume = {
        type: 'hard',
        value: this.dbToVolume(constraints.max_volume_db)
      };
    }

    if (constraints.no_spatial === true) {
      processed.noSpatial = {
        type: 'hard',
        value: true
      };
    }

    return processed;
  }

  dbToVolume(db) {
    return Math.max(0, Math.min(100, 50 + db * 5));
  }

  resolvePreset(presetName) {
    const presetMap = {
      'standard': AudioPresets.STANDARD,
      'bass_boost': AudioPresets.BASS_BOOST,
      'vocal_clarity': AudioPresets.VOCAL_CLARITY,
      'concert': AudioPresets.CONCERT,
      'night_mode': AudioPresets.NIGHT_MODE,
      'focus': AudioPresets.FOCUS,
      'party': AudioPresets.PARTY
    };

    return { ...presetMap[presetName?.toLowerCase()] } || { ...AudioPresets.STANDARD };
  }

  setVolume(params) {
    const { volume, relative } = params;
    
    if (relative) {
      this.volume = Math.max(0, Math.min(100, this.volume + relative));
    } else if (volume !== undefined) {
      this.volume = Math.max(0, Math.min(100, volume));
    }

    this.emitCollaborationEvent('volume_changed', {
      volume: this.volume
    });

    return { volume: this.volume };
  }

  setEQ(params) {
    const { bass, mid, treble } = params;
    
    if (bass !== undefined) this.currentPreset.bass = Math.max(-10, Math.min(10, bass));
    if (mid !== undefined) this.currentPreset.mid = Math.max(-10, Math.min(10, mid));
    if (treble !== undefined) this.currentPreset.treble = Math.max(-10, Math.min(10, treble));

    return {
      eq: {
        bass: this.currentPreset.bass,
        mid: this.currentPreset.mid,
        treble: this.currentPreset.treble
      }
    };
  }

  toggleSpatial(params) {
    const { enabled, mode } = params;
    
    this.currentPreset.spatial = enabled !== undefined ? enabled : !this.currentPreset.spatial;
    this.spatialMode = mode || 'default';

    return {
      spatial_enabled: this.currentPreset.spatial,
      spatial_mode: this.spatialMode
    };
  }

  mute() {
    this.muted = true;
    return { muted: true };
  }

  unmute() {
    this.muted = false;
    return { muted: false };
  }

  adjustForEnergy(params) {
    const { energy } = params;
    
    let mapping;
    if (energy < 0.3) {
      mapping = this.energyMapping.low;
    } else if (energy < 0.7) {
      mapping = this.energyMapping.medium;
    } else {
      mapping = this.energyMapping.high;
    }

    this.currentPreset = this.resolvePreset(mapping.preset);
    this.volume = Math.max(0, Math.min(100, this.volume + mapping.volumeModifier));
    this.collaborationState.lastAdjustment = {
      energy,
      preset: mapping.preset,
      timestamp: Date.now()
    };

    return {
      preset: this.currentPreset.name,
      volume: this.volume
    };
  }

  autoAdjust(params) {
    const { context } = params;
    
    if (!this.collaborationState.autoAdjustEnabled) {
      return { adjusted: false, reason: 'auto_adjust_disabled' };
    }

    let adjustments = [];

    if (context?.time === 'night') {
      this.currentPreset = this.resolvePreset('night_mode');
      adjustments.push('night_mode_preset');
    }

    if (context?.passengerCount > 2) {
      this.currentPreset.surround_level = Math.min(1, this.currentPreset.surround_level + 0.3);
      adjustments.push('increased_surround');
    }

    if (context?.fatigueLevel > 0.7) {
      this.currentPreset = this.resolvePreset('vocal_clarity');
      adjustments.push('vocal_clarity_for_fatigue');
    }

    return {
      adjusted: adjustments.length > 0,
      adjustments
    };
  }

  onTrackChanged(data) {
    if (data.energy !== undefined && this.collaborationState.autoAdjustEnabled) {
      this.adjustForEnergy({ energy: data.energy });
    }
  }

  onPlaylistReady(data) {
    if (data.energy_profile && this.collaborationState.autoAdjustEnabled) {
      const avgEnergy = data.energy_profile.average;
      this.adjustForEnergy({ energy: avgEnergy });
    }
  }

  emitCollaborationEvent(event, data) {
    eventBus.emit(EventTypes.ENGINE_ACTION, {
      engine: 'audio',
      action: event,
      data: data
    });
  }

  generateFeedbackReport() {
    return {
      engine: this.name,
      timestamp: Date.now(),
      state: {
        preset: this.currentPreset.name,
        volume: this.volume,
        muted: this.muted,
        spatial: this.currentPreset.spatial
      },
      collaboration: this.collaborationState
    };
  }

  getStatus() {
    return {
      name: this.name,
      version: this.version,
      preset: this.currentPreset,
      volume: this.volume,
      muted: this.muted,
      spatial_mode: this.spatialMode,
      collaboration: this.collaborationState
    };
  }
}

const audioEngine = new AudioEngine();

module.exports = {
  AudioEngine,
  audioEngine,
  AudioPresets,
  HintPriority
};

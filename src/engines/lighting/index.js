'use strict';

/**
 * @fileoverview Lighting Engine V2 - 灯光引擎增强版
 * @description 支持引擎间协作、音乐节拍同步、动态主题调整
 */

const { eventBus, EventTypes } = require('../../core/eventBus');

const ColorThemes = {
  WARM: { primary: '#FFA500', secondary: '#FFD700', accent: '#FF6347' },
  COOL: { primary: '#4169E1', secondary: '#87CEEB', accent: '#00CED1' },
  ROMANTIC: { primary: '#FF69B4', secondary: '#FFB6C1', accent: '#DDA0DD' },
  ENERGETIC: { primary: '#FF4500', secondary: '#FFD700', accent: '#00FF00' },
  CALM: { primary: '#2E8B57', secondary: '#98FB98', accent: '#00FA9A' },
  FOCUS: { primary: '#4682B4', secondary: '#B0C4DE', accent: '#778899' },
  NIGHT: { primary: '#191970', secondary: '#483D8B', accent: '#6A5ACD' }
};

const Patterns = {
  STATIC: 'static',
  BREATHING: 'breathing',
  PULSE: 'pulse',
  WAVE: 'wave',
  MUSIC_SYNC: 'music_sync'
};

const HintPriority = {
  REQUIRED: 'required',
  PREFERRED: 'preferred',
  SUGGESTED: 'suggested'
};

class LightingEngine {
  constructor(config = {}) {
    this.config = config;
    this.name = 'lighting';
    this.version = '2.0';
    this.currentTheme = null;
    this.currentPattern = Patterns.BREATHING;
    this.intensity = 0.5;
    this.zones = new Map();
    this.collaborationState = {
      beatSync: false,
      musicSyncEnabled: false,
      lastBeatTime: null,
      beatCount: 0
    };
    this.energyMapping = {
      low: { theme: 'calm', intensity: 0.3, pattern: 'breathing' },
      medium: { theme: 'cool', intensity: 0.5, pattern: 'breathing' },
      high: { theme: 'energetic', intensity: 0.7, pattern: 'pulse' }
    };
    
    this.setupEventListeners();
  }

  setupEventListeners() {
    eventBus.on(EventTypes.MUSIC_BEAT, (data) => {
      this.onBeat(data);
    });

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
      case 'apply_theme':
        return this.applyTheme(params);
      case 'set_intensity':
        return this.setIntensity(params);
      case 'set_pattern':
        return this.setPattern(params);
      case 'zone_control':
        return this.zoneControl(params);
      case 'sync_with_music':
        return this.syncWithMusic(params);
      case 'adjust_for_energy':
        return this.adjustForEnergy(params);
      default:
        throw new Error(`Unknown action: ${action}`);
    }
  }

  applyTheme(params) {
    const { theme, pattern, intensity, transition, hints, constraints } = params;

    const processedHints = this.processHints(hints);
    const processedConstraints = this.processConstraints(constraints);

    let finalTheme = theme;
    let finalPattern = pattern;
    let finalIntensity = intensity;

    if (processedHints.theme?.applied) {
      finalTheme = processedHints.theme.value;
    }
    if (processedHints.pattern?.applied) {
      finalPattern = processedHints.pattern.value;
    }
    if (processedHints.intensity?.applied) {
      finalIntensity = processedHints.intensity.value;
    }

    if (processedConstraints.maxIntensity) {
      finalIntensity = Math.min(finalIntensity, processedConstraints.maxIntensity.value);
    }

    const colorTheme = this.resolveTheme(finalTheme);
    this.currentTheme = colorTheme;
    this.currentPattern = finalPattern || Patterns.BREATHING;
    this.intensity = finalIntensity !== undefined ? finalIntensity : 0.5;

    const result = {
      theme: finalTheme,
      colors: colorTheme,
      pattern: this.currentPattern,
      intensity: this.intensity,
      transition: transition || { type: 'fade', duration_ms: 2000 },
      hints_applied: processedHints.applied,
      constraints_applied: Object.keys(processedConstraints).length
    };

    this.applyToAllZones(result);

    this.emitCollaborationEvent('theme_applied', {
      theme: finalTheme,
      intensity: this.intensity
    });

    return result;
  }

  processHints(hints) {
    if (!hints || !hints.lighting) {
      return { applied: 0 };
    }

    const lightingHints = hints.lighting;
    const processed = {
      theme: lightingHints.color_theme ? { applied: true, value: lightingHints.color_theme } : null,
      pattern: lightingHints.pattern ? { applied: true, value: lightingHints.pattern } : null,
      intensity: lightingHints.intensity !== undefined ? { applied: true, value: lightingHints.intensity } : null
    };

    processed.applied = Object.values(processed).filter(v => v?.applied).length;
    return processed;
  }

  processConstraints(constraints) {
    if (!constraints) return {};

    const processed = {};

    if (constraints.max_lighting_intensity !== undefined) {
      processed.maxIntensity = {
        type: 'hard',
        value: constraints.max_lighting_intensity
      };
    }

    if (constraints.no_flash === true) {
      processed.noFlash = {
        type: 'hard',
        value: true
      };
    }

    return processed;
  }

  resolveTheme(themeName) {
    const themeMap = {
      'warm': ColorThemes.WARM,
      'cool': ColorThemes.COOL,
      'romantic': ColorThemes.ROMANTIC,
      'energetic': ColorThemes.ENERGETIC,
      'calm': ColorThemes.CALM,
      'focus': ColorThemes.FOCUS,
      'night': ColorThemes.NIGHT
    };

    return themeMap[themeName?.toLowerCase()] || ColorThemes.CALM;
  }

  setIntensity(params) {
    this.intensity = Math.max(0, Math.min(1, params.intensity));
    
    this.emitCollaborationEvent('intensity_changed', {
      intensity: this.intensity
    });
    
    return { intensity: this.intensity };
  }

  setPattern(params) {
    this.currentPattern = params.pattern;
    
    if (params.pattern === Patterns.MUSIC_SYNC) {
      this.collaborationState.musicSyncEnabled = true;
    }
    
    return { pattern: this.currentPattern };
  }

  zoneControl(params) {
    const { zone, color, intensity, pattern } = params;
    
    this.zones.set(zone, {
      color: color,
      intensity: intensity !== undefined ? intensity : this.intensity,
      pattern: pattern || this.currentPattern
    });

    return {
      zone: zone,
      settings: this.zones.get(zone)
    };
  }

  syncWithMusic(params) {
    const { enabled, sensitivity } = params;
    
    this.collaborationState.musicSyncEnabled = enabled;
    this.collaborationState.beatSync = enabled;
    this.collaborationState.sensitivity = sensitivity || 0.7;
    
    if (enabled) {
      this.currentPattern = Patterns.MUSIC_SYNC;
    }

    return {
      music_sync: enabled,
      pattern: this.currentPattern
    };
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

    const colorTheme = this.resolveTheme(mapping.theme);
    this.currentTheme = colorTheme;
    this.intensity = mapping.intensity;
    this.currentPattern = mapping.pattern;

    this.applyToAllZones({
      colors: colorTheme,
      intensity: this.intensity,
      pattern: this.currentPattern
    });

    return {
      theme: mapping.theme,
      intensity: this.intensity,
      pattern: this.currentPattern
    };
  }

  onBeat(data) {
    if (!this.collaborationState.musicSyncEnabled) return;
    
    this.collaborationState.lastBeatTime = Date.now();
    this.collaborationState.beatCount++;
    
    const intensity = Math.min(1, this.intensity + (data.intensity || 0.5) * 0.2);
    
    this.applyBeatEffect(intensity);
  }

  applyBeatEffect(intensity) {
    const zones = ['dashboard', 'door_left', 'door_right'];
    
    for (const zone of zones) {
      const zoneSettings = this.zones.get(zone) || {};
      this.zones.set(zone, {
        ...zoneSettings,
        colors: this.currentTheme,
        intensity: intensity,
        pattern: Patterns.PULSE
      });
    }

    setTimeout(() => {
      this.restoreFromBeat();
    }, 200);
  }

  restoreFromBeat() {
    for (const [zone, settings] of this.zones) {
      this.zones.set(zone, {
        ...settings,
        intensity: this.intensity,
        pattern: this.currentPattern
      });
    }
  }

  onTrackChanged(data) {
    if (data.energy !== undefined) {
      this.adjustForEnergy({ energy: data.energy });
    }
  }

  onPlaylistReady(data) {
    if (data.energy_profile) {
      const avgEnergy = data.energy_profile.average;
      this.adjustForEnergy({ energy: avgEnergy });
    }
  }

  applyToAllZones(settings) {
    const defaultZones = ['dashboard', 'door_left', 'door_right', 'footwell', 'ceiling'];
    
    for (const zone of defaultZones) {
      this.zones.set(zone, {
        colors: settings.colors,
        intensity: settings.intensity,
        pattern: settings.pattern
      });
    }
  }

  emitCollaborationEvent(event, data) {
    eventBus.emit(EventTypes.ENGINE_ACTION, {
      engine: 'lighting',
      action: event,
      data: data
    });
  }

  generateFeedbackReport() {
    return {
      engine: this.name,
      timestamp: Date.now(),
      state: {
        current_theme: this.currentTheme,
        pattern: this.currentPattern,
        intensity: this.intensity,
        zones_count: this.zones.size
      },
      collaboration: this.collaborationState
    };
  }

  getStatus() {
    return {
      name: this.name,
      version: this.version,
      current_theme: this.currentTheme,
      pattern: this.currentPattern,
      intensity: this.intensity,
      zones: Object.fromEntries(this.zones),
      collaboration: this.collaborationState
    };
  }
}

const lightingEngine = new LightingEngine();

module.exports = {
  LightingEngine,
  lightingEngine,
  ColorThemes,
  Patterns,
  HintPriority
};

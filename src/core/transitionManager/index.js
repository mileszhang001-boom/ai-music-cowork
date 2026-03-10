'use strict';

const TransitionTypes = {
  GRADUAL: 'gradual',
  EVENT: 'event',
  EMERGENCY: 'emergency',
  IMMEDIATE: 'immediate'
};

const TransitionStyles = {
  FADE: 'fade',
  CROSSFADE: 'crossfade',
  SNAP: 'snap',
  PULSE: 'pulse',
  WAVE: 'wave'
};

const DefaultConfig = {
  defaultDurationSec: 5,
  emergencyDurationSec: 1,
  gradualDurationSec: 8,
  minDurationSec: 1,
  maxDurationSec: 30,
  debounceMs: 3000,
  emergencyBypassDebounce: true
};

class TransitionManager {
  constructor(config = {}) {
    this.config = { ...DefaultConfig, ...config };
    this.currentTransition = null;
    this.transitionHistory = [];
    this.lastTransitionTime = 0;
    this.pendingTransition = null;
  }

  planTransition(fromScene, toScene, trigger = {}) {
    const transitionType = this._determineTransitionType(fromScene, toScene, trigger);
    const duration = this._calculateDuration(transitionType, fromScene, toScene);
    const style = this._determineStyle(transitionType, fromScene, toScene);

    const transition = {
      transition_id: `trans_${Date.now()}`,
      type: transitionType,
      style,
      duration_sec: duration,
      from_scene: fromScene ? {
        scene_id: fromScene.scene_id,
        scene_type: fromScene.scene_type
      } : null,
      to_scene: {
        scene_id: toScene.scene_id,
        scene_type: toScene.scene_type
      },
      trigger: {
        source: trigger.source,
        reason: trigger.reason
      },
      phases: this._planPhases(transitionType, duration, style),
      created_at: new Date().toISOString()
    };

    const debounceResult = this._applyDebounce(transition);
    if (debounceResult.blocked) {
      return { ...transition, blocked: true, reason: debounceResult.reason };
    }

    this.currentTransition = transition;
    this.transitionHistory.push(transition);
    this.lastTransitionTime = Date.now();

    return transition;
  }

  _determineTransitionType(fromScene, toScene, trigger) {
    if (trigger.emergency || toScene.scene_type === 'fatigue_alert') {
      return TransitionTypes.EMERGENCY;
    }

    if (trigger.source === 'user_voice' || trigger.source === 'user_action') {
      return TransitionTypes.EVENT;
    }

    if (trigger.source === 'scene_change' || trigger.source === 'time_change') {
      return TransitionTypes.GRADUAL;
    }

    if (!fromScene) {
      return TransitionTypes.IMMEDIATE;
    }

    const energyDiff = Math.abs(
      (fromScene.intent?.energy_level || 0.5) - 
      (toScene.intent?.energy_level || 0.5)
    );

    if (energyDiff > 0.5) {
      return TransitionTypes.EVENT;
    }

    return TransitionTypes.GRADUAL;
  }

  _calculateDuration(transitionType, fromScene, toScene) {
    let duration;

    switch (transitionType) {
      case TransitionTypes.EMERGENCY:
        duration = this.config.emergencyDurationSec;
        break;
      case TransitionTypes.EVENT:
        duration = 3;
        break;
      case TransitionTypes.GRADUAL:
        duration = this.config.gradualDurationSec;
        break;
      case TransitionTypes.IMMEDIATE:
        duration = 0.5;
        break;
      default:
        duration = this.config.defaultDurationSec;
    }

    if (fromScene && toScene) {
      const energyDiff = Math.abs(
        (fromScene.intent?.energy_level || 0.5) - 
        (toScene.intent?.energy_level || 0.5)
      );
      duration += energyDiff * 2;
    }

    return Math.max(this.config.minDurationSec, 
           Math.min(this.config.maxDurationSec, duration));
  }

  _determineStyle(transitionType, fromScene, toScene) {
    switch (transitionType) {
      case TransitionTypes.EMERGENCY:
        return TransitionStyles.SNAP;
      case TransitionTypes.EVENT:
        return TransitionStyles.CROSSFADE;
      case TransitionTypes.GRADUAL:
        return TransitionStyles.FADE;
      default:
        return TransitionStyles.FADE;
    }
  }

  _planPhases(transitionType, duration, style) {
    const phases = [];

    switch (transitionType) {
      case TransitionTypes.EMERGENCY:
        phases.push({
          phase: 'immediate',
          start_offset_sec: 0,
          duration_sec: duration,
          actions: ['lighting_max', 'audio_alert', 'content_switch']
        });
        break;

      case TransitionTypes.EVENT:
        phases.push({
          phase: 'prepare',
          start_offset_sec: 0,
          duration_sec: duration * 0.2,
          actions: ['lighting_dim', 'audio_fade_start']
        });
        phases.push({
          phase: 'switch',
          start_offset_sec: duration * 0.2,
          duration_sec: duration * 0.6,
          actions: ['content_switch', 'lighting_change', 'audio_change']
        });
        phases.push({
          phase: 'settle',
          start_offset_sec: duration * 0.8,
          duration_sec: duration * 0.2,
          actions: ['lighting_settle', 'audio_settle']
        });
        break;

      case TransitionTypes.GRADUAL:
        phases.push({
          phase: 'anticipate',
          start_offset_sec: 0,
          duration_sec: duration * 0.3,
          actions: ['lighting_hint', 'audio_prepare']
        });
        phases.push({
          phase: 'transition',
          start_offset_sec: duration * 0.3,
          duration_sec: duration * 0.5,
          actions: ['content_crossfade', 'lighting_gradient', 'audio_blend']
        });
        phases.push({
          phase: 'stabilize',
          start_offset_sec: duration * 0.8,
          duration_sec: duration * 0.2,
          actions: ['all_engines_stabilize']
        });
        break;

      default:
        phases.push({
          phase: 'immediate',
          start_offset_sec: 0,
          duration_sec: duration,
          actions: ['all_engines_switch']
        });
    }

    return phases;
  }

  _applyDebounce(transition) {
    if (transition.type === TransitionTypes.EMERGENCY && 
        this.config.emergencyBypassDebounce) {
      return { blocked: false };
    }

    const timeSinceLastTransition = Date.now() - this.lastTransitionTime;

    if (timeSinceLastTransition < this.config.debounceMs) {
      return {
        blocked: true,
        reason: `距离上次过渡仅 ${timeSinceLastTransition}ms，小于防抖阈值 ${this.config.debounceMs}ms`
      };
    }

    return { blocked: false };
  }

  generateCommands(transition, descriptor) {
    const commands = {
      content: this._generateContentCommands(transition, descriptor),
      lighting: this._generateLightingCommands(transition, descriptor),
      audio: this._generateAudioCommands(transition, descriptor)
    };

    return {
      transition_id: transition.transition_id,
      commands,
      timeline: this._buildTimeline(transition)
    };
  }

  _generateContentCommands(transition, descriptor) {
    const commands = [];
    const hints = descriptor.hints?.music || {};

    for (const phase of transition.phases) {
      if (phase.actions.includes('content_switch') || 
          phase.actions.includes('content_crossfade')) {
        commands.push({
          action: 'transition_playlist',
          timing: phase.start_offset_sec * 1000,
          params: {
            crossfade_ms: transition.style === TransitionStyles.CROSSFADE ? 2000 : 500,
            new_genres: hints.suggested_tags || hints.genres || [],
            energy_target: descriptor.intent?.energy_level || 0.5
          }
        });
      }
    }

    return commands;
  }

  _generateLightingCommands(transition, descriptor) {
    const commands = [];
    const hints = descriptor.hints?.lighting || {};

    for (const phase of transition.phases) {
      if (phase.actions.includes('lighting_max')) {
        commands.push({
          action: 'set_immediate',
          timing: phase.start_offset_sec * 1000,
          params: { intensity: 1.0, pattern: 'alert' }
        });
      }

      if (phase.actions.includes('lighting_gradient') || 
          phase.actions.includes('lighting_change')) {
        commands.push({
          action: 'transition_to',
          timing: phase.start_offset_sec * 1000,
          params: {
            target_theme: hints.color_theme || 'neutral',
            target_intensity: hints.intensity || 0.5,
            target_pattern: hints.pattern || 'steady',
            duration_ms: phase.duration_sec * 1000
          }
        });
      }

      if (phase.actions.includes('lighting_dim')) {
        commands.push({
          action: 'dim',
          timing: phase.start_offset_sec * 1000,
          params: { factor: 0.7 }
        });
      }
    }

    return commands;
  }

  _generateAudioCommands(transition, descriptor) {
    const commands = [];
    const hints = descriptor.hints?.audio || {};

    for (const phase of transition.phases) {
      if (phase.actions.includes('audio_alert')) {
        commands.push({
          action: 'set_immediate',
          timing: phase.start_offset_sec * 1000,
          params: { preset: 'alert', volume_boost: 3 }
        });
      }

      if (phase.actions.includes('audio_change') || 
          phase.actions.includes('audio_blend')) {
        commands.push({
          action: 'transition_to',
          timing: phase.start_offset_sec * 1000,
          params: {
            target_preset: hints.preset || 'standard',
            duration_ms: phase.duration_sec * 1000
          }
        });
      }

      if (phase.actions.includes('audio_fade_start')) {
        commands.push({
          action: 'fade',
          timing: phase.start_offset_sec * 1000,
          params: { direction: 'out', duration_ms: phase.duration_sec * 500 }
        });
      }
    }

    return commands;
  }

  _buildTimeline(transition) {
    return {
      total_duration_ms: transition.duration_sec * 1000,
      phases: transition.phases.map(p => ({
        name: p.phase,
        start_ms: p.start_offset_sec * 1000,
        duration_ms: p.duration_sec * 1000
      }))
    };
  }

  getCurrentTransition() {
    return this.currentTransition;
  }

  getHistory(limit = 10) {
    return this.transitionHistory.slice(-limit);
  }

  clear() {
    this.currentTransition = null;
    this.transitionHistory = [];
    this.lastTransitionTime = 0;
  }

  getStats() {
    const stats = {
      total_transitions: this.transitionHistory.length,
      by_type: {},
      average_duration: 0
    };

    if (this.transitionHistory.length === 0) return stats;

    let totalDuration = 0;
    for (const t of this.transitionHistory) {
      stats.by_type[t.type] = (stats.by_type[t.type] || 0) + 1;
      totalDuration += t.duration_sec;
    }

    stats.average_duration = totalDuration / this.transitionHistory.length;

    return stats;
  }
}

const transitionManager = new TransitionManager();

module.exports = {
  TransitionManager,
  transitionManager,
  TransitionTypes,
  TransitionStyles
};

'use strict';

const { SceneDimensions, SceneTypes } = require('../types');

class SceneRecognizer {
  constructor(config = {}) {
    this.config = config;
    this.currentScene = null;
    this.sceneHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
    this.changeThreshold = config.changeThreshold || 0.3;
  }

  recognize(signals) {
    const dimensions = this.calculateDimensions(signals);
    const sceneType = this.inferSceneType(dimensions, signals);
    const confidence = this.calculateConfidence(signals);

    return {
      scene_type: sceneType,
      dimensions,
      confidence,
      timestamp: Date.now()
    };
  }

  calculateDimensions(signals) {
    return {
      [SceneDimensions.SOCIAL]: this.calculateSocialDimension(signals),
      [SceneDimensions.ENERGY]: this.calculateEnergyDimension(signals),
      [SceneDimensions.FOCUS]: this.calculateFocusDimension(signals),
      [SceneDimensions.TIME_CONTEXT]: this.calculateTimeDimension(signals),
      [SceneDimensions.WEATHER]: this.calculateWeatherDimension(signals)
    };
  }

  calculateSocialDimension(signals) {
    if (signals.vehicle?.passenger_count) {
      return Math.min(1, signals.vehicle.passenger_count / 4);
    }
    return 0;
  }

  calculateEnergyDimension(signals) {
    let energy = 0.5;
    let weight = 0;

    if (signals.vehicle?.speed_kmh) {
      energy += (signals.vehicle.speed_kmh / 200) * 0.3;
      weight += 0.3;
    }

    if (signals.biometric?.heart_rate) {
      const hr = signals.biometric.heart_rate;
      energy += ((hr - 60) / 100) * 0.3;
      weight += 0.3;
    }

    return weight > 0 ? Math.min(1, Math.max(0, energy / (weight + 0.5))) : 0.5;
  }

  calculateFocusDimension(signals) {
    let focus = 0.5;

    if (signals.user_query?.intent === 'navigation' || signals.user_query?.intent === 'control') {
      focus += 0.3;
    }

    if (signals.biometric?.stress_level) {
      focus += signals.biometric.stress_level * 0.3;
    }

    return Math.min(1, Math.max(0, focus));
  }

  calculateTimeDimension(signals) {
    if (signals.environment?.time_of_day !== undefined) {
      return signals.environment.time_of_day;
    }

    const hour = new Date().getHours();
    if (hour >= 6 && hour < 18) {
      return (hour - 6) / 12;
    }
    return 0;
  }

  calculateWeatherDimension(signals) {
    if (signals.environment?.weather) {
      const adverseConditions = ['rain', 'snow', 'fog', 'storm'];
      return adverseConditions.includes(signals.environment.weather) ? 1 : 0;
    }
    return 0;
  }

  inferSceneType(dimensions, signals) {
    const { social, energy, focus, time_context, weather } = dimensions;

    if (signals.biometric?.fatigue_level && signals.biometric.fatigue_level > 0.7) {
      return SceneTypes.FATIGUE_ALERT;
    }

    if (weather > 0.5 && time_context < 0.3) {
      return SceneTypes.RAINY_NIGHT;
    }

    if (social > 0.5 && energy > 0.7) {
      return SceneTypes.PARTY;
    }

    if (social > 0.5) {
      return SceneTypes.FAMILY_OUTING;
    }

    if (focus > 0.6 && energy < 0.4) {
      return SceneTypes.FOCUS_WORK;
    }

    if (time_context > 0.2 && time_context < 0.5 && energy < 0.5) {
      return SceneTypes.MORNING_COMMUTE;
    }

    if (time_context < 0.2 && energy < 0.4) {
      return SceneTypes.NIGHT_DRIVE;
    }

    if (energy > 0.6 && social > 0.3) {
      return SceneTypes.ROAD_TRIP;
    }

    return SceneTypes.MORNING_COMMUTE;
  }

  calculateConfidence(signals) {
    const sourceCount = [
      signals.vehicle?.speed_kmh !== undefined,
      signals.environment?.time_of_day !== undefined,
      signals.biometric?.heart_rate !== undefined,
      signals.user_query !== null
    ].filter(Boolean).length;

    return Math.min(1, sourceCount / 4 * 0.7 + 0.3);
  }

  detectChange(newScene) {
    if (!this.currentScene) {
      return {
        scene_changed: true,
        previous_scene: null,
        change_type: 'initial',
        change_magnitude: 1.0
      };
    }

    const magnitude = this.calculateChangeMagnitude(this.currentScene, newScene);
    const changed = magnitude > this.changeThreshold;

    return {
      scene_changed: changed,
      previous_scene: changed ? this.currentScene.scene_type : null,
      change_type: changed ? this.getChangeType(this.currentScene, newScene) : null,
      change_magnitude: magnitude
    };
  }

  calculateChangeMagnitude(oldScene, newScene) {
    if (oldScene.scene_type !== newScene.scene_type) {
      return 1.0;
    }

    const dims = Object.keys(SceneDimensions);
    let totalDiff = 0;
    
    for (const dim of dims) {
      const oldVal = oldScene.dimensions[dim] || 0;
      const newVal = newScene.dimensions[dim] || 0;
      totalDiff += Math.abs(oldVal - newVal);
    }

    return totalDiff / dims.length;
  }

  getChangeType(oldScene, newScene) {
    if (oldScene.scene_type !== newScene.scene_type) {
      return 'scene_transition';
    }
    return 'dimension_shift';
  }

  clear() {
    this.currentScene = null;
    this.sceneHistory = [];
  }
}

const sceneRecognizer = new SceneRecognizer();

module.exports = {
  SceneRecognizer,
  sceneRecognizer
};

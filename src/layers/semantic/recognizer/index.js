'use strict';

const { SceneDimensions, SceneTypes } = require('../types');

const MoodEnergyMap = {
  happy: 0.7,
  excited: 0.9,
  calm: 0.3,
  neutral: 0.5,
  tired: 0.2,
  stressed: 0.6
};

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
    const passengers = signals.internal_camera?.passengers;
    if (passengers) {
      const total = (passengers.children || 0) + (passengers.adults || 0) + (passengers.seniors || 0);
      return Math.min(1, total / 4);
    }
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

    if (signals.internal_camera?.mood) {
      const moodEnergy = MoodEnergyMap[signals.internal_camera.mood] || 0.5;
      energy += moodEnergy * 0.4;
      weight += 0.4;
    }

    if (signals.internal_mic?.volume_level !== undefined) {
      energy += signals.internal_mic.volume_level * 0.2;
      weight += 0.2;
    }

    return weight > 0 ? Math.min(1, Math.max(0, energy / (weight + 0.5))) : 0.5;
  }

  calculateFocusDimension(signals) {
    let focus = 0.5;

    if (signals.user_query?.intent === 'navigation' || signals.user_query?.intent === 'control') {
      focus += 0.3;
    }

    if (signals.internal_camera?.mood === 'stressed') {
      focus += 0.2;
    }

    if (signals.internal_camera?.mood === 'calm') {
      focus -= 0.1;
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

    if (signals.internal_camera?.mood === 'tired') {
      return SceneTypes.FATIGUE_ALERT;
    }

    if (weather > 0.5 && time_context < 0.3) {
      return SceneTypes.RAINY_NIGHT;
    }

    const passengers = signals.internal_camera?.passengers;
    const hasChildren = passengers && passengers.children > 0;

    if (hasChildren) {
      return SceneTypes.FAMILY_OUTING;
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
      signals.internal_camera?.mood !== undefined,
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

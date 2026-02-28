'use strict';

/**
 * @fileoverview Layer 2 - 场景识别层
 * @description 负责场景向量计算与变化检测
 */

const { eventBus, EventTypes } = require('../eventBus');

const SceneDimensions = {
  SOCIAL: 'social',
  ENERGY: 'energy',
  FOCUS: 'focus',
  TIME_CONTEXT: 'time_context',
  WEATHER: 'weather'
};

const SceneTypes = {
  MORNING_COMMUTE: 'morning_commute',
  NIGHT_DRIVE: 'night_drive',
  ROAD_TRIP: 'road_trip',
  ROMANTIC_DATE: 'romantic_date',
  FAMILY_OUTING: 'family_outing',
  FOCUS_WORK: 'focus_work',
  TRAFFIC_JAM: 'traffic_jam',
  FATIGUE_ALERT: 'fatigue_alert',
  RAINY_NIGHT: 'rainy_night',
  PARTY: 'party'
};

class Layer2Processor {
  constructor(config = {}) {
    this.config = config;
    this.currentScene = null;
    this.sceneHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
    this.changeThreshold = config.changeThreshold || 0.3;
  }

  /**
   * 处理 Layer 1 输出，生成场景向量
   * @param {Object} layer1Output - Layer 1 输出
   * @returns {Object} Layer 2 输出
   */
  process(layer1Output) {
    const sceneVector = this.calculateSceneVector(layer1Output.signals);
    const changeDetection = this.detectChange(sceneVector);

    const output = {
      output_id: `layer2_${Date.now()}`,
      timestamp: Date.now(),
      scene_vector: sceneVector,
      change_detection: changeDetection
    };

    if (changeDetection.scene_changed) {
      this.currentScene = sceneVector;
      this.sceneHistory.push({
        scene: sceneVector,
        timestamp: output.timestamp
      });
      
      if (this.sceneHistory.length > this.maxHistorySize) {
        this.sceneHistory.shift();
      }

      eventBus.emit(EventTypes.SCENE_CHANGED, {
        current: sceneVector,
        previous: changeDetection.previous_scene
      });
    }

    eventBus.emit(EventTypes.SCENE_DETECTED, output);

    return output;
  }

  /**
   * 计算场景向量
   * @param {Array} signals - 标准化信号列表
   * @returns {Object} 场景向量
   */
  calculateSceneVector(signals) {
    const dimensions = {
      [SceneDimensions.SOCIAL]: this.calculateSocialDimension(signals),
      [SceneDimensions.ENERGY]: this.calculateEnergyDimension(signals),
      [SceneDimensions.FOCUS]: this.calculateFocusDimension(signals),
      [SceneDimensions.TIME_CONTEXT]: this.calculateTimeDimension(signals),
      [SceneDimensions.WEATHER]: this.calculateWeatherDimension(signals)
    };

    const sceneType = this.inferSceneType(dimensions, signals);
    const confidence = this.calculateConfidence(signals);

    return {
      scene_type: sceneType,
      dimensions: dimensions,
      confidence: confidence,
      timestamp: Date.now()
    };
  }

  /**
   * 计算社交维度
   * @param {Array} signals - 信号列表
   * @returns {number} 社交维度值 (0=solo, 1=group)
   */
  calculateSocialDimension(signals) {
    const passengerSignal = signals.find(s => 
      s.source === 'vhal' && s.value?.passenger_count !== undefined
    );
    
    if (passengerSignal) {
      const count = passengerSignal.value.passenger_count;
      return Math.min(1, count / 4);
    }

    const voiceSignal = signals.find(s => s.source === 'voice');
    if (voiceSignal?.value?.detected_speakers > 1) {
      return Math.min(1, voiceSignal.value.detected_speakers / 4);
    }

    return 0;
  }

  /**
   * 计算能量维度
   * @param {Array} signals - 信号列表
   * @returns {number} 能量维度值 (0=calm, 1=excited)
   */
  calculateEnergyDimension(signals) {
    let energy = 0.5;
    let weight = 0;

    const speedSignal = signals.find(s => 
      s.source === 'vhal' && s.value?.vehicle_speed !== undefined
    );
    if (speedSignal) {
      energy += speedSignal.value.vehicle_speed * 0.3;
      weight += 0.3;
    }

    const musicSignal = signals.find(s => s.source === 'music_state');
    if (musicSignal?.value?.energy) {
      energy += musicSignal.value.energy * 0.4;
      weight += 0.4;
    }

    const biometricSignal = signals.find(s => s.source === 'biometric');
    if (biometricSignal?.value?.heart_rate) {
      const hr = biometricSignal.value.heart_rate;
      energy += ((hr - 60) / 100) * 0.3;
      weight += 0.3;
    }

    return weight > 0 ? Math.min(1, Math.max(0, energy / (weight + 0.5))) : 0.5;
  }

  /**
   * 计算专注维度
   * @param {Array} signals - 信号列表
   * @returns {number} 专注维度值 (0=relaxed, 1=focused)
   */
  calculateFocusDimension(signals) {
    let focus = 0.5;

    const voiceSignal = signals.find(s => s.source === 'voice');
    if (voiceSignal?.value?.intent === 'navigation' || voiceSignal?.value?.intent === 'control') {
      focus += 0.3;
    }

    const biometricSignal = signals.find(s => s.source === 'biometric');
    if (biometricSignal?.value?.stress_level) {
      focus += biometricSignal.value.stress_level * 0.3;
    }

    return Math.min(1, Math.max(0, focus));
  }

  /**
   * 计算时间维度
   * @param {Array} signals - 信号列表
   * @returns {number} 时间维度值 (0=night, 1=day)
   */
  calculateTimeDimension(signals) {
    const envSignal = signals.find(s => 
      s.source === 'environment' && s.value?.time_of_day
    );
    
    if (envSignal) {
      return envSignal.value.time_of_day;
    }

    const hour = new Date().getHours();
    if (hour >= 6 && hour < 18) {
      return (hour - 6) / 12;
    }
    return 0;
  }

  /**
   * 计算天气维度
   * @param {Array} signals - 信号列表
   * @returns {number} 天气维度值 (0=clear, 1=adverse)
   */
  calculateWeatherDimension(signals) {
    const envSignal = signals.find(s => 
      s.source === 'environment' && s.value?.weather
    );
    
    if (envSignal) {
      const weather = envSignal.value.weather;
      const adverseConditions = ['rain', 'snow', 'fog', 'storm'];
      return adverseConditions.includes(weather) ? 1 : 0;
    }

    return 0;
  }

  /**
   * 推断场景类型
   * @param {Object} dimensions - 场景维度
   * @param {Array} signals - 信号列表
   * @returns {string} 场景类型
   */
  inferSceneType(dimensions, signals) {
    const { social, energy, focus, time_context, weather } = dimensions;

    const biometricSignal = signals.find(s => s.source === 'biometric');
    const fatigueValue = biometricSignal?.value?.fatigue_level ?? biometricSignal?.value;
    if (typeof fatigueValue === 'number' && fatigueValue > 0.7) {
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

  /**
   * 计算场景置信度
   * @param {Array} signals - 信号列表
   * @returns {number} 置信度
   */
  calculateConfidence(signals) {
    if (signals.length === 0) return 0;
    
    const avgConfidence = signals.reduce((sum, s) => sum + s.confidence, 0) / signals.length;
    const sourceDiversity = new Set(signals.map(s => s.source)).size / 6;
    
    return avgConfidence * 0.7 + sourceDiversity * 0.3;
  }

  /**
   * 检测场景变化
   * @param {Object} newScene - 新场景向量
   * @returns {Object} 变化检测结果
   */
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

  /**
   * 计算变化幅度
   * @param {Object} oldScene - 旧场景
   * @param {Object} newScene - 新场景
   * @returns {number} 变化幅度
   */
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

  /**
   * 获取变化类型
   * @param {Object} oldScene - 旧场景
   * @param {Object} newScene - 新场景
   * @returns {string} 变化类型
   */
  getChangeType(oldScene, newScene) {
    if (oldScene.scene_type !== newScene.scene_type) {
      return 'scene_transition';
    }
    return 'dimension_shift';
  }

  /**
   * 获取当前场景
   * @returns {Object|null} 当前场景
   */
  getCurrentScene() {
    return this.currentScene;
  }

  /**
   * 清空历史
   */
  clear() {
    this.currentScene = null;
    this.sceneHistory = [];
  }
}

const layer2 = new Layer2Processor();

module.exports = {
  Layer2Processor,
  layer2,
  SceneDimensions,
  SceneTypes
};

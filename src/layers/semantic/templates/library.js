'use strict';

const fs = require('fs');
const path = require('path');

class TemplateLibrary {
  constructor(config = {}) {
    this.config = config;
    this.templates = new Map();
    this.templateLearner = null;
    this.loadPresetTemplates();
  }

  loadPresetTemplates() {
    const presetTemplates = [
      {
        template_id: 'TPL_001',
        scene_type: 'morning_commute',
        name: '早晨通勤',
        description: '工作日早晨，独自开车上班',
        category: 'time',
        priority: 1,
        intent: {
          mood: { valence: 0.6, arousal: 0.4 },
          energy_level: 0.4,
          atmosphere: 'fresh_morning'
        },
        hints: {
          music: { genres: ['pop', 'indie'], tempo: 'moderate', vocal_style: 'any' },
          lighting: { color_theme: 'warm', pattern: 'steady', intensity: 0.4 },
          audio: { preset: 'standard' }
        },
        announcement_templates: ['早安，为您准备了清新的晨间音乐'],
        triggers: { time_range: [6, 9], min_passengers: 0, max_passengers: 0 }
      },
      {
        template_id: 'TPL_002',
        scene_type: 'night_drive',
        name: '深夜驾驶',
        description: '深夜独自开车回家，安静放松',
        category: 'time',
        priority: 1,
        intent: {
          mood: { valence: 0.5, arousal: 0.2 },
          energy_level: 0.2,
          atmosphere: 'serene_night'
        },
        hints: {
          music: { genres: ['jazz', 'lo-fi'], tempo: 'slow', vocal_style: 'instrumental' },
          lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.2 },
          audio: { preset: 'night_mode' }
        },
        announcement_templates: ['深夜路况良好，为您切换至静谧夜行模式'],
        triggers: { time_range: [22, 6], min_passengers: 0, max_passengers: 0 }
      },
      {
        template_id: 'TPL_003',
        scene_type: 'road_trip',
        name: '朋友出游',
        description: '周末和朋友一起开车出游',
        category: 'social',
        priority: 2,
        intent: {
          mood: { valence: 0.8, arousal: 0.7 },
          energy_level: 0.7,
          atmosphere: 'energetic_road_trip'
        },
        hints: {
          music: { genres: ['rock', 'pop'], tempo: 'upbeat', vocal_style: 'any' },
          lighting: { color_theme: 'vibrant', pattern: 'pulse', intensity: 0.6 },
          audio: { preset: 'outdoor' }
        },
        announcement_templates: ['旅途愉快，让音乐伴你们同行'],
        triggers: { min_passengers: 1 }
      },
      {
        template_id: 'TPL_004',
        scene_type: 'fatigue_alert',
        name: '疲劳提醒',
        description: '检测到驾驶员疲劳，紧急唤醒',
        category: 'safety',
        priority: 0,
        intent: {
          mood: { valence: 0.4, arousal: 0.9 },
          energy_level: 0.9,
          atmosphere: 'alert_wake_up'
        },
        hints: {
          music: { genres: ['electronic', 'rock'], tempo: 'fast', vocal_style: 'energetic' },
          lighting: { color_theme: 'alert', pattern: 'flash', intensity: 0.8 },
          audio: { preset: 'bass_boost' }
        },
        announcement_templates: ['检测到您有点疲劳，为您切换到提神模式，建议找个服务区休息'],
        triggers: { fatigue_threshold: 0.7 }
      },
      {
        template_id: 'TPL_005',
        scene_type: 'rainy_night',
        name: '雨夜行车',
        description: '下雨的夜晚，安静驾驶',
        category: 'weather',
        priority: 1,
        intent: {
          mood: { valence: 0.4, arousal: 0.2 },
          energy_level: 0.2,
          atmosphere: 'cozy_rain'
        },
        hints: {
          music: { genres: ['jazz', 'ambient'], tempo: 'slow', vocal_style: 'soft' },
          lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.25 },
          audio: { preset: 'rain_mode' }
        },
        announcement_templates: ['雨夜行车，为您准备了舒缓的音乐'],
        triggers: { weather: ['rain'], time_range: [18, 6] }
      }
    ];

    for (const template of presetTemplates) {
      this.templates.set(template.template_id, template);
    }
  }

  matchTemplate(sceneVector, context = {}) {
    if (!sceneVector) return null;

    let bestMatch = null;
    let bestScore = 0;

    for (const [id, template] of this.templates) {
      const score = this.calculateMatchScore(template, sceneVector, context);
      if (score > bestScore) {
        bestScore = score;
        bestMatch = template;
      }
    }

    return bestMatch;
  }

  calculateMatchScore(template, sceneVector, context) {
    let score = 0;

    if (template.scene_type === sceneVector.scene_type) {
      score += 0.5;
    }

    if (template.triggers) {
      if (template.triggers.min_passengers !== undefined) {
        const passengers = context.passengerCount || 0;
        if (passengers >= template.triggers.min_passengers) {
          score += 0.2;
        }
      }

      if (template.triggers.weather && context.weather) {
        if (template.triggers.weather.includes(context.weather)) {
          score += 0.2;
        }
      }
    }

    score += (1 - template.priority / 10) * 0.1;

    return score;
  }

  getTemplate(templateId) {
    return this.templates.get(templateId) || null;
  }

  getAllTemplates() {
    return Array.from(this.templates.values());
  }

  getStats() {
    return {
      total: this.templates.size,
      bySource: {
        preset: this.templates.size,
        learned: 0,
        custom: 0
      }
    };
  }
}

const templateLibrary = new TemplateLibrary();

module.exports = {
  TemplateLibrary,
  templateLibrary
};

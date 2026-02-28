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
    const presetPath = path.join(__dirname, '../../../../templates/preset_templates.json');
    
    try {
      const data = fs.readFileSync(presetPath, 'utf8');
      const presetData = JSON.parse(data);
      
      if (presetData.templates && Array.isArray(presetData.templates)) {
        for (const template of presetData.templates) {
          this.templates.set(template.template_id, template);
        }
      }
    } catch (error) {
      console.warn('[TemplateLibrary] Failed to load preset templates:', error.message);
      this.loadDefaultTemplates();
    }
  }

  loadDefaultTemplates() {
    const defaultTemplates = [
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
          music: { genres: ['pop', 'indie'], tempo: 'moderate' },
          lighting: { color_theme: 'warm', pattern: 'steady', intensity: 0.4 },
          audio: { preset: 'standard' }
        },
        announcement_templates: ['早安，为您准备了清新的晨间音乐'],
        triggers: { time_range: [6, 9] }
      }
    ];

    for (const template of defaultTemplates) {
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

      if (template.triggers.has_children !== undefined) {
        const hasChildren = context.hasChildren || context.passengerComposition?.includes('child') || false;
        if (hasChildren === template.triggers.has_children) {
          score += 0.3;
        }
      }

      if (template.triggers.scene_description && context.sceneDescription) {
        if (template.triggers.scene_description === context.sceneDescription) {
          score += 0.25;
        }
      }

      if (template.triggers.mood && context.mood) {
        if (template.triggers.mood === context.mood) {
          score += 0.3;
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
    const byCategory = {};
    for (const template of this.templates.values()) {
      const cat = template.category || 'other';
      byCategory[cat] = (byCategory[cat] || 0) + 1;
    }

    return {
      total: this.templates.size,
      byCategory,
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

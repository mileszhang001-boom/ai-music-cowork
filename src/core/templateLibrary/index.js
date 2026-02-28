'use strict';

/**
 * @fileoverview Template Library - 场景模板库
 * @description 本地快通道模板库，支持快速场景匹配和多源模板加载
 */

const fs = require('fs');
const path = require('path');
const { TemplateLearner } = require('./templateLearner');

class TemplateLibrary {
  constructor(config = {}) {
    this.config = config;
    this.templates = new Map();
    this.templatesBySceneType = new Map();
    this.templatesBySource = new Map(['preset', 'learned', 'custom'].map(s => [s, new Map()]));
    
    this.templatesDir = config.templatesDir || path.join(process.cwd(), 'templates');
    
    this.presetTemplatesPath = config.presetTemplatesPath || 
      path.join(this.templatesDir, 'preset_templates.json');
    this.learnedTemplatesPath = config.learnedTemplatesPath || 
      path.join(this.templatesDir, 'learned_templates.json');
    this.customTemplatesPath = config.customTemplatesPath || 
      path.join(this.templatesDir, 'custom_templates.json');
    
    this.templateLearner = new TemplateLearner({
      learnedTemplatesPath: this.learnedTemplatesPath,
      ...config.learnerConfig
    });
    
    this.loadAllTemplates();
  }

  /**
   * 加载所有模板源
   */
  loadAllTemplates() {
    this.templates.clear();
    this.templatesBySceneType.clear();
    this.templatesBySource.forEach(map => map.clear());

    this.loadTemplatesFromPath(this.presetTemplatesPath, 'preset');
    this.loadTemplatesFromPath(this.learnedTemplatesPath, 'learned');
    this.loadTemplatesFromPath(this.customTemplatesPath, 'custom');

    this.syncLearnedTemplates();
    
    console.log(`[TemplateLibrary] Loaded ${this.templates.size} templates from all sources`);
  }

  /**
   * 从指定路径加载模板
   * @param {string} filePath - 文件路径
   * @param {string} source - 模板来源
   */
  loadTemplatesFromPath(filePath, source) {
    try {
      if (fs.existsSync(filePath)) {
        const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
        const templates = data.templates || (Array.isArray(data) ? data : []);
        
        for (const template of templates) {
          template.source = source;
          this.addTemplate(template);
        }
        
        console.log(`[TemplateLibrary] Loaded ${templates.length} templates from ${source}`);
      }
    } catch (error) {
      console.warn(`Failed to load templates from ${filePath}:`, error.message);
    }
  }

  /**
   * 同步学习的模板
   */
  syncLearnedTemplates() {
    const learnedTemplates = this.templateLearner.getLearnedTemplates();
    for (const template of learnedTemplates) {
      template.source = 'learned';
      if (!this.templates.has(template.template_id)) {
        this.addTemplate(template);
      }
    }
  }

  /**
   * 导入模板数据
   * @param {Array} templatesData - 模板数据数组
   * @param {string} source - 模板来源
   */
  importTemplates(templatesData, source = 'preset') {
    for (const template of templatesData) {
      template.source = source;
      this.addTemplate(template);
    }
  }

  /**
   * 添加模板
   * @param {Object} template - 模板对象
   */
  addTemplate(template) {
    this.templates.set(template.template_id, template);

    if (!this.templatesBySceneType.has(template.scene_type)) {
      this.templatesBySceneType.set(template.scene_type, []);
    }
    this.templatesBySceneType.get(template.scene_type).push(template);

    const source = template.source || 'preset';
    if (this.templatesBySource.has(source)) {
      this.templatesBySource.get(source).set(template.template_id, template);
    }
  }

  /**
   * 初始化默认模板
   */
  initializeDefaultTemplates() {
    const defaultTemplate = {
      template_id: 'TPL_DEFAULT',
      scene_type: 'default',
      name: '默认场景',
      description: '默认基础场景',
      category: 'default',
      intent: {
        mood: { valence: 0.5, arousal: 0.4 },
        energy_level: 0.4,
        atmosphere: 'neutral'
      },
      hints: {
        music: { genres: ['pop'], tempo: 'medium' },
        lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.4 },
        audio: { preset: 'standard' }
      },
      triggers: {},
      priority: 10,
      source: 'preset'
    };

    this.addTemplate(defaultTemplate);
  }

  /**
   * 根据场景向量匹配模板
   * @param {Object} sceneVector - 场景向量
   * @param {Object} context - 上下文信息
   * @returns {Object|null} 匹配的模板
   */
  matchTemplate(sceneVector, context = {}) {
    const candidates = this.findCandidates(sceneVector, context);
    
    if (candidates.length === 0) {
      return this.getDefaultTemplate();
    }

    candidates.sort((a, b) => {
      if (a.priority !== b.priority) {
        return a.priority - b.priority;
      }
      return (b.score || 0) - (a.score || 0);
    });

    return candidates[0];
  }

  /**
   * 查找候选模板
   * @param {Object} sceneVector - 场景向量
   * @param {Object} context - 上下文
   * @returns {Array} 候选模板列表
   */
  findCandidates(sceneVector, context) {
    const candidates = [];
    const now = new Date();
    const currentHour = now.getHours();
    const currentMinute = now.getMinutes();
    const currentTime = currentHour * 60 + currentMinute;
    const dayOfWeek = now.getDay();

    for (const [id, template] of this.templates) {
      const score = this.calculateMatchScore(template, sceneVector, context, {
        currentTime,
        dayOfWeek,
        currentHour
      });

      if (score > 0) {
        candidates.push({ ...template, score });
      }
    }

    return candidates;
  }

  /**
   * 计算匹配分数
   * @param {Object} template - 模板
   * @param {Object} sceneVector - 场景向量
   * @param {Object} context - 上下文
   * @param {Object} timeInfo - 时间信息
   * @returns {number} 匹配分数
   */
  calculateMatchScore(template, sceneVector, context, timeInfo) {
    let score = 0;
    const triggers = template.triggers || {};

    if (triggers.fatigue_level?.min !== undefined) {
      if (context.fatigueLevel === undefined || context.fatigueLevel < triggers.fatigue_level.min) {
        return 0;
      }
      score += 50;
    }

    if (triggers.time_range) {
      const [start, end] = triggers.time_range;
      const startMinutes = this.parseTime(start);
      const endMinutes = this.parseTime(end);

      let inRange = false;
      if (startMinutes <= endMinutes) {
        inRange = timeInfo.currentTime >= startMinutes && timeInfo.currentTime <= endMinutes;
      } else {
        inRange = timeInfo.currentTime >= startMinutes || timeInfo.currentTime <= endMinutes;
      }

      if (!inRange) return 0;
      score += 30;
    }

    if (triggers.weekdays && !triggers.weekdays.includes(timeInfo.dayOfWeek)) {
      return 0;
    }

    if (triggers.weather && context.weather) {
      if (!triggers.weather.includes(context.weather)) return 0;
      score += 20;
    }

    if (triggers.min_speed !== undefined && context.speed < triggers.min_speed) {
      return 0;
    }

    if (triggers.max_speed !== undefined && context.speed > triggers.max_speed) {
      return 0;
    }

    if (triggers.vehicle_speed !== undefined) {
      if (typeof triggers.vehicle_speed === 'object') {
        if (triggers.vehicle_speed.max !== undefined && context.speed > triggers.vehicle_speed.max) {
          return 0;
        }
        if (triggers.vehicle_speed.min !== undefined && context.speed < triggers.vehicle_speed.min) {
          return 0;
        }
      } else {
        if (context.speed !== triggers.vehicle_speed) return 0;
      }
    }

    if (triggers.min_passengers !== undefined) {
      const passengers = context.passengerCount || 0;
      if (passengers < triggers.min_passengers) return 0;
      score += 15;
    }

    if (triggers.max_passengers !== undefined) {
      const passengers = context.passengerCount || 0;
      if (passengers > triggers.max_passengers) return 0;
    }

    if (triggers.has_children && !context.hasChildren) {
      return 0;
    }

    if (triggers.manual_activation && !context.manualActivation) {
      return 0;
    }

    if (template.scene_type === sceneVector.scene_type) {
      score += 40;
    }

    const dimSimilarity = this.calculateDimensionSimilarity(template, sceneVector);
    score += dimSimilarity * 20;

    return score;
  }

  /**
   * 解析时间字符串
   * @param {string} timeStr - 时间字符串 (HH:MM)
   * @returns {number} 分钟数
   */
  parseTime(timeStr) {
    const [hours, minutes] = timeStr.split(':').map(Number);
    return hours * 60 + (minutes || 0);
  }

  /**
   * 计算维度相似度
   * @param {Object} template - 模板
   * @param {Object} sceneVector - 场景向量
   * @returns {number} 相似度 (0-1)
   */
  calculateDimensionSimilarity(template, sceneVector) {
    const templateIntent = template.intent || {};
    const dims = sceneVector.dimensions || {};

    let similarity = 0;
    let count = 0;

    if (templateIntent.energy_level !== undefined && dims.energy !== undefined) {
      similarity += 1 - Math.abs(templateIntent.energy_level - dims.energy);
      count++;
    }

    if (templateIntent.mood?.arousal !== undefined && dims.energy !== undefined) {
      similarity += 1 - Math.abs(templateIntent.mood.arousal - dims.energy);
      count++;
    }

    if (templateIntent.mood?.valence !== undefined && dims.social !== undefined) {
      similarity += 1 - Math.abs(templateIntent.mood.valence - dims.social) * 0.5;
      count++;
    }

    return count > 0 ? similarity / count : 0;
  }

  /**
   * 获取默认模板
   * @returns {Object} 默认模板
   */
  getDefaultTemplate() {
    return this.templates.get('TPL_DEFAULT') || this.templates.get('TPL_020') || null;
  }

  /**
   * 根据ID获取模板
   * @param {string} templateId - 模板ID
   * @returns {Object|null} 模板
   */
  getTemplate(templateId) {
    return this.templates.get(templateId) || null;
  }

  /**
   * 根据场景类型获取模板
   * @param {string} sceneType - 场景类型
   * @returns {Array} 模板列表
   */
  getTemplatesBySceneType(sceneType) {
    return this.templatesBySceneType.get(sceneType) || [];
  }

  /**
   * 根据来源获取模板
   * @param {string} source - 模板来源 (preset/learned/custom)
   * @returns {Array} 模板列表
   */
  getTemplatesBySource(source) {
    if (this.templatesBySource.has(source)) {
      return Array.from(this.templatesBySource.get(source).values());
    }
    return [];
  }

  /**
   * 获取所有模板
   * @returns {Array} 模板列表
   */
  getAllTemplates() {
    return Array.from(this.templates.values());
  }

  /**
   * 获取模板数量
   * @returns {number} 模板数量
   */
  size() {
    return this.templates.size;
  }

  /**
   * 记录场景执行（用于模板学习）
   * @param {string} executionId - 执行ID
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @param {Object} context - 上下文
   */
  recordExecution(executionId, sceneDescriptor, context) {
    this.templateLearner.recordExecution(executionId, sceneDescriptor, context);
  }

  /**
   * 记录用户反馈
   * @param {string} executionId - 执行ID
   * @param {string} action - 用户动作
   * @param {Object} data - 动作数据
   */
  recordFeedback(executionId, action, data) {
    this.templateLearner.recordFeedback(executionId, action, data);
  }

  /**
   * 添加自定义模板
   * @param {Object} template - 模板对象
   * @returns {boolean} 是否成功
   */
  addCustomTemplate(template) {
    try {
      template.source = 'custom';
      template.template_id = template.template_id || `CUSTOM_${Date.now()}`;
      
      this.addTemplate(template);
      this.saveCustomTemplates();
      
      return true;
    } catch (error) {
      console.error('Failed to add custom template:', error.message);
      return false;
    }
  }

  /**
   * 保存自定义模板
   */
  saveCustomTemplates() {
    try {
      const customTemplates = this.getTemplatesBySource('custom');
      
      const dir = path.dirname(this.customTemplatesPath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      
      fs.writeFileSync(this.customTemplatesPath, JSON.stringify({
        version: '1.0',
        description: '用户手动创建的自定义模板',
        lastUpdated: new Date().toISOString(),
        templates: customTemplates
      }, null, 2), 'utf8');
      
      return true;
    } catch (error) {
      console.error('Failed to save custom templates:', error.message);
      return false;
    }
  }

  /**
   * 删除模板
   * @param {string} templateId - 模板ID
   * @returns {boolean} 是否成功
   */
  deleteTemplate(templateId) {
    const template = this.templates.get(templateId);
    if (!template) return false;

    if (template.source === 'preset') {
      console.warn('Cannot delete preset template');
      return false;
    }

    this.templates.delete(templateId);

    const sceneTypeTemplates = this.templatesBySceneType.get(template.scene_type);
    if (sceneTypeTemplates) {
      const index = sceneTypeTemplates.findIndex(t => t.template_id === templateId);
      if (index >= 0) {
        sceneTypeTemplates.splice(index, 1);
      }
    }

    if (this.templatesBySource.has(template.source)) {
      this.templatesBySource.get(template.source).delete(templateId);
    }

    if (template.source === 'custom') {
      this.saveCustomTemplates();
    } else if (template.source === 'learned') {
      this.templateLearner.deleteLearnedTemplate(templateId);
    }

    return true;
  }

  /**
   * 重新加载模板
   */
  reload() {
    this.loadAllTemplates();
  }

  /**
   * 获取模板统计信息
   * @returns {Object} 统计信息
   */
  getStats() {
    return {
      total: this.templates.size,
      bySource: {
        preset: this.templatesBySource.get('preset').size,
        learned: this.templatesBySource.get('learned').size,
        custom: this.templatesBySource.get('custom').size
      },
      byCategory: this.getCategoryStats()
    };
  }

  /**
   * 获取分类统计
   * @returns {Object} 分类统计
   */
  getCategoryStats() {
    const stats = {};
    for (const template of this.templates.values()) {
      const category = template.category || 'unknown';
      stats[category] = (stats[category] || 0) + 1;
    }
    return stats;
  }
}

const templateLibrary = new TemplateLibrary();

module.exports = {
  TemplateLibrary,
  TemplateLearner,
  templateLibrary
};

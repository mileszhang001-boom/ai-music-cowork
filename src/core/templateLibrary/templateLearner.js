'use strict';

/**
 * @fileoverview Template Learner - 模板学习机制
 * @description 从 LLM 生成的 Scene Descriptor 中学习模板，支持用户反馈检测和优先级动态调整
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class TemplateLearner {
  constructor(config = {}) {
    this.config = {
      feedbackWindowMs: 30000,
      minAcceptCount: 2,
      maxLearnedTemplates: 100,
      priorityBoostPerAccept: 0.5,
      ...config
    };
    
    this.pendingExecutions = new Map();
    this.learnedTemplates = new Map();
    this.templateStats = new Map();
    this.negativeActions = ['skip', 'volume_change', 'pause', 'dislike', 'switch_scene'];
    
    this.learnedTemplatesPath = config.learnedTemplatesPath || 
      path.join(process.cwd(), 'templates/learned_templates.json');
    
    this.loadLearnedTemplates();
  }

  /**
   * 加载已学习的模板
   */
  loadLearnedTemplates() {
    try {
      if (fs.existsSync(this.learnedTemplatesPath)) {
        const data = JSON.parse(fs.readFileSync(this.learnedTemplatesPath, 'utf8'));
        for (const template of data.templates || []) {
          this.learnedTemplates.set(template.template_id, template);
          if (template.stats) {
            this.templateStats.set(template.template_id, template.stats);
          }
        }
      }
    } catch (error) {
      console.warn('Failed to load learned templates:', error.message);
    }
  }

  /**
   * 保存已学习的模板
   */
  saveLearnedTemplates() {
    try {
      const dir = path.dirname(this.learnedTemplatesPath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      
      const templates = Array.from(this.learnedTemplates.values()).map(t => ({
        ...t,
        stats: this.templateStats.get(t.template_id) || { acceptCount: 1, lastUsed: new Date().toISOString() }
      }));
      
      fs.writeFileSync(this.learnedTemplatesPath, JSON.stringify({
        version: '1.0',
        lastUpdated: new Date().toISOString(),
        templates
      }, null, 2), 'utf8');
      
      return true;
    } catch (error) {
      console.error('Failed to save learned templates:', error.message);
      return false;
    }
  }

  /**
   * 记录场景执行，开始反馈检测窗口
   * @param {string} executionId - 执行ID
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @param {Object} context - 上下文信息
   */
  recordExecution(executionId, sceneDescriptor, context = {}) {
    this.pendingExecutions.set(executionId, {
      sceneDescriptor,
      context,
      timestamp: Date.now(),
      feedbackReceived: false
    });

    setTimeout(() => {
      this.checkAndLearn(executionId);
    }, this.config.feedbackWindowMs);
  }

  /**
   * 记录用户反馈
   * @param {string} executionId - 执行ID
   * @param {string} action - 用户动作
   * @param {Object} data - 动作数据
   */
  recordFeedback(executionId, action, data = {}) {
    const execution = this.pendingExecutions.get(executionId);
    if (!execution) return;

    execution.feedbackReceived = true;

    if (this.negativeActions.includes(action)) {
      execution.isNegative = true;
      execution.negativeAction = action;
      execution.negativeData = data;
    }
  }

  /**
   * 检查并学习模板
   * @param {string} executionId - 执行ID
   */
  checkAndLearn(executionId) {
    const execution = this.pendingExecutions.get(executionId);
    if (!execution) return;

    this.pendingExecutions.delete(executionId);

    if (execution.feedbackReceived && execution.isNegative) {
      console.log(`[TemplateLearner] Negative feedback received for ${executionId}, skipping learning`);
      this.handleNegativeFeedback(execution);
      return;
    }

    this.learnFromDescriptor(execution.sceneDescriptor, execution.context);
  }

  /**
   * 从 Scene Descriptor 学习模板
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @param {Object} context - 上下文
   * @returns {Object|null} 学习的模板
   */
  learnFromDescriptor(sceneDescriptor, context = {}) {
    if (!sceneDescriptor || !sceneDescriptor.intent) {
      return null;
    }

    const templateId = this.generateTemplateId(sceneDescriptor);
    
    let template = this.learnedTemplates.get(templateId);
    
    if (template) {
      this.updateTemplateStats(templateId);
      this.adjustTemplatePriority(templateId);
      return template;
    }

    template = this.extractTemplate(sceneDescriptor, context);
    template.template_id = templateId;
    template.source = 'learned';
    template.learnedAt = new Date().toISOString();
    
    this.learnedTemplates.set(templateId, template);
    this.templateStats.set(templateId, {
      acceptCount: 1,
      firstLearned: new Date().toISOString(),
      lastUsed: new Date().toISOString()
    });

    console.log(`[TemplateLearner] Learned new template: ${templateId} (${template.name})`);
    
    this.saveLearnedTemplates();
    
    return template;
  }

  /**
   * 从 Scene Descriptor 提取模板特征
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @param {Object} context - 上下文
   * @returns {Object} 模板对象
   */
  extractTemplate(sceneDescriptor, context) {
    const template = {
      scene_type: sceneDescriptor.intent?.scene_type || this.inferSceneType(sceneDescriptor),
      name: this.generateTemplateName(sceneDescriptor),
      description: this.generateTemplateDescription(sceneDescriptor),
      category: this.inferCategory(sceneDescriptor, context),
      intent: this.extractIntent(sceneDescriptor),
      hints: this.extractHints(sceneDescriptor),
      triggers: this.extractTriggers(sceneDescriptor, context),
      priority: 5,
      announcement_templates: this.extractAnnouncements(sceneDescriptor)
    };

    return template;
  }

  /**
   * 提取 intent 字段
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {Object} intent
   */
  extractIntent(sceneDescriptor) {
    const intent = { ...sceneDescriptor.intent };
    
    if (!intent.mood) {
      intent.mood = { valence: 0.5, arousal: 0.4 };
    }
    if (intent.energy_level === undefined) {
      intent.energy_level = intent.mood?.arousal || 0.4;
    }
    if (!intent.atmosphere) {
      intent.atmosphere = 'neutral';
    }
    
    return intent;
  }

  /**
   * 提取 hints 字段
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {Object} hints
   */
  extractHints(sceneDescriptor) {
    const hints = sceneDescriptor.hints || {};
    
    return {
      music: hints.music || { genres: ['pop'], tempo: 'medium', vocal_style: 'any' },
      lighting: hints.lighting || { color_theme: 'calm', pattern: 'breathing', intensity: 0.4 },
      audio: hints.audio || { preset: 'standard' }
    };
  }

  /**
   * 提取触发条件
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @param {Object} context - 上下文
   * @returns {Object} triggers
   */
  extractTriggers(sceneDescriptor, context) {
    const triggers = {};
    const constraints = sceneDescriptor.intent?.constraints || {};
    
    if (context.timeRange) {
      triggers.time_range = context.timeRange;
    } else if (context.hour !== undefined) {
      const hour = context.hour;
      if (hour >= 6 && hour < 9) {
        triggers.time_range = ['06:00', '09:00'];
      } else if (hour >= 9 && hour < 12) {
        triggers.time_range = ['09:00', '12:00'];
      } else if (hour >= 12 && hour < 14) {
        triggers.time_range = ['12:00', '14:00'];
      } else if (hour >= 14 && hour < 18) {
        triggers.time_range = ['14:00', '18:00'];
      } else if (hour >= 18 && hour < 22) {
        triggers.time_range = ['18:00', '22:00'];
      } else {
        triggers.time_range = ['22:00', '06:00'];
      }
    }

    if (context.weather) {
      triggers.weather = [context.weather];
    }

    if (context.passengerCount !== undefined) {
      if (context.passengerCount === 0) {
        triggers.max_passengers = 0;
      } else if (context.passengerCount >= 2) {
        triggers.min_passengers = 2;
      }
    }

    if (context.speed !== undefined) {
      if (context.speed < 20) {
        triggers.vehicle_speed = { max: 20 };
      } else if (context.speed >= 80) {
        triggers.min_speed = 80;
      }
    }

    return triggers;
  }

  /**
   * 提取播报模板
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {Array} 播报模板列表
   */
  extractAnnouncements(sceneDescriptor) {
    if (sceneDescriptor.announcement) {
      return [sceneDescriptor.announcement];
    }
    
    const sceneType = sceneDescriptor.intent?.scene_type || 'default';
    return [`已为你切换到${sceneType}模式`];
  }

  /**
   * 推断场景类型
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {string} 场景类型
   */
  inferSceneType(sceneDescriptor) {
    const intent = sceneDescriptor.intent || {};
    const hints = sceneDescriptor.hints || {};
    
    if (intent.mood?.arousal > 0.7 && intent.energy_level > 0.7) {
      return 'high_energy';
    }
    if (intent.mood?.arousal < 0.3 && intent.energy_level < 0.3) {
      return 'relaxed';
    }
    if (hints.music?.genres?.includes('ambient') || hints.music?.tempo === 'slow') {
      return 'calm';
    }
    
    return 'custom';
  }

  /**
   * 推断分类
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @param {Object} context - 上下文
   * @returns {string} 分类
   */
  inferCategory(sceneDescriptor, context) {
    if (context.weather) return 'weather';
    if (context.passengerCount !== undefined && context.passengerCount > 0) return 'social';
    if (context.hour !== undefined) {
      if (context.hour < 6 || context.hour >= 22) return 'time';
    }
    
    const hints = sceneDescriptor.hints || {};
    if (hints.music?.tempo === 'fast' || sceneDescriptor.intent?.energy_level > 0.6) {
      return 'state';
    }
    
    return 'special';
  }

  /**
   * 生成模板名称
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {string} 模板名称
   */
  generateTemplateName(sceneDescriptor) {
    const sceneType = sceneDescriptor.intent?.scene_type || 'Custom';
    const atmosphere = sceneDescriptor.intent?.atmosphere || '';
    
    if (atmosphere) {
      return `${sceneType} - ${atmosphere}`;
    }
    return `自定义${sceneType}`;
  }

  /**
   * 生成模板描述
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {string} 模板描述
   */
  generateTemplateDescription(sceneDescriptor) {
    const intent = sceneDescriptor.intent || {};
    const parts = [];
    
    if (intent.atmosphere) {
      parts.push(intent.atmosphere);
    }
    if (intent.mood) {
      if (intent.mood.valence > 0.6) parts.push('愉悦');
      else if (intent.mood.valence < 0.4) parts.push('平静');
      
      if (intent.mood.arousal > 0.6) parts.push('活力');
      else if (intent.mood.arousal < 0.4) parts.push('放松');
    }
    
    return parts.length > 0 ? parts.join('、') + '氛围' : '用户自定义场景';
  }

  /**
   * 生成模板ID
   * @param {Object} sceneDescriptor - Scene Descriptor
   * @returns {string} 模板ID
   */
  generateTemplateId(sceneDescriptor) {
    const keyParts = [
      sceneDescriptor.intent?.scene_type || 'custom',
      sceneDescriptor.intent?.atmosphere || '',
      JSON.stringify(sceneDescriptor.hints?.music?.genres || []),
      sceneDescriptor.intent?.mood?.valence?.toFixed(1) || '0.5',
      sceneDescriptor.intent?.mood?.arousal?.toFixed(1) || '0.4'
    ];
    
    const hash = crypto
      .createHash('md5')
      .update(keyParts.join('|'))
      .digest('hex')
      .substring(0, 8);
    
    return `LEARNED_${hash.toUpperCase()}`;
  }

  /**
   * 处理负面反馈
   * @param {Object} execution - 执行记录
   */
  handleNegativeFeedback(execution) {
    const sceneDescriptor = execution.sceneDescriptor;
    const templateId = this.generateTemplateId(sceneDescriptor);
    
    if (this.learnedTemplates.has(templateId)) {
      const stats = this.templateStats.get(templateId);
      if (stats) {
        stats.rejectCount = (stats.rejectCount || 0) + 1;
        stats.lastRejected = new Date().toISOString();
        
        if (stats.rejectCount >= 3 && stats.acceptCount < stats.rejectCount) {
          this.learnedTemplates.delete(templateId);
          this.templateStats.delete(templateId);
          console.log(`[TemplateLearner] Removed template ${templateId} due to repeated negative feedback`);
          this.saveLearnedTemplates();
        }
      }
    }
  }

  /**
   * 更新模板统计
   * @param {string} templateId - 模板ID
   */
  updateTemplateStats(templateId) {
    const stats = this.templateStats.get(templateId) || {
      acceptCount: 0,
      firstLearned: new Date().toISOString()
    };
    
    stats.acceptCount = (stats.acceptCount || 0) + 1;
    stats.lastUsed = new Date().toISOString();
    
    this.templateStats.set(templateId, stats);
  }

  /**
   * 调整模板优先级
   * @param {string} templateId - 模板ID
   */
  adjustTemplatePriority(templateId) {
    const template = this.learnedTemplates.get(templateId);
    const stats = this.templateStats.get(templateId);
    
    if (!template || !stats) return;

    const basePriority = 5;
    const boost = Math.min(stats.acceptCount * this.config.priorityBoostPerAccept, 4);
    template.priority = Math.max(1, basePriority - boost);
    
    this.templateStats.set(templateId, stats);
  }

  /**
   * 获取所有学习的模板
   * @returns {Array} 模板列表
   */
  getLearnedTemplates() {
    return Array.from(this.learnedTemplates.values());
  }

  /**
   * 获取模板统计
   * @param {string} templateId - 模板ID
   * @returns {Object|null} 统计信息
   */
  getTemplateStats(templateId) {
    return this.templateStats.get(templateId) || null;
  }

  /**
   * 清除所有学习的模板
   */
  clearLearnedTemplates() {
    this.learnedTemplates.clear();
    this.templateStats.clear();
    this.saveLearnedTemplates();
  }

  /**
   * 删除特定模板
   * @param {string} templateId - 模板ID
   * @returns {boolean} 是否成功删除
   */
  deleteLearnedTemplate(templateId) {
    if (this.learnedTemplates.has(templateId)) {
      this.learnedTemplates.delete(templateId);
      this.templateStats.delete(templateId);
      this.saveLearnedTemplates();
      return true;
    }
    return false;
  }
}

module.exports = {
  TemplateLearner
};

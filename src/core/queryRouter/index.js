'use strict';

/**
 * @fileoverview Query Router - 查询路由器
 * @description 负责用户语音查询的路由、ACK 响应生成和 Announcement 协调播报
 */

const { eventBus, EventTypes } = require('../eventBus');

const QueryIntentTypes = {
  CREATIVE: 'creative',
  NAVIGATION: 'navigation',
  CONTROL: 'control',
  INFO: 'info'
};

const VoiceStyles = {
  WARM_FEMALE: 'warm_female',
  CALM_MALE: 'calm_male',
  ENERGETIC_FEMALE: 'energetic_female',
  PROFESSIONAL_MALE: 'professional_male'
};

const ACKTemplates = {
  [QueryIntentTypes.CREATIVE]: [
    '好的，让我想想适合现在的音乐...',
    '正在为您挑选合适的歌曲...',
    '我来为您安排一段音乐旅程...'
  ],
  [QueryIntentTypes.NAVIGATION]: [
    '好的，正在为您导航...',
    '已收到，正在规划路线...'
  ],
  [QueryIntentTypes.CONTROL]: [
    '好的，马上为您调整...',
    '收到，正在处理...'
  ],
  [QueryIntentTypes.INFO]: [
    '让我查一下...',
    '正在为您查询...'
  ]
};

const AnnouncementPriority = {
  URGENT: 0,
  HIGH: 1,
  NORMAL: 2,
  LOW: 3
};

class QueryRouter {
  constructor(config = {}) {
    this.config = config;
    this.defaultVoiceStyle = config.defaultVoiceStyle || VoiceStyles.WARM_FEMALE;
    this.defaultWaitTime = config.defaultWaitTime || 8;
    this.queryHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
    
    this.ackQueue = [];
    this.announcementQueue = [];
    this.isAckPlaying = false;
    this.maxQueueSize = config.maxQueueSize || 10;
    
    this.setupEventListeners();
  }

  /**
   * 设置事件监听
   */
  setupEventListeners() {
    eventBus.on(EventTypes.USER_VOICE_INPUT, (data) => {
      this.route(data);
    });

    eventBus.on(EventTypes.ACK_COMPLETED, () => {
      this.handleAckCompleted();
    });

    eventBus.on(EventTypes.SCENE_EXECUTED, (data) => {
      if (data.announcement) {
        this.queueAnnouncement(data.announcement, data.priority || AnnouncementPriority.NORMAL);
      }
    });
  }

  /**
   * 路由用户查询
   * @param {Object} voiceInput - 语音输入
   * @returns {Object} 路由结果
   */
  route(voiceInput) {
    const intent = this.classifyIntent(voiceInput);
    const ack = this.generateACK(intent, voiceInput);
    
    const result = {
      query_id: `query_${Date.now()}`,
      voice_input: voiceInput,
      intent: intent,
      ack: ack,
      timestamp: Date.now()
    };

    this.queryHistory.push(result);
    if (this.queryHistory.length > this.maxHistorySize) {
      this.queryHistory.shift();
    }

    this.queueACK(ack);

    return result;
  }

  /**
   * 分类查询意图
   * @param {Object} voiceInput - 语音输入
   * @returns {string} 意图类型
   */
  classifyIntent(voiceInput) {
    const text = (voiceInput.text || voiceInput.transcript || '').toLowerCase();

    const creativeKeywords = ['播放', '来首', '想听', '换个', '帮我选', '适合', '放点'];
    const navigationKeywords = ['导航', '去', '怎么走', '路线', '附近'];
    const controlKeywords = ['调大', '调小', '暂停', '继续', '下一首', '上一首', '打开', '关闭'];
    const infoKeywords = ['什么', '是谁', '哪里', '多少', '查询', '告诉我'];

    for (const keyword of creativeKeywords) {
      if (text.includes(keyword)) return QueryIntentTypes.CREATIVE;
    }

    for (const keyword of navigationKeywords) {
      if (text.includes(keyword)) return QueryIntentTypes.NAVIGATION;
    }

    for (const keyword of controlKeywords) {
      if (text.includes(keyword)) return QueryIntentTypes.CONTROL;
    }

    for (const keyword of infoKeywords) {
      if (text.includes(keyword)) return QueryIntentTypes.INFO;
    }

    return QueryIntentTypes.CREATIVE;
  }

  /**
   * 生成 ACK 消息
   * @param {string} intent - 意图类型
   * @param {Object} voiceInput - 语音输入
   * @returns {Object} ACK 消息
   */
  generateACK(intent, voiceInput) {
    const templates = ACKTemplates[intent] || ACKTemplates[QueryIntentTypes.CREATIVE];
    const randomIndex = Math.floor(Math.random() * templates.length);
    const text = templates[randomIndex];

    return {
      type: 'ack',
      text: text,
      voice_style: this.defaultVoiceStyle,
      timestamp: new Date().toISOString(),
      query_intent: intent,
      related_query: voiceInput.text || voiceInput.transcript,
      estimated_wait_sec: this.getEstimatedWaitTime(intent)
    };
  }

  /**
   * 获取预计等待时间
   * @param {string} intent - 意图类型
   * @returns {number} 预计等待时间（秒）
   */
  getEstimatedWaitTime(intent) {
    const waitTimes = {
      [QueryIntentTypes.CREATIVE]: 8,
      [QueryIntentTypes.NAVIGATION]: 3,
      [QueryIntentTypes.CONTROL]: 1,
      [QueryIntentTypes.INFO]: 5
    };
    return waitTimes[intent] || this.defaultWaitTime;
  }

  /**
   * 将 ACK 加入队列
   * @param {Object} ack - ACK 消息
   */
  queueACK(ack) {
    if (this.ackQueue.length >= this.maxQueueSize) {
      this.ackQueue.shift();
    }
    
    this.ackQueue.push(ack);
    this.processAckQueue();
  }

  /**
   * 处理 ACK 队列
   */
  processAckQueue() {
    if (this.isAckPlaying || this.ackQueue.length === 0) {
      return;
    }

    const ack = this.ackQueue.shift();
    this.isAckPlaying = true;

    eventBus.emit(EventTypes.ACK_TRIGGERED, ack);

    const ackDuration = this.estimateSpeechDuration(ack.text);
    setTimeout(() => {
      eventBus.emit(EventTypes.ACK_COMPLETED, { ack_id: ack.timestamp });
    }, ackDuration * 1000);
  }

  /**
   * 处理 ACK 播报完成
   */
  handleAckCompleted() {
    this.isAckPlaying = false;
    this.processAnnouncementQueue();
    this.processAckQueue();
  }

  /**
   * 将 Announcement 加入队列
   * @param {Object|string} announcement - 播报内容
   * @param {number} priority - 优先级
   */
  queueAnnouncement(announcement, priority = AnnouncementPriority.NORMAL) {
    const announcementObj = typeof announcement === 'string' 
      ? { text: announcement, type: 'announcement' }
      : announcement;

    announcementObj.priority = priority;
    announcementObj.timestamp = announcementObj.timestamp || new Date().toISOString();
    announcementObj.voice_style = announcementObj.voice_style || this.defaultVoiceStyle;

    if (this.announcementQueue.length >= this.maxQueueSize) {
      this.announcementQueue.sort((a, b) => a.priority - b.priority);
      this.announcementQueue.pop();
    }

    this.announcementQueue.push(announcementObj);
    this.announcementQueue.sort((a, b) => a.priority - b.priority);

    if (!this.isAckPlaying) {
      this.processAnnouncementQueue();
    }
  }

  /**
   * 处理 Announcement 队列
   */
  processAnnouncementQueue() {
    if (this.isAckPlaying || this.announcementQueue.length === 0) {
      return;
    }

    const announcement = this.announcementQueue.shift();
    eventBus.emit(EventTypes.ANNOUNCEMENT_TRIGGERED, announcement);
  }

  /**
   * 从模板生成 Announcement
   * @param {Object} template - 场景模板
   * @param {Object} context - 上下文
   * @returns {Object} Announcement 对象
   */
  generateAnnouncementFromTemplate(template, context = {}) {
    const announcementTemplates = template.announcement_templates || [];
    
    if (announcementTemplates.length === 0) {
      return {
        type: 'announcement',
        text: `已为您切换到${template.name || '新场景'}`,
        voice_style: this.defaultVoiceStyle,
        timestamp: new Date().toISOString(),
        scene_type: template.scene_type,
        template_id: template.template_id
      };
    }

    const randomIndex = Math.floor(Math.random() * announcementTemplates.length);
    const text = announcementTemplates[randomIndex];

    return {
      type: 'announcement',
      text: text,
      voice_style: this.getVoiceStyleForScene(template),
      timestamp: new Date().toISOString(),
      scene_type: template.scene_type,
      template_id: template.template_id,
      context: context
    };
  }

  /**
   * 根据场景获取语音风格
   * @param {Object} template - 模板
   * @returns {string} 语音风格
   */
  getVoiceStyleForScene(template) {
    const energyLevel = template.intent?.energy_level || 0.5;
    const category = template.category || '';

    if (category === 'social' && template.triggers?.min_passengers >= 2) {
      return VoiceStyles.ENERGETIC_FEMALE;
    }

    if (energyLevel > 0.7) {
      return VoiceStyles.ENERGETIC_FEMALE;
    }

    if (energyLevel < 0.3 || category === 'time' && template.scene_type?.includes('night')) {
      return VoiceStyles.CALM_MALE;
    }

    return this.defaultVoiceStyle;
  }

  /**
   * 估算语音播报时长
   * @param {string} text - 文本
   * @returns {number} 时长（秒）
   */
  estimateSpeechDuration(text) {
    const charCount = text.length;
    const baseDuration = 0.8;
    const charDuration = 0.15;
    return Math.max(1, baseDuration + charCount * charDuration);
  }

  /**
   * Voice ACK Bypass - 快速响应通道
   * @param {Object} voiceInput - 语音输入
   * @returns {Object|null} 快速 ACK 或 null
   */
  bypass(voiceInput) {
    const text = (voiceInput.text || voiceInput.transcript || '').toLowerCase();

    const bypassPatterns = [
      { pattern: /暂停/, ack: '好的，已暂停' },
      { pattern: /继续/, ack: '好的，继续播放' },
      { pattern: /下一首/, ack: '好的，下一首' },
      { pattern: /上一首/, ack: '好的，上一首' },
      { pattern: /声音大点/, ack: '好的，已调高音量' },
      { pattern: /声音小点/, ack: '好的，已调低音量' }
    ];

    for (const { pattern, ack } of bypassPatterns) {
      if (pattern.test(text)) {
        return {
          type: 'ack',
          text: ack,
          voice_style: this.defaultVoiceStyle,
          timestamp: new Date().toISOString(),
          query_intent: QueryIntentTypes.CONTROL,
          estimated_wait_sec: 0
        };
      }
    }

    return null;
  }

  /**
   * 获取查询历史
   * @param {number} [limit=10] - 限制数量
   * @returns {Array} 查询历史
   */
  getHistory(limit = 10) {
    return this.queryHistory.slice(-limit);
  }

  /**
   * 清空历史
   */
  clear() {
    this.queryHistory = [];
  }

  /**
   * 清空队列
   */
  clearQueues() {
    this.ackQueue = [];
    this.announcementQueue = [];
    this.isAckPlaying = false;
  }

  /**
   * 获取队列状态
   * @returns {Object} 队列状态
   */
  getQueueStatus() {
    return {
      ackQueueLength: this.ackQueue.length,
      announcementQueueLength: this.announcementQueue.length,
      isAckPlaying: this.isAckPlaying
    };
  }

  /**
   * 取消待播报的 Announcement
   * @param {string} [templateId] - 模板ID（可选，不提供则取消所有）
   */
  cancelAnnouncement(templateId) {
    if (templateId) {
      this.announcementQueue = this.announcementQueue.filter(
        a => a.template_id !== templateId
      );
    } else {
      this.announcementQueue = [];
    }
  }
}

const queryRouter = new QueryRouter();

module.exports = {
  QueryRouter,
  queryRouter,
  QueryIntentTypes,
  VoiceStyles,
  ACKTemplates,
  AnnouncementPriority
};

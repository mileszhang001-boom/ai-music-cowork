'use strict';

const { IntentTypes } = require('../types');

const ACKTemplates = {
  [IntentTypes.CREATIVE]: [
    '好的，让我想想适合现在的音乐...',
    '正在为您挑选合适的歌曲...',
    '我来为您安排一段音乐旅程...'
  ],
  [IntentTypes.NAVIGATION]: [
    '好的，正在为您导航...',
    '已收到，正在规划路线...'
  ],
  [IntentTypes.CONTROL]: [
    '好的，马上为您调整...',
    '收到，正在处理...'
  ],
  [IntentTypes.INFO]: [
    '让我查一下...',
    '正在为您查询...'
  ]
};

class QueryRouter {
  constructor(config = {}) {
    this.config = config;
    this.queryHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
  }

  route(userQuery) {
    if (!userQuery) return null;

    const text = userQuery.text || '';
    const intent = this.classifyIntent(text);
    const ack = this.generateACK(intent, text);

    const result = {
      query_id: `query_${Date.now()}`,
      text,
      intent,
      ack,
      timestamp: Date.now()
    };

    this.queryHistory.push(result);
    if (this.queryHistory.length > this.maxHistorySize) {
      this.queryHistory.shift();
    }

    return result;
  }

  classifyIntent(text) {
    const lowerText = text.toLowerCase();

    const creativeKeywords = ['播放', '来首', '想听', '换个', '帮我选', '适合', '放点'];
    const navigationKeywords = ['导航', '去', '怎么走', '路线', '附近'];
    const controlKeywords = ['调大', '调小', '暂停', '继续', '下一首', '上一首', '打开', '关闭'];
    const infoKeywords = ['什么', '是谁', '哪里', '多少', '查询', '告诉我'];

    for (const keyword of creativeKeywords) {
      if (lowerText.includes(keyword)) return IntentTypes.CREATIVE;
    }

    for (const keyword of navigationKeywords) {
      if (lowerText.includes(keyword)) return IntentTypes.NAVIGATION;
    }

    for (const keyword of controlKeywords) {
      if (lowerText.includes(keyword)) return IntentTypes.CONTROL;
    }

    for (const keyword of infoKeywords) {
      if (lowerText.includes(keyword)) return IntentTypes.INFO;
    }

    return IntentTypes.CREATIVE;
  }

  generateACK(intent, text) {
    const templates = ACKTemplates[intent] || ACKTemplates[IntentTypes.CREATIVE];
    const randomIndex = Math.floor(Math.random() * templates.length);

    return {
      text: templates[randomIndex],
      estimated_wait_sec: this.getEstimatedWaitTime(intent)
    };
  }

  getEstimatedWaitTime(intent) {
    const waitTimes = {
      [IntentTypes.CREATIVE]: 8,
      [IntentTypes.NAVIGATION]: 3,
      [IntentTypes.CONTROL]: 1,
      [IntentTypes.INFO]: 5
    };
    return waitTimes[intent] || 5;
  }

  clear() {
    this.queryHistory = [];
  }
}

const queryRouter = new QueryRouter();

module.exports = {
  QueryRouter,
  queryRouter,
  IntentTypes,
  ACKTemplates
};

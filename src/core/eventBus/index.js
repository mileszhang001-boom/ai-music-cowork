'use strict';

const EventEmitter = require('events');

/**
 * @fileoverview Event Bus - 事件总线实现
 * @description 提供模块间异步通信能力，支持事件发布/订阅模式
 */

class EventBus extends EventEmitter {
  constructor(options = {}) {
    super();
    this.maxListeners = options.maxListeners || 100;
    this.eventHistory = [];
    this.maxHistorySize = options.maxHistorySize || 1000;
    this.middleware = [];
  }

  /**
   * 发布事件
   * @param {string} eventName - 事件名称
   * @param {*} data - 事件数据
   * @returns {void}
   */
  emit(eventName, data) {
    const event = {
      name: eventName,
      data: data,
      timestamp: Date.now()
    };

    this.eventHistory.push(event);
    if (this.eventHistory.length > this.maxHistorySize) {
      this.eventHistory.shift();
    }

    for (const mw of this.middleware) {
      try {
        mw(event);
      } catch (err) {
        console.error(`EventBus middleware error: ${err.message}`);
      }
    }

    super.emit(eventName, event);
    super.emit('*', event);
  }

  /**
   * 订阅事件
   * @param {string} eventName - 事件名称
   * @param {Function} listener - 监听器
   * @returns {EventBus}
   */
  on(eventName, listener) {
    return super.on(eventName, (event) => listener(event.data, event));
  }

  /**
   * 一次性订阅
   * @param {string} eventName - 事件名称
   * @param {Function} listener - 监听器
   * @returns {EventBus}
   */
  once(eventName, listener) {
    return super.once(eventName, (event) => listener(event.data, event));
  }

  /**
   * 添加中间件
   * @param {Function} middleware - 中间件函数
   * @returns {void}
   */
  use(middleware) {
    this.middleware.push(middleware);
  }

  /**
   * 获取事件历史
   * @param {string} [eventName] - 可选的事件名称过滤
   * @returns {Array} 事件历史
   */
  getHistory(eventName) {
    if (eventName) {
      return this.eventHistory.filter(e => e.name === eventName);
    }
    return [...this.eventHistory];
  }

  /**
   * 清空事件历史
   * @returns {void}
   */
  clearHistory() {
    this.eventHistory = [];
  }
}

const eventBus = new EventBus();

const EventTypes = {
  MUSIC_BEAT: 'music.beat',
  MUSIC_TRACK_CHANGED: 'music.track_changed',
  MUSIC_PLAYBACK_STATE: 'music.playback_state',
  SCENE_CHANGED: 'scene.changed',
  SCENE_DETECTED: 'scene.detected',
  SCENE_EXECUTED: 'scene.executed',
  SIGNAL_RECEIVED: 'signal.received',
  SIGNAL_NORMALIZED: 'signal.normalized',
  INTENT_GENERATED: 'intent.generated',
  ACK_TRIGGERED: 'ack.triggered',
  ACK_COMPLETED: 'ack.completed',
  ANNOUNCEMENT_TRIGGERED: 'announcement.triggered',
  FEEDBACK_REPORTED: 'feedback.reported',
  ENGINE_ACTION: 'engine.action',
  ORCHESTRATOR_COMMAND: 'orchestrator.command',
  USER_VOICE_INPUT: 'user.voice_input',
  VHAL_EVENT: 'vhal.event',
  USER_FEEDBACK: 'user.feedback'
};

module.exports = {
  EventBus,
  eventBus,
  EventTypes
};

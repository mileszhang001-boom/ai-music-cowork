'use strict';

const EventEmitter = require('events');

class EventBus extends EventEmitter {
  constructor(options = {}) {
    super();
    this.maxListeners = options.maxListeners || 100;
    this.eventHistory = [];
    this.maxHistorySize = options.maxHistorySize || 1000;
  }

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

    super.emit(eventName, event);
    super.emit('*', event);
  }

  on(eventName, listener) {
    return super.on(eventName, (event) => listener(event.data, event));
  }

  once(eventName, listener) {
    return super.once(eventName, (event) => listener(event.data, event));
  }

  getHistory(eventName) {
    if (eventName) {
      return this.eventHistory.filter(e => e.name === eventName);
    }
    return [...this.eventHistory];
  }

  clearHistory() {
    this.eventHistory = [];
  }
}

const eventBus = new EventBus();

const EventTypes = {
  SIGNAL_NORMALIZED: 'signal.normalized',
  SCENE_DESCRIPTOR_GENERATED: 'scene.descriptor_generated',
  EFFECTS_EXECUTED: 'effects.executed',
  EFFECTS_OUTPUT_GENERATED: 'effects.output_generated'
};

module.exports = {
  EventBus,
  eventBus,
  EventTypes
};

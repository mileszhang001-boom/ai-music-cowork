'use strict';

const assert = require('assert');
const { EventBus, EventTypes } = require('../../src/core/eventBus');

describe('EventBus', () => {
  let eventBus;

  beforeEach(() => {
    eventBus = new EventBus();
  });

  describe('emit and on', () => {
    it('should emit and receive events', (done) => {
      eventBus.on('test.event', (data) => {
        assert.strictEqual(data.message, 'hello');
        done();
      });

      eventBus.emit('test.event', { message: 'hello' });
    });

    it('should include timestamp in events', (done) => {
      eventBus.on('test.event', (data, event) => {
        assert.ok(event.timestamp);
        done();
      });

      eventBus.emit('test.event', { message: 'test' });
    });
  });

  describe('once', () => {
    it('should only trigger once', () => {
      let count = 0;
      
      eventBus.once('test.once', () => {
        count++;
      });

      eventBus.emit('test.once', {});
      eventBus.emit('test.once', {});

      assert.strictEqual(count, 1);
    });
  });

  describe('middleware', () => {
    it('should process middleware', (done) => {
      eventBus.use((event) => {
        event.data.processed = true;
      });

      eventBus.on('test.event', (data) => {
        assert.ok(data.processed);
        done();
      });

      eventBus.emit('test.event', {});
    });
  });

  describe('history', () => {
    it('should store event history', () => {
      eventBus.emit('test.event1', { a: 1 });
      eventBus.emit('test.event2', { b: 2 });

      const history = eventBus.getHistory();
      assert.strictEqual(history.length, 2);
    });

    it('should filter history by event name', () => {
      eventBus.emit('test.event1', { a: 1 });
      eventBus.emit('test.event2', { b: 2 });
      eventBus.emit('test.event1', { c: 3 });

      const history = eventBus.getHistory('test.event1');
      assert.strictEqual(history.length, 2);
    });
  });
});

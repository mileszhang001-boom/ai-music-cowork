'use strict';

const assert = require('assert');
const { QueryRouter, QueryIntentTypes } = require('../../src/core/queryRouter');

describe('QueryRouter', () => {
  let router;

  beforeEach(() => {
    router = new QueryRouter();
  });

  afterEach(() => {
    router.clear();
  });

  describe('classifyIntent', () => {
    it('should classify creative intent', () => {
      const intent = router.classifyIntent({ text: '帮我选一首适合现在的歌' });
      assert.strictEqual(intent, QueryIntentTypes.CREATIVE);
    });

    it('should classify navigation intent', () => {
      const intent = router.classifyIntent({ text: '导航去机场' });
      assert.strictEqual(intent, QueryIntentTypes.NAVIGATION);
    });

    it('should classify control intent', () => {
      const intent = router.classifyIntent({ text: '调大音量' });
      assert.strictEqual(intent, QueryIntentTypes.CONTROL);
    });

    it('should classify info intent', () => {
      const intent = router.classifyIntent({ text: '这首歌是谁唱的' });
      assert.strictEqual(intent, QueryIntentTypes.INFO);
    });
  });

  describe('generateACK', () => {
    it('should generate ACK message', () => {
      const ack = router.generateACK(QueryIntentTypes.CREATIVE, { text: '来首轻音乐' });

      assert.strictEqual(ack.type, 'ack');
      assert.ok(ack.text);
      assert.ok(ack.voice_style);
      assert.ok(ack.timestamp);
      assert.strictEqual(ack.query_intent, QueryIntentTypes.CREATIVE);
    });
  });

  describe('bypass', () => {
    it('should bypass simple control commands', () => {
      const ack = router.bypass({ text: '暂停' });

      assert.ok(ack);
      assert.strictEqual(ack.estimated_wait_sec, 0);
    });

    it('should not bypass complex queries', () => {
      const ack = router.bypass({ text: '帮我选首歌' });

      assert.strictEqual(ack, null);
    });
  });

  describe('route', () => {
    it('should route voice input and generate ACK', () => {
      const result = router.route({ text: '来首适合开车的歌' });

      assert.ok(result.query_id);
      assert.strictEqual(result.intent, QueryIntentTypes.CREATIVE);
      assert.ok(result.ack);
    });
  });
});

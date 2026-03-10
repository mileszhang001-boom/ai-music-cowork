'use strict';

const assert = require('assert');
const { Layer1Processor, SignalSources, SignalCategories } = require('../../src/core/layer1');

describe('Layer1Processor', () => {
  let processor;

  beforeEach(() => {
    processor = new Layer1Processor();
  });

  afterEach(() => {
    processor.clear();
  });

  describe('normalize', () => {
    it('should normalize VHAL speed signal', () => {
      const rawSignal = {
        source: SignalSources.VHAL,
        type: 'vehicle_speed',
        value: 100,
        timestamp: Date.now()
      };

      const normalized = processor.normalize(rawSignal);

      assert.strictEqual(normalized.source, SignalSources.VHAL);
      assert.strictEqual(normalized.category, SignalCategories.CONTEXT);
      assert.ok(normalized.value >= 0 && normalized.value <= 1);
      assert.ok(normalized.confidence > 0);
    });

    it('should normalize biometric heart rate signal', () => {
      const rawSignal = {
        source: SignalSources.BIOMETRIC,
        type: 'heart_rate',
        value: 80,
        timestamp: Date.now(),
        confidence: 0.9
      };

      const normalized = processor.normalize(rawSignal);

      assert.strictEqual(normalized.source, SignalSources.BIOMETRIC);
      assert.strictEqual(normalized.category, SignalCategories.USER_STATE);
      assert.ok(normalized.value >= 0 && normalized.value <= 1);
    });

    it('should apply confidence decay for old signals', () => {
      const oldTimestamp = Date.now() - 10000;
      const rawSignal = {
        source: SignalSources.VHAL,
        type: 'vehicle_speed',
        value: 100,
        timestamp: oldTimestamp,
        confidence: 1.0
      };

      const normalized = processor.normalize(rawSignal);

      assert.ok(normalized.confidence < 1.0);
    });
  });

  describe('processBatch', () => {
    it('should process multiple signals', () => {
      const rawSignals = [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: 80, timestamp: Date.now() },
        { source: SignalSources.ENVIRONMENT, type: 'temperature', value: 25, timestamp: Date.now() },
        { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 75, timestamp: Date.now() }
      ];

      const output = processor.processBatch(rawSignals);

      assert.ok(output.output_id);
      assert.strictEqual(output.signals.length, 3);
      assert.ok(output.signal_summary.total_count === 3);
      assert.ok(output.signal_summary.active_sources.length === 3);
    });
  });

  describe('getActiveSignals', () => {
    it('should return only active signals', () => {
      processor.process({
        source: SignalSources.VHAL,
        type: 'vehicle_speed',
        value: 100,
        timestamp: Date.now()
      });

      const active = processor.getActiveSignals(30000);
      assert.strictEqual(active.length, 1);
    });
  });
});

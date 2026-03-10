'use strict';

const assert = require('assert');
const { Layer2Processor, SceneTypes } = require('../../src/core/layer2');

describe('Layer2Processor', () => {
  let processor;

  beforeEach(() => {
    processor = new Layer2Processor();
  });

  afterEach(() => {
    processor.clear();
  });

  describe('calculateSceneVector', () => {
    it('should calculate scene vector from signals', () => {
      const signals = [
        { source: 'vhal', type: 'vehicle_speed', value: { vehicle_speed: 0.5 }, confidence: 0.9, timestamp: Date.now() },
        { source: 'environment', type: 'time_of_day', value: { time_of_day: 0.8 }, confidence: 0.95, timestamp: Date.now() }
      ];

      const vector = processor.calculateSceneVector(signals);

      assert.ok(vector.scene_type);
      assert.ok(vector.dimensions);
      assert.ok(vector.confidence >= 0 && vector.confidence <= 1);
    });

    it('should detect fatigue alert scene', () => {
      const signals = [
        { source: 'biometric', type: 'fatigue_level', value: { fatigue_level: 0.8 }, confidence: 0.9, timestamp: Date.now() }
      ];

      const vector = processor.calculateSceneVector(signals);

      assert.strictEqual(vector.scene_type, SceneTypes.FATIGUE_ALERT);
    });
  });

  describe('detectChange', () => {
    it('should detect initial scene change', () => {
      const newScene = {
        scene_type: SceneTypes.MORNING_COMMUTE,
        dimensions: { social: 0, energy: 0.5, focus: 0.3, time_context: 0.5, weather: 0 },
        confidence: 0.8
      };

      const change = processor.detectChange(newScene);

      assert.ok(change.scene_changed);
      assert.strictEqual(change.change_type, 'initial');
    });

    it('should not detect change below threshold', () => {
      const scene1 = {
        scene_type: SceneTypes.MORNING_COMMUTE,
        dimensions: { social: 0, energy: 0.5, focus: 0.3, time_context: 0.5, weather: 0 },
        confidence: 0.8
      };

      const scene2 = {
        scene_type: SceneTypes.MORNING_COMMUTE,
        dimensions: { social: 0, energy: 0.52, focus: 0.31, time_context: 0.5, weather: 0 },
        confidence: 0.8
      };

      processor.currentScene = scene1;
      const change = processor.detectChange(scene2);

      assert.ok(!change.scene_changed);
    });
  });

  describe('process', () => {
    it('should process layer1 output and generate layer2 output', () => {
      const layer1Output = {
        signals: [
          { source: 'vhal', type: 'vehicle_speed', value: { vehicle_speed: 0.4 }, confidence: 0.9, timestamp: Date.now() },
          { source: 'environment', type: 'time_of_day', value: { time_of_day: 0.3 }, confidence: 0.95, timestamp: Date.now() }
        ]
      };

      const output = processor.process(layer1Output);

      assert.ok(output.output_id);
      assert.ok(output.scene_vector);
      assert.ok(output.change_detection);
    });
  });
});

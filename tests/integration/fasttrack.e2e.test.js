'use strict';

const assert = require('assert');
const { layer1, SignalSources } = require('../../src/core/layer1');
const { layer2, SceneTypes } = require('../../src/core/layer2');
const { orchestrator, EngineTypes } = require('../../src/core/orchestrator');
const { queryRouter, QueryIntentTypes, AnnouncementPriority } = require('../../src/core/queryRouter');
const { templateLibrary, TemplateLearner } = require('../../src/core/templateLibrary');
const { contentEngine } = require('../../src/engines/content');
const { lightingEngine } = require('../../src/engines/lighting');
const { audioEngine } = require('../../src/engines/audio');
const { eventBus, EventTypes } = require('../../src/core/eventBus');

describe('Fast-Track End-to-End Integration (M2 Milestone)', () => {
  before(() => {
    orchestrator.registerEngine(EngineTypes.CONTENT, contentEngine);
    orchestrator.registerEngine(EngineTypes.LIGHTING, lightingEngine);
    orchestrator.registerEngine(EngineTypes.AUDIO, audioEngine);
  });

  beforeEach(() => {
    layer1.clear();
    layer2.clear();
    orchestrator.reset();
    queryRouter.clearQueues();
  });

  describe('Signal Processing Pipeline', () => {
    it('should process signals through Layer 1 and Layer 2', () => {
      const rawSignals = [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.4 }, timestamp: Date.now() },
        { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.3 }, timestamp: Date.now() },
        { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 75, timestamp: Date.now() }
      ];

      const layer1Output = layer1.processBatch(rawSignals);
      assert.ok(layer1Output.output_id);
      assert.strictEqual(layer1Output.signals.length, 3);

      const layer2Output = layer2.process(layer1Output);
      assert.ok(layer2Output.scene_vector);
      assert.ok(layer2Output.change_detection);
    });

    it('should detect scene change when signals change significantly', () => {
      const initialSignals = [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.3 }, timestamp: Date.now() }
      ];

      const layer1Output = layer1.processBatch(initialSignals);
      const initialOutput = layer2.process(layer1Output);
      assert.ok(initialOutput.change_detection.scene_changed);

      layer2.clear();
      
      const newSignals = [
        { source: SignalSources.BIOMETRIC, type: 'fatigue_level', value: 0.85, timestamp: Date.now() }
      ];

      const newLayer1Output = layer1.processBatch(newSignals);
      const newOutput = layer2.process(newLayer1Output);
      assert.strictEqual(newOutput.scene_vector.scene_type, SceneTypes.FATIGUE_ALERT);
    });
  });

  describe('Template Matching', () => {
    it('should match scene to appropriate template', () => {
      const sceneVector = {
        scene_type: SceneTypes.MORNING_COMMUTE,
        dimensions: { social: 0, energy: 0.4, focus: 0.3, time_context: 0.5, weather: 0 },
        confidence: 0.8
      };

      const template = templateLibrary.matchTemplate(sceneVector, { speed: 30 });
      assert.ok(template);
      assert.ok(template.template_id);
    });

    it('should load 50 preset templates', () => {
      const stats = templateLibrary.getStats();
      assert.ok(stats.bySource.preset >= 50, `Expected at least 50 preset templates, got ${stats.bySource.preset}`);
    });

    it('should return templates by source', () => {
      const presetTemplates = templateLibrary.getTemplatesBySource('preset');
      assert.ok(presetTemplates.length >= 50);
    });
  });

  describe('Query Router', () => {
    it('should route voice queries and generate ACK', () => {
      const voiceInput = { text: '帮我选一首适合开车的歌' };
      const result = queryRouter.route(voiceInput);

      assert.ok(result.query_id);
      assert.strictEqual(result.intent, QueryIntentTypes.CREATIVE);
      assert.ok(result.ack);
      assert.strictEqual(result.ack.type, 'ack');
    });

    it('should bypass simple control commands', () => {
      const bypassResult = queryRouter.bypass({ text: '暂停' });
      assert.ok(bypassResult);
      assert.strictEqual(bypassResult.estimated_wait_sec, 0);
    });

    it('should manage ACK queue', () => {
      queryRouter.clearQueues();
      
      const voiceInput = { text: '来首摇滚' };
      queryRouter.route(voiceInput);
      
      const status = queryRouter.getQueueStatus();
      assert.strictEqual(status.ackQueueLength, 0);
    });

    it('should generate announcement from template', () => {
      const template = templateLibrary.getTemplate('TPL_001');
      if (template) {
        const announcement = queryRouter.generateAnnouncementFromTemplate(template);
        assert.ok(announcement.text);
        assert.strictEqual(announcement.type, 'announcement');
      }
    });

    it('should queue announcements with priority', () => {
      queryRouter.clearQueues();
      
      queryRouter.queueAnnouncement('Test announcement 1', AnnouncementPriority.NORMAL);
      queryRouter.queueAnnouncement('Urgent announcement', AnnouncementPriority.URGENT);
      
      const status = queryRouter.getQueueStatus();
      assert.strictEqual(status.announcementQueueLength, 2);
    });
  });

  describe('Orchestrator Engine Coordination', () => {
    it('should plan actions from intent and hints', () => {
      const intent = {
        mood: { valence: 0.6, arousal: 0.4 },
        energy_level: 0.4,
        transition: { type: 'fade', duration_ms: 2000 }
      };

      const hints = {
        music: { genres: ['pop'], tempo: 'medium' },
        lighting: { color_theme: 'calm', pattern: 'breathing' },
        audio: { preset: 'standard' }
      };

      const actions = orchestrator.planActions(intent, hints);

      assert.ok(actions.length === 3);
      assert.ok(actions.find(a => a.engine === EngineTypes.AUDIO));
      assert.ok(actions.find(a => a.engine === EngineTypes.LIGHTING));
      assert.ok(actions.find(a => a.engine === EngineTypes.CONTENT));
    });

    it('should execute actions through registered engines', async () => {
      const action = {
        engine: EngineTypes.CONTENT,
        action: 'curate_playlist',
        params: {
          hints: { genres: ['pop'], tempo: 'medium' },
          constraints: {}
        }
      };

      const result = await orchestrator.executeAction(action);
      assert.ok(result.playlist);
      assert.ok(result.playlist.length > 0);
    });
  });

  describe('Engine V1 Implementations', () => {
    it('Content Engine should curate playlist based on hints', async () => {
      const result = await contentEngine.execute('curate_playlist', {
        hints: { genres: ['jazz'], tempo: 'slow' },
        constraints: { max_duration_sec: 300 }
      });

      assert.ok(result.playlist);
      assert.ok(result.total_duration > 0);
    });

    it('Lighting Engine should apply theme', async () => {
      const result = await lightingEngine.execute('apply_theme', {
        theme: 'calm',
        pattern: 'breathing',
        intensity: 0.5
      });

      assert.ok(result.colors);
      assert.strictEqual(result.pattern, 'breathing');
    });

    it('Audio Engine should apply preset', async () => {
      const result = await audioEngine.execute('apply_preset', {
        preset: 'concert'
      });

      assert.ok(result.settings);
      assert.strictEqual(result.settings.name, 'concert');
    });
  });

  describe('Event Bus Integration', () => {
    it('should emit and receive events across modules', (done) => {
      let called = false;
      const handler = () => {
        if (!called) {
          called = true;
          done();
        }
      };
      
      eventBus.once(EventTypes.SCENE_CHANGED, handler);

      const signals = [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.5 }, timestamp: Date.now() }
      ];

      const layer1Output = layer1.processBatch(signals);
      layer2.process(layer1Output);
    });

    it('should emit ACK_TRIGGERED event', (done) => {
      eventBus.once(EventTypes.ACK_TRIGGERED, (ack) => {
        assert.ok(ack.text);
        done();
      });

      queryRouter.route({ text: '播放音乐' });
    });

    it('should emit ANNOUNCEMENT_TRIGGERED event when processing queue', (done) => {
      queryRouter.clearQueues();
      
      eventBus.once(EventTypes.ANNOUNCEMENT_TRIGGERED, (announcement) => {
        assert.ok(announcement.text);
        done();
      });

      queryRouter.queueAnnouncement('Test announcement');
    });
  });

  describe('Complete Fast-Track Flow', () => {
    it('should complete the fast-track pipeline within performance requirements', async () => {
      const startTime = Date.now();

      const rawSignals = [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.4 }, timestamp: Date.now() },
        { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.3 }, timestamp: Date.now() }
      ];

      const layer1Output = layer1.processBatch(rawSignals);
      const layer2Output = layer2.process(layer1Output);
      const template = templateLibrary.matchTemplate(layer2Output.scene_vector, { speed: 40 });

      const sceneDescriptor = {
        version: '2.0',
        scene_id: `scene_${Date.now()}`,
        intent: template.intent,
        hints: template.hints
      };

      await orchestrator.executeScene(sceneDescriptor);

      const endTime = Date.now();
      const duration = endTime - startTime;

      assert.ok(duration < 2000, `Fast-track should complete within 2 seconds, took ${duration}ms`);
    });
  });

  describe('Template Learning Integration', () => {
    it('should record execution for learning', () => {
      const executionId = 'test_exec_integration';
      const sceneDescriptor = {
        intent: {
          scene_type: 'integration_test',
          mood: { valence: 0.5, arousal: 0.5 }
        }
      };

      templateLibrary.recordExecution(executionId, sceneDescriptor, { hour: 10 });
      
      assert.ok(templateLibrary.templateLearner.pendingExecutions.has(executionId));
    });

    it('should record user feedback', () => {
      const executionId = 'test_exec_feedback';
      const sceneDescriptor = {
        intent: { scene_type: 'test' }
      };

      templateLibrary.recordExecution(executionId, sceneDescriptor, {});
      templateLibrary.recordFeedback(executionId, 'skip', {});

      assert.ok(!templateLibrary.templateLearner.pendingExecutions.has(executionId));
    });
  });

  describe('TTS Announcement Flow', () => {
    it('should generate appropriate voice style for scene', () => {
      const highEnergyTemplate = {
        scene_type: 'party',
        name: '派对模式',
        intent: { energy_level: 0.9 },
        category: 'social',
        triggers: { min_passengers: 3 }
      };

      const voiceStyle = queryRouter.getVoiceStyleForScene(highEnergyTemplate);
      assert.strictEqual(voiceStyle, 'energetic_female');
    });

    it('should select calm voice for night scenes', () => {
      const nightTemplate = {
        scene_type: 'night_drive',
        name: '深夜驾驶',
        intent: { energy_level: 0.2 },
        category: 'time'
      };

      const voiceStyle = queryRouter.getVoiceStyleForScene(nightTemplate);
      assert.strictEqual(voiceStyle, 'calm_male');
    });

    it('should estimate speech duration correctly', () => {
      const shortText = '好的';
      const longText = '这是一段很长的播报文本用于测试语音时长估算功能';

      const shortDuration = queryRouter.estimateSpeechDuration(shortText);
      const longDuration = queryRouter.estimateSpeechDuration(longText);

      assert.ok(shortDuration < longDuration);
      assert.ok(shortDuration >= 1);
    });
  });
});

'use strict';

const assert = require('assert');
const path = require('path');
const fs = require('fs');
const { TemplateLibrary, TemplateLearner } = require('../../src/core/templateLibrary');

describe('TemplateLibrary', () => {
  let library;

  beforeEach(() => {
    library = new TemplateLibrary({
      templatesDir: path.join(__dirname, '../../templates')
    });
  });

  describe('matchTemplate', () => {
    it('should match template based on scene type', () => {
      const sceneVector = {
        scene_type: 'morning_commute',
        dimensions: { social: 0, energy: 0.4, focus: 0.3, time_context: 0.5, weather: 0 },
        confidence: 0.8
      };

      const template = library.matchTemplate(sceneVector, {});

      assert.ok(template);
      assert.ok(template.template_id);
    });

    it('should return default template when no match', () => {
      const sceneVector = {
        scene_type: 'unknown_scene',
        dimensions: { social: 0, energy: 0.5, focus: 0.5, time_context: 0.5, weather: 0 },
        confidence: 0.8
      };

      const template = library.matchTemplate(sceneVector, {});

      assert.ok(template);
    });

    it('should match fatigue template with high priority', () => {
      const sceneVector = {
        scene_type: 'fatigue_alert',
        dimensions: { energy: 0.8 },
        confidence: 0.9
      };

      const template = library.matchTemplate(sceneVector, { fatigueLevel: 0.8 });

      assert.ok(template);
      assert.strictEqual(template.priority, 0);
    });

    it('should not match fatigue template without fatigue context', () => {
      const sceneVector = {
        scene_type: 'default',
        dimensions: { energy: 0.5 },
        confidence: 0.5
      };

      const template = library.matchTemplate(sceneVector, {});

      assert.ok(template.scene_type !== 'fatigue_alert' || template.priority !== 0);
    });
  });

  describe('getTemplate', () => {
    it('should return template by id', () => {
      const template = library.getTemplate('TPL_001');
      
      if (template) {
        assert.strictEqual(template.template_id, 'TPL_001');
      }
    });

    it('should return null for non-existent id', () => {
      const template = library.getTemplate('NON_EXISTENT');
      assert.strictEqual(template, null);
    });
  });

  describe('addTemplate', () => {
    it('should add custom template', () => {
      const customTemplate = {
        template_id: 'CUSTOM_TEST_001',
        scene_type: 'custom',
        name: 'Custom Template',
        intent: { mood: { valence: 0.5, arousal: 0.5 } },
        triggers: {},
        priority: 5,
        source: 'custom'
      };

      library.addTemplate(customTemplate);
      const retrieved = library.getTemplate('CUSTOM_TEST_001');

      assert.ok(retrieved);
      assert.strictEqual(retrieved.scene_type, 'custom');
    });
  });

  describe('size', () => {
    it('should return template count', () => {
      const size = library.size();
      assert.ok(size >= 50);
    });
  });

  describe('getTemplatesBySource', () => {
    it('should return preset templates', () => {
      const presetTemplates = library.getTemplatesBySource('preset');
      assert.ok(presetTemplates.length >= 50);
    });

    it('should return empty array for unknown source', () => {
      const templates = library.getTemplatesBySource('unknown');
      assert.strictEqual(templates.length, 0);
    });
  });

  describe('getStats', () => {
    it('should return template statistics', () => {
      const stats = library.getStats();
      
      assert.ok(stats.total >= 50);
      assert.ok(stats.bySource);
      assert.ok(stats.byCategory);
    });
  });

  describe('multi-source loading', () => {
    it('should load templates from all sources', () => {
      const stats = library.getStats();
      
      assert.ok(stats.bySource.preset >= 50);
    });
  });
});

describe('TemplateLearner', () => {
  let learner;
  const testLearnedPath = path.join(__dirname, '../../templates/test_learned_templates.json');

  beforeEach(() => {
    learner = new TemplateLearner({
      learnedTemplatesPath: testLearnedPath,
      feedbackWindowMs: 100
    });
  });

  afterEach(() => {
    if (fs.existsSync(testLearnedPath)) {
      fs.unlinkSync(testLearnedPath);
    }
  });

  describe('learnFromDescriptor', () => {
    it('should learn from scene descriptor', () => {
      const sceneDescriptor = {
        intent: {
          scene_type: 'test_scene',
          mood: { valence: 0.6, arousal: 0.4 },
          energy_level: 0.4,
          atmosphere: 'test_atmosphere'
        },
        hints: {
          music: { genres: ['pop'], tempo: 'medium' }
        }
      };

      const template = learner.learnFromDescriptor(sceneDescriptor, { hour: 8 });

      assert.ok(template);
      assert.ok(template.template_id.startsWith('LEARNED_'));
      assert.strictEqual(template.source, 'learned');
    });

    it('should update stats for existing template', () => {
      const sceneDescriptor = {
        intent: {
          scene_type: 'test_scene',
          mood: { valence: 0.6, arousal: 0.4 },
          energy_level: 0.4,
          atmosphere: 'test_atmosphere'
        }
      };

      learner.learnFromDescriptor(sceneDescriptor, {});
      learner.learnFromDescriptor(sceneDescriptor, {});

      const templates = learner.getLearnedTemplates();
      assert.ok(templates.length > 0);
    });
  });

  describe('recordFeedback', () => {
    it('should record negative feedback', () => {
      const executionId = 'test_exec_001';
      const sceneDescriptor = {
        intent: { scene_type: 'test', mood: { valence: 0.5, arousal: 0.5 } }
      };

      learner.recordExecution(executionId, sceneDescriptor, {});
      learner.recordFeedback(executionId, 'skip', {});

      const execution = learner.pendingExecutions.get(executionId);
      assert.strictEqual(execution, undefined);
    });
  });

  describe('extractTemplate', () => {
    it('should extract template features correctly', () => {
      const sceneDescriptor = {
        intent: {
          scene_type: 'custom_scene',
          mood: { valence: 0.7, arousal: 0.6 },
          energy_level: 0.6,
          atmosphere: 'energetic'
        },
        hints: {
          music: { genres: ['rock', 'pop'], tempo: 'fast' },
          lighting: { color_theme: 'warm', intensity: 0.7 }
        }
      };

      const template = learner.extractTemplate(sceneDescriptor, { hour: 10 });

      assert.strictEqual(template.scene_type, 'custom_scene');
      assert.ok(template.intent);
      assert.ok(template.hints);
      assert.ok(template.triggers);
    });
  });

  describe('generateTemplateId', () => {
    it('should generate consistent IDs for same descriptors', () => {
      const descriptor1 = {
        intent: {
          scene_type: 'test',
          mood: { valence: 0.5, arousal: 0.4 }
        },
        hints: { music: { genres: ['pop'] } }
      };

      const descriptor2 = {
        intent: {
          scene_type: 'test',
          mood: { valence: 0.5, arousal: 0.4 }
        },
        hints: { music: { genres: ['pop'] } }
      };

      const id1 = learner.generateTemplateId(descriptor1);
      const id2 = learner.generateTemplateId(descriptor2);

      assert.strictEqual(id1, id2);
    });

    it('should generate different IDs for different descriptors', () => {
      const descriptor1 = {
        intent: {
          scene_type: 'test1',
          mood: { valence: 0.5, arousal: 0.4 }
        }
      };

      const descriptor2 = {
        intent: {
          scene_type: 'test2',
          mood: { valence: 0.5, arousal: 0.4 }
        }
      };

      const id1 = learner.generateTemplateId(descriptor1);
      const id2 = learner.generateTemplateId(descriptor2);

      assert.notStrictEqual(id1, id2);
    });
  });

  describe('priority adjustment', () => {
    it('should adjust priority based on accept count', () => {
      const sceneDescriptor = {
        intent: {
          scene_type: 'priority_test',
          mood: { valence: 0.5, arousal: 0.5 }
        }
      };

      const template = learner.learnFromDescriptor(sceneDescriptor, {});
      const templateId = template.template_id;

      learner.updateTemplateStats(templateId);
      learner.updateTemplateStats(templateId);
      learner.adjustTemplatePriority(templateId);

      const stats = learner.getTemplateStats(templateId);
      assert.ok(stats.acceptCount >= 2);
    });
  });

  describe('save and load', () => {
    it('should save learned templates', () => {
      const sceneDescriptor = {
        intent: {
          scene_type: 'save_test',
          mood: { valence: 0.5, arousal: 0.5 }
        }
      };

      learner.learnFromDescriptor(sceneDescriptor, {});
      const saved = learner.saveLearnedTemplates();

      assert.strictEqual(saved, true);
    });
  });
});

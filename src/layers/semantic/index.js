'use strict';

const { sceneRecognizer } = require('./recognizer');
const { queryRouter } = require('./router');
const { templateLibrary } = require('./templates/library');
const { validator } = require('./validator');
const { SceneDimensions, SceneTypes, IntentTypes } = require('./types');
const { eventBus } = require('../../shared/eventBus');

const SCHEMA_VERSION = '2.0';

class SemanticLayer {
  constructor(config = {}) {
    this.config = {
      enableLLM: config.enableLLM !== false,
      debug: config.debug || false,
      ...config
    };
    this.llmReasoner = null;
    this.currentDescriptor = null;
  }

  setLLMReasoner(reasoner) {
    this.llmReasoner = reasoner;
  }

  async process(perceptionOutput, options = {}) {
    const startTime = Date.now();

    const signals = perceptionOutput?.signals || {};
    const rawSignals = perceptionOutput?.raw_signals || [];

    const sceneVector = sceneRecognizer.recognize(signals);
    const changeDetection = sceneRecognizer.detectChange(sceneVector);

    let queryResult = null;
    if (signals.user_query) {
      queryResult = queryRouter.route(signals.user_query);
    }

    const matchedTemplate = templateLibrary.matchTemplate(sceneVector, {
      passengerCount: signals.vehicle?.passenger_count,
      weather: signals.environment?.weather,
      hasChildren: signals.internal_camera?.passengers?.children > 0,
      ...options.context
    });

    let descriptor;
    let source = 'template';

    if (this.config.enableLLM && this.llmReasoner) {
      try {
        const llmResult = await this.llmReasoner.process(
          { signals: rawSignals },
          { scene_vector: sceneVector },
          { matchedTemplate, ...options }
        );
        if (llmResult?.scene_descriptor) {
          descriptor = llmResult.scene_descriptor;
          source = 'llm';
        }
      } catch (err) {
        if (this.config.debug) {
          console.error('[SemanticLayer] LLM failed:', err.message);
        }
      }
    }

    if (!descriptor && matchedTemplate) {
      descriptor = this.buildDescriptorFromTemplate(matchedTemplate, sceneVector);
      source = 'template';
    }

    if (!descriptor) {
      descriptor = this.buildDefaultDescriptor(sceneVector, signals);
      source = 'fallback';
    }

    const validation = validator.validate(descriptor);
    if (!validation.valid) {
      if (this.config.debug) {
        console.warn('[SemanticLayer] Validation errors:', validation.errors);
      }
    }

    this.currentDescriptor = descriptor;

    eventBus.emit('scene.descriptor_generated', {
      descriptor,
      source,
      sceneVector,
      processing_time_ms: Date.now() - startTime
    });

    return {
      scene_descriptor: descriptor,
      meta: {
        source,
        template_id: matchedTemplate?.template_id,
        scene_type: sceneVector.scene_type,
        confidence: sceneVector.confidence,
        processing_time_ms: Date.now() - startTime,
        validation
      }
    };
  }

  buildDescriptorFromTemplate(template, sceneVector) {
    return {
      version: SCHEMA_VERSION,
      scene_id: `scene_${Date.now()}`,
      scene_type: template.scene_type,
      scene_name: template.name,
      scene_narrative: template.description,
      intent: { ...template.intent },
      hints: { ...template.hints },
      announcement: template.announcement_templates?.[0],
      meta: {
        source: 'template',
        template_id: template.template_id,
        created_at: new Date().toISOString(),
        confidence: sceneVector.confidence
      }
    };
  }

  buildDefaultDescriptor(sceneVector, signals) {
    const dims = sceneVector.dimensions || {};
    const energyLevel = dims.energy || 0.5;
    const isNight = dims.time_context < 0.3;

    return {
      version: SCHEMA_VERSION,
      scene_id: `scene_${Date.now()}`,
      scene_type: sceneVector.scene_type || 'morning_commute',
      scene_name: '自定义场景',
      scene_narrative: '根据当前环境生成的场景',
      intent: {
        mood: { valence: 0.5, arousal: energyLevel },
        energy_level: energyLevel,
        atmosphere: isNight ? 'calm' : 'neutral'
      },
      hints: {
        music: { genres: ['pop'], tempo: 'moderate' },
        lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.4 },
        audio: { preset: 'standard' }
      },
      announcement: '已为您调整场景',
      meta: {
        source: 'fallback',
        created_at: new Date().toISOString(),
        confidence: sceneVector.confidence
      }
    };
  }

  getCurrentDescriptor() {
    return this.currentDescriptor;
  }

  clear() {
    sceneRecognizer.clear();
    queryRouter.clear();
    this.currentDescriptor = null;
  }
}

const semanticLayer = new SemanticLayer();

module.exports = {
  SemanticLayer,
  semanticLayer,
  SceneDimensions,
  SceneTypes,
  IntentTypes
};

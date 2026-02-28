'use strict';

const { perceptionLayer, SignalSources, SignalCategories } = require('./layers/perception');
const { semanticLayer, SceneTypes, IntentTypes } = require('./layers/semantic');
const { effectsLayer, EngineTypes, EffectStatus } = require('./layers/effects');
const { eventBus, EventTypes } = require('./shared/eventBus');

class InCarAI {
  constructor(config = {}) {
    this.config = {
      enableLLM: config.enableLLM !== false,
      debug: config.debug || false,
      ...config
    };

    this.perceptionLayer = perceptionLayer;
    this.semanticLayer = semanticLayer;
    this.effectsLayer = effectsLayer;
  }

  async process(input) {
    const startTime = Date.now();
    const {
      signals = [],
      context = {},
      userQuery = null
    } = input;

    const perceptionOutput = this.perceptionLayer.processBatch(signals);

    if (userQuery) {
      perceptionOutput.signals.user_query = {
        text: userQuery,
        intent: 'creative',
        confidence: 0.95
      };
    }

    const semanticOutput = await this.semanticLayer.process(perceptionOutput, { context });

    const effectsOutput = await this.effectsLayer.process(semanticOutput.scene_descriptor);

    return {
      perception: perceptionOutput,
      semantic: semanticOutput,
      effects: effectsOutput,
      meta: {
        total_processing_time_ms: Date.now() - startTime,
        scene_id: semanticOutput.scene_descriptor?.scene_id
      }
    };
  }

  async processSignalsOnly(signals) {
    return this.perceptionLayer.processBatch(signals);
  }

  async processDescriptorOnly(descriptor) {
    return this.effectsLayer.process(descriptor);
  }

  getStatus() {
    return {
      perception: {
        history_size: this.perceptionLayer.outputHistory.length
      },
      semantic: {
        current_descriptor: this.semanticLayer.currentDescriptor?.scene_id || null
      },
      effects: {
        history_size: this.effectsLayer.getHistory().length
      }
    };
  }

  clear() {
    this.perceptionLayer.clear();
    this.semanticLayer.clear();
    this.effectsLayer.clear();
  }
}

const inCarAI = new InCarAI();

module.exports = {
  InCarAI,
  inCarAI,
  SignalSources,
  SignalCategories,
  SceneTypes,
  IntentTypes,
  EngineTypes,
  EffectStatus,
  eventBus,
  EventTypes,
  perceptionLayer,
  semanticLayer,
  effectsLayer
};

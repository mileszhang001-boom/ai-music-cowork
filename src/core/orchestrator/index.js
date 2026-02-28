'use strict';

/**
 * @fileoverview Orchestrator - 编排协调器
 * @description 负责协调各引擎的执行时序和动作编排，支持快慢双通道融合
 */

const { eventBus, EventTypes } = require('../eventBus');
const { templateLibrary } = require('../templateLibrary');

const OrchestratorState = {
  IDLE: 'idle',
  PROCESSING: 'processing',
  EXECUTING: 'executing',
  WAITING_FEEDBACK: 'waiting_feedback',
  FAST_TRACK_DONE: 'fast_track_done',
  SLOW_TRACK_PENDING: 'slow_track_pending'
};

const EngineTypes = {
  CONTENT: 'content',
  LIGHTING: 'lighting',
  AUDIO: 'audio'
};

const TrackMode = {
  FAST_ONLY: 'fast_only',
  SLOW_ONLY: 'slow_only',
  DUAL: 'dual'
};

class Orchestrator {
  constructor(config = {}) {
    this.config = {
      trackMode: TrackMode.DUAL,
      slowTrackTimeout: 30000,
      enableLearning: true,
      ...config
    };
    
    this.state = OrchestratorState.IDLE;
    this.engines = new Map();
    this.actionQueue = [];
    this.currentSceneDescriptor = null;
    this.fastTrackDescriptor = null;
    this.slowTrackDescriptor = null;
    this.executionHistory = [];
    this.maxHistorySize = config.maxHistorySize || 100;
    this.pendingSlowTrack = null;
    this.layer3 = null;
    
    this.setupEventListeners();
  }

  setupEventListeners() {
    eventBus.on(EventTypes.SCENE_CHANGED, (data) => {
      this.onSceneChanged(data);
    });

    eventBus.on(EventTypes.INTENT_GENERATED, (data) => {
      this.onIntentGenerated(data);
    });

    eventBus.on(EventTypes.FEEDBACK_REPORTED, (data) => {
      this.onFeedbackReported(data);
    });
  }

  setLayer3(layer3) {
    this.layer3 = layer3;
  }

  registerEngine(type, engine) {
    this.engines.set(type, engine);
  }

  getEngine(type) {
    return this.engines.get(type) || null;
  }

  onSceneChanged(data) {
    this.state = OrchestratorState.PROCESSING;
    eventBus.emit(EventTypes.ORCHESTRATOR_COMMAND, {
      command: 'scene_transition_start',
      data: data
    });
  }

  onIntentGenerated(data) {
    const { sceneDescriptor, source } = data;
    
    if (source === 'template' || source === 'fast_track') {
      this.fastTrackDescriptor = sceneDescriptor;
      this.currentSceneDescriptor = sceneDescriptor;
      this.executeScene(sceneDescriptor, { track: 'fast' });
    } else if (source === 'llm' || source === 'slow_track') {
      this.slowTrackDescriptor = sceneDescriptor;
      this.handleSlowTrackResult(sceneDescriptor);
    } else {
      this.currentSceneDescriptor = sceneDescriptor;
      this.executeScene(sceneDescriptor);
    }
  }

  async executeDualTrack(layer1Output, layer2Output, context = {}) {
    const executionId = `exec_${Date.now()}`;
    const startTime = Date.now();
    
    const matchedTemplate = this.matchTemplate(layer2Output?.scene_vector, context);
    
    if (matchedTemplate) {
      const fastDescriptor = this.buildDescriptorFromTemplate(matchedTemplate);
      this.fastTrackDescriptor = fastDescriptor;
      this.currentSceneDescriptor = fastDescriptor;
      
      console.log('[Orchestrator] Fast track: using template', matchedTemplate.template_id);
      
      await this.executeScene(fastDescriptor, { track: 'fast' });
      
      this.state = OrchestratorState.FAST_TRACK_DONE;
    }
    
    if (this.config.trackMode !== TrackMode.FAST_ONLY && this.layer3) {
      this.state = OrchestratorState.SLOW_TRACK_PENDING;
      
      this.pendingSlowTrack = {
        executionId,
        startTime,
        layer1Output,
        layer2Output,
        context
      };
      
      try {
        const slowResult = await Promise.race([
          this.layer3.process(layer1Output, layer2Output, {
            ...context,
            matchedTemplate
          }),
          this.createTimeoutPromise(this.config.slowTrackTimeout)
        ]);
        
        if (slowResult && slowResult.scene_descriptor) {
          this.slowTrackDescriptor = slowResult.scene_descriptor;
          await this.handleSlowTrackResult(slowResult.scene_descriptor, executionId);
        }
      } catch (error) {
        console.error('[Orchestrator] Slow track failed:', error.message);
      }
    }
    
    return {
      executionId,
      fastDescriptor: this.fastTrackDescriptor,
      slowDescriptor: this.slowTrackDescriptor,
      duration: Date.now() - startTime
    };
  }

  matchTemplate(sceneVector, context) {
    if (!sceneVector) return null;
    return templateLibrary.matchTemplate(sceneVector, context);
  }

  buildDescriptorFromTemplate(template) {
    return {
      version: '2.0',
      scene_id: `scene_${Date.now()}`,
      scene_type: template.scene_type,
      scene_name: template.name,
      scene_narrative: template.description,
      intent: template.intent,
      hints: template.hints,
      announcement: template.announcement_templates?.[0],
      meta: {
        source: 'template',
        template_id: template.template_id,
        created_at: new Date().toISOString()
      }
    };
  }

  async handleSlowTrackResult(slowDescriptor, executionId = null) {
    if (!slowDescriptor) return;
    
    const diff = this.computeDiff(this.fastTrackDescriptor, slowDescriptor);
    
    if (diff.hasChanges) {
      console.log('[Orchestrator] Slow track: applying incremental update');
      
      await this.applyIncrementalUpdate(diff, slowDescriptor);
      
      this.currentSceneDescriptor = slowDescriptor;
      
      if (this.config.enableLearning && executionId) {
        this.triggerLearning(executionId, slowDescriptor);
      }
      
      eventBus.emit(EventTypes.SCENE_EXECUTED, {
        scene_id: slowDescriptor.scene_id,
        source: 'slow_track',
        diff: diff.changes,
        announcement: slowDescriptor.announcement
      });
    } else {
      console.log('[Orchestrator] Slow track: no changes needed');
    }
    
    this.state = OrchestratorState.WAITING_FEEDBACK;
  }

  computeDiff(fastDescriptor, slowDescriptor) {
    if (!fastDescriptor) {
      return { hasChanges: true, changes: { type: 'full_replace' } };
    }
    
    const changes = {};
    let hasChanges = false;
    
    if (fastDescriptor.intent?.atmosphere !== slowDescriptor.intent?.atmosphere) {
      changes.atmosphere = {
        from: fastDescriptor.intent?.atmosphere,
        to: slowDescriptor.intent?.atmosphere
      };
      hasChanges = true;
    }
    
    if (Math.abs((fastDescriptor.intent?.energy_level || 0) - (slowDescriptor.intent?.energy_level || 0)) > 0.1) {
      changes.energy_level = {
        from: fastDescriptor.intent?.energy_level,
        to: slowDescriptor.intent?.energy_level
      };
      hasChanges = true;
    }
    
    const fastGenres = JSON.stringify(fastDescriptor.hints?.music?.genres || []);
    const slowGenres = JSON.stringify(slowDescriptor.hints?.music?.genres || []);
    if (fastGenres !== slowGenres) {
      changes.music_genres = {
        from: fastDescriptor.hints?.music?.genres,
        to: slowDescriptor.hints?.music?.genres
      };
      hasChanges = true;
    }
    
    if (fastDescriptor.hints?.lighting?.color_theme !== slowDescriptor.hints?.lighting?.color_theme) {
      changes.lighting_theme = {
        from: fastDescriptor.hints?.lighting?.color_theme,
        to: slowDescriptor.hints?.lighting?.color_theme
      };
      hasChanges = true;
    }
    
    if (fastDescriptor.hints?.audio?.preset !== slowDescriptor.hints?.audio?.preset) {
      changes.audio_preset = {
        from: fastDescriptor.hints?.audio?.preset,
        to: slowDescriptor.hints?.audio?.preset
      };
      hasChanges = true;
    }
    
    return { hasChanges, changes };
  }

  async applyIncrementalUpdate(diff, slowDescriptor) {
    const { changes } = diff;
    
    if (changes.music_genres || changes.energy_level) {
      const contentEngine = this.getEngine(EngineTypes.CONTENT);
      if (contentEngine) {
        await contentEngine.execute('curate_playlist', {
          hints: slowDescriptor.hints?.music,
          constraints: slowDescriptor.intent?.constraints,
          transition: { type: 'crossfade', duration_ms: 3000 }
        });
      }
    }
    
    if (changes.lighting_theme || changes.atmosphere) {
      const lightingEngine = this.getEngine(EngineTypes.LIGHTING);
      if (lightingEngine) {
        await lightingEngine.execute('apply_theme', {
          theme: slowDescriptor.hints?.lighting?.color_theme,
          pattern: slowDescriptor.hints?.lighting?.pattern,
          intensity: slowDescriptor.hints?.lighting?.intensity,
          transition: { type: 'fade', duration_ms: 2000 }
        });
      }
    }
    
    if (changes.audio_preset) {
      const audioEngine = this.getEngine(EngineTypes.AUDIO);
      if (audioEngine) {
        await audioEngine.execute('apply_preset', {
          preset: slowDescriptor.hints?.audio?.preset,
          transition: { type: 'fade', duration_ms: 1000 }
        });
      }
    }
  }

  triggerLearning(executionId, sceneDescriptor) {
    if (!this.config.enableLearning) return;
    
    const context = this.pendingSlowTrack?.context || {};
    templateLibrary.recordExecution(executionId, sceneDescriptor, context);
  }

  createTimeoutPromise(timeout) {
    return new Promise((_, reject) => {
      setTimeout(() => reject(new Error('Slow track timeout')), timeout);
    });
  }

  async executeScene(sceneDescriptor, options = {}) {
    this.state = OrchestratorState.EXECUTING;
    const { intent, hints } = sceneDescriptor;
    const track = options.track || 'unknown';

    const actions = this.planActions(intent, hints);
    
    for (const action of actions) {
      await this.executeAction(action);
    }

    this.executionHistory.push({
      scene_id: sceneDescriptor.scene_id,
      timestamp: Date.now(),
      actions: actions,
      track: track
    });

    if (this.executionHistory.length > this.maxHistorySize) {
      this.executionHistory.shift();
    }

    if (track === 'fast') {
      this.state = OrchestratorState.FAST_TRACK_DONE;
    } else {
      this.state = OrchestratorState.WAITING_FEEDBACK;
    }
    
    eventBus.emit(EventTypes.SCENE_EXECUTED, {
      scene_id: sceneDescriptor.scene_id,
      source: track,
      announcement: sceneDescriptor.announcement
    });
  }

  planActions(intent, hints) {
    const actions = [];
    const transition = intent.transition || { type: 'fade', duration_ms: 2000 };

    if (hints?.audio) {
      actions.push({
        engine: EngineTypes.AUDIO,
        action: 'apply_preset',
        params: {
          preset: hints.audio.preset,
          transition: transition
        },
        priority: 1
      });
    }

    if (hints?.lighting) {
      actions.push({
        engine: EngineTypes.LIGHTING,
        action: 'apply_theme',
        params: {
          theme: hints.lighting.color_theme,
          pattern: hints.lighting.pattern,
          intensity: hints.lighting.intensity,
          transition: transition
        },
        priority: 2
      });
    }

    if (hints?.music) {
      actions.push({
        engine: EngineTypes.CONTENT,
        action: 'curate_playlist',
        params: {
          hints: hints.music,
          constraints: intent.constraints,
          transition: transition
        },
        priority: 3
      });
    }

    return actions.sort((a, b) => a.priority - b.priority);
  }

  async executeAction(action) {
    const engine = this.getEngine(action.engine);
    
    if (!engine) {
      console.warn(`Orchestrator: Engine ${action.engine} not registered`);
      return;
    }

    try {
      const result = await engine.execute(action.action, action.params);
      
      eventBus.emit(EventTypes.ENGINE_ACTION, {
        engine: action.engine,
        action: action.action,
        result: result,
        timestamp: Date.now()
      });

      return result;
    } catch (error) {
      console.error(`Orchestrator: Engine ${action.engine} action failed:`, error);
      throw error;
    }
  }

  onFeedbackReported(feedback) {
    if (this.state === OrchestratorState.WAITING_FEEDBACK) {
      if (this.config.enableLearning && this.pendingSlowTrack?.executionId) {
        templateLibrary.recordFeedback(
          this.pendingSlowTrack.executionId,
          feedback.action,
          feedback.data
        );
      }
      
      this.state = OrchestratorState.IDLE;
      this.pendingSlowTrack = null;
    }
  }

  recordUserFeedback(action, data = {}) {
    if (this.pendingSlowTrack?.executionId) {
      templateLibrary.recordFeedback(
        this.pendingSlowTrack.executionId,
        action,
        data
      );
    }
  }

  getStatus() {
    return {
      state: this.state,
      current_scene: this.currentSceneDescriptor?.scene_id || null,
      fast_track_scene: this.fastTrackDescriptor?.scene_id || null,
      slow_track_scene: this.slowTrackDescriptor?.scene_id || null,
      registered_engines: Array.from(this.engines.keys()),
      pending_actions: this.actionQueue.length,
      track_mode: this.config.trackMode,
      pending_slow_track: this.pendingSlowTrack !== null
    };
  }

  getHistory(limit = 10) {
    return this.executionHistory.slice(-limit);
  }

  reset() {
    this.state = OrchestratorState.IDLE;
    this.actionQueue = [];
    this.currentSceneDescriptor = null;
    this.fastTrackDescriptor = null;
    this.slowTrackDescriptor = null;
    this.pendingSlowTrack = null;
  }
}

const orchestrator = new Orchestrator();

module.exports = {
  Orchestrator,
  orchestrator,
  OrchestratorState,
  EngineTypes,
  TrackMode
};

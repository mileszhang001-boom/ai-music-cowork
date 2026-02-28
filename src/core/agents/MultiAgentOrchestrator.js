'use strict';

const { MainAgent } = require('./main/MainAgent');
const { ContentAgent, LightingAgent, AudioAgent } = require('./sub');
const { AgentMessage, MessageTypes, TaskStatus } = require('./protocol/messages');
const { AgentMetrics } = require('./metrics/collector');

const EffectStatus = {
  PENDING: 'pending',
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed'
};

class MultiAgentOrchestrator {
  constructor(config = {}) {
    this.config = {
      enableLLM: config.enableLLM !== false,
      debug: config.debug || false,
      ...config
    };

    this.mainAgent = null;
    this.contentAgent = null;
    this.lightingAgent = null;
    this.audioAgent = null;
    
    this.metrics = new AgentMetrics(config.metricsConfig);
    
    this.eventHandlers = new Map();
    this.pendingRequests = new Map();
  }

  initialize(config = {}) {
    this.contentAgent = new ContentAgent({
      name: 'ContentAgent',
      mainAgentId: 'MainAgent',
      debug: this.config.debug,
      contentEngine: config.contentEngine
    });

    this.lightingAgent = new LightingAgent({
      name: 'LightingAgent',
      mainAgentId: 'MainAgent',
      debug: this.config.debug,
      lightingEngine: config.lightingEngine
    });

    this.audioAgent = new AudioAgent({
      name: 'AudioAgent',
      mainAgentId: 'MainAgent',
      debug: this.config.debug,
      audioEngine: config.audioEngine
    });

    this.mainAgent = new MainAgent({
      name: 'MainAgent',
      debug: this.config.debug,
      llmClient: config.llmClient,
      ruleEngine: config.ruleEngine,
      contentAgent: this.contentAgent,
      lightingAgent: this.lightingAgent,
      audioAgent: this.audioAgent,
      metricsConfig: config.metricsConfig
    });

    if (this.config.debug) {
      console.log('[MultiAgentOrchestrator] Initialized with 3 sub-agents');
    }

    return this;
  }

  async process(descriptor) {
    const startTime = Date.now();

    if (!descriptor) {
      return this.buildErrorOutput('No scene descriptor provided');
    }

    try {
      const result = await this.mainAgent.processSceneDescriptor(descriptor);

      const output = {
        version: '2.0',
        scene_id: descriptor.scene_id || `scene_${Date.now()}`,
        commands: this.buildCommands(result.results),
        execution_report: {
          status: result.success ? EffectStatus.COMPLETED : EffectStatus.FAILED,
          timestamp: new Date().toISOString(),
          execution_time_ms: Date.now() - startTime,
          details: {
            content: result.results.find(r => r.agent === 'content')?.success ? 'success' : 'skipped',
            lighting: result.results.find(r => r.agent === 'lighting')?.success ? 'success' : 'skipped',
            audio: result.results.find(r => r.agent === 'audio')?.success ? 'success' : 'skipped'
          },
          verification: result.verification
        },
        announcement: descriptor.announcement,
        _meta: {
          agent_architecture: 'multi-agent',
          processing_time_ms: Date.now() - startTime
        }
      };

      this.emit('output_generated', output);

      return output;

    } catch (error) {
      if (this.config.debug) {
        console.error('[MultiAgentOrchestrator] Execution error:', error.message);
      }
      return this.buildErrorOutput(error.message, descriptor.scene_id);
    }
  }

  buildCommands(results) {
    const commands = [];

    const contentResult = results.find(r => r.agent === 'content');
    if (contentResult?.success) {
      commands.push({
        type: 'content',
        action: 'play_playlist',
        params: contentResult.result?.playlist || []
      });
    }

    const lightingResult = results.find(r => r.agent === 'lighting');
    if (lightingResult?.success) {
      commands.push({
        type: 'lighting',
        action: 'play_effect',
        params: lightingResult.result?.commands || []
      });
    }

    const audioResult = results.find(r => r.agent === 'audio');
    if (audioResult?.success) {
      commands.push({
        type: 'audio',
        action: 'apply_preset',
        params: audioResult.result?.preset || {}
      });
    }

    return commands;
  }

  buildErrorOutput(error, sceneId = null) {
    return {
      version: '2.0',
      scene_id: sceneId || `error_${Date.now()}`,
      commands: [],
      execution_report: {
        status: EffectStatus.FAILED,
        timestamp: new Date().toISOString(),
        execution_time_ms: 0,
        error: error
      },
      _meta: {
        agent_architecture: 'multi-agent',
        error: true
      }
    };
  }

  on(event, handler) {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, []);
    }
    this.eventHandlers.get(event).push(handler);
  }

  emit(event, data) {
    const handlers = this.eventHandlers.get(event) || [];
    handlers.forEach(handler => {
      try {
        handler(data);
      } catch (error) {
        console.error(`[MultiAgentOrchestrator] Event handler error:`, error);
      }
    });
  }

  getMetrics() {
    return this.mainAgent?.getMetricsSummary() || {};
  }

  getStatus() {
    return {
      main: this.mainAgent ? 'ready' : 'not_initialized',
      content: this.contentAgent ? 'ready' : 'not_initialized',
      lighting: this.lightingAgent ? 'ready' : 'not_initialized',
      audio: this.audioAgent ? 'ready' : 'not_initialized'
    };
  }

  clear() {
    if (this.contentAgent) this.contentAgent.destroy();
    if (this.lightingAgent) this.lightingAgent.destroy();
    if (this.audioAgent) this.audioAgent.destroy();
    
    this.contentAgent = null;
    this.lightingAgent = null;
    this.audioAgent = null;
    this.mainAgent = null;
  }
}

const multiAgentOrchestrator = new MultiAgentOrchestrator();

module.exports = {
  MultiAgentOrchestrator,
  multiAgentOrchestrator,
  EffectStatus
};

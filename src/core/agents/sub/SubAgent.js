'use strict';

const { ReActAgent, ActionTypes } = require('../core/ReActAgent');
const { AgentMessage, MessageTypes, TaskStatus } = require('../protocol/messages');
const { AgentMetrics } = require('../metrics/collector');

class SubAgent extends ReActAgent {
  constructor(config = {}) {
    super({
      name: config.name || 'SubAgent',
      maxIterations: config.maxIterations || 3,
      tools: config.tools || {},
      llmClient: config.llmClient,
      ruleEngine: config.ruleEngine,
      debug: config.debug
    });

    this.agentType = config.agentType || 'unknown';
    this.mainAgentId = config.mainAgentId || 'main';
    this.taskId = null;
    this.currentTask = null;
    this.reportInterval = config.reportInterval || 60000;
    this.lastReportTime = Date.now();
    this.steps = [];
    this.currentStep = 0;
    this.maxSteps = config.maxSteps || 10;
    
    this.metrics = new AgentMetrics();
    this.reportCallback = config.reportCallback || null;
    
    this.reportTimer = null;
    this.startReportTimer();
  }

  setReportCallback(callback) {
    this.reportCallback = callback;
  }

  startReportTimer() {
    this.reportTimer = setInterval(() => {
      this.sendPeriodicReport();
    }, this.reportInterval);
  }

  stopReportTimer() {
    if (this.reportTimer) {
      clearInterval(this.reportTimer);
      this.reportTimer = null;
    }
  }

  async executeTask(task) {
    this.currentTask = task;
    this.taskId = task.task_id;
    this.context.task = task;
    this.steps = [];
    this.currentStep = 0;
    this.setContext({
      agentType: this.agentType,
      taskParams: task.params
    });

    const startTime = Date.now();
    let result;
    let success = false;

    try {
      result = await this.run(task.params);
      
      this.updateStep(this.buildStepResult('completed', result));
      success = true;
      
      this.sendResultSubmit(result);
      
      this.metrics.recordTaskExecution(
        this.taskId,
        task.type,
        this.name,
        1,
        success,
        Date.now() - startTime
      );
      
    } catch (error) {
      result = { error: error.message };
      this.sendErrorReport(error);
      
      this.metrics.recordTaskExecution(
        this.taskId,
        task.type,
        this.name,
        1,
        false,
        Date.now() - startTime
      );
    }

    return {
      success,
      result,
      task_id: this.taskId,
      agent: this.name,
      execution_time_ms: Date.now() - startTime
    };
  }

  updateStep(stepResult) {
    this.steps.push({
      step: this.currentStep,
      ...stepResult,
      timestamp: Date.now()
    });
    this.currentStep++;

    if (this.shouldReportNow()) {
      this.sendPeriodicReport();
    }
  }

  shouldReportNow() {
    const now = Date.now();
    if (now - this.lastReportTime >= this.reportInterval) {
      this.lastReportTime = now;
      return true;
    }
    return false;
  }

  buildStepResult(status, data) {
    return {
      status,
      data,
      progress: Math.min(100, Math.round((this.currentStep / this.maxSteps) * 100))
    };
  }

  sendPeriodicReport() {
    if (!this.reportCallback) return;

    const report = {
      task_id: this.taskId,
      agent: this.name,
      agent_type: this.agentType,
      status: this.currentTask ? TaskStatus.RUNNING : TaskStatus.PENDING,
      progress: this.calculateProgress(),
      current_step: this.currentStep,
      total_steps: this.maxSteps,
      steps: this.steps.slice(-5),
      timestamp: Date.now()
    };

    const message = AgentMessage.createStatusReport(this.name, this.mainAgentId, report);
    this.reportCallback(message);
  }

  sendResultSubmit(result) {
    if (!this.reportCallback) return;

    const message = AgentMessage.createResultSubmit(
      this.name,
      this.mainAgentId,
      {
        task_id: this.taskId,
        result,
        execution_time_ms: Date.now() - (this.context.startTime || Date.now())
      }
    );
    this.reportCallback(message);
  }

  sendErrorReport(error) {
    if (!this.reportCallback) return;

    const message = AgentMessage.createErrorReport(
      this.name,
      this.mainAgentId,
      {
        task_id: this.taskId,
        error: error.message,
        stack: error.stack,
        timestamp: Date.now()
      }
    );
    this.reportCallback(message);
  }

  sendHeartbeat() {
    if (!this.reportCallback) return;

    const message = AgentMessage.createHeartbeat(this.name, {
      agent: this.name,
      agent_type: this.agentType,
      status: TaskStatus.RUNNING,
      task_id: this.taskId,
      timestamp: Date.now()
    });
    this.reportCallback(message);
  }

  calculateProgress() {
    if (!this.currentTask) return 0;
    
    const stepProgress = (this.currentStep / this.maxSteps) * 70;
    const executionProgress = this.steps.length > 0 ? 
      this.steps.filter(s => s.status === 'completed').length / this.steps.length * 30 : 0;
    
    return Math.min(100, Math.round(stepProgress + executionProgress));
  }

  async smallStepExecute(action, params) {
    const stepResult = {
      action,
      params,
      startTime: Date.now()
    };

    try {
      let result;
      
      switch (action) {
        case 'query_content':
          result = await this.queryContentEngine(params);
          break;
        case 'generate_playlist':
          result = await this.generatePlaylist(params);
          break;
        case 'control_lighting':
          result = await this.controlLighting(params);
          break;
        case 'generate_effect':
          result = await this.generateLightingEffect(params);
          break;
        case 'apply_audio_preset':
          result = await this.applyAudioPreset(params);
          break;
        case 'adjust_dsp':
          result = await this.adjustDSP(params);
          break;
        default:
          result = { error: `Unknown action: ${action}` };
      }

      stepResult.result = result;
      stepResult.success = true;
      
    } catch (error) {
      stepResult.result = { error: error.message };
      stepResult.success = false;
    }

    stepResult.duration_ms = Date.now() - stepResult.startTime;
    this.updateStep(stepResult);

    return stepResult;
  }

  async queryContentEngine(params) {
    return { query: params.query || {}, result: [] };
  }

  async generatePlaylist(params) {
    return { playlist: [], count: 0 };
  }

  async controlLighting(params) {
    return { commands: [] };
  }

  async generateLightingEffect(params) {
    return { effect: {} };
  }

  async applyAudioPreset(params) {
    return { preset: {} };
  }

  async adjustDSP(params) {
    return { dsp_params: {} };
  }

  clear() {
    super.clear();
    this.taskId = null;
    this.currentTask = null;
    this.steps = [];
    this.currentStep = 0;
  }

  destroy() {
    this.stopReportTimer();
    this.clear();
  }
}

module.exports = {
  SubAgent
};

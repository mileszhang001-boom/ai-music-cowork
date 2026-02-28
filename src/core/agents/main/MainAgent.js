'use strict';

const { ReActAgent, ActionTypes } = require('../core/ReActAgent');
const { AgentMessage, MessageTypes, TaskStatus, AgentTypes, TaskManager } = require('../protocol/messages');
const { AgentMetrics } = require('../metrics/collector');

class MainAgent extends ReActAgent {
  constructor(config = {}) {
    super({
      name: config.name || 'MainAgent',
      maxIterations: config.maxIterations || 3,
      tools: config.tools || {},
      llmClient: config.llmClient,
      ruleEngine: config.ruleEngine,
      debug: config.debug
    });

    this.contentAgent = config.contentAgent || null;
    this.lightingAgent = config.lightingAgent || null;
    this.audioAgent = config.audioAgent || null;
    
    this.taskManager = new TaskManager();
    this.metrics = new AgentMetrics(config.metricsConfig);
    
    this.targetAccuracy = config.targetAccuracy || 0.95;
    this.targetHitRate = config.targetHitRate || 0.90;
    this.targetFirstTimeSuccess = config.targetFirstTimeSuccess || 0.85;
    
    this.maxRetries = config.maxRetries || 3;
    this.currentSessionTasks = [];
  }

  setSubAgents(agents) {
    if (agents.content) this.contentAgent = agents.content;
    if (agents.lighting) this.lightingAgent = agents.lighting;
    if (agents.audio) this.audioAgent = agents.audio;
    
    this.registerSubAgentCallbacks();
  }

  registerSubAgentCallbacks() {
    const reportCallback = (message) => this.handleSubAgentMessage(message);
    
    if (this.contentAgent) {
      this.contentAgent.setReportCallback(reportCallback);
      this.contentAgent.mainAgentId = this.name;
    }
    if (this.lightingAgent) {
      this.lightingAgent.setReportCallback(reportCallback);
      this.lightingAgent.mainAgentId = this.name;
    }
    if (this.audioAgent) {
      this.audioAgent.setReportCallback(reportCallback);
      this.audioAgent.mainAgentId = this.name;
    }
  }

  async processSceneDescriptor(sceneDescriptor) {
    const taskId = `task_${Date.now()}`;
    const startTime = Date.now();

    this.setContext({
      sceneDescriptor,
      taskId,
      startTime
    });

    const analysis = await this.analyzeTask(sceneDescriptor);
    const shouldModify = await this.decideModification(analysis, sceneDescriptor);
    
    let finalDescriptor = sceneDescriptor;
    if (shouldModify.modify) {
      finalDescriptor = await this.modifyTask(analysis, sceneDescriptor, shouldModify.reason);
    }

    const tasks = this.distributeTasks(finalDescriptor);
    
    const results = await this.executeTasks(tasks);
    
    const verification = await this.verifyResults(results);
    
    const executionTime = Date.now() - startTime;
    this.metrics.recordAnalysis(taskId, analysis, finalDescriptor);
    this.metrics.recordResponseTime(taskId, executionTime);

    return this.buildFinalOutput(results, verification, {
      analysis,
      finalDescriptor,
      executionTime
    });
  }

  async analyzeTask(sceneDescriptor) {
    const intent = sceneDescriptor?.intent || {};
    const hints = sceneDescriptor?.hints || {};
    const constraints = sceneDescriptor?.constraints || {};

    this.setContext({
      intent,
      hints,
      constraints
    });

    if (this.llmClient) {
      try {
        const prompt = this.buildAnalysisPrompt(sceneDescriptor);
        const response = await this.llmClient.complete(prompt);
        return this.parseAnalysisResponse(response);
      } catch (error) {
        if (this.debug) {
          console.error('[MainAgent] LLM analysis error:', error.message);
        }
      }
    }

    return this.fallbackAnalysis(sceneDescriptor);
  }

  buildAnalysisPrompt(descriptor) {
    return `
You are the Main Agent for a vehicle cockpit AI entertainment system.

Analyze the following Scene Descriptor and extract task information:

Scene Descriptor:
${JSON.stringify(descriptor, null, 2)}

Extract:
1. Intent: What is the user's goal? (energy level, mood, etc.)
2. Hints: What preferences are suggested?
3. Constraints: What are the hard constraints?
4. Required Sub-Agents: Which of content/lighting/audio are needed?

Respond in JSON:
{
  "intent_summary": "...",
  "required_agents": ["content", "lighting", "audio"],
  "priority": "high|medium|low",
  "estimated_complexity": "simple|moderate|complex"
}
`;
  }

  parseAnalysisResponse(response) {
    try {
      const parsed = typeof response === 'string' ? JSON.parse(response) : response;
      return {
        intent_summary: parsed.intent_summary || 'unknown',
        required_agents: parsed.required_agents || ['content'],
        priority: parsed.priority || 'medium',
        complexity: parsed.estimated_complexity || 'simple'
      };
    } catch (e) {
      return this.fallbackAnalysis({});
    }
  }

  fallbackAnalysis(descriptor) {
    return {
      intent_summary: descriptor?.intent?.energy || 'medium',
      required_agents: ['content', 'lighting', 'audio'],
      priority: 'medium',
      complexity: 'simple'
    };
  }

  async decideModification(analysis, originalDescriptor) {
    const conflicts = this.detectConflicts(analysis, originalDescriptor);
    
    if (conflicts.length > 0) {
      return {
        modify: true,
        reason: conflicts.join('; ')
      };
    }

    if (this.ruleEngine) {
      try {
        const validation = this.ruleEngine.validate(originalDescriptor, {});
        if (!validation.passed) {
          return {
            modify: true,
            reason: 'Rule validation failed: ' + validation.violations.map(v => v.message).join('; ')
          };
        }
      } catch (error) {
        if (this.debug) {
          console.error('[MainAgent] Rule engine error:', error.message);
        }
      }
    }

    return { modify: false };
  }

  detectConflicts(analysis, descriptor) {
    const conflicts = [];
    const constraints = descriptor?.constraints || {};
    
    if (constraints.content_rating === 'R' && analysis.required_agents.includes('content')) {
      const hasChildren = this.context.passengerComposition?.includes('child');
      if (hasChildren) {
        conflicts.push('Content rating R conflicts with child passenger');
      }
    }

    return conflicts;
  }

  async modifyTask(analysis, descriptor, reason) {
    if (this.debug) {
      console.log('[MainAgent] Modifying task:', reason);
    }

    const modified = { ...descriptor };
    
    if (reason.includes('Content rating')) {
      modified.constraints = {
        ...modified.constraints,
        content_rating: 'PG'
      };
    }

    return modified;
  }

  distributeTasks(descriptor) {
    const tasks = [];
    
    const intent = descriptor?.intent || {};
    const hints = descriptor?.hints || {};
    const constraints = descriptor?.constraints || {};

    if (this.contentAgent) {
      tasks.push({
        agent: 'content',
        type: 'generate_content',
        params: { intent, hints, constraints }
      });
    }

    if (this.lightingAgent) {
      tasks.push({
        agent: 'lighting',
        type: 'generate_effect',
        params: { intent, hints, constraints }
      });
    }

    if (this.audioAgent) {
      tasks.push({
        agent: 'audio',
        type: 'apply_audio',
        params: { intent, hints, constraints }
      });
    }

    return tasks;
  }

  async executeTasks(tasks) {
    const results = [];
    
    for (const task of tasks) {
      const agent = this.getAgentByType(task.agent);
      if (!agent) {
        results.push({
          agent: task.agent,
          success: false,
          error: 'Agent not available'
        });
        continue;
      }

      const managedTask = this.taskManager.createTask(
        agent.name,
        task.type,
        task.params
      );

      try {
        const result = await agent.executeTask(managedTask);
        this.taskManager.updateTaskStatus(
          managedTask.task_id,
          result.success ? TaskStatus.COMPLETED : TaskStatus.FAILED,
          100,
          result
        );
        
        results.push({
          agent: task.agent,
          success: result.success,
          result: result.result,
          task_id: managedTask.task_id
        });
      } catch (error) {
        this.taskManager.updateTaskStatus(
          managedTask.task_id,
          TaskStatus.FAILED,
          0,
          null,
          error.message
        );
        
        results.push({
          agent: task.agent,
          success: false,
          error: error.message
        });
      }
    }

    return results;
  }

  getAgentByType(type) {
    switch (type) {
      case 'content': return this.contentAgent;
      case 'lighting': return this.lightingAgent;
      case 'audio': return this.audioAgent;
      default: return null;
    }
  }

  async verifyResults(results) {
    const verification = {
      passed: true,
      failed_agents: [],
      warnings: []
    };

    for (const result of results) {
      if (!result.success) {
        verification.passed = false;
        verification.failed_agents.push(result.agent);
        
        const retryResult = await this.retryTask(result.agent);
        if (!retryResult.success) {
          verification.warnings.push(`Agent ${result.agent} failed after retry`);
        }
      }
    }

    return verification;
  }

  async retryTask(agentType) {
    const agent = this.getAgentByType(agentType);
    if (!agent || !agent.currentTask) {
      return { success: false, error: 'No task to retry' };
    }

    try {
      const result = await agent.executeTask(agent.currentTask);
      return { success: result.success, result };
    } catch (error) {
      return { success: false, error: error.message };
    }
  }

  handleSubAgentMessage(message) {
    switch (message.type) {
      case MessageTypes.STATUS_REPORT:
        this.handleStatusReport(message);
        break;
      case MessageTypes.RESULT_SUBMIT:
        this.handleResultSubmit(message);
        break;
      case MessageTypes.ERROR_REPORT:
        this.handleErrorReport(message);
        break;
      case MessageTypes.HEARTBEAT:
        this.handleHeartbeat(message);
        break;
    }
  }

  handleStatusReport(message) {
    if (this.debug) {
      console.log(`[MainAgent] Status from ${message.sender}:`, message.payload);
    }
  }

  handleResultSubmit(message) {
    if (this.debug) {
      console.log(`[MainAgent] Result from ${message.sender}:`, message.payload.task_id);
    }
  }

  handleErrorReport(message) {
    console.error(`[MainAgent] Error from ${message.sender}:`, message.payload);
  }

  handleHeartbeat(message) {
    if (this.debug) {
      console.log(`[MainAgent] Heartbeat from ${message.sender}`);
    }
  }

  buildFinalOutput(results, verification, metadata) {
    return {
      success: verification.passed,
      results,
      verification,
      metadata: {
        ...metadata,
        task_id: metadata.taskId
      }
    };
  }

  getMetrics() {
    return this.metrics.getMetricsSummary();
  }

  getMetricsSummary() {
    const summary = this.getMetrics();
    return {
      analysis_accuracy: `${(summary.analysis_accuracy.accuracy * 100).toFixed(1)}%`,
      hit_rate: `${(summary.hit_rate.hit_rate * 100).toFixed(1)}%`,
      first_time_success: `${(summary.first_time_success.success_rate * 100).toFixed(1)}%`,
      avg_response_time: `${summary.response_time.avg.toFixed(0)}ms`,
      meets_targets: 
        summary.analysis_accuracy.accuracy >= this.targetAccuracy &&
        summary.hit_rate.hit_rate >= this.targetHitRate &&
        summary.first_time_success.success_rate >= this.targetFirstTimeSuccess
    };
  }
}

module.exports = {
  MainAgent
};

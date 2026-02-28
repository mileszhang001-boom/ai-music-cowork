'use strict';

const ActionTypes = {
  LLM: 'llm',
  RULE: 'rule',
  TOOL: 'tool',
  OBSERVATION: 'observation',
  FINAL: 'final'
};

class ReActStep {
  constructor(thought, action = null, actionInput = null, observation = null) {
    this.thought = thought;
    this.action = action;
    this.actionInput = actionInput;
    this.observation = observation;
    this.timestamp = Date.now();
  }

  toJSON() {
    return {
      thought: this.thought,
      action: this.action,
      actionInput: this.actionInput,
      observation: this.observation,
      timestamp: this.timestamp
    };
  }
}

class ReActAgent {
  constructor(config = {}) {
    this.name = config.name || 'ReActAgent';
    this.maxIterations = config.maxIterations || 3;
    this.tools = config.tools || {};
    this.llmClient = config.llmClient || null;
    this.ruleEngine = config.ruleEngine || null;
    this.debug = config.debug || false;
    this.history = [];
    this.context = {};
  }

  setLLMClient(client) {
    this.llmClient = client;
  }

  setRuleEngine(engine) {
    this.ruleEngine = engine;
  }

  registerTool(name, func) {
    this.tools[name] = func;
  }

  setContext(context) {
    this.context = { ...this.context, ...context };
  }

  async run(input) {
    this.history = [];
    this.context.input = input;
    this.context.startTime = Date.now();

    for (let iteration = 1; iteration <= this.maxIterations; iteration++) {
      if (this.debug) {
        console.log(`[${this.name}] Iteration ${iteration}/${this.maxIterations}`);
      }

      const step = await this.executeStep(iteration);
      this.history.push(step);

      if (this.shouldStop(step)) {
        break;
      }
    }

    return this.buildResult();
  }

  async executeStep(iteration) {
    const thought = await this.think(iteration);
    let action = null;
    let actionInput = null;
    let observation = null;

    if (thought.action) {
      action = thought.action;
      actionInput = thought.actionInput;
      observation = await this.act(action, actionInput);
    }

    return new ReActStep(thought.reasoning, action, actionInput, observation);
  }

  async think(iteration) {
    const prompt = this.buildThinkPrompt(iteration);
    
    if (this.llmClient) {
      try {
        const response = await this.llmClient.complete(prompt);
        return this.parseLLMResponse(response);
      } catch (error) {
        if (this.debug) {
          console.error(`[${this.name}] LLM error:`, error.message);
        }
      }
    }

    return this.fallbackThink(iteration);
  }

  buildThinkPrompt(iteration) {
    const historyText = this.history
      .map((step, i) => `Step ${i + 1}: Thought: ${step.thought}\nAction: ${step.action}\nObservation: ${step.observation}`)
      .join('\n\n');

    return `
You are a ${this.name}. Analyze the current state and decide what action to take.

Context:
${JSON.stringify(this.context, null, 2)}

History:
${historyText}

Current iteration: ${iteration}/${this.maxIterations}

Determine:
1. Reasoning (thought): What is your analysis of the current state?
2. Action: What action should be taken? (${Object.keys(ActionTypes).join(', ')})
3. Action Input: What input is needed for the action?

Respond in JSON format:
{
  "reasoning": "...",
  "action": "llm|rule|tool|final",
  "actionInput": {...}
}
`;
  }

  parseLLMResponse(response) {
    try {
      const parsed = typeof response === 'string' ? JSON.parse(response) : response;
      return {
        reasoning: parsed.reasoning || parsed.thought || 'No reasoning provided',
        action: parsed.action || ActionTypes.FINAL,
        actionInput: parsed.actionInput || {}
      };
    } catch (e) {
      return this.fallbackThink(1);
    }
  }

  fallbackThink(iteration) {
    if (iteration >= this.maxIterations) {
      return {
        reasoning: 'Maximum iterations reached, proceeding to final result',
        action: ActionTypes.FINAL,
        actionInput: {}
      };
    }

    return {
      reasoning: 'Continuing analysis',
      action: ActionTypes.FINAL,
      actionInput: {}
    };
  }

  async act(action, actionInput) {
    switch (action) {
      case ActionTypes.LLM:
        return await this.executeLLM(actionInput);
      case ActionTypes.RULE:
        return await this.executeRule(actionInput);
      case ActionTypes.TOOL:
        return await this.executeTool(actionInput);
      case ActionTypes.FINAL:
        return this.buildFinalResult();
      default:
        return { error: `Unknown action type: ${action}` };
    }
  }

  async executeLLM(input) {
    if (!this.llmClient) {
      return { error: 'LLM client not configured' };
    }

    try {
      const prompt = input.prompt || JSON.stringify(input);
      const response = await this.llmClient.complete(prompt);
      return { result: response };
    } catch (error) {
      return { error: error.message };
    }
  }

  async executeRule(input) {
    if (!this.ruleEngine) {
      return { error: 'Rule engine not configured' };
    }

    try {
      const result = this.ruleEngine.validate(input.descriptor, input.context || {});
      return result;
    } catch (error) {
      return { error: error.message };
    }
  }

  async executeTool(input) {
    const toolName = input.tool;
    const toolFunc = this.tools[toolName];

    if (!toolFunc) {
      return { error: `Tool not found: ${toolName}` };
    }

    try {
      const result = await toolFunc(input.params || {});
      return { result };
    } catch (error) {
      return { error: error.message };
    }
  }

  shouldStop(step) {
    return step.action === ActionTypes.FINAL || !step.action;
  }

  buildFinalResult() {
    return { done: true };
  }

  buildResult() {
    const finalStep = this.history.find(step => step.action === ActionTypes.FINAL);
    
    return {
      success: true,
      iterations: this.history.length,
      history: this.history.map(s => s.toJSON()),
      result: finalStep?.observation || this.context.input,
      context: this.context
    };
  }

  reflect() {
    const successSteps = this.history.filter(s => s.observation?.result).length;
    const totalSteps = this.history.length;
    
    return {
      successRate: totalSteps > 0 ? successSteps / totalSteps : 0,
      iterationsUsed: this.history.length,
      maxIterations: this.maxIterations,
      contextKeys: Object.keys(this.context)
    };
  }

  clear() {
    this.history = [];
    this.context = {};
  }
}

module.exports = {
  ReActAgent,
  ReActStep,
  ActionTypes
};

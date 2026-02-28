'use strict';

const MessageTypes = {
  TASK_DISTRIBUTE: 'TASK_DISTRIBUTE',
  TASK_ACK: 'TASK_ACK',
  STATUS_REPORT: 'STATUS_REPORT',
  RESULT_SUBMIT: 'RESULT_SUBMIT',
  ERROR_REPORT: 'ERROR_REPORT',
  HEARTBEAT: 'HEARTBEAT',
  RESULT_VERIFY: 'RESULT_VERIFY'
};

const TaskStatus = {
  PENDING: 'pending',
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed',
  CANCELLED: 'cancelled'
};

const AgentTypes = {
  MAIN: 'main',
  CONTENT: 'content',
  LIGHTING: 'lighting',
  AUDIO: 'audio'
};

class AgentMessage {
  constructor(type, sender, receiver, payload, options = {}) {
    this.message_id = options.message_id || `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    this.type = type;
    this.sender = sender;
    this.receiver = receiver;
    this.payload = payload;
    this.timestamp = options.timestamp || Date.now();
    this.correlation_id = options.correlation_id || null;
    this.reply_to = options.reply_to || null;
  }

  static createTaskDistribute(sender, receiver, task) {
    return new AgentMessage(
      MessageTypes.TASK_DISTRIBUTE,
      sender,
      receiver,
      task
    );
  }

  static createTaskAck(sender, receiver, taskId, status) {
    return new AgentMessage(
      MessageTypes.TASK_ACK,
      sender,
      receiver,
      { task_id: taskId, status }
    );
  }

  static createStatusReport(sender, receiver, status) {
    return new AgentMessage(
      MessageTypes.STATUS_REPORT,
      sender,
      receiver,
      status
    );
  }

  static createResultSubmit(sender, receiver, result) {
    return new AgentMessage(
      MessageTypes.RESULT_SUBMIT,
      sender,
      receiver,
      result
    );
  }

  static createErrorReport(sender, receiver, error) {
    return new AgentMessage(
      MessageTypes.ERROR_REPORT,
      sender,
      receiver,
      error
    );
  }

  static createHeartbeat(sender, agentStatus) {
    return new AgentMessage(
      MessageTypes.HEARTBEAT,
      sender,
      'main',
      agentStatus
    );
  }

  static createResultVerify(sender, receiver, verification) {
    return new AgentMessage(
      MessageTypes.RESULT_VERIFY,
      sender,
      receiver,
      verification
    );
  }

  toJSON() {
    return {
      message_id: this.message_id,
      type: this.type,
      sender: this.sender,
      receiver: this.receiver,
      payload: this.payload,
      timestamp: this.timestamp,
      correlation_id: this.correlation_id,
      reply_to: this.reply_to
    };
  }

  static fromJSON(json) {
    const msg = new AgentMessage(
      json.type,
      json.sender,
      json.receiver,
      json.payload,
      {
        message_id: json.message_id,
        timestamp: json.timestamp,
        correlation_id: json.correlation_id,
        reply_to: json.reply_to
      }
    );
    return msg;
  }
}

class MessageRouter {
  constructor() {
    this.handlers = new Map();
    this.messageHistory = [];
  }

  registerHandler(messageType, handler) {
    this.handlers.set(messageType, handler);
  }

  async route(message) {
    this.messageHistory.push(message);
    
    const handler = this.handlers.get(message.type);
    if (!handler) {
      console.warn(`No handler for message type: ${message.type}`);
      return null;
    }

    try {
      return await handler(message);
    } catch (error) {
      console.error(`Error handling message:`, error);
      return { error: error.message };
    }
  }

  getHistory(limit = 100) {
    return this.messageHistory.slice(-limit);
  }

  clearHistory() {
    this.messageHistory = [];
  }
}

class TaskManager {
  constructor() {
    this.tasks = new Map();
    this.taskQueue = [];
  }

  createTask(agentId, taskType, params) {
    const taskId = `task_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const task = {
      task_id: taskId,
      agent_id: agentId,
      type: taskType,
      params,
      status: TaskStatus.PENDING,
      progress: 0,
      result: null,
      error: null,
      created_at: Date.now(),
      updated_at: Date.now(),
      history: []
    };
    
    this.tasks.set(taskId, task);
    this.taskQueue.push(taskId);
    
    return task;
  }

  getTask(taskId) {
    return this.tasks.get(taskId);
  }

  updateTaskStatus(taskId, status, progress = null, result = null, error = null) {
    const task = this.tasks.get(taskId);
    if (!task) return null;

    task.status = status;
    task.updated_at = Date.now();
    
    if (progress !== null) task.progress = progress;
    if (result !== null) task.result = result;
    if (error !== null) task.error = error;
    
    task.history.push({
      status,
      progress: task.progress,
      timestamp: Date.now()
    });

    return task;
  }

  getTasksByAgent(agentId) {
    return Array.from(this.tasks.values()).filter(t => t.agent_id === agentId);
  }

  getTasksByStatus(status) {
    return Array.from(this.tasks.values()).filter(t => t.status === status);
  }

  getPendingTasks() {
    return this.taskQueue
      .map(id => this.tasks.get(id))
      .filter(t => t && t.status === TaskStatus.PENDING);
  }
}

module.exports = {
  MessageTypes,
  TaskStatus,
  AgentTypes,
  AgentMessage,
  MessageRouter,
  TaskManager
};

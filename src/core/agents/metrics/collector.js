'use strict';

class AgentMetrics {
  constructor(config = {}) {
    this.analysisResults = [];
    this.taskExecutions = [];
    this.responseTimes = [];
    this.windowSize = config.windowSize || 3600000;
    this.maxHistory = config.maxHistory || 10000;
  }

  recordAnalysis(taskId, analysis, finalResult) {
    const isAccurate = this.evaluateAccuracy(analysis, finalResult);
    this.analysisResults.push({
      task_id: taskId,
      analysis,
      final_result: finalResult,
      accurate: isAccurate,
      timestamp: Date.now()
    });
    this.trimHistory();
  }

  evaluateAccuracy(analysis, finalResult) {
    if (!analysis || !finalResult) return false;
    
    const analysisIntent = analysis.intent || {};
    const finalIntent = finalResult.intent || {};
    
    const intentMatch = JSON.stringify(analysisIntent) === JSON.stringify(finalIntent);
    const constraintsMatch = this.compareConstraints(analysis.constraints, finalResult.constraints);
    
    return intentMatch && constraintsMatch;
  }

  compareConstraints(a, b) {
    if (!a && !b) return true;
    if (!a || !b) return false;
    
    const aStr = JSON.stringify(a);
    const bStr = JSON.stringify(b);
    
    return aStr === bStr;
  }

  recordTaskExecution(taskId, taskType, subAgentId, executionCount, hitTarget, executionTime) {
    this.taskExecutions.push({
      task_id: taskId,
      task_type: taskType,
      sub_agent_id: subAgentId,
      execution_count: executionCount,
      hit_target: hitTarget,
      first_time_success: executionCount === 1 && hitTarget,
      execution_time_ms: executionTime,
      timestamp: Date.now()
    });
    this.trimHistory();
  }

  recordResponseTime(taskId, responseTime) {
    this.responseTimes.push({
      task_id: taskId,
      response_time_ms: responseTime,
      timestamp: Date.now()
    });
    this.trimHistory();
  }

  getAnalysisAccuracy(windowMs = null) {
    const results = this.filterByWindow(this.analysisResults, windowMs);
    if (results.length === 0) return { accuracy: 0, total: 0 };
    
    const accurate = results.filter(r => r.accurate).length;
    return {
      accuracy: accurate / results.length,
      total: results.length,
      accurate_count: accurate
    };
  }

  getHitRate(windowMs = null) {
    const executions = this.filterByWindow(this.taskExecutions, windowMs);
    if (executions.length === 0) return { hit_rate: 0, total: 0 };
    
    const hits = executions.filter(e => e.hit_target).length;
    return {
      hit_rate: hits / executions.length,
      total: executions.length,
      hit_count: hits
    };
  }

  getFirstTimeSuccessRate(windowMs = null) {
    const executions = this.filterByWindow(this.taskExecutions, windowMs);
    if (executions.length === 0) return { success_rate: 0, total: 0 };
    
    const successes = executions.filter(e => e.first_time_success).length;
    return {
      success_rate: successes / executions.length,
      total: executions.length,
      success_count: successes
    };
  }

  getResponseTimeStats(windowMs = null) {
    const times = this.filterByWindow(this.responseTimes, windowMs);
    if (times.length === 0) {
      return { avg: 0, min: 0, max: 0, total: 0 };
    }

    const responseTimes = times.map(t => t.response_time_ms);
    return {
      avg: responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length,
      min: Math.min(...responseTimes),
      max: Math.max(...responseTimes),
      total: responseTimes.length,
      p50: this.percentile(responseTimes, 50),
      p95: this.percentile(responseTimes, 95),
      p99: this.percentile(responseTimes, 99)
    };
  }

  getMetricsSummary(windowMs = null) {
    return {
      analysis_accuracy: this.getAnalysisAccuracy(windowMs),
      hit_rate: this.getHitRate(windowMs),
      first_time_success: this.getFirstTimeSuccessRate(windowMs),
      response_time: this.getResponseTimeStats(windowMs),
      window_ms: windowMs || 'all_time'
    };
  }

  filterByWindow(records, windowMs) {
    if (!windowMs) return records;
    
    const cutoff = Date.now() - windowMs;
    return records.filter(r => r.timestamp >= cutoff);
  }

  percentile(arr, p) {
    if (arr.length === 0) return 0;
    const sorted = [...arr].sort((a, b) => a - b);
    const index = Math.ceil((p / 100) * sorted.length) - 1;
    return sorted[Math.max(0, index)];
  }

  trimHistory() {
    if (this.analysisResults.length > this.maxHistory) {
      this.analysisResults = this.analysisResults.slice(-this.maxHistory);
    }
    if (this.taskExecutions.length > this.maxHistory) {
      this.taskExecutions = this.taskExecutions.slice(-this.maxHistory);
    }
    if (this.responseTimes.length > this.maxHistory) {
      this.responseTimes = this.responseTimes.slice(-this.maxHistory);
    }
  }

  clear() {
    this.analysisResults = [];
    this.taskExecutions = [];
    this.responseTimes = [];
  }
}

module.exports = {
  AgentMetrics
};

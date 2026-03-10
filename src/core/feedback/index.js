'use strict';

/**
 * @fileoverview Feedback Manager - 反馈报告管理器
 * @description 生成和回传反馈报告，支持用户行为分析和系统优化
 */

const { eventBus, EventTypes } = require('../eventBus');
const { templateLibrary } = require('../templateLibrary');

const FeedbackType = {
  POSITIVE: 'positive',
  NEGATIVE: 'negative',
  NEUTRAL: 'neutral'
};

const FeedbackAction = {
  SKIP: 'skip',
  LIKE: 'like',
  DISLIKE: 'dislike',
  VOLUME_CHANGE: 'volume_change',
  PAUSE: 'pause',
  RESUME: 'resume',
  SCENE_SWITCH: 'scene_switch',
  MANUAL_OVERRIDE: 'manual_override',
  ACCEPT: 'accept',
  TIMEOUT: 'timeout'
};

class FeedbackManager {
  constructor(config = {}) {
    this.config = {
      feedbackWindowMs: 30000,
      reportIntervalMs: 60000,
      maxReports: 100,
      enableAutoReport: true,
      ...config
    };
    
    this.feedbackHistory = [];
    this.pendingFeedback = new Map();
    this.sessionReports = [];
    this.engineReports = new Map();
    this.currentSession = null;
    
    this.setupEventListeners();
  }

  setupEventListeners() {
    eventBus.on(EventTypes.USER_FEEDBACK, (data) => {
      this.recordFeedback(data);
    });

    eventBus.on(EventTypes.ENGINE_ACTION, (data) => {
      this.onEngineAction(data);
    });

    eventBus.on(EventTypes.SCENE_EXECUTED, (data) => {
      this.onSceneExecuted(data);
    });

    eventBus.on(EventTypes.MUSIC_TRACK_CHANGED, (data) => {
      this.onTrackChanged(data);
    });
  }

  startSession(sessionId, context = {}) {
    this.currentSession = {
      id: sessionId,
      startTime: Date.now(),
      context,
      feedbackCount: 0,
      positiveCount: 0,
      negativeCount: 0
    };
    
    this.sessionReports.push(this.currentSession);
  }

  endSession() {
    if (this.currentSession) {
      this.currentSession.endTime = Date.now();
      this.currentSession.duration = this.currentSession.endTime - this.currentSession.startTime;
      
      const report = this.generateSessionReport();
      
      if (this.config.enableAutoReport) {
        this.submitReport(report);
      }
      
      this.currentSession = null;
    }
  }

  recordFeedback(data) {
    const feedback = {
      id: `feedback_${Date.now()}`,
      timestamp: Date.now(),
      sessionId: this.currentSession?.id,
      ...data
    };

    feedback.type = this.classifyFeedback(data.action);

    this.feedbackHistory.push(feedback);

    if (this.currentSession) {
      this.currentSession.feedbackCount++;
      if (feedback.type === FeedbackType.POSITIVE) {
        this.currentSession.positiveCount++;
      } else if (feedback.type === FeedbackType.NEGATIVE) {
        this.currentSession.negativeCount++;
      }
    }

    this.notifyTemplateLearner(feedback);

    eventBus.emit(EventTypes.FEEDBACK_REPORTED, {
      feedback_id: feedback.id,
      type: feedback.type,
      action: feedback.action
    });

    return feedback;
  }

  classifyFeedback(action) {
    const positiveActions = [FeedbackAction.LIKE, FeedbackAction.ACCEPT];
    const negativeActions = [FeedbackAction.SKIP, FeedbackAction.DISLIKE, FeedbackAction.SCENE_SWITCH];
    
    if (positiveActions.includes(action)) {
      return FeedbackType.POSITIVE;
    }
    if (negativeActions.includes(action)) {
      return FeedbackType.NEGATIVE;
    }
    return FeedbackType.NEUTRAL;
  }

  notifyTemplateLearner(feedback) {
    if (feedback.executionId && feedback.action) {
      templateLibrary.recordFeedback(feedback.executionId, feedback.action, feedback.data);
    }
  }

  onEngineAction(data) {
    if (!this.engineReports.has(data.engine)) {
      this.engineReports.set(data.engine, []);
    }
    
    this.engineReports.get(data.engine).push({
      timestamp: Date.now(),
      action: data.action,
      data: data.data
    });
  }

  onSceneExecuted(data) {
    const pending = {
      executionId: data.scene_id,
      timestamp: Date.now(),
      source: data.source,
      announcement: data.announcement
    };
    
    this.pendingFeedback.set(data.scene_id, pending);

    setTimeout(() => {
      this.checkPendingFeedback(data.scene_id);
    }, this.config.feedbackWindowMs);
  }

  onTrackChanged(data) {
    if (data.reason === 'user_next' || data.reason === 'user_previous') {
      this.recordFeedback({
        action: data.reason === 'user_next' ? FeedbackAction.SKIP : FeedbackAction.NEUTRAL,
        track_id: data.track?.track_id,
        reason: data.reason
      });
    }
  }

  checkPendingFeedback(executionId) {
    const pending = this.pendingFeedback.get(executionId);
    
    if (pending && !pending.feedbackReceived) {
      this.recordFeedback({
        executionId,
        action: FeedbackAction.TIMEOUT,
        type: FeedbackType.NEUTRAL,
        implicit: true
      });
    }
    
    this.pendingFeedback.delete(executionId);
  }

  markFeedbackReceived(executionId) {
    const pending = this.pendingFeedback.get(executionId);
    if (pending) {
      pending.feedbackReceived = true;
    }
  }

  generateSessionReport() {
    const session = this.currentSession;
    
    return {
      report_id: `report_${Date.now()}`,
      session_id: session?.id,
      generated_at: new Date().toISOString(),
      
      summary: {
        duration_ms: session?.duration || 0,
        total_feedback: session?.feedbackCount || 0,
        positive_rate: session?.feedbackCount > 0 
          ? (session.positiveCount / session.feedbackCount).toFixed(2) 
          : 0,
        negative_rate: session?.feedbackCount > 0 
          ? (session.negativeCount / session.feedbackCount).toFixed(2) 
          : 0
      },
      
      feedback_details: this.feedbackHistory.slice(-20),
      
      engine_reports: this.generateEngineReports(),
      
      recommendations: this.generateRecommendations()
    };
  }

  generateEngineReports() {
    const reports = {};
    
    for (const [engine, actions] of this.engineReports) {
      reports[engine] = {
        action_count: actions.length,
        last_action: actions[actions.length - 1]?.action,
        actions: actions.slice(-10)
      };
    }
    
    return reports;
  }

  generateRecommendations() {
    const recommendations = [];
    const recentFeedback = this.feedbackHistory.slice(-20);
    
    const skipCount = recentFeedback.filter(f => f.action === FeedbackAction.SKIP).length;
    if (skipCount > 5) {
      recommendations.push({
        type: 'content_tuning',
        message: '高频切歌行为，建议调整音乐推荐策略',
        priority: 'high'
      });
    }

    const volumeChanges = recentFeedback.filter(f => f.action === FeedbackAction.VOLUME_CHANGE).length;
    if (volumeChanges > 3) {
      recommendations.push({
        type: 'audio_tuning',
        message: '频繁调整音量，建议优化音量自适应算法',
        priority: 'medium'
      });
    }

    const sceneSwitches = recentFeedback.filter(f => f.action === FeedbackAction.SCENE_SWITCH).length;
    if (sceneSwitches > 2) {
      recommendations.push({
        type: 'scene_tuning',
        message: '多次手动切换场景，建议优化场景识别准确度',
        priority: 'high'
      });
    }

    return recommendations;
  }

  submitReport(report) {
    console.log('[FeedbackManager] Submitting report:', report.report_id);
    
    return {
      success: true,
      report_id: report.report_id,
      submitted_at: new Date().toISOString()
    };
  }

  getFeedbackStats() {
    const total = this.feedbackHistory.length;
    const positive = this.feedbackHistory.filter(f => f.type === FeedbackType.POSITIVE).length;
    const negative = this.feedbackHistory.filter(f => f.type === FeedbackType.NEGATIVE).length;
    
    return {
      total,
      positive,
      negative,
      neutral: total - positive - negative,
      positive_rate: total > 0 ? (positive / total).toFixed(2) : 0,
      negative_rate: total > 0 ? (negative / total).toFixed(2) : 0
    };
  }

  getRecentFeedback(limit = 10) {
    return this.feedbackHistory.slice(-limit);
  }

  clearHistory() {
    this.feedbackHistory = [];
    this.pendingFeedback.clear();
    this.engineReports.clear();
  }
}

const feedbackManager = new FeedbackManager();

module.exports = {
  FeedbackManager,
  feedbackManager,
  FeedbackType,
  FeedbackAction
};

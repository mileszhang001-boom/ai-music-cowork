'use strict';

const FeedbackTypes = {
  IMPLICIT: 'implicit',
  EXPLICIT: 'explicit'
};

const FeedbackActions = {
  TRACK_SKIPPED: 'track_skipped',
  TRACK_COMPLETED: 'track_completed',
  TRACK_LIKED: 'track_liked',
  TRACK_DISLIKED: 'track_disliked',
  TRACK_FAVORITED: 'track_favorited',
  VOLUME_ADJUSTED: 'volume_adjusted',
  LIGHTING_ADJUSTED: 'lighting_adjusted',
  AUDIO_PRESET_CHANGED: 'audio_preset_changed',
  SCENE_MANUAL_SWITCH: 'scene_manual_switch',
  VOICE_INTERRUPT: 'voice_interrupt',
  VOICE_PRAISE: 'voice_praise',
  VOICE_COMPLAINT: 'voice_complaint'
};

const SentimentScores = {
  [FeedbackActions.TRACK_SKIPPED]: -0.5,
  [FeedbackActions.TRACK_COMPLETED]: 0.8,
  [FeedbackActions.TRACK_LIKED]: 1.5,
  [FeedbackActions.TRACK_DISLIKED]: -1.5,
  [FeedbackActions.TRACK_FAVORITED]: 2.0,
  [FeedbackActions.VOLUME_ADJUSTED]: -0.2,
  [FeedbackActions.LIGHTING_ADJUSTED]: -0.3,
  [FeedbackActions.AUDIO_PRESET_CHANGED]: -0.2,
  [FeedbackActions.SCENE_MANUAL_SWITCH]: -0.5,
  [FeedbackActions.VOICE_INTERRUPT]: -1.0,
  [FeedbackActions.VOICE_PRAISE]: 1.0,
  [FeedbackActions.VOICE_COMPLAINT]: -1.5
};

class FeedbackCollector {
  constructor(config = {}) {
    this.config = {
      batchSize: config.batchSize || 10,
      flushInterval: config.flushInterval || 60000,
      ...config
    };
    this.feedbackBuffer = [];
    this.feedbackHistory = [];
    this.listeners = [];
    this.sessionId = null;
    this.sceneId = null;

    if (this.config.flushInterval > 0) {
      this._startFlushTimer();
    }
  }

  startSession(context = {}) {
    this.sessionId = `session_${Date.now()}`;
    this.sceneId = null;
    this.sessionContext = {
      ...context,
      started_at: new Date().toISOString()
    };
    return this.sessionId;
  }

  setScene(sceneId, sceneType) {
    this.sceneId = sceneId;
    this.currentSceneType = sceneType;
  }

  collect(action, data = {}) {
    const feedback = {
      feedback_id: `fb_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      session_id: this.sessionId,
      scene_id: this.sceneId,
      scene_type: this.currentSceneType,
      type: this._getFeedbackType(action),
      action,
      sentiment_score: SentimentScores[action] || 0,
      data,
      timestamp: new Date().toISOString(),
      context: {
        ...this.sessionContext,
        time_since_scene_start: this.sceneId ? this._getTimeSinceSceneStart() : null
      }
    };

    this.feedbackBuffer.push(feedback);
    this._notifyListeners(feedback);

    if (this.feedbackBuffer.length >= this.config.batchSize) {
      this.flush();
    }

    return feedback;
  }

  _getFeedbackType(action) {
    const implicitActions = [
      FeedbackActions.TRACK_SKIPPED,
      FeedbackActions.TRACK_COMPLETED,
      FeedbackActions.VOLUME_ADJUSTED,
      FeedbackActions.LIGHTING_ADJUSTED,
      FeedbackActions.AUDIO_PRESET_CHANGED,
      FeedbackActions.SCENE_MANUAL_SWITCH,
      FeedbackActions.VOICE_INTERRUPT
    ];

    return implicitActions.includes(action) ? FeedbackTypes.IMPLICIT : FeedbackTypes.EXPLICIT;
  }

  _getTimeSinceSceneStart() {
    if (!this.sceneStartTime) return null;
    return Date.now() - this.sceneStartTime;
  }

  collectTrackFeedback(action, track, context = {}) {
    return this.collect(action, {
      track_id: track.id || track.track_id,
      track_title: track.title,
      track_artist: track.artist,
      track_genres: track.genres,
      track_energy: track.energy,
      position_in_playlist: context.position_in_playlist,
      play_duration_sec: context.play_duration_sec,
      total_duration_sec: track.duration_sec
    });
  }

  collectLightingFeedback(action, adjustments, context = {}) {
    return this.collect(action, {
      previous_theme: adjustments.previous_theme,
      new_theme: adjustments.new_theme,
      previous_intensity: adjustments.previous_intensity,
      new_intensity: adjustments.new_intensity,
      previous_pattern: adjustments.previous_pattern,
      new_pattern: adjustments.new_pattern
    });
  }

  collectAudioFeedback(action, adjustments, context = {}) {
    return this.collect(action, {
      previous_preset: adjustments.previous_preset,
      new_preset: adjustments.new_preset,
      previous_volume: adjustments.previous_volume,
      new_volume: adjustments.new_volume,
      eq_changes: adjustments.eq_changes
    });
  }

  collectVoiceFeedback(action, transcript, context = {}) {
    return this.collect(action, {
      transcript,
      intent_detected: context.intent_detected,
      response_given: context.response_given
    });
  }

  addListener(callback) {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter(l => l !== callback);
    };
  }

  _notifyListeners(feedback) {
    for (const listener of this.listeners) {
      try {
        listener(feedback);
      } catch (err) {
        console.error('[FeedbackCollector] Listener error:', err);
      }
    }
  }

  flush() {
    if (this.feedbackBuffer.length === 0) return [];

    const batch = [...this.feedbackBuffer];
    this.feedbackBuffer = [];
    this.feedbackHistory.push(...batch);

    return batch;
  }

  _startFlushTimer() {
    this.flushTimer = setInterval(() => {
      this.flush();
    }, this.config.flushInterval);
  }

  stopFlushTimer() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }
  }

  generateReport(options = {}) {
    const { sessionId = this.sessionId, limit = 100 } = options;

    const relevantFeedback = this.feedbackHistory.filter(
      f => !sessionId || f.session_id === sessionId
    ).slice(-limit);

    const report = {
      session_id: sessionId,
      generated_at: new Date().toISOString(),
      total_feedback: relevantFeedback.length,
      summary: {
        by_action: {},
        by_type: { implicit: 0, explicit: 0 },
        average_sentiment: 0,
        negative_count: 0,
        positive_count: 0
      },
      insights: {
        skipped_tracks: [],
        liked_tracks: [],
        manual_adjustments: [],
        scene_satisfaction: {}
      },
      recommendations: []
    };

    let totalSentiment = 0;

    for (const feedback of relevantFeedback) {
      report.summary.by_action[feedback.action] = 
        (report.summary.by_action[feedback.action] || 0) + 1;
      report.summary.by_type[feedback.type]++;
      totalSentiment += feedback.sentiment_score;

      if (feedback.sentiment_score < 0) report.summary.negative_count++;
      if (feedback.sentiment_score > 0) report.summary.positive_count++;

      if (feedback.action === FeedbackActions.TRACK_SKIPPED) {
        report.insights.skipped_tracks.push({
          track_id: feedback.data.track_id,
          track_title: feedback.data.track_title,
          reason: 'skipped'
        });
      }

      if (feedback.action === FeedbackActions.TRACK_LIKED || 
          feedback.action === FeedbackActions.TRACK_FAVORITED) {
        report.insights.liked_tracks.push({
          track_id: feedback.data.track_id,
          track_title: feedback.data.track_title,
          action: feedback.action
        });
      }

      if (feedback.action === FeedbackActions.LIGHTING_ADJUSTED ||
          feedback.action === FeedbackActions.AUDIO_PRESET_CHANGED ||
          feedback.action === FeedbackActions.VOLUME_ADJUSTED) {
        report.insights.manual_adjustments.push({
          action: feedback.action,
          data: feedback.data
        });
      }

      if (feedback.scene_type) {
        if (!report.insights.scene_satisfaction[feedback.scene_type]) {
          report.insights.scene_satisfaction[feedback.scene_type] = {
            count: 0,
            total_sentiment: 0
          };
        }
        report.insights.scene_satisfaction[feedback.scene_type].count++;
        report.insights.scene_satisfaction[feedback.scene_type].total_sentiment += 
          feedback.sentiment_score;
      }
    }

    report.summary.average_sentiment = relevantFeedback.length > 0 
      ? totalSentiment / relevantFeedback.length 
      : 0;

    for (const [sceneType, data] of Object.entries(report.insights.scene_satisfaction)) {
      data.average_sentiment = data.count > 0 ? data.total_sentiment / data.count : 0;
    }

    if (report.insights.skipped_tracks.length > 3) {
      report.recommendations.push({
        type: 'music_avoid',
        message: '用户频繁跳过歌曲，建议调整音乐推荐策略',
        data: { skipped_count: report.insights.skipped_tracks.length }
      });
    }

    if (report.insights.manual_adjustments.length > 5) {
      report.recommendations.push({
        type: 'auto_adjustment_review',
        message: '用户频繁手动调整，建议优化自动调节算法',
        data: { adjustment_count: report.insights.manual_adjustments.length }
      });
    }

    if (report.summary.average_sentiment < -0.3) {
      report.recommendations.push({
        type: 'satisfaction_low',
        message: '用户满意度偏低，建议进行体验优化',
        data: { average_sentiment: report.summary.average_sentiment }
      });
    }

    return report;
  }

  getRecentFeedback(limit = 10) {
    return this.feedbackHistory.slice(-limit);
  }

  clear() {
    this.feedbackBuffer = [];
    this.feedbackHistory = [];
  }

  getStats() {
    return {
      buffer_size: this.feedbackBuffer.length,
      history_size: this.feedbackHistory.length,
      session_id: this.sessionId,
      current_scene_id: this.sceneId
    };
  }
}

const feedbackCollector = new FeedbackCollector();

module.exports = {
  FeedbackCollector,
  feedbackCollector,
  FeedbackTypes,
  FeedbackActions,
  SentimentScores
};

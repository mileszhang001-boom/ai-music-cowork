'use strict';

const MemoryTypes = {
  SHORT_TERM: 'short_term',
  MEDIUM_TERM: 'medium_term',
  LONG_TERM: 'long_term'
};

const MemoryCategories = {
  MUSIC_PREFERENCE: 'music_preference',
  SCENE_PREFERENCE: 'scene_preference',
  BEHAVIOR_PATTERN: 'behavior_pattern',
  FEEDBACK_HISTORY: 'feedback_history',
  CONTEXT_HISTORY: 'context_history'
};

const DefaultConfig = {
  shortTermTTL: 30 * 60 * 1000,
  mediumTermTTL: 7 * 24 * 60 * 60 * 1000,
  longTermTTL: 365 * 24 * 60 * 60 * 1000,
  maxShortTermEntries: 100,
  maxMediumTermEntries: 500,
  maxLongTermEntries: 1000
};

class MemorySystem {
  constructor(config = {}) {
    this.config = { ...DefaultConfig, ...config };
    this.memories = {
      [MemoryTypes.SHORT_TERM]: new Map(),
      [MemoryTypes.MEDIUM_TERM]: new Map(),
      [MemoryTypes.LONG_TERM]: new Map()
    };
    this.userProfile = null;
    this.sessionContext = {};
  }

  setUserProfile(profile) {
    this.userProfile = {
      ...profile,
      updated_at: new Date().toISOString()
    };
    this._store(MemoryTypes.LONG_TERM, 'user_profile', this.userProfile);
    return this.userProfile;
  }

  getUserProfile() {
    if (!this.userProfile) {
      const stored = this._retrieve(MemoryTypes.LONG_TERM, 'user_profile');
      if (stored) {
        this.userProfile = stored;
      }
    }
    return this.userProfile;
  }

  recordSession(context) {
    this.sessionContext = {
      session_id: `session_${Date.now()}`,
      started_at: new Date().toISOString(),
      ...context
    };
    return this.sessionContext;
  }

  recordPreference(category, data) {
    const key = `${category}_${Date.now()}`;
    const entry = {
      category,
      data,
      timestamp: new Date().toISOString(),
      session_id: this.sessionContext.session_id,
      weight: 1.0
    };

    this._store(MemoryTypes.SHORT_TERM, key, entry);
    this._aggregateToMediumTerm(category, entry);

    return entry;
  }

  recordMusicPreference(track, action, context = {}) {
    const preference = {
      track_id: track.id || track.track_id,
      title: track.title,
      artist: track.artist,
      genres: track.genres || [],
      action,
      context: {
        scene_type: context.scene_type,
        time_of_day: context.time_of_day,
        weather: context.weather,
        mood: context.mood
      }
    };

    const weight = this._calculateActionWeight(action);
    preference.weight = weight;

    return this.recordPreference(MemoryCategories.MUSIC_PREFERENCE, preference);
  }

  recordScenePreference(sceneType, feedback, context = {}) {
    const preference = {
      scene_type: sceneType,
      feedback,
      context: {
        time_of_day: context.time_of_day,
        weather: context.weather,
        passenger_count: context.passenger_count
      }
    };

    return this.recordPreference(MemoryCategories.SCENE_PREFERENCE, preference);
  }

  recordBehavior(action, details = {}) {
    const behavior = {
      action,
      details,
      timestamp: new Date().toISOString()
    };

    return this.recordPreference(MemoryCategories.BEHAVIOR_PATTERN, behavior);
  }

  _calculateActionWeight(action) {
    const weights = {
      'play': 1.0,
      'complete': 1.5,
      'skip': -0.5,
      'dislike': -2.0,
      'like': 2.0,
      'favorite': 3.0,
      'manual_adjust': -0.3,
      'voice_request': 1.0
    };
    return weights[action] || 0;
  }

  _aggregateToMediumTerm(category, entry) {
    const today = new Date().toISOString().split('T')[0];
    const aggregateKey = `${category}_${today}`;

    let aggregate = this._retrieve(MemoryTypes.MEDIUM_TERM, aggregateKey);

    if (!aggregate) {
      aggregate = {
        category,
        date: today,
        entries: [],
        summary: {}
      };
    }

    aggregate.entries.push(entry);
    aggregate.summary = this._summarizeEntries(aggregate.entries);
    aggregate.updated_at = new Date().toISOString();

    this._store(MemoryTypes.MEDIUM_TERM, aggregateKey, aggregate);
  }

  _summarizeEntries(entries) {
    const summary = {
      total_count: entries.length,
      positive_count: 0,
      negative_count: 0,
      top_genres: {},
      top_artists: {},
      avg_weight: 0
    };

    let totalWeight = 0;

    for (const entry of entries) {
      const weight = entry.data?.weight || entry.weight || 0;
      totalWeight += weight;

      if (weight > 0) summary.positive_count++;
      if (weight < 0) summary.negative_count++;

      if (entry.data?.genres) {
        for (const genre of entry.data.genres) {
          summary.top_genres[genre] = (summary.top_genres[genre] || 0) + weight;
        }
      }

      if (entry.data?.artist) {
        const artist = entry.data.artist;
        summary.top_artists[artist] = (summary.top_artists[artist] || 0) + weight;
      }
    }

    summary.avg_weight = entries.length > 0 ? totalWeight / entries.length : 0;

    summary.top_genres = this._sortAndLimit(summary.top_genres, 5);
    summary.top_artists = this._sortAndLimit(summary.top_artists, 5);

    return summary;
  }

  _sortAndLimit(obj, limit) {
    return Object.entries(obj)
      .sort((a, b) => b[1] - a[1])
      .slice(0, limit)
      .reduce((acc, [k, v]) => ({ ...acc, [k]: v }), {});
  }

  getPreferences(category, options = {}) {
    const { timeRange = 'all', limit = 10 } = options;
    const preferences = [];

    for (const [key, entry] of this.memories[MemoryTypes.SHORT_TERM]) {
      if (key.startsWith(category)) {
        preferences.push(entry);
      }
    }

    for (const [key, aggregate] of this.memories[MemoryTypes.MEDIUM_TERM]) {
      if (key.startsWith(category)) {
        preferences.push(aggregate);
      }
    }

    return preferences.slice(0, limit);
  }

  getMusicRecommendations(context = {}) {
    const profile = this.getUserProfile();
    const recentPrefs = this.getPreferences(MemoryCategories.MUSIC_PREFERENCE, { limit: 20 });

    const recommendations = {
      preferred_genres: [],
      preferred_artists: [],
      avoided_genres: [],
      avoided_artists: [],
      context_match: {}
    };

    const genreScores = {};
    const artistScores = {};

    for (const pref of recentPrefs) {
      if (pref.summary) {
        Object.entries(pref.summary.top_genres || {}).forEach(([genre, score]) => {
          genreScores[genre] = (genreScores[genre] || 0) + score;
        });
        Object.entries(pref.summary.top_artists || {}).forEach(([artist, score]) => {
          artistScores[artist] = (artistScores[artist] || 0) + score;
        });
      } else if (pref.data) {
        if (pref.data.genres) {
          const weight = pref.data.weight || 0;
          for (const genre of pref.data.genres) {
            genreScores[genre] = (genreScores[genre] || 0) + weight;
          }
        }
        if (pref.data.artist) {
          artistScores[pref.data.artist] = (artistScores[pref.data.artist] || 0) + (pref.data.weight || 0);
        }
      }
    }

    if (profile?.preferences?.liked_genres) {
      for (const genre of profile.preferences.liked_genres) {
        genreScores[genre] = (genreScores[genre] || 0) + 5;
      }
    }

    if (profile?.preferences?.disliked_genres) {
      for (const genre of profile.preferences.disliked_genres) {
        genreScores[genre] = (genreScores[genre] || 0) - 10;
      }
    }

    for (const [genre, score] of Object.entries(genreScores)) {
      if (score > 0) {
        recommendations.preferred_genres.push({ genre, score });
      } else if (score < -1) {
        recommendations.avoided_genres.push({ genre, score: Math.abs(score) });
      }
    }

    for (const [artist, score] of Object.entries(artistScores)) {
      if (score > 0) {
        recommendations.preferred_artists.push({ artist, score });
      } else if (score < -1) {
        recommendations.avoided_artists.push({ artist, score: Math.abs(score) });
      }
    }

    recommendations.preferred_genres.sort((a, b) => b.score - a.score);
    recommendations.preferred_artists.sort((a, b) => b.score - a.score);
    recommendations.avoided_genres.sort((a, b) => b.score - a.score);
    recommendations.avoided_artists.sort((a, b) => b.score - a.score);

    recommendations.preferred_genres = recommendations.preferred_genres.slice(0, 5);
    recommendations.preferred_artists = recommendations.preferred_artists.slice(0, 5);

    return recommendations;
  }

  enhanceDescriptor(descriptor, context = {}) {
    const recommendations = this.getMusicRecommendations(context);
    const profile = this.getUserProfile();

    const enhanced = JSON.parse(JSON.stringify(descriptor));

    if (!enhanced.hints) enhanced.hints = {};
    if (!enhanced.hints.music) enhanced.hints.music = {};
    if (!enhanced.intent) enhanced.intent = {};
    if (!enhanced.intent.user_overrides) enhanced.intent.user_overrides = {};

    if (recommendations.preferred_genres.length > 0) {
      const topGenres = recommendations.preferred_genres.map(g => g.genre);
      enhanced.hints.music.suggested_tags = [
        ...new Set([...(enhanced.hints.music.suggested_tags || []), ...topGenres])
      ];
    }

    if (recommendations.preferred_artists.length > 0) {
      enhanced.hints.music.suggested_artists = [
        ...new Set([
          ...(enhanced.hints.music.suggested_artists || []),
          ...recommendations.preferred_artists.map(a => a.artist)
        ])
      ];
    }

    if (recommendations.avoided_genres.length > 0) {
      enhanced.intent.user_overrides.exclude_tags = [
        ...new Set([
          ...(enhanced.intent.user_overrides.exclude_tags || []),
          ...recommendations.avoided_genres.map(g => g.genre)
        ])
      ];
    }

    if (profile?.preferences?.vibe) {
      enhanced.hints.music.suggested_vibe = profile.preferences.vibe;
    }

    enhanced._memory_enhanced = true;
    enhanced._memory_context = {
      profile_id: profile?.id,
      preference_count: recommendations.preferred_genres.length + recommendations.preferred_artists.length
    };

    return enhanced;
  }

  _store(type, key, value) {
    const memory = this.memories[type];
    memory.set(key, {
      ...value,
      _stored_at: Date.now()
    });

    this._enforceLimit(type);
  }

  _retrieve(type, key) {
    const memory = this.memories[type];
    const entry = memory.get(key);

    if (!entry) return null;

    const ttl = this._getTTL(type);
    if (Date.now() - entry._stored_at > ttl) {
      memory.delete(key);
      return null;
    }

    return entry;
  }

  _enforceLimit(type) {
    const memory = this.memories[type];
    const maxEntries = this._getMaxEntries(type);

    if (memory.size > maxEntries) {
      const keys = [...memory.keys()];
      const toDelete = keys.slice(0, memory.size - maxEntries);
      for (const key of toDelete) {
        memory.delete(key);
      }
    }
  }

  _getTTL(type) {
    switch (type) {
      case MemoryTypes.SHORT_TERM: return this.config.shortTermTTL;
      case MemoryTypes.MEDIUM_TERM: return this.config.mediumTermTTL;
      case MemoryTypes.LONG_TERM: return this.config.longTermTTL;
      default: return this.config.shortTermTTL;
    }
  }

  _getMaxEntries(type) {
    switch (type) {
      case MemoryTypes.SHORT_TERM: return this.config.maxShortTermEntries;
      case MemoryTypes.MEDIUM_TERM: return this.config.maxMediumTermEntries;
      case MemoryTypes.LONG_TERM: return this.config.maxLongTermEntries;
      default: return this.config.maxShortTermEntries;
    }
  }

  clear(type = null) {
    if (type) {
      this.memories[type].clear();
    } else {
      for (const memory of Object.values(this.memories)) {
        memory.clear();
      }
    }
  }

  export() {
    return {
      user_profile: this.userProfile,
      session_context: this.sessionContext,
      short_term: Object.fromEntries(this.memories[MemoryTypes.SHORT_TERM]),
      medium_term: Object.fromEntries(this.memories[MemoryTypes.MEDIUM_TERM]),
      long_term: Object.fromEntries(this.memories[MemoryTypes.LONG_TERM]),
      exported_at: new Date().toISOString()
    };
  }

  import(data) {
    if (data.user_profile) {
      this.userProfile = data.user_profile;
    }
    if (data.session_context) {
      this.sessionContext = data.session_context;
    }
    if (data.short_term) {
      this.memories[MemoryTypes.SHORT_TERM] = new Map(Object.entries(data.short_term));
    }
    if (data.medium_term) {
      this.memories[MemoryTypes.MEDIUM_TERM] = new Map(Object.entries(data.medium_term));
    }
    if (data.long_term) {
      this.memories[MemoryTypes.LONG_TERM] = new Map(Object.entries(data.long_term));
    }
  }

  getStats() {
    return {
      short_term_count: this.memories[MemoryTypes.SHORT_TERM].size,
      medium_term_count: this.memories[MemoryTypes.MEDIUM_TERM].size,
      long_term_count: this.memories[MemoryTypes.LONG_TERM].size,
      has_profile: !!this.userProfile,
      session_id: this.sessionContext.session_id
    };
  }
}

const memorySystem = new MemorySystem();

module.exports = {
  MemorySystem,
  memorySystem,
  MemoryTypes,
  MemoryCategories
};

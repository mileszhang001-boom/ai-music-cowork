'use strict';

const fs = require('fs');
const path = require('path');

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
  CONTEXT_HISTORY: 'context_history',
  JOURNEY_MEMORY: 'journey_memory'
};

const DefaultConfig = {
  shortTermTTL: 30 * 60 * 1000,
  mediumTermTTL: 7 * 24 * 60 * 60 * 1000,
  longTermTTL: 365 * 24 * 60 * 60 * 1000,
  maxShortTermEntries: 100,
  maxMediumTermEntries: 500,
  maxLongTermEntries: 1000,
  persistPath: './data/memory',
  autoPersist: true,
  persistInterval: 60000
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
    this.journeyCards = [];
    this.currentJourney = null;
    this.persistTimer = null;
    
    this._ensureDataDir();
    this._loadFromDisk();
    
    if (this.config.autoPersist) {
      this._startAutoPersist();
    }
  }

  _ensureDataDir() {
    const dataDir = path.resolve(this.config.persistPath);
    if (!fs.existsSync(dataDir)) {
      fs.mkdirSync(dataDir, { recursive: true });
    }
  }

  _getProfilePath() {
    return path.join(this.config.persistPath, 'user_profile.json');
  }

  _getJourneyPath() {
    return path.join(this.config.persistPath, 'journey_cards.json');
  }

  _getMemoryPath(type) {
    return path.join(this.config.persistPath, `memory_${type}.json`);
  }

  _loadFromDisk() {
    try {
      const profilePath = this._getProfilePath();
      if (fs.existsSync(profilePath)) {
        const data = JSON.parse(fs.readFileSync(profilePath, 'utf8'));
        this.userProfile = data;
      }

      const journeyPath = this._getJourneyPath();
      if (fs.existsSync(journeyPath)) {
        const data = JSON.parse(fs.readFileSync(journeyPath, 'utf8'));
        this.journeyCards = Array.isArray(data) ? data : [];
      }

      for (const type of Object.values(MemoryTypes)) {
        const memoryPath = this._getMemoryPath(type);
        if (fs.existsSync(memoryPath)) {
          const data = JSON.parse(fs.readFileSync(memoryPath, 'utf8'));
          this.memories[type] = new Map(Object.entries(data));
        }
      }
    } catch (error) {
      console.warn('[MemorySystem] Failed to load from disk:', error.message);
    }
  }

  _persistToDisk() {
    try {
      this._ensureDataDir();

      if (this.userProfile) {
        fs.writeFileSync(this._getProfilePath(), JSON.stringify(this.userProfile, null, 2), 'utf8');
      }

      fs.writeFileSync(this._getJourneyPath(), JSON.stringify(this.journeyCards, null, 2), 'utf8');

      for (const [type, memory] of Object.entries(this.memories)) {
        const data = Object.fromEntries(memory);
        fs.writeFileSync(this._getMemoryPath(type), JSON.stringify(data, null, 2), 'utf8');
      }
    } catch (error) {
      console.warn('[MemorySystem] Failed to persist to disk:', error.message);
    }
  }

  _startAutoPersist() {
    this.persistTimer = setInterval(() => {
      this._persistToDisk();
    }, this.config.persistInterval);
  }

  stopAutoPersist() {
    if (this.persistTimer) {
      clearInterval(this.persistTimer);
      this.persistTimer = null;
    }
  }

  setUserProfile(profile) {
    this.userProfile = {
      ...profile,
      id: profile.id || `user_${Date.now()}`,
      created_at: this.userProfile?.created_at || new Date().toISOString(),
      updated_at: new Date().toISOString(),
      stats: {
        total_journeys: this.userProfile?.stats?.total_journeys || 0,
        total_distance_km: this.userProfile?.stats?.total_distance_km || 0,
        total_music_hours: this.userProfile?.stats?.total_music_hours || 0,
        favorite_scenes: this.userProfile?.stats?.favorite_scenes || []
      }
    };
    this._store(MemoryTypes.LONG_TERM, 'user_profile', this.userProfile);
    this._persistToDisk();
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

  updateUserProfile(updates) {
    const current = this.getUserProfile() || {};
    return this.setUserProfile({ ...current, ...updates });
  }

  startJourney(context = {}) {
    this.currentJourney = {
      journey_id: `journey_${Date.now()}`,
      started_at: new Date().toISOString(),
      context: {
        ...context,
        start_location: context.start_location || null
      },
      scenes: [],
      music: {
        tracks_played: [],
        total_tracks: 0,
        genres_played: {}
      },
      stats: {
        duration_minutes: 0,
        distance_km: 0
      }
    };
    return this.currentJourney;
  }

  recordScene(sceneDescriptor) {
    if (!this.currentJourney) return null;

    const sceneRecord = {
      scene_id: sceneDescriptor.scene_id,
      scene_type: sceneDescriptor.scene_type,
      scene_name: sceneDescriptor.scene_name,
      timestamp: new Date().toISOString(),
      energy_level: sceneDescriptor.intent?.energy_level,
      mood: sceneDescriptor.intent?.mood
    };

    this.currentJourney.scenes.push(sceneRecord);

    if (sceneDescriptor.scene_type) {
      const count = this.currentJourney.music.genres_played[sceneDescriptor.scene_type] || 0;
      this.currentJourney.music.genres_played[sceneDescriptor.scene_type] = count + 1;
    }

    return sceneRecord;
  }

  recordMusic(track, action) {
    if (!this.currentJourney) return null;

    const trackRecord = {
      track_id: track.id || track.track_id,
      title: track.title,
      artist: track.artist,
      genres: track.genres || [],
      action,
      timestamp: new Date().toISOString()
    };

    this.currentJourney.music.tracks_played.push(trackRecord);
    this.currentJourney.music.total_tracks++;

    for (const genre of track.genres || []) {
      const count = this.currentJourney.music.genres_played[genre] || 0;
      this.currentJourney.music.genres_played[genre] = count + 1;
    }

    return trackRecord;
  }

  endJourney(context = {}) {
    if (!this.currentJourney) return null;

    this.currentJourney.ended_at = new Date().toISOString();
    this.currentJourney.context = {
      ...this.currentJourney.context,
      ...context,
      end_location: context.end_location || null
    };

    const start = new Date(this.currentJourney.started_at);
    const end = new Date(this.currentJourney.ended_at);
    this.currentJourney.stats.duration_minutes = Math.round((end - start) / 60000);
    this.currentJourney.stats.distance_km = context.distance_km || 0;

    const card = this._generateJourneyCard(this.currentJourney);
    this.journeyCards.push(card);

    this._updateProfileStats(this.currentJourney);

    this._store(MemoryTypes.LONG_TERM, `journey_${this.currentJourney.journey_id}`, this.currentJourney);

    const completedJourney = this.currentJourney;
    this.currentJourney = null;

    this._persistToDisk();

    return { journey: completedJourney, card };
  }

  _generateJourneyCard(journey) {
    const topGenres = Object.entries(journey.music.genres_played)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 3)
      .map(([genre, count]) => ({ genre, count }));

    const topScenes = this._getTopScenes(journey.scenes);

    const highlights = this._generateHighlights(journey);

    return {
      card_id: `card_${journey.journey_id}`,
      journey_id: journey.journey_id,
      created_at: new Date().toISOString(),
      summary: {
        date: journey.started_at.split('T')[0],
        duration_minutes: journey.stats.duration_minutes,
        distance_km: journey.stats.distance_km,
        start_location: journey.context.start_location,
        end_location: journey.context.end_location
      },
      music_review: {
        total_tracks: journey.music.total_tracks,
        top_genres: topGenres,
        favorite_track: journey.music.tracks_played.find(t => t.action === 'like') || journey.music.tracks_played[0]
      },
      scene_review: {
        total_scenes: journey.scenes.length,
        top_scenes: topScenes
      },
      highlights,
      mood_trajectory: this._calculateMoodTrajectory(journey.scenes)
    };
  }

  _getTopScenes(scenes) {
    const sceneCounts = {};
    for (const scene of scenes) {
      const type = scene.scene_type;
      sceneCounts[type] = (sceneCounts[type] || 0) + 1;
    }

    return Object.entries(sceneCounts)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 3)
      .map(([scene_type, count]) => ({ scene_type, count }));
  }

  _generateHighlights(journey) {
    const highlights = [];

    if (journey.stats.duration_minutes > 60) {
      highlights.push({
        type: 'long_journey',
        description: `长途旅程 ${journey.stats.duration_minutes} 分钟`
      });
    }

    if (journey.music.total_tracks >= 10) {
      highlights.push({
        type: 'music_lover',
        description: `欣赏了 ${journey.music.total_tracks} 首音乐`
      });
    }

    const fatigueScenes = journey.scenes.filter(s => s.scene_type === 'fatigue_alert');
    if (fatigueScenes.length > 0) {
      highlights.push({
        type: 'safety_alert',
        description: '系统检测到疲劳并进行了提醒'
      });
    }

    return highlights;
  }

  _calculateMoodTrajectory(scenes) {
    if (scenes.length < 2) return [];

    return scenes.map(s => ({
      timestamp: s.timestamp,
      valence: s.mood?.valence || 0.5,
      arousal: s.mood?.arousal || 0.5,
      energy: s.energy_level || 0.5
    }));
  }

  _updateProfileStats(journey) {
    const profile = this.getUserProfile();
    if (!profile) return;

    profile.stats = profile.stats || {};
    profile.stats.total_journeys = (profile.stats.total_journeys || 0) + 1;
    profile.stats.total_distance_km = (profile.stats.total_distance_km || 0) + (journey.stats.distance_km || 0);
    profile.stats.total_music_hours = (profile.stats.total_music_hours || 0) + (journey.stats.duration_minutes / 60);

    const favoriteScenes = profile.stats.favorite_scenes || [];
    for (const scene of journey.scenes) {
      const existing = favoriteScenes.find(s => s.scene_type === scene.scene_type);
      if (existing) {
        existing.count++;
      } else {
        favoriteScenes.push({ scene_type: scene.scene_type, count: 1 });
      }
    }
    favoriteScenes.sort((a, b) => b.count - a.count);
    profile.stats.favorite_scenes = favoriteScenes.slice(0, 10);

    this.setUserProfile(profile);
  }

  getJourneyCards(options = {}) {
    const { limit = 10, startDate, endDate } = options;
    
    let cards = [...this.journeyCards];
    
    if (startDate) {
      cards = cards.filter(c => c.summary.date >= startDate);
    }
    if (endDate) {
      cards = cards.filter(c => c.summary.date <= endDate);
    }
    
    return cards.slice(-limit);
  }

  getJourneyCard(cardId) {
    return this.journeyCards.find(c => c.card_id === cardId) || null;
  }

  getCurrentJourney() {
    return this.currentJourney;
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
    const { limit = 10 } = options;
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
      enhanced.hints.music.genres = [
        ...new Set([...(enhanced.hints.music.genres || []), ...topGenres])
      ].slice(0, 5);
    }

    if (recommendations.avoided_genres.length > 0) {
      enhanced.intent.user_overrides.exclude_tags = [
        ...new Set([
          ...(enhanced.intent.user_overrides.exclude_tags || []),
          ...recommendations.avoided_genres.map(g => g.genre)
        ])
      ];
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
      journey_cards: this.journeyCards,
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
    if (data.journey_cards) {
      this.journeyCards = data.journey_cards;
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
      session_id: this.sessionContext.session_id,
      journey_cards_count: this.journeyCards.length,
      current_journey: this.currentJourney ? this.currentJourney.journey_id : null
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

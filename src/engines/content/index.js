'use strict';

/**
 * @fileoverview Content Engine V2 - 内容引擎增强版
 * @description 完善自主决策逻辑，支持 Hints 采纳与硬约束处理，支持引擎间协作
 */

const { eventBus, EventTypes } = require('../../core/eventBus');

const HintPriority = {
  REQUIRED: 'required',
  PREFERRED: 'preferred',
  SUGGESTED: 'suggested'
};

const ConstraintType = {
  HARD: 'hard',
  SOFT: 'soft'
};

class ContentEngine {
  constructor(config = {}) {
    this.config = config;
    this.name = 'content';
    this.version = '2.0';
    this.playlist = [];
    this.currentTrack = null;
    this.history = [];
    this.rejectedTracks = new Set();
    this.userPreferences = new Map();
    this.collaborationState = {
      lightingSync: false,
      beatSync: false,
      lastBeatTime: null
    };
    
    this.setupEventListeners();
  }

  setupEventListeners() {
    eventBus.on(EventTypes.MUSIC_BEAT, (data) => {
      this.onBeat(data);
    });

    eventBus.on(EventTypes.USER_FEEDBACK, (data) => {
      this.onUserFeedback(data);
    });
  }

  async execute(action, params) {
    switch (action) {
    case 'curate_playlist':
      return this.curatePlaylist(params);
    case 'play':
      return this.play(params);
    case 'pause':
      return this.pause();
    case 'next':
      return this.next();
    case 'previous':
      return this.previous();
    case 'set_volume':
      return this.setVolume(params);
    case 'update_preferences':
      return this.updatePreferences(params);
    case 'get_recommendations':
      return this.getRecommendations(params);
    default:
      throw new Error(`Unknown action: ${action}`);
    }
  }

  curatePlaylist(params) {
    const { hints, constraints, transition, context } = params;
    
    const processedHints = this.processHints(hints);
    const processedConstraints = this.processConstraints(constraints);
    
    const playlist = this.generatePlaylist(processedHints, processedConstraints, context);
    
    const filteredPlaylist = this.applyConstraints(playlist, processedConstraints);
    
    this.playlist = filteredPlaylist;

    if (this.playlist.length > 0) {
      this.currentTrack = this.playlist[0];
    }

    this.emitCollaborationEvent('playlist_ready', {
      playlist: this.playlist,
      energy_profile: this.calculateEnergyProfile(this.playlist)
    });

    return {
      playlist: this.playlist,
      current_track: this.currentTrack,
      total_duration: this.calculateTotalDuration(this.playlist),
      hints_applied: processedHints.summary,
      constraints_applied: Object.keys(processedConstraints).length
    };
  }

  processHints(hints) {
    if (!hints || !hints.music) {
      return { music: {}, summary: { applied: 0, ignored: 0 } };
    }

    const musicHints = hints.music;
    const processed = {
      genres: this.processHintField(musicHints.genres, 'genres'),
      tempo: this.processHintField(musicHints.tempo, 'tempo'),
      vocalStyle: this.processHintField(musicHints.vocal_style, 'vocal_style'),
      energy: this.processHintField(musicHints.energy, 'energy'),
      mood: this.processHintField(musicHints.mood, 'mood'),
      priority: musicHints.priority || HintPriority.PREFERRED
    };

    processed.summary = {
      applied: Object.values(processed).filter(v => v && typeof v === 'object' && v.applied).length,
      ignored: Object.values(processed).filter(v => v && typeof v === 'object' && v.ignored).length
    };

    return { music: processed, summary: processed.summary };
  }

  processHintField(value, fieldName) {
    if (!value) return { applied: false, ignored: true, value: null };
    
    return {
      applied: true,
      ignored: false,
      value: value,
      weight: this.getHintWeight(fieldName)
    };
  }

  getHintWeight(fieldName) {
    const weights = {
      genres: 1.0,
      tempo: 0.8,
      vocal_style: 0.6,
      energy: 0.7,
      mood: 0.9
    };
    return weights[fieldName] || 0.5;
  }

  processConstraints(constraints) {
    if (!constraints) return {};

    const processed = {};

    if (constraints.max_volume_db !== undefined) {
      processed.maxVolumeDb = {
        type: ConstraintType.HARD,
        value: constraints.max_volume_db,
        enforceable: true
      };
    }

    if (constraints.max_duration_sec !== undefined) {
      processed.maxDurationSec = {
        type: ConstraintType.SOFT,
        value: constraints.max_duration_sec,
        enforceable: true
      };
    }

    if (constraints.explicit_content !== undefined) {
      processed.explicitContent = {
        type: ConstraintType.HARD,
        value: constraints.explicit_content === false,
        enforceable: true
      };
    }

    if (constraints.min_energy !== undefined) {
      processed.minEnergy = {
        type: ConstraintType.SOFT,
        value: constraints.min_energy,
        enforceable: true
      };
    }

    if (constraints.max_energy !== undefined) {
      processed.maxEnergy = {
        type: ConstraintType.SOFT,
        value: constraints.max_energy,
        enforceable: true
      };
    }

    return processed;
  }

  generatePlaylist(processedHints, processedConstraints, context) {
    const playlist = [];
    const musicHints = processedHints.music || {};
    const genres = musicHints.genres?.value || ['pop'];
    const tempo = musicHints.tempo?.value || 'medium';
    const vocalStyle = musicHints.vocalStyle?.value || 'any';
    const targetEnergy = musicHints.energy?.value;

    const playlistSize = this.determinePlaylistSize(processedConstraints);

    for (let i = 0; i < playlistSize; i++) {
      const track = this.generateTrack(i, {
        genres,
        tempo,
        vocalStyle,
        targetEnergy
      }, context);

      if (!this.rejectedTracks.has(track.track_id)) {
        playlist.push(track);
      }
    }

    return playlist;
  }

  determinePlaylistSize(constraints) {
    if (constraints.maxDurationSec) {
      const avgTrackDuration = 200;
      return Math.ceil(constraints.maxDurationSec.value / avgTrackDuration);
    }
    return 10;
  }

  generateTrack(index, hints, context) {
    const energyBase = hints.targetEnergy || (0.3 + Math.random() * 0.5);
    const energyVariation = 0.1 * (Math.random() - 0.5);
    
    return {
      track_id: `track_${Date.now()}_${index}`,
      title: this.generateTitle(index, hints.genres),
      artist: this.generateArtist(index),
      genre: hints.genres[index % hints.genres.length],
      duration_sec: 180 + Math.floor(Math.random() * 120),
      tempo: hints.tempo,
      vocal_style: hints.vocalStyle,
      energy: Math.max(0, Math.min(1, energyBase + energyVariation)),
      valence: 0.4 + Math.random() * 0.3,
      danceability: 0.3 + Math.random() * 0.4,
      acousticness: Math.random() * 0.5,
      explicit: Math.random() > 0.9
    };
  }

  generateTitle(index, genres) {
    const titles = {
      pop: ['阳光旋律', '城市节拍', '青春记忆', '快乐时光'],
      rock: ['自由之路', '摇滚之心', '狂野节拍', '电吉他之歌'],
      jazz: ['午夜爵士', '蓝调之夜', '咖啡时光', '即兴演奏'],
      electronic: ['电子脉冲', '数字梦境', '合成器之歌', '未来节拍'],
      classical: ['月光奏鸣曲', '春之声', '小夜曲', '交响乐章'],
      ambient: ['宁静时刻', '自然之声', '冥想空间', '晨曦微光']
    };
    
    const genre = genres[index % genres.length] || 'pop';
    const genreTitles = titles[genre] || titles.pop;
    return genreTitles[index % genreTitles.length];
  }

  generateArtist(index) {
    const artists = ['星辰乐队', '月光组合', '风之声', '城市行者', '晨曦歌手'];
    return artists[index % artists.length];
  }

  applyConstraints(playlist, constraints) {
    let filtered = [...playlist];

    for (const [key, constraint] of Object.entries(constraints)) {
      if (!constraint.enforceable) continue;

      switch (key) {
      case 'explicitContent':
        if (constraint.value === true) {
          filtered = filtered.filter(t => !t.explicit);
        }
        break;
      case 'minEnergy':
        if (constraint.type === ConstraintType.HARD) {
          filtered = filtered.filter(t => t.energy >= constraint.value);
        }
        break;
      case 'maxEnergy':
        if (constraint.type === ConstraintType.HARD) {
          filtered = filtered.filter(t => t.energy <= constraint.value);
        }
        break;
      case 'maxDurationSec':
        let totalDuration = 0;
        filtered = filtered.filter(t => {
          totalDuration += t.duration_sec;
          return totalDuration <= constraint.value;
        });
        break;
      }
    }

    return filtered;
  }

  calculateEnergyProfile(playlist) {
    if (playlist.length === 0) return { average: 0, variance: 0 };
    
    const energies = playlist.map(t => t.energy);
    const average = energies.reduce((a, b) => a + b, 0) / energies.length;
    const variance = energies.reduce((a, b) => a + Math.pow(b - average, 2), 0) / energies.length;
    
    return { average, variance, min: Math.min(...energies), max: Math.max(...energies) };
  }

  calculateTotalDuration(playlist) {
    return playlist.reduce((sum, track) => sum + track.duration_sec, 0);
  }

  onBeat(data) {
    this.collaborationState.lastBeatTime = Date.now();
    
    if (this.collaborationState.beatSync && this.currentTrack) {
      eventBus.emit(EventTypes.ENGINE_ACTION, {
        engine: 'content',
        action: 'beat_sync',
        data: {
          track_id: this.currentTrack.track_id,
          beat: data.beat,
          intensity: data.intensity
        }
      });
    }
  }

  onUserFeedback(data) {
    if (data.action === 'skip' && data.track_id) {
      this.rejectedTracks.add(data.track_id);
    }
    
    if (data.action === 'like' && data.track_id) {
      this.userPreferences.set(data.track_id, { liked: true, timestamp: Date.now() });
    }
  }

  emitCollaborationEvent(event, data) {
    eventBus.emit(EventTypes.ENGINE_ACTION, {
      engine: 'content',
      action: event,
      data: data
    });
  }

  play(params) {
    if (params?.track_id) {
      const track = this.playlist.find(t => t.track_id === params.track_id);
      if (track) {
        this.currentTrack = track;
      }
    }

    this.collaborationState.beatSync = true;

    this.emitCollaborationEvent('playback_started', {
      track: this.currentTrack,
      energy: this.currentTrack?.energy
    });

    eventBus.emit(EventTypes.MUSIC_PLAYBACK_STATE, {
      state: 'playing',
      track: this.currentTrack
    });

    return {
      state: 'playing',
      track: this.currentTrack
    };
  }

  pause() {
    this.collaborationState.beatSync = false;

    eventBus.emit(EventTypes.MUSIC_PLAYBACK_STATE, {
      state: 'paused',
      track: this.currentTrack
    });

    return { state: 'paused' };
  }

  next() {
    const currentIndex = this.playlist.findIndex(t => t.track_id === this.currentTrack?.track_id);
    const nextIndex = (currentIndex + 1) % this.playlist.length;
    this.currentTrack = this.playlist[nextIndex];

    this.emitCollaborationEvent('track_changed', {
      track: this.currentTrack,
      reason: 'user_next',
      energy: this.currentTrack?.energy
    });

    eventBus.emit(EventTypes.MUSIC_TRACK_CHANGED, {
      track: this.currentTrack,
      reason: 'user_next'
    });

    return { track: this.currentTrack };
  }

  previous() {
    const currentIndex = this.playlist.findIndex(t => t.track_id === this.currentTrack?.track_id);
    const prevIndex = currentIndex > 0 ? currentIndex - 1 : this.playlist.length - 1;
    this.currentTrack = this.playlist[prevIndex];

    this.emitCollaborationEvent('track_changed', {
      track: this.currentTrack,
      reason: 'user_previous',
      energy: this.currentTrack?.energy
    });

    eventBus.emit(EventTypes.MUSIC_TRACK_CHANGED, {
      track: this.currentTrack,
      reason: 'user_previous'
    });

    return { track: this.currentTrack };
  }

  setVolume(params) {
    return { volume: params?.volume || 50 };
  }

  updatePreferences(params) {
    if (params.liked_genres) {
      for (const genre of params.liked_genres) {
        this.userPreferences.set(`genre_${genre}`, { liked: true, timestamp: Date.now() });
      }
    }
    return { updated: true };
  }

  getRecommendations(params) {
    return {
      based_on: 'current_context',
      tracks: this.playlist.slice(0, 5)
    };
  }

  generateFeedbackReport() {
    return {
      engine: this.name,
      timestamp: Date.now(),
      state: {
        playlist_size: this.playlist.length,
        current_track: this.currentTrack?.track_id,
        rejected_count: this.rejectedTracks.size,
        preferences_count: this.userPreferences.size
      },
      metrics: {
        tracks_played: this.history.length,
        skip_rate: this.rejectedTracks.size / Math.max(1, this.playlist.length)
      }
    };
  }

  getStatus() {
    return {
      name: this.name,
      version: this.version,
      playlist_size: this.playlist.length,
      current_track: this.currentTrack,
      is_playing: !!this.currentTrack,
      collaboration: this.collaborationState
    };
  }
}

const contentEngine = new ContentEngine();

module.exports = {
  ContentEngine,
  contentEngine,
  HintPriority,
  ConstraintType
};

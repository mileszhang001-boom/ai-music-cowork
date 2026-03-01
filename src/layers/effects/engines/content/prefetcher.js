'use strict';

const fs = require('fs');
const path = require('path');

const TempoRange = {
  slow: [60, 90],
  medium: [90, 120],
  moderate: [90, 120],
  fast: [120, 160],
  upbeat: [120, 160]
};

const MIN_PLAYLIST_SIZE = 20;
const MAX_PLAYLIST_SIZE = 30;
const MAX_TRACKS_PER_ARTIST = 3;
const MIN_SCORE_THRESHOLD = 40;

function parseTags(tags) {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
}

class PlaylistPrefetcher {
  constructor(config = {}) {
    this.templateAnalysisPath = config.templateAnalysisPath || path.join(process.cwd(), 'data/template_analysis.json');
    this.musicLibraryPath = config.musicLibraryPath || path.join(process.cwd(), 'data/index.json');
    this.cacheDir = config.cacheDir || path.join(process.cwd(), 'data', 'playlist_cache');
    
    this.templateAnalysis = null;
    this.musicLibrary = null;
    this.stats = {
      templatesProcessed: 0,
      totalPlaylistsGenerated: 0,
      totalTracksCached: 0,
      errors: []
    };
  }

  async initialize() {
    console.log('[PlaylistPrefetcher] Initializing...');
    
    this._ensureCacheDirectory();
    
    this.templateAnalysis = this._loadJsonFile(this.templateAnalysisPath);
    if (!this.templateAnalysis) {
      throw new Error(`Failed to load template analysis from ${this.templateAnalysisPath}`);
    }
    
    this.musicLibrary = this._loadJsonFile(this.musicLibraryPath);
    if (!this.musicLibrary) {
      throw new Error(`Failed to load music library from ${this.musicLibraryPath}`);
    }
    
    console.log(`[PlaylistPrefetcher] Loaded ${Object.keys(this.templateAnalysis.analysis || {}).length} templates`);
    console.log(`[PlaylistPrefetcher] Loaded ${this.musicLibrary.length} tracks from music library`);
    
    return true;
  }

  _ensureCacheDirectory() {
    if (!fs.existsSync(this.cacheDir)) {
      fs.mkdirSync(this.cacheDir, { recursive: true });
      console.log(`[PlaylistPrefetcher] Created cache directory: ${this.cacheDir}`);
    }
  }

  _loadJsonFile(filePath) {
    try {
      const data = fs.readFileSync(filePath, 'utf8');
      return JSON.parse(data);
    } catch (error) {
      console.error(`[PlaylistPrefetcher] Failed to load ${filePath}:`, error.message);
      return null;
    }
  }

  _calculateOverallScore(track, hints = {}) {
    let score = 0;
    const weights = {
      genre: 20,
      energy: 15,
      valence: 15,
      tempo: 20,
      mood: 20,
      scene: 10
    };

    if (hints.genres && hints.genres.length > 0) {
      if (hints.genres.includes(track.genre)) {
        score += weights.genre;
      }
    } else {
      score += weights.genre * 0.5;
    }

    if (hints.energy_level !== undefined) {
      const trackEnergy = track.energy || 0.5;
      const energyDiff = Math.abs(trackEnergy - hints.energy_level);
      const energyScore = Math.max(0, 1 - energyDiff / 0.5) * weights.energy;
      score += energyScore;
    } else {
      score += weights.energy * 0.5;
    }

    if (hints.valence !== undefined) {
      const trackValence = track.valence !== undefined ? track.valence : 0.5;
      const valenceDiff = Math.abs(trackValence - hints.valence);
      const valenceScore = Math.max(0, 1 - valenceDiff / 0.5) * weights.valence;
      score += valenceScore;
    } else {
      score += weights.valence * 0.5;
    }

    if (hints.tempo && TempoRange[hints.tempo]) {
      const [minBpm, maxBpm] = TempoRange[hints.tempo];
      const trackBpm = track.bpm || 100;
      if (trackBpm >= minBpm && trackBpm <= maxBpm) {
        score += weights.tempo;
      } else {
        const bpmDiff = Math.min(Math.abs(trackBpm - minBpm), Math.abs(trackBpm - maxBpm));
        const tempoScore = Math.max(0, 1 - bpmDiff / 40) * weights.tempo;
        score += tempoScore;
      }
    } else {
      score += weights.tempo * 0.5;
    }

    if (hints.mood && hints.mood.length > 0) {
      const trackMoods = parseTags(track.mood_tags);
      const moodMatches = hints.mood.filter(m => 
        trackMoods.some(tm => tm.toLowerCase().includes(m.toLowerCase()))
      );
      const moodScore = (moodMatches.length / hints.mood.length) * weights.mood;
      score += moodScore;
    } else {
      score += weights.mood * 0.5;
    }

    if (hints.scene && hints.scene.length > 0) {
      const trackScenes = parseTags(track.scene_tags);
      const sceneMatches = hints.scene.filter(s => trackScenes.includes(s));
      const sceneScore = (sceneMatches.length / hints.scene.length) * weights.scene;
      score += sceneScore;
    } else {
      score += weights.scene * 0.5;
    }

    return Math.round(score);
  }

  _buildHintsFromTemplate(template) {
    const hints = {};
    
    if (template.mood_mapping) {
      if (template.mood_mapping.valence !== undefined) {
        hints.valence = template.mood_mapping.valence;
      }
      if (template.mood_mapping.energy !== undefined) {
        hints.energy_level = template.mood_mapping.energy;
      }
    }
    
    if (template.music_requirements) {
      if (template.music_requirements.genres && template.music_requirements.genres.length > 0) {
        hints.genres = template.music_requirements.genres;
      }
      if (template.music_requirements.tempo) {
        hints.tempo = template.music_requirements.tempo;
      }
      if (template.scene_type) {
        hints.scene = [template.scene_type];
      }
    }
    
    return hints;
  }

  _ensureArtistDiversity(tracks, maxTracks) {
    const result = [];
    const artistCount = {};
    
    for (const track of tracks) {
      if (result.length >= maxTracks) break;
      
      const artist = track.artist || 'Unknown';
      const count = artistCount[artist] || 0;
      
      if (count < MAX_TRACKS_PER_ARTIST) {
        result.push(track);
        artistCount[artist] = count + 1;
      }
    }
    
    return result;
  }

  _generateMatchReasons(track, hints) {
    const reasons = [];
    
    if (hints.genres && hints.genres.includes(track.genre)) {
      reasons.push(`genre:${track.genre}`);
    }
    
    if (hints.energy_level !== undefined && track.energy !== undefined) {
      const energyDiff = Math.abs(track.energy - hints.energy_level);
      if (energyDiff <= 0.2) {
        reasons.push(`energy:${track.energy.toFixed(2)}`);
      }
    }
    
    if (hints.valence !== undefined && track.valence !== undefined) {
      const valenceDiff = Math.abs(track.valence - hints.valence);
      if (valenceDiff <= 0.2) {
        reasons.push(`valence:${track.valence.toFixed(2)}`);
      }
    }
    
    if (hints.tempo && TempoRange[hints.tempo]) {
      const [minBpm, maxBpm] = TempoRange[hints.tempo];
      if (track.bpm >= minBpm && track.bpm <= maxBpm) {
        reasons.push(`tempo:${track.bpm}`);
      }
    }
    
    if (hints.scene && hints.scene.length > 0) {
      const trackScenes = parseTags(track.scene_tags);
      const matchedScenes = hints.scene.filter(s => trackScenes.includes(s));
      if (matchedScenes.length > 0) {
        reasons.push(`scene:${matchedScenes[0]}`);
      }
    }
    
    return reasons;
  }

  generatePlaylistForTemplate(templateId) {
    const template = this.templateAnalysis.analysis[templateId];
    if (!template) {
      console.warn(`[PlaylistPrefetcher] Template not found: ${templateId}`);
      return null;
    }
    
    const hints = this._buildHintsFromTemplate(template);
    
    let tracks = this.musicLibrary.map(t => ({
      ...t,
      mood_tags: parseTags(t.mood_tags),
      scene_tags: parseTags(t.scene_tags)
    }));
    
    tracks = tracks.map(t => ({
      ...t,
      _score: this._calculateOverallScore(t, hints),
      _matchReasons: this._generateMatchReasons(t, hints)
    }));
    
    tracks = tracks.filter(t => t._score >= MIN_SCORE_THRESHOLD);
    
    tracks.sort((a, b) => b._score - a._score);
    
    const targetSize = MIN_PLAYLIST_SIZE + Math.floor(Math.random() * (MAX_PLAYLIST_SIZE - MIN_PLAYLIST_SIZE + 1));
    const diverseTracks = this._ensureArtistDiversity(tracks, targetSize);
    
    const playlist = diverseTracks.map(t => ({
      id: t.id,
      title: t.title,
      artist: t.artist,
      album: t.album,
      genre: t.genre,
      bpm: t.bpm,
      energy: t.energy,
      valence: t.valence,
      duration_ms: t.duration_ms,
      file_path: t.file_path,
      score: t._score,
      match_reasons: t._matchReasons
    }));
    
    const totalDurationMs = playlist.reduce((sum, t) => sum + (t.duration_ms || 0), 0);
    const avgScore = playlist.length > 0 
      ? Math.round(playlist.reduce((sum, t) => sum + t.score, 0) / playlist.length)
      : 0;
    
    return {
      templateId: templateId,
      template_name: template.name,
      sceneType: template.scene_type,
      category: template.category,
      timestamp: Date.now(),
      generated_at: new Date().toISOString(),
      playlist,
      stats: {
        total_tracks: playlist.length,
        avg_score: avgScore,
        total_duration_ms: totalDurationMs,
        total_duration_minutes: Math.round(totalDurationMs / 60000)
      },
      mood_mapping: template.mood_mapping,
      music_requirements: template.music_requirements
    };
  }

  async generateAllPlaylists() {
    console.log('[PlaylistPrefetcher] Starting playlist generation for all templates...');
    
    const templates = this.templateAnalysis.analysis || {};
    const templateIds = Object.keys(templates);
    
    console.log(`[PlaylistPrefetcher] Processing ${templateIds.length} templates...`);
    
    for (const templateId of templateIds) {
      try {
        const cachedPlaylist = this.generatePlaylistForTemplate(templateId);
        
        if (cachedPlaylist && cachedPlaylist.playlist.length > 0) {
          const cachePath = path.join(this.cacheDir, `${templateId}.json`);
          fs.writeFileSync(cachePath, JSON.stringify(cachedPlaylist, null, 2), 'utf8');
          
          if (cachedPlaylist.sceneType) {
            const sceneCachePath = path.join(this.cacheDir, `scene_${cachedPlaylist.sceneType}.json`);
            fs.writeFileSync(sceneCachePath, JSON.stringify(cachedPlaylist, null, 2), 'utf8');
          }
          
          this.stats.templatesProcessed++;
          this.stats.totalPlaylistsGenerated++;
          this.stats.totalTracksCached += cachedPlaylist.playlist.length;
          
          console.log(`[PlaylistPrefetcher] Generated playlist for ${templateId}: ${cachedPlaylist.playlist.length} tracks (avg score: ${cachedPlaylist.stats.avg_score})`);
        } else {
          this.stats.errors.push({ templateId, error: 'No tracks matched' });
          console.warn(`[PlaylistPrefetcher] No tracks matched for template ${templateId}`);
        }
      } catch (error) {
        this.stats.errors.push({ templateId, error: error.message });
        console.error(`[PlaylistPrefetcher] Error processing template ${templateId}:`, error.message);
      }
    }
    
    console.log('[PlaylistPrefetcher] Playlist generation complete!');
    console.log(`[PlaylistPrefetcher] Stats: ${JSON.stringify(this.stats, null, 2)}`);
    
    return this.stats;
  }

  getCachedPlaylist(templateId) {
    const cachePath = path.join(this.cacheDir, `${templateId}.json`);
    
    if (fs.existsSync(cachePath)) {
      const data = fs.readFileSync(cachePath, 'utf8');
      return JSON.parse(data);
    }
    
    return null;
  }

  hasCachedPlaylist(templateId) {
    const cachePath = path.join(this.cacheDir, `${templateId}.json`);
    return fs.existsSync(cachePath);
  }

  getCacheStats() {
    const cacheFiles = fs.existsSync(this.cacheDir) 
      ? fs.readdirSync(this.cacheDir).filter(f => f.endsWith('.json'))
      : [];
    
    return {
      cache_dir: this.cacheDir,
      cached_templates: cacheFiles.length,
      templates: cacheFiles.map(f => f.replace('.json', ''))
    };
  }
}

async function run() {
  const prefetcher = new PlaylistPrefetcher();
  
  try {
    await prefetcher.initialize();
    const stats = await prefetcher.generateAllPlaylists();
    
    console.log('\n=== Prefetch Summary ===');
    console.log(`Templates processed: ${stats.templatesProcessed}`);
    console.log(`Total playlists generated: ${stats.totalPlaylistsGenerated}`);
    console.log(`Total tracks cached: ${stats.totalTracksCached}`);
    
    if (stats.errors.length > 0) {
      console.log(`\nErrors (${stats.errors.length}):`);
      stats.errors.forEach(e => console.log(`  - ${e.templateId}: ${e.error}`));
    }
    
    const cacheStats = prefetcher.getCacheStats();
    console.log(`\nCache directory: ${cacheStats.cache_dir}`);
    console.log(`Cached templates: ${cacheStats.cached_templates}`);
    
    process.exit(0);
  } catch (error) {
    console.error('[PlaylistPrefetcher] Fatal error:', error.message);
    process.exit(1);
  }
}

module.exports = { PlaylistPrefetcher, run };

if (require.main === module) {
  run();
}

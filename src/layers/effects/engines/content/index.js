'use strict';

const fs = require('fs');
const path = require('path');
const { LLMClient, Models } = require('../../../../core/llm');

const TempoRange = {
  slow: [60, 90],
  medium: [90, 120],
  moderate: [90, 120],
  fast: [120, 160],
  upbeat: [120, 160]
};

const TEMPLATE_SCENES = [
  'morning_commute', 'night_drive', 'road_trip', 'party',
  'romantic_date', 'family_outing', 'focus_work', 'relax',
  'workout', 'rainy_day', 'traffic_jam', 'fatigue_alert',
  'meditation', 'summer', 'sunny_day',
  'afternoon_drive', 'evening_commute', 'dawn_commute',
  'sunset_drive', 'noon_break', 'solo_drive', 'couple_date',
  'friends_gathering', 'party_mode', 'focus_mode', 'relax_mode',
  'excited_mode', 'stress_mode', 'happy_mode', 'calm_mode',
  'energetic_mode', 'romantic_mode', 'nostalgic_mode', 'adventure_mode',
  'cozy_mode', 'meditation_mode', 'holiday_travel', 'special_event',
  'long_distance', 'city_drive', 'highway_drive', 'rural_drive',
  'suburban_drive', 'tunnel_drive', 'bridge_drive', 'parking_mode',
  'scenic_route', 'construction_zone', 'snowy_day', 'cloudy_day',
  'windy_day', 'foggy_day', 'rainy_night', 'fatigue_alert',
  'highway_cruise', 'focus_work'
];

const FALLBACK_PLAYLIST = [
  { id: 'fallback_001', title: '晴天', artist: '周杰伦', genre: 'pop', bpm: 100, energy: 0.6, valence: 0.7, duration: 269, mood: 'happy', mood_tags: ['happy', 'uplifting'], scene_tags: ['sunny_day', 'morning_commute'] },
  { id: 'fallback_002', title: '夜曲', artist: '周杰伦', genre: 'ballad', bpm: 80, energy: 0.4, valence: 0.35, duration: 226, mood: 'melancholy', mood_tags: ['melancholy', 'romantic'], scene_tags: ['rainy_day', 'night_drive'] },
  { id: 'fallback_003', title: '七里香', artist: '周杰伦', genre: 'pop', bpm: 90, energy: 0.5, valence: 0.65, duration: 299, mood: 'romantic', mood_tags: ['romantic', 'sentimental'], scene_tags: ['romantic_date', 'relax'] },
  { id: 'fallback_004', title: '稻香', artist: '周杰伦', genre: 'pop', bpm: 85, energy: 0.55, valence: 0.75, duration: 223, mood: 'happy', mood_tags: ['happy', 'nostalgic'], scene_tags: ['summer', 'family_trip'] },
  { id: 'fallback_005', title: '告白气球', artist: '周杰伦', genre: 'pop', bpm: 95, energy: 0.5, valence: 0.7, duration: 215, mood: 'romantic', mood_tags: ['romantic', 'sweet'], scene_tags: ['romantic_date', 'sunny_day'] },
  { id: 'fallback_006', title: '简单爱', artist: '周杰伦', genre: 'pop', bpm: 100, energy: 0.6, valence: 0.72, duration: 267, mood: 'happy', mood_tags: ['happy', 'romantic'], scene_tags: ['romantic_date', 'road_trip'] },
  { id: 'fallback_007', title: '安静', artist: '周杰伦', genre: 'ballad', bpm: 70, energy: 0.3, valence: 0.3, duration: 342, mood: 'sad', mood_tags: ['sad', 'melancholy'], scene_tags: ['rainy_day', 'night_drive'] },
  { id: 'fallback_008', title: '龙卷风', artist: '周杰伦', genre: 'pop', bpm: 110, energy: 0.65, valence: 0.55, duration: 253, mood: 'energetic', mood_tags: ['energetic', 'intense'], scene_tags: ['workout', 'party'] },
  { id: 'fallback_009', title: '以父之名', artist: '周杰伦', genre: 'pop', bpm: 90, energy: 0.5, valence: 0.45, duration: 341, mood: 'epic', mood_tags: ['epic', 'dramatic'], scene_tags: ['night_drive', 'focus_work'] },
  { id: 'fallback_010', title: '听妈妈的话', artist: '周杰伦', genre: 'pop', bpm: 85, energy: 0.45, valence: 0.68, duration: 241, mood: 'warm', mood_tags: ['warm', 'nostalgic'], scene_tags: ['family_trip', 'relax'] }
];

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

class ContentEngine {
  constructor(config = {}) {
    this.config = config;
    this.currentPlaylist = [];
    this.localMusicJson = null;
    this.localMusicModule = null;
    
    this.localJsonPath = config.localJsonPath || process.env.LOCAL_MUSIC_DB || '/sdcard/Music/AiMusic/index.json';
    
    this.cacheDir = config.cacheDir || process.env.PLAYLIST_CACHE_DIR || path.join(process.cwd(), 'data', 'playlist_cache');
    this.cacheEnabled = config.cacheEnabled !== false;
    this.cacheTTL = config.cacheTTL || 24 * 60 * 60 * 1000;
    this.cacheExpireDays = config.cacheExpireDays || 7;
    
    this.cacheMetaPath = path.join(this.cacheDir, '.cache_meta.json');
    this.lastLibraryModTime = null;
    this.lastCacheRefreshTime = null;
    
    this.sceneKeywordMapping = null;
    this._loadSceneKeywordMapping();
    
    this._initCacheDir();
    this._loadCacheMeta();
    this._loadLocalJsonDatabase();
    this._checkMusicLibraryUpdate();
    
    const apiKey = this.config.apiKey || process.env.DASHSCOPE_API_KEY;
    if (apiKey) {
      this.llmClient = new LLMClient({
        apiKey: apiKey,
        model: this.config.model || Models.QWEN_PLUS
      });
      this.config.enableLLM = true;
      console.log('[ContentEngine] LLM client initialized with API key');
    } else {
      console.warn('[ContentEngine] No API key found, LLM generation disabled');
    }
  }
  
  _loadSceneKeywordMapping() {
    try {
      const mappingPath = path.join(process.cwd(), 'data', 'scene_keyword_mapping.json');
      if (fs.existsSync(mappingPath)) {
        const data = fs.readFileSync(mappingPath, 'utf8');
        this.sceneKeywordMapping = JSON.parse(data);
        console.log('[ContentEngine] Loaded scene keyword mapping');
      }
    } catch (error) {
      console.warn('[ContentEngine] Failed to load scene keyword mapping:', error.message);
    }
  }
  
  matchSceneFromKeywords(keywords) {
    if (!this.sceneKeywordMapping || !keywords || keywords.length === 0) {
      return null;
    }
    
    const sceneKeywords = this.sceneKeywordMapping.scene_keywords;
    let bestMatch = null;
    let bestScore = 0;
    
    for (const [sceneType, sceneConfig] of Object.entries(sceneKeywords)) {
      const sceneWords = sceneConfig.keywords || [];
      let matchCount = 0;
      
      for (const keyword of keywords) {
        if (sceneWords.some(word => keyword.includes(word) || word.includes(keyword))) {
          matchCount++;
        }
      }
      
      const score = matchCount / Math.max(keywords.length, 1);
      if (score > bestScore && score >= (this.sceneKeywordMapping.keyword_matching_rules?.confidence_threshold || 0.6)) {
        bestScore = score;
        bestMatch = sceneType;
      }
    }
    
    return bestMatch;
  }
  
  getSceneMusicFeatures(sceneType) {
    if (!this.sceneKeywordMapping || !this.sceneKeywordMapping.scene_keywords[sceneType]) {
      return null;
    }
    
    return this.sceneKeywordMapping.scene_keywords[sceneType].music_features;
  }
  
  getSceneHighFreqSongs(sceneType) {
    if (!this.sceneKeywordMapping || !this.sceneKeywordMapping.scene_keywords[sceneType]) {
      return [];
    }
    
    return this.sceneKeywordMapping.scene_keywords[sceneType].high_freq_songs || [];
  }
  
  _initCacheDir() {
    try {
      if (!fs.existsSync(this.cacheDir)) {
        fs.mkdirSync(this.cacheDir, { recursive: true });
        console.log(`[ContentEngine] Created cache directory: ${this.cacheDir}`);
      }
    } catch (error) {
      console.warn('[ContentEngine] Failed to create cache directory:', error.message);
      this.cacheEnabled = false;
    }
  }

  _loadCacheMeta() {
    try {
      if (fs.existsSync(this.cacheMetaPath)) {
        const data = fs.readFileSync(this.cacheMetaPath, 'utf8');
        const meta = JSON.parse(data);
        this.lastLibraryModTime = meta.lastLibraryModTime || null;
        this.lastCacheRefreshTime = meta.lastCacheRefreshTime || null;
        console.log(`[ContentEngine] Loaded cache meta: lastRefresh=${this.lastCacheRefreshTime ? new Date(this.lastCacheRefreshTime).toISOString() : 'never'}`);
      } else {
        console.log('[ContentEngine] No cache meta found, will create on first refresh');
      }
    } catch (error) {
      console.warn('[ContentEngine] Failed to load cache meta:', error.message);
    }
  }

  _saveCacheMeta() {
    try {
      const meta = {
        lastLibraryModTime: this.lastLibraryModTime,
        lastCacheRefreshTime: this.lastCacheRefreshTime,
        updatedAt: Date.now()
      };
      fs.writeFileSync(this.cacheMetaPath, JSON.stringify(meta, null, 2), 'utf8');
      console.log('[ContentEngine] Saved cache meta');
    } catch (error) {
      console.warn('[ContentEngine] Failed to save cache meta:', error.message);
    }
  }

  _checkMusicLibraryUpdate() {
    try {
      if (!fs.existsSync(this.localJsonPath)) {
        console.log('[ContentEngine] Music library file not found, skip update check');
        return false;
      }

      const stats = fs.statSync(this.localJsonPath);
      const currentModTime = stats.mtimeMs;

      if (this.lastLibraryModTime === null) {
        console.log('[ContentEngine] First time checking library, recording modification time');
        this.lastLibraryModTime = currentModTime;
        this._saveCacheMeta();
        return false;
      }

      if (currentModTime > this.lastLibraryModTime) {
        const lastTime = new Date(this.lastLibraryModTime).toLocaleString('zh-CN');
        const currentTime = new Date(currentModTime).toLocaleString('zh-CN');
        console.log(`[ContentEngine] Music library updated! Last: ${lastTime}, Current: ${currentTime}`);
        
        this.lastLibraryModTime = currentModTime;
        this._refreshAllCaches();
        return true;
      }

      const daysSinceRefresh = this.lastCacheRefreshTime 
        ? Math.floor((Date.now() - this.lastCacheRefreshTime) / (24 * 60 * 60 * 1000))
        : this.cacheExpireDays;

      if (daysSinceRefresh >= this.cacheExpireDays) {
        console.log(`[ContentEngine] Cache expired (${daysSinceRefresh} days), refreshing...`);
        this._refreshAllCaches();
        return true;
      }

      console.log(`[ContentEngine] Music library not updated, cache valid (${daysSinceRefresh}/${this.cacheExpireDays} days)`);
      return false;
    } catch (error) {
      console.warn('[ContentEngine] Failed to check music library update:', error.message);
      return false;
    }
  }

  _refreshAllCaches() {
    console.log('[ContentEngine] ========== Starting Cache Refresh ==========');
    const startTime = Date.now();
    
    try {
      if (!fs.existsSync(this.cacheDir)) {
        console.log('[ContentEngine] Cache directory not found, nothing to refresh');
        this.lastCacheRefreshTime = Date.now();
        this._saveCacheMeta();
        return true;
      }

      const files = fs.readdirSync(this.cacheDir);
      const cacheFiles = files.filter(f => f.endsWith('.json') && f !== '.cache_meta.json');
      
      if (cacheFiles.length === 0) {
        console.log('[ContentEngine] No cache files to refresh');
        this.lastCacheRefreshTime = Date.now();
        this._saveCacheMeta();
        return true;
      }

      console.log(`[ContentEngine] Found ${cacheFiles.length} cache files to refresh`);
      
      let deletedCount = 0;
      let failedCount = 0;
      let totalSize = 0;

      cacheFiles.forEach((file, index) => {
        const progress = `[${index + 1}/${cacheFiles.length}]`;
        const filePath = path.join(this.cacheDir, file);
        
        try {
          const stats = fs.statSync(filePath);
          totalSize += stats.size;
          
          fs.unlinkSync(filePath);
          deletedCount++;
          console.log(`[ContentEngine] ${progress} Deleted: ${file} (${(stats.size / 1024).toFixed(2)} KB)`);
        } catch (error) {
          failedCount++;
          console.warn(`[ContentEngine] ${progress} Failed to delete ${file}:`, error.message);
        }
      });

      this.lastCacheRefreshTime = Date.now();
      this._saveCacheMeta();

      const duration = ((Date.now() - startTime) / 1000).toFixed(2);
      console.log('[ContentEngine] ========== Cache Refresh Complete ==========');
      console.log(`[ContentEngine] Duration: ${duration}s`);
      console.log(`[ContentEngine] Deleted: ${deletedCount} files (${(totalSize / 1024).toFixed(2)} KB)`);
      console.log(`[ContentEngine] Failed: ${failedCount} files`);
      console.log(`[ContentEngine] Next refresh in: ${this.cacheExpireDays} days`);

      return true;
    } catch (error) {
      console.error('[ContentEngine] Cache refresh failed:', error.message);
      return false;
    }
  }

  setLocalMusicModule(module) {
    this.localMusicModule = module;
    console.log('[ContentEngine] Local music module connected');
  }

  isTemplateScene(sceneType) {
    return TEMPLATE_SCENES.includes(sceneType);
  }

  getTemplateScenes() {
    return [...TEMPLATE_SCENES];
  }

  _loadLocalJsonDatabase() {
    try {
      if (fs.existsSync(this.localJsonPath)) {
        const data = fs.readFileSync(this.localJsonPath, 'utf8');
        const json = JSON.parse(data);
        this.localMusicJson = json.tracks || json;
        console.log(`[ContentEngine] Loaded local music database with ${this.localMusicJson.length} tracks from ${this.localJsonPath}`);
      } else {
        console.warn(`[ContentEngine] Local music database not found at ${this.localJsonPath}`);
      }
    } catch (error) {
      console.warn('[ContentEngine] Failed to load local music database:', error.message);
      this.localMusicJson = null;
    }
  }

  isLocalDbAvailable() {
    return this.localMusicJson !== null && this.localMusicJson.length > 0;
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

  getLocalDbStats() {
    if (!this.localMusicJson) {
      return { available: false };
    }
    
    const genres = [...new Set(this.localMusicJson.map(t => t.genre).filter(Boolean))];
    return {
      available: true,
      totalTracks: this.localMusicJson.length,
      genres
    };
  }

  getCacheStats() {
    try {
      if (!fs.existsSync(this.cacheDir)) {
        return {
          enabled: this.cacheEnabled,
          cacheDir: this.cacheDir,
          exists: false,
          cacheCount: 0,
          totalSize: 0,
          totalSizeMB: 0,
          lastRefreshTime: this.lastCacheRefreshTime,
          lastRefreshTimeStr: this.lastCacheRefreshTime 
            ? new Date(this.lastCacheRefreshTime).toLocaleString('zh-CN')
            : '从未刷新',
          lastLibraryModTime: this.lastLibraryModTime,
          lastLibraryModTimeStr: this.lastLibraryModTime
            ? new Date(this.lastLibraryModTime).toLocaleString('zh-CN')
            : '未知',
          cacheExpireDays: this.cacheExpireDays,
          daysUntilExpire: this.lastCacheRefreshTime
            ? Math.max(0, this.cacheExpireDays - Math.floor((Date.now() - this.lastCacheRefreshTime) / (24 * 60 * 60 * 1000)))
            : 0
        };
      }

      const files = fs.readdirSync(this.cacheDir);
      const cacheFiles = files.filter(f => f.endsWith('.json') && f !== '.cache_meta.json');
      
      let totalSize = 0;
      const cacheDetails = [];

      cacheFiles.forEach(file => {
        const filePath = path.join(this.cacheDir, file);
        const stats = fs.statSync(filePath);
        totalSize += stats.size;
        
        try {
          const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
          cacheDetails.push({
            file,
            size: stats.size,
            sizeKB: (stats.size / 1024).toFixed(2),
            timestamp: data.timestamp,
            age: data.timestamp ? Math.floor((Date.now() - data.timestamp) / (60 * 60 * 1000)) : null,
            trackCount: data.metadata?.trackCount || 0,
            sceneType: data.metadata?.sceneType || data.sceneType || 'unknown'
          });
        } catch (e) {
          cacheDetails.push({
            file,
            size: stats.size,
            sizeKB: (stats.size / 1024).toFixed(2),
            error: 'Failed to read cache'
          });
        }
      });

      return {
        enabled: this.cacheEnabled,
        cacheDir: this.cacheDir,
        exists: true,
        cacheCount: cacheFiles.length,
        totalSize,
        totalSizeKB: (totalSize / 1024).toFixed(2),
        totalSizeMB: (totalSize / 1024 / 1024).toFixed(2),
        lastRefreshTime: this.lastCacheRefreshTime,
        lastRefreshTimeStr: this.lastCacheRefreshTime 
          ? new Date(this.lastCacheRefreshTime).toLocaleString('zh-CN')
          : '从未刷新',
        lastLibraryModTime: this.lastLibraryModTime,
        lastLibraryModTimeStr: this.lastLibraryModTime
          ? new Date(this.lastLibraryModTime).toLocaleString('zh-CN')
          : '未知',
        cacheExpireDays: this.cacheExpireDays,
        daysUntilExpire: this.lastCacheRefreshTime
          ? Math.max(0, this.cacheExpireDays - Math.floor((Date.now() - this.lastCacheRefreshTime) / (24 * 60 * 60 * 1000)))
          : 0,
        cacheDetails
      };
    } catch (error) {
      return {
        enabled: this.cacheEnabled,
        error: error.message,
        cacheCount: 0,
        totalSize: 0
      };
    }
  }

  queryLocalMusic(hints = {}, constraints = {}) {
    if (!this.localMusicJson) {
      return { tracks: [], source: 'unavailable' };
    }

    try {
      let tracks = [...this.localMusicJson].map(t => ({
        ...t,
        mood_tags: parseTags(t.mood_tags),
        scene_tags: parseTags(t.scene_tags)
      }));

      tracks = tracks.map(t => ({
        ...t,
        _score: this._calculateOverallScore(t, hints)
      }));

      tracks = tracks.filter(t => t._score >= 40);

      tracks.sort((a, b) => b._score - a._score);

      if (hints.valence !== undefined) {
        const valence = hints.valence;
        tracks = tracks.filter(t => {
          const trackValence = t.valence !== undefined ? t.valence : 0.5;
          return Math.abs(trackValence - valence) <= 0.25;
        });
      }

      if (constraints.min_energy !== undefined) {
        tracks = tracks.filter(t => (t.energy || 0.5) >= constraints.min_energy);
      }

      if (constraints.max_duration !== undefined) {
        let totalDuration = 0;
        const selectedTracks = [];
        for (const track of tracks) {
          const duration = track.duration || track.duration_ms / 1000 || 180;
          if (totalDuration + duration <= constraints.max_duration) {
            selectedTracks.push(track);
            totalDuration += duration;
          }
        }
        tracks = selectedTracks;
      }

      const maxTracks = constraints.max_tracks || 20;
      tracks = this._ensureArtistDiversity(tracks, maxTracks);

      return { 
        tracks, 
        source: 'local', 
        count: tracks.length,
        avg_score: tracks.length > 0 
          ? Math.round(tracks.reduce((sum, t) => sum + t._score, 0) / tracks.length)
          : 0
      };
    } catch (error) {
      console.error('[ContentEngine] Local music query error:', error.message);
      return { tracks: [], source: 'error', error: error.message };
    }
  }

  _ensureArtistDiversity(tracks, maxTracks) {
    const result = [];
    const artistCount = {};
    const maxPerArtist = 3;
    
    for (const track of tracks) {
      if (result.length >= maxTracks) break;
      
      const artist = track.artist || 'Unknown';
      const count = artistCount[artist] || 0;
      
      if (count < maxPerArtist) {
        result.push(track);
        artistCount[artist] = count + 1;
      }
    }
    
    return result;
  }

  searchLocalMusic(keyword) {
    if (!this.localMusicJson) {
      return { tracks: [], source: 'unavailable' };
    }

    try {
      const lowerKeyword = keyword.toLowerCase();
      const tracks = this.localMusicJson.filter(t => 
        (t.title && t.title.toLowerCase().includes(lowerKeyword)) ||
        (t.artist && t.artist.toLowerCase().includes(lowerKeyword)) ||
        (t.album && t.album.toLowerCase().includes(lowerKeyword))
      ).slice(0, 20);

      return { tracks, source: 'local', count: tracks.length, keyword };
    } catch (error) {
      console.error('[ContentEngine] Local music search error:', error.message);
      return { tracks: [], source: 'error', error: error.message };
    }
  }

  _getCachePath(templateId) {
    return path.join(this.cacheDir, `${templateId}.json`);
  }

  _isCacheValid(cacheData) {
    if (!cacheData || !cacheData.timestamp) return false;
    return Date.now() - cacheData.timestamp < this.cacheTTL;
  }

  _loadCachedPlaylist(templateId) {
    if (!this.cacheEnabled || !templateId) {
      return null;
    }

    try {
      const cachePath = this._getCachePath(templateId);
      
      if (!fs.existsSync(cachePath)) {
        console.log(`[ContentEngine] No cache found for templateId: ${templateId}`);
        return null;
      }

      const cacheData = JSON.parse(fs.readFileSync(cachePath, 'utf8'));
      
      if (!this._isCacheValid(cacheData)) {
        console.log(`[ContentEngine] Cache expired for templateId: ${templateId}`);
        return null;
      }

      console.log(`[ContentEngine] Loaded cached playlist for templateId: ${templateId}`);
      return {
        playlist: cacheData.playlist,
        metadata: cacheData.metadata,
        source: 'cache',
        sceneMatch: {
          type: 'cache',
          templateId,
          scene: cacheData.metadata?.sceneType,
          matchScore: 1.0,
          matchMethod: 'cache_hit',
          cachedAt: cacheData.timestamp
        }
      };
    } catch (error) {
      console.warn(`[ContentEngine] Failed to load cache for ${templateId}:`, error.message);
      return null;
    }
  }

  _loadSceneCache(sceneType) {
    if (!this.cacheEnabled || !sceneType) {
      return null;
    }

    try {
      const sceneCachePath = path.join(this.cacheDir, `scene_${sceneType}.json`);
      
      if (!fs.existsSync(sceneCachePath)) {
        console.log(`[ContentEngine] No scene cache found for sceneType: ${sceneType}`);
        return null;
      }

      const cacheData = JSON.parse(fs.readFileSync(sceneCachePath, 'utf8'));
      
      if (!this._isCacheValid(cacheData)) {
        console.log(`[ContentEngine] Scene cache expired for sceneType: ${sceneType}`);
        return null;
      }

      console.log(`[ContentEngine] Loaded scene cache for sceneType: ${sceneType}`);
      return {
        playlist: cacheData.playlist,
        metadata: cacheData.metadata,
        source: 'cache',
        sceneMatch: {
          type: 'scene_cache',
          scene: sceneType,
          matchScore: 0.95,
          matchMethod: 'scene_cache_hit',
          cachedAt: cacheData.timestamp
        }
      };
    } catch (error) {
      console.warn(`[ContentEngine] Failed to load scene cache for ${sceneType}:`, error.message);
      return null;
    }
  }

  _savePlaylistCache(templateId, sceneType, playlist, metadata = {}) {
    if (!this.cacheEnabled) {
      return false;
    }

    try {
      const cacheData = {
        templateId,
        sceneType,
        playlist,
        metadata: {
          ...metadata,
          sceneType,
          trackCount: playlist.length,
          totalDuration: playlist.reduce((sum, t) => sum + (t.duration || 0), 0)
        },
        timestamp: Date.now(),
        version: '1.0'
      };

      if (templateId) {
        const cachePath = this._getCachePath(templateId);
        fs.writeFileSync(cachePath, JSON.stringify(cacheData, null, 2), 'utf8');
        console.log(`[ContentEngine] Saved playlist cache for templateId: ${templateId}`);
      }

      if (sceneType) {
        const sceneCachePath = path.join(this.cacheDir, `scene_${sceneType}.json`);
        fs.writeFileSync(sceneCachePath, JSON.stringify(cacheData, null, 2), 'utf8');
        console.log(`[ContentEngine] Saved scene cache for sceneType: ${sceneType}`);
      }

      return true;
    } catch (error) {
      console.warn('[ContentEngine] Failed to save playlist cache:', error.message);
      return false;
    }
  }

  clearCache(templateId = null, sceneType = null) {
    try {
      if (templateId) {
        const cachePath = this._getCachePath(templateId);
        if (fs.existsSync(cachePath)) {
          fs.unlinkSync(cachePath);
          console.log(`[ContentEngine] Cleared cache for templateId: ${templateId}`);
        }
      }

      if (sceneType) {
        const sceneCachePath = path.join(this.cacheDir, `scene_${sceneType}.json`);
        if (fs.existsSync(sceneCachePath)) {
          fs.unlinkSync(sceneCachePath);
          console.log(`[ContentEngine] Cleared scene cache for sceneType: ${sceneType}`);
        }
      }

      if (!templateId && !sceneType) {
        const files = fs.readdirSync(this.cacheDir);
        for (const file of files) {
          if (file.endsWith('.json')) {
            fs.unlinkSync(path.join(this.cacheDir, file));
          }
        }
        console.log('[ContentEngine] Cleared all playlist caches');
      }

      return true;
    } catch (error) {
      console.warn('[ContentEngine] Failed to clear cache:', error.message);
      return false;
    }
  }

  async curatePlaylist(hints = {}, constraints = {}, sceneType = null, templateId = null) {
    console.log(`[ContentEngine] curatePlaylist called with sceneType: ${sceneType || 'unknown'}, templateId: ${templateId || 'none'}`);
    
    let playlist = [];
    let source = 'fallback';
    let sceneMatch = null;

    // 策略0: 缓存优先 - 如果提供 templateId，优先使用缓存
    if (templateId) {
      console.log(`[ContentEngine] 策略0: 尝试加载 templateId 缓存: ${templateId}`);
      const cachedResult = this._loadCachedPlaylist(templateId);
      if (cachedResult) {
        console.log(`[ContentEngine] 缓存命中: ${cachedResult.playlist.length} tracks`);
        this.currentPlaylist = cachedResult.playlist;
        return {
          playlist: cachedResult.playlist,
          total_duration: cachedResult.metadata?.totalDuration || 0,
          avg_energy: cachedResult.playlist.reduce((sum, t) => sum + (t.energy || 0), 0) / cachedResult.playlist.length,
          source: 'cache',
          scene_match: cachedResult.sceneMatch,
          localDbAvailable: this.isLocalDbAvailable(),
          strategy: '缓存命中：模板缓存快速响应',
          templateId
        };
      }
    }

    // 策略0.5: 如果只提供 sceneType，查找场景类型缓存
    if (!templateId && sceneType) {
      console.log(`[ContentEngine] 策略0.5: 尝试加载 sceneType 缓存: ${sceneType}`);
      const sceneCacheResult = this._loadSceneCache(sceneType);
      if (sceneCacheResult) {
        console.log(`[ContentEngine] 场景缓存命中: ${sceneCacheResult.playlist.length} tracks`);
        this.currentPlaylist = sceneCacheResult.playlist;
        return {
          playlist: sceneCacheResult.playlist,
          total_duration: sceneCacheResult.metadata?.totalDuration || 0,
          avg_energy: sceneCacheResult.playlist.reduce((sum, t) => sum + (t.energy || 0), 0) / sceneCacheResult.playlist.length,
          source: 'cache',
          scene_match: sceneCacheResult.sceneMatch,
          localDbAvailable: this.isLocalDbAvailable(),
          strategy: '缓存命中：场景类型缓存快速响应'
        };
      }
    }

    const isTemplate = this.isTemplateScene(sceneType);
    console.log(`[ContentEngine] Scene type: ${sceneType}, isTemplate: ${isTemplate}`);

    // 策略1: 模板场景 - 本地 JSON 匹配
    if (isTemplate && this.localMusicJson) {
      console.log('[ContentEngine] 策略1: 模板场景 - 本地 JSON 匹配');
      
      const localResult = this.queryLocalMusic({
        ...hints,
        scene: sceneType ? [sceneType] : undefined
      });
      
      if (localResult.tracks.length > 0) {
        playlist = localResult.tracks;
        source = 'local';
        sceneMatch = {
          type: 'template',
          scene: sceneType,
          matchScore: 1.0,
          matchMethod: 'json_query'
        };
        console.log(`[ContentEngine] 本地匹配成功: ${playlist.length} tracks`);
      } else {
        console.log('[ContentEngine] 本地匹配无结果，尝试宽松匹配...');
        
        const relaxedResult = this.queryLocalMusic({
          genres: hints.genres,
          energy_level: hints.energy_level
        });
        
        if (relaxedResult.tracks.length > 0) {
          playlist = relaxedResult.tracks;
          source = 'local';
          sceneMatch = {
            type: 'template_relaxed',
            scene: sceneType,
            matchScore: 0.7,
            matchMethod: 'relaxed_query'
          };
          console.log(`[ContentEngine] 宽松匹配成功: ${playlist.length} tracks`);
        }
      }
    }

    // 策略2: 非模板场景 - LLM 智能选取（只从本地音乐库选取）
    if (playlist.length === 0 && this.llmClient && this.config.enableLLM && this.localMusicJson) {
      console.log('[ContentEngine] 策略2: 非模板场景 - LLM 智能选取');
      
      try {
        const dbContext = this._buildDbContext(hints);
        playlist = await this._generatePlaylistWithLLMWithContext(hints, constraints, dbContext, sceneType);
        source = 'llm';
        sceneMatch = {
          type: 'llm',
          scene: sceneType,
          matchScore: 0.8,
          matchMethod: 'llm_selection',
          dbStats: dbContext
        };
        console.log(`[ContentEngine] LLM 选取成功: ${playlist.length} tracks`);
      } catch (error) {
        console.warn('[ContentEngine] LLM 选取失败:', error.message);
      }
    }

    // 策略3: 兜底场景 - 预设固定清单
    if (playlist.length === 0) {
      console.log('[ContentEngine] 策略3: 兜底场景 - 预设固定清单');
      
      playlist = this._getFallbackPlaylist(hints);
      source = 'fallback';
      sceneMatch = {
        type: 'fallback',
        scene: sceneType,
        matchScore: 0.5,
        matchMethod: 'preset_fallback'
      };
      console.log(`[ContentEngine] 使用兜底清单: ${playlist.length} tracks`);
    }

    const playlistSize = constraints.max_tracks || 10;
    playlist = playlist.slice(0, playlistSize).map((t, index) => ({
      ...t,
      id: t.id || `track-${Date.now()}-${index}`,
      added_at: Date.now()
    }));

    // 保存缓存
    if (source !== 'cache' && playlist.length > 0) {
      this._savePlaylistCache(templateId, sceneType, playlist, {
        source,
        hints,
        constraints
      });
    }

    this.currentPlaylist = playlist;

    const totalDuration = playlist.reduce((sum, t) => sum + (t.duration || 0), 0);

    return {
      playlist,
      total_duration: totalDuration,
      avg_energy: playlist.reduce((sum, t) => sum + (t.energy || 0), 0) / (playlist.length || 1),
      source,
      scene_match: sceneMatch,
      localDbAvailable: this.isLocalDbAvailable(),
      strategy: this._getStrategyDescription(source)
    };
  }

  _getStrategyDescription(source) {
    const descriptions = {
      cache: '缓存命中：快速响应，无需重新计算',
      local: '模板场景：本地 JSON 匹配，快速启播',
      llm: '非模板场景：LLM 智能选取，个性化歌单',
      fallback: '兜底场景：预设固定清单，备选方案'
    };
    return descriptions[source] || '未知策略';
  }

  _buildDbContext(hints = {}) {
    if (!this.localMusicJson) {
      return { available: false, totalTracks: 0 };
    }

    const stats = this.getLocalDbStats();
    
    let candidateTracks = [...this.localMusicJson];
    
    if (hints.genres && hints.genres.length > 0) {
      candidateTracks = candidateTracks.filter(t => hints.genres.includes(t.genre));
    }
    
    if (hints.energy_level !== undefined) {
      const energy = hints.energy_level;
      candidateTracks = candidateTracks.filter(t => Math.abs((t.energy || 0.5) - energy) <= 0.3);
    }

    if (hints.valence !== undefined) {
      candidateTracks = candidateTracks.filter(t => {
        const trackValence = t.valence !== undefined ? t.valence : 0.5;
        return Math.abs(trackValence - hints.valence) <= 0.3;
      });
    }
    
    candidateTracks = candidateTracks.slice(0, 50);
    
    const sampleTracks = candidateTracks.map(t => ({
      id: t.id,
      title: t.title,
      artist: t.artist,
      genre: t.genre,
      bpm: t.bpm,
      energy: t.energy,
      valence: t.valence,
      duration: Math.floor((t.duration_ms || 0) / 1000),
      filePath: t.file_path,
      mood_tags: parseTags(t.mood_tags),
      scene_tags: parseTags(t.scene_tags)
    }));

    return {
      available: true,
      ...stats,
      candidateCount: candidateTracks.length,
      sampleTracks
    };
  }

  async _generatePlaylistWithLLMWithContext(hints, constraints, dbContext, sceneType) {
    if (!this.llmClient || !this.llmClient.isReady()) {
      throw new Error('LLM client not initialized or API key missing');
    }

    const systemPrompt = `你是一个专业的车载音乐推荐专家。
请根据用户提供的音乐偏好，从给定的候选曲目列表中选取最适合的歌曲。

【重要规则】
1. 你只能从下面提供的候选曲目列表中选取歌曲，绝对不能虚构或推荐列表之外的歌曲
2. 如果列表中没有完全匹配的歌曲，选择最接近的歌曲
3. 返回的每首歌曲必须包含完整的原始信息

候选曲目列表（共 ${dbContext.candidateCount} 首，已按用户偏好预筛选）：
${JSON.stringify(dbContext.sampleTracks, null, 2)}

必须返回 JSON 格式，包含一个 playlist 数组，每个元素必须是上面列表中的歌曲：
{ "playlist": [{ "id": 1, "title": "...", "artist": "...", "genre": "...", "bpm": 120, "energy": 0.8, "duration": 200, "filePath": "..." }] }

只输出 JSON，不要其他文字。`;

    const userPrompt = `
场景类型: ${sceneType || '未知'}
音乐偏好: ${JSON.stringify(hints)}
约束条件: ${JSON.stringify(constraints)}

请从本地音乐库中选取最适合的歌曲，返回 JSON 格式：
{
  "playlist": [
    { 
      "id": "...", 
      "title": "...", 
      "artist": "...", 
      "genre": "...", 
      "bpm": 120, 
      "energy": 0.8, 
      "duration": 200,
      "filePath": "...",
      "scene_match": "..."
    }
  ]
}`;

    const response = await this.llmClient.chat([
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt }
    ], {
      model: this.config.model || Models.QWEN_FLASH,
      temperature: 0.7,
      maxTokens: 2000
    }, { enableThinking: false });

    const content = response.choices[0].message.content;
    
    try {
      const jsonMatch = content.match(/```json\n([\s\S]*?)\n```/) || content.match(/\{[\s\S]*\}/);
      const jsonStr = jsonMatch ? (jsonMatch[1] || jsonMatch[0]) : content;
      const result = JSON.parse(jsonStr);
      
      if (result && Array.isArray(result.playlist)) {
        return result.playlist;
      }
      throw new Error('Invalid playlist format');
    } catch (error) {
      console.error('[ContentEngine] Failed to parse LLM response:', error);
      throw error;
    }
  }

  _getFallbackPlaylist(hints = {}) {
    let playlist = [...FALLBACK_PLAYLIST];

    if (hints.energy_level !== undefined) {
      const targetEnergy = hints.energy_level;
      const tolerance = 0.3;
      const filtered = playlist.filter(t => Math.abs(t.energy - targetEnergy) <= tolerance);
      if (filtered.length > 0) {
        playlist = filtered;
      }
    }

    if (hints.valence !== undefined) {
      const targetValence = hints.valence;
      const tolerance = 0.3;
      const filtered = playlist.filter(t => Math.abs(t.valence - targetValence) <= tolerance);
      if (filtered.length > 0) {
        playlist = filtered;
      }
    }

    if (hints.genres && hints.genres.length > 0) {
      const filtered = playlist.filter(t => hints.genres.includes(t.genre));
      if (filtered.length > 0) {
        playlist = filtered;
      }
    }

    if (hints.mood && hints.mood.length > 0) {
      const filtered = playlist.filter(t => {
        const trackMoods = t.mood_tags || [];
        return hints.mood.some(m => trackMoods.includes(m));
      });
      if (filtered.length > 0) {
        playlist = filtered;
      }
    }

    if (hints.scene && hints.scene.length > 0) {
      const filtered = playlist.filter(t => {
        const trackScenes = t.scene_tags || [];
        return hints.scene.some(s => trackScenes.includes(s));
      });
      if (filtered.length > 0) {
        playlist = filtered;
      }
    }

    return playlist.sort(() => Math.random() - 0.5);
  }

  execute(action, params) {
    switch (action) {
    case 'curate_playlist':
      return this.curatePlaylist(params.hints, params.constraints, params.scene_type, params.template_id);
    case 'clear_cache':
      return this.clearCache(params.template_id, params.scene_type);
    case 'get_cache_stats':
      return this.getCacheStats();
    case 'refresh_cache':
      return this._refreshAllCaches();
    case 'check_library_update':
      return this._checkMusicLibraryUpdate();
    default:
      throw new Error(`Unknown action: ${action}`);
    }
  }
}

const contentEngine = new ContentEngine({
  apiKey: process.env.DASHSCOPE_API_KEY,
  localJsonPath: process.env.LOCAL_MUSIC_DB || './data/index.json'
});

module.exports = { ContentEngine, contentEngine };

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
  'romantic_date', 'family_trip', 'focus_work', 'relax',
  'workout', 'rainy_day', 'traffic_jam', 'fatigue_alert'
];

const FALLBACK_PLAYLIST = [
  { id: 'fallback_001', title: '晴天', artist: '周杰伦', genre: 'pop', bpm: 100, energy: 0.6, duration: 269, mood: 'happy' },
  { id: 'fallback_002', title: '夜曲', artist: '周杰伦', genre: 'ballad', bpm: 80, energy: 0.4, duration: 226, mood: 'melancholy' },
  { id: 'fallback_003', title: '七里香', artist: '周杰伦', genre: 'pop', bpm: 90, energy: 0.5, duration: 299, mood: 'romantic' },
  { id: 'fallback_004', title: '稻香', artist: '周杰伦', genre: 'pop', bpm: 85, energy: 0.55, duration: 223, mood: 'happy' },
  { id: 'fallback_005', title: '告白气球', artist: '周杰伦', genre: 'pop', bpm: 95, energy: 0.5, duration: 215, mood: 'romantic' },
  { id: 'fallback_006', title: '简单爱', artist: '周杰伦', genre: 'pop', bpm: 100, energy: 0.6, duration: 267, mood: 'happy' },
  { id: 'fallback_007', title: '安静', artist: '周杰伦', genre: 'ballad', bpm: 70, energy: 0.3, duration: 342, mood: 'sad' },
  { id: 'fallback_008', title: '龙卷风', artist: '周杰伦', genre: 'pop', bpm: 110, energy: 0.65, duration: 253, mood: 'energetic' },
  { id: 'fallback_009', title: '以父之名', artist: '周杰伦', genre: 'pop', bpm: 90, energy: 0.5, duration: 341, mood: 'epic' },
  { id: 'fallback_010', title: '听妈妈的话', artist: '周杰伦', genre: 'pop', bpm: 85, energy: 0.45, duration: 241, mood: 'warm' }
];

class ContentEngine {
  constructor(config = {}) {
    this.config = config;
    this.currentPlaylist = [];
    this.localMusicJson = null;
    this.localMusicModule = null;
    
    this.localJsonPath = config.localJsonPath || process.env.LOCAL_MUSIC_DB || '/sdcard/Music/AiMusic/index.json';
    
    this._loadLocalJsonDatabase();
    
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

  queryLocalMusic(hints = {}) {
    if (!this.localMusicJson) {
      return { tracks: [], source: 'unavailable' };
    }

    try {
      let tracks = [...this.localMusicJson].map(t => ({
        ...t,
        mood_tags: typeof t.mood_tags === 'string' ? JSON.parse(t.mood_tags) : (t.mood_tags || []),
        scene_tags: typeof t.scene_tags === 'string' ? JSON.parse(t.scene_tags) : (t.scene_tags || [])
      }));

      if (hints.genres && hints.genres.length > 0) {
        tracks = tracks.filter(t => hints.genres.includes(t.genre));
      }

      if (hints.tempo && TempoRange[hints.tempo]) {
        const [minBpm, maxBpm] = TempoRange[hints.tempo];
        tracks = tracks.filter(t => t.bpm >= minBpm && t.bpm <= maxBpm);
      }

      if (hints.energy_level !== undefined) {
        const energy = hints.energy_level;
        tracks = tracks.filter(t => Math.abs((t.energy || 0.5) - energy) <= 0.25);
      }

      if (hints.mood && hints.mood.length > 0) {
        tracks = tracks.filter(t => {
          const trackMoods = t.mood_tags || [];
          return hints.mood.some(m => trackMoods.includes(m));
        });
      }

      if (hints.scene && hints.scene.length > 0) {
        tracks = tracks.filter(t => {
          const trackScenes = t.scene_tags || [];
          return hints.scene.some(s => trackScenes.includes(s));
        });
      }

      tracks = tracks.sort(() => Math.random() - 0.5).slice(0, 20);

      return { tracks, source: 'local', count: tracks.length };
    } catch (error) {
      console.error('[ContentEngine] Local music query error:', error.message);
      return { tracks: [], source: 'error', error: error.message };
    }
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

  async curatePlaylist(hints = {}, constraints = {}, sceneType = null) {
    console.log(`[ContentEngine] curatePlaylist called with sceneType: ${sceneType || 'unknown'}`);
    
    let playlist = [];
    let source = 'fallback';
    let sceneMatch = null;

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
        console.log(`[ContentEngine] 本地匹配无结果，尝试宽松匹配...`);
        
        // 宽松匹配：只按流派和能量匹配
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
    
    // 预筛选：只传递符合基本条件的曲目（最多 50 首）
    let candidateTracks = [...this.localMusicJson];
    
    // 按流派筛选
    if (hints.genres && hints.genres.length > 0) {
      candidateTracks = candidateTracks.filter(t => hints.genres.includes(t.genre));
    }
    
    // 按能量范围筛选
    if (hints.energy_level !== undefined) {
      const energy = hints.energy_level;
      candidateTracks = candidateTracks.filter(t => Math.abs((t.energy || 0.5) - energy) <= 0.3);
    }
    
    // 限制最多 50 首
    candidateTracks = candidateTracks.slice(0, 50);
    
    const sampleTracks = candidateTracks.map(t => ({
      id: t.id,
      title: t.title,
      artist: t.artist,
      genre: t.genre,
      bpm: t.bpm,
      energy: t.energy,
      duration: Math.floor((t.duration_ms || 0) / 1000),
      filePath: t.file_path
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
      const jsonMatch = content.match(/\`\`\`json\n([\s\S]*?)\n\`\`\`/) || content.match(/\{[\s\S]*\}/);
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

    if (hints.genres && hints.genres.length > 0) {
      const filtered = playlist.filter(t => hints.genres.includes(t.genre));
      if (filtered.length > 0) {
        playlist = filtered;
      }
    }

    return playlist.sort(() => Math.random() - 0.5);
  }

  execute(action, params) {
    switch (action) {
      case 'curate_playlist':
        return this.curatePlaylist(params.hints, params.constraints, params.scene_type);
      default:
        throw new Error(`Unknown action: ${action}`);
    }
  }
}

module.exports = { ContentEngine, contentEngine: new ContentEngine() };

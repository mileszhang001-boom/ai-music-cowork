'use strict';

const fs = require('fs');
const path = require('path');
const { LLMClient, Models } = require('../../../../core/llm');

const MockTracks = [
  { id: 1, title: '清晨阳光', artist: '自然之声', genre: 'ambient', bpm: 80, energy: 0.3, duration: 240 },
  { id: 2, title: '城市节拍', artist: '电子先锋', genre: 'electronic', bpm: 120, energy: 0.7, duration: 180 },
  { id: 3, title: '深夜爵士', artist: '爵士三重奏', genre: 'jazz', bpm: 90, energy: 0.4, duration: 300 },
  { id: 4, title: '公路旅行', artist: '摇滚乐队', genre: 'rock', bpm: 130, energy: 0.8, duration: 210 },
  { id: 5, title: '雨中漫步', artist: '钢琴诗人', genre: 'classical', bpm: 70, energy: 0.2, duration: 270 },
  { id: 6, title: '派对时刻', artist: 'DJ Max', genre: 'electronic', bpm: 140, energy: 0.9, duration: 195 },
  { id: 7, title: '温柔晚安', artist: '轻音乐团', genre: 'lo-fi', bpm: 65, energy: 0.2, duration: 255 },
  { id: 8, title: '周末狂欢', artist: '流行天团', genre: 'pop', bpm: 125, energy: 0.75, duration: 200 }
];

class ContentEngine {
  constructor(config = {}) {
    this.config = config;
    this.currentPlaylist = [];
    this.trackLibrary = MockTracks;
    this.musicLibrary = null;
    this.libraryPath = config.libraryPath || path.join(__dirname, '../../../../../data/music_library.json');
    
    this._loadMusicLibrary();
    
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

  _loadMusicLibrary() {
    try {
      if (fs.existsSync(this.libraryPath)) {
        const data = fs.readFileSync(this.libraryPath, 'utf8');
        this.musicLibrary = JSON.parse(data);
        console.log(`[ContentEngine] Loaded music library with ${this._getTotalTracks()} tracks`);
      }
    } catch (error) {
      console.warn('[ContentEngine] Failed to load music library:', error.message);
      this.musicLibrary = null;
    }
  }

  _getTotalTracks() {
    if (!this.musicLibrary || !this.musicLibrary.scenes) return 0;
    return Object.values(this.musicLibrary.scenes).reduce((sum, scene) => sum + (scene.tracks?.length || 0), 0);
  }

  getTracksByScene(sceneType) {
    if (!this.musicLibrary || !this.musicLibrary.scenes) return [];
    
    const scene = this.musicLibrary.scenes[sceneType];
    if (!scene || !scene.tracks) return [];
    
    return scene.tracks;
  }

  getAvailableScenes() {
    if (!this.musicLibrary || !this.musicLibrary.scenes) return [];
    return Object.keys(this.musicLibrary.scenes);
  }

  _searchLibraryByHints(hints = {}, constraints = {}) {
    if (!this.musicLibrary || !this.musicLibrary.scenes) return [];

    const allTracks = [];
    const seenIds = new Set();
    
    Object.values(this.musicLibrary.scenes).forEach(scene => {
      if (scene.tracks) {
        scene.tracks.forEach(track => {
          if (!seenIds.has(track.id)) {
            seenIds.add(track.id);
            allTracks.push({ ...track, _matchScore: 0 });
          }
        });
      }
    });

    allTracks.forEach(track => {
      let score = 0;
      
      if (hints.genres && hints.genres.length > 0) {
        if (hints.genres.includes(track.genre)) {
          score += 30;
        }
      }
      
      if (hints.tempo) {
        const tempoRange = {
          slow: [60, 90],
          medium: [90, 120],
          moderate: [90, 120],
          fast: [120, 160],
          upbeat: [120, 160]
        };
        const range = tempoRange[hints.tempo] || [60, 160];
        if (track.bpm >= range[0] && track.bpm <= range[1]) {
          score += 25;
        }
      }
      
      if (hints.energy_level !== undefined) {
        const energyDiff = Math.abs(track.energy - hints.energy_level);
        if (energyDiff <= 0.1) {
          score += 25;
        } else if (energyDiff <= 0.2) {
          score += 15;
        } else if (energyDiff <= 0.3) {
          score += 5;
        }
      }
      
      if (hints.language) {
        if (Array.isArray(hints.language)) {
          if (hints.language.includes(track.language)) {
            score += 20;
          }
        } else if (track.language === hints.language) {
          score += 20;
        }
      }
      
      if (hints.vocal_style === 'instrumental' && track.language === 'instrumental') {
        score += 15;
      }
      
      // 新增：标签匹配
      score += this._matchByTags(track, hints);
      
      track._matchScore = score;
    });

    const matchedTracks = allTracks
      .filter(t => t._matchScore > 0)
      .sort((a, b) => b._matchScore - a._matchScore);

    return matchedTracks;
  }

  _matchByTags(track, hints) {
    let score = 0;
    
    // 情绪标签匹配
    if (hints.mood && track.mood_tags?.includes(hints.mood)) {
      score += 25;
    }
    if (hints.moods && Array.isArray(hints.moods)) {
      const matchCount = hints.moods.filter(m => track.mood_tags?.includes(m)).length;
      score += matchCount * 15;
    }
    
    // 活动标签匹配
    if (hints.activity && track.activity_tags?.includes(hints.activity)) {
      score += 20;
    }
    if (hints.activities && Array.isArray(hints.activities)) {
      const matchCount = hints.activities.filter(a => track.activity_tags?.includes(a)).length;
      score += matchCount * 12;
    }
    
    // 天气标签匹配
    if (hints.weather && track.weather_tags?.includes(hints.weather)) {
      score += 15;
    }
    
    // 时间标签匹配
    if (hints.time && track.time_tags?.includes(hints.time)) {
      score += 15;
    }
    if (hints.time_of_day !== undefined) {
      const timeTags = this._timeToTags(hints.time_of_day);
      const matchCount = timeTags.filter(t => track.time_tags?.includes(t)).length;
      score += matchCount * 10;
    }
    
    // 场景标签匹配
    if (hints.scene && track.scene_tags?.includes(hints.scene)) {
      score += 20;
    }
    
    // Valence-Arousal 匹配
    if (hints.valence !== undefined && track.valence !== undefined) {
      const valenceDiff = Math.abs(track.valence - hints.valence);
      score += Math.round((1 - valenceDiff) * 15);
    }
    if (hints.arousal !== undefined && track.energy !== undefined) {
      const arousalDiff = Math.abs(track.energy - hints.arousal);
      score += Math.round((1 - arousalDiff) * 15);
    }
    
    // 氛围匹配
    if (hints.atmosphere && track.mood_tags) {
      const atmosphereMap = {
        'fresh_morning': ['happy', 'hopeful', 'energetic'],
        'serene_night': ['calm', 'peaceful', 'melancholy'],
        'energetic_road_trip': ['energetic', 'happy', 'uplifting'],
        'romantic_evening': ['romantic', 'peaceful'],
        'cozy_rain': ['calm', 'melancholy', 'peaceful'],
        'focus_work': ['calm', 'peaceful'],
        'party_mode': ['energetic', 'happy', 'uplifting'],
        'meditation': ['peaceful', 'calm']
      };
      const expectedMoods = atmosphereMap[hints.atmosphere] || [];
      const matchCount = expectedMoods.filter(m => track.mood_tags.includes(m)).length;
      score += matchCount * 10;
    }
    
    return score;
  }

  _timeToTags(timeOfDay) {
    if (timeOfDay < 0.25) return ['late_night', 'night'];
    if (timeOfDay < 0.4) return ['morning'];
    if (timeOfDay < 0.6) return ['afternoon'];
    if (timeOfDay < 0.75) return ['evening'];
    return ['night', 'late_night'];
  }

  _buildResult(playlist, source) {
    const playlistSize = this.config.maxTracks || 10;
    const finalPlaylist = playlist.slice(0, playlistSize).map((t, index) => ({
      ...t,
      id: t.id || `track-${Date.now()}-${index}`,
      added_at: Date.now()
    }));

    this.currentPlaylist = finalPlaylist;

    const totalDuration = finalPlaylist.reduce((sum, t) => sum + (t.duration || 0), 0);

    return {
      playlist: finalPlaylist,
      total_duration: totalDuration,
      avg_energy: finalPlaylist.reduce((sum, t) => sum + (t.energy || 0), 0) / (finalPlaylist.length || 1),
      source
    };
  }

  async execute(action, params = {}) {
    switch (action) {
      case 'curate_playlist':
        return await this.curatePlaylist(params.hints, params.constraints, params.scene_type);
      case 'get_current':
        return { playlist: this.currentPlaylist };
      case 'get_scenes':
        return { scenes: this.getAvailableScenes() };
      case 'get_scene_tracks':
        return { tracks: this.getTracksByScene(params.scene_type) };
      default:
        return { error: `Unknown action: ${action}` };
    }
  }

  async _generatePlaylistWithLLM(hints, constraints) {
    if (!this.llmClient || !this.llmClient.isReady()) {
      throw new Error('LLM client not initialized or API key missing');
    }

    const systemPrompt = `你是一个专业的车载音乐推荐专家。
请根据用户提供的音乐偏好和约束条件，模拟全网搜索，推荐符合场景的中英文歌曲和纯音乐。
必须返回 JSON 格式，包含一个 playlist 数组，每个元素必须包含以下字段：
- id: 唯一标识符
- title: 歌曲名称
- artist: 歌手/创作者
- genre: 音乐流派
- bpm: 节拍数
- energy: 能量值 (0.0 - 1.0)
- duration: 时长（秒）
- expression: 歌曲的表达内容和情感
- user_feedback: 模拟的用户反馈或乐评
- scene_match: 解释为什么这首歌与当前场景匹配

不要输出任何其他解释文字，只输出合法的 JSON。`;

    const userPrompt = `
Hints: ${JSON.stringify(hints)}
Constraints: ${JSON.stringify(constraints)}

请生成推荐歌单，返回 JSON 格式：
{
  "playlist": [
    { 
      "id": "track_001", 
      "title": "...", 
      "artist": "...", 
      "genre": "...", 
      "bpm": 120, 
      "energy": 0.8, 
      "duration": 200,
      "expression": "...",
      "user_feedback": "...",
      "scene_match": "..."
    }
  ]
}`;

    const response = await this.llmClient.chat([
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt }
    ], {
      model: this.config.model || Models.QWEN_PLUS,
      temperature: 0.7
    });

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

  async curatePlaylist(hints = {}, constraints = {}, sceneType = null) {
    let playlist = [];
    let source = 'mock';

    const availableScenes = this.getAvailableScenes();
    const isPresetScene = sceneType && availableScenes.includes(sceneType);

    // 步骤1: 模板场景 - 直接从曲库获取，快速启播
    if (isPresetScene && this.musicLibrary) {
      const sceneTracks = this.getTracksByScene(sceneType);
      if (sceneTracks.length > 0) {
        playlist = this._filterTracksByHints(sceneTracks, hints, constraints);
        source = 'library';
        console.log(`[ContentEngine] Using library tracks for preset scene: ${sceneType}`);
        return this._buildResult(playlist, source);
      }
    }

    // 步骤2: 非模板场景 - 先从曲库智能检索
    const libraryMatches = this._searchLibraryByHints(hints, constraints);
    const minTracks = constraints.min_tracks || 5;
    
    if (libraryMatches.length >= minTracks) {
      playlist = libraryMatches;
      source = 'library_search';
      console.log(`[ContentEngine] Found ${libraryMatches.length} tracks from library search`);
      return this._buildResult(playlist, source);
    }

    // 步骤3: 曲库匹配不足 - 调用 LLM 联网搜索
    if (this.llmClient && this.config.enableLLM) {
      try {
        console.log(`[ContentEngine] Library matched ${libraryMatches.length} tracks (< ${minTracks}), calling LLM for more`);
        const llmPlaylist = await this._generatePlaylistWithLLM(hints, constraints);
        
        // 合并曲库匹配和 LLM 生成的结果
        const llmIds = new Set(llmPlaylist.map(t => t.id));
        const uniqueLibrary = libraryMatches.filter(t => !llmIds.has(t.id));
        playlist = [...uniqueLibrary.slice(0, 3), ...llmPlaylist];
        source = 'llm';
        console.log(`[ContentEngine] Combined ${uniqueLibrary.length} library + ${llmPlaylist.length} LLM tracks`);
      } catch (error) {
        console.warn('[ContentEngine] LLM generation failed, falling back to library:', error.message);
        if (libraryMatches.length > 0) {
          playlist = libraryMatches;
          source = 'library_search';
        }
      }
    }

    // 步骤4: 兜底 - 使用 mock 数据
    if (playlist.length === 0) {
      playlist = this._filterTracksByHints(this.trackLibrary, hints, constraints);
      source = 'mock';
      console.log(`[ContentEngine] Using mock data (${playlist.length} tracks)`);
    }

    return this._buildResult(playlist, source);
  }

  _filterTracksByHints(tracks, hints = {}, constraints = {}) {
    let filtered = [...tracks];

    if (hints.genres && hints.genres.length > 0) {
      filtered = filtered.filter(t => hints.genres.includes(t.genre));
    }

    if (hints.tempo) {
      const tempoRange = {
        slow: [60, 90],
        medium: [90, 120],
        moderate: [90, 120],
        fast: [120, 160],
        upbeat: [120, 160]
      };
      const range = tempoRange[hints.tempo] || [60, 160];
      filtered = filtered.filter(t => t.bpm >= range[0] && t.bpm <= range[1]);
    }

    if (hints.energy_level !== undefined) {
      const targetEnergy = hints.energy_level;
      const tolerance = 0.2;
      filtered = filtered.filter(t => Math.abs(t.energy - targetEnergy) <= tolerance);
    }

    if (hints.vocal_style === 'instrumental') {
      filtered = filtered.filter(t => t.language === 'instrumental');
    }

    if (filtered.length === 0) {
      return [...tracks];
    }

    return filtered;
  }

  getCurrentPlaylist() {
    return this.currentPlaylist;
  }

  async searchMusic(keyword, options = {}) {
    try {
      const results = this.trackLibrary.filter(t => 
        t.title.includes(keyword) || t.artist.includes(keyword) || t.genre.includes(keyword)
      );
      return { success: true, results, count: results.length };
    } catch (error) {
      console.error('[ContentEngine] Search music error:', error.message);
      return { success: false, error: error.message, results: [] };
    }
  }

  async getPlayableUrl(track) {
    if (!track) {
      return { url: null, error: 'No track provided' };
    }

    try {
      return { url: `https://mock.music.service/play/${track.id || 'unknown'}`, track };
    } catch (error) {
      console.error('[ContentEngine] Get play URL error:', error.message);
      return { url: null, error: error.message, track };
    }
  }

  async enrichPlaylistWithUrls(playlist = null) {
    const tracks = playlist || this.currentPlaylist;

    if (!tracks || tracks.length === 0) {
      return { success: false, error: 'No tracks to enrich', playlist: [] };
    }

    try {
      const enrichedTracks = tracks.map(t => ({
        ...t,
        playUrl: `https://mock.music.service/play/${t.id || 'unknown'}`
      }));

      if (!playlist) {
        this.currentPlaylist = enrichedTracks;
      }

      return {
        success: true,
        playlist: enrichedTracks,
        total: enrichedTracks.length,
        withUrls: enrichedTracks.length
      };
    } catch (error) {
      console.error('[ContentEngine] Enrich playlist error:', error.message);
      return { success: false, error: error.message, playlist: tracks };
    }
  }

  async curatePlayablePlaylist(hints = {}, constraints = {}) {
    const result = await this.curatePlaylist(hints, constraints);

    if (result.playlist && result.playlist.length > 0) {
      const enrichedResult = await this.enrichPlaylistWithUrls(result.playlist);
      result.playlist = enrichedResult.playlist;
      result.playableCount = enrichedResult.withUrls;
      result.note = '播放 URL 有时效性（通常 1 小时），过期后需重新获取';
    }

    return result;
  }
}

const contentEngine = new ContentEngine({
  apiKey: process.env.DASHSCOPE_API_KEY
});

module.exports = {
  ContentEngine,
  contentEngine
};

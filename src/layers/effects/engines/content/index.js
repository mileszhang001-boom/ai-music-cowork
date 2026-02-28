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

    if (isPresetScene && this.musicLibrary) {
      const sceneTracks = this.getTracksByScene(sceneType);
      if (sceneTracks.length > 0) {
        playlist = this._filterTracksByHints(sceneTracks, hints, constraints);
        source = 'library';
        console.log(`[ContentEngine] Using library tracks for preset scene: ${sceneType}`);
      }
    }

    if (playlist.length === 0 && this.llmClient && this.config.enableLLM) {
      try {
        console.log(`[ContentEngine] Calling LLM for ${isPresetScene ? 'preset' : 'custom'} scene: ${sceneType || 'unknown'}`);
        playlist = await this._generatePlaylistWithLLM(hints, constraints);
        source = 'llm';
        console.log(`[ContentEngine] LLM generated ${playlist.length} tracks`);
      } catch (error) {
        console.warn('[ContentEngine] LLM generation failed, falling back to mock logic:', error.message);
      }
    }

    if (playlist.length === 0) {
      playlist = this._filterTracksByHints(this.trackLibrary, hints, constraints);
      source = 'mock';
      console.log(`[ContentEngine] Using mock data (${playlist.length} tracks)`);
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
      source
    };
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

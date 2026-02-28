'use strict';

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
    // this.musicService = new MusicService(config.musicService || {}); // Removed due to missing module
  }

  async execute(action, params = {}) {
    switch (action) {
    case 'curate_playlist':
      return this.curatePlaylist(params.hints, params.constraints);
    case 'get_current':
      return { playlist: this.currentPlaylist };
    case 'search_music':
      return await this.searchMusic(params.keyword, params.options);
    case 'get_play_url':
      return await this.getPlayableUrl(params.track);
    case 'enrich_playlist':
      return await this.enrichPlaylistWithUrls(params.playlist);
    default:
      return { error: `Unknown action: ${action}` };
    }
  }

  curatePlaylist(hints = {}, constraints = {}) {
    let tracks = [...this.trackLibrary];

    if (hints.genres && hints.genres.length > 0) {
      tracks = tracks.filter(t => hints.genres.includes(t.genre));
    }

    if (hints.tempo) {
      const tempoRange = {
        slow: [60, 90],
        moderate: [90, 120],
        upbeat: [120, 160]
      };
      const range = tempoRange[hints.tempo] || [60, 160];
      tracks = tracks.filter(t => t.bpm >= range[0] && t.bpm <= range[1]);
    }

    if (tracks.length === 0) {
      tracks = [...this.trackLibrary];
    }

    const playlistSize = constraints.max_tracks || 10;
    const playlist = tracks.slice(0, playlistSize).map(t => ({
      ...t,
      added_at: Date.now()
    }));

    this.currentPlaylist = playlist;

    const totalDuration = playlist.reduce((sum, t) => sum + t.duration, 0);

    return {
      playlist,
      total_duration: totalDuration,
      avg_energy: playlist.reduce((sum, t) => sum + t.energy, 0) / playlist.length || 0
    };
  }

  getCurrentPlaylist() {
    return this.currentPlaylist;
  }

  async searchMusic(keyword, options = {}) {
    try {
      // Mock implementation since MusicService is missing
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
      // Mock implementation since MusicService is missing
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
      // Mock implementation since MusicService is missing
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
    const result = this.curatePlaylist(hints, constraints);

    if (result.playlist && result.playlist.length > 0) {
      const enrichedResult = await this.enrichPlaylistWithUrls(result.playlist);
      result.playlist = enrichedResult.playlist;
      result.playableCount = enrichedResult.withUrls;
      result.note = '播放 URL 有时效性（通常 1 小时），过期后需重新获取';
    }

    return result;
  }
}

const contentEngine = new ContentEngine();

module.exports = {
  ContentEngine,
  contentEngine
};

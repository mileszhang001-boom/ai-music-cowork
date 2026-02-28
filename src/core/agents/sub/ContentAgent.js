'use strict';

const { SubAgent } = require('./SubAgent');

class ContentAgent extends SubAgent {
  constructor(config = {}) {
    super({
      name: config.name || 'ContentAgent',
      agentType: 'content',
      mainAgentId: config.mainAgentId || 'main',
      maxIterations: config.maxIterations || 3,
      maxSteps: config.maxSteps || 10,
      debug: config.debug
    });

    this.contentEngine = config.contentEngine || null;
    this.playlist = [];
    this.currentTrack = null;
  }

  setContentEngine(engine) {
    this.contentEngine = engine;
  }

  async executeTask(task) {
    this.setContext({
      intent: task.params?.intent || {},
      hints: task.params?.hints || {},
      constraints: task.params?.constraints || {}
    });

    return super.executeTask(task);
  }

  async queryContentEngine(params) {
    if (!this.contentEngine) {
      return { error: 'Content engine not configured' };
    }

    try {
      const result = await this.contentEngine.execute('query', {
        query: params.query,
        constraints: this.context.constraints
      });
      return result;
    } catch (error) {
      return { error: error.message };
    }
  }

  async generatePlaylist(params) {
    const { intent, hints } = this.context;
    
    const playlistParams = {
      energy: intent?.energy || 'medium',
      mood: intent?.mood || 'neutral',
      genres: hints?.music?.genres || [],
      constraints: this.context.constraints
    };

    if (this.contentEngine) {
      try {
        const result = await this.contentEngine.execute('generate_playlist', playlistParams);
        this.playlist = result.playlist || [];
        return result;
      } catch (error) {
        return { error: error.message };
      }
    }

    return this.fallbackGeneratePlaylist(playlistParams);
  }

  fallbackGeneratePlaylist(params) {
    const templates = {
      high_energetic: ['欢快流行', '电子舞曲', '摇滚'],
      medium_energetic: ['流行', '轻音乐', '爵士'],
      low_energetic: ['古典', '轻音乐', '环境音'],
      joyful: ['流行', '舞曲', '欢快'],
      melancholic: ['抒情', '慢歌', '怀旧'],
      calm: ['古典', '轻音乐', '自然音']
    };

    const key = `${params.energy}_${params.mood}`;
    const genres = templates[key] || templates.medium_energetic;

    return {
      playlist: genres.map((genre, i) => ({
        id: `track_${i + 1}`,
        title: `${genre} - 推荐歌曲`,
        artist: '艺术家',
        genre,
        duration: 180
      })),
      count: genres.length,
      source: 'fallback'
    };
  }

  async smallStepExecute(action, params) {
    switch (action) {
      case 'query_content':
        return await this.queryContentEngine(params);
      case 'generate_playlist':
        return await this.generatePlaylist(params);
      default:
        return await super.smallStepExecute(action, params);
    }
  }

  getPlaylist() {
    return this.playlist;
  }

  getCurrentTrack() {
    return this.currentTrack;
  }

  selectTrack(trackId) {
    this.currentTrack = this.playlist.find(t => t.id === trackId) || null;
    return this.currentTrack;
  }
}

module.exports = {
  ContentAgent
};

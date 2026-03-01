'use strict';

const ColorThemes = {
  // 现有主题
  calm: { primary: '#1A237E', secondary: '#4A148C' },
  warm: { primary: '#FF5722', secondary: '#FF9800' },
  vibrant: { primary: '#E91E63', secondary: '#9C27B0' },
  alert: { primary: '#F44336', secondary: '#FFEB3B' },
  romantic: { primary: '#E91E63', secondary: '#FCE4EC' },
  
  // 新增冷色系主题
  cool: { primary: '#00BCD4', secondary: '#3F51B5' },
  night: { primary: '#0D1B2A', secondary: '#1B263B' },
  focus: { primary: '#2196F3', secondary: '#009688' },
  
  // 新增活力主题
  energetic: { primary: '#FF6B00', secondary: '#FFD600' },
  party: { primary: '#E91E63', secondary: '#00BCD4' }
};

const Patterns = {
  breathing: { type: 'breathing', fps: 1 },
  steady: { type: 'steady', fps: 0 },
  pulse: { type: 'pulse', fps: 2 },
  flash: { type: 'flash', fps: 4 }
};

class LightingEngine {
  constructor(config = {}) {
    this.config = config;
    this.currentTheme = 'calm';
    this.currentIntensity = 0.5;
  }

  async execute(action, params = {}) {
    switch (action) {
    case 'apply_theme':
      return this.applyTheme(params.theme, params.pattern, params.intensity);
    case 'get_current':
      return this.getCurrentState();
    default:
      return { error: `Unknown action: ${action}` };
    }
  }

  applyTheme(theme = 'calm', pattern = 'breathing', intensity = 0.5) {
    this.currentTheme = theme;
    this.currentIntensity = Math.max(0, Math.min(1, intensity));

    const colors = ColorThemes[theme] || ColorThemes.calm;
    const patternConfig = Patterns[pattern] || Patterns.breathing;

    return {
      theme,
      colors,
      pattern: patternConfig.type,
      intensity: this.currentIntensity,
      applied_at: Date.now()
    };
  }

  getCurrentState() {
    return {
      theme: this.currentTheme,
      colors: ColorThemes[this.currentTheme] || ColorThemes.calm,
      intensity: this.currentIntensity
    };
  }
}

const lightingEngine = new LightingEngine();

module.exports = {
  LightingEngine,
  lightingEngine,
  ColorThemes,
  Patterns
};

'use strict';

const SCENE_THEME_MAP = {
  'family_outing': 'ocean',
  'sunset_drive': 'sunset',
  'rainy_day': 'rainy',
  'night_drive': 'citynight',
  'road_trip': 'forest',
  'morning_commute': 'spring',
  'beach_vacation': 'summer',
  'autumn_drive': 'autumn',
  'winter_drive': 'winter',
  'romantic_date': 'romantic',
  'party': 'party',
  'meditation': 'meditation',
  'cloudy_day': 'gloomy'
};

const THEME_KEYWORD_MAP = {
  ocean: { keywords: ['海边', '沙滩', '海洋', 'beach', 'ocean', 'sea'], colors: ['#00CED1', '#20B2AA'] },
  sunset: { keywords: ['夕阳', '黄昏', '日落', 'sunset', 'dusk'], colors: ['#FF6B35', '#FFD700'] },
  rainy: { keywords: ['雨', '雨天', 'rain', 'rainy'], colors: ['#4682B4', '#708090'] },
  forest: { keywords: ['森林', '树林', 'forest', 'wood'], colors: ['#228B22', '#2E8B57'] },
  citynight: { keywords: ['城市', '夜景', 'city', 'night'], colors: ['#191970', '#FFD700'] },
  spring: { keywords: ['春天', '春日', 'spring'], colors: ['#90EE90', '#FFB6C1'] },
  summer: { keywords: ['夏天', '夏日', 'summer'], colors: ['#00BFFF', '#FFDAB9'] },
  autumn: { keywords: ['秋天', '秋日', 'autumn', 'fall'], colors: ['#D2691E', '#FF8C00'] },
  winter: { keywords: ['冬天', '冬日', '雪', 'winter', 'snow'], colors: ['#B0E0E6', '#FFFFFF'] },
  romantic: { keywords: ['浪漫', '约会', 'romantic', 'date', 'love'], colors: ['#FF69B4', '#FFB6C1'] },
  party: { keywords: ['派对', '聚会', 'party', 'dance'], colors: ['#FF1493', '#00FF7F'] },
  meditation: { keywords: ['冥想', '放松', 'meditation', 'relax', 'zen'], colors: ['#9370DB', '#E6E6FA'] },
  gloomy: { keywords: ['阴天', '阴沉', '多云', 'cloudy', 'gloomy', 'overcast'], colors: ['#4A5568', '#696969'] }
};

const ColorThemes = {
  calm: { primary: '#1A237E', secondary: '#4A148C' },
  warm: { primary: '#FF5722', secondary: '#FF9800' },
  vibrant: { primary: '#E91E63', secondary: '#9C27B0' },
  alert: { primary: '#F44336', secondary: '#FFEB3B' },
  romantic: { primary: '#FF69B4', secondary: '#FFB6C1' },
  cool: { primary: '#00BCD4', secondary: '#3F51B5' },
  night: { primary: '#0D1B2A', secondary: '#1B263B' },
  focus: { primary: '#2196F3', secondary: '#009688' },
  energetic: { primary: '#FF6B00', secondary: '#FFD600' },
  party: { primary: '#FF1493', secondary: '#00FF7F' },
  ocean: { primary: '#00CED1', secondary: '#20B2AA' },
  sunset: { primary: '#FF6B35', secondary: '#FFD700' },
  rainy: { primary: '#4682B4', secondary: '#708090' },
  forest: { primary: '#228B22', secondary: '#2E8B57' },
  citynight: { primary: '#191970', secondary: '#FFD700' },
  spring: { primary: '#90EE90', secondary: '#FFB6C1' },
  summer: { primary: '#00BFFF', secondary: '#FFDAB9' },
  autumn: { primary: '#D2691E', secondary: '#FF8C00' },
  winter: { primary: '#B0E0E6', secondary: '#FFFFFF' },
  meditation: { primary: '#9370DB', secondary: '#E6E6FA' },
  gloomy: { primary: '#4A5568', secondary: '#696969' }
};

const Patterns = {
  breathing: { type: 'breathing', fps: 1 },
  steady: { type: 'steady', fps: 0 },
  pulse: { type: 'pulse', fps: 2 },
  flash: { type: 'flash', fps: 4 }
};

function isValidTheme(theme) {
  return typeof theme === 'string' && theme in ColorThemes;
}

function hexToHSL(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!result) {
    return { h: 0, s: 0, l: 0 };
  }

  let r = parseInt(result[1], 16) / 255;
  let g = parseInt(result[2], 16) / 255;
  let b = parseInt(result[3], 16) / 255;

  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  let h = 0;
  let s = 0;
  const l = (max + min) / 2;

  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

    switch (max) {
    case r:
      h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
      break;
    case g:
      h = ((b - r) / d + 2) / 6;
      break;
    case b:
      h = ((r - g) / d + 4) / 6;
      break;
    }
  }

  return {
    h: Math.round(h * 360),
    s: Math.round(s * 100),
    l: Math.round(l * 100)
  };
}

function hslToHex(h, s, l) {
  h = h / 360;
  s = s / 100;
  l = l / 100;

  let r, g, b;

  if (s === 0) {
    r = g = b = l;
  } else {
    const hue2rgb = (p, q, t) => {
      if (t < 0) t += 1;
      if (t > 1) t -= 1;
      if (t < 1 / 6) return p + (q - p) * 6 * t;
      if (t < 1 / 2) return q;
      if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
      return p;
    };

    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    r = hue2rgb(p, q, h + 1 / 3);
    g = hue2rgb(p, q, h);
    b = hue2rgb(p, q, h - 1 / 3);
  }

  const toHex = x => {
    const hex = Math.round(x * 255).toString(16);
    return hex.length === 1 ? '0' + hex : hex;
  };

  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function adjustColorByEnergy(hexColor, energy) {
  const { h, s, l } = hexToHSL(hexColor);

  let adjustedS = s;
  if (energy > 0.7) {
    adjustedS = Math.min(100, s * 1.2);
  } else if (energy < 0.3) {
    adjustedS = s * 0.7;
  }

  return hslToHex(h, adjustedS, l);
}

function adjustColorByValence(hexColor, valence) {
  const { h, s, l } = hexToHSL(hexColor);

  let adjustedH = h;
  if (valence > 0.7) {
    adjustedH = (h + 10) % 360;
  } else if (valence < 0.3) {
    adjustedH = (h - 10 + 360) % 360;
  }

  return hslToHex(adjustedH, s, l);
}

function mapSceneToLightingTheme(sceneType, sceneKeywords = []) {
  if (sceneType && SCENE_THEME_MAP[sceneType]) {
    return SCENE_THEME_MAP[sceneType];
  }

  if (sceneKeywords && sceneKeywords.length > 0) {
    for (const [theme, config] of Object.entries(THEME_KEYWORD_MAP)) {
      const keywords = config.keywords || [];
      if (sceneKeywords.some(k => keywords.includes(k.toLowerCase()) || keywords.includes(k))) {
        return theme;
      }
    }
  }

  return 'calm';
}

function extractKeywordsFromScene(sceneDescription) {
  if (!sceneDescription || typeof sceneDescription !== 'string') {
    return [];
  }

  const allKeywords = [];
  for (const config of Object.values(THEME_KEYWORD_MAP)) {
    if (config.keywords) {
      allKeywords.push(...config.keywords);
    }
  }

  const desc = sceneDescription.toLowerCase();
  return allKeywords.filter(keyword => desc.includes(keyword.toLowerCase()));
}

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

  generateLighting(options = {}) {
    const { sceneType, sceneDescription, pattern = 'breathing', intensity = 0.5, musicFeatures } = options;

    let theme;
    if (sceneType) {
      const keywords = sceneDescription ? extractKeywordsFromScene(sceneDescription) : [];
      theme = mapSceneToLightingTheme(sceneType, keywords);
    } else if (sceneDescription) {
      const keywords = extractKeywordsFromScene(sceneDescription);
      theme = mapSceneToLightingTheme(null, keywords);
    } else {
      theme = 'calm';
    }

    const baseColors = ColorThemes[theme] || ColorThemes.calm;
    let adjustedColors = { ...baseColors };

    if (musicFeatures) {
      if (musicFeatures.energy !== undefined) {
        adjustedColors.primary = adjustColorByEnergy(baseColors.primary, musicFeatures.energy);
        adjustedColors.secondary = adjustColorByEnergy(baseColors.secondary, musicFeatures.energy);
      }

      if (musicFeatures.valence !== undefined) {
        adjustedColors.primary = adjustColorByValence(adjustedColors.primary, musicFeatures.valence);
        adjustedColors.secondary = adjustColorByValence(adjustedColors.secondary, musicFeatures.valence);
      }
    }

    this.currentTheme = theme;
    this.currentIntensity = Math.max(0, Math.min(1, intensity));
    const patternConfig = Patterns[pattern] || Patterns.breathing;

    return {
      theme,
      colors: adjustedColors,
      pattern: patternConfig.type,
      intensity: this.currentIntensity,
      musicFeatures: musicFeatures || null,
      applied_at: Date.now()
    };
  }
}

const lightingEngine = new LightingEngine();

module.exports = {
  LightingEngine,
  lightingEngine,
  ColorThemes,
  Patterns,
  SCENE_THEME_MAP,
  THEME_KEYWORD_MAP,
  isValidTheme,
  mapSceneToLightingTheme,
  extractKeywordsFromScene,
  hexToHSL,
  hslToHex,
  adjustColorByEnergy,
  adjustColorByValence
};

'use strict';

const RuleTypes = {
  CONTENT_RATING: 'content_rating',
  VOLUME_LIMIT: 'volume_limit',
  BRIGHTNESS_LIMIT: 'brightness_limit',
  SUDDEN_CHANGE: 'sudden_change',
  USER_OVERRIDE: 'user_override',
  CHILD_MODE: 'child_mode'
};

const ContentRatings = {
  G: 'G',
  PG: 'PG',
  PG13: 'PG-13',
  R: 'R'
};

const RatingOrder = [ContentRatings.G, ContentRatings.PG, ContentRatings.PG13, ContentRatings.R];

const DefaultRules = {
  maxVolumeDb: 85,
  maxBrightness: 1.0,
  defaultContentRating: ContentRatings.PG,
  preventSuddenChanges: true,
  childModeContentRating: ContentRatings.G,
  childModeMaxVolumeDb: 70
};

class RulesEngine {
  constructor(config = {}) {
    this.rules = { ...DefaultRules, ...config };
    this.violations = [];
    this.warnings = [];
  }

  validate(descriptor, context = {}) {
    this.violations = [];
    this.warnings = [];

    if (!descriptor || !descriptor.intent) {
      this.violations.push({
        type: 'INVALID_DESCRIPTOR',
        message: 'Descriptor 或 intent 缺失',
        severity: 'critical'
      });
      return this.buildResult(false);
    }

    this.checkContentRating(descriptor, context);
    this.checkVolumeLimit(descriptor, context);
    this.checkBrightnessLimit(descriptor, context);
    this.checkSuddenChanges(descriptor, context);
    this.checkUserOverrides(descriptor, context);

    const passed = this.violations.length === 0;
    return this.buildResult(passed);
  }

  checkContentRating(descriptor, context) {
    const constraints = descriptor.intent?.constraints || {};
    const hints = descriptor.hints || {};
    const hasChildren = context.passengerComposition?.includes('child');

    let maxRating = constraints.content_rating || this.rules.defaultContentRating;

    if (hasChildren) {
      maxRating = this.rules.childModeContentRating;
      this.warnings.push({
        type: RuleTypes.CHILD_MODE,
        message: '检测到儿童乘客，内容分级强制设为 G',
        applied: { content_rating: ContentRatings.G }
      });
    }

    const musicHints = hints.music || {};
    const genres = musicHints.genres || [];
    const inappropriateGenres = this.checkInappropriateGenres(genres, maxRating);

    if (inappropriateGenres.length > 0) {
      this.violations.push({
        type: RuleTypes.CONTENT_RATING,
        message: `内容分级违规：检测到不适宜类型 ${inappropriateGenres.join(', ')}`,
        severity: 'high',
        current: genres,
        allowed: maxRating
      });
    }
  }

  checkInappropriateGenres(genres, maxRating) {
    const restrictedGenres = {
      [ContentRatings.R]: ['explicit', 'adult_only'],
      [ContentRatings.PG13]: ['explicit', 'adult_only', 'teen_mature'],
      [ContentRatings.PG]: ['explicit', 'adult_only', 'teen_mature', 'horror'],
      [ContentRatings.G]: ['explicit', 'adult_only', 'teen_mature', 'horror', 'intense_rock', 'heavy_metal']
    };

    const forbidden = restrictedGenres[maxRating] || restrictedGenres[ContentRatings.PG];
    return genres.filter(g => forbidden.includes(g.toLowerCase()));
  }

  checkVolumeLimit(descriptor, context) {
    const constraints = descriptor.intent?.constraints || {};
    const hasChildren = context.passengerComposition?.includes('child');

    let maxVolume = constraints.max_volume_db || this.rules.maxVolumeDb;

    if (hasChildren) {
      maxVolume = Math.min(maxVolume, this.rules.childModeMaxVolumeDb);
    }

    const audioHints = descriptor.hints?.audio || {};
    const suggestedVolume = audioHints.suggested_volume_db;

    if (suggestedVolume !== undefined && suggestedVolume > maxVolume) {
      this.violations.push({
        type: RuleTypes.VOLUME_LIMIT,
        message: `音量超限：${suggestedVolume}dB > 最大允许 ${maxVolume}dB`,
        severity: 'high',
        current: suggestedVolume,
        allowed: maxVolume
      });
    }
  }

  checkBrightnessLimit(descriptor, context) {
    const constraints = descriptor.intent?.constraints || {};
    const maxBrightness = constraints.brightness_max || this.rules.maxBrightness;

    const lightingHints = descriptor.hints?.lighting || {};
    const intensity = lightingHints.intensity;

    if (intensity !== undefined && intensity > maxBrightness) {
      this.violations.push({
        type: RuleTypes.BRIGHTNESS_LIMIT,
        message: `亮度超限：${intensity} > 最大允许 ${maxBrightness}`,
        severity: 'medium',
        current: intensity,
        allowed: maxBrightness
      });
    }
  }

  checkSuddenChanges(descriptor, context) {
    const constraints = descriptor.intent?.constraints || {};
    const preventSudden = constraints.avoid_sudden_changes ?? this.rules.preventSuddenChanges;

    if (!preventSudden) return;

    const transition = descriptor.intent?.transition || {};
    const lightingHints = descriptor.hints?.lighting || {};

    if (lightingHints.pattern === 'flash' && transition.type !== 'immediate') {
      this.warnings.push({
        type: RuleTypes.SUDDEN_CHANGE,
        message: '检测到闪烁模式，建议增加过渡时间或降低频率',
        suggestion: { transition_duration_sec: 2 }
      });
    }

    if (transition.duration_sec !== undefined && transition.duration_sec < 1) {
      this.violations.push({
        type: RuleTypes.SUDDEN_CHANGE,
        message: `过渡时间过短：${transition.duration_sec}秒 < 最小 1 秒`,
        severity: 'medium',
        current: transition.duration_sec,
        allowed: 1
      });
    }
  }

  checkUserOverrides(descriptor, context) {
    const overrides = descriptor.intent?.user_overrides || {};

    if (overrides.exclude_tags && overrides.exclude_tags.length > 0) {
      const musicHints = descriptor.hints?.music || {};
      const genres = musicHints.genres || [];
      const conflicts = genres.filter(g => 
        overrides.exclude_tags.some(tag => 
          g.toLowerCase().includes(tag.toLowerCase())
        )
      );

      if (conflicts.length > 0) {
        this.violations.push({
          type: RuleTypes.USER_OVERRIDE,
          message: `违反用户排除偏好：${conflicts.join(', ')}`,
          severity: 'high',
          current: conflicts,
          excluded: overrides.exclude_tags
        });
      }
    }
  }

  buildResult(passed) {
    return {
      passed,
      violations: this.violations,
      warnings: this.warnings,
      summary: {
        total_issues: this.violations.length + this.warnings.length,
        critical: this.violations.filter(v => v.severity === 'critical').length,
        high: this.violations.filter(v => v.severity === 'high').length,
        medium: this.violations.filter(v => v.severity === 'medium').length,
        warnings: this.warnings.length
      }
    };
  }

  applyFixes(descriptor, context = {}) {
    const result = this.validate(descriptor, context);
    if (result.passed) return descriptor;

    const fixed = JSON.parse(JSON.stringify(descriptor));
    const constraints = fixed.intent.constraints || {};
    const hasChildren = context.passengerComposition?.includes('child');

    if (hasChildren) {
      constraints.content_rating = ContentRatings.G;
      constraints.max_volume_db = Math.min(
        constraints.max_volume_db || this.rules.maxVolumeDb,
        this.rules.childModeMaxVolumeDb
      );
    }

    if (constraints.max_volume_db && constraints.max_volume_db > this.rules.maxVolumeDb) {
      constraints.max_volume_db = this.rules.maxVolumeDb;
    }

    if (constraints.brightness_max && constraints.brightness_max > this.rules.maxBrightness) {
      constraints.brightness_max = this.rules.maxBrightness;
    }

    if (constraints.avoid_sudden_changes !== false) {
      const transition = fixed.intent.transition || {};
      if (transition.duration_sec !== undefined && transition.duration_sec < 1) {
        transition.duration_sec = 1;
        fixed.intent.transition = transition;
      }
    }

    fixed.intent.constraints = constraints;
    fixed._validation = {
      fixed: true,
      original_violations: result.violations.length,
      fixes_applied: result.violations.filter(v => v.severity !== 'critical').length
    };

    return fixed;
  }

  getSafeTemplate(context = {}) {
    const hasChildren = context.passengerComposition?.includes('child');

    return {
      version: '2.0',
      scene_id: `safe_${Date.now()}`,
      scene_type: 'safe_default',
      scene_name: '安全默认场景',
      intent: {
        mood: { valence: 0.5, arousal: 0.3 },
        energy_level: 0.3,
        atmosphere: 'neutral',
        constraints: {
          content_rating: hasChildren ? ContentRatings.G : ContentRatings.PG,
          max_volume_db: hasChildren ? this.rules.childModeMaxVolumeDb : this.rules.maxVolumeDb,
          avoid_sudden_changes: true,
          brightness_max: 0.6
        }
      },
      hints: {
        music: { genres: ['pop'], tempo: 'moderate' },
        lighting: { color_theme: 'neutral', pattern: 'steady', intensity: 0.4 },
        audio: { preset: 'standard' }
      },
      meta: {
        source: 'safe_template',
        reason: '规则校验失败，回退到安全模板'
      }
    };
  }
}

const rulesEngine = new RulesEngine();

module.exports = {
  RulesEngine,
  rulesEngine,
  RuleTypes,
  ContentRatings,
  DefaultRules
};

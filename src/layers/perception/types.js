'use strict';

const SignalSources = {
  VHAL: 'vhal',
  VOICE: 'voice',
  ENVIRONMENT: 'environment',
  EXTERNAL_CAMERA: 'external_camera',
  INTERNAL_CAMERA: 'internal_camera',
  INTERNAL_MIC: 'internal_mic',
  BIOMETRIC: 'biometric',
  USER_PROFILE: 'user_profile',
  MUSIC_STATE: 'music_state'
};

const SignalCategories = {
  CONTEXT: 'context',
  USER_STATE: 'user_state',
  USER_INTENT: 'user_intent',
  ENVIRONMENT: 'environment',
  VISION: 'vision',
  AUDIO: 'audio'
};

const SignalConfigs = {
  [SignalSources.VHAL]: {
    category: SignalCategories.CONTEXT,
    defaultTTL: 5000,
    confidenceDecay: 0.1
  },
  [SignalSources.VOICE]: {
    category: SignalCategories.USER_INTENT,
    defaultTTL: 30000,
    confidenceDecay: 0.05
  },
  [SignalSources.ENVIRONMENT]: {
    category: SignalCategories.ENVIRONMENT,
    defaultTTL: 60000,
    confidenceDecay: 0.02
  },
  [SignalSources.EXTERNAL_CAMERA]: {
    category: SignalCategories.VISION,
    defaultTTL: 1000,
    confidenceDecay: 0.15
  },
  [SignalSources.INTERNAL_CAMERA]: {
    category: SignalCategories.VISION,
    defaultTTL: 2000,
    confidenceDecay: 0.12
  },
  [SignalSources.INTERNAL_MIC]: {
    category: SignalCategories.AUDIO,
    defaultTTL: 500,
    confidenceDecay: 0.2
  },
  [SignalSources.BIOMETRIC]: {
    category: SignalCategories.USER_STATE,
    defaultTTL: 5000,
    confidenceDecay: 0.1
  },
  [SignalSources.USER_PROFILE]: {
    category: SignalCategories.USER_STATE,
    defaultTTL: 300000,
    confidenceDecay: 0.01
  },
  [SignalSources.MUSIC_STATE]: {
    category: SignalCategories.CONTEXT,
    defaultTTL: 1000,
    confidenceDecay: 0.2
  }
};

const DateTypes = {
  WEEKDAY: 'weekday',
  WEEKEND: 'weekend',
  HOLIDAY: 'holiday'
};

const MoodTypes = {
  HAPPY: 'happy',
  CALM: 'calm',
  TIRED: 'tired',
  STRESSED: 'stressed',
  NEUTRAL: 'neutral',
  EXCITED: 'excited'
};

module.exports = {
  SignalSources,
  SignalCategories,
  SignalConfigs,
  DateTypes,
  MoodTypes
};

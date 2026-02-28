'use strict';

const SignalSources = {
  VHAL: 'vhal',
  VOICE: 'voice',
  BIOMETRIC: 'biometric',
  ENVIRONMENT: 'environment',
  USER_PROFILE: 'user_profile',
  MUSIC_STATE: 'music_state'
};

const SignalCategories = {
  CONTEXT: 'context',
  USER_STATE: 'user_state',
  USER_INTENT: 'user_intent',
  ENVIRONMENT: 'environment'
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
  [SignalSources.BIOMETRIC]: {
    category: SignalCategories.USER_STATE,
    defaultTTL: 10000,
    confidenceDecay: 0.08
  },
  [SignalSources.ENVIRONMENT]: {
    category: SignalCategories.ENVIRONMENT,
    defaultTTL: 60000,
    confidenceDecay: 0.02
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

module.exports = {
  SignalSources,
  SignalCategories,
  SignalConfigs
};

'use strict';

const { SignalSources } = require('../types');

class VHALSensor {
  constructor(config = {}) {
    this.config = config;
    this.name = 'vhal';
    this.source = SignalSources.VHAL;
  }

  read(signalType, value) {
    return {
      source: this.source,
      type: signalType,
      value: value,
      timestamp: Date.now(),
      confidence: 0.99
    };
  }

  readSpeed(speedKmh) {
    return this.read('vehicle_speed', { vehicle_speed: speedKmh / 200, speed_kmh: speedKmh });
  }

  readPassengerCount(count) {
    return this.read('passenger_count', { passenger_count: count });
  }

  readGear(gear) {
    return this.read('gear_position', gear);
  }
}

class EnvironmentSensor {
  constructor(config = {}) {
    this.config = config;
    this.name = 'environment';
    this.source = SignalSources.ENVIRONMENT;
  }

  read(signalType, value) {
    return {
      source: this.source,
      type: signalType,
      value: value,
      timestamp: Date.now(),
      confidence: 0.95
    };
  }

  readTimeOfDay(hour) {
    const normalized = hour >= 6 && hour < 18 ? (hour - 6) / 12 : 0;
    return this.read('time_of_day', { time_of_day: normalized, hour });
  }

  readWeather(weather) {
    return this.read('weather', { weather });
  }

  readTemperature(temp) {
    return this.read('temperature', temp);
  }
}

class BiometricSensor {
  constructor(config = {}) {
    this.config = config;
    this.name = 'biometric';
    this.source = SignalSources.BIOMETRIC;
  }

  read(signalType, value) {
    return {
      source: this.source,
      type: signalType,
      value: value,
      timestamp: Date.now(),
      confidence: 0.80
    };
  }

  readHeartRate(bpm) {
    return this.read('heart_rate', bpm);
  }

  readFatigue(level) {
    return this.read('fatigue_level', level);
  }

  readStress(level) {
    return this.read('stress_level', level);
  }
}

class VoiceSensor {
  constructor(config = {}) {
    this.config = config;
    this.name = 'voice';
    this.source = SignalSources.VOICE;
  }

  read(text, intent = 'creative') {
    return {
      source: this.source,
      type: 'user_query',
      value: { text, intent },
      timestamp: Date.now(),
      confidence: 0.90
    };
  }
}

class UserProfileSensor {
  constructor(config = {}) {
    this.config = config;
    this.name = 'user_profile';
    this.source = SignalSources.USER_PROFILE;
  }

  read(preferences) {
    return {
      source: this.source,
      type: 'preferences',
      value: preferences,
      timestamp: Date.now(),
      confidence: 0.95
    };
  }
}

class MusicStateSensor {
  constructor(config = {}) {
    this.config = config;
    this.name = 'music_state';
    this.source = SignalSources.MUSIC_STATE;
  }

  read(state) {
    return {
      source: this.source,
      type: 'playback_state',
      value: state,
      timestamp: Date.now(),
      confidence: 0.99
    };
  }
}

module.exports = {
  VHALSensor,
  EnvironmentSensor,
  BiometricSensor,
  VoiceSensor,
  UserProfileSensor,
  MusicStateSensor,
  vhalSensor: new VHALSensor(),
  environmentSensor: new EnvironmentSensor(),
  biometricSensor: new BiometricSensor(),
  voiceSensor: new VoiceSensor(),
  userProfileSensor: new UserProfileSensor(),
  musicStateSensor: new MusicStateSensor()
};

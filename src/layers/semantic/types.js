'use strict';

const SceneDimensions = {
  SOCIAL: 'social',
  ENERGY: 'energy',
  FOCUS: 'focus',
  TIME_CONTEXT: 'time_context',
  WEATHER: 'weather'
};

const SceneTypes = {
  MORNING_COMMUTE: 'morning_commute',
  NIGHT_DRIVE: 'night_drive',
  ROAD_TRIP: 'road_trip',
  ROMANTIC_DATE: 'romantic_date',
  FAMILY_OUTING: 'family_outing',
  FOCUS_WORK: 'focus_work',
  TRAFFIC_JAM: 'traffic_jam',
  FATIGUE_ALERT: 'fatigue_alert',
  RAINY_NIGHT: 'rainy_night',
  PARTY: 'party'
};

const IntentTypes = {
  CREATIVE: 'creative',
  NAVIGATION: 'navigation',
  CONTROL: 'control',
  INFO: 'info'
};

module.exports = {
  SceneDimensions,
  SceneTypes,
  IntentTypes
};

'use strict';

const fs = require('fs');
const path = require('path');

const GENRE_VALENCE_MAP = {
  'pop': { valence: [0.5, 0.8], acousticness: [0.2, 0.5] },
  'rock': { valence: [0.4, 0.7], acousticness: [0.1, 0.4] },
  'jazz': { valence: [0.3, 0.6], acousticness: [0.5, 0.8] },
  'classical': { valence: [0.2, 0.6], acousticness: [0.8, 1.0] },
  'electronic': { valence: [0.5, 0.9], acousticness: [0.0, 0.2] },
  'ambient': { valence: [0.3, 0.5], acousticness: [0.6, 0.9] },
  'folk': { valence: [0.4, 0.7], acousticness: [0.6, 0.9] },
  'hip-hop': { valence: [0.4, 0.8], acousticness: [0.1, 0.3] },
  'r&b': { valence: [0.4, 0.7], acousticness: [0.3, 0.6] },
  'indie': { valence: [0.4, 0.7], acousticness: [0.4, 0.7] },
  'lo-fi': { valence: [0.3, 0.5], acousticness: [0.5, 0.8] },
  'ballad': { valence: [0.2, 0.5], acousticness: [0.5, 0.8] },
  'new_age': { valence: [0.3, 0.5], acousticness: [0.7, 1.0] },
  'children': { valence: [0.6, 0.9], acousticness: [0.3, 0.6] },
  'instrumental': { valence: [0.3, 0.6], acousticness: [0.6, 0.9] }
};

const MOOD_ENERGY_MAP = {
  'happy': { energy: [0.5, 0.9], valence: [0.6, 1.0] },
  'sad': { energy: [0.1, 0.4], valence: [0.0, 0.4] },
  'calm': { energy: [0.1, 0.3], valence: [0.3, 0.6] },
  'energetic': { energy: [0.7, 1.0], valence: [0.5, 1.0] },
  'romantic': { energy: [0.2, 0.5], valence: [0.4, 0.7] },
  'melancholy': { energy: [0.2, 0.5], valence: [0.1, 0.4] },
  'hopeful': { energy: [0.4, 0.7], valence: [0.5, 0.8] },
  'peaceful': { energy: [0.1, 0.3], valence: [0.4, 0.7] },
  'uplifting': { energy: [0.5, 0.8], valence: [0.6, 0.9] },
  'nostalgic': { energy: [0.2, 0.5], valence: [0.3, 0.6] }
};

const SCENE_TAGS_MAP = {
  'morning_commute': { time: ['morning'], activity: ['commute', 'driving'], scene: ['city', 'commute'], weather: ['sunny'], mood: ['hopeful', 'energetic'] },
  'night_drive': { time: ['night', 'late_night'], activity: ['driving'], scene: ['city', 'highway'], weather: ['night'], mood: ['calm', 'melancholy'] },
  'road_trip': { time: ['afternoon'], activity: ['driving', 'travel'], scene: ['highway'], weather: ['sunny'], mood: ['happy', 'energetic'] },
  'romantic_date': { time: ['evening'], activity: ['date'], scene: ['home', 'restaurant'], weather: ['sunny', 'cloudy'], mood: ['romantic', 'peaceful'] },
  'family_outing': { time: ['afternoon'], activity: ['family', 'driving'], scene: ['city', 'park'], weather: ['sunny'], mood: ['happy', 'calm'] },
  'focus_work': { time: ['morning', 'afternoon'], activity: ['working', 'focus'], scene: ['office', 'home'], weather: ['sunny', 'cloudy'], mood: ['calm', 'peaceful'] },
  'traffic_jam': { time: ['morning', 'afternoon'], activity: ['commute'], scene: ['city'], weather: ['sunny', 'cloudy'], mood: ['calm', 'peaceful'] },
  'rainy_night': { time: ['night'], activity: ['driving'], scene: ['city'], weather: ['rainy', 'night'], mood: ['calm', 'melancholy'] },
  'fatigue_alert': { time: ['afternoon', 'night'], activity: ['driving'], scene: ['highway'], weather: ['sunny', 'cloudy'], mood: ['energetic', 'uplifting'] },
  'party': { time: ['evening', 'night'], activity: ['party'], scene: ['home', 'club'], weather: ['sunny', 'cloudy'], mood: ['energetic', 'happy'] },
  'kids_mode': { time: ['morning', 'afternoon'], activity: ['family'], scene: ['home', 'car'], weather: ['sunny'], mood: ['happy', 'calm'] },
  'highway_cruise': { time: ['afternoon'], activity: ['driving'], scene: ['highway'], weather: ['sunny', 'cloudy'], mood: ['calm', 'hopeful'] },
  'sunset_drive': { time: ['evening'], activity: ['driving'], scene: ['highway', 'city'], weather: ['sunny'], mood: ['peaceful', 'nostalgic'] },
  'rainy_day': { time: ['morning', 'afternoon'], activity: ['driving', 'relax'], scene: ['city'], weather: ['rainy'], mood: ['calm', 'melancholy'] },
  'workout': { time: ['morning', 'afternoon'], activity: ['exercise'], scene: ['gym', 'home'], weather: ['sunny'], mood: ['energetic', 'uplifting'] },
  'meditation': { time: ['morning', 'evening'], activity: ['relax', 'meditation'], scene: ['home'], weather: ['sunny', 'cloudy'], mood: ['peaceful', 'calm'] },
  'weekend_leisure': { time: ['morning', 'afternoon'], activity: ['relax'], scene: ['home', 'park'], weather: ['sunny'], mood: ['happy', 'calm'] },
  'late_night_solo': { time: ['late_night'], activity: ['driving', 'relax'], scene: ['city', 'highway'], weather: ['night'], mood: ['melancholy', 'calm'] },
  'morning_energy': { time: ['morning'], activity: ['exercise', 'commute'], scene: ['city', 'gym'], weather: ['sunny'], mood: ['energetic', 'hopeful'] },
  'default': { time: ['morning', 'afternoon'], activity: ['driving'], scene: ['city'], weather: ['sunny'], mood: ['calm', 'happy'] }
};

function randomInRange(min, max) {
  return Math.round((Math.random() * (max - min) + min) * 100) / 100;
}

function generateAudioFeatures(track) {
  const genre = track.genre || 'pop';
  const genreConfig = GENRE_VALENCE_MAP[genre] || GENRE_VALENCE_MAP['pop'];
  
  const valence = randomInRange(genreConfig.valence[0], genreConfig.valence[1]);
  const acousticness = randomInRange(genreConfig.acousticness[0], genreConfig.acousticness[1]);
  const danceability = Math.min(1, Math.round((track.bpm / 150) * 100) / 100);
  const instrumentalness = track.language === 'instrumental' ? randomInRange(0.85, 0.98) : randomInRange(0.0, 0.15);
  const speechiness = track.language === 'instrumental' ? randomInRange(0.0, 0.05) : randomInRange(0.3, 0.6);
  
  return { valence, danceability, acousticness, instrumentalness, speechiness };
}

function generateTagsFromTrack(track, sceneType) {
  const tags = {
    mood_tags: [],
    activity_tags: [],
    weather_tags: [],
    time_tags: [],
    scene_tags: []
  };
  
  // 从场景映射获取基础标签
  const sceneConfig = SCENE_TAGS_MAP[sceneType] || SCENE_TAGS_MAP['default'];
  tags.mood_tags = [...(sceneConfig.mood || [])];
  tags.activity_tags = [...(sceneConfig.activity || [])];
  tags.weather_tags = [...(sceneConfig.weather || [])];
  tags.time_tags = [...(sceneConfig.time || [])];
  tags.scene_tags = [...(sceneConfig.scene || [])];
  
  // 根据 energy 和 valence 补充 mood_tags
  const energy = track.energy;
  const valence = track.valence || 0.5;
  
  if (energy > 0.7 && valence > 0.6 && !tags.mood_tags.includes('energetic')) {
    tags.mood_tags.push('energetic');
  }
  if (energy < 0.3 && valence < 0.4 && !tags.mood_tags.includes('calm')) {
    tags.mood_tags.push('calm');
  }
  if (valence > 0.7 && !tags.mood_tags.includes('happy')) {
    tags.mood_tags.push('happy');
  }
  if (valence < 0.3 && !tags.mood_tags.includes('melancholy')) {
    tags.mood_tags.push('melancholy');
  }
  
  // 去重
  Object.keys(tags).forEach(key => {
    tags[key] = [...new Set(tags[key])];
  });
  
  return tags;
}

function enhanceTrack(track, sceneType) {
  const audioFeatures = generateAudioFeatures(track);
  const tags = generateTagsFromTrack({ ...track, ...audioFeatures }, sceneType);
  
  return {
    ...track,
    ...audioFeatures,
    ...tags
  };
}

function enhanceMusicLibrary() {
  const libraryPath = path.join(__dirname, '../data/music_library.json');
  const library = JSON.parse(fs.readFileSync(libraryPath, 'utf8'));
  
  let totalEnhanced = 0;
  
  Object.keys(library.scenes).forEach(sceneType => {
    const scene = library.scenes[sceneType];
    if (scene.tracks) {
      scene.tracks = scene.tracks.map(track => {
        if (!track.valence) {
          totalEnhanced++;
          return enhanceTrack(track, sceneType);
        }
        return track;
      });
    }
  });
  
  library.last_updated = new Date().toISOString();
  library.version = '2.0';
  
  fs.writeFileSync(libraryPath, JSON.stringify(library, null, 2), 'utf8');
  
  console.log(`[TagEnhancer] Enhanced ${totalEnhanced} tracks with tags`);
  console.log(`[TagEnhancer] Library saved to ${libraryPath}`);
  
  return { totalEnhanced, library };
}

module.exports = {
  enhanceTrack,
  enhanceMusicLibrary,
  generateAudioFeatures,
  generateTagsFromTrack
};

if (require.main === module) {
  enhanceMusicLibrary();
}

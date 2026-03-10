const fs = require('fs');
const data = JSON.parse(fs.readFileSync('./data/index.json', 'utf8'));

console.log('=== scene_tags 分布 ===');
const sceneTags = {};
data.forEach(t => {
  if (t.scene_tags) {
    try {
      const tags = JSON.parse(t.scene_tags);
      tags.forEach(tag => {
        sceneTags[tag] = (sceneTags[tag] || 0) + 1;
      });
    } catch(e) {}
  }
});

Object.entries(sceneTags).sort((a,b) => b[1] - a[1]).forEach(([tag, count]) => {
  console.log(tag + ': ' + count);
});

console.log('\n=== 预置模板场景匹配情况 ===');
const TEMPLATE_SCENES = [
  'morning_commute', 'night_drive', 'road_trip', 'party',
  'romantic_date', 'family_trip', 'focus_work', 'relax',
  'workout', 'rainy_day', 'traffic_jam', 'fatigue_alert'
];
TEMPLATE_SCENES.forEach(s => {
  console.log(s + ': ' + (sceneTags[s] || 0) + ' 首');
});

console.log('\n=== genre 分布 ===');
const genres = {};
data.forEach(t => {
  if (t.genre) {
    genres[t.genre] = (genres[t.genre] || 0) + 1;
  }
});
Object.entries(genres).sort((a,b) => b[1] - a[1]).forEach(([tag, count]) => {
  console.log(tag + ': ' + count);
});

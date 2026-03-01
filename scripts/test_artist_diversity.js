#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { ContentEngine } = require('../src/layers/effects/engines/content');

console.log('=== 测试艺术家多样性限制 ===\n');

const engine = new ContentEngine({
  apiKey: process.env.DASHSCOPE_API_KEY,
  localJsonPath: './data/index.json'
});

async function test() {
  console.log('测试场景: family_trip (家庭出行)\n');
  
  const result = await engine.curatePlaylist(
    { 
      genres: ['pop'], 
      energy_level: 0.6,
      mood: ['happy', 'warm']
    },
    { max_tracks: 10 },
    'family_trip'
  );
  
  console.log(`数据来源: ${result.source}`);
  console.log(`返回曲目: ${result.playlist.length} 首`);
  console.log(`平均评分: ${result.avg_score || 'N/A'} 分\n`);
  
  console.log('推荐歌单:');
  const artistCount = {};
  result.playlist.forEach((track, i) => {
    const artist = track.artist || 'Unknown';
    artistCount[artist] = (artistCount[artist] || 0) + 1;
    console.log(`  ${i+1}. ${track.title} - ${track.artist} (评分: ${track.score || track._score || 'N/A'})`);
  });
  
  console.log('\n艺术家分布:');
  const sorted = Object.entries(artistCount).sort((a, b) => b[1] - a[1]);
  sorted.forEach(([artist, count]) => {
    console.log(`  ${artist}: ${count} 首`);
  });
  
  console.log('\n✅ 测试完成');
  console.log(`多样性检查: ${sorted[0][1] <= 3 ? '通过' : '失败'} (最多艺术家: ${sorted[0][0]} - ${sorted[0][1]} 首)`);
}

test().catch(console.error);

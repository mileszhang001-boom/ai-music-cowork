#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { contentEngine } = require('../src/layers/effects/engines/content');

console.log('=== 测试场景关键词匹配和推荐优化 ===\n');

async function testSceneMatching() {
  // 测试场景关键词匹配
  console.log('1. 测试场景关键词匹配\n');
  
  const testKeywords = [
    ['早晨', '上班', '通勤'],
    ['夜晚', '开车', '城市'],
    ['家庭', '孩子', '出游'],
    ['浪漫', '约会', '情侣'],
    ['运动', '健身', '跑步']
  ];
  
  for (const keywords of testKeywords) {
    const matchedScene = contentEngine.matchSceneFromKeywords(keywords);
    console.log(`关键词: [${keywords.join(', ')}]`);
    console.log(`  匹配场景: ${matchedScene || '无匹配'}\n`);
  }
  
  // 测试场景音乐特征
  console.log('2. 测试场景音乐特征获取\n');
  
  const scenes = ['morning_commute', 'family_outing', 'romantic_date', 'night_drive'];
  for (const scene of scenes) {
    const features = contentEngine.getSceneMusicFeatures(scene);
    console.log(`场景: ${scene}`);
    if (features) {
      console.log(`  能量范围: ${features.energy_range}`);
      console.log(`  情绪范围: ${features.valence_range}`);
      console.log(`  BPM范围: ${features.bpm_range}`);
      console.log(`  偏好流派: ${features.preferred_genres.join(', ')}`);
    } else {
      console.log('  无特征数据');
    }
    console.log('');
  }
  
  // 测试推荐效果
  console.log('3. 测试推荐效果\n');
  
  const testScene = 'family_outing';
  console.log(`测试场景: ${testScene}`);
  
  const result = await contentEngine.curatePlaylist(
    {},
    { max_tracks: 10 },
    testScene
  );
  
  console.log(`数据来源: ${result.source}`);
  console.log(`返回曲目: ${result.playlist.length} 首`);
  console.log(`平均评分: ${result.avg_score || 'N/A'} 分\n`);
  
  console.log('推荐歌单:');
  result.playlist.forEach((track, i) => {
    console.log(`  ${i+1}. ${track.title} - ${track.artist} (评分: ${track.score || 'N/A'})`);
  });
  
  // 测试高频歌曲
  console.log('\n4. 测试高频歌曲获取\n');
  
  for (const scene of scenes) {
    const highFreqSongs = contentEngine.getSceneHighFreqSongs(scene);
    console.log(`场景: ${scene}, 高频歌曲: ${highFreqSongs.length} 首`);
    if (highFreqSongs.length > 0) {
      console.log(`  示例: ${highFreqSongs[0].title} - ${highFreqSongs[0].artist}`);
    }
  }
}

testSceneMatching().catch(console.error);

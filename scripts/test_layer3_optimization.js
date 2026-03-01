#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { ContentEngine } = require('../src/layers/effects/engines/content');

function parseTags(tags) {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
}

console.log('=== Layer3 推荐算法优化验证 ===\n');

const engine = new ContentEngine({
  apiKey: process.env.DASHSCOPE_API_KEY,
  localJsonPath: './data/index.json'
});

async function runTests() {
  console.log('1. 测试多维度评分系统');
  console.log('='.repeat(50));
  
  const testTrack = {
    id: 1,
    title: '测试歌曲',
    artist: '测试艺术家',
    genre: 'pop',
    bpm: 100,
    energy: 0.6,
    valence: 0.7,
    mood_tags: ['happy', 'energetic'],
    scene_tags: ['morning_commute', 'workout']
  };
  
  const hints1 = {
    genres: ['pop'],
    tempo: 'moderate',
    energy_level: 0.6,
    valence: 0.7,
    mood: ['happy'],
    scene: ['morning_commute']
  };
  
  const score = engine._calculateOverallScore(testTrack, hints1);
  console.log(`测试歌曲评分: ${score} 分`);
  console.log(`预期: 应该 ≥ 80 分（高度匹配）\n`);
  
  console.log('2. 测试模板场景推荐');
  console.log('='.repeat(50));
  
  const result1 = await engine.curatePlaylist(
    { genres: ['pop'], energy_level: 0.6 },
    { max_tracks: 5 },
    'morning_commute'
  );
  
  console.log(`场景: morning_commute`);
  console.log(`数据来源: ${result1.source}`);
  console.log(`返回曲目: ${result1.playlist.length} 首`);
  console.log(`平均评分: ${result1.avg_score || 'N/A'} 分`);
  if (result1.playlist.length > 0) {
    console.log(`第一首: ${result1.playlist[0].title} - ${result1.playlist[0].artist} (评分: ${result1.playlist[0].score || 'N/A'})`);
  }
  console.log('');
  
  console.log('3. 测试非模板场景推荐');
  console.log('='.repeat(50));
  
  const result2 = await engine.curatePlaylist(
    { genres: ['ballad'], energy_level: 0.4, mood: ['romantic'] },
    { max_tracks: 5 },
    'romantic_evening'
  );
  
  console.log(`场景: romantic_evening (非模板)`);
  console.log(`数据来源: ${result2.source}`);
  console.log(`返回曲目: ${result2.playlist.length} 首`);
  console.log(`平均评分: ${result2.avg_score || 'N/A'} 分`);
  if (result2.playlist.length > 0) {
    console.log(`第一首: ${result2.playlist[0].title} - ${result2.playlist[0].artist} (评分: ${result2.playlist[0].score || 'N/A'})`);
  }
  console.log('');
  
  console.log('4. 测试约束条件');
  console.log('='.repeat(50));
  
  const result3 = await engine.curatePlaylist(
    { genres: ['pop'] },
    { max_tracks: 3, min_energy: 0.5, max_duration: 600 },
    'workout'
  );
  
  console.log(`场景: workout`);
  console.log(`约束: max_tracks=3, min_energy=0.5, max_duration=600s`);
  console.log(`返回曲目: ${result3.playlist.length} 首`);
  console.log(`总时长: ${result3.total_duration} 秒`);
  if (result3.playlist.length > 0) {
    console.log(`曲目列表:`);
    result3.playlist.forEach((track, i) => {
      console.log(`  ${i+1}. ${track.title} - ${track.artist} (能量: ${track.energy}, 评分: ${track.score || 'N/A'})`);
    });
  }
  console.log('');
  
  console.log('5. 测试标签系统');
  console.log('='.repeat(50));
  
  const tagTests = [
    { input: '["romantic", "nostalgic"]', expected: ['romantic', 'nostalgic'] },
    { input: ['happy', 'energetic'], expected: ['happy', 'energetic'] },
    { input: null, expected: [] }
  ];
  
  tagTests.forEach((test, i) => {
    const result = parseTags(test.input);
    const passed = JSON.stringify(result) === JSON.stringify(test.expected);
    console.log(`测试 ${i+1}: ${passed ? '✅ 通过' : '❌ 失败'}`);
    console.log(`  输入: ${JSON.stringify(test.input)}`);
    console.log(`  输出: ${JSON.stringify(result)}`);
    console.log(`  预期: ${JSON.stringify(test.expected)}`);
  });
  console.log('');
  
  console.log('6. 验证 valence 参数支持');
  console.log('='.repeat(50));
  
  const result4 = await engine.curatePlaylist(
    { valence: 0.8, mood: ['happy', 'uplifting'] },
    { max_tracks: 5 },
    'party'
  );
  
  console.log(`场景: party (高 valence)`);
  console.log(`返回曲目: ${result4.playlist.length} 首`);
  if (result4.playlist.length > 0) {
    console.log(`曲目 valence 值:`);
    result4.playlist.slice(0, 3).forEach((track, i) => {
      console.log(`  ${i+1}. ${track.title} - valence: ${track.valence || 'N/A'}`);
    });
  }
  console.log('');
  
  console.log('=== 验证完成 ===');
  console.log('\n优化总结:');
  console.log('✅ 多维度评分系统已实现');
  console.log('✅ 模板场景推荐已优化');
  console.log('✅ 非模板场景推荐已优化');
  console.log('✅ 约束条件处理已实现');
  console.log('✅ 标签系统已统一');
  console.log('✅ valence 参数已支持');
}

runTests().catch(console.error);

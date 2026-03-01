#!/usr/bin/env node
'use strict';

const path = require('path');

console.log('=== ContentEngine LLM 集成测试 ===\n');

// 测试 1: 检查环境变量
console.log('1. 检查环境变量:');
console.log('   DASHSCOPE_API_KEY:', process.env.DASHSCOPE_API_KEY ? '✅ 已设置' : '❌ 未设置');
console.log('');

// 测试 2: 检查音乐库加载
console.log('2. 检查音乐库:');
const fs = require('fs');
const libraryPath = path.join(__dirname, '../data/music_library.json');
if (fs.existsSync(libraryPath)) {
  const library = JSON.parse(fs.readFileSync(libraryPath, 'utf8'));
  const scenes = Object.keys(library.scenes || {});
  console.log('   ✅ 音乐库已加载');
  console.log('   可用场景:', scenes.slice(0, 5).join(', '), '...');
  console.log('   总场景数:', scenes.length);
} else {
  console.log('   ❌ 音乐库未找到');
}
console.log('');

// 测试 3: 测试 ContentEngine 初始化
console.log('3. 测试 ContentEngine 初始化:');
const { ContentEngine } = require('../src/layers/effects/engines/content');

const engineWithLLM = new ContentEngine({
  apiKey: 'test-api-key-for-demo'
});
console.log('   enableLLM:', engineWithLLM.config.enableLLM ? '✅ 已启用' : '❌ 未启用');
console.log('   llmClient:', engineWithLLM.llmClient ? '✅ 已初始化' : '❌ 未初始化');
console.log('');

const engineWithoutLLM = new ContentEngine({});
console.log('   无 API Key 时:');
console.log('   enableLLM:', engineWithoutLLM.config.enableLLM ? '✅ 已启用' : '❌ 未启用');
console.log('   llmClient:', engineWithoutLLM.llmClient ? '✅ 已初始化' : '❌ 未初始化');
console.log('');

// 测试 4: 测试场景判断逻辑
console.log('4. 测试场景判断逻辑:');
const availableScenes = engineWithLLM.getAvailableScenes();
console.log('   可用预设场景数:', availableScenes.length);

const testCases = [
  { sceneType: 'morning_commute', expected: 'library' },
  { sceneType: 'holiday_travel', expected: 'llm' },
  { sceneType: 'custom_scene', expected: 'llm' }
];

testCases.forEach(tc => {
  const isPreset = availableScenes.includes(tc.sceneType);
  console.log(`   ${tc.sceneType}: ${isPreset ? '预设场景' : '自定义场景'} → 应使用 ${isPreset ? 'library' : 'llm'}`);
});
console.log('');

// 测试 5: 模拟歌单生成流程
console.log('5. 模拟歌单生成流程:');
async function testPlaylistGeneration() {
  const engine = new ContentEngine({
    apiKey: process.env.DASHSCOPE_API_KEY || 'demo-key'
  });
  
  console.log('   测试预设场景 (morning_commute):');
  const result1 = await engine.curatePlaylist(
    { genres: ['pop'], tempo: 'moderate' },
    { max_tracks: 3 },
    'morning_commute'
  );
  console.log('   - 数据来源:', result1.source);
  console.log('   - 歌曲数量:', result1.playlist.length);
  if (result1.playlist.length > 0) {
    console.log('   - 第一首歌:', result1.playlist[0].title, '-', result1.playlist[0].artist);
  }
  console.log('');
  
  console.log('   测试自定义场景 (holiday_travel):');
  const result2 = await engine.curatePlaylist(
    { genres: ['pop', 'folk'], tempo: 'moderate' },
    { max_tracks: 3 },
    'holiday_travel'
  );
  console.log('   - 数据来源:', result2.source);
  console.log('   - 歌曲数量:', result2.playlist.length);
  if (result2.playlist.length > 0) {
    console.log('   - 第一首歌:', result2.playlist[0].title, '-', result2.playlist[0].artist);
  }
}

testPlaylistGeneration().then(() => {
  console.log('\n=== 测试完成 ===');
  console.log('\n💡 提示:');
  console.log('   - 如果 DASHSCOPE_API_KEY 未设置，LLM 生成将不可用');
  console.log('   - 预设场景会优先使用音乐库数据');
  console.log('   - 自定义场景会尝试调用 LLM 生成歌单');
  console.log('   - 如果 LLM 不可用，会降级使用 mock 数据');
}).catch(err => {
  console.error('测试失败:', err.message);
});

#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { ContentEngine } = require('../src/layers/effects/engines/content');

console.log('=== 验证 ContentEngine 逻辑 ===\n');

const engine = new ContentEngine({
  apiKey: process.env.DASHSCOPE_API_KEY || 'demo-key'
});

async function test() {
  // 测试 1: 预设场景
  console.log('1. 测试预设场景 (morning_commute):');
  const result1 = await engine.curatePlaylist(
    { genres: ['pop'], tempo: 'moderate' },
    { max_tracks: 3 },
    'morning_commute'
  );
  console.log('   数据来源:', result1.source);
  console.log('   歌曲数:', result1.playlist.length);
  if (result1.playlist[0]) {
    console.log('   示例:', result1.playlist[0].title, '-', result1.playlist[0].artist);
  }
  console.log('');

  // 测试 2: 非预设场景
  console.log('2. 测试非预设场景 (holiday_travel):');
  const result2 = await engine.curatePlaylist(
    { genres: ['pop', 'folk'], tempo: 'moderate' },
    { max_tracks: 3 },
    'holiday_travel'
  );
  console.log('   数据来源:', result2.source);
  console.log('   歌曲数:', result2.playlist.length);
  if (result2.playlist[0]) {
    console.log('   示例:', result2.playlist[0].title, '-', result2.playlist[0].artist);
  }
  console.log('');

  console.log('✅ 逻辑验证完成');
  console.log('- 预设场景: 使用音乐库数据（快速生成）');
  console.log('- 非预设场景: 尝试调用 LLM（需要 API Key）');
  console.log('- LLM 失败时: 降级使用 mock 数据');
}

test().catch(console.error);

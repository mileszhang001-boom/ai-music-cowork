#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { contentEngine } = require('../src/layers/effects/engines/content');

async function testTemplateScenes() {
  console.log('=== 测试模板场景识别 ===\n');
  
  // 测试场景列表
  const testScenes = [
    'family_outing',      // 家庭出行（海边度假属于这个类型）
    'road_trip',          // 公路旅行
    'holiday_travel',     // 假期旅行
    'morning_commute',    // 早晨通勤
    'night_drive',        // 夜间驾驶
    'sunny_day',          // 晴天
    'custom_scene'        // 自定义场景（应该不是模板）
  ];
  
  console.log('场景识别测试:');
  testScenes.forEach(scene => {
    const isTemplate = contentEngine.isTemplateScene(scene);
    console.log(`  ${scene}: ${isTemplate ? '✅ 模板场景' : '❌ 非模板场景'}`);
  });
  
  console.log('\n=== 测试推荐结果 ===\n');
  
  // 测试海边度假场景
  console.log('测试场景: family_outing (海边度假)');
  const result = await contentEngine.curatePlaylist(
    {
      genres: ['pop', 'reggae'],
      energy_level: 0.65,
      valence: 0.9
    },
    { max_tracks: 10 },
    'family_outing'
  );
  
  console.log(`数据来源: ${result.source}`);
  console.log(`返回曲目: ${result.playlist.length} 首`);
  console.log(`平均评分: ${result.avg_score || 'N/A'} 分`);
  
  console.log('\n推荐歌单:');
  result.playlist.slice(0, 5).forEach((track, i) => {
    console.log(`  ${i+1}. ${track.title} - ${track.artist} (评分: ${track.score || 'N/A'})`);
  });
  
  // 验证是否使用了缓存
  console.log('\n=== 缓存统计 ===');
  const stats = contentEngine.getCacheStats();
  console.log(`缓存数量: ${stats.count}`);
  console.log(`缓存大小: ${stats.totalSizeKB} KB`);
  console.log(`最后刷新: ${stats.lastRefreshTime}`);
}

testTemplateScenes().catch(console.error);

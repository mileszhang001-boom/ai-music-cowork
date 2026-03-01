#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { contentEngine } = require('../src/layers/effects/engines/content');
const { mapSceneToLightingTheme, ColorThemes } = require('../src/layers/effects/engines/lighting');

async function testCloudyDay() {
  console.log('=== 测试阴天驾驶场景 ===\n');
  
  // 测试灯光主题映射
  console.log('1. 灯光主题映射测试');
  const lightingTheme = mapSceneToLightingTheme('cloudy_day');
  const colors = ColorThemes[lightingTheme];
  console.log(`  场景: cloudy_day`);
  console.log(`  灯光主题: ${lightingTheme}`);
  console.log(`  主色调: ${colors.primary} (阴沉灰)`);
  console.log(`  辅助色: ${colors.secondary} (暗灰)`);
  console.log('');
  
  // 测试音乐推荐
  console.log('2. 音乐推荐测试');
  const result = await contentEngine.curatePlaylist(
    {},
    { max_tracks: 10 },
    'cloudy_day'
  );
  
  console.log(`  数据来源: ${result.source}`);
  console.log(`  返回曲目: ${result.playlist.length} 首`);
  console.log(`  平均评分: ${result.avg_score || 'N/A'} 分\n`);
  
  console.log('  推荐歌单:');
  result.playlist.forEach((track, i) => {
    console.log(`    ${i+1}. ${track.title} - ${track.artist}`);
    console.log(`       流派: ${track.genre || 'N/A'} | 能量: ${track.energy?.toFixed(2) || 'N/A'} | 情绪: ${track.valence?.toFixed(2) || 'N/A'}`);
  });
  
  // 统计流派分布
  console.log('\n  流派分布:');
  const genres = {};
  result.playlist.forEach(track => {
    const genre = track.genre || '未知';
    genres[genre] = (genres[genre] || 0) + 1;
  });
  Object.entries(genres).sort((a, b) => b[1] - a[1]).forEach(([genre, count]) => {
    console.log(`    ${genre}: ${count} 首`);
  });
  
  // 统计能量和情绪分布
  const avgEnergy = result.playlist.reduce((sum, t) => sum + (t.energy || 0), 0) / result.playlist.length;
  const avgValence = result.playlist.reduce((sum, t) => sum + (t.valence || 0), 0) / result.playlist.length;
  
  console.log('\n  平均能量值:', avgEnergy.toFixed(2), '(预期: 0.2-0.4)');
  console.log('  平均情绪值:', avgValence.toFixed(2), '(预期: 0.2-0.45)');
  
  // 验证是否符合预期
  const energyOK = avgEnergy >= 0.2 && avgEnergy <= 0.5;
  const valenceOK = avgValence >= 0.2 && avgValence <= 0.5;
  
  console.log('\n  ✅ 能量值符合预期:', energyOK ? '是' : '否');
  console.log('  ✅ 情绪值符合预期:', valenceOK ? '是' : '否');
}

testCloudyDay().catch(console.error);

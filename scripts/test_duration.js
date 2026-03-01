#!/usr/bin/env node
'use strict';

require('dotenv').config();

const { contentEngine } = require('../src/layers/effects/engines/content');

async function testDuration() {
  console.log('=== 测试歌单时长计算 ===\n');
  
  // 测试场景
  const result = await contentEngine.curatePlaylist(
    { genres: ['pop'], energy_level: 0.6 },
    { max_tracks: 5 },
    'family_outing'
  );
  
  console.log(`数据来源: ${result.source}`);
  console.log(`返回曲目: ${result.playlist.length} 首\n`);
  
  let totalSeconds = 0;
  
  result.playlist.forEach((track, i) => {
    const durationSec = track.duration_ms ? Math.floor(track.duration_ms / 1000) : (track.duration || 0);
    const duration = durationSec ? `${Math.floor(durationSec / 60)}:${String(Math.floor(durationSec % 60)).padStart(2, '0')}` : '--:--';
    
    console.log(`${i + 1}. ${track.title} - ${track.artist}`);
    console.log(`   duration_ms: ${track.duration_ms || 'N/A'}`);
    console.log(`   duration: ${track.duration || 'N/A'}`);
    console.log(`   时长: ${duration}`);
    
    totalSeconds += durationSec;
  });
  
  const totalMinutes = Math.floor(totalSeconds / 60);
  const remainingSeconds = Math.floor(totalSeconds % 60);
  
  console.log(`\n📊 歌单统计:`);
  console.log(`   总时长: ${totalMinutes}分${remainingSeconds}秒`);
  console.log(`   总秒数: ${totalSeconds}`);
}

testDuration().catch(console.error);

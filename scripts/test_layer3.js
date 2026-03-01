require('dotenv').config({ path: '.env.local' });
const { ContentEngine } = require('../src/layers/effects/engines/content');

async function test() {
  const engine = new ContentEngine();
  
  console.log('=== 测试非模板场景（LLM 选取）===\n');
  
  const startTime = Date.now();
  
  const result = await engine.curatePlaylist(
    { genres: ['rock'], energy_level: 0.8, tempo: 'fast' },
    {},
    'custom_rock_drive'
  );
  
  const elapsed = Date.now() - startTime;
  
  console.log('source:', result.source);
  console.log('strategy:', result.strategy);
  console.log('playlist count:', result.playlist.length);
  console.log('耗时:', elapsed + 'ms');
  
  if (result.playlist.length > 0) {
    console.log('\n前 3 首:');
    result.playlist.slice(0, 3).forEach((t, i) => {
      console.log('  ' + (i + 1) + '. ' + t.title + ' - ' + t.artist);
    });
  }
}

test();

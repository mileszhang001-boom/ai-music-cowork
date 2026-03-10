'use strict';

const { ContentEngine } = require('../../src/layers/effects/engines/content');
const { Models } = require('../../src/core/llm');

async function runTest() {
  console.log('=== Testing ContentEngine with LLM ===\n');

  const engine = new ContentEngine({
    enableLLM: true,
    model: Models.QWEN_PLUS,
    apiKey: process.env.DASHSCOPE_API_KEY
  });

  const hints = {
    genres: ['jazz', 'ambient'],
    tempo: 'slow',
    mood: 'relaxed'
  };

  const constraints = {
    max_tracks: 3
  };

  console.log('Hints:', hints);
  console.log('Constraints:', constraints);
  console.log('\nGenerating playlist...');

  try {
    const startTime = Date.now();
    const result = await engine.execute('curate_playlist', { hints, constraints });
    const duration = Date.now() - startTime;

    console.log(`\nSuccess! (took ${duration}ms)`);
    console.log(`Source: ${result.source}`);
    console.log(`Total Duration: ${result.total_duration}s`);
    console.log(`Average Energy: ${result.avg_energy.toFixed(2)}`);
    console.log('\nPlaylist:');
    result.playlist.forEach((track, i) => {
      console.log(`${i + 1}. ${track.title} - ${track.artist} (${track.genre}, ${track.bpm} BPM, Energy: ${track.energy})`);
      if (track.expression) console.log(`   Expression: ${track.expression}`);
      if (track.user_feedback) console.log(`   User Feedback: ${track.user_feedback}`);
      if (track.scene_match) console.log(`   Scene Match: ${track.scene_match}`);
      console.log('');
    });
  } catch (error) {
    console.error('\nTest failed:', error);
  }
}

runTest();

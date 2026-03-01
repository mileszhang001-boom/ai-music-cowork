'use strict';

const { ContentEngine } = require('../../src/layers/effects/engines/content');

async function runTest() {
  console.log('=== Testing ContentEngine with Local Music Library ===\n');

  const engine = new ContentEngine();

  console.log('Available Scenes:', engine.getAvailableScenes());
  console.log('');

  const testCases = [
    { scene_type: 'morning_commute', hints: { genres: ['pop', 'indie'], tempo: 'medium' }, constraints: { max_tracks: 5 } },
    { scene_type: 'night_drive', hints: { genres: ['jazz', 'ambient'], tempo: 'slow' }, constraints: { max_tracks: 5 } },
    { scene_type: 'road_trip', hints: { genres: ['rock', 'pop'], tempo: 'fast' }, constraints: { max_tracks: 5 } }
  ];

  for (const testCase of testCases) {
    console.log(`\n--- Testing Scene: ${testCase.scene_type} ---`);
    console.log('Hints:', testCase.hints);
    console.log('Constraints:', testCase.constraints);

    try {
      const startTime = Date.now();
      const result = await engine.execute('curate_playlist', {
        hints: testCase.hints,
        constraints: testCase.constraints,
        scene_type: testCase.scene_type
      });
      const duration = Date.now() - startTime;

      console.log(`\nSuccess! (took ${duration}ms)`);
      console.log(`Source: ${result.source}`);
      console.log(`Total Duration: ${result.total_duration}s`);
      console.log(`Average Energy: ${result.avg_energy.toFixed(2)}`);
      console.log('\nPlaylist:');
      result.playlist.forEach((track, i) => {
        console.log(`${i + 1}. ${track.title} - ${track.artist} (${track.genre}, ${track.bpm} BPM, Energy: ${track.energy})`);
        if (track.expression) console.log(`   Expression: ${track.expression}`);
      });
    } catch (error) {
      console.error('\nTest failed:', error);
    }
  }

  console.log('\n=== Test Complete ===');
}

runTest();

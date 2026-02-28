#!/usr/bin/env node
'use strict';

const { effectsLayer, EngineTypes } = require('../../src/layers/effects');
const { validator } = require('../../src/layers/effects/validator');
const { contentEngine } = require('../../src/layers/effects/engines/content');
const { lightingEngine } = require('../../src/layers/effects/engines/lighting');
const { audioEngine } = require('../../src/layers/effects/engines/audio');

const COLORS = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
  cyan: '\x1b[36m',
  magenta: '\x1b[35m'
};

function printHeader(title) {
  console.log(`\n${COLORS.cyan}════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.blue}  Layer 3: ${title}${COLORS.reset}`);
  console.log(`${COLORS.cyan}════════════════════════════════════════${COLORS.reset}`);
}

function printResult(label, value, valid = true) {
  const color = valid ? COLORS.green : COLORS.red;
  console.log(`  ${label}: ${color}${typeof value === 'object' ? JSON.stringify(value) : value}${COLORS.reset}`);
}

const mockDescriptors = {
  morning_commute: {
    version: '2.0',
    scene_id: 'scene_morning',
    scene_type: 'morning_commute',
    scene_name: '早晨通勤',
    intent: {
      mood: { valence: 0.6, arousal: 0.4 },
      energy_level: 0.4,
      atmosphere: 'fresh_morning'
    },
    hints: {
      music: { genres: ['pop', 'indie'], tempo: 'moderate' },
      lighting: { color_theme: 'warm', pattern: 'steady', intensity: 0.4 },
      audio: { preset: 'standard' }
    },
    announcement: '早安，为您准备了清新的晨间音乐',
    meta: { source: 'template' }
  },
  night_drive: {
    version: '2.0',
    scene_id: 'scene_night',
    scene_type: 'night_drive',
    scene_name: '深夜驾驶',
    intent: {
      mood: { valence: 0.5, arousal: 0.2 },
      energy_level: 0.2,
      atmosphere: 'serene_night'
    },
    hints: {
      music: { genres: ['jazz', 'lo-fi'], tempo: 'slow' },
      lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.2 },
      audio: { preset: 'night_mode' }
    },
    announcement: '深夜路况良好，为您切换至静谧夜行模式',
    meta: { source: 'template' }
  },
  fatigue_alert: {
    version: '2.0',
    scene_id: 'scene_fatigue',
    scene_type: 'fatigue_alert',
    scene_name: '疲劳提醒',
    intent: {
      mood: { valence: 0.4, arousal: 0.9 },
      energy_level: 0.9,
      atmosphere: 'alert_wake_up'
    },
    hints: {
      music: { genres: ['electronic', 'rock'], tempo: 'fast' },
      lighting: { color_theme: 'alert', pattern: 'flash', intensity: 0.8 },
      audio: { preset: 'bass_boost' }
    },
    announcement: '检测到您有点疲劳，为您切换到提神模式',
    meta: { source: 'template' }
  }
};

async function validateEffects() {
  printHeader('效果指令验证');

  const testOutput = {
    version: '1.0',
    scene_id: 'scene_test',
    commands: {
      content: { action: 'play_playlist', playlist: [] },
      lighting: { action: 'apply_theme', theme: 'calm', intensity: 0.3 },
      audio: { action: 'apply_preset', preset: 'standard' }
    },
    execution_report: {
      status: 'completed',
      timestamp: new Date().toISOString()
    }
  };

  console.log('\n测试输出:');
  console.log(JSON.stringify(testOutput, null, 2));

  const validation = validator.validate(testOutput);
  console.log('\n验证结果:');
  printResult('valid', validation.valid);
  if (!validation.valid) {
    printResult('errors', validation.errors, false);
  }

  return validation.valid;
}

async function testEngines() {
  printHeader('引擎测试');

  console.log(`\n${COLORS.yellow}内容引擎测试:${COLORS.reset}`);
  const contentResult = await contentEngine.execute('curate_playlist', {
    hints: { genres: ['jazz', 'lo-fi'], tempo: 'slow' }
  });
  printResult('playlist_length', contentResult.playlist?.length || 0);
  printResult('total_duration', `${Math.floor((contentResult.total_duration || 0) / 60)} 分钟`);
  if (contentResult.playlist?.[0]) {
    printResult('first_track', `${contentResult.playlist[0].title} - ${contentResult.playlist[0].artist}`);
  }

  console.log(`\n${COLORS.yellow}灯光引擎测试:${COLORS.reset}`);
  const lightingResult = await lightingEngine.execute('apply_theme', {
    theme: 'calm',
    pattern: 'breathing',
    intensity: 0.3
  });
  printResult('theme', lightingResult.theme);
  printResult('colors', lightingResult.colors);
  printResult('intensity', lightingResult.intensity);

  console.log(`\n${COLORS.yellow}音效引擎测试:${COLORS.reset}`);
  const audioResult = await audioEngine.execute('apply_preset', {
    preset: 'night_mode'
  });
  printResult('preset', audioResult.preset);
  printResult('settings', audioResult.settings);

  return true;
}

async function simulateOutput() {
  printHeader('效果输出模拟');

  for (const [key, descriptor] of Object.entries(mockDescriptors)) {
    console.log(`\n${COLORS.yellow}场景: ${descriptor.scene_name}${COLORS.reset}`);

    const output = await effectsLayer.process(descriptor);

    console.log(`\n${COLORS.cyan}输出 JSON:${COLORS.reset}`);
    console.log(JSON.stringify(output, null, 2));
  }

  return true;
}

async function generateMockInput() {
  printHeader('Mock 输入数据');

  console.log(`\n${COLORS.yellow}可用的 Mock Scene Descriptor 输入:${COLORS.reset}\n`);

  for (const [key, descriptor] of Object.entries(mockDescriptors)) {
    console.log(`${COLORS.cyan}const ${key}Descriptor = ${COLORS.reset}`);
    console.log(JSON.stringify(descriptor, null, 2));
    console.log('');
  }

  return true;
}

async function main() {
  console.log(`${COLORS.cyan}\n⚡ Layer 3 效果生成层 - 验证工具${COLORS.reset}`);

  const args = process.argv.slice(2);

  if (args.includes('--validate')) {
    await validateEffects();
  } else if (args.includes('--engines')) {
    await testEngines();
  } else if (args.includes('--simulate')) {
    await simulateOutput();
  } else if (args.includes('--mock')) {
    await generateMockInput();
  } else {
    await validateEffects();
    await testEngines();
    await simulateOutput();
  }

  console.log(`\n${COLORS.green}✅ Layer 3 验证完成${COLORS.reset}`);
  console.log(`${COLORS.cyan}用法: node scripts/layer3/demo.js [--validate|--engines|--simulate|--mock]${COLORS.reset}\n`);
}

main().catch(console.error);

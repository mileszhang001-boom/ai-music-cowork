#!/usr/bin/env node
'use strict';

const { semanticLayer, SceneTypes } = require('../../src/layers/semantic');
const { validator } = require('../../src/layers/semantic/validator');
const { templateLibrary } = require('../../src/layers/semantic/templates/library');

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
  console.log(`${COLORS.blue}  Layer 2: ${title}${COLORS.reset}`);
  console.log(`${COLORS.cyan}════════════════════════════════════════${COLORS.reset}`);
}

function printResult(label, value, valid = true) {
  const color = valid ? COLORS.green : COLORS.red;
  console.log(`  ${label}: ${color}${typeof value === 'object' ? JSON.stringify(value) : value}${COLORS.reset}`);
}

const mockPerceptionOutputs = {
  morning_commute: {
    version: '1.0',
    timestamp: new Date().toISOString(),
    signals: {
      vehicle: { speed_kmh: 70, passenger_count: 0 },
      environment: { time_of_day: 0.15, weather: 'clear' },
      biometric: { heart_rate: 72 },
      user_query: null
    },
    raw_signals: [],
    confidence: { overall: 0.85, by_source: {} }
  },
  night_drive: {
    version: '1.0',
    timestamp: new Date().toISOString(),
    signals: {
      vehicle: { speed_kmh: 50, passenger_count: 0 },
      environment: { time_of_day: 0.05, weather: 'clear' },
      biometric: { heart_rate: 65 },
      user_query: { text: '我想听点放松的音乐', intent: 'creative', confidence: 0.9 }
    },
    raw_signals: [],
    confidence: { overall: 0.80, by_source: {} }
  },
  road_trip: {
    version: '1.0',
    timestamp: new Date().toISOString(),
    signals: {
      vehicle: { speed_kmh: 120, passenger_count: 3 },
      environment: { time_of_day: 0.6, weather: 'sunny' },
      biometric: { heart_rate: 85 },
      user_query: { text: '来点嗨歌', intent: 'creative', confidence: 0.95 }
    },
    raw_signals: [],
    confidence: { overall: 0.90, by_source: {} }
  },
  fatigue_alert: {
    version: '1.0',
    timestamp: new Date().toISOString(),
    signals: {
      vehicle: { speed_kmh: 60, passenger_count: 0 },
      environment: { time_of_day: 0.4, weather: 'clear' },
      biometric: { heart_rate: 58, fatigue_level: 0.85 },
      user_query: null
    },
    raw_signals: [],
    confidence: { overall: 0.75, by_source: {} }
  }
};

async function validateDescriptor() {
  printHeader('Scene Descriptor 验证');

  const testDescriptor = {
    version: '2.0',
    scene_id: 'scene_test',
    scene_type: 'morning_commute',
    intent: {
      mood: { valence: 0.6, arousal: 0.4 },
      energy_level: 0.4,
      atmosphere: 'fresh_morning'
    },
    hints: {
      music: { genres: ['pop'], tempo: 'moderate' },
      lighting: { color_theme: 'warm', pattern: 'steady', intensity: 0.4 },
      audio: { preset: 'standard' }
    },
    announcement: '早安，为您准备了清新的晨间音乐'
  };

  console.log('\n测试 Descriptor:');
  console.log(JSON.stringify(testDescriptor, null, 2));

  const validation = validator.validate(testDescriptor);
  console.log('\n验证结果:');
  printResult('valid', validation.valid);
  if (!validation.valid) {
    printResult('errors', validation.errors, false);
  }

  return validation.valid;
}

async function testScenes() {
  printHeader('场景识别测试');

  for (const [key, perceptionOutput] of Object.entries(mockPerceptionOutputs)) {
    console.log(`\n${COLORS.yellow}场景: ${key}${COLORS.reset}`);

    const result = await semanticLayer.process(perceptionOutput, { enableLLM: false });

    printResult('scene_type', result.scene_descriptor?.scene_type);
    printResult('source', result.meta?.source);
    printResult('confidence', result.meta?.confidence?.toFixed(2));
    printResult('template_id', result.meta?.template_id || 'null');
  }

  return true;
}

async function testTemplates() {
  printHeader('模板库测试');

  const stats = templateLibrary.getStats();
  console.log('\n模板库统计:');
  printResult('总模板数', stats.total);
  printResult('预置模板', stats.bySource.preset);

  console.log('\n模板列表:');
  const templates = templateLibrary.getAllTemplates();
  templates.slice(0, 5).forEach((t, i) => {
    console.log(`  ${i + 1}. ${COLORS.magenta}${t.template_id}${COLORS.reset}: ${t.name} (${t.scene_type})`);
  });

  return true;
}

async function generateMockInput() {
  printHeader('Mock 输入数据');

  console.log(`\n${COLORS.yellow}可用的 Mock Perception 输出:${COLORS.reset}\n`);

  for (const [key, output] of Object.entries(mockPerceptionOutputs)) {
    console.log(`${COLORS.cyan}const ${key}Input = ${COLORS.reset}`);
    console.log(JSON.stringify(output, null, 2));
    console.log('');
  }

  return true;
}

async function main() {
  console.log(`${COLORS.cyan}\n🧠 Layer 2 语义推理层 - 验证工具${COLORS.reset}`);

  const args = process.argv.slice(2);

  if (args.includes('--validate')) {
    await validateDescriptor();
  } else if (args.includes('--scenes')) {
    await testScenes();
  } else if (args.includes('--templates')) {
    await testTemplates();
  } else if (args.includes('--mock')) {
    await generateMockInput();
  } else {
    await validateDescriptor();
    await testScenes();
    await testTemplates();
  }

  console.log(`\n${COLORS.green}✅ Layer 2 验证完成${COLORS.reset}`);
  console.log(`${COLORS.cyan}用法: node scripts/layer2/demo.js [--validate|--scenes|--templates|--mock]${COLORS.reset}\n`);
}

main().catch(console.error);

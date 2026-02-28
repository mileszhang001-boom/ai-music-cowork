#!/usr/bin/env node
'use strict';

const { perceptionLayer, SignalSources } = require('../../src/layers/perception');
const { validator } = require('../../src/layers/perception/validator');

const COLORS = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
  cyan: '\x1b[36m'
};

function printHeader(title) {
  console.log(`\n${COLORS.cyan}════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.blue}  Layer 1: ${title}${COLORS.reset}`);
  console.log(`${COLORS.cyan}════════════════════════════════════════${COLORS.reset}`);
}

function printResult(label, value, valid = true) {
  const color = valid ? COLORS.green : COLORS.red;
  console.log(`  ${label}: ${color}${JSON.stringify(value)}${COLORS.reset}`);
}

async function validateSignals() {
  printHeader('信号验证测试');

  const testSignals = [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.35, speed_kmh: 70 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.15 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
    { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 72 }
  ];

  console.log('\n输入信号:');
  testSignals.forEach((s, i) => {
    console.log(`  ${i + 1}. ${s.source}.${s.type}: ${JSON.stringify(s.value)}`);
  });

  const output = perceptionLayer.processBatch(testSignals);

  console.log('\n输出结构:');
  printResult('version', output.version);
  printResult('timestamp', output.timestamp);
  printResult('confidence.overall', output.confidence.overall.toFixed(2));

  const validation = validator.validate(output);
  console.log('\n验证结果:');
  printResult('valid', validation.valid);
  if (!validation.valid) {
    printResult('errors', validation.errors, false);
  }

  return validation.valid;
}

async function testSensors() {
  printHeader('传感器测试');

  const { 
    vhalSensor, 
    environmentSensor, 
    biometricSensor, 
    voiceSensor 
  } = require('../../src/layers/perception/sensors');

  console.log('\nVHAL 传感器:');
  const speedSignal = vhalSensor.readSpeed(70);
  printResult('readSpeed(70)', speedSignal);

  console.log('\n环境传感器:');
  const timeSignal = environmentSensor.readTimeOfDay(8);
  printResult('readTimeOfDay(8)', timeSignal);
  const weatherSignal = environmentSensor.readWeather('rain');
  printResult('readWeather("rain")', weatherSignal);

  console.log('\n生物传感器:');
  const hrSignal = biometricSensor.readHeartRate(72);
  printResult('readHeartRate(72)', hrSignal);

  console.log('\n语音传感器:');
  const voiceSignal = voiceSensor.read('帮我选一首适合现在的歌');
  printResult('read("帮我选一首适合现在的歌")', voiceSignal);

  return true;
}

async function generateMockData() {
  printHeader('Mock 数据生成');

  const scenarios = {
    morning_commute: {
      name: '早晨通勤',
      signals: [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.35, speed_kmh: 70 } },
        { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.15 } },
        { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
        { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 72 }
      ]
    },
    night_drive: {
      name: '深夜驾驶',
      signals: [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.25, speed_kmh: 50 } },
        { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.05 } },
        { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 65 }
      ]
    },
    fatigue_alert: {
      name: '疲劳提醒',
      signals: [
        { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.3, speed_kmh: 60 } },
        { source: SignalSources.BIOMETRIC, type: 'fatigue_level', value: 0.85 },
        { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 58 }
      ]
    }
  };

  console.log('\n生成的 Mock 数据:\n');

  for (const [key, scenario] of Object.entries(scenarios)) {
    console.log(`${COLORS.yellow}场景: ${scenario.name}${COLORS.reset}`);
    const output = perceptionLayer.processBatch(scenario.signals);
    console.log(JSON.stringify(output, null, 2));
    console.log('');
  }

  return true;
}

async function main() {
  console.log(`${COLORS.cyan}\n🚗 Layer 1 物理感知层 - 验证工具${COLORS.reset}`);

  const args = process.argv.slice(2);

  if (args.includes('--validate')) {
    await validateSignals();
  } else if (args.includes('--sensors')) {
    await testSensors();
  } else if (args.includes('--mock')) {
    await generateMockData();
  } else {
    await validateSignals();
    await testSensors();
    await generateMockData();
  }

  console.log(`\n${COLORS.green}✅ Layer 1 验证完成${COLORS.reset}`);
  console.log(`${COLORS.cyan}用法: node scripts/layer1/demo.js [--validate|--sensors|--mock]${COLORS.reset}\n`);
}

main().catch(console.error);

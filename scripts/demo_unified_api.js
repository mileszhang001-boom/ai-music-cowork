#!/usr/bin/env node
'use strict';

const { InCarAI, SignalBuilder, SignalSources } = require('../src/index');

const COLORS = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
  red: '\x1b[31m'
};

function colorize(text, color) {
  return `${COLORS[color]}${text}${COLORS.reset}`;
}

function printSeparator(title) {
  console.log('\n' + colorize('═'.repeat(60), 'cyan'));
  console.log(colorize(`  ${title}`, 'bright'));
  console.log(colorize('═'.repeat(60), 'cyan'));
}

async function demoBasicUsage() {
  printSeparator('基础用法: 环境参数 + 用户Query → JSON输出');

  const ai = new InCarAI({ debug: true });

  console.log(colorize('\n📥 输入:', 'blue'));
  console.log('  环境参数:');
  console.log('    - 车速: 70 km/h');
  console.log('    - 时间: 早晨 8:00');
  console.log('    - 天气: 晴天');
  console.log('    - 心率: 72 bpm');
  console.log('  用户Query: "帮我选一首适合现在的歌"');

  const result = await ai.process({
    signals: [
      SignalBuilder.vhal('vehicle_speed', { vehicle_speed: 0.35 }),
      SignalBuilder.environment('time_of_day', { time_of_day: 0.15 }),
      SignalBuilder.environment('weather', { weather: 'clear' }),
      SignalBuilder.biometric('heart_rate', 72)
    ],
    context: { speed: 70, passengerCount: 0 },
    userQuery: '帮我选一首适合现在的歌'
  });

  console.log(colorize('\n📤 输出 Scene Descriptor JSON:', 'green'));
  console.log(JSON.stringify(result.scene_descriptor, null, 2));

  console.log(colorize('\n📊 元数据:', 'magenta'));
  console.log(JSON.stringify(result.metadata, null, 2));
}

async function demoNightDrive() {
  printSeparator('深夜驾驶场景');

  const ai = new InCarAI({ debug: true });

  console.log(colorize('\n📥 输入:', 'blue'));
  console.log('  环境参数: 深夜、独自驾驶、安静');
  console.log('  用户Query: "我想听点放松的音乐"');

  const result = await ai.process({
    signals: [
      SignalBuilder.vhal('vehicle_speed', { vehicle_speed: 0.25 }),
      SignalBuilder.environment('time_of_day', { time_of_day: 0.05 }),
      SignalBuilder.environment('weather', { weather: 'clear' }),
      SignalBuilder.biometric('heart_rate', 65)
    ],
    context: { speed: 50, passengerCount: 0 },
    userQuery: '我想听点放松的音乐'
  });

  console.log(colorize('\n📤 输出 JSON:', 'green'));
  console.log(JSON.stringify(result.scene_descriptor, null, 2));
}

async function demoRoadTrip() {
  printSeparator('朋友出游场景');

  const ai = new InCarAI({ debug: true });

  console.log(colorize('\n📥 输入:', 'blue'));
  console.log('  环境参数: 白天、3位乘客、高速行驶');
  console.log('  用户Query: "来点嗨歌"');

  const result = await ai.process({
    signals: [
      SignalBuilder.vhal('vehicle_speed', { vehicle_speed: 0.6 }),
      SignalBuilder.vhal('passenger_count', { passenger_count: 3 }),
      SignalBuilder.environment('time_of_day', { time_of_day: 0.6 }),
      SignalBuilder.environment('weather', { weather: 'sunny' }),
      SignalBuilder.biometric('heart_rate', 85)
    ],
    context: { speed: 120, passengerCount: 3 },
    userQuery: '来点嗨歌'
  });

  console.log(colorize('\n📤 输出 JSON:', 'green'));
  console.log(JSON.stringify(result.scene_descriptor, null, 2));
}

async function demoFatigueAlert() {
  printSeparator('疲劳提醒场景');

  const ai = new InCarAI({ debug: true });

  console.log(colorize('\n📥 输入:', 'blue'));
  console.log('  环境参数: 检测到疲劳');
  console.log('  用户Query: null (自动触发)');

  const result = await ai.process({
    signals: [
      SignalBuilder.vhal('vehicle_speed', { vehicle_speed: 0.3 }),
      SignalBuilder.biometric('fatigue_level', 0.85),
      SignalBuilder.biometric('heart_rate', 58)
    ],
    context: { speed: 60, fatigueLevel: 0.85 }
  });

  console.log(colorize('\n📤 输出 JSON:', 'green'));
  console.log(JSON.stringify(result.scene_descriptor, null, 2));
}

async function demoFastTrackOnly() {
  printSeparator('仅快通道模式 (不调用LLM)');

  const ai = new InCarAI({ 
    debug: true,
    enableLLM: false
  });

  console.log(colorize('\n📥 输入:', 'blue'));
  console.log('  模式: 快通道优先，不调用云端LLM');

  const result = await ai.process({
    signals: [
      SignalBuilder.vhal('vehicle_speed', { vehicle_speed: 0.35 }),
      SignalBuilder.environment('time_of_day', { time_of_day: 0.15 }),
      SignalBuilder.environment('weather', { weather: 'clear' })
    ],
    context: { speed: 70 },
    userQuery: '播放适合早晨的音乐'
  });

  console.log(colorize('\n📤 输出 JSON:', 'green'));
  console.log(JSON.stringify(result.scene_descriptor, null, 2));
  console.log(colorize('\n📊 元数据 (注意 track: fast):', 'magenta'));
  console.log(JSON.stringify(result.metadata, null, 2));
}

async function demoCustomInput() {
  printSeparator('自定义输入示例');

  const ai = new InCarAI({ debug: true });

  const customSignals = [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.4 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.5 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' } },
    { source: SignalSources.BIOMETRIC, type: 'stress_level', value: 0.6 }
  ];

  console.log(colorize('\n📥 自定义信号:', 'blue'));
  customSignals.forEach((s, i) => {
    console.log(`  ${i + 1}. ${s.source}.${s.type}: ${JSON.stringify(s.value)}`);
  });

  const result = await ai.process({
    signals: customSignals,
    context: { speed: 80, weather: 'rain' },
    userQuery: '下雨天堵车，放点轻松的歌'
  });

  console.log(colorize('\n📤 输出 JSON:', 'green'));
  console.log(JSON.stringify(result.scene_descriptor, null, 2));
}

function printUsageGuide() {
  printSeparator('API 使用指南');

  console.log(colorize('\n📝 基础用法:', 'yellow'));
  console.log(`
const { InCarAI, SignalBuilder } = require('./src/index');

const ai = new InCarAI({ debug: true });

const result = await ai.process({
  signals: [
    SignalBuilder.vhal('vehicle_speed', { vehicle_speed: 0.35 }),
    SignalBuilder.environment('time_of_day', { time_of_day: 0.15 }),
    SignalBuilder.environment('weather', { weather: 'clear' }),
    SignalBuilder.biometric('heart_rate', 72)
  ],
  context: { speed: 70, passengerCount: 0 },
  userQuery: '帮我选一首适合现在的歌'
});

console.log(result.scene_descriptor);
console.log(result.metadata);
`);

  console.log(colorize('\n📋 可用信号源 (SignalSources):', 'yellow'));
  console.log('  - vhal (车辆信号): vehicle_speed, passenger_count, gear_position...');
  console.log('  - environment (环境信号): time_of_day, weather, temperature...');
  console.log('  - biometric (生物信号): heart_rate, fatigue_level, stress_level...');
  console.log('  - voice (语音信号): 用户语音输入');
  console.log('  - user_profile (用户画像): 音乐偏好、历史行为...');
  console.log('  - music_state (音乐状态): 当前播放状态...');

  console.log(colorize('\n⚙️  配置选项:', 'yellow'));
  console.log('  - debug: true/false - 打印调试信息');
  console.log('  - enableLLM: true/false - 是否启用云端LLM');
  console.log('  - trackMode: "dual"/"fast_only" - 通道模式');
  console.log('  - enableLearning: true/false - 是否启用模板学习');

  console.log(colorize('\n📤 输出结构:', 'yellow'));
  console.log(`
{
  scene_descriptor: {
    version: '2.0',
    scene_id: 'scene_xxx',
    scene_type: 'morning_commute',
    scene_name: '早晨通勤',
    scene_narrative: '...',
    intent: { atmosphere, energy_level, mood, constraints },
    hints: { music, lighting, audio },
    announcement: '...',
    meta: { source, template_id, confidence, created_at }
  },
  metadata: {
    track: 'fast' | 'slow',
    source: 'template' | 'llm' | 'fallback',
    processing_time_ms: 123,
    scene_confidence: 0.85
  }
}
`);
}

async function main() {
  const args = process.argv.slice(2);

  if (args.includes('--help') || args.includes('-h')) {
    printUsageGuide();
    return;
  }

  console.log(colorize('\n🚗 车载座舱 AI 娱乐融合方案 - 统一输入输出接口演示', 'bright'));
  console.log(colorize('═'.repeat(60), 'cyan'));

  if (args.includes('--basic')) {
    await demoBasicUsage();
  } else if (args.includes('--night')) {
    await demoNightDrive();
  } else if (args.includes('--roadtrip')) {
    await demoRoadTrip();
  } else if (args.includes('--fatigue')) {
    await demoFatigueAlert();
  } else if (args.includes('--fast')) {
    await demoFastTrackOnly();
  } else if (args.includes('--custom')) {
    await demoCustomInput();
  } else {
    await demoBasicUsage();
    await demoNightDrive();
    await demoFastTrackOnly();
  }

  console.log(colorize('\n\n✅ 演示完成!', 'green'));
  console.log(colorize('\n提示: 使用 --help 查看API使用指南', 'cyan'));
  console.log(colorize('可用选项: --basic, --night, --roadtrip, --fatigue, --fast, --custom', 'cyan'));
}

main().catch(console.error);

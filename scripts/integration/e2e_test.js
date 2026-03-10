#!/usr/bin/env node
'use strict';

const { InCarAI, SignalSources } = require('../../src/index');

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

function printSeparator(title) {
  console.log('\n' + COLORS.cyan + '═'.repeat(60) + COLORS.reset);
  console.log(COLORS.bright + `  ${title}` + COLORS.reset);
  console.log(COLORS.cyan + '═'.repeat(60) + COLORS.reset);
}

async function runE2ETest() {
  printSeparator('端到端测试: 三层架构协作');

  const ai = new InCarAI({ debug: true, enableLLM: false });

  console.log(COLORS.yellow + '\n📥 输入:' + COLORS.reset);
  console.log('  环境参数:');
  console.log('    - 车速: 70 km/h');
  console.log('    - 时间: 早晨 8:00');
  console.log('    - 天气: 晴天');
  console.log('    - 心率: 72 bpm');
  console.log('  用户Query: "帮我选一首适合现在的歌"');

  const signals = [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.35, speed_kmh: 70 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.15 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
    { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 72 }
  ];

  const result = await ai.process({
    signals,
    context: { speed: 70, passengerCount: 0 },
    userQuery: '帮我选一首适合现在的歌'
  });

  console.log(COLORS.green + '\n📤 Layer 1 输出 (物理感知层):' + COLORS.reset);
  console.log('  version:', result.perception.version);
  console.log('  confidence:', result.perception.confidence.overall.toFixed(2));
  console.log('  signals.vehicle:', JSON.stringify(result.perception.signals.vehicle));
  console.log('  signals.environment:', JSON.stringify(result.perception.signals.environment));

  console.log(COLORS.magenta + '\n📤 Layer 2 输出 (语义推理层):' + COLORS.reset);
  console.log('  scene_id:', result.semantic.scene_descriptor?.scene_id);
  console.log('  scene_type:', result.semantic.scene_descriptor?.scene_type);
  console.log('  scene_name:', result.semantic.scene_descriptor?.scene_name);
  console.log('  source:', result.semantic.meta?.source);
  console.log('  intent:', JSON.stringify(result.semantic.scene_descriptor?.intent));
  console.log('  hints.music:', JSON.stringify(result.semantic.scene_descriptor?.hints?.music));

  console.log(COLORS.blue + '\n📤 Layer 3 输出 (效果生成层):' + COLORS.reset);
  console.log('  scene_id:', result.effects.scene_id);
  console.log('  status:', result.effects.execution_report?.status);
  console.log('  commands.content:', result.effects.commands?.content ? '✓' : '✗');
  console.log('  commands.lighting:', result.effects.commands?.lighting ? '✓' : '✗');
  console.log('  commands.audio:', result.effects.commands?.audio ? '✓' : '✗');

  console.log(COLORS.cyan + '\n📊 元数据:' + COLORS.reset);
  console.log('  total_processing_time_ms:', result.meta.total_processing_time_ms);

  if (result.effects.commands?.content?.playlist) {
    console.log(COLORS.green + '\n🎵 生成的播放列表:' + COLORS.reset);
    result.effects.commands.content.playlist.slice(0, 3).forEach((track, i) => {
      console.log(`  ${i + 1}. ${track.title} - ${track.artist} (${track.genre})`);
    });
  }

  if (result.effects.commands?.lighting) {
    console.log(COLORS.yellow + '\n💡 灯光效果:' + COLORS.reset);
    console.log('  theme:', result.effects.commands.lighting.theme);
    console.log('  colors:', JSON.stringify(result.effects.commands.lighting.colors));
    console.log('  intensity:', result.effects.commands.lighting.intensity);
  }

  if (result.effects.commands?.audio) {
    console.log(COLORS.magenta + '\n🔊 音效设置:' + COLORS.reset);
    console.log('  preset:', result.effects.commands.audio.preset);
    console.log('  eq:', JSON.stringify(result.effects.commands.audio.eq));
  }

  console.log(COLORS.green + '\n✅ 端到端测试完成!' + COLORS.reset);

  return result;
}

async function runLayerIsolationTest() {
  printSeparator('层隔离测试: 各层独立运行');

  const ai = new InCarAI({ enableLLM: false });

  console.log(COLORS.yellow + '\n测试 Layer 1 独立运行:' + COLORS.reset);
  const perceptionOutput = await ai.processSignalsOnly([
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.5, speed_kmh: 100 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.5 } }
  ]);
  console.log('  ✓ Layer 1 独立运行成功');
  console.log('  输出 confidence:', perceptionOutput.confidence.overall.toFixed(2));

  console.log(COLORS.yellow + '\n测试 Layer 3 独立运行:' + COLORS.reset);
  const mockDescriptor = {
    version: '2.0',
    scene_id: 'test_scene',
    intent: { mood: { valence: 0.5, arousal: 0.5 }, energy_level: 0.5 },
    hints: {
      music: { genres: ['pop'] },
      lighting: { color_theme: 'calm', intensity: 0.3 },
      audio: { preset: 'standard' }
    }
  };
  const effectsOutput = await ai.processDescriptorOnly(mockDescriptor);
  console.log('  ✓ Layer 3 独立运行成功');
  console.log('  输出 status:', effectsOutput.execution_report?.status);

  console.log(COLORS.green + '\n✅ 层隔离测试完成!' + COLORS.reset);
}

async function main() {
  console.log(COLORS.bright + '\n🚗 车载座舱 AI 娱乐融合方案 - 集成测试' + COLORS.reset);
  console.log(COLORS.cyan + '═'.repeat(60) + COLORS.reset);

  const args = process.argv.slice(2);

  if (args.includes('--isolation')) {
    await runLayerIsolationTest();
  } else {
    await runE2ETest();
    await runLayerIsolationTest();
  }

  console.log(COLORS.cyan + '\n用法: node scripts/integration/e2e_test.js [--isolation]' + COLORS.reset);
}

main().catch(console.error);

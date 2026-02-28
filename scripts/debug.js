#!/usr/bin/env node
'use strict';

const readline = require('readline');

const { perceptionLayer, SignalSources } = require('../src/layers/perception');
const { semanticLayer } = require('../src/layers/semantic');
const { effectsLayer } = require('../src/layers/effects');
const { rulesEngine } = require('../src/core/rulesEngine');

const COLORS = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  dim: '\x1b[2m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
  cyan: '\x1b[36m',
  magenta: '\x1b[35m',
  bgBlue: '\x1b[44m'
};

const SCENARIOS = {
  '1': {
    name: '早晨通勤',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 70 } },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.35 } },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 72 }
    ],
    context: { passengerComposition: ['adult'] }
  },
  '2': {
    name: '深夜驾驶',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 50 } },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.05 } },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 65 }
    ],
    context: { passengerComposition: ['adult'] }
  },
  '3': {
    name: '疲劳提醒',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 60 } },
      { source: SignalSources.BIOMETRIC, type: 'fatigue_level', value: 0.85 },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 58 }
    ],
    context: { passengerComposition: ['adult'] }
  },
  '4': {
    name: '家庭出行(儿童)',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 80 } },
      { source: SignalSources.VHAL, type: 'passenger_count', value: { passenger_count: 3 } },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'sunny' } }
    ],
    context: { passengerComposition: ['adult', 'adult', 'child'] }
  },
  '5': {
    name: '雨夜驾驶',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 40 } },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.1 } },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' } }
    ],
    context: { passengerComposition: ['adult'] }
  },
  '6': {
    name: '语音请求',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 80 } },
      { source: SignalSources.VOICE, type: 'user_query', value: { text: '来点嗨歌', intent: 'creative' } }
    ],
    context: { passengerComposition: ['adult'] }
  }
};

const LAYERS = {
  '1': { name: 'Layer 1', key: 'layer1' },
  '2': { name: 'Layer 2', key: 'layer2' },
  '3': { name: 'Layer 3', key: 'layer3' },
  '4': { name: '全链路', key: 'full' },
  'full': { name: '全链路', key: 'full' }
};

let rl;
let currentLayer = '4';
let currentScenario = '1';

function printBox(title, content, color = COLORS.cyan) {
  const width = 60;
  const line = '─'.repeat(width - 2);
  console.log(`\n${color}┌${line}┐${COLORS.reset}`);
  console.log(`${color}│ ${COLORS.bold}${title.padEnd(width - 3)}${COLORS.reset}${color}│${COLORS.reset}`);
  console.log(`${color}├${line}┤${COLORS.reset}`);
  content.forEach(line => {
    console.log(`${color}│${COLORS.reset} ${line.padEnd(width - 3)} ${color}│${COLORS.reset}`);
  });
  console.log(`${color}└${line}┘${COLORS.reset}`);
}

function formatJson(obj, indent = 0) {
  return JSON.stringify(obj, null, 2).split('\n').map(l => ' '.repeat(indent) + l);
}

async function runFullChain(scenario) {
  console.log(`\n${COLORS.bold}${COLORS.bgBlue} 🚗 ${scenario.name} ${COLORS.reset}\n`);

  const startTime = Date.now();

  const layer1Output = perceptionLayer.processBatch(scenario.signals);
  
  const layer2Result = await semanticLayer.process(layer1Output, { 
    enableLLM: false, 
    context: scenario.context 
  });
  const descriptor = layer2Result.scene_descriptor;

  const validation = rulesEngine.validate(descriptor, scenario.context);
  let finalDescriptor = descriptor;
  if (!validation.passed) {
    finalDescriptor = rulesEngine.applyFixes(descriptor, scenario.context);
  }

  const layer3Output = await effectsLayer.process(finalDescriptor);

  const totalTime = Date.now() - startTime;

  printBox('📥 输入', [
    `场景: ${scenario.name}`,
    `信号: ${scenario.signals.length} 个`,
    `乘客: ${scenario.context.passengerComposition?.join(', ') || '未知'}`
  ], COLORS.blue);

  printBox('⚙️ 处理过程', [
    `Layer 1 置信度: ${(layer1Output.confidence.overall * 100).toFixed(0)}%`,
    `Layer 2 场景: ${descriptor.scene_type} (${descriptor.scene_name})`,
    `Layer 2 来源: ${layer2Result.meta.source}`,
    `规则校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`,
    `Layer 3 状态: ${layer3Output.execution_report?.status || 'completed'}`
  ], COLORS.yellow);

  printBox('📤 输出', [
    `🎵 音乐: ${layer3Output.commands?.content?.playlist?.length || 0} 首`,
    `   首曲: ${layer3Output.commands?.content?.playlist?.[0]?.title || '无'}`,
    `💡 灯光: ${layer3Output.commands?.lighting?.theme || 'default'} 主题`,
    `   亮度: ${layer3Output.commands?.lighting?.intensity || 0}`,
    `🔊 音效: ${layer3Output.commands?.audio?.preset || 'standard'} 预设`,
    `📢 播报: "${descriptor.announcement?.text || layer3Output.announcement?.text || '无'}"`
  ], COLORS.green);

  printBox('📊 执行摘要', [
    `总耗时: ${totalTime}ms`,
    `场景ID: ${descriptor.scene_id}`,
    `能量级别: ${descriptor.intent?.energy_level || 0}`
  ], COLORS.magenta);

  return { layer1Output, descriptor, layer3Output, validation };
}

async function runLayer1(scenario) {
  console.log(`\n${COLORS.bold}${COLORS.cyan} Layer 1: 物理感知层 ${COLORS.reset}\n`);
  
  const startTime = Date.now();
  const output = perceptionLayer.processBatch(scenario.signals);
  const totalTime = Date.now() - startTime;

  printBox('📥 输入信号', scenario.signals.map((s, i) => 
    `${i + 1}. ${s.source}.${s.type}: ${JSON.stringify(s.value)}`
  ), COLORS.blue);

  printBox('📤 输出', [
    `版本: ${output.version}`,
    `时间戳: ${output.timestamp}`,
    `置信度: ${(output.confidence.overall * 100).toFixed(0)}%`,
    `活跃源: ${output._meta?.active_sources?.join(', ') || 'N/A'}`
  ], COLORS.green);

  printBox('📊 执行摘要', [
    `处理耗时: ${totalTime}ms`,
    `信号总数: ${output._meta?.total_count || scenario.signals.length}`
  ], COLORS.magenta);

  return output;
}

async function runLayer2(scenario) {
  console.log(`\n${COLORS.bold}${COLORS.cyan} Layer 2: 语义推理层 ${COLORS.reset}\n`);
  
  const layer1Output = perceptionLayer.processBatch(scenario.signals);
  
  const startTime = Date.now();
  const result = await semanticLayer.process(layer1Output, { 
    enableLLM: false, 
    context: scenario.context 
  });
  const totalTime = Date.now() - startTime;
  const descriptor = result.scene_descriptor;

  const validation = rulesEngine.validate(descriptor, scenario.context);

  printBox('📥 输入 (Layer 1 输出)', [
    `置信度: ${(layer1Output.confidence.overall * 100).toFixed(0)}%`,
    `用户请求: ${layer1Output.signals?.user_query?.text || '无'}`
  ], COLORS.blue);

  printBox('📤 输出 (Scene Descriptor)', [
    `场景类型: ${descriptor.scene_type}`,
    `场景名称: ${descriptor.scene_name}`,
    `来源: ${result.meta.source}`,
    `能量级别: ${descriptor.intent?.energy_level || 0}`,
    `情绪: valence=${descriptor.intent?.mood?.valence || 0}, arousal=${descriptor.intent?.mood?.arousal || 0}`
  ], COLORS.green);

  printBox('🔍 规则校验', [
    `结果: ${validation.passed ? '✅ 通过' : '❌ 失败'}`,
    `违规数: ${validation.violations.length}`,
    `警告数: ${validation.warnings.length}`
  ], validation.passed ? COLORS.green : COLORS.red);

  printBox('📊 执行摘要', [
    `处理耗时: ${totalTime}ms`,
    `场景ID: ${descriptor.scene_id}`
  ], COLORS.magenta);

  return { descriptor, validation };
}

async function runLayer3(scenario) {
  console.log(`\n${COLORS.bold}${COLORS.cyan} Layer 3: 效果生成层 ${COLORS.reset}\n`);
  
  const layer1Output = perceptionLayer.processBatch(scenario.signals);
  const layer2Result = await semanticLayer.process(layer1Output, { 
    enableLLM: false, 
    context: scenario.context 
  });
  const descriptor = layer2Result.scene_descriptor;

  const startTime = Date.now();
  const output = await effectsLayer.process(descriptor);
  const totalTime = Date.now() - startTime;

  printBox('📥 输入 (Scene Descriptor)', [
    `场景: ${descriptor.scene_type} (${descriptor.scene_name})`,
    `能量: ${descriptor.intent?.energy_level || 0}`,
    `音乐提示: ${(descriptor.hints?.music?.genres || []).join(', ') || '无'}`
  ], COLORS.blue);

  printBox('📤 输出 (Effect Commands)', [
    `🎵 音乐: ${output.commands?.content?.playlist?.length || 0} 首`,
    `💡 灯光: ${output.commands?.lighting?.theme || 'default'}`,
    `🔊 音效: ${output.commands?.audio?.preset || 'standard'}`,
    `状态: ${output.execution_report?.status || 'completed'}`
  ], COLORS.green);

  printBox('📊 执行摘要', [
    `处理耗时: ${totalTime}ms`,
    `执行时间: ${output.execution_report?.execution_time_ms || 0}ms`
  ], COLORS.magenta);

  return output;
}

async function runSingleLayer(layerKey, scenario) {
  switch (layerKey) {
    case 'layer1': await runLayer1(scenario); break;
    case 'layer2': await runLayer2(scenario); break;
    case 'layer3': await runLayer3(scenario); break;
  }
}

function showMainMenu() {
  console.log(`\n${COLORS.bold}══════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}       🚗 车载座舱 AI 娱乐系统 - 调试工具${COLORS.reset}`);
  console.log(`${COLORS.bold}══════════════════════════════════════════════════${COLORS.reset}`);

  console.log(`\n${COLORS.cyan}【当前设置】${COLORS.reset}`);
  console.log(`  调试阶段: ${COLORS.green}${LAYERS[currentLayer].name}${COLORS.reset}`);
  console.log(`  测试场景: ${COLORS.green}${SCENARIOS[currentScenario].name}${COLORS.reset}`);

  console.log(`\n${COLORS.cyan}【操作选项】${COLORS.reset}`);
  console.log(`  ${COLORS.yellow}1${COLORS.reset}. 选择调试阶段`);
  console.log(`  ${COLORS.yellow}2${COLORS.reset}. 选择测试场景`);
  console.log(`  ${COLORS.yellow}3${COLORS.reset}. 运行调试`);
  console.log(`  ${COLORS.yellow}q${COLORS.reset}. 退出`);

  console.log(`\n${COLORS.dim}提示: 直接输入 1-6 快速运行对应场景${COLORS.reset}`);
}

function showLayerMenu() {
  console.log(`\n${COLORS.cyan}【选择调试阶段】${COLORS.reset}`);
  for (const [key, layer] of Object.entries(LAYERS)) {
    const marker = currentLayer === layer.key ? `${COLORS.green}✓${COLORS.reset} ` : '  ';
    console.log(`  ${marker}${COLORS.yellow}${key}${COLORS.reset}. ${layer.name}`);
  }
  console.log(`  ${COLORS.yellow}0${COLORS.reset}. 返回`);
}

function showScenarioMenu() {
  console.log(`\n${COLORS.cyan}【选择测试场景】${COLORS.reset}`);
  for (const [key, scenario] of Object.entries(SCENARIOS)) {
    const marker = currentScenario === key ? `${COLORS.green}✓${COLORS.reset} ` : '  ';
    console.log(`  ${marker}${COLORS.yellow}${key}${COLORS.reset}. ${scenario.name}`);
  }
  console.log(`  ${COLORS.yellow}0${COLORS.reset}. 返回`);
}

async function handleInput(input) {
  if (input === 'q' || input === 'quit' || input === 'exit') {
    console.log(`\n${COLORS.green}再见！${COLORS.reset}\n`);
    rl.close();
    process.exit(0);
  }

  if (SCENARIOS[input] && currentLayer !== 'select_layer' && currentLayer !== 'select_scenario') {
    currentScenario = input;
    await runCurrentDebug();
    return;
  }

  if (currentLayer === 'select_layer') {
    if (input === '0') currentLayer = '4';
    else if (LAYERS[input]) currentLayer = input;
    return;
  }

  if (currentLayer === 'select_scenario') {
    if (input === '0') currentLayer = '4';
    else if (SCENARIOS[input]) currentScenario = input;
    return;
  }

  switch (input) {
    case '1': currentLayer = 'select_layer'; break;
    case '2': currentLayer = 'select_scenario'; break;
    case '3':
    case 'run': await runCurrentDebug(); break;
    default: console.log(`${COLORS.red}未知命令: ${input}${COLORS.reset}`);
  }
}

async function runCurrentDebug() {
  const scenario = SCENARIOS[currentScenario];
  const layerKey = LAYERS[currentLayer]?.key || 'full';
  if (layerKey === 'full') {
    await runFullChain(scenario);
  } else {
    await runSingleLayer(layerKey, scenario);
  }
}

async function main() {
  const args = process.argv.slice(2);
  if (args.includes('--run') || args.includes('-r')) {
    await runCurrentDebug();
    process.exit(0);
  }
  if (args.includes('--help') || args.includes('-h')) {
    console.log(`用法: node scripts/debug.js [选项]
选项:
  --run, -r    直接运行当前配置
  --help, -h   显示帮助`);
    process.exit(0);
  }

  rl = readline.createInterface({ input: process.stdin, output: process.stdout });

  const prompt = () => {
    if (currentLayer === 'select_layer') showLayerMenu();
    else if (currentLayer === 'select_scenario') showScenarioMenu();
    else showMainMenu();

    rl.question('\n请输入选项: ', async (input) => {
      await handleInput(input.trim());
      prompt();
    });
  };

  prompt();
}

main().catch(console.error);

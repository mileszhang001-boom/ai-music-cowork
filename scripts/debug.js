#!/usr/bin/env node
'use strict';

const readline = require('readline');

const { perceptionLayer, SignalSources } = require('../src/layers/perception');
const { semanticLayer } = require('../src/layers/semantic');
const { effectsLayer } = require('../src/layers/effects');
const { rulesEngine } = require('../src/core/rulesEngine');

const COLORS = {
  reset: '\x1b[0m', bold: '\x1b[1m', dim: '\x1b[2m',
  green: '\x1b[32m', yellow: '\x1b[33m', blue: '\x1b[34m',
  red: '\x1b[31m', cyan: '\x1b[36m', magenta: '\x1b[35m'
};

const SCENARIOS = {
  '1': { name: '早晨通勤', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 70 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.35 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } }
  ], context: { passengerComposition: ['adult'] }},
  '2': { name: '深夜驾驶', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 50 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.05 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } }
  ], context: { passengerComposition: ['adult'] }},
  '3': { name: '疲劳提醒', signals: [
    { source: SignalSources.BIOMETRIC, type: 'fatigue_level', value: 0.85 },
    { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 58 }
  ], context: { passengerComposition: ['adult'] }},
  '4': { name: '家庭出行(儿童)', signals: [
    { source: SignalSources.VHAL, type: 'passenger_count', value: { passenger_count: 3 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'sunny' } }
  ], context: { passengerComposition: ['adult', 'adult', 'child'] }},
  '5': { name: '雨夜驾驶', signals: [
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.1 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' } }
  ], context: { passengerComposition: ['adult'] }},
  '6': { name: '语音请求', signals: [
    { source: SignalSources.VOICE, type: 'user_query', value: { text: '来点嗨歌', intent: 'creative' } }
  ], context: { passengerComposition: ['adult'] }}
};

const LAYERS = {
  '1': 'Layer 1 (物理感知层)',
  '2': 'Layer 2 (语义推理层)',
  '3': 'Layer 3 (效果生成层)',
  '4': '全链路'
};

let rl;
let selectedLayer = '4';
let selectedScenario = '1';

function printBox(title, lines, color = COLORS.cyan) {
  const w = 58;
  const line = '─'.repeat(w - 2);
  console.log(`\n${color}┌${line}┐${COLORS.reset}`);
  console.log(`${color}│${COLORS.bold} ${title.padEnd(w - 2)}${COLORS.reset}${color}│${COLORS.reset}`);
  console.log(`${color}├${line}┤${COLORS.reset}`);
  lines.forEach(l => console.log(`${color}│${COLORS.reset} ${l.padEnd(w - 2)} ${color}│${COLORS.reset}`));
  console.log(`${color}└${line}┘${COLORS.reset}`);
}

async function runDebug() {
  const scenario = SCENARIOS[selectedScenario];
  console.log(`\n${COLORS.bold}${COLORS.cyan}══════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}  🚗 调试运行: ${scenario.name}${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}══════════════════════════════════════════════════${COLORS.reset}`);

  const t0 = Date.now();
  let result = {};

  if (selectedLayer === '1') {
    const output = perceptionLayer.processBatch(scenario.signals);
    result = { layer1: output, time: Date.now() - t0 };
    printBox('📥 输入信号', scenario.signals.map((s, i) => `${i + 1}. ${s.source}: ${JSON.stringify(s.value)}`), COLORS.blue);
    printBox('📤 输出 (StandardizedSignals)', [
      `置信度: ${(output.confidence.overall * 100).toFixed(0)}%`,
      `时间戳: ${output.timestamp}`,
      `活跃源: ${output._meta?.active_sources?.join(', ') || 'N/A'}`
    ], COLORS.green);
  } else if (selectedLayer === '2') {
    const l1 = perceptionLayer.processBatch(scenario.signals);
    const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
    const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
    result = { layer1: l1, layer2: l2, validation, time: Date.now() - t0 };
    printBox('📥 输入', [`置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`], COLORS.blue);
    printBox('📤 输出 (Scene Descriptor)', [
      `场景: ${l2.scene_descriptor.scene_type} (${l2.scene_descriptor.scene_name})`,
      `来源: ${l2.meta.source}`,
      `能量: ${l2.scene_descriptor.intent?.energy_level || 0}`,
      `校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
    ], COLORS.green);
  } else if (selectedLayer === '3') {
    const l1 = perceptionLayer.processBatch(scenario.signals);
    const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
    const l3 = await effectsLayer.process(l2.scene_descriptor);
    result = { layer1: l1, layer2: l2, layer3: l3, time: Date.now() - t0 };
    printBox('📥 输入', [`场景: ${l2.scene_descriptor.scene_type}`], COLORS.blue);
    printBox('📤 输出 (EffectCommands)', [
      `🎵 音乐: ${l3.commands?.content?.playlist?.length || 0} 首`,
      `💡 灯光: ${l3.commands?.lighting?.theme || 'default'}`,
      `🔊 音效: ${l3.commands?.audio?.preset || 'standard'}`,
      `状态: ${l3.execution_report?.status || 'completed'}`
    ], COLORS.green);
  } else {
    const l1 = perceptionLayer.processBatch(scenario.signals);
    const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
    const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
    const l3 = await effectsLayer.process(l2.scene_descriptor);
    result = { layer1: l1, layer2: l2, layer3: l3, validation, time: Date.now() - t0 };
    
    printBox('📥 输入', [
      `场景: ${scenario.name}`,
      `信号: ${scenario.signals.length} 个`,
      `乘客: ${scenario.context.passengerComposition?.join(', ') || '未知'}`
    ], COLORS.blue);
    
    printBox('⚙️ 处理过程', [
      `Layer 1 置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`,
      `Layer 2 场景: ${l2.scene_descriptor.scene_type}`,
      `规则校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`,
      `Layer 3 状态: ${l3.execution_report?.status || 'completed'}`
    ], COLORS.yellow);
    
    printBox('📤 输出', [
      `🎵 音乐: ${l3.commands?.content?.playlist?.length || 0} 首 - ${l3.commands?.content?.playlist?.[0]?.title || '无'}`,
      `💡 灯光: ${l3.commands?.lighting?.theme || 'default'} (亮度: ${l3.commands?.lighting?.intensity || 0})`,
      `🔊 音效: ${l3.commands?.audio?.preset || 'standard'}`,
      `📢 播报: "${l2.scene_descriptor.announcement?.text || '无'}"`
    ], COLORS.green);
  }

  printBox('📊 执行摘要', [
    `调试阶段: ${LAYERS[selectedLayer]}`,
    `总耗时: ${result.time}ms`,
    `场景ID: ${result.layer2?.scene_descriptor?.scene_id || result.layer1?._meta?.output_id || 'N/A'}`
  ], COLORS.magenta);

  return result;
}

function showMainMenu() {
  console.log(`\n${COLORS.bold}══════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}       🚗 车载座舱 AI 娱乐系统 - 调试工具${COLORS.reset}`);
  console.log(`${COLORS.bold}══════════════════════════════════════════════════${COLORS.reset}`);
  
  console.log(`\n${COLORS.cyan}【当前配置】${COLORS.reset}`);
  console.log(`  调试阶段: ${COLORS.green}${LAYERS[selectedLayer]}${COLORS.reset}`);
  console.log(`  测试场景: ${COLORS.green}${SCENARIOS[selectedScenario].name}${COLORS.reset}`);
  
  console.log(`\n${COLORS.cyan}【操作菜单】${COLORS.reset}`);
  console.log(`  ${COLORS.yellow}L${COLORS.reset} - 选择调试阶段`);
  console.log(`  ${COLORS.yellow}S${COLORS.reset} - 选择测试场景`);
  console.log(`  ${COLORS.yellow}R${COLORS.reset} - 运行调试`);
  console.log(`  ${COLORS.yellow}Q${COLORS.reset} - 退出`);
}

function showLayerMenu() {
  console.log(`\n${COLORS.cyan}【选择调试阶段】${COLORS.reset}`);
  for (const [k, v] of Object.entries(LAYERS)) {
    const mark = selectedLayer === k ? `${COLORS.green}✓${COLORS.reset} ` : '  ';
    console.log(`  ${mark}${COLORS.yellow}${k}${COLORS.reset}. ${v}`);
  }
  console.log(`  ${COLORS.dim}输入数字选择，按回车确认${COLORS.reset}`);
}

function showScenarioMenu() {
  console.log(`\n${COLORS.cyan}【选择测试场景】${COLORS.reset}`);
  for (const [k, v] of Object.entries(SCENARIOS)) {
    const mark = selectedScenario === k ? `${COLORS.green}✓${COLORS.reset} ` : '  ';
    console.log(`  ${mark}${COLORS.yellow}${k}${COLORS.reset}. ${v.name}`);
  }
  console.log(`  ${COLORS.dim}输入数字选择，按回车确认${COLORS.reset}`);
}

let mode = 'main';

async function main() {
  const args = process.argv.slice(2);
  if (args.includes('--run') || args.includes('-r')) {
    await runDebug();
    process.exit(0);
  }

  rl = readline.createInterface({ input: process.stdin, output: process.stdout });

  const prompt = () => {
    if (mode === 'layer') showLayerMenu();
    else if (mode === 'scenario') showScenarioMenu();
    else showMainMenu();

    rl.question('\n请输入: ', async (input) => {
      const cmd = input.trim().toUpperCase();

      if (mode === 'layer') {
        if (LAYERS[input.trim()]) selectedLayer = input.trim();
        mode = 'main';
      } else if (mode === 'scenario') {
        if (SCENARIOS[input.trim()]) selectedScenario = input.trim();
        mode = 'main';
      } else {
        if (cmd === 'L') mode = 'layer';
        else if (cmd === 'S') mode = 'scenario';
        else if (cmd === 'R') await runDebug();
        else if (cmd === 'Q') { console.log(`\n${COLORS.green}再见！${COLORS.reset}\n`); rl.close(); process.exit(0); }
        else console.log(`${COLORS.red}未知命令: ${input}${COLORS.reset}`);
      }
      prompt();
    });
  };

  prompt();
}

main().catch(console.error);

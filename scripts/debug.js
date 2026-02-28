#!/usr/bin/env node
'use strict';

const readline = require('readline');
const fs = require('fs');
const path = require('path');

const { perceptionLayer, SignalSources } = require('../src/layers/perception');
const { semanticLayer } = require('../src/layers/semantic');
const { effectsLayer } = require('../src/layers/effects');
const { rulesEngine } = require('../src/core/rulesEngine');

const COLORS = {
  reset: '\x1b[0m', bold: '\x1b[1m', dim: '\x1b[2m',
  green: '\x1b[32m', yellow: '\x1b[33m', blue: '\x1b[34m',
  red: '\x1b[31m', cyan: '\x1b[36m', magenta: '\x1b[35m'
};

const LAYERS = {
  '1': { name: 'Layer 1', desc: '物理感知层', input: '硬件输入信号' },
  '2': { name: 'Layer 2', desc: '语义推理层', input: 'StandardizedSignals' },
  '3': { name: 'Layer 3', desc: '效果生成层', input: 'Scene Descriptor' },
  '4': { name: '全链路', desc: 'Layer1→Layer2→Layer3', input: '硬件输入信号' }
};

const LAYER1_SCENARIOS = {
  '1': { name: '早晨通勤', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 70 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.35 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
    { source: SignalSources.ENVIRONMENT, type: 'date_type', value: { date_type: 'weekday' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#FFA500', secondary_color: '#87CEEB', brightness: 0.6, scene_description: 'city' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'neutral', confidence: 0.85, passengers: { children: 0, adults: 1, seniors: 0 } } },
    { source: SignalSources.INTERNAL_MIC, type: 'cabin_audio', value: { volume_level: 0.2, has_voice: false, voice_count: 0, noise_level: 0.1 } }
  ], context: { passengerComposition: ['adult'] }},
  '2': { name: '深夜驾驶', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 50 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.05 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#1E3A5F', secondary_color: '#2C5F7C', brightness: 0.2, scene_description: 'highway' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'calm', confidence: 0.8, passengers: { children: 0, adults: 1, seniors: 0 } } }
  ], context: { passengerComposition: ['adult'] }},
  '3': { name: '疲劳提醒', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 60 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.5 } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'tired', confidence: 0.9, passengers: { children: 0, adults: 1, seniors: 0 } } }
  ], context: { passengerComposition: ['adult'] }},
  '4': { name: '家庭出行', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 80 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.6 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'sunny' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'happy', confidence: 0.85, passengers: { children: 1, adults: 2, seniors: 0 } } }
  ], context: { passengerComposition: ['adult', 'adult', 'child'] }},
  '5': { name: '雨夜驾驶', signals: [
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.1 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'calm', confidence: 0.75, passengers: { children: 0, adults: 1, seniors: 0 } } }
  ], context: { passengerComposition: ['adult'] }},
  '6': { name: '语音请求', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 80 } },
    { source: SignalSources.VOICE, type: 'user_query', value: { text: '来点嗨歌', intent: 'creative' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'excited', confidence: 0.8, passengers: { children: 0, adults: 1, seniors: 0 } } }
  ], context: { passengerComposition: ['adult'] }}
};

let LAYER3_SCENARIOS = {};
let testData = null;

function loadTestData() {
  try {
    const dataPath = path.join(__dirname, '../tests/data/scene_templates_test.json');
    testData = JSON.parse(fs.readFileSync(dataPath, 'utf8'));
    
    let idx = 1;
    if (testData.presets) {
      testData.presets.forEach(preset => {
        LAYER3_SCENARIOS[String(idx)] = {
          name: `[预] ${preset.scene_descriptor.scene_name}`,
          scene_descriptor: preset.scene_descriptor,
          isPreset: true
        };
        idx++;
      });
    }
    
    if (testData.generated) {
      testData.generated.forEach(gen => {
        LAYER3_SCENARIOS[String(idx)] = {
          name: `[变] ${gen.scene_name}`,
          scene_descriptor: gen,
          isPreset: false
        };
        idx++;
      });
    }
    
    return true;
  } catch (e) {
    console.log(`${COLORS.yellow}警告: 无法加载测试数据: ${e.message}${COLORS.reset}`);
    return false;
  }
}

const state = {
  step: 1,
  selectedLayer: '4',
  selectedScenario: '1',
  pageOffset: 0
};

function clearScreen() {
  console.log('\x1b[2J\x1b[H');
}

function printBox(title, lines, color = COLORS.cyan) {
  const w = 70;
  const line = '─'.repeat(w - 2);
  console.log(`\n${color}┌${line}┐${COLORS.reset}`);
  console.log(`${color}│${COLORS.bold} ${title.padEnd(w - 2)}${COLORS.reset}${color}│${COLORS.reset}`);
  console.log(`${color}├${line}┤${COLORS.reset}`);
  lines.forEach(l => console.log(`${color}│${COLORS.reset} ${l.padEnd(w - 2)} ${color}│${COLORS.reset}`));
  console.log(`${color}└${line}┘${COLORS.reset}`);
}

function formatTime(timeOfDay) {
  if (timeOfDay < 0.25) return '深夜 (00:00-06:00)';
  if (timeOfDay < 0.4) return '早晨 (06:00-09:00)';
  if (timeOfDay < 0.6) return '上午 (09:00-14:00)';
  if (timeOfDay < 0.75) return '下午 (14:00-18:00)';
  if (timeOfDay < 0.9) return '傍晚 (18:00-21:00)';
  return '深夜 (21:00-24:00)';
}

function showStep1() {
  clearScreen();
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.1               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 1/3】选择调试阶段${COLORS.reset}\n`);
  
  for (const [k, v] of Object.entries(LAYERS)) {
    const mark = state.selectedLayer === k ? `${COLORS.green}●${COLORS.reset} ` : '○ ';
    console.log(`  ${mark}${COLORS.bold}${k}${COLORS.reset}. ${v.name} - ${v.desc}`);
    console.log(`      ${COLORS.dim}输入: ${v.input}${COLORS.reset}`);
  }
  
  console.log(`\n  ${COLORS.cyan}⬆️⬇️ 选择  Enter 确认  Esc 退出${COLORS.reset}`);
}

function showStep2() {
  clearScreen();
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.1               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name} - ${layer.desc}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 2/3】选择测试场景${COLORS.reset}\n`);
  
  let scenarios, totalScenarios, pageSize = 10;
  
  if (state.selectedLayer === '3') {
    scenarios = Object.entries(LAYER3_SCENARIOS);
    console.log(`${COLORS.cyan}  Scene Descriptor 场景 (共 ${scenarios.length} 个):${COLORS.reset}`);
  } else {
    scenarios = Object.entries(LAYER1_SCENARIOS);
    console.log(`${COLORS.cyan}  硬件输入场景 (共 ${scenarios.length} 个):${COLORS.reset}`);
  }
  
  totalScenarios = scenarios.length;
  const totalPages = Math.ceil(totalScenarios / pageSize);
  const currentPage = Math.floor(state.pageOffset / pageSize) + 1;
  
  const startIdx = state.pageOffset;
  const endIdx = Math.min(startIdx + pageSize, totalScenarios);
  const displayScenarios = scenarios.slice(startIdx, endIdx);
  
  displayScenarios.forEach(([k, v], idx) => {
    const actualIdx = startIdx + idx;
    const mark = state.selectedScenario === k ? `${COLORS.green}●${COLORS.reset} ` : '○ ';
    console.log(`  ${mark}${COLORS.bold}${String(actualIdx + 1).padStart(3)}${COLORS.reset}. ${v.name}`);
  });
  
  console.log(`\n  ${COLORS.dim}第 ${currentPage}/${totalPages} 页${COLORS.reset}`);
  console.log(`  ${COLORS.cyan}⬆️ 上页  ⬇️ 下页  Enter 确认  Esc 返回${COLORS.reset}`);
}

async function runAndShowResult() {
  clearScreen();
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.1               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name}${COLORS.reset}`);
  
  let scenarioName = '';
  if (state.selectedLayer === '3') {
    scenarioName = LAYER3_SCENARIOS[state.selectedScenario]?.name || '';
  } else {
    scenarioName = LAYER1_SCENARIOS[state.selectedScenario]?.name || '';
  }
  console.log(`${COLORS.bold}${COLORS.green}✓ 测试场景: ${scenarioName}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 3/3】生成效果${COLORS.reset}\n`);
  
  const t0 = Date.now();
  
  try {
    if (state.selectedLayer === '1') {
      const scenario = LAYER1_SCENARIOS[state.selectedScenario];
      const output = perceptionLayer.processBatch(scenario.signals);
      showLayer1Result(scenario, output, Date.now() - t0);
    } else if (state.selectedLayer === '2') {
      const scenario = LAYER1_SCENARIOS[state.selectedScenario];
      const l1 = perceptionLayer.processBatch(scenario.signals);
      const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
      const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
      showLayer2Result(l1, l2, validation, Date.now() - t0);
    } else if (state.selectedLayer === '3') {
      const scenario = LAYER3_SCENARIOS[state.selectedScenario];
      const l3 = await effectsLayer.process(scenario.scene_descriptor);
      showLayer3Result(scenario.scene_descriptor, l3, Date.now() - t0);
    } else {
      const scenario = LAYER1_SCENARIOS[state.selectedScenario];
      const l1 = perceptionLayer.processBatch(scenario.signals);
      const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
      const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
      const l3 = await effectsLayer.process(l2.scene_descriptor);
      showFullPipelineResult(l1, l2, l3, validation, Date.now() - t0);
    }
  } catch (e) {
    console.log(`${COLORS.red}执行错误: ${e.message}${COLORS.reset}`);
    console.log(e.stack);
  }
  
  console.log(`\n  ${COLORS.cyan}按任意键返回步骤1${COLORS.reset}`);
}

function showLayer1Result(scenario, output, time) {
  const activeSources = output._meta?.active_sources || [];
  const allSources = ['vhal', 'environment', 'external_camera', 'internal_camera', 'internal_mic', 'voice'];
  const sourceStatus = allSources.map(s => ({
    source: s,
    status: activeSources.includes(s) ? '✅ 活跃' : (output.signals[s] && Object.keys(output.signals[s]).length > 0 ? '⚠️ 有数据' : '○ 无')
  }));

  printBox('📊 信号源状态', sourceStatus.map(s => `${s.source.padEnd(16)}: ${s.status}`), COLORS.yellow);

  const extCam = output.signals?.external_camera || {};
  const intCam = output.signals?.internal_camera || {};

  printBox('📤 StandardizedSignals 输出', [
    `置信度: ${(output.confidence.overall * 100).toFixed(0)}%`,
    `时间: ${formatTime(output.signals?.environment?.time_of_day)}`,
    `天气: ${output.signals?.environment?.weather || '未知'}`,
    `车速: ${output.signals?.vehicle?.speed_kmh || 0} km/h`,
    `心情: ${intCam.mood || '未知'}`,
    `乘客: 儿童${intCam.passengers?.children || 0} 成人${intCam.passengers?.adults || 0}`
  ], COLORS.green);

  printBox('⏱️ 执行摘要', [`耗时: ${time}ms`], COLORS.cyan);
}

function showLayer2Result(l1, l2, validation, time) {
  const channel = l2.scene_descriptor.scene_type?.includes('fatigue') ? '🚨 紧急通道' : 
                 (l1.signals?.user_query ? '🐢 慢通道' : '⚡ 快通道');
  
  printBox('⚙️ 推理过程', [
    `通道: ${channel}`,
    `场景: ${l2.scene_descriptor.scene_type} (${l2.scene_descriptor.scene_name})`,
    `校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
  ], COLORS.yellow);

  printBox('📤 Scene Descriptor 输出', [
    `能量: ${l2.scene_descriptor.intent?.energy_level || 0}`,
    `情绪: valence=${l2.scene_descriptor.intent?.mood?.valence || 0.5}`,
    `播报: "${l2.scene_descriptor.announcement || '无'}"`
  ], COLORS.green);

  printBox('⏱️ 执行摘要', [`耗时: ${time}ms`], COLORS.cyan);
}

function showLayer3Result(sceneDescriptor, l3, time) {
  const content = l3.commands?.content;
  const lighting = l3.commands?.lighting;
  const audio = l3.commands?.audio;

  printBox('🎵 内容生成', [
    `播放列表: ${content?.playlist?.length || 0} 首`,
    `首曲: ${content?.playlist?.[0]?.title || '无'}`
  ], COLORS.green);

  printBox('💡 氛围灯', [
    `主题: ${lighting?.theme || 'default'}`,
    `亮度: ${((lighting?.intensity || 1) * 100).toFixed(0)}%`
  ], COLORS.yellow);

  printBox('🔊 音频', [
    `预设: ${audio?.preset || 'standard'}`,
    `音量: ${audio?.settings?.volume_db || 65} dB`
  ], COLORS.cyan);

  printBox('⏱️ 执行摘要', [`耗时: ${time}ms`], COLORS.cyan);
}

function showFullPipelineResult(l1, l2, l3, validation, time) {
  console.log(`${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}  Layer 1: 物理感知层${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);

  const intCam1 = l1.signals?.internal_camera || {};
  printBox('📤 Layer 1 输出', [
    `置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`,
    `时间: ${formatTime(l1.signals?.environment?.time_of_day)}`,
    `心情: ${intCam1.mood || '未知'}`
  ], COLORS.green);

  console.log(`\n${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}  Layer 2: 语义推理层${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);

  const channel = l2.scene_descriptor.scene_type?.includes('fatigue') ? '🚨 紧急通道' : 
                 (l1.signals?.user_query ? '🐢 慢通道' : '⚡ 快通道');

  printBox('⚙️ 推理过程', [
    `通道: ${channel}`,
    `场景: ${l2.scene_descriptor.scene_type}`,
    `校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
  ], COLORS.yellow);

  printBox('📤 Layer 2 输出', [
    `能量: ${l2.scene_descriptor.intent?.energy_level || 1}`,
    `播报: "${l2.scene_descriptor.announcement || '无'}"`
  ], COLORS.green);

  console.log(`\n${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}  Layer 3: 效果生成层${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);

  const content = l3.commands?.content;
  const lighting = l3.commands?.lighting;
  const audio = l3.commands?.audio;

  printBox('🎵 内容生成', [
    `播放列表: ${content?.playlist?.length || 0} 首`,
    `首曲: ${content?.playlist?.[0]?.title || '无'}`
  ], COLORS.green);

  printBox('💡 氛围灯', [
    `主题: ${lighting?.theme || 'default'}`,
    `亮度: ${((lighting?.intensity || 1) * 100).toFixed(0)}%`
  ], COLORS.yellow);

  printBox('🔊 音频', [
    `预设: ${audio?.preset || 'standard'}`,
    `音量: ${audio?.settings?.volume_db || 65} dB`
  ], COLORS.cyan);

  printBox('⏱️ 执行摘要', [`总耗时: ${time}ms`], COLORS.cyan);
}

async function main() {
  const args = process.argv.slice(2);
  
  if (!loadTestData()) {
    console.log(`${COLORS.red}无法加载测试数据${COLORS.reset}`);
  }
  
  if (args.includes('--run') || args.includes('-r')) {
    state.selectedLayer = args.find(a => LAYERS[a]) || '4';
    state.selectedScenario = '1';
    await runAndShowResult();
    process.exit(0);
  }

  process.stdin.setRawMode(true);
  process.stdin.resume();
  readline.emitKeypressEvents(process.stdin);

  let layerKeys = Object.keys(LAYERS);
  let layerIndex = layerKeys.indexOf(state.selectedLayer);
  
  let scenarioKeys = [];
  let scenarioIndex = 0;
  const pageSize = 10;

  const updateScenarioKeys = () => {
    if (state.selectedLayer === '3') {
      scenarioKeys = Object.keys(LAYER3_SCENARIOS);
    } else {
      scenarioKeys = Object.keys(LAYER1_SCENARIOS);
    }
    scenarioIndex = 0;
    state.selectedScenario = scenarioKeys[0] || '1';
    state.pageOffset = 0;
  };

  showStep1();

  process.stdin.on('keypress', async (str, key) => {
    if (key.name === 'escape' || (key.name === 'c' && key.ctrl)) {
      console.log(`\n${COLORS.green}再见！${COLORS.reset}`);
      process.stdin.setRawMode(false);
      process.exit(0);
      return;
    }

    if (state.step === 1) {
      if (key.name === 'up') {
        layerIndex = (layerIndex - 1 + layerKeys.length) % layerKeys.length;
        state.selectedLayer = layerKeys[layerIndex];
        showStep1();
      } else if (key.name === 'down') {
        layerIndex = (layerIndex + 1) % layerKeys.length;
        state.selectedLayer = layerKeys[layerIndex];
        showStep1();
      } else if (key.name === 'return') {
        updateScenarioKeys();
        state.step = 2;
        showStep2();
      }
    } else if (state.step === 2) {
      const totalPages = Math.ceil(scenarioKeys.length / pageSize);
      
      if (key.name === 'up') {
        if (scenarioIndex > 0) {
          scenarioIndex--;
          state.selectedScenario = scenarioKeys[scenarioIndex];
        } else if (state.pageOffset > 0) {
          state.pageOffset -= pageSize;
          scenarioIndex = pageSize - 1;
          state.selectedScenario = scenarioKeys[state.pageOffset + scenarioIndex];
        }
        showStep2();
      } else if (key.name === 'down') {
        if (scenarioIndex < Math.min(pageSize, scenarioKeys.length - state.pageOffset) - 1) {
          scenarioIndex++;
          state.selectedScenario = scenarioKeys[state.pageOffset + scenarioIndex];
        } else if (state.pageOffset + pageSize < scenarioKeys.length) {
          state.pageOffset += pageSize;
          scenarioIndex = 0;
          state.selectedScenario = scenarioKeys[state.pageOffset];
        }
        showStep2();
      } else if (key.name === 'return') {
        state.step = 3;
        await runAndShowResult();
        state.step = 1;
        state.pageOffset = 0;
        layerIndex = layerKeys.indexOf(state.selectedLayer);
        showStep1();
      } else if (key.name === 'escape' || key.name === 'b') {
        state.step = 1;
        state.pageOffset = 0;
        showStep1();
      }
    } else if (state.step === 3) {
      state.step = 1;
      showStep1();
    }
  });
}

main().catch(console.error);

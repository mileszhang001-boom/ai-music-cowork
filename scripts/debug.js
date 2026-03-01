#!/usr/bin/env node
'use strict';

require('dotenv').config();

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

const COLOR_NAMES = {
  '#FF0000': '红色', '#FF5722': '深橙', '#FF6B00': '活力橙', '#FF9800': '琥珀',
  '#FFD600': '明亮黄', '#FFEB3B': '黄色', '#FFFF00': '纯黄',
  '#00FF00': '纯绿', '#009688': '青绿', '#00BCD4': '青色', '#4CAF50': '绿色',
  '#0000FF': '纯蓝', '#2196F3': '蓝色', '#1A237E': '深蓝', '#0D1B2A': '深蓝黑',
  '#1B263B': '深蓝灰', '#3F51B5': '靛蓝', '#2C5F7C': '蓝灰',
  '#E91E63': '粉红', '#9C27B0': '紫色', '#4A148C': '深紫', '#FCE4EC': '浅粉',
  '#1E3A5F': '海军蓝', '#87CEEB': '天蓝', '#FFA500': '橙色'
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
  pageOffset: 0,
  jsonInputMode: false,
  jsonInputBuffer: '',
  customInput: null
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

function getColorName(hex) {
  if (!hex) return '未知';
  const upperHex = hex.toUpperCase();
  if (COLOR_NAMES && COLOR_NAMES[upperHex]) return COLOR_NAMES[upperHex];
  
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  
  if (r > 200 && g < 100 && b < 100) return '红色系';
  if (r > 200 && g > 100 && b < 100) return '橙色系';
  if (r > 200 && g > 200 && b < 100) return '黄色系';
  if (r < 100 && g > 150 && b < 100) return '绿色系';
  if (r < 100 && g > 150 && b > 150) return '青色系';
  if (r < 100 && g < 100 && b > 150) return '蓝色系';
  if (r > 150 && g < 100 && b > 150) return '紫色系';
  if (r > 150 && g > 100 && b > 150) return '粉色系';
  if (r < 50 && g < 50 && b < 50) return '黑色系';
  if (r < 100 && g < 100 && b < 100) return '深灰色';
  if (r > 200 && g > 200 && b > 200) return '白色系';
  
  return '混合色';
}

function hexToAnsi(hex) {
  if (!hex || !hex.startsWith('#')) return '\x1b[0m';
  
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  
  const ri = Math.round(r / 51);
  const gi = Math.round(g / 51);
  const bi = Math.round(b / 51);
  
  const ansiCode = 16 + (ri * 36) + (gi * 6) + bi;
  return `\x1b[38;5;${ansiCode}m`;
}

function parseLooseJson(str) {
  let jsonStr = str.trim();
  
  jsonStr = jsonStr.replace(/,\s*}/g, '}');
  jsonStr = jsonStr.replace(/,\s*]/g, ']');
  jsonStr = jsonStr.replace(/\/\/.*$/gm, '');
  jsonStr = jsonStr.replace(/\/\*[\s\S]*?\*\//g, '');
  
  return JSON.parse(jsonStr);
}

function showJsonInputMode() {
  clearScreen();
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.2               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.magenta}【自定义JSON输入模式】${COLORS.reset}`);
  console.log(`\n  ${COLORS.yellow}操作说明:${COLORS.reset}`);
  console.log(`    ${COLORS.cyan}Enter${COLORS.reset} - 换行`);
  console.log(`    ${COLORS.cyan}Tab${COLORS.reset}   - 确认执行`);
  console.log(`    ${COLORS.cyan}Esc${COLORS.reset}   - 取消返回`);
  console.log(`\n  ${COLORS.dim}支持宽松JSON格式（允许尾随逗号、注释）${COLORS.reset}`);
  console.log(`  ${COLORS.dim}直接粘贴您的JSON数据即可${COLORS.reset}\n`);
  
  if (state.jsonInputBuffer) {
    const lines = state.jsonInputBuffer.split('\n');
    lines.forEach(line => {
      console.log(`  ${COLORS.green}${line}${COLORS.reset}`);
    });
  } else {
    console.log(`  ${COLORS.cyan}等待输入... (可直接粘贴JSON)${COLORS.reset}`);
  }
}

function getJsonInputTemplate() {
  const layer = state.selectedLayer;
  
  if (layer === '1') {
    return `[
  { "source": "vhal", "type": "vehicle_speed", "value": { "speed_kmh": 60 } },
  { "source": "environment", "type": "time_of_day", "value": { "time_of_day": 0.5 } },
  { "source": "internal_camera", "type": "cabin_analysis", "value": { "mood": "neutral", "passengers": { "adults": 1 } } }
]`;
  } else if (layer === '2') {
    return `{
  "confidence": { "overall": 0.9 },
  "signals": {
    "environment": { "time_of_day": 0.5, "weather": "clear" },
    "internal_camera": { "mood": "neutral", "passengers": { "adults": 1 } }
  }
}`;
  } else if (layer === '3') {
    return `{
  "scene_type": "custom_scene",
  "scene_name": "自定义场景",
  "intent": {
    "mood": { "valence": 0.6, "arousal": 0.5 },
    "energy_level": 0.5
  },
  "hints": {
    "music": { "genres": ["pop"] },
    "lighting": { "color_theme": "warm" }
  }
}`;
  }
  
  return `{}`;
}

function showStep1() {
  clearScreen();
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.2               ║');
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
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.2               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name} - ${layer.desc}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 2/3】选择测试场景${COLORS.reset}\n`);
  
  let scenarios, totalScenarios, pageSize = 8;
  
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
  
  console.log(`\n  ${COLORS.magenta}● 0. 自定义JSON输入${COLORS.reset}`);
  
  console.log(`\n  ${COLORS.dim}第 ${currentPage}/${totalPages} 页${COLORS.reset}`);
  console.log(`  ${COLORS.cyan}⬆️ 上页  ⬇️ 下页  Enter 确认  0 自定义JSON  Esc 返回${COLORS.reset}`);
}

async function runAndShowResult() {
  clearScreen();
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.2               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name}${COLORS.reset}`);
  
  const isCustom = state.customInput !== null;
  let scenarioName = '';
  
  if (isCustom) {
    scenarioName = `${COLORS.magenta}自定义JSON输入${COLORS.reset}`;
  } else if (state.selectedLayer === '3') {
    scenarioName = LAYER3_SCENARIOS[state.selectedScenario]?.name || '';
  } else {
    scenarioName = LAYER1_SCENARIOS[state.selectedScenario]?.name || '';
  }
  console.log(`${COLORS.bold}${COLORS.green}✓ 测试场景: ${scenarioName}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 3/3】生成效果${COLORS.reset}\n`);
  
  const t0 = Date.now();
  
  try {
    if (state.selectedLayer === '1') {
      let signals, context;
      if (isCustom) {
        signals = Array.isArray(state.customInput) ? state.customInput : [state.customInput];
        context = { passengerComposition: ['adult'] };
      } else {
        const scenario = LAYER1_SCENARIOS[state.selectedScenario];
        signals = scenario.signals;
        context = scenario.context;
      }
      const output = perceptionLayer.processBatch(signals);
      showLayer1Result({ signals, context }, output, Date.now() - t0);
    } else if (state.selectedLayer === '2') {
      let l1Input, context;
      if (isCustom) {
        l1Input = state.customInput;
        context = { passengerComposition: ['adult'] };
      } else {
        const scenario = LAYER1_SCENARIOS[state.selectedScenario];
        l1Input = perceptionLayer.processBatch(scenario.signals);
        context = scenario.context;
      }
      const l2 = await semanticLayer.process(l1Input, { enableLLM: false, context });
      const validation = rulesEngine.validate(l2.scene_descriptor, context);
      showLayer2Result(l1Input, l2, validation, Date.now() - t0);
    } else if (state.selectedLayer === '3') {
      let sceneDescriptor;
      if (isCustom) {
        sceneDescriptor = state.customInput;
      } else {
        sceneDescriptor = LAYER3_SCENARIOS[state.selectedScenario].scene_descriptor;
      }
      const l3 = await effectsLayer.process(sceneDescriptor);
      showLayer3Result(sceneDescriptor, l3, Date.now() - t0);
    } else {
      let signals, context;
      if (isCustom) {
        signals = Array.isArray(state.customInput) ? state.customInput : [state.customInput];
        context = { passengerComposition: ['adult'] };
      } else {
        const scenario = LAYER1_SCENARIOS[state.selectedScenario];
        signals = scenario.signals;
        context = scenario.context;
      }
      const l1 = perceptionLayer.processBatch(signals);
      const l2 = await semanticLayer.process(l1, { enableLLM: false, context });
      const validation = rulesEngine.validate(l2.scene_descriptor, context);
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
  const allSources = [
    { key: 'vhal', name: '车辆信号(VHAL)' },
    { key: 'environment', name: '环境信号' },
    { key: 'external_camera', name: '车外摄像头' },
    { key: 'internal_camera', name: '车内摄像头' },
    { key: 'internal_mic', name: '车内麦克风' },
    { key: 'voice', name: '语音识别' }
  ];
  
  const sourceStatus = allSources.map(s => {
    const hasData = output.signals[s.key] && Object.keys(output.signals[s.key] || {}).length > 0;
    const isActive = activeSources.includes(s.key);
    let status, detail = '';
    
    if (isActive) {
      status = `${COLORS.green}✅ 跑通${COLORS.reset}`;
      detail = getSignalDetail(s.key, output.signals[s.key]);
    } else if (hasData) {
      status = `${COLORS.yellow}⚠️ 有数据${COLORS.reset}`;
      detail = getSignalDetail(s.key, output.signals[s.key]);
    } else {
      status = `${COLORS.dim}○ 空信号${COLORS.reset}`;
    }
    
    return { name: s.name, status, detail };
  });

  console.log(`\n${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}  📥 输入信号分析${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  sourceStatus.forEach(s => {
    console.log(`  ${s.name.padEnd(16)}: ${s.status}`);
    if (s.detail) console.log(`    ${COLORS.dim}${s.detail}${COLORS.reset}`);
  });

  console.log(`\n${COLORS.bold}${COLORS.green}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}  📤 StandardizedSignals 输出${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  console.log(`\n  ${COLORS.cyan}综合置信度: ${(output.confidence.overall * 100).toFixed(0)}%${COLORS.reset}`);
  
  console.log(`\n  ${COLORS.yellow}环境状态:${COLORS.reset}`);
  console.log(`    时间: ${formatTime(output.signals?.environment?.time_of_day)}`);
  console.log(`    日期类型: ${output.signals?.environment?.date_type || 'weekday'}`);
  console.log(`    天气: ${output.signals?.environment?.weather || '未知'}`);
  
  console.log(`\n  ${COLORS.yellow}车辆状态:${COLORS.reset}`);
  console.log(`    车速: ${output.signals?.vehicle?.speed_kmh || 0} km/h`);
  
  const extCam = output.signals?.external_camera || {};
  console.log(`\n  ${COLORS.yellow}车外环境:${COLORS.reset}`);
  console.log(`    场景描述: ${extCam.scene_description || '未知'}`);
  console.log(`    主色调: ${extCam.primary_color || '未知'}`);
  console.log(`    亮度: ${((extCam.brightness || 0) * 100).toFixed(0)}%`);
  
  const intCam = output.signals?.internal_camera || {};
  console.log(`\n  ${COLORS.yellow}车内状态:${COLORS.reset}`);
  console.log(`    驾驶员心情: ${intCam.mood || '未知'} (置信度: ${((intCam.confidence || 0) * 100).toFixed(0)}%)`);
  console.log(`    乘客分布: 儿童${intCam.passengers?.children || 0} 成人${intCam.passengers?.adults || 0} 老人${intCam.passengers?.seniors || 0}`);
  
  const intMic = output.signals?.internal_mic || {};
  console.log(`\n  ${COLORS.yellow}车内音频:${COLORS.reset}`);
  console.log(`    音量级别: ${((intMic.volume_level || 0) * 100).toFixed(0)}%`);
  console.log(`    检测到人声: ${intMic.has_voice ? '是' : '否'}`);
  
  const userQuery = output.signals?.user_query;
  if (userQuery) {
    console.log(`\n  ${COLORS.yellow}用户请求:${COLORS.reset}`);
    console.log(`    文本: "${userQuery.text || ''}"`);
    console.log(`    意图: ${userQuery.intent || '未知'}`);
  }

  console.log(`\n${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}  📄 输出JSON (精简)${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  const jsonOutput = {
    confidence: output.confidence,
    signals: {
      environment: output.signals?.environment,
      vehicle: output.signals?.vehicle,
      internal_camera: { mood: intCam.mood, passengers: intCam.passengers },
      external_camera: { scene_description: extCam.scene_description },
      user_query: userQuery
    }
  };
  console.log(`\n${COLORS.dim}${JSON.stringify(jsonOutput, null, 2)}${COLORS.reset}`);

  console.log(`\n${COLORS.bold}⏱️ 执行耗时: ${time}ms${COLORS.reset}`);
}

function getSignalDetail(key, data) {
  if (!data) return '';
  const keys = Object.keys(data);
  if (keys.length === 0) return '';
  return keys.slice(0, 3).join(', ') + (keys.length > 3 ? '...' : '');
}

function showLayer2Result(l1, l2, validation, time) {
  const isEmergency = l2.scene_descriptor.scene_type?.includes('fatigue');
  const hasUserQuery = l1.signals?.user_query;
  
  let channel, channelReason;
  if (isEmergency) {
    channel = `${COLORS.red}🚨 紧急通道${COLORS.reset}`;
    channelReason = '检测到疲劳驾驶等紧急情况';
  } else if (hasUserQuery) {
    channel = `${COLORS.magenta}🐢 慢通道 (LLM)${COLORS.reset}`;
    channelReason = `用户语音请求: "${hasUserQuery.text}"`;
  } else {
    channel = `${COLORS.green}⚡ 快通道 (模板匹配)${COLORS.reset}`;
    channelReason = '基于传感器数据自动匹配场景模板';
  }

  console.log(`\n${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}  ⚙️ 场景执行判断${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  console.log(`\n  执行通道: ${channel}`);
  console.log(`  判断依据: ${channelReason}`);
  console.log(`  匹配场景: ${COLORS.bold}${l2.scene_descriptor.scene_name}${COLORS.reset} (${l2.scene_descriptor.scene_type})`);
  console.log(`  数据来源: ${l2.meta?.source || 'template'}`);
  
  if (l2.meta?.matched_triggers) {
    console.log(`  触发条件: ${l2.meta.matched_triggers.join(', ')}`);
  }

  console.log(`\n${COLORS.bold}${COLORS.green}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}  ✅ 规则校验${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  console.log(`\n  校验结果: ${validation.passed ? `${COLORS.green}✅ 通过${COLORS.reset}` : `${COLORS.yellow}⚠️ 已自动修复${COLORS.reset}`}`);
  if (validation.warnings && validation.warnings.length > 0) {
    console.log(`  警告信息:`);
    validation.warnings.forEach(w => {
      console.log(`    - ${w.message}`);
    });
  }
  if (validation.fixes && validation.fixes.length > 0) {
    console.log(`  自动修复:`);
    validation.fixes.forEach(f => {
      console.log(`    - ${f.field}: ${f.from} → ${f.to}`);
    });
  }

  console.log(`\n${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}  📤 Scene Descriptor 输出${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  const intent = l2.scene_descriptor.intent || {};
  console.log(`\n  ${COLORS.yellow}场景意图:${COLORS.reset}`);
  console.log(`    能量级别: ${intent.energy_level || 0} / 1.0`);
  console.log(`    情绪效价(valence): ${intent.mood?.valence || 0.5} (积极←→消极)`);
  console.log(`    情绪唤醒(arousal): ${intent.mood?.arousal || 0.5} (平静←→兴奋)`);
  console.log(`    社交语境: ${intent.social_context || 'solo'}`);
  
  const hints = l2.scene_descriptor.hints || {};
  const musicGenres = Array.isArray(hints.music?.genres) ? hints.music.genres : [];
  const musicDecades = Array.isArray(hints.music?.decades) ? hints.music.decades : [];
  
  console.log(`\n  ${COLORS.yellow}内容提示:${COLORS.reset}`);
  console.log(`    音乐流派: ${musicGenres.join(', ') || '自动选择'}`);
  console.log(`    音乐年代: ${musicDecades.join(', ') || '不限'}`);
  console.log(`    灯光主题: ${hints.lighting?.color_theme || 'default'}`);

  console.log(`\n${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}  📢 用户回复${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  const announcement = l2.scene_descriptor.announcement;
  if (typeof announcement === 'string') {
    console.log(`\n  播报文本: "${announcement}"`);
  } else if (announcement) {
    console.log(`\n  播报文本: "${announcement.text || '无'}"`);
    console.log(`  语音风格: ${announcement.voice_style || 'default'}`);
  } else {
    console.log(`\n  播报文本: (无播报)`);
  }

  console.log(`\n${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}  📄 输出JSON (精简)${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
  const jsonOutput = {
    scene_id: l2.scene_descriptor.scene_id,
    scene_type: l2.scene_descriptor.scene_type,
    scene_name: l2.scene_descriptor.scene_name,
    intent: l2.scene_descriptor.intent,
    hints: l2.scene_descriptor.hints,
    announcement: l2.scene_descriptor.announcement
  };
  console.log(`\n${COLORS.dim}${JSON.stringify(jsonOutput, null, 2)}${COLORS.reset}`);

  console.log(`\n${COLORS.bold}⏱️ 执行耗时: ${time}ms${COLORS.reset}`);
}

function showLayer3Result(sceneDescriptor, l3, time) {
  const content = l3.commands?.content;
  const lighting = l3.commands?.lighting;
  const audio = l3.commands?.audio;
  const playlist = content?.playlist || [];

  console.log(`\n${COLORS.bold}${COLORS.green}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}  🎵 内容生成 - 播放列表 (${playlist.length}首)${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  if (playlist.length > 0) {
    console.log(``);
    playlist.forEach((track, i) => {
      const num = String(i + 1).padStart(2, ' ');
      const title = track.title || '未知';
      const artist = track.artist || '未知艺术家';
      const genre = track.genre || '未知流派';
      const energy = track.energy || 0;
      const bpm = track.bpm || '--';
      const duration = track.duration ? `${Math.floor(track.duration / 60)}:${String(track.duration % 60).padStart(2, '0')}` : '--:--';
      
      console.log(`  ${COLORS.bold}${num}.${COLORS.reset} ${COLORS.green}${title}${COLORS.reset} - ${COLORS.cyan}${artist}${COLORS.reset}`);
      console.log(`      ${COLORS.dim}流派: ${genre} | 能量: ${energy.toFixed(1)} | BPM: ${bpm} | 时长: ${duration}${COLORS.reset}`);
    });
    
    const totalDuration = playlist.reduce((sum, t) => sum + (t.duration || 0), 0);
    const avgEnergy = playlist.reduce((sum, t) => sum + (t.energy || 0), 0) / playlist.length;
    const allGenres = [...new Set(playlist.map(t => t.genre).filter(Boolean))];
    
    console.log(`\n  ${COLORS.yellow}📊 歌单统计:${COLORS.reset}`);
    console.log(`      总时长: ${Math.floor(totalDuration / 60)}分${totalDuration % 60}秒`);
    console.log(`      平均能量: ${avgEnergy.toFixed(2)}`);
    console.log(`      包含流派: ${allGenres.slice(0, 5).join(', ')}${allGenres.length > 5 ? '...' : ''}`);
  } else {
    console.log(`\n  ${COLORS.yellow}⚠️ 播放列表为空${COLORS.reset}`);
  }

  console.log(`\n${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}  💡 氛围灯效果${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  console.log(`\n  灯光主题: ${COLORS.bold}${lighting?.theme || 'default'}${COLORS.reset}`);
  
  // 显示色值
  if (lighting?.colors) {
    const primary = lighting.colors.primary || 'N/A';
    const secondary = lighting.colors.secondary || 'N/A';
    console.log(`  主色调: ${COLORS.red}●${COLORS.reset} ${primary}`);
    console.log(`  辅助色: ${COLORS.yellow}●${COLORS.reset} ${secondary}`);
  } else {
    console.log(`  主色调: 默认`);
  }
  
  console.log(`  亮度: ${((lighting?.intensity || 1) * 100).toFixed(0)}%`);
  console.log(`  动态模式: ${lighting?.pattern || 'steady'}`);
  console.log(`  过渡时间: ${lighting?.transition_ms || 1000}ms`);
  
  if (lighting?.sync_with_music) {
    console.log(`  音乐同步: ${COLORS.green}开启${COLORS.reset}`);
  }

  console.log(`\n${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}  🔊 音频效果${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  
  console.log(`\n  音效预设: ${COLORS.bold}${audio?.preset || 'standard'}${COLORS.reset}`);
  console.log(`  音量: ${audio?.settings?.volume_db || 65} dB`);
  
  const eq = audio?.settings?.eq;
  if (eq) {
    console.log(`  均衡器: 低音+${eq.bass || 0} 中音+${eq.mid || 0} 高音+${eq.treble || 0}`);
  }
  
  console.log(`  空间音频: ${audio?.settings?.spatial || 'off'}`);
  console.log(`  降噪: ${audio?.settings?.noise_cancellation ? '开启' : '关闭'}`);

  const experienceDesc = generateExperienceDescription(sceneDescriptor, content, lighting, audio);
  
  console.log(`\n${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}  🎯 场景体验描述${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`\n  ${COLORS.bold}${COLORS.green}${experienceDesc}${COLORS.reset}`);

  console.log(`\n${COLORS.bold}⏱️ 执行耗时: ${time}ms${COLORS.reset}`);
}

function generateExperienceDescription(sceneDescriptor, content, lighting, audio) {
  const sceneName = sceneDescriptor?.scene_name || '未知场景';
  const trackCount = content?.playlist?.length || 0;
  const firstTrack = content?.playlist?.[0];
  const theme = lighting?.theme || 'default';
  const brightness = ((lighting?.intensity || 1) * 100).toFixed(0);
  const preset = audio?.preset || 'standard';
  
  let desc = `${sceneName}：`;
  
  if (trackCount > 0) {
    desc += `播放${trackCount}首`;
    if (firstTrack?.genre) {
      desc += `${firstTrack.genre}风格的`;
    }
    desc += '音乐';
  }
  
  desc += `，${theme}主题灯光(${brightness}%亮度)`;
  
  if (preset !== 'standard') {
    desc += `，${preset}音效`;
  }
  
  const energy = sceneDescriptor?.intent?.energy_level || 0.5;
  if (energy > 0.7) {
    desc += '，营造活力四射的驾驶氛围';
  } else if (energy > 0.4) {
    desc += '，营造舒适惬意的驾驶氛围';
  } else {
    desc += '，营造宁静放松的驾驶氛围';
  }
  
  return desc;
}

function showFullPipelineResult(l1, l2, l3, validation, time) {
  console.log(`\n${COLORS.bold}${COLORS.blue}╔════════════════════════════════════════════════════════╗${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}║  Layer 1: 物理感知层                                    ║${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}╚════════════════════════════════════════════════════════╝${COLORS.reset}`);

  const activeSources = l1._meta?.active_sources || [];
  const allSources = ['vhal', 'environment', 'external_camera', 'internal_camera', 'internal_mic', 'voice'];
  
  console.log(`\n  ${COLORS.yellow}📊 信号源状态:${COLORS.reset}`);
  allSources.forEach(s => {
    const isActive = activeSources.includes(s);
    const hasData = l1.signals[s] && Object.keys(l1.signals[s] || {}).length > 0;
    const status = isActive ? `${COLORS.green}✅${COLORS.reset}` : (hasData ? `${COLORS.yellow}⚠️${COLORS.reset}` : `${COLORS.dim}○${COLORS.reset}`);
    console.log(`    ${status} ${s}`);
  });

  const intCam1 = l1.signals?.internal_camera || {};
  const extCam1 = l1.signals?.external_camera || {};
  
  console.log(`\n  ${COLORS.yellow}📤 关键输出:${COLORS.reset}`);
  console.log(`    置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`);
  console.log(`    时间: ${formatTime(l1.signals?.environment?.time_of_day)}`);
  console.log(`    心情: ${intCam1.mood || '未知'}`);
  console.log(`    环境: ${extCam1.scene_description || '未知'}`);

  console.log(`\n${COLORS.bold}${COLORS.yellow}╔════════════════════════════════════════════════════════╗${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}║  Layer 2: 语义推理层                                    ║${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}╚════════════════════════════════════════════════════════╝${COLORS.reset}`);

  const isEmergency = l2.scene_descriptor.scene_type?.includes('fatigue');
  const hasUserQuery = l1.signals?.user_query;
  
  let channel, channelColor;
  if (isEmergency) {
    channel = '🚨 紧急通道';
    channelColor = COLORS.red;
  } else if (hasUserQuery) {
    channel = '🐢 慢通道 (LLM)';
    channelColor = COLORS.magenta;
  } else {
    channel = '⚡ 快通道 (模板)';
    channelColor = COLORS.green;
  }

  console.log(`\n  ${COLORS.yellow}⚙️ 执行判断:${COLORS.reset}`);
  console.log(`    通道: ${channelColor}${channel}${COLORS.reset}`);
  console.log(`    场景: ${COLORS.bold}${l2.scene_descriptor.scene_name}${COLORS.reset} (${l2.scene_descriptor.scene_type})`);
  console.log(`    校验: ${validation.passed ? `${COLORS.green}✅ 通过${COLORS.reset}` : `${COLORS.yellow}⚠️ 已修复${COLORS.reset}`}`);

  console.log(`\n  ${COLORS.yellow}📤 关键输出:${COLORS.reset}`);
  console.log(`    能量: ${l2.scene_descriptor.intent?.energy_level || 0}`);
  const l2Genres = Array.isArray(l2.scene_descriptor.hints?.music?.genres) ? l2.scene_descriptor.hints.music.genres : [];
  console.log(`    流派: ${l2Genres.join(', ') || '自动'}`);
  
  const announcement = l2.scene_descriptor.announcement;
  console.log(`    播报: "${typeof announcement === 'string' ? announcement : (announcement?.text || '无')}"`);

  console.log(`\n${COLORS.bold}${COLORS.magenta}╔════════════════════════════════════════════════════════╗${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}║  Layer 3: 效果生成层                                    ║${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}╚════════════════════════════════════════════════════════╝${COLORS.reset}`);

  const content = l3.commands?.content;
  const lighting = l3.commands?.lighting;
  const audio = l3.commands?.audio;
  const playlist = content?.playlist || [];

  console.log(`\n  ${COLORS.yellow}🎵 播放列表 (${playlist.length}首):${COLORS.reset}`);
  if (playlist.length > 0) {
    playlist.forEach((track, i) => {
      const num = String(i + 1).padStart(2, ' ');
      const genre = track.genre || '未知流派';
      const energy = track.energy || 0;
      const bpm = track.bpm || '--';
      const duration = track.duration ? `${Math.floor(track.duration / 60)}:${String(track.duration % 60).padStart(2, '0')}` : '--:--';
      console.log(`    ${num}. ${COLORS.green}${track.title}${COLORS.reset} - ${COLORS.cyan}${track.artist}${COLORS.reset}`);
      console.log(`        ${COLORS.dim}流派: ${genre} | 能量: ${energy.toFixed(1)} | BPM: ${bpm} | 时长: ${duration}${COLORS.reset}`);
    });
    
    const totalDuration = playlist.reduce((sum, t) => sum + (t.duration || 0), 0);
    const avgEnergy = playlist.reduce((sum, t) => sum + (t.energy || 0), 0) / playlist.length;
    const allGenres = [...new Set(playlist.map(t => t.genre).filter(Boolean))];
    console.log(`\n    ${COLORS.yellow}📊 歌单统计:${COLORS.reset}`);
    console.log(`        总时长: ${Math.floor(totalDuration / 60)}分${totalDuration % 60}秒`);
    console.log(`        平均能量: ${avgEnergy.toFixed(2)}`);
    console.log(`        包含流派: ${allGenres.slice(0, 5).join(', ')}${allGenres.length > 5 ? '...' : ''}`);
  }

  console.log(`\n  ${COLORS.yellow}💡 灯光:${COLORS.reset}`);
  console.log(`    主题: ${lighting?.theme || 'default'} (${((lighting?.intensity || 1) * 100).toFixed(0)}%)`);
  if (lighting?.colors) {
    const primary = lighting.colors.primary || 'N/A';
    const secondary = lighting.colors.secondary || 'N/A';
    console.log(`    主色调: ${hexToAnsi(primary)}●${COLORS.reset} ${primary} (${getColorName(primary)})`);
    console.log(`    辅助色: ${hexToAnsi(secondary)}●${COLORS.reset} ${secondary} (${getColorName(secondary)})`);
  }

  console.log(`\n  ${COLORS.yellow}🔊 音频:${COLORS.reset}`);
  console.log(`    预设: ${audio?.preset || 'standard'} (${audio?.settings?.volume_db || 65}dB)`);

  const experienceDesc = generateExperienceDescription(l2.scene_descriptor, content, lighting, audio);
  
  console.log(`\n${COLORS.bold}${COLORS.green}╔════════════════════════════════════════════════════════╗${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}║  🎯 场景体验描述                                        ║${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.green}╚════════════════════════════════════════════════════════╝${COLORS.reset}`);
  console.log(`\n  ${COLORS.bold}${experienceDesc}${COLORS.reset}`);

  console.log(`\n${COLORS.bold}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}⏱️ 总执行耗时: ${time}ms${COLORS.reset}`);
  console.log(`${COLORS.bold}════════════════════════════════════════════════════════${COLORS.reset}`);
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
      if (state.jsonInputMode) {
        state.jsonInputMode = false;
        state.jsonInputBuffer = '';
        state.step = 2;
        showStep2();
        return;
      }
      console.log(`\n${COLORS.green}再见！${COLORS.reset}`);
      process.stdin.setRawMode(false);
      process.exit(0);
      return;
    }

    if (state.jsonInputMode) {
      if (key.name === 'tab') {
        try {
          const jsonStr = state.jsonInputBuffer.trim();
          if (jsonStr) {
            state.customInput = parseLooseJson(jsonStr);
            state.jsonInputMode = false;
            state.jsonInputBuffer = '';
            state.step = 3;
            await runAndShowResult();
          }
        } catch (e) {
          console.log(`\n${COLORS.red}JSON解析错误: ${e.message}${COLORS.reset}`);
          console.log(`${COLORS.yellow}请修正JSON或按 Esc 取消${COLORS.reset}`);
          process.stdout.write('\n继续输入: ');
        }
      } else if (key.name === 'return') {
        state.jsonInputBuffer += '\n';
        showJsonInputMode();
      } else if (key.name === 'backspace') {
        state.jsonInputBuffer = state.jsonInputBuffer.slice(0, -1);
        showJsonInputMode();
      } else if (str && str.length === 1) {
        state.jsonInputBuffer += str;
        showJsonInputMode();
      }
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
      } else if (str === '0') {
        state.jsonInputMode = true;
        state.jsonInputBuffer = '';
        showJsonInputMode();
      } else if (key.name === 'escape' || key.name === 'b') {
        state.step = 1;
        state.pageOffset = 0;
        showStep1();
      }
    } else if (state.step === 3) {
      state.step = 1;
      state.pageOffset = 0;
      state.customInput = null;
      layerIndex = layerKeys.indexOf(state.selectedLayer);
      showStep1();
    }
  });
}

main().catch(console.error);

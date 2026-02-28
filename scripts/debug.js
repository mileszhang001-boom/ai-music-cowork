#!/usr/bin/env node
'use strict';

const readline = require('readline');
const fs = require('fs');
const path = require('path');
const { stdin: rawInput, stdout: rawOutput } = require('process');

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
    { source: SignalSources.ENVIRONMENT, type: 'date_type', value: { date_type: 'weekday' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#1E3A5F', secondary_color: '#2C5F7C', brightness: 0.2, scene_description: 'highway' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'calm', confidence: 0.8, passengers: { children: 0, adults: 1, seniors: 0 } } },
    { source: SignalSources.INTERNAL_MIC, type: 'cabin_audio', value: { volume_level: 0.1, has_voice: false, voice_count: 0, noise_level: 0.05 } }
  ], context: { passengerComposition: ['adult'] }},
  '3': { name: '疲劳提醒', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 60 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.5 } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'tired', confidence: 0.9, passengers: { children: 0, adults: 1, seniors: 0 } } }
  ], context: { passengerComposition: ['adult'] }},
  '4': { name: '家庭出行(儿童)', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 80 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.6 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'sunny' } },
    { source: SignalSources.ENVIRONMENT, type: 'date_type', value: { date_type: 'weekend' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#87CEEB', secondary_color: '#FFFFFF', brightness: 0.8, scene_description: 'suburban' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'happy', confidence: 0.85, passengers: { children: 1, adults: 2, seniors: 0 } } },
    { source: SignalSources.INTERNAL_MIC, type: 'cabin_audio', value: { volume_level: 0.5, has_voice: true, voice_count: 3, noise_level: 0.3 } }
  ], context: { passengerComposition: ['adult', 'adult', 'child'] }},
  '5': { name: '雨夜驾驶', signals: [
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.1 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#4A5568', secondary_color: '#718096', brightness: 0.3, scene_description: 'city' } },
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
          name: `[预设] ${preset.scene_descriptor.scene_name}`,
          scene_descriptor: preset.scene_descriptor,
          input_signals: preset.input_signals,
          isPreset: true
        };
        idx++;
      });
    }
    
    if (testData.generated) {
      testData.generated.forEach(gen => {
        LAYER3_SCENARIOS[String(idx)] = {
          name: `[变体] ${gen.scene_name}`,
          scene_descriptor: gen,
          input_signals: gen.input_signals,
          isPreset: false
        };
        idx++;
      });
    }
    
    return true;
  } catch (e) {
    console.log(`${COLORS.yellow}警告: 无法加载测试数据文件: ${e.message}${COLORS.reset}`);
    return false;
  }
}

let rl;
let state = {
  step: 1,
  selectedLayer: '4',
  selectedScenario: null,
  pageOffset: 0
};

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

function clearScreen() {
  console.log('\x1b[2J\x1b[H');
}

function showHeader() {
  console.log(`${COLORS.bold}${COLORS.cyan}`);
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║          🚗 车载座舱 AI 娱乐系统 - 调试工具 v2.0               ║');
  console.log('╚════════════════════════════════════════════════════════════════╝');
  console.log(`${COLORS.reset}`);
}

function showStep1() {
  clearScreen();
  showHeader();
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 1/3】选择调试阶段${COLORS.reset}\n`);
  
  for (const [k, v] of Object.entries(LAYERS)) {
    const mark = state.selectedLayer === k ? `${COLORS.green}●${COLORS.reset} ` : '○ ';
    console.log(`  ${mark}${COLORS.bold}${k}${COLORS.reset}. ${v.name} - ${v.desc}`);
    console.log(`      ${COLORS.dim}输入: ${v.input}${COLORS.reset}`);
  }
  
  console.log(`\n  ${COLORS.dim}输入数字选择，按回车确认${COLORS.reset}`);
  console.log(`  ${COLORS.dim}输入 Q 退出${COLORS.reset}`);
}

function showStep2() {
  clearScreen();
  showHeader();
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name} - ${layer.desc}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 2/3】选择测试场景${COLORS.reset}\n`);
  
  let scenarios, totalScenarios, pageSize = 15;
  if (state.selectedLayer === '1' || state.selectedLayer === '4') {
    console.log(`${COLORS.cyan}  硬件输入场景:${COLORS.reset}`);
    scenarios = Object.entries(LAYER1_SCENARIOS);
    totalScenarios = scenarios.length;
  } else if (state.selectedLayer === '2') {
    console.log(`${COLORS.cyan}  StandardizedSignals 场景:${COLORS.reset}`);
    scenarios = Object.entries(LAYER1_SCENARIOS);
    totalScenarios = scenarios.length;
  } else if (state.selectedLayer === '3') {
    console.log(`${COLORS.cyan}  Scene Descriptor 场景 (共 ${Object.keys(LAYER3_SCENARIOS).length} 个):${COLORS.reset}`);
    scenarios = Object.entries(LAYER3_SCENARIOS);
    totalScenarios = scenarios.length;
  }
  
  const totalPages = Math.ceil(totalScenarios / pageSize);
  const currentPage = Math.floor(state.pageOffset / pageSize) + 1;
  
  const startIdx = state.pageOffset;
  const endIdx = Math.min(startIdx + pageSize, totalScenarios);
  const displayScenarios = scenarios.slice(startIdx, endIdx);
  
  displayScenarios.forEach(([k, v], idx) => {
    const actualIdx = startIdx + idx;
    const mark = state.selectedScenario === k ? `${COLORS.green}●${COLORS.reset} ` : '○ ';
    
    if (state.selectedLayer === '3') {
      const tag = v.isPreset ? `${COLORS.blue}[预]${COLORS.reset}` : `${COLORS.magenta}[变]${COLORS.reset}`;
      console.log(`  ${mark}${COLORS.bold}${String(actualIdx + 1).padStart(3)}${COLORS.reset}. ${tag} ${v.name}`);
    } else {
      console.log(`  ${mark}${COLORS.bold}${k}${COLORS.reset}. ${v.name}`);
    }
  });
  
  console.log(`\n  ${COLORS.dim}第 ${currentPage}/${totalPages} 页，共 ${totalScenarios} 个场景${COLORS.reset}`);
  console.log(`  ${COLORS.cyan}⬆️ 上页  ⬇️ 下页  ⬆️⬇️ 选择  回车确认  Esc 退出${COLORS.reset}`);
}

async function runAndShowResult() {
  clearScreen();
  showHeader();
  
  const layer = LAYERS[state.selectedLayer];
  console.log(`\n${COLORS.bold}${COLORS.green}✓ 调试阶段: ${layer.name} - ${layer.desc}${COLORS.reset}`);
  
  let scenarioName = '';
  if (state.selectedLayer === '3') {
    scenarioName = LAYER3_SCENARIOS[state.selectedScenario]?.name || '';
  } else {
    scenarioName = LAYER1_SCENARIOS[state.selectedScenario]?.name || '';
  }
  console.log(`${COLORS.bold}${COLORS.green}✓ 测试场景: ${scenarioName}${COLORS.reset}`);
  
  console.log(`\n${COLORS.bold}${COLORS.yellow}【步骤 3/3】生成效果${COLORS.reset}\n`);
  
  const t0 = Date.now();
  let result = {};
  
  try {
    if (state.selectedLayer === '1') {
      const scenario = LAYER1_SCENARIOS[state.selectedScenario];
      const output = perceptionLayer.processBatch(scenario.signals);
      result = { layer1: output, time: Date.now() - t0 };
      
      showLayer1Result(scenario, output, result.time);
      
    } else if (state.selectedLayer === '2') {
      const scenario = LAYER1_SCENARIOS[state.selectedScenario];
      const l1 = perceptionLayer.processBatch(scenario.signals);
      const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
      const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
      result = { layer1: l1, layer2: l2, validation, time: Date.now() - t0 };
      
      showLayer2Result(l1, l2, validation, result.time);
      
    } else if (state.selectedLayer === '3') {
      const scenario = LAYER3_SCENARIOS[state.selectedScenario];
      const sceneDescriptor = scenario.scene_descriptor;
      const l3 = await effectsLayer.process(sceneDescriptor);
      result = { sceneDescriptor, layer3: l3, time: Date.now() - t0 };
      
      showLayer3Result(sceneDescriptor, l3, result.time);
      
    } else {
      const scenario = LAYER1_SCENARIOS[state.selectedScenario];
      const l1 = perceptionLayer.processBatch(scenario.signals);
      const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
      const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
      const l3 = await effectsLayer.process(l2.scene_descriptor);
      result = { layer1: l1, layer2: l2, layer3: l3, validation, time: Date.now() - t0 };
      
      showFullPipelineResult(l1, l2, l3, validation, result.time);
    }
  } catch (e) {
    console.log(`${COLORS.red}执行错误: ${e.message}${COLORS.reset}`);
    console.log(e.stack);
  }
  
  console.log(`\n${COLORS.dim}按回车返回步骤1，输入 Q 退出${COLORS.reset}`);
}

function showLayer1Result(scenario, output, time) {
  const activeSources = output._meta?.active_sources || [];
  const allSources = ['vhal', 'environment', 'external_camera', 'internal_camera', 'internal_mic', 'voice'];
  const sourceStatus = allSources.map(s => ({
    source: s,
    status: activeSources.includes(s) ? '✅ 活跃' : (output.signals[s] && Object.keys(output.signals[s]).length > 0 ? '⚠️ 有数据' : '○ 无')
  }));

  printBox('📥 输入信号', scenario.signals.map((s, i) => 
    `${i + 1}. ${s.source}.${s.type}`
  ), COLORS.blue);

  printBox('📊 信号源状态', sourceStatus.map(s => 
    `${s.source.padEnd(16)}: ${s.status}`
  ), COLORS.yellow);

  const extCam = output.signals?.external_camera || {};
  const intCam = output.signals?.internal_camera || {};

  printBox('📤 StandardizedSignals 输出', [
    `置信度: ${(output.confidence.overall * 100).toFixed(0)}%`,
    `时间: ${formatTime(output.signals?.environment?.time_of_day)}`,
    `日期: ${output.signals?.environment?.date_type || 'weekday'}`,
    `天气: ${output.signals?.environment?.weather || '未知'}`,
    `车速: ${output.signals?.vehicle?.speed_kmh || 0} km/h`,
    `环境: ${extCam.scene_description || '未知'}`,
    `心情: ${intCam.mood || '未知'} (${((intCam.confidence || 0) * 100).toFixed(0)}%)`,
    `乘客: 儿童${intCam.passengers?.children || 0} 成人${intCam.passengers?.adults || 0} 老人${intCam.passengers?.seniors || 0}`
  ], COLORS.green);

  printBox('⏱️ 执行摘要', [
    `耗时: ${time}ms`,
    `输出ID: ${output._meta?.output_id || 'N/A'}`
  ], COLORS.cyan);
}

function showLayer2Result(l1, l2, validation, time) {
  printBox('📥 输入 (StandardizedSignals)', [
    `置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`,
    `心情: ${l1.signals?.internal_camera?.mood || '未知'}`,
    `乘客: 儿童${l1.signals?.internal_camera?.passengers?.children || 0} 成人${l1.signals?.internal_camera?.passengers?.adults || 0}`,
    `用户请求: ${l1.signals?.user_query?.text || '无'}`
  ], COLORS.blue);

  const channel = l2.scene_descriptor.scene_type?.includes('fatigue') ? '🚨 紧急通道' : 
                 (l1.signals?.user_query ? '🐢 慢通道 (LLM)' : '⚡ 快通道 (模板)');
  
  printBox('⚙️ 推理过程', [
    `通道: ${channel}`,
    `场景类型: ${l2.scene_descriptor.scene_type}`,
    `场景名称: ${l2.scene_descriptor.scene_name}`,
    `来源: ${l2.meta.source}`,
    `规则校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
  ], COLORS.yellow);

  printBox('📤 Scene Descriptor 输出', [
    `能量级别: ${l2.scene_descriptor.intent?.energy_level || 0}`,
    `情绪效价: ${l2.scene_descriptor.intent?.mood?.valence || 0.5}`,
    `情绪唤醒: ${l2.scene_descriptor.intent?.mood?.arousal || 0.5}`,
    `音乐流派: ${(l2.scene_descriptor.hints?.music?.genres || []).join(', ') || '无'}`,
    `灯光主题: ${l2.scene_descriptor.hints?.lighting?.color_theme || 'default'}`
  ], COLORS.green);

  printBox('📢 用户回复', [
    `播报文本: "${l2.scene_descriptor.announcement?.text || l2.scene_descriptor.announcement || '无'}"`,
    `语音风格: ${l2.scene_descriptor.announcement?.voice_style || 'default'}`
  ], COLORS.magenta);

  printBox('⏱️ 执行摘要', [
    `耗时: ${time}ms`,
    `场景ID: ${l2.scene_descriptor.scene_id || 'N/A'}`
  ], COLORS.cyan);
}

function showLayer3Result(sceneDescriptor, l3, time) {
  printBox('📥 输入 (Scene Descriptor)', [
    `场景: ${sceneDescriptor.scene_type}`,
    `名称: ${sceneDescriptor.scene_name}`,
    `能量: ${sceneDescriptor.intent?.energy_level || 1}`
  ], COLORS.blue);

  const content = l3.commands?.content;
  const lighting = l3.commands?.lighting;
  const audio = l3.commands?.audio;

  printBox('🎵 内容生成', [
    `播放列表: ${content?.playlist?.length || 0} 首`,
    `首曲: ${content?.playlist?.[0]?.title || '无'}`,
    `艺术家: ${content?.playlist?.[0]?.artist || '未知'}`,
    `流派: ${(content?.playlist?.[0]?.genres || []).join(', ') || '未知'}`,
    `能量: ${content?.playlist?.[0]?.energy || 0}`
  ], COLORS.green);

  printBox('💡 氛围灯效果', [
    `主题: ${lighting?.theme || 'default'}`,
    `颜色: ${(lighting?.colors || []).join(', ') || '默认'}`,
    `模式: ${lighting?.pattern || 'steady'}`,
    `亮度: ${((lighting?.intensity || 0) * 100).toFixed(0)}%`,
    `过渡: ${lighting?.transition_ms || 1000}ms`
  ], COLORS.yellow);

  printBox('🔊 音频效果', [
    `预设: ${audio?.preset || 'standard'}`,
    `均衡器: ${JSON.stringify(audio?.settings?.eq || '默认')}`,
    `空间音频: ${audio?.settings?.spatial || '默认'}`,
    `音量: ${audio?.settings?.volume_db || 65} dB`
  ], COLORS.cyan);

  const experienceDesc = `场景体验: ${sceneDescriptor.scene_name}，音乐${content?.playlist?.length || 0}首，` +
    `灯光${lighting?.theme}主题${((lighting?.intensity || 1) * 100).toFixed(0)}%亮度，` +
    `音效${audio?.preset}预设`;

  printBox('🎯 场景体验描述', [experienceDesc], COLORS.magenta);

  printBox('⏱️ 执行摘要', [
    `耗时: ${time}ms`,
    `场景ID: ${sceneDescriptor.scene_id || 'N/A'}`
  ], COLORS.cyan);
}

function showFullPipelineResult(l1, l2, l3, validation, time) {
  console.log(`\n${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}  Layer 1: 物理感知层${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);

  const activeSources = l1._meta?.active_sources || [];
  const allSources = ['vhal', 'environment', 'external_camera', 'internal_camera', 'internal_mic', 'voice'];
  const sourceStatus = allSources.map(s => ({
    source: s,
    status: activeSources.includes(s) ? '✅ 活跃' : (l1.signals[s] && Object.keys(l1.signals[s] || {}).length > 0 ? '⚠️ 有数据' : '○ 无')
  }));

  printBox('📊 信号源状态', sourceStatus.map(s => 
    `${s.source.padEnd(16)}: ${s.status}`
  ), COLORS.yellow);

  const intCam1 = l1.signals?.internal_camera || {};
  const extCam1 = l1.signals?.external_camera || {};
  printBox('📤 Layer 1 输出', [
    `置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`,
    `时间: ${formatTime(l1.signals?.environment?.time_of_day)}`,
    `日期: ${l1.signals?.environment?.date_type || 'weekday'}`,
    `天气: ${l1.signals?.environment?.weather || '未知'}`,
    `车速: ${l1.signals?.vehicle?.speed_kmh || 0} km/h`,
    `环境: ${extCam1.scene_description || '未知'}`,
    `心情: ${intCam1.mood || '未知'}`,
    `乘客: 儿童${intCam1.passengers?.children || 0} 成人${intCam1.passengers?.adults || 0}`
  ], COLORS.green);

  console.log(`\n${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}  Layer 2: 语义推理层${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);

  const channel = l2.scene_descriptor.scene_type?.includes('fatigue') ? '🚨 紧急通道' : 
                 (l1.signals?.user_query ? '🐢 慢通道 (LLM)' : '⚡ 快通道 (模板)');

  printBox('⚙️ 推理过程', [
    `通道: ${channel}`,
    `场景: ${l2.scene_descriptor.scene_type} (${l2.scene_descriptor.scene_name})`,
    `来源: ${l2.meta.source}`,
    `校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
  ], COLORS.yellow);

  printBox('📤 Layer 2 输出', [
    `能量: ${l2.scene_descriptor.intent?.energy_level || 1}`,
    `情绪: valence=${l2.scene_descriptor.intent?.mood?.valence || 0.5}, arousal=${l2.scene_descriptor.intent?.mood?.arousal || 0.5}`,
    `播报: "${l2.scene_descriptor.announcement?.text || l2.scene_descriptor.announcement || '无'}"`
  ], COLORS.green);

  console.log(`\n${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}  Layer 3: 效果生成层${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.magenta}════════════════════════════════════════════════════════${COLORS.reset}`);

  const content = l3.commands?.content;
  const lighting = l3.commands?.lighting;
  const audio = l3.commands?.audio;

  printBox('🎵 内容生成', [
    `播放列表: ${content?.playlist?.length || 0} 首`,
    `首曲: ${content?.playlist?.[0]?.title || '无'} - ${content?.playlist?.[0]?.artist || '未知'}`
  ], COLORS.green);

  printBox('💡 氛围灯效果', [
    `主题: ${lighting?.theme || 'default'}`,
    `亮度: ${((lighting?.intensity || 1) * 100).toFixed(0)}%`,
    `模式: ${lighting?.pattern || 'steady'}`
  ], COLORS.yellow);

  printBox('🔊 音频效果', [
    `预设: ${audio?.preset || 'standard'}`,
    `音量: ${audio?.settings?.volume_db || 65} dB`
  ], COLORS.cyan);

  const experienceDesc = `场景体验: ${l2.scene_descriptor.scene_name}，音乐${content?.playlist?.length || 0}首，` +
    `灯光${lighting?.theme}主题${((lighting?.intensity || 1) * 100).toFixed(0)}%亮度，音效${audio?.preset}预设`;

  printBox('🎯 场景体验描述', [experienceDesc], COLORS.magenta);

  printBox('⏱️ 执行摘要', [
    `总耗时: ${time}ms`,
    `场景ID: ${l2.scene_descriptor.scene_id || 'N/A'}`
  ], COLORS.cyan);
}

async function main() {
  const args = process.argv.slice(2);
  
  if (!loadTestData()) {
    console.log(`${COLORS.red}无法加载测试数据，部分功能不可用${COLORS.reset}`);
  }
  
  if (args.includes('--run') || args.includes('-r')) {
    const layerArg = args.find(a => LAYERS[a]);
    state.selectedLayer = layerArg || '4';
    state.selectedScenario = state.selectedLayer === '3' ? '1' : '1';
    await runAndShowResult();
    process.exit(0);
  }

  rawInput.setRawMode(true);
  rawOutput.write('\x1b[?25l\x1b[?1c\x1b[?1049h');
  
  rl = readline.createInterface({ 
    input: rawInput, 
    output: rawOutput,
    terminal: true,
    historySize: 0
  });

  const prompt = () => {
    if (state.step === 1) {
      showStep1();
    } else if (state.step === 2) {
      showStep2();
    } else if (state.step === 3) {
      return;
    }
  };

  prompt();
  
  const handleKeyPress = (str, key) => {
    if (key.name === 'escape') {
      console.log(`${COLORS.green}再见！${COLORS.reset}\n`);
      rl.close();
      process.exit(0);
      return;
    }
    
    if (key.name === 'return') {
      if (state.step === 2) {
        state.step = 1;
        prompt();
      } else if (state.step === 3) {
        state.step = 1;
        prompt();
      }
      return;
    }
    
    if (state.step === 2) {
      const pageSize = 15;
      let totalScenarios;
      if (state.selectedLayer === '1' || state.selectedLayer === '4') {
        totalScenarios = Object.keys(LAYER1_SCENARIOS).length;
      } else if (state.selectedLayer === '2') {
        totalScenarios = Object.keys(LAYER1_SCENARIOS).length;
      } else if (state.selectedLayer === '3') {
        totalScenarios = Object.keys(LAYER3_SCENARIOS).length;
      }
      
      const totalPages = Math.ceil(totalScenarios / pageSize);
      
      if (key.name === 'up') {
        state.pageOffset = Math.max(0, state.pageOffset - pageSize);
        prompt();
      } else if (key.name === 'down') {
        state.pageOffset = Math.min((totalPages - 1) * pageSize, state.pageOffset + pageSize);
        prompt();
      }
    }
  };

  rawInput.on('keypress', handleKeyPress);
  
  rl.question('\n', async (input) => {
    const cmd = input.trim().toUpperCase();

    if (cmd === 'Q') {
      console.log(`\n${COLORS.green}再见！${COLORS.reset}\n`);
      rl.close();
      process.exit(0);
    }

    if (state.step === 1) {
      if (LAYERS[input.trim()]) {
        state.selectedLayer = input.trim();
        state.step = 2;
        state.selectedScenario = state.selectedLayer === '3' ? '1' : '1';
        state.pageOffset = 0;
      }
    } else if (state.step === 2) {
      if (cmd === 'B') {
        state.step = 1;
        state.pageOffset = 0;
      } else if (state.selectedLayer === '3' && LAYER3_SCENARIOS[input.trim()]) {
        state.selectedScenario = input.trim();
        state.step = 3;
        await runAndShowResult();
        state.step = 1;
      } else if ((state.selectedLayer === '1' || state.selectedLayer === '2' || state.selectedLayer === '4') && LAYER1_SCENARIOS[input.trim()]) {
        state.selectedScenario = input.trim();
        state.step = 3;
        await runAndShowResult();
        state.step = 1;
      }
    }

    prompt();
  });

  rl.on('close', () => {
    rawInput.setRawMode(false);
    rawInput.write('\x1b[?1049h\x1b[?25l');
  });
}

main().catch(console.error);

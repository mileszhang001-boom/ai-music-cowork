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
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
    { source: SignalSources.ENVIRONMENT, type: 'date_type', value: { date_type: 'weekday' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#FFA500', secondary_color: '#87CEEB', brightness: 0.6 } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'neutral', confidence: 0.85, passengers: { children: 0, adults: 1, seniors: 0 } } },
    { source: SignalSources.INTERNAL_MIC, type: 'cabin_audio', value: { volume_level: 0.2, has_voice: false, voice_count: 0, noise_level: 0.1 } }
  ], context: { passengerComposition: ['adult'] }},
  '2': { name: '深夜驾驶', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 50 } },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.05 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' } },
    { source: SignalSources.ENVIRONMENT, type: 'date_type', value: { date_type: 'weekday' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#1E3A5F', secondary_color: '#2C5F7C', brightness: 0.2 } },
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
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#87CEEB', secondary_color: '#FFFFFF', brightness: 0.8 } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'happy', confidence: 0.85, passengers: { children: 1, adults: 2, seniors: 0 } } },
    { source: SignalSources.INTERNAL_MIC, type: 'cabin_audio', value: { volume_level: 0.5, has_voice: true, voice_count: 3, noise_level: 0.3 } }
  ], context: { passengerComposition: ['adult', 'adult', 'child'] }},
  '5': { name: '雨夜驾驶', signals: [
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.1 } },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' } },
    { source: SignalSources.EXTERNAL_CAMERA, type: 'environment_colors', value: { primary_color: '#4A5568', secondary_color: '#718096', brightness: 0.3 } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'calm', confidence: 0.75, passengers: { children: 0, adults: 1, seniors: 0 } } }
  ], context: { passengerComposition: ['adult'] }},
  '6': { name: '语音请求', signals: [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { speed_kmh: 80 } },
    { source: SignalSources.VOICE, type: 'user_query', value: { text: '来点嗨歌', intent: 'creative' } },
    { source: SignalSources.INTERNAL_CAMERA, type: 'cabin_analysis', value: { mood: 'excited', confidence: 0.8, passengers: { children: 0, adults: 1, seniors: 0 } } }
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

async function runDebug() {
  const scenario = SCENARIOS[selectedScenario];
  console.log(`\n${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}  🚗 调试运行: ${scenario.name}${COLORS.reset}`);
  console.log(`${COLORS.bold}${COLORS.cyan}════════════════════════════════════════════════════════${COLORS.reset}`);

  const t0 = Date.now();
  let result = {};

  if (selectedLayer === '1') {
    const output = perceptionLayer.processBatch(scenario.signals);
    result = { layer1: output, time: Date.now() - t0 };
    
    const activeSources = output._meta?.active_sources || [];
    const allSources = ['vhal', 'environment', 'external_camera', 'internal_camera', 'internal_mic', 'voice', 'user_profile', 'music_state'];
    const sourceStatus = allSources.map(s => ({
      source: s,
      status: activeSources.includes(s) ? '✅ 跑通' : (output.signals[s] && Object.keys(output.signals[s]).length > 0 ? '⚠️ 有数据' : '❌ 空')
    }));

    printBox('📥 输入信号', scenario.signals.map((s, i) => 
      `${i + 1}. ${s.source}.${s.type}: ${JSON.stringify(s.value)}`
    ), COLORS.blue);

    printBox('📊 设备信号状态', sourceStatus.map(s => 
      `${s.source}: ${s.status}`
    ), COLORS.yellow);

    const extCam = output.signals?.external_camera || {};
    const intCam = output.signals?.internal_camera || {};
    const intMic = output.signals?.internal_mic || {};

    printBox('📤 输出 (StandardizedSignals)', [
      `置信度: ${(output.confidence.overall * 100).toFixed(0)}%`,
      `时间: ${formatTime(output.signals?.environment?.time_of_day)}`,
      `日期: ${output.signals?.environment?.date_type || 'weekday'}`,
      `天气: ${output.signals?.environment?.weather || '未知'}`,
      `车速: ${output.signals?.vehicle?.speed_kmh || 0} km/h`,
      `心情: ${intCam.mood || '未知'} (${((intCam.confidence || 0) * 100).toFixed(0)}%)`,
      `乘客: 儿童${intCam.passengers?.children || 0} 成人${intCam.passengers?.adults || 0} 老人${intCam.passengers?.seniors || 0}`
    ], COLORS.green);

    console.log(`\n${COLORS.dim}--- JSON 数据 ---${COLORS.reset}`);
    console.log(JSON.stringify(output.signals, null, 2));

  } else if (selectedLayer === '2') {
    const l1 = perceptionLayer.processBatch(scenario.signals);
    const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
    const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
    result = { layer1: l1, layer2: l2, validation, time: Date.now() - t0 };

    printBox('📥 输入 (Layer 1 输出)', [
      `置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`,
      `心情: ${l1.signals?.internal_camera?.mood || '未知'}`,
      `乘客: 儿童${l1.signals?.internal_camera?.passengers?.children || 0} 成人${l1.signals?.internal_camera?.passengers?.adults || 0}`,
      `用户请求: ${l1.signals?.user_query?.text || '无'}`
    ], COLORS.blue);

    const channel = l2.scene_descriptor.scene_type?.includes('fatigue') ? '紧急通道' : 
                   (l1.signals?.user_query ? '慢通道 (LLM)' : '快通道 (模板)');
    
    printBox('⚙️ 执行判断', [
      `通道: ${channel}`,
      `场景类型: ${l2.scene_descriptor.scene_type}`,
      `场景名称: ${l2.scene_descriptor.scene_name}`,
      `来源: ${l2.meta.source}`,
      `规则校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
    ], COLORS.yellow);

    printBox('📤 输出 (Scene Descriptor)', [
      `能量级别: ${l2.scene_descriptor.intent?.energy_level || 0}`,
      `情绪效价: ${l2.scene_descriptor.intent?.mood?.valence || 0.5}`,
      `情绪唤醒: ${l2.scene_descriptor.intent?.mood?.arousal || 0.5}`,
      `音乐流派: ${(l2.scene_descriptor.hints?.music?.genres || []).join(', ') || '无'}`,
      `灯光主题: ${l2.scene_descriptor.hints?.lighting?.color_theme || 'default'}`
    ], COLORS.green);

    printBox('📢 用户回复', [
      `播报文本: "${l2.scene_descriptor.announcement?.text || '无'}"`,
      `语音风格: ${l2.scene_descriptor.announcement?.voice_style || 'default'}`
    ], COLORS.magenta);

    console.log(`\n${COLORS.dim}--- JSON 数据 ---${COLORS.reset}`);
    console.log(JSON.stringify(l2.scene_descriptor, null, 2));

  } else if (selectedLayer === '3') {
    const l1 = perceptionLayer.processBatch(scenario.signals);
    const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
    const l3 = await effectsLayer.process(l2.scene_descriptor);
    result = { layer1: l1, layer2: l2, layer3: l3, time: Date.now() - t0 };

    printBox('📥 输入 (Scene Descriptor)', [
      `场景: ${l2.scene_descriptor.scene_type}`,
      `能量: ${l2.scene_descriptor.intent?.energy_level || 1}`
    ], COLORS.blue);

    const content = l3.commands?.content;
    const lighting = l3.commands?.lighting
    const audio = l3.commands?.audio

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

    const experienceDesc = `场景体验: ${l2.scene_descriptor.scene_name}，音乐${content?.playlist?.length || 0}首，` +
      `灯光${lighting?.theme}主题${((lighting?.intensity || 1) * 100).toFixed(0)}%亮度，` +
      `音效${audio?.preset}预设`;

    printBox('🎯 场景体验描述', [experienceDesc], COLORS.magenta);

    console.log(`\n${COLORS.dim}--- JSON 数据 ---${COLORS.reset}`);
    console.log(JSON.stringify(l3.commands, null, 2));

  } else {
    const l1 = perceptionLayer.processBatch(scenario.signals);
    const l2 = await semanticLayer.process(l1, { enableLLM: false, context: scenario.context });
    const validation = rulesEngine.validate(l2.scene_descriptor, scenario.context);
    const l3 = await effectsLayer.process(l2.scene_descriptor);
    result = { layer1: l1, layer2: l2, layer3: l3, validation, time: Date.now() - t0 };

    console.log(`\n${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);
    console.log(`${COLORS.bold}${COLORS.blue}  Layer 1: 物理感知层${COLORS.reset}`);
    console.log(`${COLORS.bold}${COLORS.blue}════════════════════════════════════════════════════════${COLORS.reset}`);

    const activeSources = l1._meta?.active_sources || [];
    const allSources = ['vhal', 'environment', 'external_camera', 'internal_camera', 'internal_mic', 'voice', 'user_profile', 'music_state'];
    const sourceStatus = allSources.map(s => ({
      source: s,
      status: activeSources.includes(s) ? '✅ 跑通' : (l1.signals[s] && Object.keys(l1.signals[s] || {}).length > 0 ? '⚠️ 有数据' : '❌ 空')
    }));

    printBox('📊 设备信号状态', sourceStatus.map(s => 
      `${s.source}: ${s.status}`
    ), COLORS.yellow);

    const intCam1 = l1.signals?.internal_camera || {};
    printBox('📤 Layer 1 输出', [
      `置信度: ${(l1.confidence.overall * 100).toFixed(0)}%`,
      `时间: ${formatTime(l1.signals?.environment?.time_of_day)}`,
      `日期: ${l1.signals?.environment?.date_type || 'weekday'}`,
      `天气: ${l1.signals?.environment?.weather || '未知'}`,
      `车速: ${l1.signals?.vehicle?.speed_kmh || 0} km/h`,
      `心情: ${intCam1.mood || '未知'}`,
      `乘客: 儿童${intCam1.passengers?.children || 0} 成人${intCam1.passengers?.adults || 0}`
    ], COLORS.green);

    console.log(`\n${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);
    console.log(`${COLORS.bold}${COLORS.yellow}  Layer 2: 语义推理层${COLORS.reset}`);
    console.log(`${COLORS.bold}${COLORS.yellow}════════════════════════════════════════════════════════${COLORS.reset}`);

    const channel = l2.scene_descriptor.scene_type?.includes('fatigue') ? '紧急通道' : 
                   (l1.signals?.user_query ? '慢通道 (LLM)' : '快通道 (模板)');

    printBox('⚙️ 执行判断', [
      `通道: ${channel}`,
      `场景: ${l2.scene_descriptor.scene_type} (${l2.scene_descriptor.scene_name})`,
      `来源: ${l2.meta.source}`,
      `校验: ${validation.passed ? '✅ 通过' : '⚠️ 已修复'}`
    ], COLORS.yellow);

    printBox('📤 Layer 2 输出', [
      `能量: ${l2.scene_descriptor.intent?.energy_level || 1}`,
      `情绪: valence=${l2.scene_descriptor.intent?.mood?.valence || 0.5}, arousal=${l2.scene_descriptor.intent?.mood?.arousal || 0.5}`,
      `播报: "${l2.scene_descriptor.announcement?.text || '无'}"`
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
  }

  printBox('📊 执行摘要', [
    `调试阶段: ${LAYERS[selectedLayer]}`,
    `总耗时: ${result.time}ms`,
    `场景ID: ${result.layer2?.scene_descriptor?.scene_id || result.layer1?._meta?.output_id || 'N/A'}`
  ], COLORS.cyan);

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

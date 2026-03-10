#!/usr/bin/env node
'use strict';

const { layer1, SignalSources } = require('../src/core/layer1');
const { layer2, SceneTypes } = require('../src/core/layer2');
const { templateLibrary } = require('../src/core/templateLibrary');
const { orchestrator, EngineTypes } = require('../src/core/orchestrator');
const { queryRouter, AnnouncementPriority } = require('../src/core/queryRouter');
const { contentEngine } = require('../src/layers/effects/engines/content');
const { lightingEngine } = require('../src/layers/effects/engines/lighting');
const { audioEngine } = require('../src/layers/effects/engines/audio');

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

function printJSON(data, title) {
  console.log(colorize(`\n📋 ${title}:`, 'yellow'));
  console.log(JSON.stringify(data, null, 2));
}

const scenarios = {
  morning_commute: {
    name: '早晨通勤',
    description: '工作日早晨，独自开车上班',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.35 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.15 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' }, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 72, timestamp: Date.now() }
    ],
    context: { speed: 70, passengerCount: 0 }
  },
  night_drive: {
    name: '深夜驾驶',
    description: '深夜独自开车回家，安静放松',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.25 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.05 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' }, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 65, timestamp: Date.now() }
    ],
    context: { speed: 50, passengerCount: 0 }
  },
  road_trip: {
    name: '朋友出游',
    description: '周末和朋友一起开车出游',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.6 }, timestamp: Date.now() },
      { source: SignalSources.VHAL, type: 'passenger_count', value: { passenger_count: 3 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.6 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'sunny' }, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 85, timestamp: Date.now() }
    ],
    context: { speed: 120, passengerCount: 3 }
  },
  fatigue_alert: {
    name: '疲劳提醒',
    description: '检测到驾驶员疲劳，紧急唤醒',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.3 }, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'fatigue_level', value: 0.85, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 58, timestamp: Date.now() }
    ],
    context: { speed: 60, fatigueLevel: 0.85 }
  },
  rainy_night: {
    name: '雨夜行车',
    description: '下雨的夜晚，安静驾驶',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.2 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.1 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'rain' }, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'heart_rate', value: 68, timestamp: Date.now() }
    ],
    context: { speed: 40, weather: 'rain' }
  },
  romantic_date: {
    name: '浪漫约会',
    description: '晚上和伴侣开车兜风',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.25 }, timestamp: Date.now() },
      { source: SignalSources.VHAL, type: 'passenger_count', value: { passenger_count: 1 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.75 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'clear' }, timestamp: Date.now() }
    ],
    context: { speed: 50, passengerCount: 1 }
  },
  traffic_jam: {
    name: '交通拥堵',
    description: '堵车中，缓解焦躁情绪',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.02 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.5 }, timestamp: Date.now() },
      { source: SignalSources.BIOMETRIC, type: 'stress_level', value: 0.6, timestamp: Date.now() }
    ],
    context: { speed: 5 }
  },
  focus_work: {
    name: '车内办公',
    description: '停车休息，在车内专注工作',
    signals: [
      { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0 }, timestamp: Date.now() },
      { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.4 }, timestamp: Date.now() }
    ],
    context: { speed: 0 }
  }
};

async function runScenario(scenarioKey) {
  const scenario = scenarios[scenarioKey];
  if (!scenario) {
    console.error(colorize(`❌ 未知场景: ${scenarioKey}`, 'red'));
    return;
  }

  printSeparator(`场景: ${scenario.name}`);
  console.log(colorize(`📝 描述: ${scenario.description}`, 'cyan'));

  console.log(colorize('\n📥 输入信号:', 'blue'));
  scenario.signals.forEach((s, i) => {
    console.log(`   ${i + 1}. ${s.source}.${s.type}: ${JSON.stringify(s.value)}`);
  });

  layer1.clear();
  layer2.clear();

  const layer1Output = layer1.processBatch(scenario.signals);
  console.log(colorize('\n🔄 Layer 1 输出 (信号预处理):', 'green'));
  console.log(`   信号数量: ${layer1Output.signal_summary.total_count}`);
  console.log(`   高置信度信号: ${layer1Output.signal_summary.high_confidence_count}`);
  console.log(`   活跃信号源: ${layer1Output.signal_summary.active_sources.join(', ')}`);

  const layer2Output = layer2.process(layer1Output);
  console.log(colorize('\n🧠 Layer 2 输出 (场景识别):', 'green'));
  console.log(`   场景类型: ${colorize(layer2Output.scene_vector.scene_type, 'magenta')}`);
  console.log(`   置信度: ${(layer2Output.scene_vector.confidence * 100).toFixed(1)}%`);
  console.log(`   场景维度:`);
  Object.entries(layer2Output.scene_vector.dimensions).forEach(([key, value]) => {
    console.log(`      - ${key}: ${value.toFixed(2)}`);
  });

  const template = templateLibrary.matchTemplate(layer2Output.scene_vector, scenario.context);
  console.log(colorize('\n📚 匹配模板:', 'green'));
  console.log(`   模板ID: ${template.template_id}`);
  console.log(`   模板名称: ${template.name}`);
  console.log(`   优先级: ${template.priority}`);
  console.log(`   来源: ${template.source || 'preset'}`);

  if (template.announcement_templates && template.announcement_templates.length > 0) {
    console.log(colorize('\n📢 TTS 播报文案:', 'magenta'));
    template.announcement_templates.forEach((text, i) => {
      console.log(`   ${i + 1}. "${text}"`);
    });
    
    const announcement = queryRouter.generateAnnouncementFromTemplate(template, scenario.context);
    console.log(colorize(`\n🔊 选中播报: "${announcement.text}"`, 'yellow'));
    console.log(`   语音风格: ${announcement.voice_style}`);
  }

  const sceneDescriptor = {
    version: '2.0',
    scene_id: `scene_${Date.now()}`,
    scene_name: template.name,
    scene_narrative: template.description,
    intent: template.intent,
    hints: template.hints,
    announcement: template.announcement_templates ? template.announcement_templates[0] : undefined,
    meta: {
      created_at: Date.now(),
      source: 'template',
      template_id: template.template_id,
      confidence: layer2Output.scene_vector.confidence
    }
  };

  printJSON(sceneDescriptor, '最终 Scene Descriptor JSON');

  orchestrator.registerEngine(EngineTypes.CONTENT, contentEngine);
  orchestrator.registerEngine(EngineTypes.LIGHTING, lightingEngine);
  orchestrator.registerEngine(EngineTypes.AUDIO, audioEngine);

  console.log(colorize('\n⚙️  引擎执行结果:', 'blue'));

  const musicResult = await contentEngine.execute('curate_playlist', {
    hints: template.hints?.music || {},
    constraints: template.intent?.constraints || {}
  });
  console.log(colorize('\n🎵 内容引擎:', 'green'));
  console.log(`   播放列表长度: ${musicResult.playlist.length} 首`);
  console.log(`   总时长: ${Math.floor(musicResult.total_duration / 60)} 分钟`);
  console.log(`   前3首歌曲:`);
  musicResult.playlist.slice(0, 3).forEach((track, i) => {
    console.log(`      ${i + 1}. ${track.title} - ${track.artist} (${track.genre})`);
  });

  const lightingResult = await lightingEngine.execute('apply_theme', {
    theme: template.hints?.lighting?.color_theme || 'calm',
    pattern: template.hints?.lighting?.pattern || 'breathing',
    intensity: template.hints?.lighting?.intensity || 0.5
  });
  console.log(colorize('\n💡 灯光引擎:', 'green'));
  console.log(`   颜色主题: ${lightingResult.theme}`);
  console.log(`   主色调: ${lightingResult.colors.primary}`);
  console.log(`   动效模式: ${lightingResult.pattern}`);
  console.log(`   亮度: ${(lightingResult.intensity * 100).toFixed(0)}%`);

  const audioResult = await audioEngine.execute('apply_preset', {
    preset: template.hints?.audio?.preset || 'standard'
  });
  console.log(colorize('\n🔊 音效引擎:', 'green'));
  console.log(`   音效预设: ${audioResult.preset}`);
  console.log(`   低音: ${audioResult.settings.bass > 0 ? '+' : ''}${audioResult.settings.bass}`);
  console.log(`   中音: ${audioResult.settings.mid > 0 ? '+' : ''}${audioResult.settings.mid}`);
  console.log(`   高音: ${audioResult.settings.treble > 0 ? '+' : ''}${audioResult.settings.treble}`);
  console.log(`   空间音频: ${audioResult.settings.spatial ? '开启' : '关闭'}`);

  console.log(colorize('\n✅ 场景处理完成!', 'green'));

  return sceneDescriptor;
}

async function demoTemplateLearning() {
  printSeparator('模板学习机制演示');
  
  console.log(colorize('\n📝 模拟 LLM 生成的 Scene Descriptor:', 'cyan'));
  const llmSceneDescriptor = {
    intent: {
      scene_type: 'custom_relaxation',
      mood: { valence: 0.6, arousal: 0.2 },
      energy_level: 0.2,
      atmosphere: 'peaceful_custom'
    },
    hints: {
      music: { genres: ['ambient', 'nature'], tempo: 'slow', vocal_style: 'instrumental' },
      lighting: { color_theme: 'calm', pattern: 'breathing', intensity: 0.2 },
      audio: { preset: 'night_mode' }
    },
    announcement: '为您准备了专属的放松音乐'
  };
  
  console.log(JSON.stringify(llmSceneDescriptor, null, 2));
  
  const executionId = `exec_${Date.now()}`;
  console.log(colorize(`\n🔄 记录执行 (ID: ${executionId})...`, 'yellow'));
  
  templateLibrary.recordExecution(executionId, llmSceneDescriptor, { hour: 22 });
  
  console.log(colorize('\n⏳ 等待反馈窗口 (模拟无负面反馈)...', 'cyan'));
  
  const learnedTemplate = templateLibrary.templateLearner.learnFromDescriptor(llmSceneDescriptor, { hour: 22 });
  
  if (learnedTemplate) {
    console.log(colorize('\n✅ 学习成功! 新模板:', 'green'));
    console.log(`   模板ID: ${learnedTemplate.template_id}`);
    console.log(`   场景类型: ${learnedTemplate.scene_type}`);
    console.log(`   名称: ${learnedTemplate.name}`);
    console.log(`   来源: ${learnedTemplate.source}`);
  }
  
  console.log(colorize('\n📊 模板库统计:', 'blue'));
  const stats = templateLibrary.getStats();
  console.log(`   总模板数: ${stats.total}`);
  console.log(`   预置模板: ${stats.bySource.preset}`);
  console.log(`   学习模板: ${stats.bySource.learned}`);
  console.log(`   自定义模板: ${stats.bySource.custom}`);
}

async function demoTTSAnnouncement() {
  printSeparator('TTS 引导词机制演示');
  
  console.log(colorize('\n🎤 模拟用户语音输入:', 'cyan'));
  const voiceInput = { text: '帮我选一首适合现在的歌' };
  console.log(`   "${voiceInput.text}"`);
  
  queryRouter.clearQueues();
  
  const routeResult = queryRouter.route(voiceInput);
  console.log(colorize('\n📤 路由结果:', 'green'));
  console.log(`   意图类型: ${routeResult.intent}`);
  console.log(`   ACK 文案: "${routeResult.ack.text}"`);
  console.log(`   语音风格: ${routeResult.ack.voice_style}`);
  console.log(`   预计等待: ${routeResult.ack.estimated_wait_sec} 秒`);
  
  console.log(colorize('\n📢 模拟场景切换 Announcement:', 'cyan'));
  const template = templateLibrary.getTemplate('TPL_009');
  if (template) {
    const announcement = queryRouter.generateAnnouncementFromTemplate(template, { weather: 'rain' });
    console.log(`   播报文案: "${announcement.text}"`);
    console.log(`   语音风格: ${announcement.voice_style}`);
    console.log(`   场景类型: ${announcement.scene_type}`);
  }
  
  console.log(colorize('\n🔄 播报队列状态:', 'blue'));
  const queueStatus = queryRouter.getQueueStatus();
  console.log(`   ACK 队列长度: ${queueStatus.ackQueueLength}`);
  console.log(`   Announcement 队列长度: ${queueStatus.announcementQueueLength}`);
  console.log(`   ACK 播报中: ${queueStatus.isAckPlaying}`);
}

async function runAllScenarios() {
  console.log(colorize('\n🚗 车载座舱 AI 娱乐融合方案 - 快通道演示', 'bright'));
  console.log(colorize('═'.repeat(60), 'cyan'));

  const stats = templateLibrary.getStats();
  console.log(colorize(`\n📚 模板库已加载 ${stats.total} 个模板`, 'green'));
  console.log(`   预置模板: ${stats.bySource.preset}`);
  console.log(`   学习模板: ${stats.bySource.learned}`);
  console.log(`   自定义模板: ${stats.bySource.custom}`);

  const scenarioKeys = Object.keys(scenarios);
  const results = [];

  for (const key of scenarioKeys) {
    const result = await runScenario(key);
    results.push({ key, scene_name: result?.scene_name });
    await new Promise(resolve => setTimeout(resolve, 500));
  }

  printSeparator('场景对比汇总');
  console.log('\n');
  results.forEach((r, i) => {
    console.log(`${i + 1}. ${colorize(r.key.padEnd(20), 'cyan')} → ${colorize(r.scene_name || 'N/A', 'yellow')}`);
  });

  await demoTemplateLearning();
  await demoTTSAnnouncement();

  console.log(colorize('\n\n🎉 所有演示完成!', 'green'));
  console.log(colorize('\n提示: 可以运行单个场景: node scripts/demo_fast_track.js <场景名>', 'cyan'));
  console.log(colorize('可用场景: ' + Object.keys(scenarios).join(', '), 'cyan'));
  console.log(colorize('\n新增功能演示:', 'magenta'));
  console.log(colorize('  --learning  演示模板学习机制', 'cyan'));
  console.log(colorize('  --tts       演示 TTS 引导词机制', 'cyan'));
}

async function main() {
  const args = process.argv.slice(2);

  if (args.includes('--learning')) {
    await demoTemplateLearning();
  } else if (args.includes('--tts')) {
    await demoTTSAnnouncement();
  } else if (args.length === 0) {
    await runAllScenarios();
  } else {
    for (const scenarioKey of args) {
      await runScenario(scenarioKey);
    }
  }
}

main().catch(console.error);

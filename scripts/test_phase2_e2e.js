#!/usr/bin/env node
'use strict';

/**
 * @fileoverview Phase 2 端到端集成测试
 * @description 测试快慢双通道、引擎协作、反馈机制
 */

const assert = require('assert');
const { layer1, SignalSources } = require('../src/core/layer1');
const { layer2, SceneTypes } = require('../src/core/layer2');
const { Layer3 } = require('../src/core/layer3');
const { orchestrator, EngineTypes, TrackMode } = require('../src/core/orchestrator');
const { queryRouter } = require('../src/core/queryRouter');
const { templateLibrary } = require('../src/core/templateLibrary');
const { feedbackManager, FeedbackAction } = require('../src/core/feedback');
const { eventBus, EventTypes } = require('../src/core/eventBus');
const { contentEngine } = require('../src/engines/content');
const { lightingEngine } = require('../src/engines/lighting');
const { audioEngine } = require('../src/engines/audio');

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

const testResults = [];

function recordTest(name, passed, details = '') {
  testResults.push({ name, passed, details });
  const status = passed ? colorize('✅ PASS', 'green') : colorize('❌ FAIL', 'red');
  console.log(`  ${status} ${name}${details ? ` - ${details}` : ''}`);
}

async function testEngineV2Features() {
  printSeparator('测试引擎 V2 特性');

  console.log(colorize('\n📋 测试 Hints 处理', 'yellow'));
  const playlistResult = await contentEngine.execute('curate_playlist', {
    hints: {
      music: {
        genres: ['jazz', 'blues'],
        tempo: 'slow',
        vocal_style: 'instrumental',
        energy: 0.3
      }
    },
    constraints: {
      max_duration_sec: 600,
      explicit_content: false
    }
  });
  
  recordTest('Content Engine 处理 Hints', playlistResult.hints_applied > 0, `应用了 ${Number(playlistResult.hints_applied)} 个 hints`);
  recordTest('Content Engine 处理约束', playlistResult.constraints_applied > 0, `应用了 ${playlistResult.constraints_applied} 个约束`);
  recordTest('播放列表生成', playlistResult.playlist.length > 0, `${playlistResult.playlist.length} 首歌曲`);

  console.log(colorize('\n📋 测试灯光引擎协作', 'yellow'));
  const lightingResult = await lightingEngine.execute('apply_theme', {
    theme: 'calm',
    pattern: 'breathing',
    intensity: 0.4,
    hints: {
      lighting: {
        color_theme: 'night',
        intensity: 0.3
      }
    }
  });
  
  recordTest('Lighting Engine 处理 Hints', lightingResult.hints_applied > 0);
  recordTest('灯光主题应用', lightingResult.theme === 'night', `主题: ${lightingResult.theme}`);

  console.log(colorize('\n📋 测试音效引擎协作', 'yellow'));
  const audioResult = await audioEngine.execute('apply_preset', {
    preset: 'vocal_clarity',
    hints: {
      audio: {
        bass: -2,
        spatial: true
      }
    }
  });
  
  recordTest('Audio Engine 处理 Hints', audioResult.hints_applied > 0);
  recordTest('音效预设应用', audioResult.preset === 'vocal_clarity');
}

async function testEngineCollaboration() {
  printSeparator('测试引擎间协作');

  console.log(colorize('\n📋 测试音乐节拍同步', 'yellow'));
  
  lightingEngine.execute('sync_with_music', { enabled: true, sensitivity: 0.8 });
  
  eventBus.emit(EventTypes.MUSIC_BEAT, {
    beat: 1,
    intensity: 0.7,
    timestamp: Date.now()
  });
  
  await new Promise(resolve => setTimeout(resolve, 100));
  
  const lightingStatus = lightingEngine.getStatus();
  recordTest('灯光节拍同步', lightingStatus.collaboration.beatSync === true);

  console.log(colorize('\n📋 测试能量级别联动', 'yellow'));
  
  contentEngine.execute('curate_playlist', {
    hints: { music: { energy: 0.8 } }
  });
  
  await lightingEngine.execute('adjust_for_energy', { energy: 0.8 });
  await audioEngine.execute('adjust_for_energy', { energy: 0.8 });
  
  const lightingStatus2 = lightingEngine.getStatus();
  const audioStatus = audioEngine.getStatus();
  
  recordTest('灯光能量联动', lightingStatus2.intensity > 0.5, `强度: ${lightingStatus2.intensity}`);
  recordTest('音效能量联动', audioStatus.preset.name !== 'night_mode', `预设: ${audioStatus.preset.name}`);
}

async function testFeedbackMechanism() {
  printSeparator('测试反馈机制');

  console.log(colorize('\n📋 测试反馈记录', 'yellow'));
  
  feedbackManager.startSession('test_session_001', { scene: 'morning_commute' });
  
  feedbackManager.recordFeedback({
    action: FeedbackAction.LIKE,
    track_id: 'track_001',
    user_id: 'user_001'
  });
  
  feedbackManager.recordFeedback({
    action: FeedbackAction.SKIP,
    track_id: 'track_002',
    reason: 'user_skip'
  });
  
  const stats = feedbackManager.getFeedbackStats();
  recordTest('反馈统计', stats.total === 2, `总数: ${stats.total}`);
  recordTest('正面反馈率', stats.positive_rate === '0.50', `率: ${stats.positive_rate}`);

  console.log(colorize('\n📋 测试会话报告生成', 'yellow'));
  
  feedbackManager.endSession();
  
  const recentFeedback = feedbackManager.getRecentFeedback(5);
  recordTest('反馈历史', recentFeedback.length >= 2, `${recentFeedback.length} 条记录`);
}

async function testDualTrackFlow() {
  printSeparator('测试快慢双通道流程');

  console.log(colorize('\n📋 初始化 Orchestrator', 'yellow'));
  
  orchestrator.registerEngine(EngineTypes.CONTENT, contentEngine);
  orchestrator.registerEngine(EngineTypes.LIGHTING, lightingEngine);
  orchestrator.registerEngine(EngineTypes.AUDIO, audioEngine);
  
  const layer3 = new Layer3({
    apiKey: process.env.DASHSCOPE_API_KEY || 'sk-fb1a1b32bf914059a043ee4ebd1c845a',
    model: 'qwen-plus',
    timeout: 15000,
    enableCache: false
  });
  
  orchestrator.setLayer3(layer3);
  orchestrator.config.trackMode = TrackMode.DUAL;

  console.log(colorize('\n📋 测试信号处理流程', 'yellow'));
  
  layer1.clear();
  layer2.clear();
  
  const rawSignals = [
    { source: SignalSources.VHAL, type: 'vehicle_speed', value: { vehicle_speed: 0.4 }, timestamp: Date.now() },
    { source: SignalSources.ENVIRONMENT, type: 'time_of_day', value: { time_of_day: 0.3 }, timestamp: Date.now() },
    { source: SignalSources.ENVIRONMENT, type: 'weather', value: { weather: 'sunny' }, timestamp: Date.now() }
  ];

  const layer1Output = layer1.processBatch(rawSignals);
  recordTest('Layer 1 信号处理', layer1Output.signals.length === 3);

  const layer2Output = layer2.process(layer1Output);
  recordTest('Layer 2 场景识别', layer2Output.scene_vector !== null, `类型: ${layer2Output.scene_vector.scene_type}`);

  console.log(colorize('\n📋 测试快通道模板匹配', 'yellow'));
  
  const context = { speed: 80, passengerCount: 0, weather: 'sunny' };
  const matchedTemplate = templateLibrary.matchTemplate(layer2Output.scene_vector, context);
  
  recordTest('模板匹配', matchedTemplate !== null, `模板: ${matchedTemplate?.name || '无'}`);

  console.log(colorize('\n📋 测试双通道执行', 'yellow'));
  
  const startTime = Date.now();
  
  const result = await orchestrator.executeDualTrack(layer1Output, layer2Output, context);
  
  const duration = Date.now() - startTime;
  
  recordTest('快通道响应', result.fastDescriptor !== null, '快通道已返回');
  recordTest('双通道完成时间', duration < 10000, `${duration}ms`);
  
  console.log(colorize('\n📊 双通道结果:', 'magenta'));
  console.log(`   快通道场景: ${result.fastDescriptor?.scene_name || '未知'}`);
  console.log(`   慢通道场景: result.slowDescriptor ? '已生成' : '未生成'`);
  console.log(`   总耗时: ${result.duration}ms`);
}

async function testEventBusIntegration() {
  printSeparator('测试事件总线集成');

  console.log(colorize('\n📋 测试事件发布订阅', 'yellow'));
  
  let eventReceived = false;
  
  eventBus.once(EventTypes.ENGINE_ACTION, (data) => {
    if (data && data.engine === 'test') {
      eventReceived = true;
    }
  });
  
  eventBus.emit(EventTypes.ENGINE_ACTION, {
    engine: 'test',
    action: 'test_action'
  });
  
  await new Promise(resolve => setTimeout(resolve, 50));
  
  recordTest('事件发布订阅', eventReceived);

  console.log(colorize('\n📋 测试事件历史', 'yellow'));
  
  const history = eventBus.getHistory();
  recordTest('事件历史记录', history.length > 0, `${history.length} 条事件`);
}

async function runAllTests() {
  console.log(colorize('\n🚀 Phase 2 端到端集成测试', 'bright'));
  console.log(colorize('═'.repeat(60), 'cyan'));

  try {
    await testEngineV2Features();
    await testEngineCollaboration();
    await testFeedbackMechanism();
    await testDualTrackFlow();
    await testEventBusIntegration();
  } catch (error) {
    console.error(colorize(`\n❌ 测试执行错误: ${error.message}`, 'red'));
  }

  printSeparator('测试结果汇总');
  
  const passed = testResults.filter(t => t.passed).length;
  const failed = testResults.filter(t => !t.passed).length;
  const total = testResults.length;
  
  console.log(colorize('\n📊 统计:', 'yellow'));
  console.log(`   总测试数: ${total}`);
  console.log(`   ${colorize('通过: ' + passed, 'green')}`);
  console.log(`   ${colorize('失败: ' + failed, failed > 0 ? 'red' : 'green')}`);
  console.log(`   通过率: ${((passed / total) * 100).toFixed(1)}%`);

  if (failed > 0) {
    console.log(colorize('\n❌ 失败的测试:', 'red'));
    testResults.filter(t => !t.passed).forEach(t => {
      console.log(`   - ${t.name}: ${t.details}`);
    });
  }

  console.log(colorize('\n✅ Phase 2 集成测试完成!', 'green'));
  
  return { passed, failed, total };
}

runAllTests().then(({ passed, failed }) => {
  process.exit(failed > 0 ? 1 : 0);
}).catch(error => {
  console.error(error);
  process.exit(1);
});

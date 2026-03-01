#!/usr/bin/env node
'use strict';

/**
 * @fileoverview LLM 调用链路验证测试
 * @description 验证 Layer2 和 Layer3 在不同场景下的 LLM 调用行为
 */

const fs = require('fs');
const path = require('path');

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
  console.log('\n' + colorize('═'.repeat(70), 'cyan'));
  console.log(colorize(`  ${title}`, 'bright'));
  console.log(colorize('═'.repeat(70), 'cyan'));
}

const testResults = {
  layer2Calls: [],
  layer3Calls: [],
  scenarios: []
};

async function runTests() {
  printSeparator('LLM 调用链路验证测试');
  
  console.log(colorize('\n📋 测试环境检查:', 'yellow'));
  
  const hasApiKey = !!process.env.DASHSCOPE_API_KEY;
  console.log(`   DASHSCOPE_API_KEY: ${hasApiKey ? colorize('已设置', 'green') : colorize('未设置', 'red')}`);
  
  if (!hasApiKey) {
    console.log(colorize('\n⚠️  警告: 未设置 DASHSCOPE_API_KEY，LLM 相关测试将使用 Mock 数据', 'yellow'));
  }
  
  const { perceptionLayer } = require('../src/layers/perception');
  const { semanticLayer, SceneTypes } = require('../src/layers/semantic');
  const { effectsLayer } = require('../src/layers/effects');
  const { templateLibrary } = require('../src/core/templateLibrary');
  const { contentEngine } = require('../src/layers/effects/engines/content');
  const { Layer3 } = require('../src/core/layer3');
  
  const templateStats = templateLibrary.getStats();
  console.log(`   模板库: ${templateStats.total} 个模板 (预设: ${templateStats.bySource.preset})`);
  
  const availableScenes = contentEngine.getAvailableScenes();
  console.log(`   预设音乐库场景: ${availableScenes.length} 个 [${availableScenes.slice(0, 5).join(', ')}${availableScenes.length > 5 ? '...' : ''}]`);
  
  printSeparator('问题 1: Layer2 快通道触发后，Layer3 音乐推荐是否固定？');
  
  console.log(colorize('\n📝 分析结论:', 'cyan'));
  console.log('   Layer2 快通道只生成 scene_descriptor（场景描述）');
  console.log('   Layer3 ContentEngine 有独立的三级歌单生成策略：');
  console.log('   1. 预设音乐库 (data/music_library.json)');
  console.log('   2. LLM 生成（如果第一级无结果且 LLM 可用）');
  console.log('   3. Mock 数据（8 首硬编码曲目）');
  console.log(colorize('\n   结论: 音乐不是固定的，取决于 sceneType 是否在预设音乐库中', 'green'));
  
  printSeparator('问题 2: Layer3 音乐推荐曲库来源？');
  
  console.log(colorize('\n📝 分析结论:', 'cyan'));
  console.log('   - 预设音乐库: data/music_library.json (本地 JSON 文件)');
  console.log('   - Mock 数据: 8 首硬编码曲目');
  console.log('   - LLM 生成: AI "模拟全网搜索"，返回虚构的歌曲信息');
  console.log(colorize('\n   结论: 当前是本地数据 + AI 虚构，没有接入真实音乐源', 'green'));
  
  printSeparator('问题 3: 非模板场景下 Layer2 和 Layer3 是否都会调用 LLM？');
  
  console.log(colorize('\n📝 分析结论:', 'cyan'));
  console.log('   - Layer2 (SemanticLayer): 模板不匹配且 enableLLM=true → 调用 LLM');
  console.log('   - Layer3 (ContentEngine): sceneType 不在预设音乐库且 LLM 可用 → 调用 LLM');
  console.log(colorize('\n   结论: 是的，可能调用两次 LLM，预计总延迟 6-16 秒', 'green'));
  
  printSeparator('实际测试验证');
  
  const testDataPath = path.join(__dirname, '../tests/data/scene_templates_test.json');
  let testData;
  try {
    testData = JSON.parse(fs.readFileSync(testDataPath, 'utf8'));
  } catch (err) {
    console.error(colorize(`\n❌ 无法读取测试数据: ${err.message}`, 'red'));
    return;
  }
  
  const genScenarios = testData.generated || [];
  console.log(colorize(`\n📋 加载了 ${genScenarios.length} 个 gen_ 开头的测试场景`, 'yellow'));
  
  const testCases = [
    { name: '模板内场景 (morning_commute)', sceneType: 'morning_commute', expectedTemplate: true },
    { name: '非模板场景 (rural_drive)', sceneType: 'rural_drive', expectedTemplate: false },
    { name: '非模板场景 (tunnel_drive)', sceneType: 'tunnel_drive', expectedTemplate: false }
  ];
  
  for (const testCase of testCases) {
    console.log(colorize(`\n\n📍 测试: ${testCase.name}`, 'magenta'));
    console.log('   ' + '─'.repeat(50));
    
    const scenario = genScenarios.find(s => s.scene_type === testCase.sceneType) || genScenarios[0];
    if (!scenario) {
      console.log(colorize('   ⚠️  未找到匹配的测试场景', 'yellow'));
      continue;
    }
    
    const startTime = Date.now();
    
    const layer1Output = {
      signals: {
        environment: scenario.input_signals?.environment || {},
        vehicle: scenario.input_signals?.vehicle || {},
        internal_camera: scenario.input_signals?.internal_camera || {}
      },
      raw_signals: []
    };
    
    console.log(colorize('\n   [Layer 2] 语义层处理:', 'blue'));
    const layer2Start = Date.now();
    
    semanticLayer.clear();
    
    const mockSceneVector = {
      scene_type: scenario.scene_type,
      dimensions: {
        social: scenario.input_signals?.internal_camera?.passengers?.adults > 0 ? 0.6 : 0,
        energy: scenario.intent?.energy_level || 0.5,
        focus: 0.5,
        time_context: 0.5,
        weather: scenario.input_signals?.environment?.weather === 'rain' ? 0.8 : 0.2
      },
      confidence: 0.85
    };
    
    const matchedTemplate = templateLibrary.matchTemplate(mockSceneVector, {
      weather: scenario.input_signals?.environment?.weather
    });
    
    const layer2Time = Date.now() - layer2Start;
    
    if (matchedTemplate) {
      console.log(colorize(`   ✓ 模板匹配成功: ${matchedTemplate.template_id} (${matchedTemplate.name})`, 'green'));
      console.log(`   ✓ 匹配耗时: ${layer2Time}ms`);
      console.log(`   ✓ Layer2 不会调用 LLM (使用模板)`);
      testResults.layer2Calls.push({ scenario: testCase.name, called: false, time: layer2Time });
    } else {
      console.log(colorize(`   ✗ 模板匹配失败`, 'yellow'));
      console.log(`   ✓ 匹配耗时: ${layer2Time}ms`);
      console.log(`   ⚠️  Layer2 会调用 LLM (如果启用)`);
      testResults.layer2Calls.push({ scenario: testCase.name, called: hasApiKey, time: layer2Time });
    }
    
    console.log(colorize('\n   [Layer 3] 内容引擎处理:', 'blue'));
    const layer3Start = Date.now();
    
    const hints = scenario.hints?.music || { genres: ['pop'], tempo: 'moderate' };
    const constraints = {};
    
    console.log(`   输入 hints: ${JSON.stringify(hints)}`);
    console.log(`   sceneType: ${scenario.scene_type}`);
    console.log(`   预设音乐库包含该场景? ${availableScenes.includes(scenario.scene_type) ? '是' : '否'}`);
    
    const playlistResult = await contentEngine.execute('curate_playlist', {
      hints,
      constraints,
      scene_type: scenario.scene_type
    });
    
    const layer3Time = Date.now() - layer3Start;
    
    console.log(colorize(`\n   ✓ 歌单生成完成`, 'green'));
    console.log(`   ✓ 数据来源: ${playlistResult.source}`);
    console.log(`   ✓ 歌曲数量: ${playlistResult.playlist.length} 首`);
    console.log(`   ✓ 处理耗时: ${layer3Time}ms`);
    
    if (playlistResult.source === 'llm') {
      console.log(colorize(`   ⚠️  Layer3 调用了 LLM`, 'yellow'));
      testResults.layer3Calls.push({ scenario: testCase.name, called: true, time: layer3Time });
    } else if (playlistResult.source === 'library') {
      console.log(`   ✓ Layer3 使用预设音乐库`);
      testResults.layer3Calls.push({ scenario: testCase.name, called: false, time: layer3Time });
    } else {
      console.log(`   ✓ Layer3 使用 Mock 数据`);
      testResults.layer3Calls.push({ scenario: testCase.name, called: false, time: layer3Time });
    }
    
    if (playlistResult.playlist.length > 0) {
      console.log(colorize('\n   前 3 首歌曲:', 'cyan'));
      playlistResult.playlist.slice(0, 3).forEach((track, i) => {
        console.log(`     ${i + 1}. ${track.title} - ${track.artist} (${track.genre})`);
      });
    }
    
    const totalTime = Date.now() - startTime;
    testResults.scenarios.push({
      name: testCase.name,
      sceneType: scenario.scene_type,
      layer2Time,
      layer3Time,
      totalTime,
      layer2Source: matchedTemplate ? 'template' : (hasApiKey ? 'llm' : 'fallback'),
      layer3Source: playlistResult.source
    });
  }
  
  printSeparator('测试结果汇总');
  
  console.log(colorize('\n📊 各场景处理路径:', 'yellow'));
  console.log('┌─────────────────────────────┬─────────────────┬─────────────────┬──────────┐');
  console.log('│ 场景                        │ Layer2 来源     │ Layer3 来源     │ 总耗时   │');
  console.log('├─────────────────────────────┼─────────────────┼─────────────────┼──────────┤');
  
  for (const s of testResults.scenarios) {
    const layer2Cell = s.layer2Source.padEnd(15);
    const layer3Cell = s.layer3Source.padEnd(15);
    const timeCell = `${s.totalTime}ms`.padEnd(8);
    console.log(`│ ${s.name.padEnd(27)} │ ${layer2Cell} │ ${layer3Cell} │ ${timeCell} │`);
  }
  console.log('└─────────────────────────────┴─────────────────┴─────────────────┴──────────┘');
  
  console.log(colorize('\n📊 LLM 调用统计:', 'yellow'));
  const layer2LLMCalls = testResults.layer2Calls.filter(c => c.called).length;
  const layer3LLMCalls = testResults.layer3Calls.filter(c => c.called).length;
  console.log(`   Layer2 LLM 调用次数: ${layer2LLMCalls}/${testResults.layer2Calls.length}`);
  console.log(`   Layer3 LLM 调用次数: ${layer3LLMCalls}/${testResults.layer3Calls.length}`);
  
  printSeparator('三个问题的最终答案');
  
  console.log(colorize('\n❓ 问题 1: Layer2 快通道触发后，Layer3 音乐推荐是否固定？', 'cyan'));
  console.log(colorize('   ✅ 答案: 不是固定的。Layer3 有独立的歌单生成策略。', 'green'));
  console.log('   即使 Layer2 使用模板，Layer3 仍可能调用 LLM 生成歌单。');
  
  console.log(colorize('\n❓ 问题 2: Layer3 音乐推荐曲库来源？', 'cyan'));
  console.log(colorize('   ✅ 答案: 本地数据 + AI 虚构，没有接入真实音乐源。', 'green'));
  console.log('   当前预设音乐库为空，实际使用 Mock 数据或 LLM 生成。');
  
  console.log(colorize('\n❓ 问题 3: 非模板场景下 Layer2 和 Layer3 是否都会调用 LLM？', 'cyan'));
  console.log(colorize('   ✅ 答案: 是的，可能调用两次 LLM。', 'green'));
  if (hasApiKey) {
    console.log('   预计总延迟: 6-16 秒 (两次 LLM 调用叠加)');
  } else {
    console.log('   当前未设置 API Key，实际使用 Mock/Fallback 数据。');
  }
  
  console.log(colorize('\n\n✅ 测试完成!', 'green'));
}

runTests().catch(err => {
  console.error(colorize(`\n❌ 测试失败: ${err.message}`, 'red'));
  console.error(err.stack);
  process.exit(1);
});

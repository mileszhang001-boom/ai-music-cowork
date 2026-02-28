#!/usr/bin/env node
'use strict';

/**
 * @fileoverview LLM 接入测试脚本
 * @description 测试阿里云百炼 Qwen 系列模型的接入
 */

const path = require('path');
const fs = require('fs');

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

const API_KEY = 'sk-fb1a1b32bf914059a043ee4ebd1c845a';
const BASE_URL = 'https://dashscope.aliyuncs.com/compatible-mode/v1';

const MODELS = {
  FLASH: 'qwen3.5-flash',
  PLUS: 'qwen3.5-plus',
  TURBO: 'qwen-turbo',
  QWEN_PLUS: 'qwen-plus'
};

const testResults = [];

async function testBasicConnection(model, enableThinking = false) {
  const testName = `基础连接测试 - ${model}${enableThinking ? ' (思考模式)' : ''}`;
  console.log(colorize(`\n📋 ${testName}`, 'yellow'));
  
  try {
    const OpenAI = require('openai');
    const openai = new OpenAI({
      apiKey: API_KEY,
      baseURL: BASE_URL,
      timeout: 30000
    });

    const requestConfig = {
      model: model,
      messages: [
        { role: 'system', content: '你是一个友好的助手。' },
        { role: 'user', content: '你好，请用一句话介绍你自己。' }
      ],
      max_tokens: 100
    };

    if (model.includes('qwen3.5')) {
      requestConfig.enable_thinking = enableThinking;
    }

    const startTime = Date.now();
    const completion = await openai.chat.completions.create(requestConfig);
    const duration = Date.now() - startTime;

    const content = completion.choices[0]?.message?.content;
    const reasoningTokens = completion.usage?.completion_tokens_details?.reasoning_tokens || 0;
    
    console.log(colorize(`✅ 连接成功!`, 'green'));
    console.log(`   响应时间: ${duration}ms`);
    console.log(`   模型: ${completion.model}`);
    console.log(`   思考Token: ${reasoningTokens}`);
    console.log(`   响应: ${content?.substring(0, 100)}...`);
    console.log(`   Token 使用: ${JSON.stringify(completion.usage)}`);

    testResults.push({
      test: testName,
      model,
      success: true,
      duration,
      reasoningTokens,
      tokens: completion.usage?.total_tokens
    });

    return { success: true, duration, content, reasoningTokens };
  } catch (error) {
    console.log(colorize(`❌ 连接失败: ${error.message}`, 'red'));
    
    testResults.push({
      test: testName,
      model,
      success: false,
      error: error.message
    });

    return { success: false, error: error.message };
  }
}

async function testSceneReasoning(model, enableThinking = false) {
  const testName = `场景推理测试 - ${model}${enableThinking ? ' (思考模式)' : ''}`;
  console.log(colorize(`\n📋 ${testName}`, 'yellow'));

  const systemPrompt = `你是一个车载座舱 AI 娱乐助手，专门负责根据当前驾驶场景为用户推荐最合适的音乐、灯光和音效配置。

输出要求：
- 必须输出符合 Scene Descriptor V2.0 规范的 JSON 格式
- 包含 intent、hints、announcement 三个核心字段
- 不要输出任何额外的解释文字，只输出 JSON`;

  const userPrompt = `当前场景信息：
- 时间: 早晨 7:30
- 天气: 晴朗
- 车速: 60 km/h
- 乘客: 0人（独自驾驶）
- 疲劳度: 0.2（状态良好）

请根据以上场景信息，生成一个 Scene Descriptor JSON。`;

  try {
    const OpenAI = require('openai');
    const openai = new OpenAI({
      apiKey: API_KEY,
      baseURL: BASE_URL,
      timeout: 60000
    });

    const requestConfig = {
      model: model,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt }
      ],
      max_tokens: 1000,
      temperature: 0.7
    };

    if (model.includes('qwen3.5')) {
      requestConfig.enable_thinking = enableThinking;
    }

    const startTime = Date.now();
    const completion = await openai.chat.completions.create(requestConfig);
    const duration = Date.now() - startTime;

    const content = completion.choices[0]?.message?.content;
    const reasoningTokens = completion.usage?.completion_tokens_details?.reasoning_tokens || 0;
    
    console.log(colorize(`✅ 场景推理成功!`, 'green'));
    console.log(`   响应时间: ${duration}ms`);
    console.log(`   思考Token: ${reasoningTokens}`);
    console.log(`   Token 使用: ${JSON.stringify(completion.usage)}`);
    
    let parsedJson = null;
    try {
      let jsonContent = content.trim();
      if (jsonContent.startsWith('```json')) {
        jsonContent = jsonContent.slice(7);
      }
      if (jsonContent.startsWith('```')) {
        jsonContent = jsonContent.slice(3);
      }
      if (jsonContent.endsWith('```')) {
        jsonContent = jsonContent.slice(0, -3);
      }
      parsedJson = JSON.parse(jsonContent.trim());
      console.log(colorize(`   JSON 解析: 成功`, 'green'));
      console.log(`   场景类型: ${parsedJson.intent?.atmosphere || '未知'}`);
      console.log(`   能量级别: ${parsedJson.intent?.energy_level || '未知'}`);
    } catch (parseError) {
      console.log(colorize(`   JSON 解析: 失败 - ${parseError.message}`, 'yellow'));
    }

    testResults.push({
      test: testName,
      model,
      success: true,
      duration,
      reasoningTokens,
      tokens: completion.usage?.total_tokens,
      jsonValid: parsedJson !== null
    });

    return { success: true, duration, content, parsedJson, reasoningTokens };
  } catch (error) {
    console.log(colorize(`❌ 场景推理失败: ${error.message}`, 'red'));
    
    testResults.push({
      test: testName,
      model,
      success: false,
      error: error.message
    });

    return { success: false, error: error.message };
  }
}

async function testErrorHandling(model) {
  const testName = `错误处理测试 - ${model}`;
  console.log(colorize(`\n📋 ${testName}`, 'yellow'));

  try {
    const OpenAI = require('openai');
    const openai = new OpenAI({
      apiKey: 'invalid-api-key',
      baseURL: BASE_URL,
      timeout: 5000
    });

    await openai.chat.completions.create({
      model: model,
      messages: [{ role: 'user', content: 'test' }]
    });

    console.log(colorize(`❌ 错误处理测试失败: 应该抛出错误`, 'red'));
    testResults.push({ test: testName, model, success: false, error: 'Should have thrown' });
    return { success: false };
  } catch (error) {
    console.log(colorize(`✅ 错误处理正确!`, 'green'));
    console.log(`   错误类型: ${error.status || error.code || 'unknown'}`);
    console.log(`   错误信息: ${error.message?.substring(0, 100)}`);
    
    testResults.push({
      test: testName,
      model,
      success: true,
      errorType: error.status || error.code
    });

    return { success: true };
  }
}

async function testModelComparison() {
  printSeparator('模型对比测试 (禁用思考模式)');

  const models = [MODELS.FLASH, MODELS.PLUS, MODELS.QWEN_PLUS];
  const comparisonResults = [];

  for (const model of models) {
    console.log(colorize(`\n📊 测试模型: ${model}`, 'cyan'));
    
    const basicResult = await testBasicConnection(model, false);
    
    if (basicResult.success) {
      const sceneResult = await testSceneReasoning(model, false);
      comparisonResults.push({
        model,
        basicSuccess: basicResult.success,
        sceneSuccess: sceneResult.success,
        basicDuration: basicResult.duration,
        sceneDuration: sceneResult.duration,
        reasoningTokens: sceneResult.reasoningTokens || 0,
        jsonValid: sceneResult.parsedJson !== null
      });
    } else {
      comparisonResults.push({
        model,
        basicSuccess: false,
        sceneSuccess: false,
        basicDuration: basicResult.duration,
        error: basicResult.error
      });
    }
  }

  printSeparator('对比结果汇总');
  console.log('\n');
  console.log(colorize('模型'.padEnd(20) + '基础测试'.padEnd(12) + '场景推理'.padEnd(12) + '基础耗时'.padEnd(12) + '推理耗时'.padEnd(12) + '思考Token', 'bright'));
  console.log('─'.repeat(80));
  
  for (const result of comparisonResults) {
    const basicStatus = result.basicSuccess ? colorize('✅ 通过', 'green') : colorize('❌ 失败', 'red');
    const sceneStatus = result.sceneSuccess ? colorize('✅ 通过', 'green') : colorize('❌ 失败', 'red');
    const reasoningTokens = result.reasoningTokens || 0;
    
    console.log(
      result.model.padEnd(20) + 
      basicStatus.padEnd(20) + 
      sceneStatus.padEnd(20) + 
      `${result.basicDuration || '-'}ms`.padEnd(12) + 
      `${result.sceneDuration || '-'}ms`.padEnd(12) +
      `${reasoningTokens}`
    );
  }

  return comparisonResults;
}

async function testLayer3Integration() {
  printSeparator('Layer 3 集成测试');

  const { LLMClient, Models } = require('../src/core/llm');
  const { Layer3 } = require('../src/core/layer3');

  const llmClient = new LLMClient({
    apiKey: API_KEY,
    baseUrl: BASE_URL,
    model: Models.QWEN_PLUS
  });

  const layer3 = new Layer3({
    apiKey: API_KEY,
    baseUrl: BASE_URL,
    model: Models.QWEN_PLUS
  });

  console.log(colorize('\n📋 测试 LLM 客户端状态', 'yellow'));
  const stats = llmClient.getStats();
  console.log(`   就绪状态: ${llmClient.isReady() ? '✅ 就绪' : '❌ 未就绪'}`);
  console.log(`   配置模型: ${stats.config.model}`);
  console.log(`   配置地域: ${stats.config.region}`);

  console.log(colorize('\n📋 测试 Layer 3 场景推理', 'yellow'));
  
  const mockLayer1Output = {
    output_id: 'test_output',
    signals: [
      { source: 'vhal', type: 'vehicle_speed', normalized_value: { vehicle_speed: 0.5 } },
      { source: 'environment', type: 'time_of_day', normalized_value: { time_of_day: 0.3 } },
      { source: 'environment', type: 'weather', normalized_value: { weather: 'sunny' } }
    ]
  };

  const mockLayer2Output = {
    scene_vector: {
      scene_type: 'morning_commute',
      dimensions: { energy: 0.4, social: 0, focus: 0.5 },
      confidence: 0.8
    }
  };

  const mockContext = {
    speed: 60,
    passengerCount: 0,
    weather: 'sunny',
    hour: 7
  };

  try {
    const startTime = Date.now();
    const result = await layer3.process(mockLayer1Output, mockLayer2Output, mockContext);
    const duration = Date.now() - startTime;

    console.log(colorize(`✅ Layer 3 推理成功!`, 'green'));
    console.log(`   总耗时: ${duration}ms`);
    console.log(`   状态: ${result.status}`);
    console.log(`   来源: ${result.meta.source}`);
    
    if (result.scene_descriptor) {
      console.log(`   场景ID: ${result.scene_descriptor.scene_id}`);
      console.log(`   氛围: ${result.scene_descriptor.intent?.atmosphere}`);
      console.log(`   能量级别: ${result.scene_descriptor.intent?.energy_level}`);
      console.log(`   播报: ${result.scene_descriptor.announcement?.substring(0, 50)}...`);
    }

    testResults.push({
      test: 'Layer 3 集成测试',
      success: true,
      duration
    });

    return { success: true, result };
  } catch (error) {
    console.log(colorize(`❌ Layer 3 推理失败: ${error.message}`, 'red'));
    
    testResults.push({
      test: 'Layer 3 集成测试',
      success: false,
      error: error.message
    });

    return { success: false, error: error.message };
  }
}

function generateReport() {
  printSeparator('测试报告');

  const successCount = testResults.filter(r => r.success).length;
  const failCount = testResults.filter(r => !r.success).length;
  const totalDuration = testResults.reduce((sum, r) => sum + (r.duration || 0), 0);

  console.log(colorize('\n📊 测试统计', 'yellow'));
  console.log(`   总测试数: ${testResults.length}`);
  console.log(`   通过: ${colorize(successCount.toString(), 'green')}`);
  console.log(`   失败: ${colorize(failCount.toString(), failCount > 0 ? 'red' : 'green')}`);
  console.log(`   总耗时: ${totalDuration}ms`);

  console.log(colorize('\n📋 详细结果', 'yellow'));
  for (const result of testResults) {
    const status = result.success ? colorize('✅', 'green') : colorize('❌', 'red');
    const duration = result.duration ? ` (${result.duration}ms)` : '';
    const error = result.error ? ` - ${result.error}` : '';
    console.log(`   ${status} ${result.test}${duration}${error}`);
  }

  const reportPath = path.join(__dirname, '../test-reports/llm-test-report.json');
  const reportDir = path.dirname(reportPath);
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }
  
  fs.writeFileSync(reportPath, JSON.stringify({
    timestamp: new Date().toISOString(),
    summary: {
      total: testResults.length,
      passed: successCount,
      failed: failCount,
      totalDuration
    },
    results: testResults
  }, null, 2));

  console.log(colorize(`\n📄 测试报告已保存: ${reportPath}`, 'cyan'));
}

async function main() {
  console.log(colorize('\n🚀 阿里云百炼 LLM 接入测试', 'bright'));
  console.log(colorize('═'.repeat(60), 'cyan'));
  console.log(colorize(`\n📋 配置信息`, 'yellow'));
  console.log(`   API Key: ${API_KEY.substring(0, 10)}...`);
  console.log(`   Base URL: ${BASE_URL}`);
  console.log(`   测试模型: ${Object.values(MODELS).join(', ')}`);

  const args = process.argv.slice(2);

  if (args.includes('--basic')) {
    printSeparator('基础连接测试');
    for (const model of Object.values(MODELS)) {
      await testBasicConnection(model);
    }
  } else if (args.includes('--scene')) {
    printSeparator('场景推理测试');
    for (const model of [MODELS.FLASH, MODELS.PLUS, MODELS.QWEN_PLUS]) {
      await testSceneReasoning(model);
    }
  } else if (args.includes('--error')) {
    printSeparator('错误处理测试');
    await testErrorHandling(MODELS.QWEN_PLUS);
  } else if (args.includes('--compare')) {
    await testModelComparison();
  } else if (args.includes('--layer3')) {
    await testLayer3Integration();
  } else {
    await testModelComparison();
    await testLayer3Integration();
  }

  generateReport();
}

main().catch(console.error);

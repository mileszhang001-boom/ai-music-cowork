#!/usr/bin/env node
'use strict';

const {
  ColorThemes,
  SCENE_THEME_MAP,
  THEME_KEYWORD_MAP,
  mapSceneToLightingTheme,
  extractKeywordsFromScene,
  adjustColorByEnergy,
  adjustColorByValence,
  hexToHSL,
  hslToHex
} = require('../src/layers/effects/engines/lighting');

console.log('=== 场景化灯光主题测试 ===\n');

// 测试 1: 场景主题映射
console.log('1. 场景主题映射测试\n');
const testScenes = ['family_outing', 'sunset_drive', 'rainy_day', 'night_drive', 'road_trip'];
testScenes.forEach(scene => {
  const theme = mapSceneToLightingTheme(scene);
  const colors = ColorThemes[theme];
  console.log(`  ${scene} → ${theme}`);
  console.log(`    主色调: ${colors.primary}, 辅助色: ${colors.secondary}`);
});
console.log('');

// 测试 2: 关键词匹配
console.log('2. 关键词匹配测试\n');
const testDescriptions = [
  '海边度假，阳光沙滩',
  '夕阳西下，黄昏时分',
  '下雨天，阴沉的天空',
  '森林中的公路旅行'
];
testDescriptions.forEach(desc => {
  const keywords = extractKeywordsFromScene(desc);
  const theme = mapSceneToLightingTheme(null, keywords);
  console.log(`  "${desc}"`);
  console.log(`    提取关键词: [${keywords.join(', ')}]`);
  console.log(`    匹配主题: ${theme}\n`);
});

// 测试 3: 动态颜色调整
console.log('3. 动态颜色调整测试\n');
const baseColor = '#00CED1'; // ocean 主题主色调
console.log(`  基础颜色: ${baseColor}`);

const highEnergy = adjustColorByEnergy(baseColor, 0.8);
const lowEnergy = adjustColorByEnergy(baseColor, 0.2);
console.log(`  高能量 (0.8): ${highEnergy}`);
console.log(`  低能量 (0.2): ${lowEnergy}`);

const highValence = adjustColorByValence(baseColor, 0.8);
const lowValence = adjustColorByValence(baseColor, 0.2);
console.log(`  高情绪 (0.8): ${highValence} (暖色调偏移)`);
console.log(`  低情绪 (0.2): ${lowValence} (冷色调偏移)\n`);

// 测试 4: 所有主题颜色展示
console.log('4. 所有主题颜色展示\n');
Object.entries(ColorThemes).forEach(([name, colors]) => {
  console.log(`  ${name.padEnd(12)} 主: ${colors.primary}  辅: ${colors.secondary}`);
});
console.log('');

// 测试 5: 验证场景模板映射
console.log('5. 场景模板映射验证\n');
const sceneTemplateMap = {
  'family_outing': 'ocean',
  'sunset_drive': 'sunset',
  'rainy_day': 'rainy',
  'night_drive': 'citynight',
  'road_trip': 'forest',
  'morning_commute': 'spring',
  'party': 'party',
  'meditation': 'meditation'
};

let passed = 0;
let total = Object.keys(sceneTemplateMap).length;
Object.entries(sceneTemplateMap).forEach(([scene, expected]) => {
  const actual = mapSceneToLightingTheme(scene);
  const match = actual === expected;
  if (match) passed++;
  console.log(`  ${scene.padEnd(18)} ${match ? '✅' : '❌'} 期望: ${expected}, 实际: ${actual}`);
});
console.log(`\n  通过率: ${passed}/${total} (${Math.round(passed/total*100)}%)\n`);

console.log('=== 测试完成 ===');

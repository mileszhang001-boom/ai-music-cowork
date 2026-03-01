#!/usr/bin/env node
'use strict';

// HEX 转 ANSI 256 色
function hexToAnsi(hex) {
  if (!hex || hex === 'N/A') return '\x1b[0m';
  
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  
  const ansi256 = 16 + Math.round(r / 255 * 5) * 36 + Math.round(g / 255 * 5) * 6 + Math.round(b / 255 * 5);
  
  return `\x1b[38;5;${ansi256}m`;
}

// 颜色名称映射
const COLOR_NAMES = {
  '#FF0000': '红色', '#FF5722': '深橙', '#FF6B00': '活力橙', '#FF9800': '琥珀',
  '#FFD600': '明亮黄', '#FFEB3B': '黄色', '#FFFF00': '纯黄', '#FFD700': '金黄',
  '#00FF00': '纯绿', '#009688': '青绿', '#00BCD4': '青色', '#4CAF50': '绿色',
  '#0000FF': '纯蓝', '#2196F3': '蓝色', '#1A237E': '深蓝', '#0D1B2A': '深蓝黑',
  '#1B263B': '深蓝灰', '#3F51B5': '靛蓝', '#2C5F7C': '蓝灰',
  '#E91E63': '粉红', '#9C27B0': '紫色', '#4A148C': '深紫', '#FCE4EC': '浅粉',
  '#1E3A5F': '海军蓝', '#87CEEB': '天蓝', '#FFA500': '橙色',
  '#00CED1': '深青色', '#20B2AA': '浅海洋绿', '#FF6B35': '夕阳橙',
  '#4682B4': '钢蓝', '#708090': '板岩灰', '#228B22': '森林绿', '#2E8B57': '海洋绿',
  '#191970': '午夜蓝', '#90EE90': '浅绿', '#FFB6C1': '浅粉红',
  '#00BFFF': '深天蓝', '#FFDAB9': '桃色', '#D2691E': '巧克力色', '#FF8C00': '深橙',
  '#B0E0E6': '粉蓝', '#FFFFFF': '雪白', '#FF69B4': '热粉红',
  '#FF1493': '深粉', '#00FF7F': '春绿', '#9370DB': '中紫色', '#E6E6FA': '薰衣草'
};

function getColorName(hex) {
  if (!hex || hex === 'N/A') return '未知';
  return COLOR_NAMES[hex.toUpperCase()] || '自定义色';
}

// 测试颜色显示
const testColors = [
  '#00CED1',  // ocean 主色调
  '#20B2AA',  // ocean 辅助色
  '#FF6B35',  // sunset 主色调
  '#FFD700',  // sunset 辅助色
  '#4682B4',  // rainy 主色调
  '#708090',  // rainy 辅助色
  '#228B22',  // forest 主色调
  '#2E8B57',  // forest 辅助色
  '#191970',  // citynight 主色调
  '#FFD700',  // citynight 辅助色
];

console.log('=== 场景化灯光颜色显示测试 ===\n');

const scenes = [
  { name: 'ocean (海边度假)', primary: '#00CED1', secondary: '#20B2AA' },
  { name: 'sunset (夕阳驾驶)', primary: '#FF6B35', secondary: '#FFD700' },
  { name: 'rainy (雨天驾驶)', primary: '#4682B4', secondary: '#708090' },
  { name: 'forest (森林公路)', primary: '#228B22', secondary: '#2E8B57' },
  { name: 'citynight (城市夜景)', primary: '#191970', secondary: '#FFD700' },
];

scenes.forEach(scene => {
  console.log(`\n${scene.name}:`);
  console.log(`  主色调: ${hexToAnsi(scene.primary)}●\x1b[0m ${scene.primary} (${getColorName(scene.primary)})`);
  console.log(`  辅助色: ${hexToAnsi(scene.secondary)}●\x1b[0m ${scene.secondary} (${getColorName(scene.secondary)})`);
});

console.log('\n=== 测试完成 ===');

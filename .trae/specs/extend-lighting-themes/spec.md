# 扩展氛围灯主题色值 Spec

## Why
场景模板中定义了 7 种 color_theme（cool、night、energetic、romantic、warm、focus、calm），但 Lighting Engine 只实现了 5 种主题。缺少冷色系主题（cool、night、focus）和活力主题（energetic），导致部分场景无法正确匹配灯光颜色。

## What Changes
- 扩展 `ColorThemes` 对象，新增 4 个主题色值
- 新增冷色系主题：cool（冷色）、night（夜晚）、focus（专注）
- 新增活力主题：energetic（活力）
- 确保所有场景模板的 color_theme 都有对应的色值定义

## Impact
- Affected code: `src/layers/effects/engines/lighting/index.js`
- Affected scenes: morning_commute, night_drive, road_trip, fatigue_alert, highway_cruise, sunset_drive, workout, meditation, focus_work, rainy_night, late_night_solo

## ADDED Requirements

### Requirement: 新增冷色系主题
Lighting Engine SHALL 提供以下冷色系主题：

| 主题 | 主色调 | 辅助色 | 适用场景 |
|------|--------|--------|----------|
| cool | #00BCD4 (青色) | #3F51B5 (靛蓝) | 早晨通勤、高速巡航 |
| night | #0D1B2A (深蓝黑) | #1B263B (深蓝灰) | 深夜驾驶、雨夜驾驶、深夜独行 |
| focus | #2196F3 (蓝色) | #009688 (青绿) | 车内办公、专注模式 |

### Requirement: 新增活力主题
Lighting Engine SHALL 提供 energetic 主题：
- 主色调: #FF6B00 (活力橙)
- 辅助色: #FFD600 (明亮黄)
- 适用场景: 朋友出游、派对、疲劳提醒、运动健身

### Requirement: 主题匹配验证
所有场景模板的 color_theme 必须在 ColorThemes 中有对应定义，fallback 为 calm 主题。

## MODIFIED Requirements

### Requirement: ColorThemes 对象扩展
```javascript
const ColorThemes = {
  // 现有主题
  calm: { primary: '#1A237E', secondary: '#4A148C' },
  warm: { primary: '#FF5722', secondary: '#FF9800' },
  vibrant: { primary: '#E91E63', secondary: '#9C27B0' },
  alert: { primary: '#F44336', secondary: '#FFEB3B' },
  romantic: { primary: '#E91E63', secondary: '#FCE4EC' },
  
  // 新增主题
  cool: { primary: '#00BCD4', secondary: '#3F51B5' },
  night: { primary: '#0D1B2A', secondary: '#1B263B' },
  focus: { primary: '#2196F3', secondary: '#009688' },
  energetic: { primary: '#FF6B00', secondary: '#FFD600' }
};
```

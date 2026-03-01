# 场景化灯光主题优化 Spec

## Why
当前 Layer3 的氛围灯推荐经常是 warm，缺乏场景特色。需要根据场景模板生成更多的灯光主题，并优化取色逻辑，参考场景实体的实际颜色（如海边是蓝绿色，夕阳是橙色），使灯光效果更加贴合场景氛围。

## What Changes
- 根据场景实体特征定义专属灯光主题
- 优化取色逻辑，从场景关键词提取颜色特征
- 扩展 ColorThemes 支持更多场景化主题
- 添加场景到灯光主题的智能映射

## Impact
- Affected code: 
  - `src/layers/effects/engines/lighting/index.js`
  - `templates/preset_templates.json`
  - `src/layers/effects/orchestrator/index.js`

## ADDED Requirements

### Requirement: 场景化灯光主题定义
系统 SHALL 根据场景实体特征定义专属灯光主题：

| 场景类型 | 主题名称 | 主色调 | 辅助色 | 颜色来源 |
|----------|----------|--------|--------|----------|
| 海边度假 | ocean | #00CED1 (深青色) | #20B2AA (浅海洋绿) | 海洋、沙滩 |
| 夕阳驾驶 | sunset | #FF6B35 (夕阳橙) | #FFD700 (金黄) | 夕阳、黄昏 |
| 雨天驾驶 | rainy | #4682B4 (钢蓝) | #708090 (板岩灰) | 雨天、乌云 |
| 森林公路 | forest | #228B22 (森林绿) | #2E8B57 (海洋绿) | 森林、树木 |
| 城市夜景 | citynight | #191970 (午夜蓝) | #FFD700 (霓虹金) | 城市灯光、霓虹 |
| 春日出行 | spring | #90EE90 (浅绿) | #FFB6C1 (浅粉) | 春天、花朵 |
| 夏日海滩 | summer | #00BFFF (深天蓝) | #FFDAB9 (桃色) | 夏天、阳光 |
| 秋日公路 | autumn | #D2691E (巧克力色) | #FF8C00 (深橙) | 秋叶、落叶 |
| 冬日雪景 | winter | #B0E0E6 (粉蓝) | #FFFFFF (雪白) | 雪花、冰晶 |
| 浪漫约会 | romantic | #FF69B4 (热粉红) | #FFB6C1 (浅粉红) | 玫瑰、爱心 |
| 派对狂欢 | party | #FF1493 (深粉) | #00FF7F (春绿) | 霓虹、活力 |
| 冥想放松 | meditation | #9370DB (中紫色) | #E6E6FA (薰衣草) | 宁静、禅意 |

### Requirement: 场景关键词颜色映射
系统 SHALL 从场景关键词提取颜色特征：

#### Scenario: 自然场景颜色提取
- **WHEN** 场景包含 "海边"、"沙滩"、"海洋" 关键词
- **THEN** 使用蓝绿色系（#00CED1, #20B2AA）

#### Scenario: 时间场景颜色提取
- **WHEN** 场景包含 "夕阳"、"黄昏"、"日落" 关键词
- **THEN** 使用橙黄色系（#FF6B35, #FFD700）

#### Scenario: 天气场景颜色提取
- **WHEN** 场景包含 "雨"、"雪"、"雾" 关键词
- **THEN** 使用对应的天气颜色系

### Requirement: 智能主题映射
系统 SHALL 智能映射场景到灯光主题：

```javascript
function mapSceneToLightingTheme(sceneType, sceneKeywords) {
  // 1. 精确匹配场景类型
  if (SCENE_THEME_MAP[sceneType]) {
    return SCENE_THEME_MAP[sceneType];
  }
  
  // 2. 关键词匹配
  const keywords = extractKeywords(sceneKeywords);
  for (const [theme, config] of Object.entries(THEME_KEYWORD_MAP)) {
    if (keywords.some(k => config.keywords.includes(k))) {
      return theme;
    }
  }
  
  // 3. 默认主题
  return 'calm';
}
```

### Requirement: 动态颜色生成
系统 SHALL 支持动态颜色生成：

#### Scenario: 基于音乐特征调整颜色
- **WHEN** 音乐能量值 > 0.7
- **THEN** 提高颜色饱和度和亮度
- **WHEN** 音乐能量值 < 0.3
- **THEN** 降低颜色饱和度，使用柔和色调

#### Scenario: 基于情绪值调整颜色
- **WHEN** valence > 0.7 (积极情绪)
- **THEN** 使用暖色调
- **WHEN** valence < 0.3 (消极情绪)
- **THEN** 使用冷色调

## MODIFIED Requirements

### Requirement: ColorThemes 对象扩展
扩展 ColorThemes 支持场景化主题：

```javascript
const ColorThemes = {
  // 基础主题
  calm: { primary: '#1A237E', secondary: '#4A148C' },
  warm: { primary: '#FF5722', secondary: '#FF9800' },
  
  // 场景化主题
  ocean: { primary: '#00CED1', secondary: '#20B2AA' },
  sunset: { primary: '#FF6B35', secondary: '#FFD700' },
  rainy: { primary: '#4682B4', secondary: '#708090' },
  forest: { primary: '#228B22', secondary: '#2E8B57' },
  citynight: { primary: '#191970', secondary: '#FFD700' },
  spring: { primary: '#90EE90', secondary: '#FFB6C1' },
  summer: { primary: '#00BFFF', secondary: '#FFDAB9' },
  autumn: { primary: '#D2691E', secondary: '#FF8C00' },
  winter: { primary: '#B0E0E6', secondary: '#FFFFFF' },
  meditation: { primary: '#9370DB', secondary: '#E6E6FA' }
};
```

### Requirement: 场景模板更新
更新 `preset_templates.json` 中的 color_theme 字段：

| 场景 | 原主题 | 新主题 |
|------|--------|--------|
| family_outing (海边度假) | warm | ocean |
| sunset_drive (夕阳驾驶) | warm | sunset |
| rainy_day (雨天驾驶) | calm | rainy |
| night_drive (城市夜景) | night | citynight |
| road_trip (森林公路) | energetic | forest |

## REMOVED Requirements
无

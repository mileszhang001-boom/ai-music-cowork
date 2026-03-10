# Tasks

- [x] Task 1: 定义场景化灯光主题
  - [x] SubTask 1.1: 创建 SCENE_THEME_MAP 映射表
  - [x] SubTask 1.2: 定义 12 个场景化主题的色值
  - [x] SubTask 1.3: 创建 THEME_KEYWORD_MAP 关键词映射

- [x] Task 2: 扩展 ColorThemes 对象
  - [x] SubTask 2.1: 在 lighting/index.js 中添加新主题
  - [x] SubTask 2.2: 添加主题验证函数
  - [x] SubTask 2.3: 更新主题文档

- [x] Task 3: 实现场景到主题的智能映射
  - [x] SubTask 3.1: 实现 mapSceneToLightingTheme 函数
  - [x] SubTask 3.2: 实现关键词提取和匹配
  - [x] SubTask 3.3: 添加默认主题回退逻辑

- [x] Task 4: 实现动态颜色调整
  - [x] SubTask 4.1: 基于音乐能量调整颜色饱和度
  - [x] SubTask 4.2: 基于情绪值调整色调
  - [x] SubTask 4.3: 添加颜色过渡动画支持

- [x] Task 5: 更新场景模板
  - [x] SubTask 5.1: 更新 preset_templates.json 的 color_theme
  - [x] SubTask 5.2: 验证所有场景的主题映射
  - [x] SubTask 5.3: 更新模板文档

- [x] Task 6: 测试和验证
  - [x] SubTask 6.1: 测试海边度假场景（ocean 主题）
  - [x] SubTask 6.2: 测试夕阳驾驶场景（sunset 主题）
  - [x] SubTask 6.3: 测试雨天驾驶场景（rainy 主题）
  - [x] SubTask 6.4: 验证动态颜色调整效果

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 2]
- [Task 5] depends on [Task 2, Task 3]
- [Task 6] depends on [Task 4, Task 5]

'use strict';

/**
 * @fileoverview Prompt Builder - 场景推理 Prompt 构建器
 * @description 构建 LLM 场景推理所需的 System Prompt 和 User Prompt
 */

const SystemPrompts = {
  SCENE_REASONING: `你是一个车载座舱 AI 娱乐助手，专门负责根据当前驾驶场景为用户推荐最合适的音乐、灯光和音效配置。

你的核心能力：
1. 理解驾驶场景：时间、天气、乘客数量、车辆状态、用户情绪等
2. 推荐音乐：根据场景选择合适的音乐风格、节奏和能量级别
3. 配置灯光：根据场景调整车内氛围灯的颜色、亮度和动效
4. 调整音效：根据场景优化音效预设，提升听觉体验

输出要求：
- 必须输出符合 Scene Descriptor V2.0 规范的 JSON 格式
- 所有数值字段必须在合理范围内
- 必须包含 intent、hints、announcement 三个核心字段
- 不要输出任何额外的解释文字，只输出 JSON`,

  SCENE_REASONING_SIMPLE: `你是车载音乐推荐助手。根据当前场景推荐合适的音乐、灯光和音效配置。
输出要求：只输出 JSON 格式的 Scene Descriptor，不要其他文字。`
};

const UserPromptTemplates = {
  BASIC: `当前场景信息：
{{context}}

请根据以上场景信息，生成合适的 Scene Descriptor JSON。`,

  DETAILED: `当前场景详细信息：

【时间信息】
- 当前时间：{{timeContext}}
- 时段：{{timePeriod}}

【环境信息】
- 天气：{{weather}}
- 温度：{{temperature}}

【车辆信息】
- 车速：{{speed}} km/h
- 乘客数量：{{passengerCount}}
- 是否有儿童：{{hasChildren}}

【用户状态】
- 疲劳程度：{{fatigueLevel}}
- 心率：{{heartRate}}
- 情绪状态：{{mood}}

【用户偏好】
{{userPreferences}}

【历史记忆】
{{recentMemories}}

请根据以上信息，生成一个完整的 Scene Descriptor JSON，包含以下字段：
1. intent: 场景意图，包含 mood (valence, arousal)、energy_level、atmosphere、constraints
2. hints: 引擎提示，包含 music、lighting、audio 配置
3. announcement: TTS 播报文案`,

  WITH_TEMPLATE: `当前场景信息：
{{context}}

已匹配的快通道模板：
{{matchedTemplate}}

请基于快通道模板进行优化，生成更精细的 Scene Descriptor JSON。
可以保持或调整模板中的配置，但需要保持场景一致性。`
};

class PromptBuilder {
  constructor(config = {}) {
    this.config = {
      defaultSystemPrompt: SystemPrompts.SCENE_REASONING,
      defaultUserTemplate: UserPromptTemplates.DETAILED,
      ...config
    };
  }

  buildSystemPrompt(type = 'default') {
    if (type === 'simple') {
      return SystemPrompts.SCENE_REASONING_SIMPLE;
    }
    return this.config.defaultSystemPrompt;
  }

  buildUserPrompt(context, options = {}) {
    const template = options.template || this.config.defaultUserTemplate;
    
    const contextData = this.extractContextData(context);
    
    let prompt = template;
    
    for (const [key, value] of Object.entries(contextData)) {
      const placeholder = `{{${key}}}`;
      prompt = prompt.replace(new RegExp(placeholder, 'g'), String(value));
    }

    prompt = prompt.replace(/\{\{[^}]+\}\}/g, '未提供');
    
    return prompt;
  }

  extractContextData(context) {
    const data = {
      context: this.formatContextSummary(context),
      timeContext: context.time || '未知',
      timePeriod: this.getTimePeriod(context.hour),
      weather: context.weather || '未知',
      temperature: context.temperature || '未知',
      speed: context.speed || 0,
      passengerCount: context.passengerCount || 0,
      hasChildren: context.hasChildren ? '是' : '否',
      fatigueLevel: context.fatigueLevel || 0,
      heartRate: context.heartRate || 72,
      mood: context.mood || '平静',
      userPreferences: this.formatUserPreferences(context.userPreferences),
      recentMemories: this.formatRecentMemories(context.recentMemories),
      matchedTemplate: context.matchedTemplate ? JSON.stringify(context.matchedTemplate, null, 2) : '无'
    };

    return data;
  }

  formatContextSummary(context) {
    const parts = [];
    
    if (context.hour !== undefined) {
      parts.push(`时间: ${context.hour}:00`);
    }
    if (context.weather) {
      parts.push(`天气: ${context.weather}`);
    }
    if (context.speed !== undefined) {
      parts.push(`车速: ${context.speed} km/h`);
    }
    if (context.passengerCount !== undefined) {
      parts.push(`乘客: ${context.passengerCount}人`);
    }
    if (context.fatigueLevel !== undefined) {
      parts.push(`疲劳度: ${(context.fatigueLevel * 100).toFixed(0)}%`);
    }
    
    return parts.length > 0 ? parts.join(', ') : '无场景信息';
  }

  getTimePeriod(hour) {
    if (hour === undefined) return '未知';
    if (hour >= 5 && hour < 9) return '早晨';
    if (hour >= 9 && hour < 12) return '上午';
    if (hour >= 12 && hour < 14) return '中午';
    if (hour >= 14 && hour < 18) return '下午';
    if (hour >= 18 && hour < 22) return '傍晚';
    return '深夜';
  }

  formatUserPreferences(preferences) {
    if (!preferences) return '无用户偏好记录';
    
    const parts = [];
    if (preferences.favoriteGenres) {
      parts.push(`喜欢的音乐风格: ${preferences.favoriteGenres.join(', ')}`);
    }
    if (preferences.preferredTempo) {
      parts.push(`偏好节奏: ${preferences.preferredTempo}`);
    }
    if (preferences.volumePreference) {
      parts.push(`音量偏好: ${preferences.volumePreference}`);
    }
    
    return parts.length > 0 ? parts.join('\n') : '无特殊偏好';
  }

  formatRecentMemories(memories) {
    if (!memories || memories.length === 0) return '无近期记忆';
    
    return memories.slice(0, 5).map((m, i) => 
      `${i + 1}. ${m.sceneType || m.scene_type} (${m.timestamp || '未知时间'})`
    ).join('\n');
  }

  buildMessages(context, options = {}) {
    const systemPrompt = this.buildSystemPrompt(options.systemPromptType);
    const userPrompt = this.buildUserPrompt(context, options);
    
    return [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt }
    ];
  }

  buildMessagesWithHistory(context, history = [], options = {}) {
    const messages = [];
    
    messages.push({ 
      role: 'system', 
      content: this.buildSystemPrompt(options.systemPromptType) 
    });
    
    for (const msg of history) {
      messages.push({
        role: msg.role,
        content: msg.content
      });
    }
    
    messages.push({ 
      role: 'user', 
      content: this.buildUserPrompt(context, options) 
    });
    
    return messages;
  }
}

const promptBuilder = new PromptBuilder();

module.exports = {
  PromptBuilder,
  promptBuilder,
  SystemPrompts,
  UserPromptTemplates
};

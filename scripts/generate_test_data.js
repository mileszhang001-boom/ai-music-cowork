const fs = require('fs');
const path = require('path');

const { templateLibrary } = require('../src/layers/semantic/templates/library');

function generateTestFile() {
  const templates = templateLibrary.getAllTemplates();
  
  const presets = templates.map(t => ({
    template_id: t.template_id,
    scene_descriptor: {
      version: '2.0',
      scene_id: `preset_${t.scene_type}_${t.template_id.toLowerCase()}`,
      scene_type: t.scene_type,
      scene_name: t.name,
      scene_narrative: t.description,
      intent: {
        mood: t.intent.mood,
        energy_level: t.intent.energy_level,
        atmosphere: t.intent.atmosphere,
        social_context: inferSocialContext(t),
        constraints: inferConstraints(t)
      },
      hints: t.hints,
      announcement: t.announcement_templates ? t.announcement_templates[0] : null,
      meta: {
        source: 'template',
        template_id: t.template_id,
        category: t.category
      }
    },
    input_signals: generateInputSignals(t)
  }));

  const generated = generateVariants(templates);

  const output = {
    metadata: {
      version: '2.0',
      created_at: new Date().toISOString().split('T')[0],
      description: '场景模板测试数据集 - 100个预置模板 + 200条变体',
      total_presets: presets.length,
      total_generated: generated.length
    },
    presets,
    generated
  };

  const outputPath = path.join(__dirname, '../tests/data/scene_templates_test.json');
  fs.writeFileSync(outputPath, JSON.stringify(output, null, 2), 'utf8');
  
  console.log('测试数据生成完成:');
  console.log('  - 预置模板:', presets.length);
  console.log('  - 生成变体:', generated.length);
  console.log('  - 输出路径:', outputPath);
}

function inferSocialContext(t) {
  if (t.triggers?.has_children) return 'family';
  if (t.triggers?.has_seniors) return 'family';
  if (t.triggers?.min_passengers >= 3) return 'group';
  if (t.triggers?.min_passengers >= 2) return 'couple';
  if (t.scene_type?.includes('family')) return 'family';
  if (t.scene_type?.includes('couple')) return 'couple';
  if (t.scene_type?.includes('party')) return 'group';
  if (t.scene_type?.includes('friends')) return 'group';
  return 'solo';
}

function inferConstraints(t) {
  const constraints = {};
  if (t.triggers?.has_children || t.scene_type?.includes('family')) {
    constraints.content_rating = 'G';
  }
  if (t.scene_type?.includes('fatigue')) {
    constraints.max_volume_db = 80;
  }
  return Object.keys(constraints).length > 0 ? constraints : undefined;
}

function generateInputSignals(t) {
  const signals = {
    environment: {},
    vehicle: {},
    internal_camera: { passengers: { children: 0, adults: 1, seniors: 0 } }
  };

  if (t.triggers?.time_range) {
    const [start, end] = t.triggers.time_range;
    signals.environment.time_of_day = (start + (end - start) / 2) / 24;
  }

  if (t.triggers?.weather) {
    signals.environment.weather = t.triggers.weather[0];
  }

  if (t.triggers?.has_children) {
    signals.internal_camera.passengers = { children: 1, adults: 2, seniors: 0 };
  }

  if (t.triggers?.has_seniors) {
    signals.internal_camera.passengers = { children: 0, adults: 1, seniors: 1 };
  }

  if (t.triggers?.min_passengers) {
    signals.vehicle.passenger_count = t.triggers.min_passengers;
  }

  if (t.triggers?.mood) {
    signals.internal_camera.mood = t.triggers.mood;
  }

  if (t.triggers?.scene_description) {
    signals.external_camera = { scene_description: t.triggers.scene_description };
  }

  return signals;
}

function generateVariants(templates) {
  const variants = [];
  let id = 1;

  const timeVariants = [
    { name: '早高峰变体', time: 0.32, weather: 'clear', mood: 'neutral' },
    { name: '午间变体', time: 0.5, weather: 'sunny', mood: 'calm' },
    { name: '傍晚变体', time: 0.75, weather: 'clear', mood: 'happy' },
    { name: '深夜变体', time: 0.05, weather: 'clear', mood: 'calm' },
    { name: '凌晨变体', time: 0.02, weather: 'clear', mood: 'tired' }
  ];

  const weatherVariants = [
    { name: '晴天', weather: 'sunny', mood: 'happy' },
    { name: '雨天', weather: 'rain', mood: 'calm' },
    { name: '雪天', weather: 'snow', mood: 'neutral' },
    { name: '雾天', weather: 'fog', mood: 'neutral' },
    { name: '多云', weather: 'cloudy', mood: 'neutral' }
  ];

  const socialVariants = [
    { name: '独自', passengers: { children: 0, adults: 1, seniors: 0 }, context: 'solo' },
    { name: '情侣', passengers: { children: 0, adults: 2, seniors: 0 }, context: 'couple' },
    { name: '家庭', passengers: { children: 1, adults: 2, seniors: 0 }, context: 'family' },
    { name: '朋友', passengers: { children: 0, adults: 3, seniors: 0 }, context: 'group' },
    { name: '三代', passengers: { children: 1, adults: 2, seniors: 1 }, context: 'family' }
  ];

  const baseTemplates = templates.slice(0, 20);

  for (const t of baseTemplates) {
    for (const v of timeVariants) {
      variants.push(createVariant(id++, t, v.name + '_' + t.name, {
        environment: { time_of_day: v.time, weather: v.weather },
        internal_camera: { mood: v.mood }
      }));
    }
  }

  for (const t of baseTemplates) {
    for (const v of weatherVariants) {
      variants.push(createVariant(id++, t, v.name + '_' + t.name, {
        environment: { weather: v.weather },
        internal_camera: { mood: v.mood }
      }));
    }
  }

  for (const t of baseTemplates) {
    for (const v of socialVariants) {
      variants.push(createVariant(id++, t, v.name + '_' + t.name, {
        internal_camera: { passengers: v.passengers }
      }));
    }
  }

  return variants.slice(0, 250);
}

function createVariant(id, template, name, overrides) {
  const signals = {
    environment: overrides.environment || {},
    vehicle: overrides.vehicle || {},
    internal_camera: {
      passengers: overrides.internal_camera?.passengers || { children: 0, adults: 1, seniors: 0 },
      mood: overrides.internal_camera?.mood || 'neutral'
    }
  };

  return {
    scene_id: `gen_${String(id).padStart(3, '0')}`,
    scene_type: template.scene_type,
    scene_name: name,
    scene_narrative: `${name}场景`,
    intent: {
      mood: template.intent.mood,
      energy_level: template.intent.energy_level,
      atmosphere: template.intent.atmosphere
    },
    hints: template.hints,
    input_signals: signals
  };
}

generateTestFile();

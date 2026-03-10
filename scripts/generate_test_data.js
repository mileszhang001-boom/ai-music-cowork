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
      description: '场景模板测试数据集 - 精简版',
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
  
  const size = fs.statSync(outputPath).size / 1024;
  console.log('  - 文件大小:', size.toFixed(1), 'KB');
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

  const variantConfigs = [
    { suffix: '雨天版', env: { weather: 'rain' }, mood: 'calm' },
    { suffix: '晴天版', env: { weather: 'sunny' }, mood: 'happy' },
    { suffix: '夜间版', env: { time_of_day: 0.9 }, mood: 'calm' },
    { suffix: '家庭版', passengers: { children: 1, adults: 2, seniors: 0 } },
    { suffix: '情侣版', passengers: { children: 0, adults: 2, seniors: 0 } }
  ];

  const categoryTemplates = {};
  templates.forEach(t => {
    if (!categoryTemplates[t.category]) {
      categoryTemplates[t.category] = [];
    }
    if (categoryTemplates[t.category].length < 5) {
      categoryTemplates[t.category].push(t);
    }
  });

  for (const [category, categoryTpls] of Object.entries(categoryTemplates)) {
    for (const t of categoryTpls) {
      const config = variantConfigs[Math.floor(Math.random() * variantConfigs.length)];
      
      const signals = {
        environment: { ...config.env },
        vehicle: {},
        internal_camera: {
          passengers: config.passengers || { children: 0, adults: 1, seniors: 0 },
          mood: config.mood || 'neutral'
        }
      };

      variants.push({
        scene_id: `gen_${String(id++).padStart(3, '0')}`,
        scene_type: t.scene_type,
        scene_name: `${t.name}_${config.suffix}`,
        scene_narrative: `${t.name}的${config.suffix}变体`,
        intent: {
          mood: t.intent.mood,
          energy_level: t.intent.energy_level,
          atmosphere: t.intent.atmosphere
        },
        hints: t.hints,
        input_signals: signals
      });
    }
  }

  return variants;
}

generateTestFile();

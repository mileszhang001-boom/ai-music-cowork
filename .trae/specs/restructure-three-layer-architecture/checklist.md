# Checklist

## Phase 1: 目录结构

- [x] `src/layers/` 目录结构创建完成
- [x] `src/layers/perception/` (Layer 1) 目录创建完成
- [x] `src/layers/semantic/` (Layer 2) 目录创建完成
- [x] `src/layers/effects/` (Layer 3) 目录创建完成
- [x] `src/shared/` 共享模块目录创建完成
- [x] `src/shared/schema/` JSON Schema 目录创建完成
- [x] `scripts/layer1/` 目录创建完成
- [x] `scripts/layer2/` 目录创建完成
- [x] `scripts/layer3/` 目录创建完成
- [x] `scripts/integration/` 目录创建完成
- [x] `scripts/tools/` 目录创建完成
- [x] `tests/layer1/` 目录创建完成
- [x] `tests/layer2/` 目录创建完成
- [x] `tests/layer3/` 目录创建完成
- [x] `tests/integration/` 目录创建完成

## Phase 2: Layer 1 物理感知层

- [x] Layer 1 核心代码迁移完成
- [x] 传感器适配器模块 `sensors/` 创建完成
- [x] 信号标准化逻辑 `normalizer.js` 重构完成
- [x] 输出验证器 `validator.js` 创建完成
- [x] 类型定义 `types.js` 创建完成
- [x] 统一入口 `index.js` 创建完成
- [x] `signals.schema.json` 创建完成
- [x] `scripts/layer1/validate_signals.js` 创建完成
- [x] `scripts/layer1/test_sensors.js` 创建完成
- [x] `scripts/layer1/mock_data.js` 创建完成
- [x] `scripts/layer1/demo.js` 创建完成
- [x] Layer 1 可独立运行验证

## Phase 3: Layer 2 语义推理层

- [x] Layer 2 核心代码迁移完成
- [x] 场景识别模块 `recognizer/` 迁移完成
- [x] 查询路由模块 `router/` 迁移完成
- [x] LLM 推理模块 `reasoner/` 迁移完成
- [x] 模板库模块 `templates/` 迁移完成
- [x] 输出验证器 `validator.js` 创建完成
- [x] 类型定义 `types.js` 创建完成
- [x] 统一入口 `index.js` 创建完成
- [x] `scripts/layer2/validate_descriptor.js` 创建完成
- [x] `scripts/layer2/test_scenes.js` 创建完成
- [x] `scripts/layer2/compare_models.js` 创建完成
- [x] `scripts/layer2/mock_input.js` 创建完成
- [x] `scripts/layer2/demo.js` 创建完成
- [x] Layer 2 可独立运行验证

## Phase 4: Layer 3 效果生成层

- [x] Layer 3 核心代码迁移完成
- [x] 编排协调模块 `orchestrator/` 迁移完成
- [x] 引擎模块 `engines/` 迁移完成
- [x] 输出验证器 `validator.js` 创建完成
- [x] 类型定义 `types.js` 创建完成
- [x] 统一入口 `index.js` 创建完成
- [x] `effects.schema.json` 创建完成
- [x] `scripts/layer3/validate_effects.js` 创建完成
- [x] `scripts/layer3/test_engines.js` 创建完成
- [x] `scripts/layer3/simulate_output.js` 创建完成
- [x] `scripts/layer3/mock_input.js` 创建完成
- [x] `scripts/layer3/demo.js` 创建完成
- [x] Layer 3 可独立运行验证

## Phase 5: 共享模块

- [x] `src/shared/eventBus/` 迁移完成
- [x] `src/shared/feedback/` 迁移完成
- [x] `src/shared/types.js` 迁移完成
- [x] `src/shared/utils/` 创建完成

## Phase 6: 集成与兼容

- [x] `tests/integration/layer1_to_layer2.test.js` 创建完成
- [x] `tests/integration/layer2_to_layer3.test.js` 创建完成
- [x] `tests/integration/e2e.test.js` 创建完成
- [x] `scripts/integration/e2e_test.js` 创建完成
- [x] `scripts/integration/demo_full.js` 创建完成
- [x] `src/index.js` 向后兼容更新完成
- [x] `scripts/tools/schema_validator.js` 创建完成
- [x] `scripts/tools/data_generator.js` 创建完成
- [x] `package.json` 脚本命令添加完成

## Phase 7: 验收

- [x] `npm run test:layer1` 通过
- [x] `npm run test:layer2` 通过
- [x] `npm run test:layer3` 通过
- [x] `npm run test:integration` 通过
- [x] `npm run demo:layer1` 正常运行
- [x] `npm run demo:layer2` 正常运行
- [x] `npm run demo:layer3` 正常运行
- [x] 所有 JSON Schema 验证通过
- [x] 旧代码清理完成，无残留
- [x] 所有 import 路径更新正确

## 团队独立开发验收

- [x] Layer 1 团队可使用 Mock 数据独立测试
- [x] Layer 2 团队可使用 Layer 1 Mock 输出独立测试
- [x] Layer 3 团队可使用 Layer 2 Mock 输出独立测试
- [x] 各层输出格式符合 JSON Schema 定义
- [x] 各层验证工具可独立运行

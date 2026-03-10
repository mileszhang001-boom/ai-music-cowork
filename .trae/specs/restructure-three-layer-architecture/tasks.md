# Tasks

## Phase 1: 目录结构重组

- [x] Task 1: 创建新的目录结构
  - [x] SubTask 1.1: 创建 `src/layers/` 目录结构
  - [x] SubTask 1.2: 创建 `src/layers/perception/` (Layer 1)
  - [x] SubTask 1.3: 创建 `src/layers/semantic/` (Layer 2)
  - [x] SubTask 1.4: 创建 `src/layers/effects/` (Layer 3)
  - [x] SubTask 1.5: 创建 `src/shared/` 共享模块目录
  - [x] SubTask 1.6: 创建 `src/shared/schema/` JSON Schema 目录

- [x] Task 2: 创建脚本目录结构
  - [x] SubTask 2.1: 创建 `scripts/layer1/` 目录
  - [x] SubTask 2.2: 创建 `scripts/layer2/` 目录
  - [x] SubTask 2.3: 创建 `scripts/layer3/` 目录
  - [x] SubTask 2.4: 创建 `scripts/integration/` 目录
  - [x] SubTask 2.5: 创建 `scripts/tools/` 目录

- [x] Task 3: 创建测试目录结构
  - [x] SubTask 3.1: 创建 `tests/layer1/` 目录
  - [x] SubTask 3.2: 创建 `tests/layer2/` 目录
  - [x] SubTask 3.3: 创建 `tests/layer3/` 目录
  - [x] SubTask 3.4: 创建 `tests/integration/` 目录

## Phase 2: Layer 1 物理感知层重构

- [x] Task 4: 迁移 Layer 1 核心代码
  - [x] SubTask 4.1: 迁移 `src/core/layer1/` 到 `src/layers/perception/`
  - [x] SubTask 4.2: 创建传感器适配器模块 `sensors/`
  - [x] SubTask 4.3: 重构信号标准化逻辑 `normalizer.js`
  - [x] SubTask 4.4: 创建输出验证器 `validator.js`
  - [x] SubTask 4.5: 创建类型定义 `types.js`
  - [x] SubTask 4.6: 创建统一入口 `index.js`

- [x] Task 5: 创建 Layer 1 JSON Schema
  - [x] SubTask 5.1: 创建 `signals.schema.json` 标准化信号 Schema
  - [x] SubTask 5.2: 添加 Schema 验证逻辑

- [x] Task 6: 创建 Layer 1 验证工具
  - [x] SubTask 6.1: 创建 `scripts/layer1/validate_signals.js`
  - [x] SubTask 6.2: 创建 `scripts/layer1/test_sensors.js`
  - [x] SubTask 6.3: 创建 `scripts/layer1/mock_data.js`
  - [x] SubTask 6.4: 创建 `scripts/layer1/demo.js`

## Phase 3: Layer 2 语义推理层重构

- [x] Task 7: 迁移 Layer 2 核心代码
  - [x] SubTask 7.1: 迁移 `src/core/layer2/` 到 `src/layers/semantic/recognizer/`
  - [x] SubTask 7.2: 迁移 `src/core/queryRouter/` 到 `src/layers/semantic/router/`
  - [x] SubTask 7.3: 迁移 `src/core/llm/` 到 `src/layers/semantic/reasoner/`
  - [x] SubTask 7.4: 迁移 `src/core/layer3/` 到 `src/layers/semantic/reasoner/`
  - [x] SubTask 7.5: 迁移 `src/core/templateLibrary/` 到 `src/layers/semantic/templates/`
  - [x] SubTask 7.6: 创建输出验证器 `validator.js`
  - [x] SubTask 7.7: 创建类型定义 `types.js`
  - [x] SubTask 7.8: 创建统一入口 `index.js`

- [x] Task 8: 创建 Layer 2 验证工具
  - [x] SubTask 8.1: 创建 `scripts/layer2/validate_descriptor.js`
  - [x] SubTask 8.2: 创建 `scripts/layer2/test_scenes.js`
  - [x] SubTask 8.3: 创建 `scripts/layer2/compare_models.js`
  - [x] SubTask 8.4: 创建 `scripts/layer2/mock_input.js`
  - [x] SubTask 8.5: 创建 `scripts/layer2/demo.js`

## Phase 4: Layer 3 效果生成层重构

- [x] Task 9: 迁移 Layer 3 核心代码
  - [x] SubTask 9.1: 迁移 `src/core/orchestrator/` 到 `src/layers/effects/orchestrator/`
  - [x] SubTask 9.2: 迁移 `src/engines/` 到 `src/layers/effects/engines/`
  - [x] SubTask 9.3: 创建输出验证器 `validator.js`
  - [x] SubTask 9.4: 创建类型定义 `types.js`
  - [x] SubTask 9.5: 创建统一入口 `index.js`

- [x] Task 10: 创建 Layer 3 JSON Schema
  - [x] SubTask 10.1: 创建 `effects.schema.json` 效果指令 Schema
  - [x] SubTask 10.2: 添加 Schema 验证逻辑

- [x] Task 11: 创建 Layer 3 验证工具
  - [x] SubTask 11.1: 创建 `scripts/layer3/validate_effects.js`
  - [x] SubTask 11.2: 创建 `scripts/layer3/test_engines.js`
  - [x] SubTask 11.3: 创建 `scripts/layer3/simulate_output.js`
  - [x] SubTask 11.4: 创建 `scripts/layer3/mock_input.js`
  - [x] SubTask 11.5: 创建 `scripts/layer3/demo.js`

## Phase 5: 共享模块迁移

- [x] Task 12: 迁移共享模块
  - [x] SubTask 12.1: 迁移 `src/core/eventBus/` 到 `src/shared/eventBus/`
  - [x] SubTask 12.2: 迁移 `src/core/feedback/` 到 `src/shared/feedback/`
  - [x] SubTask 12.3: 迁移 `src/core/types.js` 到 `src/shared/types.js`
  - [x] SubTask 12.4: 创建 `src/shared/utils/` 工具函数目录

## Phase 6: 集成与兼容

- [x] Task 13: 创建集成测试
  - [x] SubTask 13.1: 创建 `tests/integration/layer1_to_layer2.test.js`
  - [x] SubTask 13.2: 创建 `tests/integration/layer2_to_layer3.test.js`
  - [x] SubTask 13.3: 创建 `tests/integration/e2e.test.js`

- [x] Task 14: 创建集成脚本
  - [x] SubTask 14.1: 创建 `scripts/integration/e2e_test.js`
  - [x] SubTask 14.2: 创建 `scripts/integration/demo_full.js`

- [x] Task 15: 更新统一入口
  - [x] SubTask 15.1: 更新 `src/index.js` 保持向后兼容
  - [x] SubTask 15.2: 创建 `scripts/tools/schema_validator.js`
  - [x] SubTask 15.3: 创建 `scripts/tools/data_generator.js`

- [x] Task 16: 更新 package.json
  - [x] SubTask 16.1: 添加 `test:layer1` 脚本
  - [x] SubTask 16.2: 添加 `test:layer2` 脚本
  - [x] SubTask 16.3: 添加 `test:layer3` 脚本
  - [x] SubTask 16.4: 添加 `test:integration` 脚本
  - [x] SubTask 16.5: 添加 `demo:layer1`, `demo:layer2`, `demo:layer3` 脚本

## Phase 7: 清理与文档

- [x] Task 17: 清理旧代码
  - [x] SubTask 17.1: 删除旧的 `src/core/` 目录 (保留迁移后的引用)
  - [x] SubTask 17.2: 删除旧的 `src/engines/` 目录
  - [x] SubTask 17.3: 更新所有 import 路径

- [x] Task 18: 验证与测试
  - [x] SubTask 18.1: 运行所有单元测试确保通过
  - [x] SubTask 18.2: 运行所有集成测试确保通过
  - [x] SubTask 18.3: 运行演示脚本确保正常工作

# Task Dependencies

```
Phase 1 (Task 1-3) - 无依赖，可并行
    ↓
Phase 2 (Task 4-6) - 依赖 Phase 1
    ↓
Phase 3 (Task 7-8) - 依赖 Phase 1, 可与 Phase 2 并行
    ↓
Phase 4 (Task 9-11) - 依赖 Phase 1, 可与 Phase 2-3 并行
    ↓
Phase 5 (Task 12) - 依赖 Phase 2-4
    ↓
Phase 6 (Task 13-16) - 依赖 Phase 5
    ↓
Phase 7 (Task 17-18) - 依赖 Phase 6
```

# 并行执行建议

以下任务组可以并行执行：
- **Group A**: Task 1, Task 2, Task 3 (目录创建)
- **Group B**: Task 4-6 (Layer 1), Task 7-8 (Layer 2), Task 9-11 (Layer 3) - 三个团队可并行
- **Group C**: Task 13, Task 14, Task 15 (集成相关)

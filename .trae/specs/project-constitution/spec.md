# Project Constitution Spec

## Why
本项目是一个复杂的多模块车载 AI 娱乐融合 Agent。为了确保高效协作、快速开发（今天跑通链路，明天调优），并在各团队（感知、语义、生成、执行）之间建立清晰的边界，需要一份基础的“宪法”文档（constitution.md）来统一所有参与者的共识。

## What Changes
- 在项目根目录创建 `constitution.md` 文件。
- 明确核心产品目标：构建一个车载场景下，基于场景驱动的娱乐 Agent。
- 引用 `spec.md` 作为项目架构的依据，引用 `tech.md` 作为硬件平台约束的依据。
- 确立解耦的合作模式，强烈强调基于 Mock 数据的开发与调试能力。
- 设定时间线与开发哲学：“先研发-再测试”，确保今天跑通全链路，明天集中调优。

## Impact
- Affected specs: 无（这是一个基础性的元文档）。
- Affected code: `constitution.md`（新文件）。

## ADDED Requirements
### Requirement: Project Constitution Document
系统必须提供一份 `constitution.md` 文件，作为团队协作规则、项目目标和开发时间线的唯一事实来源。

#### Scenario: 开发者协作与对齐
- **WHEN** 团队成员开始开发特定模块（如语义层或执行层）时
- **THEN** 他们可以通过阅读 `constitution.md` 明确自己模块的边界、如何 Mock 外部依赖数据，以及整体项目的交付时间要求。
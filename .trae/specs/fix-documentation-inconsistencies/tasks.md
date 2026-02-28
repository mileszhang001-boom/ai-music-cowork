# Tasks

- [ ] Task 1: Fix Markdown formatting and duplications in `spec-all.md`
  - [ ] SubTask 1.1: Remove unclosed code blocks and stray text (e.g., "服务端异常，请稍后重试 (-1)").
  - [ ] SubTask 1.2: Remove the duplicated Scene Descriptor definition.
- [ ] Task 2: Consolidate Risk Management and Testing Scenarios
  - [ ] SubTask 2.1: Merge risk management sections from `plan.md` and `spec-all.md` into a single source of truth (e.g., keep in `plan.md` and reference in `spec-all.md`, or vice versa).
  - [ ] SubTask 2.2: Update `testing.md` to reference the scene walkthroughs in `spec-all.md` instead of duplicating the content.
- [ ] Task 3: Create Formal JSON Schema Definitions
  - [ ] SubTask 3.1: Extract JSON structures from `spec-all.md` and create formal `.schema.json` files (e.g., `schemas/scene_descriptor.schema.json`, `schemas/ack.schema.json`).
- [ ] Task 4: Align Existing Mock Data with Specifications
  - [ ] SubTask 4.1: Update existing Scene Descriptor mock JSON files to match the V2.0 structure defined in `spec-all.md`.
  - [ ] SubTask 4.2: Update the existing ACK mock JSON file to match the structure defined in `spec-all.md`.
- [ ] Task 5: Expand Mock Data Coverage
  - [ ] SubTask 5.1: Create additional Scene Descriptor mock files to reach 10+ scenarios.
  - [ ] SubTask 5.2: Create mock data for Feedback Reports, Layer 1 outputs, and Layer 2 outputs.
  - [ ] SubTask 5.3: Create mock data for user personas and memory data.
- [ ] Task 6: Initialize Project Scaffolding and Tools
  - [ ] SubTask 6.1: Create a basic project directory structure and build configuration file.
  - [ ] SubTask 6.2: Implement the Event Bus Mocker tool script for beat injection.

# Task Dependencies
- Task 4 depends on Task 1 (to ensure the spec is correct before aligning mock data).
- Task 5 depends on Task 3 and Task 4.

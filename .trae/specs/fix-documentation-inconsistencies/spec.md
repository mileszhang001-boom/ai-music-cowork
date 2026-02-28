# Fix Documentation Inconsistencies Spec

## Why
A recent review (`check.md`) identified several critical inconsistencies, formatting errors, and missing components across the project's documentation and mock data. These issues include mismatched JSON structures between mock data and specifications, duplicated content, missing JSON schemas, and incomplete mock data coverage. Resolving these is essential to ensure a "single source of truth" and provide a solid foundation for subsequent development.

## What Changes
- **Fix Mock Data Inconsistencies**: Update `mock_data` JSON files (Scene Descriptors and ACKs) to strictly align with the structures defined in `spec-all.md`.
- **Fix Markdown Errors**: Correct unclosed code blocks, duplicate headers, and stray text in `spec-all.md`.
- **Remove Duplications**: 
  - Remove the duplicated Scene Descriptor definition in `spec-all.md`.
  - Consolidate risk management into a single document (e.g., `plan.md` or `spec-all.md`) and remove the overlap.
  - Refactor `testing.md` to reference `spec-all.md` scene walkthroughs instead of duplicating them.
- **Add Missing Artifacts**:
  - Create formal JSON Schema definition files (`.schema.json`) for core data structures.
  - Expand `mock_data` to cover 10+ scenarios, feedback reports, Layer 1/2 outputs, and user personas.
  - Initialize a basic project engineering structure (scaffolding).
  - Create the Event Bus Mocker tool script.

## Impact
- Affected specs: `spec-all.md`, `plan.md`, `testing.md`, `tasks.md`
- Affected code: `mock_data/*.json`, new JSON schema files, new project scaffolding, Event Bus Mocker tool.

## ADDED Requirements
### Requirement: Formal JSON Schemas
The system SHALL provide formal `.schema.json` files for Scene Descriptors, ACK messages, and other core data structures to enable automated validation.

### Requirement: Comprehensive Mock Data
The system SHALL provide a comprehensive suite of mock data covering at least 10 typical scenarios, user personas, and intermediate layer outputs.

### Requirement: Project Scaffolding
The system SHALL include a basic project directory structure and build configuration (e.g., `build.gradle` or `package.json` depending on the target stack) to support upcoming development.

## MODIFIED Requirements
### Requirement: Single Source of Truth for Risks and Scenarios
Risk management and end-to-end scenario walkthroughs SHALL be defined in exactly one place to prevent divergence.

## REMOVED Requirements
### Requirement: Duplicated Content
**Reason**: Duplicated risk lists and scenario walkthroughs increase maintenance overhead and risk of inconsistency.
**Migration**: Keep the primary definition in `spec-all.md` (or `plan.md` for risks, as appropriate) and use references in other documents.

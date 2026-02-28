'use strict';

/**
 * @fileoverview LLM Module Index
 * @description 导出 LLM 相关模块
 */

const { LLMClient, llmClient, Models, Regions, RegionBaseUrls, DefaultConfig } = require('./llmClient');
const { PromptBuilder, SystemPrompts, UserPromptTemplates } = require('./promptBuilder');

module.exports = {
  LLMClient,
  llmClient,
  Models,
  Regions,
  RegionBaseUrls,
  DefaultConfig,
  PromptBuilder,
  SystemPrompts,
  UserPromptTemplates
};

# Tasks
- [ ] Task 1: 修改 `src/layers/effects/engines/content/index.js`，引入 `llmClient`。
- [ ] Task 2: 在 `ContentEngine` 中新增 `_generatePlaylistWithLLM` 私有方法，构建 Prompt 并调用 LLM 生成推荐歌曲。
- [ ] Task 3: 更新 `curatePlaylist` 方法，优先使用 LLM 生成播放列表，若失败则降级使用原有的 Mock 逻辑。
- [ ] Task 4: 编写或更新测试脚本，验证 Content Engine 的 LLM 推荐功能是否按预期工作。

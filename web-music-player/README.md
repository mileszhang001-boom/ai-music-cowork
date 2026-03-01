# 🎵 场景音乐播放器

一个基于 Vue 3 + 网易云音乐 API 的场景化音乐推荐播放器。

## 功能特性

- 🎭 **场景选择** - 6种预设场景（深夜独处、清晨唤醒、工作专注、运动健身、浪漫约会、派对狂欢）
- 🎵 **智能推荐** - 根据场景自动推荐相关歌曲
- 🎨 **精美界面** - 现代化深色主题设计
- 🎧 **完整播放器** - 播放/暂停、上下首切换、进度控制、音量调节
- 📱 **响应式设计** - 支持桌面端和移动端

## 技术栈

- **前端框架**: Vue 3 + Vite
- **样式**: Tailwind CSS
- **音频播放**: Howler.js
- **音乐API**: NeteaseCloudMusicApi

## 快速开始

### 1. 启动网易云音乐API服务

```bash
cd web-music-player/api-server
npm install  # 首次运行需要安装依赖
npm start
```

API服务将在 http://localhost:3000 运行

### 2. 启动前端应用

打开新终端：

```bash
cd web-music-player
npm install  # 首次运行需要安装依赖
npm run dev
```

前端应用将在 http://localhost:5173 运行

### 3. 访问应用

在浏览器中打开 http://localhost:5173

## 使用说明

1. **选择场景** - 在首页选择一个符合当前心情的场景
2. **浏览歌曲** - 系统会自动推荐该场景相关的歌曲
3. **播放音乐** - 点击任意歌曲开始播放
4. **控制播放** - 使用底部播放器控制播放、调节音量

## 项目结构

```
web-music-player/
├── api-server/          # 网易云音乐API服务
├── src/
│   ├── components/      # Vue组件
│   │   ├── SceneSelector.vue  # 场景选择器
│   │   ├── SongCard.vue       # 歌曲卡片
│   │   ├── SongList.vue       # 歌曲列表
│   │   └── Player.vue         # 播放器
│   ├── services/        # 服务层
│   │   ├── neteaseApi.js      # 网易云API封装
│   │   └── recommendEngine.js # 推荐引擎
│   ├── hooks/           # 自定义Hooks
│   │   └── usePlayer.js       # 播放器逻辑
│   ├── App.vue          # 主应用
│   ├── main.js          # 入口文件
│   └── style.css        # 全局样式
├── package.json
├── vite.config.js
└── README.md
```

## 注意事项

- 需要先启动 API 服务，再启动前端应用
- 部分歌曲可能因版权限制无法播放
- 建议使用现代浏览器（Chrome、Firefox、Safari）

## License

MIT

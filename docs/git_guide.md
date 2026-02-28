# Git 协作指南

## 仓库信息

- **GitHub 仓库**: https://github.com/mileszhang001-boom/ai-music-cowork.git
- **默认分支**: `main`

---

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/mileszhang001-boom/ai-music-cowork.git
cd ai-music-cowork
```

### 2. 安装依赖

```bash
npm install
```

### 3. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填入你的 API Key
```

---

## 分支策略

### 分支命名规范

```
main                    # 主分支，稳定版本
├── develop            # 开发分支
├── feature/xxx        # 功能分支
├── fix/xxx            # 修复分支
├── docs/xxx           # 文档分支
└── layer1/xxx         # Layer 1 相关
├── layer2/xxx         # Layer 2 相关
└── layer3/xxx         # Layer 3 相关
```

### 分支命名示例

```bash
# Layer 1 团队
feature/layer1-add-gps-sensor
fix/layer1-confidence-calculation

# Layer 2 团队
feature/layer2-new-template
fix/layer2-scene-recognition

# Layer 3 团队
feature/layer3-new-engine
fix/layer3-orchestrator

# 文档更新
docs/update-layer1-guide
```

---

## 工作流程

### 1. 创建功能分支

```bash
# 确保在最新代码基础上开发
git checkout main
git pull origin main

# 创建新分支
git checkout -b feature/your-feature-name
```

### 2. 开发与提交

```bash
# 查看修改
git status

# 添加文件
git add .

# 提交 (使用规范的提交信息)
git commit -m "feat(layer1): 添加 GPS 传感器支持"
```

### 3. 推送分支

```bash
git push origin feature/your-feature-name
```

### 4. 创建 Pull Request

1. 访问 GitHub 仓库页面
2. 点击 "New Pull Request"
3. 选择 `feature/your-feature-name` → `main`
4. 填写 PR 描述，关联相关 Issue
5. 等待代码审查

---

## 提交信息规范

### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式 (不影响功能) |
| `refactor` | 重构 |
| `test` | 测试相关 |
| `chore` | 构建/工具相关 |

### Scope 范围

| Scope | 说明 |
|-------|------|
| `layer1` | 物理感知层 |
| `layer2` | 语义推理层 |
| `layer3` | 效果生成层 |
| `core` | 核心模块 |
| `docs` | 文档 |
| `config` | 配置 |

### 示例

```bash
# 新功能
git commit -m "feat(layer1): 添加 GPS 传感器适配器"

# Bug 修复
git commit -m "fix(layer2): 修复场景识别置信度计算错误"

# 文档更新
git commit -m "docs(layer3): 更新引擎接口文档"

# 重构
git commit -m "refactor(core): 优化 Event Bus 实现"
```

---

## 常用命令

### 日常操作

```bash
# 查看状态
git status

# 查看提交历史
git log --oneline -10

# 查看远程仓库
git remote -v

# 拉取最新代码
git pull origin main

# 推送分支
git push origin <branch-name>
```

### 分支操作

```bash
# 查看所有分支
git branch -a

# 切换分支
git checkout <branch-name>

# 创建并切换
git checkout -b <new-branch>

# 删除本地分支
git branch -d <branch-name>

# 删除远程分支
git push origin --delete <branch-name>
```

### 撤销操作

```bash
# 撤销工作区修改
git checkout -- <file>

# 撤销暂存
git reset HEAD <file>

# 撤销最近一次提交 (保留修改)
git reset --soft HEAD~1

# 撤销最近一次提交 (丢弃修改)
git reset --hard HEAD~1
```

### 合并操作

```bash
# 合并指定分支到当前分支
git merge <branch-name>

# 变基 (保持提交历史整洁)
git rebase main
```

---

## 团队协作规范

### 代码审查 (Code Review)

1. **PR 必须经过审查才能合并**
2. 审查者检查:
   - 代码质量
   - 测试覆盖
   - 文档更新
   - 是否影响其他层

### 合并要求

- [ ] 代码通过所有测试
- [ ] 更新相关文档
- [ ] 提交信息符合规范
- [ ] 至少一人审查通过

### 冲突解决

```bash
# 1. 拉取最新主分支
git checkout main
git pull origin main

# 2. 切换到功能分支并变基
git checkout feature/your-feature
git rebase main

# 3. 解决冲突
# 编辑冲突文件，解决冲突标记

# 4. 标记冲突已解决
git add <resolved-files>
git rebase --continue

# 5. 强制推送 (变基后需要)
git push origin feature/your-feature --force
```

---

## 各层协作要点

### Layer 1 团队

**修改影响范围**:
- 输出结构变化 → 需通知 Layer 2 团队
- 新增信号源 → 需更新 `schemas/layer1_output.schema.json`

**文档更新**:
- `docs/layer1_perception.md`
- `schemas/layer1_output.schema.json`

### Layer 2 团队

**修改影响范围**:
- Scene Descriptor 结构变化 → 需通知 Layer 3 团队
- 新增场景模板 → 需更新模板库文档

**文档更新**:
- `docs/layer2_semantic.md`
- `schemas/scene_descriptor.schema.json`

### Layer 3 团队

**修改影响范围**:
- 引擎接口变化 → 需更新引擎文档
- 新增引擎 → 需注册到 Orchestrator

**文档更新**:
- `docs/layer3_effects.md`
- `schemas/feedback_report.schema.json`

---

## 公共文档维护

以下文档为团队共同知识库，修改需经过团队讨论:

- `spec-all.md` - 技术规范文档
- `plan.md` - 研发实施计划
- `tasks.md` - 研发任务拆解
- `testing.md` - 测试与验收路径

### 更新流程

1. 创建文档更新分支: `docs/update-xxx`
2. 提交修改并创建 PR
3. 团队评审讨论
4. 合并到主分支

---

## 问题排查

### 常见问题

**Q: 推送被拒绝 (rejected)**

```bash
# 先拉取远程更新
git pull origin <branch-name> --rebase

# 解决冲突后推送
git push origin <branch-name>
```

**Q: 如何查看某文件的修改历史**

```bash
git log --follow -p <file-path>
```

**Q: 如何回退到某个历史版本**

```bash
# 查看历史提交
git log --oneline

# 回退到指定版本
git checkout <commit-hash>
```

---

## 相关链接

- [GitHub 仓库](https://github.com/mileszhang001-boom/ai-music-cowork)
- [贡献指南](CONTRIBUTING.md)
- [技术规范](spec-all.md)

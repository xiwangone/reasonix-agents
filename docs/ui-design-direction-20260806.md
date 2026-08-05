# Reasonix Agents — 移动端 UI 设计方向报告

> 日期：2026-08-06  
> 调研范围：AI 编码助手客户端（LobeChat / ChatGPT / Claude / Cursor）+ 工具类 App（Notion / Microsoft 365 Copilot / AI Orchestrator TUI）+ 2025-2026 移动端 AI 设计趋势  
> 项目上下文：基于 RikkaHub Agents 现有侧边栏布局 + 后端 `run` 模式需求

---

## 一、参考 App UI 调研清单

| 类别 | App | 核心 UI 特征 | 对 Reasonix 的参考价值 |
|------|-----|------------|---------------------|
| **同类** | **LobeChat** | 左侧会话列表 + 右侧对话区；顶部模型选择器；支持多模型切换、语音输入、文件上传、知识库；插件市场 | 侧边栏会话管理范式已成熟，多模型切换的交互模式 |
| **同类** | **ChatGPT** | 底部输入栏 + 全屏对话；正在从纯聊天转向「任务导向 UI」；支持 MCP 连接器；generative UI 初探 | 对话区是 base，但不能只有对话；需辅助任务面板 |
| **同类** | **Claude** | Claude Design — 代码原生 UI 生成；直接编辑迭代；设计系统可复用 | AI 生成结果不仅展示，还应允许用户继续操作 |
| **同类** | **Cursor (iOS)** | Agent 云端+本地双轨；Live Activities 锁屏进度；PR diff 审查；Remote Control；Push 通知完成状态 | ⭐ Agent 后台运行 + 实时进度 + 通知 = run 模式核心参考 |
| **同类** | **Claude Code / Cursor / Cline / Aider** | 进度面板模式（conversation + 独立任务状态面板）——行业共识 | ⭐ run 模式 UI 的最佳实践：对话+进度双栏 |
| **不同类** | **Notion** | adaptive grids、侧边栏+分割视图、foldable 适配 | 多面板布局、响应式设计的参考 |
| **不同类** | **Microsoft 365 Copilot** | Dynamic Action Button (DAB) — generative UI；hamburger menu；渐进式展示；voice-first | ⭐ 移动端 AI 的上下文感知入口设计 |
| **不同类** | **AI Orchestrator TUI** | Agent 面板（状态指示: RUNNING/DONE/ERROR）+ System Monitor（CPU/MEM/NET/TOKENS） | ⭐ Token 实时统计 + agent 运行状态的终端参考 |

---

## 二、"Run 模式"功能特性与 UI 需求映射

后端 `run` 模式包含以下关键阶段，每个阶段有明确 UI 需求：

| 阶段 | 后端行为 | 移动端 UI 需求 |
|------|---------|---------------|
| **1. 任务输入** | 用户提交任务描述 + 参数（模型选择、上下文、附件等） | 结构化输入区：文本框 + 参数卡片（模型/tag/选项）+ 附件入口 |
| **2. 参数选择** | 模型路由、temperature、max_tokens、思考链开关等 | 参数面板（可折叠），预设 + 自定义，选完即关闭 |
| **3. 实时进度流** | SSE 流式推送 tool call、中间步骤、文件操作、子 agent 状态 | ⭐ 进度面板（参考 Cursor/Claude Code）；分步骤卡片，带状态图标（⏳/✅/❌） |
| **4. 结果展示** | 最终输出（Markdown/代码/文件）+ 附件链接 | 渲染结果区（Markdown+代码高亮），支持复制/分享/保存 |
| **5. Token 统计** | input/output/total token + 缓存命中率 | ⭐ Token 仪表盘（参考 AI Orchestrator TUI），展示在结果底部或进度面板中 |

### 行业设计共识（来源：jacar.es Agent UX 研究，2026-07）

1. **Chat 是基础但非全部** — 对话适用于探索性任务，结构化任务需要专用控件
2. **后台 Agent 模式已胜出** — 超过 30 秒的任务应后台运行并展示进度（GitHub Copilot / Devin / Cursor 已收敛）
3. **结构化输入** — 当用户明确知道要什么时，提供按钮/滑块/选择器而非纯文本框
4. **进度面板** — conversation + 独立任务状态面板是行业共识
5. **人工控制环** — 不可逆操作前明确确认（diff 预览/审批卡片）

---

## 三、设计方向方案

### 方向 A：「会话中心 + 任务卡片」（渐进优化）

**核心理念**：在现有侧边栏 + 对话区架构基础上增量优化，用 Material 3 卡片承载 run 模式的各阶段。

```
┌──────────────────────────────────────┐
│  ≡ 侧边栏（左侧）                      │
│  ┌──────────────────────────────┐     │
│  │  👤 头像 / 配置                    │     │
│  │  ──────────────────────────── │     │
│  │  🔍 模型搜索 / 切换                │     │
│  │  ──────────────────────────── │     │
│  │  📅 今天                         │     │
│  │    ├ 会话 1                      │     │
│  │    ├ 会话 2 (run: 🟢进行中)        │     │
│  │    └ 会话 3                      │     │
│  │  📅 昨天                         │     │
│  │    └ 会话 4                      │     │
│  └──────────────────────────────┘     │
│                                       │
│  右侧主内容区：                         │
│  ┌──────────────────────────────┐     │
│  │  📋 Run 任务卡片                │     │
│  │  ┌─────────────────────────┐  │     │
│  │  │ 任务：分析代码库安全漏洞     │  │     │
│  │  │ 模型：claude-sonnet-4-5 │  │     │
│  │  │                         │  │     │
│  │  │  📂 读取文件...     ✅   │  │     │
│  │  │  🔍 Grep 搜索...    ⏳   │  │     │
│  │  │  📝 编辑文件...     ⏸    │  │     │
│  │  │                         │  │     │
│  │  │  📊 Token: ↓1,234 ↑567 │  │     │
│  │  └─────────────────────────┘  │     │
│  │                               │     │
│  │  ┌─────────────────────────┐  │     │
│  │  │  结果：2 个高危漏洞发现    │  │     │
│  │  │  (Markdown 渲染区)       │  │     │
│  │  └─────────────────────────┘  │     │
│  └──────────────────────────────┘     │
│                                       │
│  底部输入区（DAB 风格）：                 │
│  ┌──────────────────────────────┐     │
│  │  💬 输入任务描述...    📎 🎤 ▶️  │     │
│  └──────────────────────────────┘     │
└──────────────────────────────────────┘
```

**优点**：
- 改动最小，复用现有侧边栏架构
- 用户学习成本低
- 快速交付

**缺点**：
- 任务卡片和对话混排，长任务时信息密度高
- 缺乏专用进度视图

---

### 方向 B：「双面板工作室」（⭐ 推荐）

**核心理念**：借鉴 Cursor + Claude Code 的进度面板共识，将对话区和进度面板分离为独立 Tab/面板，中间可拖动分割。Material 3 NavigationBar 底部切换。

```
┌──────────────────────────────────────┐
│  顶部 Bar：Reasonix Agents           │
│  ┌──────────────────────────────┐     │
│  └──────────────────────────────┘     │
│                                       │
│  ┌────────────┬─────────────────┐     │
│  │  侧边栏     │  主视图          │     │
│  │ (折叠/展开) │                 │     │
│  │            │  [Tab: 对话]    │     │
│  │ 👤 头像     │  ┌───────────┐  │     │
│  │ ─────     │  │ 💬 会话     │  │     │
│  │ 🔍 搜索    │  │ (聊天内容)  │  │     │
│  │ ─────     │  │            │  │     │
│  │ 📅 今天    │  │ 用户：分析  │  │     │
│  │  ├ 会话1   │  │ 代码库...  │  │     │
│  │  ├ 会话2   │  └───────────┘  │     │
│  │  └ 会话3   │                 │     │
│  │ 📅 昨天    │  [Tab: 进度 🔴]│     │
│  │  └ 会话4   │  ┌───────────┐  │     │
│  │            │  │ 📋 Run #3  │  │     │
│  │            │  │ ⏳ 读取文件 │  │     │
│  │            │  │ ✅ Grep    │  │     │
│  │            │  │ ⏸ 等待审批 │  │     │
│  │            │  │            │  │     │
│  │            │  │ 📊 Token   │  │     │
│  │            │  │ ↓3,456    │  │     │
│  │            │  │ ↑1,890    │  │     │
│  │            │  │ 🕐 2m 34s │  │     │
│  │            │  └───────────┘  │     │
│  │            │                 │     │
│  │            │  [Tab: 结果]    │     │
│  │            │  ┌───────────┐  │     │
│  │            │  │ Markdown  │  │     │
│  │            │  │ 渲染内容    │  │     │
│  │            │  │ + 代码高亮  │  │     │
│  │            │  │ [复制][分享]│     │
│  │            │  └───────────┘  │     │
│  └────────────┴─────────────────┘     │
│                                       │
│  底部 NavigationBar（Material 3）：    │
│  ┌──────────────────────────────┐     │
│  │  💬 对话  │  ⚡ Run  │  📊 结果  │     │
│  └──────────────────────────────┘     │
│                                       │
│  悬浮 DAB（上下文感知快捷入口）：        │
│  ┌──┐                                 │
│  │✨│  → 新 Run / 继续 / 审批          │
│  └──┘                                 │
└──────────────────────────────────────┘
```

**Run 流程交互细节**：

1. **发起 Run**：从会话底部或 DAB 入口触发 → 弹出参数面板（Bottom Sheet）
   - 任务描述（必填，支持语音输入）
   - 模型选择（chip 组：Claude / GPT / Gemini / 自动）
   - 高级参数（可折叠）：temperature、max_tokens、思考链开关
   - 附件（📎 上传图片/文件作为上下文）
   - 「开始 Run」按钮

2. **进行中**：进度 Tab 自动激活，顶部显示 Live Activity（Android 通知栏）
   - 每步一张 Material 3 Card：`[图标] 步骤描述 [状态 chip]`
   - 状态 chip：⏳ running / ✅ done / ❌ error / ⏸ awaiting
   - 点击卡片展开详细日志
   - 底部 Token 仪表盘实时更新（↓input / ↑output / 🕐耗时）
   - 可中途「暂停」或「取消」

3. **完成后**：结果 Tab 激活 + Push 通知
   - Markdown/代码高亮渲染
   - 一键复制 / 分享 / 导出
   - 可继续对话追问

**侧边栏增强**：
- 左侧栏会话列表项增加运行状态指示器（🟢进行中 / ✅已完成 / ❌失败）
- 进行中的 Run 会话高亮 + 点击直接跳到进度面板

**优点**：
- 对话、进度、结果各司其职，信息架构清晰
- 与 Cursor / Claude Code 的行业共识一致
- Run 进度面板是独立空间，不受对话滚动干扰
- Material 3 原生支持 NavigationBar + NavigationRail（平板适配）

**缺点**：
- 改动较大，需要重构主视图为多 Tab 架构
- 底部 NavigationBar 会占用屏幕空间（≈80dp）

---

### 方向 C：「全屏沉浸 Run」（激进探索）

**核心理念**：Run 模式完全独立于对话，进入 Run 即为全屏沉浸式工作台，类似 Cursor Agent Mode。左侧文件树/步骤列表，右侧实时输出，顶部参数条。

```
进入 Run 模式（从侧边栏或 DAB 触发）：
┌──────────────────────────────────────┐
│  ◀ Run  #3          claude-sonnet   │  ← 顶部栏（可折叠）
│  🕐 3m 12s           ↓4.5k ↑2.1k    │
│──────────────────────────────────────│
│  ┌────────────┬─────────────────┐    │
│  │  步骤列表    │  实时输出区       │    │
│  │            │                 │    │
│  │ ✅ 初始化    │  分析代码库结构..  │    │
│  │ ✅ 读取配置  │                 │    │
│  │ ⏳ 搜索漏洞  │  (Markdown 流式   │    │
│  │ ⏸ 编辑文件  │   渲染 + 代码块)  │    │
│  │ ⏸ 保存     │                 │    │
│  │            │                 │    │
│  │            │  [代码高亮区]     │    │
│  └────────────┴─────────────────┘    │
│                                       │
│  底部操作栏：                           │
│  ┌──────────────────────────────┐     │
│  │  ⏸ 暂停  │  ⏹ 停止  │  ✅ 批准  │     │
│  └──────────────────────────────┘     │
└──────────────────────────────────────┘
```

**优点**：
- Run 是核心体验，独立沉浸无干扰
- 空间最大化利用，适合复杂长任务
- 与 Coding Agent 工具（Cursor/Claude Code）体验一致

**缺点**：
- 与其他功能（对话/设置）割裂感强
- 离开对话上下文，不利于追问式交互
- 开发成本最高

---

## 四、方案对比与推荐

| 维度 | A: 任务卡片 | B: 双面板工作室 ⭐ | C: 全屏沉浸 |
|------|-----------|-----------------|-----------|
| 开发工作量 | 低（1-2 周） | 中（3-5 周） | 高（6-8 周） |
| 与现有侧边栏兼容 | ✅ 完全兼容 | ✅ 兼容（侧边栏保留） | ⚠️ Run 时隐藏侧边栏 |
| 行业共识对齐 | 部分对齐 | ✅ 完全对齐 | ✅ 对齐 |
| 对话+进度并存 | ⚠️ 卡片堆叠 | ✅ Tab 分离 | ❌ 互斥 |
| Token 实时展示 | 基础 | ✅ 专用仪表盘 | ✅ 顶部常驻 |
| 平板适配潜力 | 基础 | ✅ NavigationRail | ✅ 分割视图 |
| 用户学习成本 | 低 | 中 | 高 |
| 差异化竞争力 | 低 | ✅ 高 | 中 |

### ⭐ 推荐：方向 B「双面板工作室」

**理由**：
1. **行业共识对齐**：Cursor、Claude Code、Cline、Aider 已收敛到 conversation + progress panel 模式，验证了该方向的可行性
2. **与现有架构兼容**：侧边栏保留，只需将右侧主视图改为 Material 3 NavigationBar 三 Tab 布局
3. **差异化竞争力**：目前移动端 AI 助手大多是纯聊天（ChatGPT/Claude App），带独立 Run 进度面板 + Token 仪表盘的很少，Reasonix 可以占据差异化位
4. **扩展性好**：后续可轻松增加 Tab（如文件管理、设置等），支持平板端 NavigationRail 适配

---

## 五、与 Web 版的差异化要点

| 维度 | Web 版 | 移动端（推荐方向 B） |
|------|--------|-------------------|
| **侧边栏** | 常驻左侧，全高 | 可折叠抽屉（ModalNavigationDrawer），滑出式 |
| **主视图** | 对话区全屏 | 三 Tab 底部导航（对话/Run/结果） |
| **Run 进度** | 侧边栏内或右侧浮动面板 | 独立 Tab 全屏卡片列表，拇指友好 |
| **参数面板** | 弹出 Dialog | Bottom Sheet（Material 3 ModalBottomSheet） |
| **快捷入口** | 快捷键 / 顶部按钮 | Dynamic Action Button（悬浮 FAB 或底部 DAB） |
| **Token 统计** | 底部/侧边栏常驻 | Run Tab 内专用仪表盘卡片 |
| **通知** | 浏览器通知 | Android Notification + Live Activity 锁屏进度 |
| **输入方式** | 键盘为主 | 键盘 + 语音 + 附件（相机/相册） |
| **审批交互** | 对话框/侧边栏按钮 | 通知栏快捷操作 + 应用内卡片 |
| **响应式** | 固定宽度 | Foldable 适配（展开分两栏，折叠单栏） |
| **设计语言** | 项目已有风格 | Material 3 (Material You) — 动态主题色、圆角卡片、柔和阴影 |

---

## 六、技术要求与实现路径（不在此范围，仅标注）

- Material 3 NavigationBar + ModalNavigationDrawer
- Compose `AnimatedVisibility` / `SharedTransition` 过渡动画
- SSE 流式数据驱动的进度卡片（`StateFlow<List<RunStep>>`）
- Token 仪表盘（`Text` + `LinearProgressIndicator`）
- Android Notification Channel + Foreground Service（长任务保活）
- Foldable 设备 `WindowSizeClass` 适配（展开: NavigationRail，折叠: NavigationBar）
- Bottom Sheet 参数面板（`ModalBottomSheet`）

---

## 七、参考资料

1. [Cursor for iOS — Build from anywhere](https://cursor.com/blog/ios-mobile-app) — Agent 云端/本地双轨、Live Activities、Remote Control
2. [Agent UX: design consensus (jacar.es, 2026-07)](https://jacar.es/en/ux-for-agents-first-design-consensus/) — 五大设计共识
3. [UI design for agents: principles (jacar.es, 2026-07)](https://jacar.es/en/ui-design-for-agents-principles-were-starting-to-understand/) — 进度面板/审批界面/执行浏览器模式
4. [Designing an Honest Mobile Run-State UI (dev.to)](https://dev.to/igorganapolsky/designing-an-honest-mobile-run-state-ui-for-ai-agents-3a9n) — 移动端 Run 状态 UI
5. [Microsoft 365 Copilot mobile redesign](https://microsoft.design/articles/the-new-microsoft-365-copilot-mobile-experience/) — DAB、hamburger menu、conversational paradigm
6. [Smashing Magazine — Designing Better AI Experiences (2026)](https://www.smashingmagazine.com/) — chat→task 转型、结构化输入模式
7. [AI Orchestrator TUI — Agent Panels + Token Monitor](https://github.com/Open-Makers/ai-coding-agents-orchestrator) — Agent 面板状态 + Token 统计终端参考
8. [LobeChat — Open-source ChatGPT UI](https://github.com/lobehub/lobe-chat) — 侧边栏+多模型+知识库

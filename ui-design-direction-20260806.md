# Reasonix Agents — 移动端 UI 设计方向报告

> 日期：2026-08-06  
> 调研方法：网络搜索（anysearch batch_search 覆盖 8 条查询）  
> 项目上下文：RikkaHub Agents 现有侧边栏布局（左上角头像→模型搜索→会话列表按日期分组）+ 后端 `run` 模式需求

---

## 一、参考 App UI 调研清单

| 类别 | App | 核心 UI 特征 | 对 Reasonix 的参考价值 |
|------|-----|------------|---------------------|
| 同类 | **LobeChat** | 左会话列表 + 右对话区；顶部模型选择器；多模型、语音、知识库、插件 | 侧边栏会话管理范式已成熟，模型切换交互 |
| 同类 | **ChatGPT** | 底部输入 + 全屏对话；正从纯聊天转向任务导向 UI；生成式 UI | 对话区是基础但不能只有对话，需任务面板 |
| 同类 | **Claude** | Claude Design 代码原生 UI 生成；直接编辑迭代 | AI 生成结果应允许用户继续编辑操作 |
| 同类 | **Cursor (iOS)** | Agent 云端+本地双轨；Live Activities 锁屏进度；PR diff 审查；Remote Control；Push 完成通知 | ⭐ Agent 后台运行 + 实时进度 + 通知 = run 模式核心 |
| 同类 | **Claude Code / Cline / Aider** | 进度面板共识 — conversation + 独立任务状态面板双栏 | ⭐ run 模式 UI 最佳实践 |
| 不同类 | **Microsoft 365 Copilot** | Dynamic Action Button (DAB)；hamburger menu；渐进式展示；voice-first | ⭐ 移动端 AI 上下文感知入口设计 |
| 不同类 | **AI Orchestrator TUI** | Agent 面板（RUNNING/DONE/ERROR）+ System Monitor（CPU/MEM/NET/TOKENS） | ⭐ Token 实时统计 + agent 状态 UI 参考 |

---

## 二、"Run 模式"功能特性与 UI 需求映射

| 阶段 | 后端行为 | 移动端 UI 需求 |
|------|---------|---------------|
| 1. 任务输入 | 用户提交任务描述 + 参数 | 结构化输入区：文本框 + 参数卡片 + 附件入口 |
| 2. 参数选择 | 模型路由、temperature 等 | 可折叠参数面板（Bottom Sheet），预设 + 自定义 |
| 3. 实时进度流 | SSE 推送 tool call、文件操作、子 agent | ⭐ 进度面板，分步骤卡片带状态图标（⏳/✅/❌） |
| 4. 结果展示 | Markdown / 代码 / 文件输出 | Markdown+代码高亮渲染，支持复制/分享/保存 |
| 5. Token 统计 | input/output/total + 缓存命中率 | ⭐ Token 仪表盘，展示在进度面板或结果区 |

**行业设计共识（来源：jacar.es Agent UX 研究，2026-07）：**

1. Chat 是基础但非全部 — 结构化任务需专用控件
2. 后台 Agent 模式已胜出 — 超过 30 秒的任务应后台运行展示进度
3. 结构化输入 — 用户明确意图时用按钮/选择器代替纯文本框
4. 进度面板 — conversation + 独立任务状态面板是行业共识
5. 人工控制环 — 不可逆操作前明确确认（审批卡片）

---

## 三、设计方向方案

### 方向 A：「会话中心 + 任务卡片」（渐进优化）

**理念**：在现有侧边栏 + 对话区基础上增量优化，Material 3 卡片承载 run 各阶段。

```
┌──────────────────────────────────────────┐
│  ≡ 侧边栏（左侧滑出）                      │
│  ┌──────────────────────────────┐         │
│  │  👤 头像 / 配置                         │
│  │  ────────────────────────────           │
│  │  🔍 模型搜索 / 切换                      │
│  │  ────────────────────────────           │
│  │  📅 今天                               │
│  │    ├ 会话 1                            │
│  │    ├ 会话 2 (run: 🟢进行中)              │
│  │    └ 会话 3                            │
│  │  📅 昨天                               │
│  │    └ 会话 4                            │
│  └──────────────────────────────┘         │
│                                           │
│  主内容区（对话 + 任务卡片混排）：            │
│  ┌──────────────────────────────┐         │
│  │  📋 Run 任务卡片                        │
│  │  ┌─────────────────────────┐           │
│  │  │ 任务：分析代码库安全漏洞  │           │
│  │  │ 模型：claude-sonnet-4-5  │           │
│  │  │ 📂 读取文件...    ✅    │           │
│  │  │ 🔍 Grep 搜索...   ⏳    │           │
│  │  │ 📝 编辑文件...    ⏸     │           │
│  │  │ 📊 Token: ↓1,234 ↑567  │           │
│  │  └─────────────────────────┘           │
│  │  ┌─────────────────────────┐           │
│  │  │ 💬 对话气泡区域          │           │
│  │  │ (聊天内容混排)          │           │
│  │  └─────────────────────────┘           │
│  └──────────────────────────────┘         │
│                                           │
│  底部输入区（DAB 风格）：                   │
│  ┌──────────────────────────────┐         │
│  │  💬 输入任务…       📎 🎤 ▶️  │         │
│  └──────────────────────────────┘         │
└──────────────────────────────────────────┘
```

| 维度 | 评价 |
|------|------|
| 开发量 | 低（1-2 周），复用现有侧边栏 |
| 优点 | 学习成本低，快速交付 |
| 缺点 | 卡片和对话混排，长任务信息密度高，缺乏专用进度视图 |

---

### 方向 B：「双面板工作室」⭐ 推荐

**理念**：借鉴 Cursor + Claude Code 行业共识 — conversation + progress panel 分离。Material 3 NavigationBar 三 Tab 底部切换，侧边栏保留为折叠抽屉。

```
┌──────────────────────────────────────────┐
│  顶部 Bar：Reasonix Agents               │
│──────────────────────────────────────────│
│  ┌────────────┬─────────────────┐        │
│  │  侧边栏     │  主视图区        │        │
│  │ (抽屉折叠)  │                 │        │
│  │            │  [Tab: 💬 对话] │        │
│  │ 👤 头像     │  ┌───────────┐  │        │
│  │ ─────     │  │ 会话内容    │  │        │
│  │ 🔍 模型    │  │ (标准聊天) │  │        │
│  │ ─────     │  └───────────┘  │        │
│  │ 📅 今天    │                 │        │
│  │  ├ 会话1   │  [Tab: ⚡ Run] │        │
│  │  ├ 会话2🔴 │  ┌───────────┐  │        │
│  │  └ 会话3   │  │ 📋 Run #3 │  │        │
│  │ 📅 昨天    │  │ ⏳ 读取... │  │        │
│  │  └ 会话4   │  │ ✅ Grep   │  │        │
│  │            │  │ ⏸ 审批    │  │        │
│  │            │  │           │  │        │
│  │            │  │ 📊 Token  │  │        │
│  │            │  │ ↓3,456   │  │        │
│  │            │  │ ↑1,890   │  │        │
│  │            │  │ 🕐 2m34s │  │        │
│  │            │  └───────────┘  │        │
│  │            │                 │        │
│  │            │  [Tab: 📊 结果]│        │
│  │            │  ┌───────────┐  │        │
│  │            │  │ Markdown  │  │        │
│  │            │  │ 渲染 +    │  │        │
│  │            │  │ 代码高亮   │  │        │
│  │            │  │ [复制][分享]│       │
│  │            │  └───────────┘  │        │
│  └────────────┴─────────────────┘        │
│                                           │
│  底部 NavigationBar（Material 3）：        │
│  ┌──────────────────────────────┐         │
│  │  💬 对话  │  ⚡ Run  │  📊 结果  │      │
│  └──────────────────────────────┘         │
│                                           │
│  悬浮 DAB：                               │
│  ┌──┐                                     │
│  │✨│ → 新 Run / 继续 / 审批               │
│  └──┘                                     │
└──────────────────────────────────────────┘
```

**Run 流程交互：**

1. **发起**：DAB 或底部入口 → Bottom Sheet 参数面板 → 任务描述 + 模型 chips + 高级参数折叠 + 附件
2. **进行中**：Run Tab 自动激活，步骤卡片（⏳/✅/❌/⏸），可展开详细日志，Token 仪表盘实时刷新
3. **完成**：结果 Tab 激活 + Push 通知，Markdown/代码渲染，一键复制分享，可继续追问

**侧边栏增强：** 会话列表项增加运行状态指示器，进行中会话高亮。

| 维度 | 评价 |
|------|------|
| 开发量 | 中（3-5 周） |
| 优点 | 对话/进度/结果各司其职；行业共识对齐；Run 独立空间不受对话干扰；Material 3 原生 NavigationRail（平板） |
| 缺点 | 需重构主视图为多 Tab 架构；底部 Bar 占 ≈80dp |

---

### 方向 C：「全屏沉浸 Run」（激进）

**理念**：Run 完全脱离对话，全屏沉浸工作台。左步骤列表，右实时输出，顶参数条。

```
┌──────────────────────────────────────────┐
│  ◀ Run #3    claude-sonnet   🕐 3m12s   │
│              ↓4.5k ↑2.1k                │
│──────────────────────────────────────────│
│  ┌──────────┬───────────────────┐        │
│  │ 步骤列表  │  实时输出区        │        │
│  │          │                   │        │
│  │ ✅ 初始化 │ 分析代码库结构...   │        │
│  │ ✅ 读配置 │                   │        │
│  │ ⏳ 扫描   │ (Markdown 流式)   │        │
│  │ ⏸ 编辑   │                   │        │
│  │ ⏸ 保存   │ [代码高亮区]       │        │
│  └──────────┴───────────────────┘        │
│                                           │
│  ┌──────────────────────────────┐         │
│  │  ⏸ 暂停  │  ⏹ 停止  │  ✅ 批准  │      │
│  └──────────────────────────────┘         │
└──────────────────────────────────────────┘
```

| 维度 | 评价 |
|------|------|
| 开发量 | 高（6-8 周） |
| 优点 | 沉浸无干扰，空间最大化，与 Cursor Agent Mode 一致 |
| 缺点 | 与对话/设置割裂，不利于追问，开发成本最高 |

---

## 四、方案对比与推荐

| 维度 | A: 任务卡片 | B: 双面板工作室 ⭐ | C: 全屏沉浸 |
|------|-----------|-----------------|-----------|
| 开发工作量 | 低（1-2 周） | 中（3-5 周） | 高（6-8 周） |
| 现有侧边栏兼容 | ✅ | ✅ | ⚠️ Run 时隐藏 |
| 行业共识对齐 | 部分 | ✅ 完全 | ✅ |
| 对话+进度并存 | ⚠️ 混排 | ✅ Tab 分离 | ❌ 互斥 |
| Token 实时展示 | 基础 | ✅ 专用仪表盘 | ✅ 顶部常驻 |
| 平板适配 | 基础 | ✅ NavigationRail | ✅ 分割视图 |
| 用户学习成本 | 低 | 中 | 高 |
| 差异化竞争力 | 低 | ✅ 高 | 中 |

### ⭐ 推荐方案 B「双面板工作室」

**四点理由：**

1. **行业共识对齐** — Cursor、Claude Code、Cline、Aider 已收敛到 conversation + progress panel，方案经过市场验证
2. **架构兼容** — 侧边栏保留为折叠抽屉，仅需将右侧主视图改为 NavigationBar 三 Tab
3. **差异化竞争** — 移动端 AI 助手基本是纯聊天（ChatGPT/Claude），带独立 Run 进度面板 + Token 仪表盘的极少
4. **扩展性** — 后续可加 Tab（文件/设置），平板用 NavigationRail 双栏，Foldable 设备自适应

---

## 五、与 Web 版的差异化要点

| 维度 | Web 版 | 移动端（方向 B） |
|------|--------|-------------------|
| 侧边栏 | 常驻左侧全高 | ModalNavigationDrawer 折叠抽屉，滑出式 |
| 主视图 | 对话全屏 | NavigationBar 三 Tab（对话/Run/结果） |
| Run 进度 | 侧边栏或浮动面板 | 独立 Tab 全屏卡片列表，拇指友好 |
| 参数面板 | 弹出 Dialog | ModalBottomSheet，底部自然触达 |
| 快捷入口 | 快捷键/顶部按钮 | DAB 悬浮按钮，上下文感知变换 |
| Token 统计 | 底部/侧边栏常驻 | Run Tab 内专用仪表盘卡片 |
| 通知 | 浏览器通知 | Android Notification + Live Activity 锁屏进度条 |
| 输入方式 | 键盘为主 | 键盘 + 语音（麦克风）+ 附件（相机/相册） |
| 审批交互 | Dialog/侧边栏按钮 | 通知栏快捷操作 + 应用内卡片确认 |
| 响应式 | 固定宽度 | WindowSizeClass：折叠单栏 / 展开双栏 |
| 设计语言 | 项目已有风格 | Material 3 (Material You) 动态主题色、圆角卡片 |

---

## 六、移动端独有功能（竞品差异点）

| 功能 | 说明 | 竞品现状 |
|------|------|---------|
| **Live Activity 进度** | Android 锁屏/通知栏显示 Run 实时进度条和步骤 | Cursor iOS 有此功能，Android 生态空白 |
| **DAB 悬浮入口** | 上下文感知的浮动按钮，根据当前状态变换为「新 Run」「继续」「审批」 | Microsoft Copilot 首创，同类 App 极少 |
| **Bottom Sheet 参数面板** | 上滑式参数配置，拇指自然触达，选完即走 | 多数 App 用全屏页面或 Dialog |
| **语音输入任务描述** | Run 模式支持语音描述复杂任务 | ChatGPT 支持基础语音，但非 Run 场景 |
| **Token 仪表盘** | 实时 ↓input / ↑output / 缓存率 / 耗时 | 仅终端工具（Claude Code / AI Orchestrator）有 |
| **Foreground Service** | 长 Run 任务通过 Foreground Service 保活，不被系统杀死 | 极少移动端 AI App 实现 |
| **Foldable 双栏** | 折叠屏展开时 NavigationRail + 对话/Run 双栏 | 几乎所有 AI App 未适配 |

---

## 七、参考资料

1. [Cursor for iOS — Build from anywhere](https://cursor.com/blog/ios-mobile-app) — Agent 云端+本地、Live Activities、Remote Control
2. [Agent UX: design consensus (jacar.es, 2026-07)](https://jacar.es/en/ux-for-agents-first-design-consensus/) — 五大设计共识
3. [UI design for agents: principles (jacar.es, 2026-07)](https://jacar.es/en/ui-design-for-agents-principles-were-starting-to-understand/) — 进度面板/审批界面/执行浏览器
4. [Designing an Honest Mobile Run-State UI (dev.to)](https://dev.to/igorganapolsky/designing-an-honest-mobile-run-state-ui-for-ai-agents-3a9n) — 移动端 Run 状态 UI
5. [Microsoft 365 Copilot mobile redesign](https://microsoft.design/articles/the-new-microsoft-365-copilot-mobile-experience/) — DAB、hamburger menu、conversational paradigm
6. [Smashing Magazine — Designing Better AI Experiences](https://www.smashingmagazine.com/) — chat→task 转型、结构化输入模式
7. [AI Orchestrator TUI](https://github.com/Open-Makers/ai-coding-agents-orchestrator) — Agent 面板 + Token Monitor 终端参考
8. [LobeChat](https://github.com/lobehub/lobe-chat) — 开源 ChatGPT UI，侧边栏+多模型+知识库

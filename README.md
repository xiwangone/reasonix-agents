<div align="center">

<img src="logo.png" width="96" height="96" alt="Reasonix Android" style="border-radius: 24px" />

# Reasonix Android

**🤖 由 AI 维护迭代的 AI 编码助手客户端** —— 通过 SSE 流式连接自部署的 Reasonix 服务端，实时渲染推理过程与工具调用，随时随地完成编码任务。还有更多 Agent 体验模式待探索。（多服务器 / 模型分组 / 提示词系统 / 坚果云同步已支持，更多能力持续迭代中……）

[![Release](https://img.shields.io/github/v/release/xiwangone/reasonix-agents?color=2ea44f&label=最新版本&logo=github)](https://github.com/xiwangone/reasonix-agents/releases/latest)
[![Build](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml/badge.svg)](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml)
[![Stars](https://img.shields.io/github/stars/xiwangone/reasonix-agents?color=cb3837&label=Stars&logo=github)](https://github.com/xiwangone/reasonix-agents)
[![Downloads](https://img.shields.io/github/downloads/xiwangone/reasonix-agents/total?color=blue&label=下载量&logo=download)](https://github.com/xiwangone/reasonix-agents/releases)
[![License](https://img.shields.io/github/license/xiwangone/reasonix-agents?color=ff69b4&label=许可)](LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/xiwangone/reasonix-agents?color=yellow&label=最近提交&logo=github)](https://github.com/xiwangone/reasonix-agents/commits/master)

[**English**](README_EN.md) | **简体中文**

[![📥 下载 APK](https://img.shields.io/badge/📥-下载%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/xiwangone/reasonix-agents/releases/latest)
[![RikkaHub Agents 开发中](https://img.shields.io/badge/🤖-RikkaHub%20Agents%20开发中-8b5cf6?style=for-the-badge)](https://github.com/xiwangone/rikkahub-agents)

**🤝 相关项目：[RikkaHub Agents](https://github.com/xiwangone/rikkahub-agents)** — 由 AI 维护迭代的手机端 Agent 工具（80+ 设备工具，按需启用）

> <span style="color:red">**❗️❗️❗️ 注：RikkaHub Agents 包含 80+ 工具，请按需启用，避免常驻过多增加消耗！！！**</span>

> <span style="color:red">**❗️❗️❗️ 注：Reasonix Agents 需在本地或服务器自部署 Reasonix 服务端（DeepSeek-Reasonix 协议）使用，不支持云端托管，请自备服务资源！**</span>


</div>

---

## ✨ 功能特性

- 🧠 **提示词系统** — 10 槽位自定义，注入会话上下文
- ⚙️ **设置组件化** — 二级界面分组管理
- 💾 **备份导入导出** — 配置 + 会话，凭据加密
- 🗑️ **会话多选批量删除** — 批量管理会话
- 🔧 **CLI 集成** — 调用部署的 aider / opencode
- 🔐 **多服务器 / 多用户自配置** — 防白嫖
- 🤫 **Web 系统提示静默化** — 新会话不再弹出系统提示
- 🖼️ **图片发送** — 本地 OCR 识别
- 📊 **CI 监控悬浮窗** — 三色状态指示
- 🎨 **多主题图标** — 默认 / Material 蓝 / 品牌深色

---

## 🚨 重要声明

| 项目 | 链接 | 说明 |
|------|------|------|
| 🟡 **本仓库（AI 协助维护版）** | https://github.com/xiwangone/reasonix-agents | **AI 协助合并上游 + 编译** |
| 🟣 **RikkaHub Agents（并列项目）** | https://github.com/xiwangone/rikkahub-agents | **Android 设备端 LLM 智能体 · 80+ 设备工具（开发中）** |
| 🔵 **DeepSeek-Reasonix（协议上游）** | https://github.com/esengine/DeepSeek-Reasonix | **后端服务与协议定义，本客户端遵循其协议** |
| 🟢 **DeepSeek-Reasonix-android（客户端原版）** | https://github.com/hxr66666/DeepSeek-Reasonix-android | **原版 Fork，本仓库基于此（MIT）** |

> ### ⚠️ 使用须知
>
> - **❌ 非官方发布** — 不是 esengine 官方发布
> - **❌ 非原版发布** — 不是 hxr66666 原版开发者发布
> - ✅ 代码来源可信（MIT 许可），由 AI 协助合并上游代码并持续编译，已在 LICENSE 注明原作者致谢
> - 💡 如遇问题，建议优先使用 [原版仓库](https://github.com/hxr66666/DeepSeek-Reasonix-android) 或 [协议上游](https://github.com/esengine/DeepSeek-Reasonix)

---

## 🤖 AI 维护说明

> 本项目由 **AI（Reasonix Agents 助手）参与开发与维护**，与人工协作完成。

| 维护项 | 说明 |
|--------|------|
| 🧑💻 **AI 协作开发** | Basic Auth、HTTP/HTTPS 切换等改动由 AI 编码 + CI 验证闭环 |
| 🛠️ **AI 持续维护** | 需求拆解、代码审查、构建排查、文档同步均由 AI 协助完成 |
| 🔍 **AI 驱动迭代** | 每次功能迭代遵循「分析 → 实现 → 验证 → 同步手册」流程 |
| 👤 **人工决策** | 关键决策（安全边界、凭据管理、发布策略）由人工确认后执行 |

> **安全边界**：AI 严格遵循凭证脱敏、沙箱内自由操作、外部变更经确认的原则；代码提交、密钥管理全程可追溯。

---

## 简介

[DeepSeek-Reasonix](https://github.com/esengine/DeepSeek-Reasonix) 的 **Android 原生客户端**，基于 Web 前端协议完整重写，使用 **Kotlin + Jetpack Compose + Markwon** 构建。非 WebView 套壳，Compose 声明式布局自动适配移动端。

**特色**：底部 Tab 导航、SSE 流式对话 + 断线自动重连、推理过程实时渲染、工具调用卡片 + Patch diff 渲染、Todo 任务面板、Rewind 回退、Slash 命令、中英双语。

---

## 功能

| 模块 | 说明 |
|------|------|
| 🗂️ **底部 Tab 导航** | Chat / Files / Settings 三页签，切换保留各页状态（navigation「导航」方案见 [docs/开发方案.md](docs/开发方案.md)） |
| 💬 **AI 对话** | 完整的 SSE 流式通信，支持实时渲染推理过程、工具调用卡片、费用统计 |
| 🔄 **SSE 自动重连** | 网络抖动指数退避自动重连（1s→30s），HTTP 层错误不重连；顶栏绿/黄/红连接状态点 |
| ✅ **Todo 面板** | 会话任务进度面板：进度条、完成划线、进行中高亮，随事件自动刷新 |
| 📝 **Patch diff 渲染** | 工具卡内嵌 diff 视图：SEARCH/REPLACE、apply_patch、unified diff 自动识别，红绿着色 + 折叠 |
| 📁 **文件浏览** | 从会话工具事件聚合文件清单（树形 + 状态着色 + 内容预览）；完整版依赖上游 /file API（见 [docs/upstream-file-api-request.md](docs/upstream-file-api-request.md)） |
| 📝 **Markdown 渲染** | Markwon 原生引擎，支持代码高亮（Prism4j）、表格、图片、HTML、任务列表 |
| 🧠 **推理展示** | 可折叠的 reasoning block，展示 AI 思考过程 |
| 🔧 **工具卡片** | 实时展示工具调用——名称、参数、输出，支持折叠展开 |
| ⏪ **Rewind 回退** | 回退到历史检查点，支持多种作用域（代码+对话 / 仅对话 / 仅代码 / 分叉） |
| 📦 **会话管理** | 新建 / 恢复 / 切换会话，会话列表 |
| ⌨️ **Slash 命令** | `/compact` `/new` `/resume` `/rewind` `/model` `/mcp` `/help` 等 |
| 🔐 **Basic Auth** | 连接支持用户名/密码认证（可选填，兼容无认证直连） |
| 🌐 **HTTP/HTTPS** | 协议可切换，HTTPS 默认端口 443；明文 HTTP 仅限本机/模拟器/Tailscale 白名单 |
| 🔑 **凭据回填** | 服务器地址/凭据持久化，下次启动自动回填 |
| 🔒 **密码遮蔽** | 密码输入框 `PasswordVisualTransformation` 遮蔽，防窥屏 |
| 🛡️ **网络安全收紧** | 默认禁用明文流量，仅白名单放行 localhost / 10.0.2.2 / *.ts.net |
| 📥 **自动发行** | GitHub Releases 一键发布：手动触发 CI 自动构建 + 上传 APK |
| 🌙 **暗色主题** | Material 3 暗色主题，与 Web 端一致的 OKLCH 色彩 |
| 🌐 **国际化** | 中 / 英双语 |

---

## 界面

原生 Compose 界面，暗色主题，自动适配移动端：

- **服务器配置页**：协议（HTTP/HTTPS）切换、地址/端口输入、用户名/密码（可选）、连接预览
- **底部 Tab**：聊天 / 文件 / 设置三页签，切换保留各页状态
- **聊天页**：流式消息、推理折叠、工具卡片（含 Patch diff）、Todo 面板、会话管理、Slash 命令、Rewind、连接状态指示
- **文件页**：会话文件清单（树形 + 状态着色 + 内容预览）
- **设置页**：主题、模型、显示选项、服务器信息、CI 监控、关于

---

## 技术栈

| 层面 | 技术 |
|------|------|
| **语言** | Kotlin 2.1 |
| **UI** | Jetpack Compose (Material 3, BOM 2026.02) |
| **架构** | MVVM (ViewModel + Repository) |
| **网络** | OkHttp 4.12 + OkHttp-SSE |
| **序列化** | Gson 2.10 |
| **Markdown** | Markwon 4.6（core + syntax-highlight + html + image + tables + tasklist + strikethrough + linkify） |
| **语法高亮** | Prism4j 2.0 |
| **图片加载** | Coil 2.7 / Glide 4.16 / Picasso 2.8 |
| **协程** | Kotlinx Coroutines 1.7 |
| **构建** | Gradle 9.3 + AGP 9.1 + Version Catalog |

### 兼容性

- **最低**: Android 6.0 (API 23)
- **目标**: Android 14 (API 36)
- **编译**: Android 15 (API 37)

---

## 快速开始

### 前置条件

- **JDK 17+**
- **Android Studio** (最新稳定版)
- **Android SDK** (API 37)

### 构建 & 运行

```bash
# 1. 克隆仓库
git clone https://github.com/xiwangone/reasonix-agents.git
cd reasonix-agents

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

或在 Android Studio 中直接打开项目，点击 Run。

### 部署 Reasonix 服务端

Reasonix 服务端（DeepSeek-Reasonix 协议）需自部署（本地或云服务器），App 通过 SSE 连接使用。推荐 systemd 方式：

**1. 安装（需 Node.js 18+）**

```bash
npm install -g reasonix
reasonix --version   # v1.19.x
```

**2. systemd 服务**（`/etc/systemd/system/reasonix-serve.service`）

```ini
[Unit]
Description=Reasonix serve
After=network.target
[Service]
Type=simple
ExecStart=/usr/bin/reasonix serve --addr 0.0.0.0:9899
Restart=on-failure
RestartSec=3
Environment=HOME=/root
# 模型 API key 按需添加：Environment=OPENAI_API_KEY=xxx
[Install]
WantedBy=multi-user.target
```

**3. 启动**

```bash
systemctl enable --now reasonix-serve
systemctl status reasonix-serve
```

**4. 访问方式**

- 本地/局域网：防火墙放行 9899，App 填 `http://服务器IP:9899`
- 公网推荐：nginx 反代 + HTTPS（`proxy_pass http://127.0.0.1:9899`）

**5. 连接 App**：填写服务器地址 `http(s)://你的地址:端口` + 认证（Basic Auth / Token），支持多服务器配置切换。

### 连接后端

客户端需要连接到 Reasonix 后端服务才能正常工作。

```bash
# 启动 Reasonix 服务（见协议上游 DeepSeek-Reasonix）
reasonix serve --addr "0.0.0.0:8787"
```

打开 App → 服务器配置页：

| 配置项 | 说明 |
|--------|------|
| 协议 | HTTP / HTTPS（HTTPS 默认端口 443） |
| 地址 | 服务器 IP 或域名 |
| 端口 | 默认 8920（HTTP）/ 443（HTTPS），可改 |
| 用户名/密码 | 可选；Basic Auth 认证时填写，留空兼容无认证直连 |

---

## 项目结构

```
reasonix-agents/
├── app/
│   ├── build.gradle.kts          # 应用构建配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/reasonix/agents/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ServerConfigStore.kt        # 配置持久化（含凭据/协议）
│       │   │   ├── api/
│       │   │   │   ├── ReasonixApi.kt          # REST API（Basic Auth）
│       │   │   │   └── ReasonixSseClient.kt    # SSE 流式客户端（Basic Auth）
│       │   │   ├── model/Models.kt             # 数据模型
│       │   │   └── repository/ChatRepository.kt
│       │   └── ui/
│       │       ├── screen/
│       │       │   ├── ChatScreen.kt           # 聊天主界面
│       │       │   └── ServerConfigScreen.kt   # 服务器配置（协议/凭据）
│       │       ├── components/
│       │       │   ├── ChatMessage.kt          # 消息气泡
│       │       │   ├── MarkdownRenderer.kt     # Markwon 渲染器
│       │       │   ├── MessageList.kt          # 消息列表
│       │       │   ├── ReasoningBlock.kt       # 推理块
│       │       │   ├── RewindPickerDialog.kt   # 回退选择器
│       │       │   ├── SlashMenu.kt            # Slash 命令菜单
│       │       │   ├── StatsDialog.kt          # 统计弹窗
│       │       │   ├── ToolCard.kt             # 工具调用卡片
│       │       │   └── WelcomeScreen.kt        # 欢迎页
│       │       ├── theme/
│       │       └── viewmodel/ChatViewModel.kt  # 聊天 ViewModel
│       └── res/
│           ├── drawable/logo.png               # 应用 Logo (720×720)
│           └── mipmap-*/                        # 启动图标
├── gradle/
│   └── libs.versions.toml                      # Version Catalog
├── settings.gradle.kts
└── build.gradle.kts
```

---

## CI/CD

本项目使用 **GitHub Actions** 自动构建。每次 push 到 `master` 分支或发起 Pull Request 时触发：

- ✅ 编译检查
- ✅ Debug APK 构建
- ✅ Release APK 构建
- ✅ 上传 APK 到 Actions Artifacts

工作流配置见 [`.github/workflows/build.yml`](.github/workflows/build.yml)。

构建状态：[![Build](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml/badge.svg)](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml)

---

## 与 Web 端的关系

```
┌─────────────────────────────────────────┐
│              Reasonix 后端               │
│   POST /submit   GET /events (SSE)      │
│   POST /approve  POST /rewind           │
│   GET /sessions  GET /history           │
└──────────┬───────────────┬──────────────┘
           │               │
           ▼               ▼
┌──────────────────┐  ┌──────────────────┐
│  Web 前端（参考） │  │  Android 客户端   │
│  (官方 index.html)│  │  (本仓库)        │
└──────────────────┘  └──────────────────┘
```

Android 端完全遵循 [DeepSeek-Reasonix](https://github.com/esengine/DeepSeek-Reasonix) 定义的后端协议（SSE 流式消息格式、工具调用结构、rewind 语义等），提供与 Web 端一致的 AI 编码助手体验。

---

## License

MIT — 详见 [LICENSE](LICENSE)。

**致谢**：本仓库 fork 自 [hxr66666/DeepSeek-Reasonix-android](https://github.com/hxr66666/DeepSeek-Reasonix-android)（MIT），协议遵循 [esengine/DeepSeek-Reasonix](https://github.com/esengine/DeepSeek-Reasonix)。感谢原作者的工作。

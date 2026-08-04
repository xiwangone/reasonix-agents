<div align="center">

<img src="logo.png" width="96" height="96" alt="Reasonix Android" style="border-radius: 24px" />

# Reasonix Android

**🤖 Native Android client for Reasonix · Kotlin + Jetpack Compose · Basic Auth / HTTPS support**

[![Release](https://img.shields.io/github/v/release/xiwangone/reasonix-agents?color=2ea44f&label=Latest%20Release&logo=github)](https://github.com/xiwangone/reasonix-agents/releases/latest)
[![Build](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml/badge.svg)](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml)
[![Stars](https://img.shields.io/github/stars/xiwangone/reasonix-agents?color=cb3837&label=Stars&logo=github)](https://github.com/xiwangone/reasonix-agents)
[![Downloads](https://img.shields.io/github/downloads/xiwangone/reasonix-agents/total?color=blue&label=Downloads&logo=download)](https://github.com/xiwangone/reasonix-agents/releases)
[![License](https://img.shields.io/github/license/xiwangone/reasonix-agents?color=ff69b4&label=License)](LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/xiwangone/reasonix-agents?color=yellow&label=Last%20Commit&logo=github)](https://github.com/xiwangone/reasonix-agents/commits/master)

[**简体中文**](README.md) | **English**

[![📥 Download APK](https://img.shields.io/badge/📥-Download%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/xiwangone/reasonix-agents/releases/latest)

</div>

---

## 🚨 Disclaimer

| Project | Link | Description |
|------|------|------|
| 🔵 **DeepSeek-Reasonix (Protocol Upstream)** | https://github.com/esengine/DeepSeek-Reasonix | **Backend service & protocol definition this client follows** |
| 🟢 **DeepSeek-Reasonix-android (Original Client)** | https://github.com/hxr66666/DeepSeek-Reasonix-android | **Original fork this repo is based on (MIT)** |
| 🟡 **This Repo (Independently Maintained)** | https://github.com/xiwangone/reasonix-agents | **Forked with credit to the original author; independently developed** |

> ### ⚠️ Notice
>
> - **❌ Not an official release** — not published by esengine
> - **❌ Not the original release** — not published by hxr66666
> - ✅ Source is trustworthy (MIT license), AI-assisted maintenance merging upstream with continuous builds, original author credited in LICENSE
> - 💡 For issues, prefer the [original repo](https://github.com/hxr66666/DeepSeek-Reasonix-android) or [protocol upstream](https://github.com/esengine/DeepSeek-Reasonix)

---

## 🤖 AI Maintenance

> This project is **co-developed and maintained by AI (Reasonix Agents Assistant)** alongside humans.

| Area | Description |
|--------|------|
| 🧑💻 **AI Co-development** | Basic Auth, HTTP/HTTPS switch, fixed signing etc. implemented by AI with CI verification loop |
| 🛠️ **AI Ongoing Maintenance** | Requirement breakdown, code review, build troubleshooting, docs sync all AI-assisted |
| 🔍 **AI-driven Iteration** | Every feature follows "analyze → implement → verify → sync manual" |
| 👤 **Human Decisions** | Key decisions (security boundaries, credential management, release policy) confirmed by humans |

> **Security**: AI strictly follows credential masking, free operations inside sandbox, external changes require confirmation; code commits and key management fully traceable.

---

## Overview

A **native Android client** for [DeepSeek-Reasonix](https://github.com/esengine/DeepSeek-Reasonix), fully rewritten based on the Web frontend protocol using **Kotlin + Jetpack Compose + Markwon**. Not a WebView wrapper — Compose declarative layout auto-adapts to mobile.

**Highlights**: SSE streaming chat, real-time reasoning rendering, tool call cards, Rewind, Slash commands, bilingual (zh/en).

---

## Features

| Module | Description |
|------|------|
| 💬 **AI Chat** | Full SSE streaming, real-time reasoning rendering, tool call cards, cost stats |
| 📝 **Markdown Rendering** | Markwon engine: syntax highlight (Prism4j), tables, images, HTML, task lists |
| 🧠 **Reasoning Display** | Collapsible reasoning blocks |
| 🔧 **Tool Cards** | Real-time tool calls — name, args, output, collapsible |
| ⏪ **Rewind** | Roll back to checkpoints (code+chat / chat-only / code-only / fork) |
| 📦 **Session Management** | New / resume / switch / delete sessions |
| ⌨️ **Slash Commands** | `/compact` `/new` `/resume` `/rewind` `/model` `/mcp` `/help` etc. |
| 🔐 **Basic Auth** | Username/password auth (optional, backward compatible) |
| 🌐 **HTTP/HTTPS** | Protocol switchable, HTTPS default port 443 |
| 🔑 **Credential Persist** | Server address/credentials saved, auto-fill on next launch |
| 🔒 **Password Masking** | `PasswordVisualTransformation` hides input |
| 📥 **Auto Release** | GitHub Releases one-click: manual CI trigger builds & uploads APK |
| 🌙 **Dark Theme** | Material 3 dark theme, OKLCH colors matching Web |
| 🌐 **i18n** | Chinese / English |

---

## UI

Native Compose UI, dark theme, auto-adapts to mobile:

- **Server Config page**: protocol (HTTP/HTTPS) switch, address/port input, username/password (optional), connection preview
- **Chat page**: streaming messages, collapsible reasoning, tool cards, session management, Slash commands, Rewind

---

## Tech Stack

| Layer | Tech |
|------|------|
| **Language** | Kotlin 2.1 |
| **UI** | Jetpack Compose (Material 3, BOM 2026.02) |
| **Architecture** | MVVM (ViewModel + Repository) |
| **Network** | OkHttp 4.12 + OkHttp-SSE |
| **Serialization** | Gson 2.10 |
| **Markdown** | Markwon 4.6 (core + syntax-highlight + html + image + tables + tasklist + strikethrough + linkify) |
| **Highlighting** | Prism4j 2.0 |
| **Image Loading** | Coil 2.7 / Glide 4.16 / Picasso 2.8 |
| **Coroutines** | Kotlinx Coroutines 1.7 |
| **Build** | Gradle 9.3 + AGP 9.1 + Version Catalog |

### Compatibility

- **Min**: Android 6.0 (API 23)
- **Target**: Android 14 (API 36)
- **Compile**: Android 15 (API 37)

---

## Quick Start

### Prerequisites

- **JDK 17+**
- **Android Studio** (latest stable)
- **Android SDK** (API 37)

### Build & Run

```bash
# 1. Clone
git clone https://github.com/xiwangone/reasonix-agents.git
cd reasonix-agents

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open in Android Studio and hit Run.

### Connect to Backend

```bash
# Start Reasonix server (see DeepSeek-Reasonix upstream)
reasonix serve --addr "0.0.0.0:8787"
```

Open the app → Server Config page:

| Field | Description |
|--------|------|
| Protocol | HTTP / HTTPS (HTTPS default port 443) |
| Address | Server IP or domain |
| Port | Default 8920 (HTTP) / 443 (HTTPS), configurable |
| Username/Password | Optional; for Basic Auth, leave blank for no-auth direct connect |

---

## Project Structure

```
reasonix-agents/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/reasonix/agents/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ServerConfigStore.kt        # Config persistence (credentials/protocol)
│       │   │   ├── api/
│       │   │   │   ├── ReasonixApi.kt          # REST API (Basic Auth)
│       │   │   │   └── ReasonixSseClient.kt    # SSE streaming (Basic Auth)
│       │   │   ├── model/Models.kt
│       │   │   └── repository/ChatRepository.kt
│       │   └── ui/
│       │       ├── screen/
│       │       │   ├── ChatScreen.kt
│       │       │   └── ServerConfigScreen.kt   # Server config (protocol/credentials)
│       │       ├── components/
│       │       │   ├── ChatMessage.kt
│       │       │   ├── MarkdownRenderer.kt
│       │       │   ├── MessageList.kt
│       │       │   ├── ReasoningBlock.kt
│       │       │   ├── RewindPickerDialog.kt
│       │       │   ├── SlashMenu.kt
│       │       │   ├── StatsDialog.kt
│       │       │   ├── ToolCard.kt
│       │       │   └── WelcomeScreen.kt
│       │       ├── theme/
│       │       └── viewmodel/ChatViewModel.kt
│       └── res/
│           ├── drawable/logo.png
│           └── mipmap-*/
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

---

## CI/CD

GitHub Actions builds on every push to `master` or Pull Request:

- ✅ Compile check
- ✅ Debug APK build
- ✅ Release APK build
- ✅ Upload APKs to Actions Artifacts

Workflow: [`.github/workflows/build.yml`](.github/workflows/build.yml)

Build status: [![Build](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml/badge.svg)](https://github.com/xiwangone/reasonix-agents/actions/workflows/build.yml)

---

## Relation to Web

Android client fully follows the [DeepSeek-Reasonix](https://github.com/esengine/DeepSeek-Reasonix) backend protocol (SSE message format, tool call structure, rewind semantics), providing the same AI coding assistant experience as the Web UI.

---

## License

MIT — see [LICENSE](LICENSE).

**Credits**: Forked from [hxr66666/DeepSeek-Reasonix-android](https://github.com/hxr66666/DeepSeek-Reasonix-android) (MIT), protocol follows [esengine/DeepSeek-Reasonix](https://github.com/esengine/DeepSeek-Reasonix). Thanks to the original authors.

# 上游 Feature Request：serve 增加 /file 系列 API

> 提交对象：esengine/DeepSeek-Reasonix（Reasonix 协议上游，main-v2）
> 提出方：reasonix-agents（Android 原生客户端）
> 日期：2026-08-05
> 状态：已形成文本，待环境具备 GitHub 认证后创建 issue；本文档随仓库存档

---

## 一、背景

Reasonix Agents 客户端（reasonix-agents）需要「会话文件浏览」能力，但 serve（main-v2）
目前**没有文件系统 API**。已核实的 serve 端点清单（`internal/serve/serve.go` 逐行核对）：

- GET：`/events`（SSE）、`/history`、`/context`、`/status`、`/models`、`/sessions`、
  `/todos`、`/skills`、`/checkpoints`、`/branches`
- POST：`/submit` `/cancel` `/approve` `/plan` `/compact` `/new` `/rewind` `/fork`
  `/summarize` `/answer` `/resume` `/delete-session` `/tool-approval-mode`
  `/auto-approve-tools` `/bypass` `/goal` `/forget`

客户端因此只能退而求其次：从 SSE 工具调用事件（`tool_dispatch`/`tool_result`）中
**聚合**文件清单与内容缓存，无法按需读取任意文件、无法反映会话目录真实状态。

## 二、请求的端点（3 个 GET）

| 方法 | 端点 | 语义 | 建议响应 |
|---|---|---|---|
| GET | `/files` | 会话工作目录文件清单 | `[{path, size, mtime, kind(file/dir)}]`（相对路径，树形由客户端折叠） |
| GET | `/file?path=…` | 读取文件内容 | `{path, content}` 或 404 `{error}` |
| GET | `/file/status?path=…` | 单文件状态 | `{path, status: modified/added/deleted/unchanged}` |

鉴权：与现有端点一致，走 serve 的 `authGate`（none / token / password）。
错误语义：文件不存在返回 404 + JSON `{error}`；目录返回 400。

## 三、对照：opencode serve 已有同类端点

opencode（grapeot/opencode_android_client 客户端对应）的服务端已提供：

| opencode 端点 | 语义 |
|---|---|
| `GET file/content` | 读取文件内容 |
| `GET file/status` | 文件状态（modified/added/deleted） |
| `GET find/file` | 按路径/关键字搜索文件 |

DeepSeek-Reasonix 与 opencode 同为「远程连 CLI agent 服务器」场景，客户端对文件能力的
诉求一致，建议 serve 参照补齐。

## 四、为什么需要

1. **移动端体验**：Android 客户端无法像 Web/桌面端那样开终端，文件浏览是观察
   agent 工作成果的主要途径。
2. **安全边界**：客户端聚合方案只能看到「工具碰过的文件」，无法核实未改动文件，
   不利于代码审查。
3. **一致性**：`/todos` 已有（客户端已接入），文件清单是同类「会话状态可视化」能力。

## 五、兼容性说明

- 客户端 JSON 解析一律宽松（Gson 忽略未知字段），新增端点不影响现有功能。
- 若端点短期不落地，客户端维持「工具事件聚合」模式（本仓库 Files 页），
  端点落地后平滑升级为直连模式。

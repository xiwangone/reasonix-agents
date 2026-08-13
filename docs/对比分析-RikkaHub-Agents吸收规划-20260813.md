# Reasonix Agents ← RikkaHub Agents 对比分析与吸收规划

> 日期：2026-08-13 · 对象：/root/repo（RikkaHub Agents，基于 rikkahub/rikkahub + ExTV/rikkahub-agent 的 AI 维护 Fork，AGPL-3.0）
> vs /root/reasonix-agents（Reasonix Agents，基于 hxr66666/DeepSeek-Reasonix-android 的独立客户端，MIT）
> 注意：本规划与《对比分析-RikkaHub官方吸收规划-20260808.md》对象不同（后者对比 RikkaHub 官方仓库，聚焦 UI/渲染）。
> 原则：只做理念级借鉴（看设计、自行重写实现），**不搬代码**——RikkaHub 官方与 /root/repo 均为 AGPL-3.0，Reasonix 为 MIT，AGPL 传染会污染 MIT 授权。

---

## 一、定位对比

| 维度 | RikkaHub Agents（/root/repo） | Reasonix Agents |
|---|---|---|
| 本质 | **设备端 Agent 工具**：AI 直接操控手机（80+ 工具：点击/SSH/Shizuku/浏览器/TG Bot/定时任务/本地 LLM） | **AI 编码助手客户端**：连接自部署 Reasonix serve，做对话/推理/工具调用的可视化前端 |
| 能力所在 | 客户端全栈（工具执行在 App 内） | 服务端（客户端只管交互渲染） |
| 近期主攻 | Vault 凭证库、SSH/Shizuku、记忆分层注入、缓存命中优化、自动压缩 | 消息流式视觉、CLI 集成、图片 OCR、CI 悬浮窗、WebDAV 同步 |

## 二、可吸收清单（按价值/成本排序）

### P0 — 高价值，定位兼容，建议吸收
1. **记忆分层注入**（core 常驻 + conditional 按需检索）
   - RikkaHub：MemoryEntity 加 tier 字段（core/conditional），core 每轮常驻注入（纪律/决策/指针），conditional 默认不注入、AI 涉及相关场景时先调 memory_search 检索再使用（MemoryTools.kt + MemoryRepository.searchConditionalMemories）
   - Reasonix 现状：MemoryStore（注释即"仿 RikkaHub 记忆功能"）停留在早期版——单层全量注入 + 800 字符截断
   - 借鉴：MemoryItem 加 tier；core 常驻注入；conditional 客户端本地按关键词匹配注入（Reasonix 无服务端 memory_search 工具，客户端对用户消息分词匹配）
2. **自动压缩 UX**
   - RikkaHub：双模式触发（百分比阈值 / token 消耗累计）+ 会话级触发点 + 确认弹窗 + 生成中不弹窗（延后到对话结束）
   - Reasonix 现状：仅 `/compact auto|manual` 命令 + CompactionNotice 渲染
   - 借鉴：客户端侧自动触发策略 + 确认/延后弹窗交互
3. **Vault 凭据安全体系**
   - RikkaHub：指纹门禁（biometric）+ 审计日志 + 输出掩码 SecretMasker + 会话 TTL/撤销
   - Reasonix 现状：CredentialCrypto（AES-256-GCM + AndroidKeyStore + 备份排除）基础已有
   - 借鉴：进入凭据/服务器配置前指纹门禁；日志/备份输出掩码

### P1 — 中价值
4. **缓存命中优化**：保前缀只回收尾部 + tool schema 规范化排序（RikkaHub 目标 50%→90%+）。Reasonix 客户端提示词注入顺序稳定可借鉴，间接提升服务端前缀缓存命中
5. **悬浮窗交互**：18dp 小圆点常态 + 点击卡片动画展开收起（Reasonix 已有 CI 悬浮球，可升级交互）
6. **网络代理**：TG 代理（SOCKS5/HTTP）思路移植为客户端连接代理设置，利于连海外 VPS 的 serve
7. **CI 流程细节**：构建/配置缓存 + 缓存防堆积 + release.yml 发版专用工作流 + 统一 APK 命名

### P2 — 评估后做
8. Skills（服务端已有 `GET /skills` 端点"待接"）、多语言扩展（RikkaHub 6 语 vs Reasonix 双语）、子 Agent 并行展示（依赖服务端协议支持）

## 三、明确不吸收（定位冲突）

80+ 设备工具、Telegram Bot、内置浏览器、定时任务/工作流、本地 LLM（LiteRT）、proot 工作区、Shizuku 提权——这些是 RikkaHub Agents 的核心方向，与 Reasonix「连自部署服务端做编码」定位冲突，吸收会拖垮轻量客户端定位。

## 四、关键约束

**许可证**：RikkaHub 官方（rikkahub/rikkahub，已核实 master LICENSE）与 /root/repo 均为 **AGPL-3.0**；Reasonix 为 **MIT**。MIT 项目不能直接复制 AGPL 代码。所有吸收只做理念级借鉴（看设计、自行重写实现），不搬代码。既有《对比分析-RikkaHub官方吸收规划-20260808.md》中「移植 RikkaHub CardGroup DSL（MIT 可直接借鉴）」的前提需重新核实——该文档把官方仓库当作 MIT，实际为 AGPL-3.0，移植需谨慎或重写。

## 五、执行方式

- 每项独立 commit 可回退；遵循「分析 → 实现 → 验证 → 同步手册」流程
- 执行清单见同目录《自动任务-吸收执行清单.md》，逐项验收

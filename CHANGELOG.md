## 2026-08-13

- **功能** 记忆分层注入（仿 RikkaHub Agents 记忆分层）— MemoryItem 加 tier（core 常驻 / conditional 按需），core 每轮注入、conditional 按用户消息关键词匹配注入（省 token）；旧数据归一化兼容；设置页添加可选类型 + 条目「按需」徽标（`e614db9`）
- **文档** 新增《对比分析-RikkaHub-Agents吸收规划-20260813.md》（定位对比 + P0/P1/P2 吸收清单 + AGPL/MIT 许可约束）与《自动任务-吸收执行清单.md》（T1-T7 逐项验收）

# Changelog — Reasonix Agents

> Reasonix 的 Android 原生客户端（Kotlin + Jetpack Compose + Markwon），与 RikkaHub Agents 并列独立维护。
> fork 自 hxr66666/DeepSeek-Reasonix-android（MIT）；协议上游 esengine/DeepSeek-Reasonix（MIT）。
> 只保留时间线的**功能改动**与**修复成功**记录。

---

## 2026-08-05

- **改名** 仓库 reasonix-android → **reasonix-agents**（旧名自动重定向；git remote / 包名 `com.reasonix.agents` / README 徽章全部更新）
- **功能** 批 1-5 开发落地（`9decc25`）：
  - 批 1：底部 Tab 导航重构（Chat / Files / Settings 三 Tab，新增 `ui/navigation/Screens.kt` 路由）
  - 批 2：SSE 断线自愈（指数退避自动重连 1s→30s + 顶栏连接状态点 + 断线增量合并）
  - 批 3：Todo 面板 + Patch diff 渲染（`GET /todos` + DiffCard 四形态 diff 高亮，Prism4j diff 语法）
  - 批 4：Files 页轻量聚合版 + 上游 `/file` API feature request（`docs/upstream-file-api-request.md`）
  - 批 5：网络安全收紧（`network_security_config.xml` cleartext 白名单化）
- **修复** CI 编译错误（`e0053ba`）— ReasonixSseClient `retryWhen` Long/Int 类型不匹配 + DiffCard @Composable 标注缺失（11 处）
- **功能** 应用图标升级（`48de46d`）— Reasonix 品牌定制版（R+闪电元素、紫蓝渐变、PCB 纹理，无水印）

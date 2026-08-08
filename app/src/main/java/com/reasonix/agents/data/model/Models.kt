package com.reasonix.agents.data.model

import com.google.gson.annotations.SerializedName

// ── SSE 事件类型 ──

/** SSE 连接状态：已连接 / 重连中 / 断开（驱动 Chat 顶栏绿/黄/红状态点） */
enum class ConnectionState {
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
}

enum class SseEventKind {
    turn_started,
    reasoning,
    text,
    message,
    tool_dispatch,
    tool_result,
    tool_progress,
    usage,
    notice,
    phase,
    approval_request,
    ask_request,
    compaction_started,
    compaction_done,
    turn_done,

    @SerializedName("")
    unknown,
}

data class SseEvent(
    val kind: String = "",
    val text: String? = null,
    val reasoning: String? = null,
    val err: String? = null,
    val level: String? = null,
    val tool: ToolPayload? = null,
    val usage: UsagePayload? = null,
    val approval: ApprovalPayload? = null,
    val ask: AskPayload? = null,
    val compaction: CompactionPayload? = null,
    val message: MessagePayload? = null,
)

// ── 工具相关 ──

data class ToolPayload(
    val id: String = "",
    val name: String = "",
    val args: String? = null,
    val arguments: String? = null,
    val output: String? = null,
    val err: String? = null,
    val truncated: Boolean = false,
    val readOnly: Boolean = false,
    val subject: String? = null,
)

// ── 用量统计 ──

data class UsagePayload(
    val totalTokens: Long = 0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val cacheHitTokens: Long = 0,
    val cacheMissTokens: Long = 0,
    // 2026-08-08：服务端额外推送的会话累计缓存快照（SSE usage 事件），
    // 比单轮 cacheHitTokens 更适合顶部累计统计展示。
    val sessionCacheHitTokens: Long = 0,
    val sessionCacheMissTokens: Long = 0,
    val cost: Double? = null,
    val costUsd: Double? = null,
    val currency: String? = null,
)

// ── 审批 ──

data class ApprovalPayload(
    val id: String = "",
    val tool: String = "",
    val subject: String? = null,
)

// ── 提问卡片 ──

data class AskPayload(
    val id: String = "",
    val questions: List<AskQuestion> = emptyList(),
)

data class AskQuestion(
    val id: String = "",
    val prompt: String = "",
    val multi: Boolean = false,
    val options: List<AskOption> = emptyList(),
)

data class AskOption(
    val label: String = "",
    val description: String? = null,
)

// ── 压缩通知 ──

data class CompactionPayload(
    val trigger: String? = null,
    val summary: String? = null,
    val messages: Int = 0,
)

// ── 消息（历史记录用） ──

// ── Todo（GET /todos 返回的任务清单，驱动 Todo 面板） ──

data class TodoItem(
    val id: String = "",
    val content: String = "",
    /** pending / in_progress / completed（兼容 "done"） */
    val status: String = "pending",
    /** 进行中状态的动作描述（serve 端 optional「可选」字段） */
    val activeForm: String? = null,
    /** 层级缩进（serve 端 optional 字段，0 = 顶层） */
    val level: Int = 0,
    val details: String? = null,
) {
    val isCompleted: Boolean
        get() = status == "completed" || status == "done"
    val isInProgress: Boolean
        get() = status == "in_progress" || status == "running"
}

data class MessagePayload(
    val role: String = "",
    val content: String? = null,
    val reasoning: String? = null,
)

// ── 历史消息 ──

data class HistoryMessage(
    val role: String = "",
    val content: String? = null,
    val reasoning: String? = null,
    val toolCalls: List<ToolCallPayload>? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
)

data class ToolCallPayload(
    val id: String = "",
    val name: String = "",
    val arguments: String? = null,
)

// ── 会话 ──

data class SessionInfo(
    val name: String = "",
    val path: String = "",
    val current: Boolean = false,
    val title: String? = null,
    val turns: Int = 0,
)

// ── 状态 ──

data class StatusInfo(
    val label: String? = null,
    val plan: Boolean = false,
    val toolApprovalMode: String? = null,
    val autoApproveTools: Boolean? = null,
    val bypass: Boolean? = null,
    val used: Long = 0,
    val window: Long = 0,
    val cacheHit: Long = 0,
    val cacheMiss: Long = 0,
    val lastUsage: LastUsage? = null,
    val balance: BalanceInfo? = null,
)

data class LastUsage(
    val cost: Double? = null,
    val costUsd: Double? = null,
    val totalCost: Double? = null,
    val currency: String? = null,
)

data class BalanceInfo(
    val display: String? = null,
)

// ── 检查点 ──

data class CheckpointInfo(
    val turn: Int = 0,
    val prompt: String? = null,
    val files: Int = 0,
)

// ── UI 消息模型 ──

sealed class ChatItem {
    /**
     * 用户消息。第六批：图片发送——[imagePath] 为本地缓存图片文件路径
     * （OCR 识别文本作为 [content] 发送；发送原图时 [content] 为空、仅展示图片）。
     */
    data class UserMessage(
        val content: String,
        val imagePath: String? = null,
    ) : ChatItem()

    data class AssistantMessage(
        val content: String = "",
        val reasoning: String? = null,
        val reasoningExpanded: Boolean = false,
    ) : ChatItem()

    /**
     * 2026-08-06：对齐 RikkaHub Agents 对话层次结构——
     * 一轮助手回复 = 有序块序列（推理/正文/工具按事件到达顺序交错）。
     * 渲染时严格按 blocks 顺序输出：推理折叠、正文平铺、工具折叠。
     */
    data class AssistantTurn(
        val blocks: List<TurnBlock>,
    ) : ChatItem()

    data class ToolCard(
        val id: String,
        val name: String,
        val args: String? = null,
        val output: String? = null,
        val err: String? = null,
        val truncated: Boolean = false,
        val isRunning: Boolean = true,
        val expanded: Boolean = false,
    ) : ChatItem()

    data class SystemNotice(
        val text: String,
        val isWarning: Boolean = false,
    ) : ChatItem()

    data class ErrorMessage(
        val text: String,
    ) : ChatItem()

    data class PhaseIndicator(
        val text: String,
    ) : ChatItem()

    data class UsageStats(
        val usage: UsagePayload,
    ) : ChatItem()

    data class CompactionNotice(
        val trigger: String? = null,
        val summary: String? = null,
        val messages: Int = 0,
    ) : ChatItem()

    data class ApprovalCard(
        val id: String,
        val tool: String,
        val subject: String? = null,
    ) : ChatItem()

    data class AskCard(
        val id: String,
        val questions: List<AskQuestion> = emptyList(),
    ) : ChatItem()
}

// ═══════════════════════════════════════════════
// TurnBlock — 一轮助手回复内的有序块（对齐 RikkaHub MessagePartBlock）
// ═══════════════════════════════════════════════

sealed class TurnBlock {
    /** 推理文本（默认折叠展示） */
    data class Reasoning(val text: String) : TurnBlock()

    /** 正文（Markdown 平铺） */
    data class Text(val text: String) : TurnBlock()

    /** 工具调用（默认折叠，含调用/结果） */
    data class Tool(
        val id: String,
        val name: String,
        val args: String? = null,
        val output: String? = null,
        val err: String? = null,
        val truncated: Boolean = false,
        val isRunning: Boolean = true,
        val expanded: Boolean = false,
    ) : TurnBlock()
}

// ── 模型列表（GET /models）──
data class ModelInfo(
    val ref: String = "",
    val provider: String = "",
    val model: String = "",
    val kind: String = "openai",
    val active: Boolean = false,
    val default: Boolean = false,
)

data class ModelsResponse(
    val current: String = "",
    val default: String = "",
    val label: String = "",
    val models: List<ModelInfo> = emptyList(),
)

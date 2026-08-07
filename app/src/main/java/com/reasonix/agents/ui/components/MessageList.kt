package com.reasonix.agents.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.model.ChatItem
import com.reasonix.agents.data.model.TurnBlock
import com.reasonix.agents.ui.theme.LocalPalette

// 匹配 index.html 调色板
private val bg: Color @Composable get() = LocalPalette.current.bg
private val border: Color @Composable get() = LocalPalette.current.border

/**
 * 聊天消息列表 — LazyColumn 渲染所有 ChatItem 类型。
 * 自动滚动到最新消息。
 */
@Composable
fun MessageList(
    items: List<ChatItem>,
    modifier: Modifier = Modifier,
    balance: String? = null,
    cumulativeTokens: Long = 0,
    onApprove: ((session: Boolean, persist: Boolean, scope: String) -> Unit)? = null,
    onDeny: (() -> Unit)? = null,
    onAskSubmit: ((List<Map<String, String>>) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
    // 2026-08-07：是否流式生成中（推理卡三态折叠：生成中计时/脉动/完成后自动收起）
    isStreaming: Boolean = false,
    // 2026-08-07：注入上下文折叠卡（系统提示词/用户提示词/记忆）
    systemPrompt: String? = null,
    userPrompt: String = "",
    memoryText: String? = null,
    onSaveUserPrompt: (String) -> Unit = {},
    onSaveMemory: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()

    // 新消息到达时自动滚到底部
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(items.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .background(bg),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // 2026-08-07：注入上下文折叠卡——默认折叠，展示/编辑注入 AI 的上下文
        if (systemPrompt != null || userPrompt.isNotBlank() || !memoryText.isNullOrBlank()) {
            item(key = "injection_context") {
                InjectionContextCard(
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    memoryText = memoryText,
                    onSaveUserPrompt = onSaveUserPrompt,
                    onSaveMemory = onSaveMemory,
                )
            }
        }

        itemsIndexed(items, key = { index, _ -> "msg_$index" }) { _, item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ChatItemRow(
                    item = item,
                    balance = balance,
                    isStreaming = isStreaming,
                    onApprove = onApprove,
                    onDeny = onDeny,
                    onAskSubmit = onAskSubmit,
                    cumulativeTokens = cumulativeTokens,
                    onRegenerate = onRegenerate,
                    onDeleteMessage = onDeleteMessage,
                )
            }
        }
    }
}

@Composable
private fun ChatItemRow(
    item: ChatItem,
    balance: String? = null,
    cumulativeTokens: Long = 0,
    isStreaming: Boolean = false,
    onApprove: ((session: Boolean, persist: Boolean, scope: String) -> Unit)?,
    onDeny: (() -> Unit)?,
    onAskSubmit: ((List<Map<String, String>>) -> Unit)?,
    onRegenerate: (() -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
) {
    when (item) {
        is ChatItem.UserMessage -> {
            UserMessageBubble(item.content, item.imagePath)
        }

        is ChatItem.AssistantMessage -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 2026-08-06：对齐 RikkaHub —— 正文在上正常展示，推理折叠块在下方
                // 助手正文（Markdown）
                if (item.content.isNotBlank()) {
                    AssistantMessageBubble(
                        text = item.content,
                        onRegenerate = onRegenerate,
                        onDelete = onDeleteMessage,
                    )
                }
                // 推理文本（如有）— 默认折叠，点击展开
                if (!item.reasoning.isNullOrBlank()) {
                    ReasoningBlock(text = item.reasoning, isStreaming = isStreaming)
                }
            }
        }

        // 2026-08-06 对齐 RikkaHub 层次结构：一轮回复 = 有序块序列，严格按 blocks 顺序渲染
        // （推理折叠 → 正文平铺 → 工具折叠 → 推理折叠 → …，不跳位）
        is ChatItem.AssistantTurn -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 2026-08-08：连续 Tool 块聚合为 ToolStepsCard（满屏工具卡 → 一轮一张折叠卡）
                var toolAccumulator = mutableListOf<TurnBlock.Tool>()
                @Composable
                fun flushTools() {
                    if (toolAccumulator.isNotEmpty()) {
                        ToolStepsCard(blocks = toolAccumulator.toList(), isStreaming = isStreaming)
                        toolAccumulator.clear()
                    }
                }
                item.blocks.forEach { block ->
                    when (block) {
                        is TurnBlock.Reasoning -> {
                            flushTools()
                            if (block.text.isNotBlank()) {
                                ReasoningBlock(text = block.text, isStreaming = isStreaming)
                            }
                        }

                        is TurnBlock.Text -> {
                            flushTools()
                            if (block.text.isNotBlank()) {
                                AssistantMessageBubble(
                                    text = block.text,
                                    onRegenerate = onRegenerate,
                                    onDelete = onDeleteMessage,
                                )
                            }
                        }

                        is TurnBlock.Tool -> {
                            toolAccumulator.add(block)
                        }
                    }
                }
                flushTools()
            }
        }

        // 工具卡片：保持平铺（在正文下方、按 SSE 顺序排列），默认折叠可展开
        is ChatItem.ToolCard -> {
            ToolCard(
                id = item.id,
                name = item.name,
                args = item.args,
                output = item.output,
                err = item.err,
                truncated = item.truncated,
                isRunning = item.isRunning,
            )
        }

        is ChatItem.SystemNotice -> {
            SystemNotice(
                text = item.text,
                isWarning = item.isWarning,
            )
        }

        is ChatItem.ErrorMessage -> {
            ErrorMessage(text = item.text)
        }

        is ChatItem.PhaseIndicator -> {
            PhaseIndicator(text = item.text)
        }

        is ChatItem.UsageStats -> {
            UsageStatsRow(usage = item.usage, balance = balance, cumulativeTokens = cumulativeTokens)
        }

        is ChatItem.CompactionNotice -> {
            CompactionNoticeCard(item)
        }

        is ChatItem.ApprovalCard -> {
            if (onApprove != null && onDeny != null) {
                ApprovalCard(
                    id = item.id,
                    tool = item.tool,
                    subject = item.subject,
                    onAllow = onApprove,
                    onDeny = onDeny,
                )
            }
        }

        is ChatItem.AskCard -> {
            if (onAskSubmit != null) {
                AskCard(
                    id = item.id,
                    questions = item.questions,
                    onSubmit = onAskSubmit,
                )
            }
        }
    }
}

/**
 * 压缩通知卡片
 */
@Composable
private fun CompactionNoticeCard(notice: ChatItem.CompactionNotice) {
    // 2026-08-07：浅色模式修复——压缩卡改用 LocalPalette 动态取色（不再硬编码暗色）
    val p = LocalPalette.current
    val bg2 = p.bg2
    val muted = p.muted
    val border = p.border
    val fg2 = p.fg2

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = bg2,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "压缩",
                color = muted,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )
            if (notice.trigger != null) {
                Text(
                    text = "触发: ${notice.trigger}",
                    color = fg2,
                    fontSize = 12.sp,
                )
            }
            if (notice.summary != null) {
                Text(
                    text = notice.summary,
                    color = fg2,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = "${notice.messages} 条消息已压缩",
                color = muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

package com.reasonix.agents.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.model.ChatItem
import com.reasonix.agents.data.model.TurnBlock
import com.reasonix.agents.ui.theme.LocalPalette

// 匹配 index.html 调色板
private val bg: Color @Composable get() = LocalPalette.current.bg
private val border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted

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
    // 2026-08-08：消息区显示 AI/用户名称（AI 名 = 服务端 label，用户 = 本地资料 displayName）
    assistantName: String? = null,
    userName: String? = null,
    // 2026-08-07：注入上下文折叠卡（系统提示词/用户提示词/记忆）
    systemPrompt: String? = null,
    userPrompt: String = "",
    memoryText: String? = null,
    onSaveUserPrompt: (String) -> Unit = {},
    onSaveMemory: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 2026-08-08：首次进入（恢复历史）强制跳到最后一条记录；之后保持「靠近底部才跟随」逻辑
    var didInitialScroll by remember { mutableStateOf(false) }

    // 新消息到达时自动滚到底部（流式中 instant 不蹦跳；离开底部不抢夺）
    LaunchedEffect(items.size, isStreaming) {
        if (items.isNotEmpty()) {
            if (!didInitialScroll) {
                listState.scrollToItem(items.size - 1)
                didInitialScroll = true
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val nearBottom = lastVisible >= items.size - 3
                if (nearBottom) {
                    if (isStreaming) {
                        listState.scrollToItem(items.size - 1)
                    } else {
                        listState.animateScrollToItem(items.size - 1)
                    }
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .background(bg),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        assistantName = assistantName,
                        userName = userName,
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

        // 2026-08-08：消息足够多时显示右侧快速导航——垂直滑动条（拖动跳转）+ 跳最上/最下按钮
        if (items.size > 8) {
            val total = items.size
            val firstVisible = listState.firstVisibleItemIndex.coerceIn(0, total - 1)
            var barHeightPx by remember { mutableIntStateOf(0) }
            val thumbSizePx = with(density) { 14.dp.toPx() }
            val thumbOffsetPx =
                if (barHeightPx > 0 && total > 1) {
                    ((barHeightPx - thumbSizePx) * (firstVisible.toFloat() / (total - 1).toFloat())).toInt()
                } else {
                    0
                }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .fillMaxHeight(0.94f)
                        .width(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.size(22.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "跳转到最上",
                        tint = Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                // 垂直快速滑动条：按下/拖动按 y 比例跳转到对应消息
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.10f))
                                .onSizeChanged { barHeightPx = it.height }
                                .pointerInput(total) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val y = event.changes.firstOrNull()?.position?.y ?: continue
                                            val target =
                                                ((y / size.height) * total)
                                                    .toInt()
                                                    .coerceIn(0, total - 1)
                                            scope.launch { listState.scrollToItem(target) }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                },
                    )
                    Box(
                        modifier =
                            Modifier
                                .offset { IntOffset(0, thumbOffsetPx) }
                                .padding(horizontal = 6.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.7f)),
                    )
                }
                IconButton(
                    onClick = { scope.launch { listState.animateScrollToItem(items.size - 1) } },
                    modifier = Modifier.size(22.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "跳转到最下",
                        tint = Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
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
    assistantName: String? = null,
    userName: String? = null,
    onApprove: ((session: Boolean, persist: Boolean, scope: String) -> Unit)?,
    onDeny: (() -> Unit)?,
    onAskSubmit: ((List<Map<String, String>>) -> Unit)?,
    onRegenerate: (() -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
) {
    when (item) {
        is ChatItem.UserMessage -> {
            UserMessageBubble(text = item.content, imagePath = item.imagePath, userName = userName)
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
                // 2026-08-08：AI 名称行（名称下方依次：思考卡 → 工具卡 → 正文）
                if (!assistantName.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Accent),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = assistantName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // 2026-08-08：全轮 Tool 块聚合为【一个】ToolStepsCard；正文块聚合到工具卡之后渲染，
                // 满足「思考卡 → 工具命令卡 → 正文」的固定层次（原为正文在工具前，随 blocks 顺序穿插）
                val toolAccumulator = mutableListOf<TurnBlock.Tool>()
                val textAccumulator = mutableListOf<String>()
                item.blocks.forEach { block ->
                    when (block) {
                        is TurnBlock.Reasoning -> {
                            // 2026-08-08：思考中（text 尚空）也渲染——显示「思考中 · Ns」而非整体消失
                            if (block.text.isNotBlank() || isStreaming) {
                                ReasoningBlock(text = block.text, isStreaming = isStreaming)
                            }
                        }

                        is TurnBlock.Text -> {
                            if (block.text.isNotBlank()) {
                                textAccumulator.add(block.text)
                            }
                        }

                        is TurnBlock.Tool -> {
                            toolAccumulator.add(block)
                        }
                    }
                }
                if (toolAccumulator.isNotEmpty()) {
                    ToolStepsCard(blocks = toolAccumulator.toList(), isStreaming = isStreaming)
                }
                // 正文统一在思考卡/工具卡之后渲染
                textAccumulator.forEach { t ->
                    AssistantMessageBubble(
                        text = t,
                        onRegenerate = onRegenerate,
                        onDelete = onDeleteMessage,
                    )
                }
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

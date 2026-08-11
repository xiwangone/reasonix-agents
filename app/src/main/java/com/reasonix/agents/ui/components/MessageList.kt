package com.reasonix.agents.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.Dp
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

            itemsIndexed(items, key = { index, item -> item.id.ifBlank { "msg_$index" } }) { index, item ->
                // 2026-08-09：入场动画 = 淡入 + 轻微上滑（1/4 高度）——新消息「浮现」而非硬出现；
                // 分组视觉：同轮助手内容紧凑（4dp）、轮间/新用户轮稍大（12~14dp），见 gapBefore
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(240)) + slideInVertically(animationSpec = tween(240), initialOffsetY = { it / 4 }),
                    exit = fadeOut(tween(160)),
                    modifier = Modifier.padding(top = gapBefore(items, index)),
                ) {
                    ChatItemRow(
                        assistantName = assistantName,
                        userName = userName,
                        item = item,
                        balance = balance,
                        isStreaming = isStreaming,
                        // 2026-08-09：最后一条且流式生成中 → 正文尾部显示闪烁光标
                        streamCursor = isStreaming && index == items.lastIndex,
                        onApprove = onApprove,
                        onDeny = onDeny,
                        onAskSubmit = onAskSubmit,
                        cumulativeTokens = cumulativeTokens,
                        onRegenerate = onRegenerate,
                        onDeleteMessage = onDeleteMessage,
                    )
                }
            }

            // 2026-08-09：流式生成中，列表底部显示「正在思考…」呼吸微光提示
            if (isStreaming) {
                item(key = "streaming_thinking") {
                    StreamingThinkingHint()
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

// ═══════════════════════════════════════════════
// 分组视觉：同一轮内紧凑、轮与轮之间留白
// ═══════════════════════════════════════════════

/** 判断两个相邻 ChatItem 是否属于同一侧（都是助手侧 / 都是用户侧）。 */
private fun sameSide(a: ChatItem, b: ChatItem): Boolean =
    (a is ChatItem.UserMessage) == (b is ChatItem.UserMessage)

/**
 * 第 [index] 条消息相对上一条的顶部间距：
 * - 助手侧连续（思考/工具/正文/用量同一轮）→ 4dp 紧凑
 * - 用户 → 助手（轮内回复开始）→ 10dp
 * - 助手 → 用户（新轮开始）→ 14dp 留白
 * - 用户连续两条（各自独立请求）→ 12dp
 */
private fun gapBefore(items: List<ChatItem>, index: Int): Dp {
    if (index == 0) return 0.dp
    val prev = items[index - 1]
    val cur = items[index]
    val prevUser = prev is ChatItem.UserMessage
    val curUser = cur is ChatItem.UserMessage
    return when {
        sameSide(prev, cur) && !curUser -> 4.dp // 助手侧同轮
        prevUser && curUser -> 12.dp // 用户独立轮次
        prevUser -> 10.dp // 用户 → 助手回复
        else -> 14.dp // 助手 → 新用户轮
    }
}

/** 流式生成中底部「正在思考…」——alpha 0.4→1.0 呼吸微光。 */
@Composable
private fun StreamingThinkingHint() {
    val alpha by
        rememberInfiniteTransition(label = "thinkingPulse").animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
            label = "thinkingAlpha",
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = alpha)),
        )
        Text(
            text = "正在思考…",
            color = Accent.copy(alpha = alpha),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ChatItemRow(
    item: ChatItem,
    balance: String? = null,
    cumulativeTokens: Long = 0,
    isStreaming: Boolean = false,
    // 2026-08-09：该消息为最后一条流式消息（正文尾部显示闪烁光标）
    streamCursor: Boolean = false,
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
            UserMessageBubble(text = item.content, imagePaths = item.imagePaths, userName = userName)
        }

        is ChatItem.AssistantMessage -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 2026-08-06：对齐 RikkaHub —— 正文在上正常展示，推理折叠块在下方
                // 助手正文（Markdown）
                if (item.content.isNotBlank()) {
                    AssistantMessageBubble(
                        text = item.content,
                        showStreamCursor = streamCursor,
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
                // 正文统一在思考卡/工具卡之后渲染；流式光标只挂在最后一个文本块尾部
                textAccumulator.forEachIndexed { i, t ->
                    AssistantMessageBubble(
                        text = t,
                        showStreamCursor = streamCursor && i == textAccumulator.lastIndex,
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

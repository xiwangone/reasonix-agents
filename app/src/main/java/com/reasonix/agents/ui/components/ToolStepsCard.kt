package com.reasonix.agents.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.model.TurnBlock
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.theme.ToolNames

// ═══════════════════════════════════════════════
// ToolStepsCard — 一轮对话的工具步骤合并卡
// ═══════════════════════════════════════════════

private val CardBg: Color @Composable get() = LocalPalette.current.card
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Success: Color @Composable get() = LocalPalette.current.success
private val Danger: Color @Composable get() = LocalPalette.current.danger
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val SuccessSoft: Color @Composable get() = LocalPalette.current.successS
private val DangerSoft: Color @Composable get() = LocalPalette.current.dangerS
private val AccentSoft: Color @Composable get() = LocalPalette.current.accentS

/** 折叠态最多展示的工具名数量（超出显示 +N） */
private const val MAX_VISIBLE_NAMES = 3

/**
 * 工具步骤合并卡 —— 把一轮回复中连续的工具调用块合并为一张卡。
 *
 * 解决「一轮对话满屏工具卡，翻消息翻半天」的痛点：
 * - **折叠态**：一行显示「N 个工具步骤」+ 最新工具状态图标 + 前 [MAX_VISIBLE_NAMES] 个工具名
 * - **展开态**：按序展示每一步（名称 + 参数 + 输出/错误，带状态色），点击可复制输出
 *
 * @param blocks 一轮中连续的工具块（按 SSE 顺序）
 * @param isStreaming 是否仍在流式生成（最新一步运行中则显示旋转态）
 */
@Composable
fun ToolStepsCard(
    blocks: List<TurnBlock.Tool>,
    isStreaming: Boolean = false,
) {
    if (blocks.isEmpty()) return

    // 折叠状态——remember 不跨重组保持：滚动回收/位置复用时自动回折叠（默认折叠）
    var expanded by remember { mutableStateOf(false) }
    // 2026-08-09：箭头旋转过渡（展开 180°），折叠/展开不生硬
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "arrowRotation",
    )

    val running = isStreaming && blocks.any { it.isRunning }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
    ) {
        Column {
            // ═══════════ 头部（一行） ═══════════
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 状态点：运行中=accent / 有失败=红 / 否则绿
                StatusDot(running = running, hasError = blocks.any { it.err != null })

                // 主标题：N 个工具步骤
                Text(
                    text = if (blocks.size == 1) "1 个工具步骤" else "${blocks.size} 个工具步骤",
                    color = Fg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.weight(1f))

                // 折叠箭头（旋转过渡：折叠=向下 0° → 展开=向上 180°）
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Muted,
                    modifier = Modifier.size(20.dp).rotate(arrowRotation),
                )
            }

            // 折叠态：工具名摘要行（点击展开）——AnimatedVisibility 平滑出现/收起
            AnimatedVisibility(
                visible = !expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(tween(160)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val shown = blocks.take(MAX_VISIBLE_NAMES)
                    val rest = blocks.size - shown.size
                    shown.forEachIndexed { index, block ->
                        Text(
                            text = ToolNames.display(block.name),
                            color = if (block.err != null) Danger else if (block.isRunning) Accent else Success,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (index < shown.lastIndex) {
                            Text(text = "·", color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (rest > 0) {
                        Text(text = "+$rest", color = Muted, fontSize = 12.sp)
                    }
                }
            }

            // ═══════════ 展开态：全部步骤（expandVertically + fadeIn 过渡） ═══════════
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(tween(160)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    blocks.forEach { block ->
                        ToolStepRow(block = block)
                    }
                }
            }
        }
    }
}

/** 单步工具行：状态圆点 + 名称 + 参数 + 输出/错误截断 */
@Composable
private fun ToolStepRow(block: TurnBlock.Tool) {
    val status =
        when {
            block.isRunning -> StepStatus.RUNNING
            block.err != null -> StepStatus.ERROR
            else -> StepStatus.SUCCESS
        }
    val statusColor =
        when (status) {
            StepStatus.RUNNING -> Accent
            StepStatus.ERROR -> Danger
            StepStatus.SUCCESS -> Success
        }
    val statusBg =
        when (status) {
            StepStatus.RUNNING -> AccentSoft
            StepStatus.ERROR -> DangerSoft
            StepStatus.SUCCESS -> SuccessSoft
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // 名称行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(statusBg, RoundedCornerShape(4.dp)),
            )
            Text(
                text = ToolNames.display(block.name),
                color = Fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (!block.args.isNullOrBlank()) {
                Text(
                    text = block.args,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        // 输出/错误（等宽截断，最多 3 行）
        val body = block.err ?: block.output
        if (!body.isNullOrBlank()) {
            Text(
                text = body.take(600) + if (body.length > 600) "\n…" else "",
                color = Fg2,
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 16.dp),
            )
        }
    }
}

private enum class StepStatus { RUNNING, SUCCESS, ERROR }

/** 头部状态点：运行中「…」/ 有错误「!」/ 成功「✓」 */
@Composable
private fun StatusDot(
    running: Boolean,
    hasError: Boolean,
) {
    val (bg, fg) =
        when {
            running -> AccentSoft to Accent
            hasError -> DangerSoft to Danger
            else -> SuccessSoft to Success
        }
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .background(bg, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (running) "…" else if (hasError) "!" else "✓",
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

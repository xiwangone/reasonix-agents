package com.reasonix.agents.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.model.AskQuestion
import com.reasonix.agents.ui.theme.LocalPalette

// ═══════════════════════════════════════════════
// 调色板 (Reasonix 暗色主题)
// ═══════════════════════════════════════════════

private val panel: Color @Composable get() = LocalPalette.current.panel
private val panel2: Color @Composable get() = LocalPalette.current.panel2
private val bg2: Color @Composable get() = LocalPalette.current.bg2
private val card: Color @Composable get() = LocalPalette.current.card
private val border: Color @Composable get() = LocalPalette.current.border
private val borderStrong: Color @Composable get() = LocalPalette.current.borderStr
private val accent: Color @Composable get() = LocalPalette.current.accent
private val accentSoft: Color @Composable get() = LocalPalette.current.accentS
private val fg: Color @Composable get() = LocalPalette.current.fg
private val fg2: Color @Composable get() = LocalPalette.current.fg2
private val muted: Color @Composable get() = LocalPalette.current.muted
private val muted2: Color @Composable get() = LocalPalette.current.muted2
private val warning: Color @Composable get() = LocalPalette.current.warning
private val warningSoft: Color @Composable get() = LocalPalette.current.warningS
private val danger: Color @Composable get() = LocalPalette.current.danger

// ═══════════════════════════════════════════════
// 1. ReasoningBlock
// ═══════════════════════════════════════════════

/**
 * 可折叠的推理过程展示面板。
 * 默认折叠，点击展开后以等宽字体沿左侧彩色边框滚动展示推理文本。
 *
 * @param text     推理文本内容
 * @param modifier 外部修饰符
 */
@Composable
fun ReasoningBlock(
    text: String,
    modifier: Modifier = Modifier,
    // 2026-08-07（设计稿 #5 推理三态折叠）：生成中状态 / 完成后自动收起
    isStreaming: Boolean = false,
    autoCollapse: Boolean = true,
) {
    // 三态折叠：预览（折叠·2 行渐隐）↔ 展开（全文滚动）
    var expanded by rememberSaveable { mutableStateOf(false) }
    // 生成中已思考秒数
    var elapsed by remember { mutableIntStateOf(0) }

    // 进入流式生成：默认展开实时可见（对标 Claude/Kimi 实时思考流）；
    // 计时（秒）。生成结束后保留定格耗时用于摘要标签。
    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            expanded = true
            elapsed = 0
            while (true) {
                delay(1000)
                elapsed++
            }
        }
    }

    // 生成结束 + 开启自动收起 → 回到摘要行（标题保留字数与耗时标签）
    LaunchedEffect(isStreaming, autoCollapse, text) {
        if (!isStreaming && autoCollapse && text.isNotBlank()) {
            expanded = false
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevronRotation",
    )
    // 生成中：内容区呼吸脉动（替代 shimmer，无额外依赖）
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isStreaming) 0.45f else 1f,
        animationSpec =
            if (isStreaming) {
                infiniteRepeatable(animation = tween(durationMillis = 650), repeatMode = RepeatMode.Reverse)
            } else {
                tween(durationMillis = 200)
            },
        label = "pulse",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // ── 切换按钮 ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "▶",
                modifier = Modifier.rotate(rotation),
                color = muted,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text =
                    when {
                        isStreaming && text.isBlank() -> "思考中 · ${elapsed}s"
                        isStreaming -> "思考 · ${text.length} 字 · ${elapsed}s"
                        text.isBlank() -> "思考中…"
                        // 完成后折叠为摘要行：字数 + 定格耗时（对标 Claude/Gemini 耗时标签）
                        else -> "思考 · ${text.length} 字 · ${elapsed}s"
                    },
                color = if (isStreaming) accent else muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (isStreaming) {
                Spacer(modifier = Modifier.width(6.dp))
                // 生成中圆点呼吸
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .background(accent, RoundedCornerShape(3.dp)),
                )
            }
        }

        // ── 内容区（预览 2 行 / 展开全文） ──
        if (text.isNotBlank()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(if (expanded) 280.dp else 44.dp)
                            .background(if (isStreaming) accent.copy(alpha = 0.5f) else border, RoundedCornerShape(1.dp)),
                )
                if (expanded) {
                    // 展开：全文滚动（可点标题收起）
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(max = 280.dp)
                                .padding(start = 8.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = fg2,
                            lineHeight = 20.sp,
                        )
                    }
                } else {
                    // 预览：2 行省略 + 底部渐隐
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                                .heightIn(min = 44.dp)
                                .clickable { expanded = true },
                    ) {
                        Text(
                            text = text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = fg2.copy(alpha = pulseAlpha),
                            lineHeight = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(if (expanded) 1f else 0.9f),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 2. ApprovalCard}

// ═══════════════════════════════════════════════
// 2. ApprovalCard
// ═══════════════════════════════════════════════

/**
 * 工具执行审批卡片。
 * 展示待审批的工具名称与操作对象，并提供四个审批级别按钮。
 *
 * @param id      审批请求 ID
 * @param tool    工具名称
 * @param subject 操作对象描述（可选）
 * @param onAllow 批准回调 — (session: Boolean, persist: Boolean, scope: String)
 * @param onDeny  拒绝回调
 */
@Composable
fun ApprovalCard(
    id: String,
    tool: String,
    subject: String? = null,
    onAllow: (session: Boolean, persist: Boolean, scope: String) -> Unit,
    onDeny: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = card),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── 标题行 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠",
                    fontSize = 16.sp,
                    color = warning,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "需要审批",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 工具信息区 ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = panel,
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = tool,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    if (!subject.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subject,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = muted,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 操作按钮组 ──
            // 第一行：Allow (Y) + Allow for session (A)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ApprovalButton(
                    label = "允许",
                    shortcut = "Y",
                    backgroundColor = accent,
                    onClick = { onAllow(false, false, "once") },
                    modifier = Modifier.weight(1f),
                )
                ApprovalButton(
                    label = "本次会话允许",
                    shortcut = "A",
                    backgroundColor = accent,
                    onClick = { onAllow(true, false, "session") },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：Always allow (P) + Deny (N)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ApprovalButton(
                    label = "始终允许",
                    shortcut = "P",
                    backgroundColor = accent,
                    onClick = { onAllow(false, true, "persist") },
                    modifier = Modifier.weight(1f),
                )
                ApprovalButton(
                    label = "拒绝",
                    shortcut = "N",
                    backgroundColor = Color.Transparent,
                    borderColor = muted2,
                    onClick = onDeny,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 审批按钮组件。
 */
@Composable
private fun ApprovalButton(
    label: String,
    shortcut: String,
    backgroundColor: Color,
    borderColor: Color = backgroundColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor.copy(alpha = if (backgroundColor == Color.Transparent) 0f else 0.15f),
        border =
            if (borderColor != backgroundColor || backgroundColor == Color.Transparent) {
                androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (backgroundColor == Color.Transparent) muted else accent,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color =
                    if (backgroundColor == Color.Transparent) {
                        muted2.copy(alpha = 0.15f)
                    } else {
                        accent.copy(alpha = 0.2f)
                    },
            ) {
                Text(
                    text = shortcut,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (backgroundColor == Color.Transparent) muted else accent,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 3. AskCard
// ═══════════════════════════════════════════════

/**
 * 向用户提问的卡片组件。
 * 支持单选和多选问题，选中项以 accent 色高亮，底部提供提交按钮。
 *
 * @param id        提问请求 ID
 * @param questions 问题列表（[AskQuestion]）
 * @param onSubmit  提交回调，传递每个问题的选中选项列表
 */
@Composable
fun AskCard(
    id: String,
    questions: List<AskQuestion>,
    onSubmit: (answers: List<Map<String, String>>) -> Unit,
) {
    val selections =
        remember(questions) {
            questions.map { q -> mutableStateListOf<String>() }
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = card),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── 标题 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "提问",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                )
            }

            // ── 问题列表 ──
            questions.forEachIndexed { qIndex, question ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question.prompt,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                )
                val modeLabel = if (question.multi) " (多选)" else " (单选)"
                Text(
                    text = modeLabel,
                    fontSize = 11.sp,
                    color = muted2,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // ── 选项列表 ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.options.forEach { option ->
                        val isSelected = option.label in selections[qIndex]
                        val selectedBg by animateColorAsState(
                            targetValue = if (isSelected) accentSoft else Color.Transparent,
                            animationSpec = tween(durationMillis = 150),
                            label = "optionBg",
                        )
                        val selectedBorder by animateColorAsState(
                            targetValue = if (isSelected) accent else border,
                            animationSpec = tween(durationMillis = 150),
                            label = "optionBorder",
                        )

                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (question.multi) {
                                            if (isSelected) {
                                                selections[qIndex].remove(option.label)
                                            } else {
                                                selections[qIndex].add(option.label)
                                            }
                                        } else {
                                            selections[qIndex].clear()
                                            selections[qIndex].add(option.label)
                                        }
                                    },
                            shape = RoundedCornerShape(8.dp),
                            color = selectedBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, selectedBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) accent else fg2,
                                )
                                if (!option.description.isNullOrBlank()) {
                                    Text(
                                        text = option.description,
                                        fontSize = 12.sp,
                                        color = muted,
                                        lineHeight = 17.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 提交按钮 ──
            val hasSelection = selections.any { it.isNotEmpty() }
            Button(
                onClick = {
                    val answers =
                        selections.mapIndexed { index, selectedLabels ->
                            mapOf(
                                "id" to questions[index].id,
                                "answers" to selectedLabels.joinToString(","),
                            )
                        }
                    onSubmit(answers)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = fg,
                        disabledContainerColor = accent.copy(alpha = 0.3f),
                        disabledContentColor = fg.copy(alpha = 0.4f),
                    ),
                enabled = hasSelection,
            ) {
                Text(
                    text = "提交",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

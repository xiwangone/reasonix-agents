package com.reasonix.agents.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reasonix.agents.data.model.UsagePayload
import com.reasonix.agents.R
import com.reasonix.agents.ui.theme.LocalPalette
import java.io.File
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

// ═══════════════════════════════════════════════
// 调色板（与深色主题对齐）
// ═══════════════════════════════════════════════

private val accent: Color @Composable get() = LocalPalette.current.accent
private val fg: Color @Composable get() = LocalPalette.current.fg
private val fg2: Color @Composable get() = LocalPalette.current.fg2
private val muted: Color @Composable get() = LocalPalette.current.muted
private val muted2: Color @Composable get() = LocalPalette.current.muted2
private val danger: Color @Composable get() = LocalPalette.current.danger
private val dangerSoft: Color @Composable get() = LocalPalette.current.dangerS
private val success: Color @Composable get() = LocalPalette.current.success
private val border: Color @Composable get() = LocalPalette.current.border
private val bg2: Color @Composable get() = LocalPalette.current.bg2
private val panel2: Color @Composable get() = LocalPalette.current.panel2
private val userBubbleBg: Color @Composable get() = LocalPalette.current.accent
private val userBubbleFg = Color(0xFFFFFFFF)

// ═══════════════════════════════════════════════
// 格式化工具
// ═══════════════════════════════════════════════

private fun fmtTok(n: Long): String = if (n >= 1000) "%.1fk".format(n / 1000.0) else "$n"

private fun fmtCost(
    costUsd: Double?,
    cost: Double?,
): String? {
    val v = costUsd ?: cost ?: return null
    return when {
        v >= 1.0 -> "$${"%.2f".format(v)}"
        v >= 0.01 -> "$${"%.4f".format(v)}"
        else -> "$${"%.6f".format(v)}"
    }
}

// ═══════════════════════════════════════════════
// 1. UserMessageBubble
// ═══════════════════════════════════════════════

/**
 * 用户消息气泡：右对齐，橙底白字圆角气泡，顶部带有复制按钮。
 * 第六批：图片发送——[imagePath] 非空时在文字上方展示本地图片（OCR 识别文字）。
 */
@Composable
fun UserMessageBubble(
    text: String,
    imagePath: String? = null,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        // 2026-08-06：操作行（对齐 RikkaHub）——复制 + 分享
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(text)) },
                modifier =
                    Modifier
                        .padding(end = 12.dp)
                        .size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                    tint = muted,
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(
                onClick = {
                    val sendIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                    context.startActivity(Intent.createChooser(sendIntent, "分享消息"))
                },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = muted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        // 气泡
        Box(
            modifier =
                Modifier
                    .padding(start = 64.dp, top = 2.dp, bottom = 6.dp, end = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(userBubbleBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // 图片（本地缓存文件，coil 异步加载；气泡 wrap-content，用固定宽度限宽）
                if (imagePath != null) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "发送的图片",
                        modifier =
                            Modifier
                                .width(240.dp)
                                .heightIn(max = 260.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22000000)),
                    )
                    if (text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                // 文字（OCR 识别文本；发送原图时为空）
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = userBubbleFg,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 2. AssistantMessageBubble
// ═══════════════════════════════════════════════

/**
 * 助手消息气泡：左对齐，Markdown 富文本渲染 + 右上角复制按钮。
 */
@Composable
fun AssistantMessageBubble(
    text: String,
    modifier: Modifier = Modifier,
    onRegenerate: (() -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    // 2026-08-07：操作行扩展——复制 / 刷新 / ⋯更多（分享含导出、添加收藏、翻译、删除）
    // toast 文案需在 @Composable 作用域预解析（onClick 内不能调 stringResource）
    val translateComingSoon = stringResource(R.string.toast_translate_coming_soon)
    val favoritedToast = stringResource(R.string.toast_favorited)
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
    ) {
        // 2026-08-06：操作行（对齐 RikkaHub 消息操作行）——复制 + 分享（本地能力，无需服务端）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            // 复制
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(text)) },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                    tint = muted,
                    modifier = Modifier.size(17.dp),
                )
            }
            // 刷新（重新生成）
            if (onRegenerate != null) {
                IconButton(
                    onClick = onRegenerate,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = muted,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            // ⋯更多（DropdownMenu）
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                        tint = muted,
                        modifier = Modifier.size(17.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    // 分享（含导出为文件）
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            shareMessage(context, text)
                        },
                    )
                    // 导出为文件（txt，走系统分享）
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export_file), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            exportMessage(context, text)
                        },
                    )
                    // 添加收藏（存本地文件）
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_favorite), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            favoriteMessage(context, text, favoritedToast)
                        },
                    )
                    // 翻译（占位：提示开发中）
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_translate), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Translate, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuExpanded = false
                            android.widget.Toast
                                .makeText(context, translateComingSoon, android.widget.Toast.LENGTH_SHORT)
                                .show()
                        },
                    )
                    // 删除
                    if (onDelete != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), fontSize = 13.sp, color = Color(0xFFE5484D)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color(0xFFE5484D)) },
                            onClick = {
                                menuExpanded = false
                                onDelete(text)
                            },
                        )
                    }
                }
            }
        }

        // Markdown 正文（Markwon 原生引擎：支持 HTML / 图片 / 表格 / 任务列表）
        MarkdownRenderer(
            markdown = text,
            codeBackground = bg2,
            codeTextColor = fg2,
            linkColor = accent,
        )
    }
}

/** 2026-08-07：分享消息文本（系统分享面板）。 */
private fun shareMessage(context: Context, text: String) {
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    context.startActivity(Intent.createChooser(sendIntent, "分享消息"))
}

/** 2026-08-07：导出消息为 txt 文件（写入 cacheDir 后走系统分享）。 */
private fun exportMessage(context: Context, text: String) {
    try {
        val file = File(context.cacheDir, "message_${System.currentTimeMillis()}.txt")
        file.writeText(text)
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                ))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(sendIntent, "导出消息"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** 2026-08-07：收藏消息到本地（应用 files/favorites/ 目录，追加 JSONL）。 */
private fun favoriteMessage(context: Context, text: String, favoritedToast: String) {
    try {
        val dir = File(context.filesDir, "favorites").apply { mkdirs() }
        val file = File(dir, "favorites.jsonl")
        val entry =
            org.json.JSONObject().apply {
                put("ts", System.currentTimeMillis())
                put("text", text)
            }
        file.appendText(entry.toString() + "\n")
        android.widget.Toast.makeText(context, favoritedToast, android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "收藏失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ═══════════════════════════════════════════════
// 3. SystemNotice
// ═══════════════════════════════════════════════

/**
 * 系统通知：左侧 2dp 色条 + 小字正文。
 * 警告模式使用 warning 色条和 "!" 前缀。
 */
@Composable
fun SystemNotice(
    text: String,
    isWarning: Boolean = false,
) {
    val borderColor = if (isWarning) accent else muted
    val prefix = if (isWarning) "! " else ""

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(borderColor, RoundedCornerShape(1.dp)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$prefix$text",
            color = if (isWarning) fg2 else muted,
            fontSize = 13.sp,
            lineHeight = 22.sp,
        )
    }
}

// ═══════════════════════════════════════════════
// 4. ErrorMessage
// ═══════════════════════════════════════════════

/**
 * 错误消息：红色左边框 + 微红背景 + 小字。
 */
@Composable
fun ErrorMessage(text: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(dangerSoft)
                .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(danger, RoundedCornerShape(1.dp)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = danger,
            fontSize = 13.sp,
            lineHeight = 22.sp,
        )
    }
}

// ═══════════════════════════════════════════════
// 5. PhaseIndicator
// ═══════════════════════════════════════════════

/**
 * 阶段指示器：居中、大写、等宽、小号、低调色。
 */
@Composable
fun PhaseIndicator(text: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = muted,
            letterSpacing = 1.sp,
        )
    }
}

// ═══════════════════════════════════════════════
// 6. UsageStatsRow
// ═══════════════════════════════════════════════

/**
 * 用量统计卡片：醒目展示本轮费用 + 剩余余额。
 * 上排：本轮费用（橙色） ｜ 余额（绿色）
 * 下排：Token 明细（Total / In / Out） + 缓存命中率
 */
@Composable
fun UsageStatsRow(
    usage: UsagePayload,
    balance: String? = null,
    cumulativeTokens: Long = 0,
) {
    val total = usage.totalTokens
    val prompt = usage.promptTokens
    val completion = usage.completionTokens
    val cacheHit = usage.cacheHitTokens
    val cacheMiss = usage.cacheMissTokens
    val costStr = fmtCost(usage.costUsd, usage.cost)

    // 计算缓存命中率
    val cacheTotal = cacheHit + cacheMiss
    val cachePercent: String? =
        if (cacheTotal > 0) {
            val pct = cacheHit.toDouble() / cacheTotal * 100.0
            if (pct >= 1.0) "${pct.toInt()}%" else "%.1f%%".format(pct)
        } else {
            null
        }

    // 卡片容器
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg2)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        // ── 上排：费用 + 余额 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 本轮费用
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "本轮",
                    color = muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = costStr ?: "-",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // 剩余余额
            if (balance != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "余额",
                        color = muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = balance,
                        color = success,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── 下排：Token 明细 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "T:${fmtTok(total)}",
                color = muted2,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "入:${fmtTok(prompt)}",
                color = muted2,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "出:${fmtTok(completion)}",
                color = muted2,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )

            // 缓存命中率
            if (cachePercent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "缓存 $cachePercent",
                    color = if (cacheHit > cacheMiss) success else muted2,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // 2026-08-07：累计统计（会话级）——本轮 vs 累计
        if (cumulativeTokens > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = border.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "累计",
                    color = muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fmtTok(cumulativeTokens),
                    color = fg2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "本轮 ${fmtTok(total)}",
                    color = muted2,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

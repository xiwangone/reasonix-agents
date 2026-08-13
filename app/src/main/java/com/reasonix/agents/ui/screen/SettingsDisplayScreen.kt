package com.reasonix.agents.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * 显示设置二级界面（第四批：设置组件化）。
 * 从设置页「显示」入口进入，含推理过程 / Token 费用显示开关。
 */
@Composable
fun SettingsDisplayScreen(
    settings: AppSettingsStore.Settings,
    onSettingsChange: (AppSettingsStore.Settings) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            // ── 顶栏（返回 + 标题）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Fg)
                }
                Text(
                    text = "显示",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 显示选项 ──
            SettingSwitch(
                title = "显示推理过程",
                checked = settings.showReasoning,
                onCheckedChange = { on ->
                    onSettingsChange(settings.copy(showReasoning = on))
                },
            )
            SettingSwitch(
                title = "显示 Token / 费用",
                checked = settings.showTokens,
                onCheckedChange = { on ->
                    onSettingsChange(settings.copy(showTokens = on))
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 聊天字体（2026-08-06：RikkaHub ChatFontFamily 适配）──
            SectionTitle("聊天字体")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FontChip(
                    "默认",
                    settings.chatFont == AppSettingsStore.CHAT_FONT_DEFAULT,
                    { onSettingsChange(settings.copy(chatFont = AppSettingsStore.CHAT_FONT_DEFAULT)) },
                )
                FontChip(
                    "衬线",
                    settings.chatFont == AppSettingsStore.CHAT_FONT_SERIF,
                    { onSettingsChange(settings.copy(chatFont = AppSettingsStore.CHAT_FONT_SERIF)) },
                )
                FontChip(
                    "等宽",
                    settings.chatFont == AppSettingsStore.CHAT_FONT_MONO,
                    { onSettingsChange(settings.copy(chatFont = AppSettingsStore.CHAT_FONT_MONO)) },
                )
                FontChip(
                    "JetBrains Mono",
                    settings.chatFont == AppSettingsStore.CHAT_FONT_JETBRAINS,
                    { onSettingsChange(settings.copy(chatFont = AppSettingsStore.CHAT_FONT_JETBRAINS)) },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "默认：系统字体；衬线/等宽：系统内置；JetBrains Mono：内置编程字体（RikkaHub 附带）。",
                fontSize = 10.sp,
                color = Muted2,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 自动压缩（2026-08-13，仿 RikkaHub Agents 双模式）──
            SectionTitle("自动压缩")
            Text(
                text = "上下文用量达到阈值时自动询问是否压缩，降低 token 消耗。",
                fontSize = 11.sp,
                color = Muted2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AutoCompactChip(
                    "关闭",
                    settings.autoCompactMode == AppSettingsStore.AUTO_COMPACT_OFF,
                    { onSettingsChange(settings.copy(autoCompactMode = AppSettingsStore.AUTO_COMPACT_OFF)) },
                )
                AutoCompactChip(
                    "按百分比",
                    settings.autoCompactMode == AppSettingsStore.AUTO_COMPACT_PERCENT,
                    { onSettingsChange(settings.copy(autoCompactMode = AppSettingsStore.AUTO_COMPACT_PERCENT)) },
                )
                AutoCompactChip(
                    "按 Token 数",
                    settings.autoCompactMode == AppSettingsStore.AUTO_COMPACT_TOKEN,
                    { onSettingsChange(settings.copy(autoCompactMode = AppSettingsStore.AUTO_COMPACT_TOKEN)) },
                )
            }
            if (settings.autoCompactMode == AppSettingsStore.AUTO_COMPACT_PERCENT) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "阈值: ${settings.autoCompactThreshold}%",
                        fontSize = 13.sp,
                        color = Fg,
                        modifier = Modifier.width(90.dp),
                    )
                    androidx.compose.material3.Slider(
                        value = settings.autoCompactThreshold.toFloat(),
                        onValueChange = { v ->
                            onSettingsChange(settings.copy(autoCompactThreshold = v.toInt().coerceIn(30, 95)))
                        },
                        valueRange = 30f..95f,
                        steps = 12,
                    )
                }
            } else if (settings.autoCompactMode == AppSettingsStore.AUTO_COMPACT_TOKEN) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "阈值: ${settings.autoCompactThreshold / 1000}k",
                        fontSize = 13.sp,
                        color = Fg,
                        modifier = Modifier.width(90.dp),
                    )
                    androidx.compose.material3.Slider(
                        value = (settings.autoCompactThreshold / 1000).toFloat(),
                        onValueChange = { v ->
                            onSettingsChange(settings.copy(autoCompactThreshold = (v.toInt() * 1000).coerceIn(5000, 200000)))
                        },
                        valueRange = 5f..200f,
                        steps = 38,
                    )
                }
            }
        }
    }
}

/** 自动压缩模式选择 chip（2026-08-13） */
@Composable
private fun AutoCompactChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalPalette.current.accent
    val fg = LocalPalette.current.fg
    val border = LocalPalette.current.border
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accent else LocalPalette.current.panel,
        border = BorderStroke(1.dp, if (selected) accent else border),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = if (selected) Color.White else fg,
        )
    }
}

/** 字体选择 chip（2026-08-06 RikkaHub ChatFont 适配） */
@Composable
private fun FontChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalPalette.current.accent
    val fg = LocalPalette.current.fg
    val border = LocalPalette.current.border
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accent else LocalPalette.current.panel,
        border = BorderStroke(1.dp, if (selected) accent else border),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = if (selected) Color.White else fg,
        )
    }
}

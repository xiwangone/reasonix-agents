package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Accent,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/**
 * 2026-08-08：圆角卡片分组容器（自写实现，视觉对齐主流设置页观感）。
 * - 大圆角（16dp）卡片承载一组设置项，内部条目间以细分隔线区隔
 * - 顶部小标题（Accent 色，同 SectionTitle 风格）
 * - 不复制任何第三方实现（RikkaHub CardGroup 为 AGPL，避免许可传染）
 */
// 卡片组内「当前是否为首条」——用于条目间分隔线（首条前不加线）
private val LocalGroupFirstItem = compositionLocalOf { true }

@Composable
internal fun SettingCardGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Accent,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Bg2)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // 组内首个条目：不画上方分隔线
            CompositionLocalProvider(LocalGroupFirstItem provides true) {
                content()
            }
        }
    }
}

/** 卡片组内条目包装：非首条自动在顶部画细分隔线。 */
@Composable
internal fun ColumnScope.CardGroupItem(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isFirst = LocalGroupFirstItem.current
    CompositionLocalProvider(LocalGroupFirstItem provides false) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (!isFirst) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Border.copy(alpha = 0.6f),
                    thickness = 1.dp,
                )
            }
            content()
        }
    }
}

@Composable
internal fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    colorDot: Color? = null,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Accent else Panel)
                .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 2026-08-06 优化：主题预设实时预览——配色小圆点
            if (colorDot != null) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(colorDot)
                            .padding(end = 6.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (selected) Color.White else Muted,
            )
        }
    }
}

@Composable
internal fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Fg,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Fg2,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 可点击的 InfoRow（仓库链接跳转浏览器，批 A-6）。 */
@Composable
internal fun ClickableInfoRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(90.dp),
        )
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Accent,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Muted2, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
internal fun LabeledField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

/** 通用下拉（弹窗内小选项）。 */
@Composable
internal fun SimpleDropdown(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Panel)
                    .border(1.dp, Border, RoundedCornerShape(6.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontSize = 13.sp, color = Fg, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Panel,
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 13.sp, color = Fg) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    },
                )
            }
        }
    }
}

/** 兼容方式中文标签。 */
internal fun compatLabel(compat: String): String =
    when (compat) {
        "openai" -> "OpenAI"
        "deepseek" -> "DeepSeek-Reasonix"
        else -> "其他"
    }

/**
 * 设置入口卡片（第四批：设置组件化）。
 * 设置页一级界面只保留分组入口，点击进入对应二级设置界面。
 */
@Composable
internal fun SettingEntry(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    grouped: Boolean = false,
) {
    Row(
        modifier =
            if (grouped) {
                // 2026-08-08：卡片组内条目——背景/边框由父级 SettingCardGroup 提供，仅保留点击区与内边距
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Bg2)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = Fg, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = Muted2, modifier = Modifier.padding(top = 1.dp))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted2, modifier = Modifier.size(16.dp))
    }
}

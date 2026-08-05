package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.CliIntegrationStore
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * CLI 集成二级界面（第五批 E-3）。
 *
 * - 开关：启用 / 禁用 reasonix 调用部署 CLI；
 * - 工具选择：aider / opencode / 全部；
 * - 工作目录（默认 /tmp）与调用超时（默认 120s）；
 * - 开关开启后，发送消息时自动向模型注入「可使用部署 CLI 工具完成任务」的指令。
 */
@Composable
fun SettingsCliScreen(
    cliSettings: CliIntegrationStore.CliSettings,
    onCliSettingsChange: (CliIntegrationStore.CliSettings) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding(),
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
                    text = "CLI 集成",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 开关 ──
            SettingSwitch(
                title = "启用 CLI 集成",
                checked = cliSettings.enabled,
                onCheckedChange = { on ->
                    onCliSettingsChange(cliSettings.copy(enabled = on))
                },
            )
            Text(
                "开启后，发送消息时将自动附带指令：你可使用部署的 CLI 工具（aide-wrap.sh / oc-wrap.sh）完成任务（注入提示词层）。",
                fontSize = 11.sp,
                color = Muted2,
                lineHeight = 16.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 工具选择 ──
            SectionTitle("工具选择")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CliToolChip("aider", cliSettings.tool == CliIntegrationStore.TOOL_AIDER) {
                    onCliSettingsChange(cliSettings.copy(tool = CliIntegrationStore.TOOL_AIDER))
                }
                CliToolChip("opencode", cliSettings.tool == CliIntegrationStore.TOOL_OPENCODE) {
                    onCliSettingsChange(cliSettings.copy(tool = CliIntegrationStore.TOOL_OPENCODE))
                }
                CliToolChip("全部", cliSettings.tool == CliIntegrationStore.TOOL_ALL) {
                    onCliSettingsChange(cliSettings.copy(tool = CliIntegrationStore.TOOL_ALL))
                }
            }
            Text(
                "选择「全部」时两条包装脚本均可用（aide-wrap.sh / oc-wrap.sh）。",
                fontSize = 11.sp,
                color = Muted2,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 工作目录 ──
            SectionTitle("工作目录")
            CliTextField(
                value = cliSettings.workdir,
                onValueChange = { v -> onCliSettingsChange(cliSettings.copy(workdir = v)) },
                hint = "/tmp",
                numeric = false,
            )
            Text(
                "CLI 工具的默认工作目录（默认 /tmp）。",
                fontSize = 11.sp,
                color = Muted2,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 超时 ──
            SectionTitle("调用超时（秒）")
            CliTextField(
                value = cliSettings.timeoutSec.toString(),
                onValueChange = { v ->
                    val num = v.filter { it.isDigit() }.take(4).toIntOrNull()
                    onCliSettingsChange(cliSettings.copy(timeoutSec = num ?: 0))
                },
                hint = "120",
                numeric = true,
            )
            Text(
                "CLI 工具调用超时时间（默认 120 秒，范围 10–3600）。",
                fontSize = 11.sp,
                color = Muted2,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 预览 ──
            if (cliSettings.enabled) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                ) {
                    Text(
                        text = "注入指令预览：\n「你可使用部署的 CLI 工具（${
                            when (cliSettings.tool) {
                                CliIntegrationStore.TOOL_AIDER -> "aide-wrap.sh"
                                CliIntegrationStore.TOOL_OPENCODE -> "oc-wrap.sh"
                                else -> "aide-wrap.sh / oc-wrap.sh"
                            }
                        }）完成任务。工作目录：${cliSettings.workdir.ifBlank { "/tmp" }}；调用超时：${cliSettings.timeoutSec.coerceIn(10, 3600)}s。」",
                        fontSize = 12.sp,
                        color = Fg2,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CliToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Accent.copy(alpha = 0.15f) else Panel)
                .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Accent else Muted,
        )
    }
}

@Composable
private fun CliTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    numeric: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Panel)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
            cursorBrush = SolidColor(Accent),
            singleLine = true,
            keyboardOptions =
                if (numeric) {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions(keyboardType = KeyboardType.Text)
                },
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, fontSize = 13.sp, color = Muted2)
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

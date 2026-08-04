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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.model.StatusInfo
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg @Composable get() = LocalPalette.current.bg
private val Bg2 @Composable get() = LocalPalette.current.bg2
private val Panel @Composable get() = LocalPalette.current.panel
private val Border @Composable get() = LocalPalette.current.border
private val Accent @Composable get() = LocalPalette.current.accent
private val Violet @Composable get() = LocalPalette.current.violet
private val Fg @Composable get() = LocalPalette.current.fg
private val Fg2 @Composable get() = LocalPalette.current.fg2
private val Muted @Composable get() = LocalPalette.current.muted
private val Muted2 @Composable get() = LocalPalette.current.muted2

/**
 * 设置页（全屏覆盖层）。
 * 含：主题切换、显示选项、服务器信息、关于。
 */
@Composable
fun SettingsScreen(
    serverUrl: String,
    status: StatusInfo?,
    models: List<ModelInfo> = emptyList(),
    currentModel: String = "",
    systemPrompt: String? = null,
    settings: AppSettingsStore.Settings,
    onThemeModeChange: (Int) -> Unit,
    onShowReasoningChange: (Boolean) -> Unit,
    onShowTokensChange: (Boolean) -> Unit,
    onModelSelect: (String) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── 标题栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设置",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable { onClose() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("关闭", fontSize = 13.sp, color = Muted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 主题 ──
            SectionTitle("主题")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChip("跟随系统", settings.themeMode == 0, { onThemeModeChange(0) })
                ThemeChip("浅色", settings.themeMode == 1, { onThemeModeChange(1) })
                ThemeChip("深色", settings.themeMode == 2, { onThemeModeChange(2) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 模型 ──
            SectionTitle("模型")
            if (models.isEmpty()) {
                InfoRow("当前", currentModel.ifEmpty { "—" })
            } else {
                InfoRow("当前", currentModel.ifEmpty { "—" })
                Spacer(modifier = Modifier.height(6.dp))
                models.forEach { m ->
                    val label = m.model.ifEmpty { m.ref }
                    val selected = label == currentModel || m.ref == currentModel || m.active
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Accent.copy(alpha = 0.15f) else Panel)
                            .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                            .clickable { onModelSelect(m.ref) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = if (selected) Accent else Fg,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Text("✓", fontSize = 13.sp, color = Accent)
                        } else if (m.default) {
                            Text("默认", fontSize = 11.sp, color = Muted2)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 显示选项 ──
            SectionTitle("显示")
            SettingSwitch(
                title = "显示推理过程",
                checked = settings.showReasoning,
                onCheckedChange = onShowReasoningChange
            )
            SettingSwitch(
                title = "显示 Token / 费用",
                checked = settings.showTokens,
                onCheckedChange = onShowTokensChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 服务器信息 ──
            SectionTitle("服务器")
            InfoRow("地址", serverUrl)
            InfoRow("标签", status?.label ?: "—")
            InfoRow("计划模式", status?.plan?.let { if (it) "开" else "关" } ?: "—")
            InfoRow("工具审批", status?.toolApprovalMode ?: "—")
            InfoRow("余额", status?.balance?.display ?: "—")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 系统提示词（只读）──
            SectionTitle("系统提示词")
            if (systemPrompt.isNullOrBlank()) {
                InfoRow("提示词", "—")
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = systemPrompt,
                        fontSize = 12.sp,
                        color = Fg2,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "只读：服务端不支持修改系统提示词",
                    fontSize = 11.sp,
                    color = Muted2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 关于 ──
            SectionTitle("关于")
            InfoRow("版本", "Reasonix Agents · AI 协助维护版")
            InfoRow("本仓库", "github.com/xiwangone/reasonix-agents")
            InfoRow("基于原版 (MIT)", "github.com/hxr66666/DeepSeek-Reasonix-android")
            InfoRow("协议上游", "github.com/esengine/DeepSeek-Reasonix")
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Accent,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Accent else Panel)
            .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) Color.White else Muted
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Fg,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Fg2,
            modifier = Modifier.weight(1f)
        )
    }
}

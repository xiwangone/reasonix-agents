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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.CiMonitorStore
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * CI 监控设置二级界面（第四批：设置组件化）。
 * 从设置页「CI 监控」入口进入，含悬浮球开关 / Token / Owner / Repo / 刷新间隔。
 */
@Composable
fun SettingsCiScreen(
    ciSettings: CiMonitorStore.CiSettings,
    onCiSettingsChange: (CiMonitorStore.CiSettings) -> Unit,
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
                    text = "CI 监控",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── CI 监控 ──
            SettingSwitch(
                title = "启用悬浮球",
                checked = ciSettings.enabled,
                onCheckedChange = { on ->
                    onCiSettingsChange(ciSettings.copy(enabled = on))
                },
            )
            if (ciSettings.enabled) {
                Spacer(modifier = Modifier.height(8.dp))

                // GitHub Token（脱敏展示）
                InfoRow("GitHub Token", if (ciSettings.githubToken.isEmpty()) "未填写" else CiMonitorStore.maskToken(ciSettings.githubToken))
                CiTokenInput(
                    currentToken = ciSettings.githubToken,
                    onTokenChange = { t -> onCiSettingsChange(ciSettings.copy(githubToken = t)) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Owner / Repo
                CiTextField(
                    label = "Owner",
                    value = ciSettings.owner,
                    onValueChange = { v -> onCiSettingsChange(ciSettings.copy(owner = v)) },
                )
                CiTextField(
                    label = "Repo",
                    value = ciSettings.repo,
                    onValueChange = { v -> onCiSettingsChange(ciSettings.copy(repo = v)) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 刷新间隔
                InfoRow(
                    "刷新间隔",
                    when (ciSettings.intervalMs) {
                        30_000L -> "30 秒"
                        60_000L -> "1 分钟"
                        300_000L -> "5 分钟"
                        else -> "${ciSettings.intervalMs / 1000} 秒"
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30_000L to "30s", 60_000L to "1m", 300_000L to "5m").forEach { (ms, label) ->
                        val selected = ciSettings.intervalMs == ms
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Accent.copy(alpha = 0.18f) else Panel)
                                    .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                                    .clickable { onCiSettingsChange(ciSettings.copy(intervalMs = ms)) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (selected) Accent else Fg,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "悬浮球颜色：运行中=橙 成功=绿 失败=红 排队=蓝",
                    fontSize = 11.sp,
                    color = Muted2,
                )
                Text(
                    text = "提示：Token 仅存本机，不会上传",
                    fontSize = 11.sp,
                    color = Muted2,
                )
            }
        }
    }
}

@Composable
private fun CiTokenInput(
    currentToken: String,
    onTokenChange: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (editing) "新 Token" else "Token",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.width(64.dp),
            )
            if (editing) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier =
                        Modifier
                            .weight(1f)
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                    cursorBrush = SolidColor(Accent),
                    singleLine = true,
                )
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Accent)
                            .clickable {
                                onTokenChange(draft.trim())
                                editing = false
                                draft = ""
                            }.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("保存", fontSize = 12.sp, color = Color.White)
                }
            } else {
                Text(
                    text = if (currentToken.isEmpty()) "点击填写" else CiMonitorStore.maskToken(currentToken),
                    fontSize = 13.sp,
                    color = if (currentToken.isEmpty()) Muted2 else Fg,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(6.dp))
                            .clickable { editing = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(if (currentToken.isEmpty()) "填写" else "更换", fontSize = 12.sp, color = Accent)
                }
            }
        }
        if (editing) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "输入 GitHub Personal Access Token（repo 权限）",
                fontSize = 11.sp,
                color = Muted2,
            )
        }
    }
}

@Composable
private fun CiTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(64.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .weight(1f)
                    .background(Panel)
                    .border(1.dp, Border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
            singleLine = true,
        )
    }
}

package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/**
 * 显示设置二级界面（第四批：设置组件化）。
 * 从设置页「显示」入口进入，含推理过程 / Token 费用显示开关。
 */
@Composable
fun SettingsDisplayScreen(
    settings: AppSettingsStore.Settings,
    onSettingsChange: (AppSettingsStore.Settings) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
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
            // ── 顶栏（返回 + 标题）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Fg)
                }
                Text(
                    text = "显示",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 显示选项 ──
            SettingSwitch(
                title = "显示推理过程",
                checked = settings.showReasoning,
                onCheckedChange = { on ->
                    onSettingsChange(settings.copy(showReasoning = on))
                }
            )
            SettingSwitch(
                title = "显示 Token / 费用",
                checked = settings.showTokens,
                onCheckedChange = { on ->
                    onSettingsChange(settings.copy(showTokens = on))
                }
            )
        }
    }
}

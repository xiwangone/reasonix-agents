package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
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
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * 网络设置二级界面（第四批：设置组件化）。
 * 从设置页「网络」入口进入，含连接超时 / SSE 断线重连。
 */
@Composable
fun SettingsNetworkScreen(
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
                    text = "网络",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 网络（批 B-11 设置项扩展）──
            Text(
                text = "连接超时",
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15 to "15 秒", 30 to "30 秒", 60 to "60 秒", 120 to "120 秒").forEach { (sec, label) ->
                    ThemeChip(
                        label,
                        settings.connectTimeoutSec == sec,
                        { onSettingsChange(settings.copy(connectTimeoutSec = sec)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SettingSwitch(
                title = "SSE 断线自动重连",
                checked = settings.sseReconnectEnabled,
                onCheckedChange = { on ->
                    onSettingsChange(settings.copy(sseReconnectEnabled = on))
                }
            )
            if (settings.sseReconnectEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "重连退避上限",
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10 to "10 秒", 30 to "30 秒", 60 to "60 秒", 120 to "120 秒").forEach { (sec, label) ->
                        ThemeChip(
                            label,
                            settings.sseReconnectMaxDelaySec == sec,
                            { onSettingsChange(settings.copy(sseReconnectMaxDelaySec = sec)) }
                        )
                    }
                }
                Text(
                    text = "指数退避 1s→2s→4s…，达到上限后保持该间隔重试",
                    fontSize = 10.sp,
                    color = Muted2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

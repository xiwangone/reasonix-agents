package com.reasonix.agents.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.R
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Violet: Color @Composable get() = LocalPalette.current.violet
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/** 示例服务器（本地默认部署地址，可在连接页修改；不强制使用）。 */
const val EXAMPLE_SERVER_IP = "127.0.0.1"
const val EXAMPLE_SERVER_PORT = "8920"

/**
 * 首次启动引导页（第五批 E-4：多用户自配置服务，防白嫖）。
 *
 * 两个并列选项，默认不强制使用示例服务器：
 * - 「配置自己的服务器」：进入连接页自定义服务器地址 / 认证；
 * - 「使用示例服务器」：进入连接页并预填示例地址，可修改后连接。
 */
@Composable
fun FirstLaunchScreen(
    settings: AppSettingsStore.Settings,
    onSettingsChange: (AppSettingsStore.Settings) -> Unit,
    onConfigureOwn: () -> Unit,
    onUseExample: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 顶部设置入口（主题/明暗/语言，全局生效）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                ThemeQuickToggle(settings = settings, onSettingsChange = onSettingsChange)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Logo ──
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("欢迎使用 Reasonix", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Fg)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "选择如何连接你的 AI 编程服务",
                fontSize = 13.sp,
                color = Fg2,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 选项一：配置自己的服务器 ──
            LaunchOptionCard(
                icon = { Icon(Icons.Default.Dns, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp)) },
                title = "配置自己的服务器",
                description = "自定义服务器地址 / 端口 / 认证方式，连接自建或第三方 Reasonix 服务",
                onClick = onConfigureOwn,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 选项二：使用示例服务器 ──
            LaunchOptionCard(
                icon = { Icon(Icons.Default.Public, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp)) },
                title = "使用示例服务器",
                description = "快速填入示例地址（$EXAMPLE_SERVER_IP:$EXAMPLE_SERVER_PORT），可在连接页修改",
                onClick = onUseExample,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "提示：示例服务器为本地默认部署地址，不会强制使用；你可以随时在连接页切换或修改服务器配置。",
                fontSize = 11.sp,
                color = Muted2,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun LaunchOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush =
                        Brush.linearGradient(
                            colors = listOf(Panel, Panel.copy(alpha = 0.6f)),
                        ),
                ).border(1.dp, Border, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(modifier = Modifier.size(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Fg)
        }
        Text(description, fontSize = 12.sp, color = Muted, lineHeight = 17.sp)
    }
}

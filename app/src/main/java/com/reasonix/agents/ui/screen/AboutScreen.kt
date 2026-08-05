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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.api.GitHubReleaseApi
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════
// 调色板
// ═══════════════════════════════════════════════

private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Violet: Color @Composable get() = LocalPalette.current.violet
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * 关于页（批 A-6/A-7）。
 * 多入口：登录后设置页 / 侧边栏 / Chat 顶栏。
 * 内容：版本信息、本仓库链接、RikkaHub Agents 并列项目、上游致谢、检测更新。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    var checking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    fun checkUpdate() {
        if (checking) return
        checking = true
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val release = GitHubReleaseApi().checkLatest()
                    if (release == null || release.tagName.isBlank()) {
                        // 批 C-6：无更新提示「暂时没有更新」
                        "暂时没有更新"
                    } else {
                        val cmp = GitHubReleaseApi.compareVersions(versionName, release.tagName)
                        // 批 C-6：有更新保持弹窗提示下载
                        if (cmp > 0) {
                            "发现新版本 v${release.tagName}（当前 v$versionName）\n\n${release.name}\n\n点击「前往下载」跳转 Release 页面。"
                        } else {
                            "暂时没有更新"
                        }
                    }
                } catch (e: Exception) {
                    // 批 C-6：网络错误提示「网络错误，请稍后重试」
                    "网络错误，请稍后重试"
                }
            }
            checking = false
            checkResult = result
        }
    }

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
            // ── 顶栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Fg
                    )
                }
                Text(
                    text = "关于",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Fg,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Logo + 版本 ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Accent, Violet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("R", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Reasonix Agents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Fg)
                Spacer(modifier = Modifier.height(2.dp))
                Text("版本 v$versionName", fontSize = 12.sp, color = Muted2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "DeepSeek-Reasonix 协议的 Android 原生客户端（Kotlin + Compose）",
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 检测更新 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Panel)
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .clickable { checkUpdate() }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (checking) "检查中…" else "检查更新",
                    fontSize = 14.sp,
                    color = Accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 本项目（批 C-2：放上方区域）──
            SectionCard("本项目（reasonix-agents）") {
                AboutLink("github.com/xiwangone/reasonix-agents") {
                    openUrl("https://github.com/xiwangone/reasonix-agents")
                }
                Text(
                    "Reasonix Agents · AI 协助维护版\n❌ 非官方发布 · ❌ 非原版发布\n代码来源可信（MIT），由 AI 协助合并上游并持续编译",
                    fontSize = 11.sp,
                    color = Muted2
                )
            }

            // ── 并列项目：RikkaHub Agents（批 C-2：与本项目同在上方区域）──
            SectionCard("并列项目（RikkaHub Agents）") {
                Text(
                    "另一款由 AI 协助维护的 Android 端 Agent 客户端（fork 自 RikkaHub 原版 Fork），" +
                        "提供 80+ 设备工具、工作流引擎、Telegram Bot、内置浏览器、SSH 等能力，" +
                        "与本仓库并列独立维护。",
                    fontSize = 12.sp,
                    color = Fg2,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                AboutLink("github.com/xiwangone/rikkahub-agents") {
                    openUrl("https://github.com/xiwangone/rikkahub-agents")
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("上游：github.com/rikkahub/rikkahub · github.com/ExTV/rikkahub-agent", fontSize = 10.sp, color = Muted2)
            }

            // ── 上游项目（批 C-2：放下方区域，仅保留协议上游一个连接）──
            SectionCard("上游项目") {
                AboutLink("协议上游: github.com/esengine/DeepSeek-Reasonix") {
                    openUrl("https://github.com/esengine/DeepSeek-Reasonix")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "MIT License · Reasonix Agents",
                fontSize = 11.sp,
                color = Muted2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
    }

    // ── 更新结果弹窗 ──
    checkResult?.let { result ->
        val hasUpdate = result.startsWith("发现新版本")
        AlertDialog(
            onDismissRequest = { checkResult = null },
            title = { Text(if (hasUpdate) "🎉 发现新版本" else "版本检查", color = Fg) },
            text = { Text(result, color = Fg2, fontSize = 13.sp) },
            confirmButton = {
                if (hasUpdate) {
                    TextButton(onClick = {
                        checkResult = null
                        openUrl("https://github.com/xiwangone/reasonix-agents/releases/latest")
                    }) { Text("前往下载", color = Accent) }
                } else {
                    TextButton(onClick = { checkResult = null }) { Text("好的", color = Accent) }
                }
            },
            dismissButton = {
                TextButton(onClick = { checkResult = null }) { Text("关闭", color = Muted) }
            },
            containerColor = Panel
        )
    }
}

// ═══════════════════════════════════════════════
// 内部组件
// ═══════════════════════════════════════════════

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg
        )
        content()
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun AboutLink(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Accent,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Muted2, modifier = Modifier.size(13.dp))
    }
}

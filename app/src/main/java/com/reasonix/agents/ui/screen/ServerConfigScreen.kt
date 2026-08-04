package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.ServerConfigStore
import com.reasonix.agents.data.api.ReasonixApi
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════
// 调色板 — Reasonix 暗色主题
// ═══════════════════════════════════════════════

private val Bg get() = LocalPalette.current.bg
private val Bg2 get() = LocalPalette.current.bg2
private val Panel get() = LocalPalette.current.panel
private val Panel2 get() = LocalPalette.current.panel2
private val Border get() = LocalPalette.current.border
private val Accent get() = LocalPalette.current.accent
private val Violet get() = LocalPalette.current.violet
private val Fg get() = LocalPalette.current.fg
private val Fg2 get() = LocalPalette.current.fg2
private val Muted get() = LocalPalette.current.muted
private val Muted2 get() = LocalPalette.current.muted2
private val Success get() = LocalPalette.current.success

// ═══════════════════════════════════════════════
// ServerConfigScreen
// ═══════════════════════════════════════════════

/**
 * 启动服务器配置页面。
 * 用户输入 IP 和端口（协议可选 HTTP/HTTPS），点击连接后回调 [onConnect]。
 * 若留空，默认连接 http://127.0.0.1:8920（HTTPS 时默认 443）。
 * 自动从本地存储加载上次配置并回填表单，连接时持久化。
 */
@Composable
fun ServerConfigScreen(
    onConnect: (String, Pair<String, String>?) -> Unit
) {
    val context = LocalContext.current
    var connecting by remember { mutableStateOf(false) }
    var connectError by remember { mutableStateOf<String?>(null) }
    val saved = remember { ServerConfigStore.load(context) }

    var ipInput by remember { mutableStateOf(saved.ip) }
    var portInput by remember { mutableStateOf(saved.port) }
    var usernameInput by remember { mutableStateOf(saved.username) }
    var passwordInput by remember { mutableStateOf(saved.password) }
    var useHttps by remember { mutableStateOf(saved.useHttps) }

    val defaultIp = "127.0.0.1"
    val defaultPort = if (useHttps) "443" else "8920"

    val resolvedIp = ipInput.ifBlank { defaultIp }
    val resolvedPort = portInput.ifBlank { defaultPort }
    val previewUrl = "${if (useHttps) "https" else "http"}://$resolvedIp:$resolvedPort"

    val canConnect = resolvedIp.isNotBlank() && resolvedPort.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo ──
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Accent, Violet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "R",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.Text(
                text = "Reasonix",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Fg
            )

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.material3.Text(
                text = "AI 编程助手",
                fontSize = 13.sp,
                color = Fg2
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 服务器地址配置卡片 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Panel)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "连接服务器",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Fg
                )

                // ── 协议（HTTP / HTTPS 切换） ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "协议",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HTTP" to false, "HTTPS" to true).forEach { (label, isHttps) ->
                            val selected = useHttps == isHttps
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Accent.copy(alpha = 0.18f) else Bg2)
                                    .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                                    .clickable { useHttps = isHttps }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) Accent else Muted
                                )
                            }
                        }
                    }
                }

                // ── IP 地址 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "地址",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (ipInput.isEmpty()) {
                            androidx.compose.material3.Text(
                                text = defaultIp,
                                fontSize = 14.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            textStyle = TextStyle(
                                color = Fg,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true
                        )
                    }
                }

                // ── 端口 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "端口",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (portInput.isEmpty()) {
                            androidx.compose.material3.Text(
                                text = defaultPort,
                                fontSize = 14.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = portInput,
                            onValueChange = { portInput = it.filter { c -> c.isDigit() } },
                            textStyle = TextStyle(
                                color = Fg,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                // ── 用户名（Basic Auth，可选）──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "用户",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (usernameInput.isEmpty()) {
                            androidx.compose.material3.Text(
                                text = "可选",
                                fontSize = 14.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            textStyle = TextStyle(
                                color = Fg,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true
                        )
                    }
                }

                // ── 密码（Basic Auth，可选）──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "密码",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (passwordInput.isEmpty()) {
                            androidx.compose.material3.Text(
                                text = "可选",
                                fontSize = 14.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            textStyle = TextStyle(
                                color = Fg,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }

                // ── 预览 URL ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = "地址",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    androidx.compose.material3.Text(
                        text = previewUrl,
                        fontSize = 13.sp,
                        color = if (ipInput.isBlank() && portInput.isBlank()) Muted2 else Fg2,
                        textAlign = TextAlign.Start
                    )
                }

                // ── 提示 ──
                if (ipInput.isBlank() || portInput.isBlank()) {
                    androidx.compose.material3.Text(
                        text = "留空将使用默认地址 ${defaultIp}:${defaultPort}",
                        fontSize = 11.sp,
                        color = Muted2
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── 连接按钮 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canConnect) Accent else Panel2)
                        .then(
                            if (canConnect && !connecting) Modifier.clickable {
                            val ip = resolvedIp
                            val port = resolvedPort
                            val creds = if (usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                usernameInput to passwordInput
                            } else null
                            connecting = true
                            connectError = null
                            CoroutineScope(Dispatchers.Main).launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val api = ReasonixApi(previewUrl, creds)
                                        val status = api.getStatus()
                                        status != null
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                connecting = false
                                if (ok) {
                                    ServerConfigStore.save(context, ip, port, usernameInput, passwordInput, useHttps)
                                    onConnect(previewUrl, creds)
                                } else {
                                    connectError = "连接失败：无法访问服务器或认证失败。请检查地址/端口/用户名密码。\n提示：reasonix 默认端口 10002（HTTP）或 443（HTTPS）"
                                }
                            }
                        }
                            else Modifier
                        )
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = if (connecting) "连接中…" else "连接",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canConnect) Color.White else Muted
                    )
                }

                // ── 连接错误提示 ──
                connectError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        text = err,
                        fontSize = 11.sp,
                        color = Color(0xFFFF6B6B)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 关于说明 ──
                androidx.compose.material3.Text(
                    text = "关于",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Fg
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.Text(
                    text = "Reasonix Agents · AI 协助维护版（非官方 / 非原版发布）",
                    fontSize = 11.sp,
                    color = Muted2
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = "本仓库: github.com/xiwangone/reasonix-agents",
                    fontSize = 11.sp,
                    color = Muted2
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = "基于原版 (MIT): github.com/hxr66666/DeepSeek-Reasonix-android",
                    fontSize = 11.sp,
                    color = Muted2
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = "协议上游: github.com/esengine/DeepSeek-Reasonix",
                    fontSize = 11.sp,
                    color = Muted2
                )
            }
        }
    }
}



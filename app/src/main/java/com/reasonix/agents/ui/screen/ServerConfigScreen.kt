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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.AuthType
import com.reasonix.agents.data.ServerConfigStore
import com.reasonix.agents.data.ServerConfigStore.ServerProfile
import com.reasonix.agents.data.api.ConnectFailKind
import com.reasonix.agents.data.api.ConnectResult
import com.reasonix.agents.data.api.ReasonixApi
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

// ═══════════════════════════════════════════════
// 调色板 — 从 LocalPalette 读取（支持主题切换）
// ═══════════════════════════════════════════════

private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Panel2: Color @Composable get() = LocalPalette.current.panel2
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Violet: Color @Composable get() = LocalPalette.current.violet
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2
private val Warning: Color @Composable get() = LocalPalette.current.warning

// ═══════════════════════════════════════════════
// ServerConfigScreen（登录 / 连接页）
// ═══════════════════════════════════════════════

/**
 * 连接页（批 A + B 增强版）。
 * - 批 A-1：HTTP 明文全局放开 + 首次选 HTTP 弹窗警告（一次性）+ 常驻小字 + 错误分类提示
 * - 批 A-2：顶部主题/语言快捷入口（全局生效）
 * - 批 A-3：协议切换 SegmentedButton + 地址智能解析（粘贴完整 URL 自动拆分）
 * - 批 A-4：认证方式（无 / Basic Auth / Bearer Token）
 * - 批 A-5：登录页保留主题、语言切换
 * - 批 A-8：输入框点击区域修复（fillMaxWidth + 整行可聚焦）
 * - 批 B-11：密码/Token 显隐切换（眼睛图标）
 * - 批 B-12：记住上次配置自动回填 + 多服务器配置保存/快速切换
 */
@Composable
fun ServerConfigScreen(
    settings: AppSettingsStore.Settings,
    onSettingsChange: (AppSettingsStore.Settings) -> Unit,
    onConnect: (String, AuthInfo?) -> Unit
) {
    val context = LocalContext.current
    var connecting by remember { mutableStateOf(false) }
    var connectError by remember { mutableStateOf<String?>(null) }
    val saved = remember { ServerConfigStore.load(context) }

    val profileFocusRequester = remember { FocusRequester() }
    val addressFocusRequester = remember { FocusRequester() }
    val portFocusRequester = remember { FocusRequester() }
    val userFocusRequester = remember { FocusRequester() }

    var ipInput by remember { mutableStateOf(saved.ip) }
    var portInput by remember { mutableStateOf(saved.port) }
    var useHttps by remember { mutableStateOf(saved.useHttps) }
    var authType by remember { mutableStateOf(AuthType.from(saved.authType)) }
    var usernameInput by remember { mutableStateOf(saved.username) }
    var passwordInput by remember { mutableStateOf(saved.password) }
    var tokenInput by remember { mutableStateOf(saved.token) }
    var passwordVisible by remember { mutableStateOf(false) }
    var tokenVisible by remember { mutableStateOf(false) }

    // 多服务器配置（批 B-12）
    var profiles by remember { mutableStateOf(ServerConfigStore.loadProfiles(context)) }
    var profileName by remember { mutableStateOf("") }
    var showHttpWarning by remember { mutableStateOf(false) }

    val defaultIp = "127.0.0.1"
    val defaultPort = if (useHttps) "443" else "8920"

    val resolvedIp = ipInput.ifBlank { defaultIp }
    val resolvedPort = portInput.ifBlank { defaultPort }
    val previewUrl = "${if (useHttps) "https" else "http"}://$resolvedIp:$resolvedPort"

    val canConnect = resolvedIp.isNotBlank() && resolvedPort.isNotBlank()

    /** 切换到 HTTP：未确认过警告则弹窗；确认后放行（批 A-1 一次性确认）。 */
    fun requestHttpSwitch() {
        if (AppSettingsStore.load(context).httpWarningAcked) {
            useHttps = false
        } else {
            showHttpWarning = true
        }
    }

    fun onUrlInput(raw: String) {
        // 批 A-3 地址智能解析：粘贴完整 URL（如 https://host:443）自动拆分为协议/主机/端口
        val parsed = parseServerUrl(raw)
        if (parsed != null) {
            val (isHttps, host, port) = parsed
            if (!isHttps && !AppSettingsStore.load(context).httpWarningAcked) {
                // 明文 URL 粘贴同样走一次性警告
                showHttpWarning = true
                ipInput = raw
                return
            }
            useHttps = isHttps
            ipInput = host
            portInput = port
        } else {
            ipInput = raw
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 顶部设置入口（批 A-2/A-5：主题预设/明暗/语言，全局生效）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                ThemeQuickToggle(settings = settings, onSettingsChange = onSettingsChange)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Logo（品牌图标，随主题）──
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(13.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reasonix",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Fg
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "AI 编程助手",
                fontSize = 13.sp,
                color = Fg2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 服务器配置卡片 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Panel)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "连接服务器",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Fg
                )

                // ── 服务器配置（多配置管理，批 B-12）──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 配置名输入框（可编辑）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .clickable { profileFocusRequester.requestFocus() }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        if (profileName.isEmpty()) {
                            Text(
                                text = "配置名（可选）",
                                fontSize = 13.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(profileFocusRequester)
                        )
                    }
                    // 已保存配置下拉
                    ServerProfilesDropdown(
                        profiles = profiles,
                        onSelect = { p ->
                            profileName = p.name
                            ipInput = p.ip
                            portInput = p.port
                            useHttps = p.useHttps
                            authType = AuthType.from(p.authType)
                            usernameInput = p.username
                            passwordInput = p.password
                            tokenInput = p.token
                        }
                    )
                    // 保存当前配置
                    IconButton(
                        onClick = {
                            val profile = ServerProfile(
                                name = profileName.ifBlank { "$resolvedIp:$resolvedPort" },
                                ip = resolvedIp,
                                port = resolvedPort,
                                useHttps = useHttps,
                                authType = authType.name,
                                username = usernameInput,
                                password = passwordInput,
                                token = tokenInput
                            )
                            profiles = upsertProfile(profiles, profile)
                            ServerConfigStore.saveProfiles(context, profiles)
                            ServerConfigStore.saveLast(context, profile)
                            profileName = profile.name
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存配置", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                    // 删除当前配置
                    IconButton(
                        onClick = {
                            val name = profileName.ifBlank { "$resolvedIp:$resolvedPort" }
                            profiles = profiles.filterNot { it.name == name }
                            ServerConfigStore.saveProfiles(context, profiles)
                            profileName = ""
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除配置", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }

                // ── 协议（批 A-3：SegmentedButton）──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "协议",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = !useHttps,
                            onClick = { if (useHttps) requestHttpSwitch() },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Accent.copy(alpha = 0.18f),
                                activeContentColor = Accent,
                                inactiveContainerColor = Bg2,
                                inactiveContentColor = Muted
                            )
                        ) { Text("HTTP", fontSize = 14.sp) }
                        SegmentedButton(
                            selected = useHttps,
                            onClick = { useHttps = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Accent.copy(alpha = 0.18f),
                                activeContentColor = Accent,
                                inactiveContainerColor = Bg2,
                                inactiveContentColor = Muted
                            )
                        ) { Text("HTTPS", fontSize = 14.sp) }
                    }
                }

                // ── 地址（支持完整 URL 智能解析）──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
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
                            .clickable { addressFocusRequester.requestFocus() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (ipInput.isEmpty()) {
                            Text(
                                text = "$defaultIp（可粘贴完整 URL，如 https://host:443）",
                                fontSize = 14.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = ipInput,
                            onValueChange = { onUrlInput(it) },
                            textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(addressFocusRequester)
                        )
                    }
                }

                // ── 端口 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
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
                            .clickable { portFocusRequester.requestFocus() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (portInput.isEmpty()) {
                            Text(
                                text = defaultPort,
                                fontSize = 14.sp,
                                color = Muted2
                            )
                        }
                        BasicTextField(
                            value = portInput,
                            onValueChange = { portInput = it.filter { c -> c.isDigit() } },
                            textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(portFocusRequester)
                        )
                    }
                }

                // ── 认证方式（批 A-4）──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "认证",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    AuthTypeDropdown(
                        authType = authType,
                        onSelect = { authType = it }
                    )
                }

                // Basic Auth：用户名 + 密码
                if (authType == AuthType.BASIC) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
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
                                .clickable { userFocusRequester.requestFocus() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (usernameInput.isEmpty()) {
                                Text(text = "用户名", fontSize = 14.sp, color = Muted2)
                            }
                            BasicTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                                cursorBrush = SolidColor(Accent),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(userFocusRequester)
                            )
                        }
                    }
                    PasswordField(
                        label = "密码",
                        value = passwordInput,
                        visible = passwordVisible,
                        onValueChange = { passwordInput = it },
                        onToggleVisible = { passwordVisible = !passwordVisible },
                        placeholder = "密码"
                    )
                    Text(
                        text = "Basic Auth 适用于服务端配置了用户名/密码的场景",
                        fontSize = 11.sp,
                        color = Muted2
                    )
                }

                // Bearer Token
                if (authType == AuthType.BEARER) {
                    PasswordField(
                        label = "Token",
                        value = tokenInput,
                        visible = tokenVisible,
                        onValueChange = { tokenInput = it },
                        onToggleVisible = { tokenVisible = !tokenVisible },
                        placeholder = "Bearer Token"
                    )
                    Text(
                        text = "Bearer Token 适用于 API Token / 密钥认证（如 GitHub Token 等）",
                        fontSize = 11.sp,
                        color = Muted2
                    )
                }

                // ── 预览 URL ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "地址",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.width(48.dp)
                    )
                    Text(
                        text = previewUrl,
                        fontSize = 13.sp,
                        color = if (ipInput.isBlank() && portInput.isBlank()) Muted2 else Fg2,
                        textAlign = TextAlign.Start
                    )
                }

                // ── HTTP 明文常驻警示（批 A-1）──
                if (!useHttps) {
                    Text(
                        text = "⚠ HTTP 明文不安全，推荐 HTTPS（数据以明文传输，仅建议在内网/本机使用）",
                        fontSize = 11.sp,
                        color = Warning
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // ── 连接按钮 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canConnect) Accent else Panel2)
                        .then(
                            if (canConnect && !connecting) Modifier.clickable {
                                val url = previewUrl
                                val auth = buildAuth(authType, usernameInput, passwordInput, tokenInput)
                                connecting = true
                                connectError = null
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            ReasonixApi(url, auth, settings.connectTimeoutSec).diagnose()
                                        } catch (e: Exception) {
                                            ConnectResult.Fail(ConnectFailKind.UNKNOWN, "连接失败：${e.message ?: "未知错误"}")
                                        }
                                    }
                                    connecting = false
                                    when (result) {
                                        is ConnectResult.Ok -> {
                                            val profile = ServerProfile(
                                                name = profileName.ifBlank { "$resolvedIp:$resolvedPort" },
                                                ip = resolvedIp,
                                                port = resolvedPort,
                                                useHttps = useHttps,
                                                authType = authType.name,
                                                username = usernameInput,
                                                password = passwordInput,
                                                token = tokenInput
                                            )
                                            // 批 B-12：记住上次连接配置 + 同步到命名配置
                                            ServerConfigStore.saveLast(context, profile)
                                            if (profileName.isNotBlank()) {
                                                profiles = upsertProfile(profiles, profile)
                                                ServerConfigStore.saveProfiles(context, profiles)
                                            }
                                            onConnect(url, auth)
                                        }
                                        is ConnectResult.Fail -> connectError = result.message
                                    }
                                }
                            } else Modifier
                        )
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (connecting) "连接中…" else "连接",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canConnect) Color.White else Muted
                    )
                }

                // ── 连接错误分类提示（批 A-1）──
                connectError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = err,
                        fontSize = 11.sp,
                        color = Color(0xFFFF6B6B)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 关于说明 ──
                Text(
                    text = "关于",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Fg
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reasonix Agents · AI 协助维护版（非官方 / 非原版发布）",
                    fontSize = 11.sp,
                    color = Muted2
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinkText("本仓库: github.com/xiwangone/reasonix-agents")
                Spacer(modifier = Modifier.height(4.dp))
                LinkText("基于原版 (MIT): github.com/hxr66666/DeepSeek-Reasonix-android")
                Spacer(modifier = Modifier.height(4.dp))
                LinkText("协议上游: github.com/esengine/DeepSeek-Reasonix")
                Spacer(modifier = Modifier.height(4.dp))
                LinkText("并列项目: RikkaHub Agents — github.com/xiwangone/rikkahub-agents")
            }
        }
    }

    // ── HTTP 明文一次性警告弹窗（批 A-1）──
    if (showHttpWarning) {
        AlertDialog(
            onDismissRequest = { showHttpWarning = false },
            title = { Text("⚠ HTTP 明文不安全", color = Fg) },
            text = {
                Text(
                    "HTTP 流量为明文传输，公网环境下数据可能被窃听/篡改，推荐使用 HTTPS。\n\n确定要继续使用 HTTP 吗？（仅首次确认，之后不再提示）",
                    color = Fg2,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showHttpWarning = false
                    AppSettingsStore.setHttpWarningAcked(context, true)
                    useHttps = false
                    // 弹窗确认后，若地址框还保留着完整 URL，则重新解析拆分（批 A-3）
                    parseServerUrl(ipInput)?.let { (_, host, port) ->
                        ipInput = host
                        portInput = port
                    }
                }) { Text("继续使用 HTTP", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showHttpWarning = false }) { Text("改用 HTTPS", color = Muted) }
            },
            containerColor = Panel
        )
    }
}

// ═══════════════════════════════════════════════
// 内部组件
// ═══════════════════════════════════════════════

/** 顶部快捷设置：主题预设 / 明暗 / 语言 循环切换（批 A-2/A-5，全局生效）。 */
@Composable
private fun ThemeQuickToggle(
    settings: AppSettingsStore.Settings,
    onSettingsChange: (AppSettingsStore.Settings) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // 主题预设：品牌紫蓝 / Material
        QuickIconButton(
            icon = { Icon(Icons.Default.Palette, contentDescription = "主题预设", tint = Muted, modifier = Modifier.size(18.dp)) },
            label = if (settings.themePreset == AppSettingsStore.THEME_PRESET_MATERIAL) "Material" else "品牌紫蓝",
            onClick = {
                val next = if (settings.themePreset == AppSettingsStore.THEME_PRESET_MATERIAL) {
                    AppSettingsStore.THEME_PRESET_BRAND
                } else {
                    AppSettingsStore.THEME_PRESET_MATERIAL
                }
                onSettingsChange(settings.copy(themePreset = next))
            }
        )
        // 明暗：跟随系统 / 浅色 / 深色
        QuickIconButton(
            icon = {
                Icon(
                    imageVector = when (settings.themeMode) {
                        AppSettingsStore.THEME_MODE_DARK -> Icons.Default.DarkMode
                        AppSettingsStore.THEME_MODE_LIGHT -> Icons.Default.LightMode
                        else -> Icons.Default.BrightnessAuto
                    },
                    contentDescription = "明暗",
                    tint = Muted,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = when (settings.themeMode) {
                AppSettingsStore.THEME_MODE_DARK -> "深色"
                AppSettingsStore.THEME_MODE_LIGHT -> "浅色"
                else -> "跟随系统"
            },
            onClick = {
                val next = when (settings.themeMode) {
                    AppSettingsStore.THEME_MODE_SYSTEM -> AppSettingsStore.THEME_MODE_LIGHT
                    AppSettingsStore.THEME_MODE_LIGHT -> AppSettingsStore.THEME_MODE_DARK
                    else -> AppSettingsStore.THEME_MODE_SYSTEM
                }
                onSettingsChange(settings.copy(themeMode = next))
            }
        )
        // 语言（当前仅中文，偏好持久化预留）
        QuickIconButton(
            icon = { Icon(Icons.Default.Translate, contentDescription = "语言", tint = Muted, modifier = Modifier.size(18.dp)) },
            label = if (settings.language == "en") "EN" else "中",
            onClick = {
                val next = if (settings.language == "en") "zh" else "en"
                onSettingsChange(settings.copy(language = next))
            }
        )
    }
}

@Composable
private fun QuickIconButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Bg2)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 9.sp, color = Muted2)
    }
}

/** 已保存服务器配置下拉（批 B-12）。 */
@Composable
private fun ServerProfilesDropdown(
    profiles: List<ServerProfile>,
    onSelect: (ServerProfile) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Bg2)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${profiles.size} 套", fontSize = 12.sp, color = Fg2)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Panel
        ) {
            if (profiles.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("暂无已保存配置", fontSize = 12.sp, color = Muted2) },
                    onClick = { expanded = false }
                )
            } else {
                profiles.forEach { p ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(p.label, fontSize = 13.sp, color = Fg)
                                Text(
                                    "${if (p.useHttps) "https" else "http"}://${p.ip}:${p.port}",
                                    fontSize = 10.sp,
                                    color = Muted2
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(p)
                        }
                    )
                }
            }
        }
    }
}

/** 认证方式下拉（批 A-4）。 */
@Composable
private fun AuthTypeDropdown(
    authType: AuthType,
    onSelect: (AuthType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Bg2)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (authType) {
                        AuthType.BASIC -> "Basic Auth"
                        AuthType.BEARER -> "Bearer Token"
                        AuthType.NONE -> "无认证"
                    },
                    fontSize = 14.sp,
                    color = Fg
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Panel
        ) {
            DropdownMenuItem(
                text = { Text("无认证（直连）", fontSize = 13.sp, color = Fg) },
                onClick = { expanded = false; onSelect(AuthType.NONE) }
            )
            DropdownMenuItem(
                text = { Text("Basic Auth（用户名/密码）", fontSize = 13.sp, color = Fg) },
                onClick = { expanded = false; onSelect(AuthType.BASIC) }
            )
            DropdownMenuItem(
                text = { Text("Bearer Token", fontSize = 13.sp, color = Fg) },
                onClick = { expanded = false; onSelect(AuthType.BEARER) }
            )
        }
    }
}

/** 密码/Token 输入框（带眼睛显隐切换，批 B-11）。 */
@Composable
private fun PasswordField(
    label: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
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
                .clickable { focusRequester.requestFocus() }
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, fontSize = 14.sp, color = Muted2)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                IconButton(onClick = onToggleVisible, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "隐藏$label" else "显示$label",
                        tint = Muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** 仓库链接文本（可点击跳转浏览器）。 */
@Composable
private fun LinkText(text: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val url = text.substringAfter(": ").ifBlank { text }
    Text(
        text = text,
        fontSize = 11.sp,
        color = Accent.copy(alpha = 0.85f),
        modifier = Modifier.clickable {
            val href = if (url.startsWith("http")) url else "https://$url"
            uriHandler.openUri(href)
        }
    )
}

// ═══════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════

/** 解析完整服务器 URL（批 A-3）：https://host:443/path → (useHttps, host, port)。非法输入返回 null。 */
internal fun parseServerUrl(raw: String): Triple<Boolean, String, String>? {
    val input = raw.trim()
    val match = Regex(
        "^(https?)://([^/:\\s]+)(?::(\\d{1,5}))?(/.*)?$",
        RegexOption.IGNORE_CASE
    ).find(input) ?: return null
    val scheme = match.groupValues[1].lowercase()
    val host = match.groupValues[2]
    val port = match.groupValues[3]
    val isHttps = scheme == "https"
    val resolvedPort = port.ifBlank { if (isHttps) "443" else "8920" }
    return Triple(isHttps, host, resolvedPort)
}

/** 保存/覆盖命名配置（同名覆盖）。 */
internal fun upsertProfile(profiles: List<ServerProfile>, profile: ServerProfile): List<ServerProfile> {
    val result = profiles.toMutableList()
    result.removeAll { it.name == profile.name && profile.name.isNotBlank() }
    result.add(profile)
    return result
}

/** 根据认证方式构建 AuthInfo（批 A-4）。 */
internal fun buildAuth(
    type: AuthType,
    username: String,
    password: String,
    token: String
): AuthInfo? = when (type) {
    AuthType.BASIC -> if (username.isNotBlank() || password.isNotBlank()) {
        AuthInfo(AuthType.BASIC, username, password)
    } else {
        null
    }
    AuthType.BEARER -> if (token.isNotBlank()) AuthInfo(AuthType.BEARER, token = token) else null
    AuthType.NONE -> null
}

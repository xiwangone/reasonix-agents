package com.reasonix.agents.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.CustomModelStore
import com.reasonix.agents.data.PinnedSessionsStore
import com.reasonix.agents.data.ServerConfigStore
import com.reasonix.agents.data.model.ConnectionState
import com.reasonix.agents.data.model.ModelInfo
import com.reasonix.agents.data.model.SessionInfo
import com.reasonix.agents.data.model.StatusInfo
import com.reasonix.agents.ui.components.*
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.viewmodel.ChatViewModel
import com.reasonix.agents.util.ImageOcr
import com.reasonix.agents.util.SessionExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ═══════════════════════════════════════════════
// 调色板 — 匹配 index.html 的 Reasonix 暗色主题
// ═══════════════════════════════════════════════

private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Panel2: Color @Composable get() = LocalPalette.current.panel2
private val Card: Color @Composable get() = LocalPalette.current.card
private val CardHover: Color @Composable get() = LocalPalette.current.cardHover
private val Border: Color @Composable get() = LocalPalette.current.border
private val BorderStr: Color @Composable get() = LocalPalette.current.borderStr
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val AccentS: Color @Composable get() = LocalPalette.current.accentS
private val Violet: Color @Composable get() = LocalPalette.current.violet
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2
private val Danger: Color @Composable get() = LocalPalette.current.danger
private val DangerS: Color @Composable get() = LocalPalette.current.dangerS
private val Success: Color @Composable get() = LocalPalette.current.success
private val SuccessS: Color @Composable get() = LocalPalette.current.successS
private val Warning: Color @Composable get() = LocalPalette.current.warning

// ═══════════════════════════════════════════════
// ChatScreen — 主界面入口
// ═══════════════════════════════════════════════

@Composable
fun ChatScreen(
    initialServerUrl: String = "http://127.0.0.1:8920",
    initialAuth: AuthInfo? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToServerConfig: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // 首次启动时配置服务器地址
    LaunchedEffect(Unit) {
        if (state.serverUrl != initialServerUrl) {
            viewModel.configureServer(initialServerUrl, initialAuth)
        }
    }

    // ── 配置列表 / 模型分组选择 弹窗状态（批 C-1/C-3）──
    var showConfigsDialog by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // ── 会话导出（批 B-16）──
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("文本") }
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("*/*"),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val content =
                if (exportFormat == "JSON") {
                    SessionExporter.buildJson(state.messages)
                } else {
                    SessionExporter.buildText(state.messages)
                }
            val ok = SessionExporter.write(context, uri, content)
            android.widget.Toast
                .makeText(
                    context,
                    if (ok) "会话已导出" else "导出失败",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
        }

    fun startExport(format: String) {
        exportFormat = format
        exportLauncher.launch(if (format == "JSON") "reasonix-会话.json" else "reasonix-会话.txt")
    }

    // ── 发送图片（第六批：相册选择 + 本地 OCR 优先）──
    val scope = rememberCoroutineScope()
    var imageProcessing by remember { mutableStateOf(false) }
    // OCR 失败后待确认的本地图片路径（弹窗询问是否发送原图）
    var ocrFailedPath by remember { mutableStateOf<String?>(null) }

    // 相册选择：PickVisualMedia（Android 13+ Photo Picker，低版本自动回退系统选择器）
    val pickImageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                imageProcessing = true
                // ① 拷贝到内部存储（避免选择器 Uri 权限失效）
                val saved =
                    withContext(Dispatchers.IO) {
                        ImageOcr.copyToInternal(context, uri)
                    }
                if (saved == null) {
                    imageProcessing = false
                    android.widget.Toast
                        .makeText(context, "图片读取失败", android.widget.Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                // ② 采样解码（防 OOM）
                val bitmap =
                    withContext(Dispatchers.IO) {
                        ImageOcr.decodeSampledBitmap(saved)
                    }
                if (bitmap == null) {
                    imageProcessing = false
                    android.widget.Toast
                        .makeText(context, "图片读取失败", android.widget.Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                // ③ 本地 OCR（ML Kit 中文识别）
                val ocrText =
                    withContext(Dispatchers.IO) {
                        ImageOcr.recognize(context, bitmap)
                    }
                imageProcessing = false
                if (ocrText.isNullOrBlank()) {
                    // 识别失败：提示，可选发送原图（无文字）
                    android.widget.Toast
                        .makeText(context, "图片识别失败", android.widget.Toast.LENGTH_SHORT)
                        .show()
                    ocrFailedPath = saved
                } else {
                    // 识别成功：识别文本作为消息发送（图片+文字展示在消息中）
                    android.widget.Toast
                        .makeText(context, "识别成功，正在发送…", android.widget.Toast.LENGTH_SHORT)
                        .show()
                    viewModel.sendImageMessage(ocrText, saved)
                }
            }
        }

    // ── 附件面板（2026-08-06 对齐 RikkaHub 附件聚合）：拍照 / 文件 / 相册 ──
    var showAttachSheet by remember { mutableStateOf(false) }

    // 拍照：TakePicture 到内部缓存（FileProvider 提供 content:// Uri，无存储权限）
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) {
                scope.launch {
                    imageProcessing = true
                    // 复用图片管线：解码 + OCR
                    val bitmap =
                        withContext(Dispatchers.IO) {
                            ImageOcr.decodeSampledBitmap(cameraUri!!.path ?: "")
                        }
                    if (bitmap == null) {
                        imageProcessing = false
                        android.widget.Toast
                            .makeText(context, "拍照读取失败", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        return@launch
                    }
                    val ocrText =
                        withContext(Dispatchers.IO) {
                            ImageOcr.recognize(context, bitmap)
                        }
                    imageProcessing = false
                    if (ocrText.isNullOrBlank()) {
                        android.widget.Toast
                            .makeText(context, "图片识别失败", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        ocrFailedPath = cameraUri!!.path
                    } else {
                        android.widget.Toast
                            .makeText(context, "识别成功，正在发送…", android.widget.Toast.LENGTH_SHORT)
                            .show()
                        viewModel.sendImageMessage(ocrText, cameraUri!!.path)
                    }
                }
            }
        }

    // 文件：OpenDocument 任意类型 → 以「文件路径」发送（发送文件名 + 提示，供服务端工具读取）
    val pickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val name =
                runCatching {
                    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                    }
                }.getOrNull() ?: uri.lastPathSegment ?: "文件"
            android.widget.Toast
                .makeText(context, "已选择文件：$name", android.widget.Toast.LENGTH_SHORT)
                .show()
            // sendMessage() 无参（从 inputText state 读取）——先填入输入框再发送
            viewModel.onInputChange("请读取并处理文件：$name（路径 $uri）")
            viewModel.sendMessage()
        }

    // 全局键盘事件处理
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding()
                .imePadding()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) return@onKeyEvent false
                    when {
                        // / 聚焦输入框
                        event.key == Key.Slash && state.isStreaming -> {
                            viewModel.onInputChange("/")
                            focusRequester.requestFocus()
                            true
                        }

                        // Esc → 取消流式 / 双 Esc 倒带
                        event.key == Key.Escape -> {
                            if (state.isStreaming) {
                                viewModel.cancelStreaming()
                            } else {
                                viewModel.tryDoubleEscRewind()
                            }
                            true
                        }

                        // Shift+Tab → 切计划模式
                        event.key == Key.Tab && event.isShiftPressed && !state.isStreaming -> {
                            viewModel.togglePlanMode()
                            true
                        }

                        // Ctrl+Y → 切 YOLO
                        event.key == Key.Y && event.isCtrlPressed && !state.isStreaming -> {
                            val newMode = if (state.toolApprovalMode == "yolo") "auto" else "yolo"
                            viewModel.setToolApprovalMode(newMode)
                            true
                        }

                        else -> {
                            false
                        }
                    }
                },
    ) {
        // ── 主内容区 ──
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部栏（批 A-2：左上品牌 logo / 右上 关于 + 设置 入口）──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧：品牌 logo（随主题渐变）+ 应用名 + 连接状态点；点击弹出「保存的配置」列表（批 C-1）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showConfigsDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(brush = Brush.linearGradient(colors = listOf(Accent, Violet))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("R", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reasonix",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Fg,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 连接健康状态点（绿=已连接 / 黄=重连中 / 红=断开 / 灰=就绪）
                    val dotColor =
                        when {
                            state.connectionState == ConnectionState.RECONNECTING -> Warning
                            state.connectionState == ConnectionState.DISCONNECTED && state.isStreaming -> Danger
                            state.connectionState == ConnectionState.CONNECTED -> Success
                            else -> Muted2
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // 模型选择器（批 C-3：按 key 分组选择模型）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Panel2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .clickable { showModelPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = state.currentModel.ifEmpty { "选择模型" },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.currentModel.isEmpty()) Muted2 else Fg2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(13.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                // 右侧：设置入口（关于入口统一在设置页中）
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置",
                        tint = Muted,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            // 欢迎页（无消息时） 或 消息列表
            if (state.messages.isEmpty() && !state.isStreaming) {
                WelcomeScreen(
                    onPromptClick = { prompt ->
                        viewModel.onInputChange(prompt)
                        viewModel.sendMessage()
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                // Todo 面板：有任务时显示在消息列表上方
                if (state.todos.isNotEmpty()) {
                    TodoPanel(
                        todos = state.todos,
                        onRefresh = { viewModel.loadTodos() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                MessageList(
                    items = state.messages,
                    modifier = Modifier.weight(1f),
                    balance = state.status?.balance?.display,
                    onApprove = { session, persist, scope ->
                        val approval =
                            state.messages.lastOrNull {
                                it is com.reasonix.agents.data.model.ChatItem.ApprovalCard
                            } as? com.reasonix.agents.data.model.ChatItem.ApprovalCard
                        approval?.let { viewModel.approveTool(it.id, session, persist, scope) }
                    },
                    onDeny = {
                        val approval =
                            state.messages.lastOrNull {
                                it is com.reasonix.agents.data.model.ChatItem.ApprovalCard
                            } as? com.reasonix.agents.data.model.ChatItem.ApprovalCard
                        approval?.let { viewModel.denyTool(it.id) }
                    },
                    onAskSubmit = { answers ->
                        val ask =
                            state.messages.lastOrNull {
                                it is com.reasonix.agents.data.model.ChatItem.AskCard
                            } as? com.reasonix.agents.data.model.ChatItem.AskCard
                        ask?.let { viewModel.submitAskAnswers(it.id, answers) }
                    },
                )
            }

            // 底部输入区域
            Footer(
                inputText = state.inputText,
                onInputChange = { viewModel.onInputChange(it) },
                onSend = { viewModel.sendMessage() },
                onCancel = { viewModel.cancelStreaming() },
                isStreaming = state.isStreaming,
                planMode = state.planMode,
                toolApprovalMode = state.toolApprovalMode,
                onTogglePlan = { viewModel.togglePlanMode() },
                onToggleBypass = {
                    val newMode = if (state.toolApprovalMode == "yolo") "auto" else "yolo"
                    viewModel.setToolApprovalMode(newMode)
                },
                onToggleAuto = { viewModel.setToolApprovalMode("auto") },
                serverUrl = state.serverUrl,
                onServerUrlChange = { viewModel.onServerUrlChange(it) },
                connectionState = state.connectionState,
                cumulativeCost = state.cumulativeCost,
                cumulativeTokens = state.cumulativeTokens,
                balance = state.status?.balance?.display,
                focusRequester = focusRequester,
                imageProcessing = imageProcessing,
                onPickImage = {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onAttach = {
                    showAttachSheet = true
                },
                onQuickAction = { template ->
                    viewModel.onInputChange(template)
                    focusRequester.requestFocus()
                },
            )
        }

        // ── 侧边栏悬浮面板 ──
        AnimatedVisibility(
            visible = state.showSidebar,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.zIndex(10f),
        ) {
            Sidebar(
                sessions = state.sessions,
                status = state.status,
                isStreaming = state.isStreaming,
                cumulativeCost = state.cumulativeCost,
                onNewSession = {
                    viewModel.newSession()
                    viewModel.toggleSidebar()
                },
                onSelectSession = {
                    viewModel.selectSession(it)
                    viewModel.toggleSidebar()
                },
                onDeleteSession = { viewModel.deleteSession(it) },
                onDeleteSessions = { viewModel.deleteSessions(it) },
                onCompact = { viewModel.compactConversation() },
                onRewind = { viewModel.showRewindPicker() },
                onFork = { viewModel.showRewindPicker() },
                onStats = { viewModel.showStatsDialog() },
                onExport = {
                    viewModel.toggleSidebar()
                    showExportDialog = true
                },
                modifier = Modifier.width(220.dp),
            )
        }

        // ── 侧边栏遮罩 ──
        if (state.showSidebar) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(9f)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { viewModel.toggleSidebar() },
            )
        }

        // ── Slash 菜单（悬浮在输入框上方，向上展开） ──
        val slashPrefix =
            remember(state.inputText) {
                if (state.inputText.startsWith("/") && !state.inputText.contains(" ")) {
                    state.inputText.removePrefix("/")
                } else {
                    null
                }
            }
        if (slashPrefix != null) {
            SlashMenu(
                prefix = slashPrefix,
                onSelect = { command ->
                    viewModel.onInputChange(command + " ")
                    focusRequester.requestFocus()
                },
                onDismiss = {},
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 100.dp)
                        .zIndex(8f),
            )
        }

        // ── 侧边栏切换按钮（左侧 2/5 高度处） ──
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = maxHeight * 0.4f, start = 8.dp)
                    .zIndex(5f)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Bg2.copy(alpha = 0.9f))
                    .border(1.dp, Border, CircleShape)
                    .clickable { viewModel.toggleSidebar() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.showSidebar) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (state.showSidebar) "Close sidebar" else "Open sidebar",
                tint = if (state.showSidebar) Accent else Muted,
                modifier = Modifier.size(26.dp),
            )
        }

        // ── Rewind Picker 对话框 ──
        if (state.showRewindPicker) {
            RewindPickerDialog(
                checkpoints = state.checkpoints,
                onRewind = { turn, scope -> viewModel.rewindTo(turn, scope) },
                onFork = { turn -> viewModel.forkAt(turn) },
                onSummarize = { turn, mode -> viewModel.summarizeAt(turn, mode) },
                onDismiss = { viewModel.dismissRewindPicker() },
            )
        }

        // ── Stats 对话框 ──
        if (state.showStatsDialog) {
            StatsDialog(
                status = state.status,
                sessionCount = state.sessions.size,
                cumulativeTokens = state.cumulativeTokens,
                cumulativeCost = state.cumulativeCost,
                cumulativeCacheHit = state.cumulativeCacheHit,
                cumulativeCacheMiss = state.cumulativeCacheMiss,
                onDismiss = { viewModel.dismissStatsDialog() },
            )
        }

        // ── 导出会话对话框（批 B-16）──
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("导出会话", color = Fg) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "当前会话共 ${state.messages.size} 条消息，选择导出格式：",
                            fontSize = 13.sp,
                            color = Fg2,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("文本" to "Markdown 风格可读文本", "JSON" to "结构化数据（便于程序处理）").forEach { (fmt, desc) ->
                                Column(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (exportFormat == fmt) Accent.copy(alpha = 0.15f) else Panel2)
                                            .border(1.dp, if (exportFormat == fmt) Accent else Border, RoundedCornerShape(8.dp))
                                            .clickable { exportFormat = fmt }
                                            .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        fmt,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color =
                                            if (exportFormat ==
                                                fmt
                                            ) {
                                                Accent
                                            } else {
                                                Fg
                                            },
                                    )
                                    Text(desc, fontSize = 10.sp, color = Muted2)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        startExport(exportFormat)
                    }) { Text("导出", color = Accent) }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) { Text("取消", color = Muted) }
                },
                containerColor = Panel,
            )
        }

        // ── 图片识别失败对话框（第六批：可选发送原图，无文字）──
        ocrFailedPath?.let { failedPath ->
            AlertDialog(
                onDismissRequest = { ocrFailedPath = null },
                title = { Text("图片识别失败", color = Fg) },
                text = {
                    Text(
                        "未能识别图片中的文字，是否发送原始图片（无文字）？",
                        fontSize = 13.sp,
                        color = Fg2,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        ocrFailedPath = null
                        viewModel.sendImageMessage("", failedPath)
                    }) { Text("发送原图", color = Accent) }
                },
                dismissButton = {
                    TextButton(onClick = { ocrFailedPath = null }) { Text("取消", color = Muted) }
                },
                containerColor = Panel,
            )
        }

        // ── 附件面板（2026-08-06 对齐 RikkaHub 附件聚合：拍照 / 文件 / 相册）──
        if (showAttachSheet) {
            AlertDialog(
                onDismissRequest = { showAttachSheet = false },
                title = { Text("添加附件", color = Fg) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AttachSheetItem("拍照", Icons.Default.PhotoCamera) {
                            showAttachSheet = false
                            // 创建临时文件 + FileProvider Uri
                            val file =
                                File(
                                    context.cacheDir,
                                    "camera_${System.currentTimeMillis()}.jpg",
                                )
                            cameraUri =
                                androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                            takePictureLauncher.launch(cameraUri)
                        }
                        AttachSheetItem("文件", Icons.Default.AttachFile) {
                            showAttachSheet = false
                            pickFileLauncher.launch(arrayOf("*/*"))
                        }
                        AttachSheetItem("相册（OCR 识别）", Icons.Default.Image) {
                            showAttachSheet = false
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachSheet = false }) { Text("取消", color = Muted) }
                },
                containerColor = Panel,
            )
        }

        // ── 保存的配置列表对话框（批 C-1：左上角「配置」入口）──
        if (showConfigsDialog) {
            SavedConfigsDialog(
                profiles = ServerConfigStore.loadProfiles(context),
                currentServerUrl = state.serverUrl,
                onSelect = { p ->
                    showConfigsDialog = false
                    val url = "${if (p.useHttps) "https" else "http"}://${p.ip}:${p.port}"
                    viewModel.configureServer(url, p.toAuth())
                },
                onAddNew = {
                    showConfigsDialog = false
                    onNavigateToServerConfig()
                },
                onDismiss = { showConfigsDialog = false },
            )
        }

        // ── 模型分组选择对话框（批 C-3：按 key 分组）──
        if (showModelPicker) {
            ModelPickerDialog(
                models = state.models,
                customModels = state.customModels,
                currentModel = state.currentModel,
                onSelect = { model ->
                    showModelPicker = false
                    viewModel.setModel(model)
                },
                onRemoveModel = { name ->
                    // 通过 name 查找对应的自定义模型 id 并删除
                    val cm = state.customModels.find { it.name == name }
                    if (cm != null) viewModel.removeCustomModel(cm.id)
                },
                onDismiss = { showModelPicker = false },
            )
        }
    }
}

// ═══════════════════════════════════════════════
// SavedConfigsDialog — 保存的配置列表（批 C-1）
// ═══════════════════════════════════════════════

@Composable
private fun SavedConfigsDialog(
    profiles: List<ServerConfigStore.ServerProfile>,
    currentServerUrl: String,
    onSelect: (ServerConfigStore.ServerProfile) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 第五批 E-4：按协议分组展示（HTTP / HTTPS），当前连接高亮并标记「当前」
    val grouped = profiles.groupBy { if (it.useHttps) "HTTPS" else "HTTP" }
    val groupOrder = listOf("HTTP", "HTTPS")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存的配置", color = Fg) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (profiles.isEmpty()) {
                    Text(
                        "暂无保存的配置，点击下方「新增配置」创建",
                        fontSize = 13.sp,
                        color = Muted2,
                    )
                } else {
                    groupOrder.forEach { group ->
                        val items = grouped[group].orEmpty()
                        if (items.isEmpty()) return@forEach
                        // 分组标题
                        Text(
                            group,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        items.forEach { p ->
                            val isCurrent =
                                currentServerUrl ==
                                    "${if (p.useHttps) "https" else "http"}://${p.ip}:${p.port}"
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) AccentS else Panel)
                                        .border(1.dp, if (isCurrent) Accent.copy(alpha = 0.6f) else Border, RoundedCornerShape(8.dp))
                                        .clickable { onSelect(p) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Fg,
                                    )
                                    Text(
                                        text =
                                            "${if (p.useHttps) "https" else "http"}://${p.ip}:${p.port}" +
                                                if (p.authType != "NONE") " · ${p.authType}" else "",
                                        fontSize = 11.sp,
                                        color = Muted2,
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        "当前",
                                        fontSize = 10.sp,
                                        color = Accent,
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                }
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "切换",
                                    tint = if (isCurrent) Accent else Muted2,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增配置", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = Muted) }
        },
        containerColor = Panel,
    )
}

// ═══════════════════════════════════════════════
// ModelPickerDialog — 模型按 key 分组选择（批 C-3/C-4）
// ═══════════════════════════════════════════════

/** 模型分组：label 为分组名，items 为 (显示名, 选择值) 列表。 */
private data class ModelGroup(
    val label: String,
    val items: List<Pair<String, String>>,
)

@Composable
private fun ModelPickerDialog(
    models: List<ModelInfo>,
    customModels: List<CustomModelStore.CustomModel>,
    currentModel: String,
    onSelect: (String) -> Unit,
    onRemoveModel: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    // 分组：自定义模型按 key 分组（批 C-4）；服务端模型按 provider 分组
    val groups =
        remember(models, customModels) {
            val custom =
                customModels
                    .groupBy { it.groupLabel }
                    .map { (key, list) -> ModelGroup(key, list.map { it.name to it.name }) }
            val server =
                models
                    .groupBy { it.provider.ifBlank { "服务器模型" } }
                    .map { (provider, list) ->
                        ModelGroup(provider, list.map { m -> (m.model.ifEmpty { m.ref }) to m.ref })
                    }
            custom + server
        }
    var currentGroup by remember { mutableStateOf<ModelGroup?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    // 自定义模型名称集合（用于判断是否可删除）
    val customModelNames = remember(customModels) { customModels.map { it.name }.toSet() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentGroup != null) {
                    TextButton(onClick = { currentGroup = null }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = currentGroup?.label ?: "选择模型（按 key 分组）",
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        text = {
            if (groups.isEmpty()) {
                Text("暂无模型（可在设置页添加）", fontSize = 13.sp, color = Muted2)
            } else if (currentGroup == null) {
                // 第一层：分组列表
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    groups.forEach { g ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Panel)
                                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                                    .clickable { currentGroup = g }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(g.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Fg)
                                Text("${g.items.size} 个模型", fontSize = 10.sp, color = Muted2)
                            }
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Muted2,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            } else {
                // 第二层：组内模型列表
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    currentGroup?.items?.forEach { (display, value) ->
                        val selected = value == currentModel || display == currentModel
                        val canDelete = onRemoveModel != null && value in customModelNames
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) AccentS else Panel)
                                    .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = { onSelect(value) },
                                        onLongClick = if (canDelete) ({ showDeleteConfirm = value }) else null,
                                    ).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = display,
                                fontSize = 13.sp,
                                color = if (selected) Accent else Fg,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (canDelete) {
                                IconButton(
                                    onClick = { showDeleteConfirm = value },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除模型",
                                        tint = Muted,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = "当前模型", tint = Accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = Muted) }
        },
        containerColor = Panel,
    )

    // ── 删除模型确认对话框 ──
    showDeleteConfirm?.let { modelName ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除模型", color = Fg) },
            text = { Text("确定要删除模型「$modelName」吗？删除后需重新添加。", fontSize = 13.sp, color = Fg2) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveModel?.invoke(modelName)
                    showDeleteConfirm = null
                }) { Text("删除", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消", color = Muted) }
            },
            containerColor = Panel,
        )
    }
}

// ═══════════════════════════════════════════════
// Sidebar
// ═══════════════════════════════════════════════

@Composable
private fun Sidebar(
    sessions: List<SessionInfo>,
    status: StatusInfo?,
    isStreaming: Boolean,
    cumulativeCost: Double,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteSessions: (List<String>) -> Unit,
    onCompact: () -> Unit,
    onRewind: () -> Unit,
    onFork: () -> Unit,
    onStats: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 第五批 E-2：会话多选模式——长按进入，支持全选 / 批量删除 / 单条删除
    var selectionMode by remember { mutableStateOf(false) }
    var selectedNames by remember { mutableStateOf(setOf<String>()) }

    // 2026-08-06 对齐 RikkaHub 会话抽屉：置顶（本地记录，置顶排前 + 星标切换）
    val context = LocalContext.current
    var pinnedNames by remember { mutableStateOf(PinnedSessionsStore.load(context)) }
    // 置顶会话排前（保持原有相对顺序）
    val orderedSessions =
        remember(sessions, pinnedNames) {
            sessions.sortedBy { if (it.name in pinnedNames) 0 else 1 }
        }

    // 会话列表刷新后清理已不存在的选中项；列表清空时退出多选
    LaunchedEffect(sessions) {
        val valid = sessions.map { it.name }.toSet()
        selectedNames = selectedNames.filter { it in valid }.toSet()
        if (sessions.isEmpty()) {
            selectionMode = false
            selectedNames = emptySet()
        }
    }

    // 长按会话：进入多选模式并选中该项
    val enterSelection: (String) -> Unit = { name ->
        selectionMode = true
        selectedNames = selectedNames + name
    }

    val allSelected = sessions.isNotEmpty() && selectedNames.size == sessions.size

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .background(Bg2)
                .border(1.dp, Border),
    ) {
        // ── Brand ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            brush =
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Accent, Violet),
                                ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text("R", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text("Reasonix", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Fg)
        }

        HorizontalDivider(color = Border, thickness = 1.dp)

        // ── 导航 ──
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            SidebarItem("新建会话", onClick = onNewSession, accent = true)
            SidebarItem("压缩", onClick = onCompact)
            SidebarItem("倒带", onClick = onRewind)
            SidebarItem("分支", onClick = onFork)

            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

            SidebarItem("统计", onClick = onStats)

            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

            // 批七：侧边栏不再重复「设置 / 关于」入口（底部导航已有「设置」，关于保留设置页入口）
            SidebarItem("导出会话", onClick = onExport)
        }

        // ── 会话标签 ──
        Text(
            "会话",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = Muted,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )

        // ── 多选操作栏（第五批 E-2）：已选 N 项 + 全选 + 批量删除 + 退出 ──
        if (selectionMode) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentS)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "已选 ${selectedNames.size} 项",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    selectedNames = if (allSelected) emptySet() else sessions.map { it.name }.toSet()
                }) {
                    Text(if (allSelected) "取消全选" else "全选", fontSize = 12.sp, color = Accent)
                }
                TextButton(onClick = {
                    if (selectedNames.isNotEmpty()) {
                        onDeleteSessions(selectedNames.toList())
                        selectedNames = emptySet()
                        selectionMode = false
                    }
                }) {
                    Text("删除", fontSize = 12.sp, color = Danger)
                }
                TextButton(onClick = {
                    selectionMode = false
                    selectedNames = emptySet()
                }) {
                    Text("退出", fontSize = 12.sp, color = Muted)
                }
            }
        }

        // ── 会话列表 ──
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            if (sessions.isEmpty()) {
                Text(
                    "无会话",
                    fontSize = 12.sp,
                    color = Muted2,
                    modifier = Modifier.padding(10.dp),
                )
            } else {
                orderedSessions.forEach { session ->
                    SessionRow(
                        session = session,
                        isStreaming = isStreaming,
                        selectionMode = selectionMode,
                        pinned = session.name in pinnedNames,
                        onTogglePin = {
                            pinnedNames = PinnedSessionsStore.toggle(context, session.name)
                        },
                        selected = session.name in selectedNames,
                        onSelect = { onSelectSession(session.path) },
                        onToggleSelect = {
                            selectedNames =
                                if (session.name in selectedNames) {
                                    selectedNames - session.name
                                } else {
                                    selectedNames + session.name
                                }
                        },
                        onLongPress = { enterSelection(session.name) },
                        onDelete = { onDeleteSession(session.name) },
                    )
                }
            }
        }

        // ── 底部状态 ──
        HorizontalDivider(color = Border, thickness = 1.dp)

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "状态",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = Muted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )

            // 上下文用量条
            val used = status?.used ?: 0
            val window = status?.window ?: 0
            if (window > 0) {
                val pct = (used.toFloat() / window).coerceIn(0f, 1f)
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    // 3 段颜色进度条
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Panel2),
                    ) {
                        val barColor =
                            when {
                                pct > 0.83f -> Danger
                                pct > 0.6f -> Warning
                                else -> Accent
                            }
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(pct)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(barColor),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(fmtTok(used), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted2)
                        Text(fmtTok(window), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Muted2)
                    }
                }
            }

            // ── 状态指标 ──
            val cacheTotal = (status?.cacheHit ?: 0) + (status?.cacheMiss ?: 0)
            if (cacheTotal > 0) {
                val cachePct = (status!!.cacheHit.toFloat() / cacheTotal * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("缓存", fontSize = 11.sp, color = Muted2)
                    Text(
                        "$cachePct%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (cachePct > 50) Success else Muted2,
                    )
                }
            }

            if (cumulativeCost > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("费用", fontSize = 11.sp, color = Muted2)
                    Text(
                        fmtMoney(cumulativeCost),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Fg2,
                    )
                }
            }

            status?.balance?.display?.let { bal ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("余额", fontSize = 11.sp, color = Muted2)
                    Text(
                        bal,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Fg2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 状态指示器
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isStreaming) Accent else Muted2),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status?.label ?: "-",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (accent) Accent else Card)
                .clickable(onClick = onClick)
                // 批七：加大 padding，扩大点击热区
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            // 批七：字号 13 → 15sp，提升可读性与点击体验
            fontSize = 15.sp,
            fontWeight = if (accent) FontWeight.Medium else FontWeight.Normal,
            color = if (accent) Color.White else Fg2,
        )
    }
}

/**
 * 会话行（第五批 E-2 重构）。
 *
 * - 修复删除按钮 bug：原实现用 Text("×") + 顺序错误的 modifier（padding 在 clickable 之前），
 *   可点击区域极小且与父级 combinedClickable 冲突导致「点击无法删除」；
 *   改为多选模式下常显的 IconButton（独立消费点击事件，可点击区域充足）。
 * - 长按进入多选模式并选中该项；多选模式下点击切换选中，点击删除按钮单条删除；
 *   普通模式下点击切换会话（当前会话与流式期间不可切换）。
 */
@Composable
private fun SessionRow(
    session: SessionInfo,
    isStreaming: Boolean,
    selectionMode: Boolean,
    pinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    selected: Boolean,
    onSelect: () -> Unit,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        selected -> AccentS
                        session.current && !selectionMode -> AccentS
                        else -> Card
                    },
                ).combinedClickable(
                    onClick = {
                        if (selectionMode) {
                            onToggleSelect()
                        } else if (!isStreaming && !session.current) {
                            onSelect()
                        }
                    },
                    onLongClick = onLongPress,
                ).padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 多选模式：勾选指示器
        if (selectionMode) {
            Box(
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (selected) Accent else CardHover)
                        .border(1.dp, if (selected) Accent else Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = session.title ?: session.name.take(30),
            // 批七：会话行字号 12 → 14sp
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected || (session.current && !selectionMode)) Accent else Fg2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // 置顶星标（非多选模式显示；对齐 RikkaHub 会话抽屉置顶）
        if (!selectionMode) {
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(26.dp),
            ) {
                Icon(
                    imageVector = if (pinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (pinned) "取消置顶" else "置顶",
                    tint = if (pinned) Accent else Muted,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        // 多选模式：单条删除按钮（IconButton 修复点击区域，独立消费点击）
        if (selectionMode) {
            IconButton(
                onClick = onDelete,
                modifier =
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp)),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除会话",
                    tint = Danger,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Footer — 工具栏 + 输入框 + SlashMenu
// ═══════════════════════════════════════════════

@Composable
private fun Footer(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isStreaming: Boolean,
    planMode: Boolean,
    toolApprovalMode: String,
    onTogglePlan: () -> Unit,
    onToggleBypass: (() -> Unit)?,
    onToggleAuto: (() -> Unit)?,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    connectionState: ConnectionState,
    cumulativeCost: Double,
    cumulativeTokens: Long,
    balance: String?,
    focusRequester: FocusRequester,
    imageProcessing: Boolean,
    onPickImage: () -> Unit,
    onAttach: () -> Unit = {},
    onQuickAction: ((String) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Bg)
                .border(1.dp, Border),
    ) {
        // ── 工具栏 ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Auto
            ToolbarButton("Auto", active = toolApprovalMode == "auto", accent = false) { onToggleAuto?.invoke() }
            // Plan
            ToolbarButton("Plan", active = planMode, accent = false) { onTogglePlan() }
            // YOLO
            ToolbarButton("YOLO", active = toolApprovalMode == "yolo", danger = true) { onToggleBypass?.invoke() }

            // ── 快捷任务模板（run 常用功能点击项）──
            if (onQuickAction != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Border))
                Spacer(modifier = Modifier.width(2.dp))
                QuickActionButton("解释代码", onClick = { onQuickAction("请解释以下代码的功能和结构：") })
                QuickActionButton("修复错误", onClick = { onQuickAction("请查找并修复以下代码中的错误：") })
                QuickActionButton("编写测试", onClick = { onQuickAction("请为以下代码编写单元测试：") })
                QuickActionButton("代码审查", onClick = { onQuickAction("请对以下代码进行代码审查，指出潜在问题和改进建议：") })
                QuickActionButton("优化性能", onClick = { onQuickAction("请分析以下代码的性能瓶颈并给出优化建议：") })
            }

            // 分隔
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Border))

            // 状态（连接健康度：绿=已连接 / 黄=重连中 / 红=流式中断开 / 灰=就绪）
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (dotColor, statusText) =
                    when {
                        connectionState == ConnectionState.RECONNECTING -> Warning to "重连中…"
                        connectionState == ConnectionState.DISCONNECTED && isStreaming -> Danger to "连接断开"
                        isStreaming -> Accent to "思考中…"
                        connectionState == ConnectionState.CONNECTED -> Success to "已连接"
                        else -> Muted2 to "就绪"
                    }
                Box(
                    modifier =
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // turn info + balance
            if (cumulativeTokens > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "T:${fmtTok(cumulativeTokens)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Muted2,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (cumulativeCost > 0.0) {
                Text(
                    text = fmtMoney(cumulativeCost),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Muted2,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            balance?.let { b ->
                Text(
                    text = b,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Success,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 服务器地址
            Text(
                text = serverUrl,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Muted2,
            )
        }

        // ── 输入框（输入区 + 独立发送按钮）──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 输入区：独立背景/边框，整块可点击输入
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Card)
                        .border(1.dp, BorderStr, RoundedCornerShape(14.dp))
                        .padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "›",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.width(6.dp))

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .padding(vertical = 10.dp)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    event.key == Key.Enter &&
                                    !event.isShiftPressed &&
                                    inputText.isNotBlank()
                                ) {
                                    onSend()
                                    true
                                } else {
                                    false
                                }
                            },
                    textStyle =
                        TextStyle(
                            color = Fg,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                    cursorBrush = SolidColor(Accent),
                    singleLine = false,
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    "输入消息…  / 查看命令",
                                    color = Muted2,
                                    fontSize = 15.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 附件按钮（2026-08-06 对齐 RikkaHub 附件聚合）：
            // 单击 = 相册选图（本地 OCR），长按 = 附件面板（拍照 / 文件 / 相册）
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .combinedClickable(
                            onClick = onPickImage,
                            onLongClick = onAttach,
                            enabled = !imageProcessing,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (imageProcessing) Panel2 else Bg2)
                            .border(1.dp, BorderStr, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (imageProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = Accent,
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "发送图片（长按更多附件）",
                            tint = Muted,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 发送/停止按钮（独立于输入区）
            if (isStreaming) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Danger),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White),
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (inputText.isNotBlank()) Accent else Panel2,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("↑", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Bg2,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Muted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    active: Boolean,
    accent: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color =
            when {
                active && danger -> DangerS
                active -> AccentS
                else -> Bg2
            },
        border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color =
                when {
                    active && danger -> Danger
                    active -> Accent
                    else -> Muted
                },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

// ═══════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════

private fun fmtTok(n: Long): String = if (n >= 1000) "%.1fk".format(n / 1000.0) else "$n"

private fun fmtMoney(n: Double): String =
    when {
        n >= 1.0 -> "¥%.2f".format(n)
        n >= 0.01 -> "¥%.4f".format(n)
        else -> "¥%.6f".format(n)
    }

// ═══════════════════════════════════════════════
// 附件面板项（2026-08-06）
// ═══════════════════════════════════════════════
@Composable
private fun AttachSheetItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = Fg)
    }
}

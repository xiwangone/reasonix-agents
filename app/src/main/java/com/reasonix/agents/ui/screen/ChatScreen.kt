package com.reasonix.agents.ui.screen

import android.net.Uri
import java.util.Calendar
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.CustomModelStore
import com.reasonix.agents.data.MemoryStore
import com.reasonix.agents.data.PinnedSessionsStore
import com.reasonix.agents.data.SessionTimestampsStore
import com.reasonix.agents.R
import com.reasonix.agents.data.ProfileStore
import com.reasonix.agents.data.ServerConfigStore
import com.reasonix.agents.data.model.ConnectionState
import com.reasonix.agents.data.model.ModelInfo
import com.reasonix.agents.data.model.SessionInfo
import com.reasonix.agents.data.model.StatusInfo
import com.reasonix.agents.ui.components.*
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.theme.ToolNames
import com.reasonix.agents.ui.viewmodel.ChatViewModel
import com.reasonix.agents.util.ImageOcr
import com.reasonix.agents.util.SessionExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
    // 2026-08-07：本地资料（昵称 + Emoji 头像）——左上角配置 → 编辑资料
    val localContext = LocalContext.current
    var profile by remember { mutableStateOf(ProfileStore.load(localContext)) }
    var showProfileEdit by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    // ── 会话导出（批 B-16）──
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showMemoryModeDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("文本") }
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("*/*"),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val content =
                when (exportFormat) {
                    "JSON" -> SessionExporter.buildJson(state.messages)
                    "JSONL" -> SessionExporter.buildJsonl(state.messages)
                    else -> SessionExporter.buildText(state.messages)
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
        val fileName =
            when (format) {
                "JSON" -> "reasonix-会话.json"
                "JSONL" -> "reasonix-会话.jsonl"
                else -> "reasonix-会话.txt"
            }
        exportLauncher.launch(fileName)
    }

    // ── 发送图片（第六批：相册选择 + 本地 OCR 优先）──
    val scope = rememberCoroutineScope()
    var imageProcessing by remember { mutableStateOf(false) }
    // OCR 失败后待确认的本地图片路径（弹窗询问是否发送原图）
    var ocrFailedPath by remember { mutableStateOf<String?>(null) }
    // 2026-08-07：OCR 转述确认——图片识别成功后先弹确认框（预览 + 可折叠编辑转述内容），
    // 用户可修改转述文本避免 AI 误判，确认后发送
    // 2026-08-08：待发送图片列表（最多 3 张，选图先入附件条随消息一起发送）
    var pendingImages by remember { mutableStateOf<List<PendingImage>>(emptyList()) }

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
                    // 识别成功：弹出转述确认框（2026-08-07 起不再直接发送）
                    if (pendingImages.size >= 3) {
                        android.widget.Toast.makeText(context, "最多同时添加 3 张图片", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        pendingImages = pendingImages + PendingImage(imagePath = saved, ocrText = ocrText)
                    }
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
                        // 识别成功：弹出转述确认框（2026-08-07 起不再直接发送）
                        if (pendingImages.size >= 3) {
                            android.widget.Toast.makeText(context, "最多同时添加 3 张图片", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            pendingImages = pendingImages + PendingImage(imagePath = cameraUri!!.path ?: "", ocrText = ocrText)
                        }
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
                // 2026-08-07：移除 safeDrawingPadding——Scaffold contentWindowInsets 已通过
                // NavHost innerPadding 处理安全区，此处再 padding 会双重下移（顶栏离刘海过远）
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
        // 2026-08-07：侧边栏融合——手写 overlay 改为 Material3 ModalNavigationDrawer
        // （手势滑动 + 返回键关闭 + 遮罩，对齐 RikkaHub 抽屉交互）
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()
        LaunchedEffect(state.showSidebar) {
            if (state.showSidebar) drawerState.open() else drawerState.close()
        }
        LaunchedEffect(drawerState) {
            snapshotFlow { drawerState.currentValue }.collect { v ->
                val show = v == DrawerValue.Open
                if (show != state.showSidebar) viewModel.setSidebar(show)
            }
        }

        // 返回键：抽屉打开时先关闭抽屉（而非退出页面）
        BackHandler(enabled = drawerState.isOpen) {
            drawerScope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp),
                    drawerContainerColor = Bg,
                ) {
                    Sidebar(
                        sessions = state.sessions,
                        status = state.status,
                        isStreaming = state.isStreaming,
                        hasError = state.error != null,
                        cumulativeCost = state.cumulativeCost,
                        onNewSession = {
                            viewModel.newSession()
                            drawerScope.launch { drawerState.close() }
                        },
                        onSelectSession = {
                            viewModel.selectSession(it)
                            drawerScope.launch { drawerState.close() }
                        },
                        onDeleteSession = { viewModel.deleteSession(it) },
                        onDeleteSessions = { viewModel.deleteSessions(it) },
                        onCompact = { viewModel.compactConversation() },
                        onRewind = { viewModel.showRewindPicker() },
                        onFork = { viewModel.showRewindPicker() },
                        onStats = { viewModel.showStatsDialog() },
                        onExport = {
                            drawerScope.launch { drawerState.close() }
                            showExportDialog = true
                        },
                        onMemoryMode = {
                            drawerScope.launch { drawerState.close() }
                            showMemoryModeDialog = true
                        },
                        isOffline = state.isOffline,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            gesturesEnabled = true,
        ) {
        // ── 主内容区 ──
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶部栏（批 A-2：左上品牌 logo / 右上 关于 + 设置 入口）──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    // 头像：优先本地 Emoji 头像；其次认证用户名首字母；否则品牌 R 渐变块（2026-08-07 支持编辑资料）
                    val avatarLetter =
                        initialAuth
                            ?.takeIf { it.username.isNotBlank() }
                            ?.username
                            ?.trim()
                            ?.firstOrNull()
                            ?.uppercaseChar()
                    if (profile.avatarEmoji.isNotBlank()) {
                        // Emoji 头像
                        Box(
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Bg2)
                                    .border(1.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = profile.avatarEmoji,
                                fontSize = 15.sp,
                            )
                        }
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush =
                                            Brush.linearGradient(
                                                colors =
                                                    if (avatarLetter != null) listOf(Violet, Accent)
                                                    else listOf(Accent, Violet),
                                            ),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = avatarLetter?.toString() ?: "R",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profile.displayName.ifBlank { "Reasonix" },
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
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = state.currentModel.ifEmpty { "选择模型" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.currentModel.isEmpty()) Muted2 else Fg2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
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
                // 2026-08-18：注入上下文卡片固定悬浮在消息区顶部（不区分新旧对话）
                InjectionContextCard(
                    systemPrompt = state.systemPrompt,
                    userPrompt = viewModel.activePromptContent(),
                    memoryText = state.memoryText,
                    onSaveUserPrompt = { viewModel.saveActivePrompt(it) },
                    onSaveMemory = { viewModel.saveMemoryText(it) },
                )
                // Todo 面板：有任务时显示在消息列表上方
                if (state.todos.isNotEmpty()) {
                    TodoPanel(
                        todos = state.todos,
                        onRefresh = { viewModel.loadTodos() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                // 2026-08-18：引用回复预览
                state.quotedMessage?.let { quoted ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LocalPalette.current.bg2)
                            .border(1.dp, LocalPalette.current.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = null,
                            tint = LocalPalette.current.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (quoted.length > 80) quoted.take(80) + "..." else quoted,
                            fontSize = 12.sp,
                            color = LocalPalette.current.fg2,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = { viewModel.clearQuotedMessage() },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消引用",
                                tint = LocalPalette.current.muted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                MessageList(
                    items = state.messages,
                    modifier = Modifier.weight(1f),
                    isStreaming = state.isStreaming,
                    assistantName = state.status?.label,
                    userName = profile.displayName.ifBlank { null },
                    balance = state.status?.balance?.display,
                    cumulativeTokens = state.cumulativeTokens,
                    onRegenerate = { viewModel.regenerateLast() },
                    onDeleteMessage = { viewModel.deleteMessage(it) },
                    onSwipeLeft = { message -> viewModel.setQuotedMessage(message) },
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
            // 当前正在执行的工具名（供状态区显示「正在 <工具>…」；null=推理/正文阶段）
            val runningToolName =
                if (state.isStreaming) {
                    state.messages.asReversed().firstNotNullOfOrNull { item ->
                        (item as? com.reasonix.agents.data.model.ChatItem.AssistantTurn)
                            ?.blocks
                            ?.firstOrNull { it is com.reasonix.agents.data.model.TurnBlock.Tool && it.isRunning }
                            ?.let { ToolNames.display((it as com.reasonix.agents.data.model.TurnBlock.Tool).name) }
                    }
                } else {
                    null
                }
            Footer(
                inputText = state.inputText,
                onInputChange = { viewModel.onInputChange(it) },
                onSend = {
                    if (pendingImages.isNotEmpty()) {
                        // 2026-08-14：图片消息 = 用户输入 + [图片] 标记；2026-08-15：恢复 OCR 文本随消息发送
                        // （OCR 识别文本作为图片内容发给 AI，AI 依赖转述内容理解图片）
                        val typed = state.inputText.trim()
                        viewModel.sendImageMessage(
                            typed,
                            pendingImages.map { it.imagePath },
                            pendingImages.map { it.ocrText },
                        )
                        pendingImages = emptyList()
                        viewModel.onInputChange("")
                    } else {
                        viewModel.sendMessage()
                    }
                },
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
                runningToolName = runningToolName,
                serverRunning = state.serverRunning,
                pendingCount = state.pendingMessages.size,
                cumulativeCost = state.cumulativeCost,
                cumulativeTokens = state.cumulativeTokens,
                balance = state.status?.balance?.display,
                focusRequester = focusRequester,
                imageProcessing = imageProcessing,
                pendingImages = pendingImages,
                onRemoveImage = { img -> pendingImages = pendingImages - img },
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
                // 2026-08-18：传递自定义快捷操作列表
                quickActions = state.settings.quickActions,
                showSidebar = state.showSidebar,
                onToggleSidebar = { viewModel.toggleSidebar() },
            )
        }
        } // ← ModalNavigationDrawer 关闭

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

        // ── 自动压缩确认对话框（2026-08-13，仿 RikkaHub Agents）──
        if (state.autoCompactPending) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAutoCompact() },
                title = { Text("建议压缩上下文", color = Fg) },
                text = {
                    Text(
                        "当前会话上下文用量已达阈值，压缩可降低 token 消耗。是否现在压缩？",
                        fontSize = 13.sp,
                        color = Fg2,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmAutoCompact() }) {
                        Text("压缩", color = Accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAutoCompact() }) {
                        Text("暂不", color = Fg2)
                    }
                },
                containerColor = Panel,
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
                            listOf(
                                "文本" to "Markdown 风格可读文本",
                                "JSON" to "结构化数据（便于程序处理）",
                                "JSONL" to "流式事件（每行一条，可回放）",
                            ).forEach { (fmt, desc) ->
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
                    TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.action_cancel), color = Muted) }
                },
                containerColor = Panel,
            )
        }

        // ── 记忆模式对话框（2026-08-08：会话级 互通/隔离/关闭）──
        if (showMemoryModeDialog) {
            val ctx = LocalContext.current
            val sk = viewModel.currentSessionKeyForUi()
            val currentMode = MemoryStore.memoryMode(ctx, sk)
            AlertDialog(
                onDismissRequest = { showMemoryModeDialog = false },
                title = { Text("会话记忆模式", color = Fg) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (sk.isNullOrBlank()) "当前：未进入会话（使用全局设置）" else "当前会话：$sk",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                        listOf(
                            Triple("互通", "所有对话共享同一份记忆", MemoryStore.MemoryMode.GLOBAL),
                            Triple("隔离", "本会话使用独立记忆，不影响其他对话", MemoryStore.MemoryMode.LOCAL),
                            Triple("关闭", "本会话不使用记忆", MemoryStore.MemoryMode.OFF),
                        ).forEach { (label, desc, mode) ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (currentMode == mode) Accent.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            MemoryStore.setMemoryMode(ctx, sk, mode)
                                            viewModel.refreshMemoryText()
                                            showMemoryModeDialog = false
                                        }
                                        .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, fontSize = 14.sp, color = Fg, fontWeight = FontWeight.Medium)
                                    Text(desc, fontSize = 11.sp, color = Fg2)
                                }
                                if (currentMode == mode) {
                                    Text("✓", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMemoryModeDialog = false }) { Text("完成", color = Accent) }
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
                    // 2026-08-14：失败原图 → 无用户输入，消息体为纯 [图片] 标记
                    TextButton(onClick = {
                        ocrFailedPath = null
                        viewModel.sendImageMessage("", listOf(failedPath))
                    }) { Text("发送原图", color = Accent) }
                },
                dismissButton = {
                    TextButton(onClick = { ocrFailedPath = null }) { Text(stringResource(R.string.action_cancel), color = Muted) }
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
                    TextButton(onClick = { showAttachSheet = false }) { Text(stringResource(R.string.action_cancel), color = Muted) }
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
                onEditProfile = {
                    showConfigsDialog = false
                    showProfileEdit = true
                },
                onDismiss = { showConfigsDialog = false },
            )
        }

        // ── 2026-08-07：编辑资料对话框（昵称 + Emoji 头像）──
        if (showProfileEdit) {
            ProfileEditDialog(
                profile = profile,
                onSave = { newProfile ->
                    ProfileStore.save(context, newProfile)
                    profile = newProfile
                    showProfileEdit = false
                },
                onDismiss = { showProfileEdit = false },
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
    onEditProfile: () -> Unit = {},
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
                // 2026-08-07：编辑资料入口（改名 / 换头像）
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentS)
                            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                onEditProfile()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.edit_profile_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Fg)
                        Text(stringResource(R.string.edit_profile_subtitle), fontSize = 11.sp, color = Muted2)
                    }
                }
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
                TextButton(onClick = { showDeleteConfirm = null }) { Text(stringResource(R.string.action_cancel), color = Muted) }
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
    hasError: Boolean,
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
    onMemoryMode: () -> Unit,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // 第五批 E-2：会话多选模式——长按进入，支持全选 / 批量删除 / 单条删除
    var selectionMode by remember { mutableStateOf(false) }
    var selectedNames by remember { mutableStateOf(setOf<String>()) }

    // 2026-08-06 对齐 RikkaHub 会话抽屉：置顶（本地记录，置顶排前 + 星标切换）
    val context = LocalContext.current
    var pinnedNames by remember { mutableStateOf(PinnedSessionsStore.load(context)) }
    // 2026-08-07（设计稿 #1）：会话最近访问时间戳（本地记录，用于今天/昨天/更早分组）
    var sessionTimes by remember { mutableStateOf(SessionTimestampsStore.loadAll(context)) }
    // 置顶会话排前（保持原有相对顺序）
    val orderedSessions =
        remember(sessions, pinnedNames) {
            sessions.sortedBy { if (it.name in pinnedNames) 0 else 1 }
        }
    // 今天/昨天/更早 分组键（Calendar 兼容 minSdk 23）
    fun dateGroupKey(ts: Long?): String {
        if (ts == null) return "今天"
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        return when {
            ts >= todayStart -> "今天"
            ts >= todayStart - 86_400_000L -> "昨天"
            else -> "更早"
        }
    }
    // 点击会话：记录最近访问时间（分组数据源）
    val selectWithTouch: (String, String) -> Unit = { name, path ->
        SessionTimestampsStore.touch(context, name)
        sessionTimes = sessionTimes + (name to System.currentTimeMillis())
        onSelectSession(path)
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
            // 离线模式指示器
            if (isOffline) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "（离线）",
                    fontSize = 10.sp,
                    color = Danger,
                    fontWeight = FontWeight.Medium,
                )
            }
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

            // 2026-08-08：会话记忆模式（互通/隔离/关闭）
            SidebarItem("记忆模式", onClick = onMemoryMode)
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
                // 2026-08-07（设计稿 #1）：会话分组（置顶 / 今天 / 昨天 / 更早）——分组小标题 + 各自列表
                val pinned = orderedSessions.filter { it.name in pinnedNames }
                val others = orderedSessions.filter { it.name !in pinnedNames }
                if (pinned.isNotEmpty()) {
                    GroupLabel("置顶")
                    pinned.forEach { session ->
                        SessionRow(
                            session = session,
                            isStreaming = isStreaming,
                            hasError = hasError,
                            selectionMode = selectionMode,
                            pinned = true,
                            onTogglePin = {
                                pinnedNames = PinnedSessionsStore.toggle(context, session.name)
                            },
                            selected = session.name in selectedNames,
                            onSelect = { selectWithTouch(session.name, session.path) },
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
                // 非置顶按最近访问时间分三组（无记录→今天）
                for (group in listOf("今天", "昨天", "更早")) {
                    val grouped = others.filter { dateGroupKey(sessionTimes[it.name]) == group }
                    if (grouped.isNotEmpty()) {
                        GroupLabel(group)
                        grouped.forEach { session ->
                            SessionRow(
                                session = session,
                                isStreaming = isStreaming,
                                hasError = hasError,
                                selectionMode = selectionMode,
                                pinned = false,
                                onTogglePin = {
                                    pinnedNames = PinnedSessionsStore.toggle(context, session.name)
                                },
                                selected = session.name in selectedNames,
                                onSelect = { selectWithTouch(session.name, session.path) },
                                onToggleSelect = {
                                    selectedNames =
                                        if (session.name in selectedNames) {
                                            selectedNames - session.name
                                        } else {
                                            selectedNames + session.name
                                        }
                                },
                                onLongPress = { enterSelection(session.name) },
                                onDelete = {
                                    SessionTimestampsStore.remove(context, session.name)
                                    sessionTimes = sessionTimes - session.name
                                    onDeleteSession(session.name)
                                },
                            )
                        }
                    }
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

            // ── AI 状态指示器（红/黄/绿：空闲/忙碌/错误）──
            // 纯前端：isStreaming=当前会话生成中（忙碌/黄）；hasError=出错（红）；否则空闲（绿）
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val aiColor =
                    when {
                        hasError -> Danger
                        isStreaming -> Warning
                        else -> Success
                    }
                val aiText =
                    when {
                        hasError -> "出错"
                        isStreaming -> "忙碌中…"
                        else -> "空闲"
                    }
                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(aiColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = aiText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = aiColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status?.label ?: "-",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    hasError: Boolean,
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
        // 会话 AI 状态点（红/黄/绿）：错误=红 / 生成中=黄（忙碌）/ 其余会话=绿（空闲）
        // 纯前端显示：serve 单活跃会话；当前会话流式期间其他会话不可切换，视为「排队等待」
        if (!selectionMode) {
            val dotColor =
                when {
                    session.current && hasError -> Danger
                    session.current && isStreaming -> Warning
                    else -> Success
                }
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

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
    runningToolName: String?,
    serverRunning: Boolean = false,
    pendingCount: Int,
    cumulativeCost: Double,
    cumulativeTokens: Long,
    cumulativePromptTokens: Long = 0,
    cumulativeCompletionTokens: Long = 0,
    cumulativeCacheHit: Long = 0,
    cumulativeCacheMiss: Long = 0,
    balance: String?,
    focusRequester: FocusRequester,
    imageProcessing: Boolean,
    pendingImages: List<PendingImage> = emptyList(),
    onRemoveImage: (PendingImage) -> Unit = {},
    onPickImage: () -> Unit,
    onAttach: () -> Unit = {},
    onQuickAction: ((String) -> Unit)? = null,
    // 2026-08-18：自定义快捷操作列表
    quickActions: List<AppSettingsStore.QuickAction> = AppSettingsStore.DEFAULT_QUICK_ACTIONS,
    showSidebar: Boolean = false,
    onToggleSidebar: () -> Unit = {},
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
            // ── 侧边栏切换（2026-08-07 移到输入框同一行，避免遮挡消息区） ──
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (showSidebar) Accent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onToggleSidebar() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (showSidebar) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = if (showSidebar) "Close sidebar" else "Open sidebar",
                    tint = if (showSidebar) Accent else Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            // Auto
            ToolbarButton("Auto", active = toolApprovalMode == "auto", accent = false) { onToggleAuto?.invoke() }
            // Plan
            ToolbarButton("Plan", active = planMode, accent = false) { onTogglePlan() }
            // YOLO
            ToolbarButton("YOLO", active = toolApprovalMode == "yolo", danger = true) { onToggleBypass?.invoke() }
            Spacer(modifier = Modifier.width(2.dp))

            // AI 状态（红/黄/绿）：流式中执行工具=黄「正在 <工具>…」/ 流式推理=黄「思考中…」/ 出错=红 / 空闲=绿
            // 2026-08-08：移到 YOLO 旁（原在最右需横向滑动才可见），加大到 8dp + 运行中脉冲
            val (dotColor, statusText) =
                when {
                    connectionState == ConnectionState.RECONNECTING -> Warning to "重连中…"
                    connectionState == ConnectionState.DISCONNECTED && isStreaming -> Danger to "连接断开"
                    isStreaming && runningToolName != null -> Warning to "正在 $runningToolName…"
                    isStreaming -> Warning to "思考中…"
                    // 2026-08-08：服务端 running 校准——本地流已收尾但服务端 turn 仍活动（如断线重连后工具还在跑）
                    serverRunning -> Warning to "运行中…"
                    connectionState == ConnectionState.CONNECTED -> Success to "空闲"
                    else -> Muted2 to "就绪"
                }
            val pulse =
                rememberInfiniteTransition(label = "aiStatus")
                    .animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec =
                            infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        label = "pulse",
                    )
            Box(
                modifier =
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .alpha(if (isStreaming || connectionState != ConnectionState.CONNECTED) pulse.value else 1f),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = dotColor,
            )

            // ── 快捷任务模板（run 常用功能点击项）──
            if (onQuickAction != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Border))
                Spacer(modifier = Modifier.width(2.dp))
                // 2026-08-18：使用自定义快捷操作
                quickActions.forEach { action ->
                    QuickActionButton(action.label, onClick = { onQuickAction(action.prompt) })
                }
            }

            // 分隔
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Border))

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

        // ── 会话 token 累计条（输入框上方常驻，始终显示；0 值也可见布局位置） ──
        run {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            val cacheTotal = cumulativeCacheHit + cumulativeCacheMiss
            val cachePct: String? =
                if (cacheTotal > 0) {
                    val pct = cumulativeCacheHit.toDouble() / cacheTotal * 100.0
                    if (pct >= 1.0) "${pct.toInt()}%" else "%.1f%%".format(pct)
                } else {
                    null
                }
            val cacheShow = cachePct?.let { "${fmtTokens(cumulativeCacheHit)}·$it" } ?: "${fmtTokens(cumulativeCacheHit)}"
            // 2026-08-14：有累计（/status used 生效）但输入/输出明细恒 0 → 服务端未回传 usage（SSE 事件与 lastUsage 均缺失）
            val usageMissing =
                cumulativeTokens > 0 &&
                    cumulativePromptTokens == 0L &&
                    cumulativeCompletionTokens == 0L
            val summary =
                "会话统计｜↑输入 ${fmtTokens(cumulativePromptTokens)} · 缓存 $cacheShow · ↓输出 ${fmtTokens(cumulativeCompletionTokens)}（累计 ${fmtTokens(cumulativeTokens)} · ${fmtCost(cumulativeCost)}）${if (usageMissing) "（usage 未回传）" else ""}"
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(summary))
                            Toast.makeText(context, "会话统计已复制", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "会话",
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "↑${fmtTokens(cumulativePromptTokens)} · 缓存 $cacheShow · ↓${fmtTokens(cumulativeCompletionTokens)}${if (usageMissing) "（usage 未回传）" else ""}",
                    color = Fg2,
                    fontSize = 11.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "累计 ${fmtTokens(cumulativeTokens)} · ${fmtCost(cumulativeCost)}",
                    color = Muted2,
                    fontSize = 11.sp,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制统计",
                    tint = Muted2,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        // ── AI 状态条（输入框上方常驻，替代工具栏微小状态点）──
        if (isStreaming || connectionState == ConnectionState.RECONNECTING || connectionState == ConnectionState.DISCONNECTED) {
            val statusColor: Color
            val statusLabel: String
            val statusIcon: String
            when {
                connectionState == ConnectionState.RECONNECTING -> {
                    statusColor = Warning; statusIcon = "⟳"; statusLabel = "重连中…"
                }
                connectionState == ConnectionState.DISCONNECTED -> {
                    statusColor = Danger; statusIcon = "✕"; statusLabel = "连接断开"
                }
                isStreaming && runningToolName != null -> {
                    statusColor = Warning; statusIcon = "⚡"; statusLabel = "正在 $runningToolName…"
                }
                isStreaming -> {
                    statusColor = Warning; statusIcon = "💭"; statusLabel = "思考中…"
                }
                else -> {
                    statusColor = Success; statusIcon = "●"; statusLabel = "空闲"
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isStreaming) {
                    Text(
                        text = "点击 ⬛ 取消",
                        fontSize = 11.sp,
                        color = Muted2,
                    )
                }
            }
        }

        // ── 待发送图片附件条（2026-08-08：选图先加到对话框，随消息一起发送；最多 3 张横排）──
        if (pendingImages.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pendingImages.forEach { img ->
                    // 2026-08-14：图片下方展示 OCR 识别文本（仅本地预览，不进消息体）
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            AsyncImage(
                                model = File(img.imagePath),
                                contentDescription = "待发送图片",
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Bg2),
                            )
                            // 右上角删除
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC000000))
                                        .clickable { onRemoveImage(img) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除图片",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                        if (img.ocrText.isNotBlank()) {
                            Text(
                                text = "▣ ${img.ocrText.replace('\n', ' ').take(24)}",
                                fontSize = 10.sp,
                                color = Muted2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                // 计数提示
                Text(
                    "${pendingImages.size}/3",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Muted2,
                )
            }
        }

        // ── 输入框（输入区 + 发送/打断）──
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
                                    (inputText.isNotBlank() || pendingImages.isNotEmpty())
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

            // 排队计数 badge（忙时入队的消息数）
            if (pendingCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Warning.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "$pendingCount 排队中",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Warning,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // 2026-08-17（对标 ChatGPT/Claude/RikkaHub 稳定按钮不换位）：
            // 发送箭头常驻（生成中可点 = 打断并发送新消息）；打断按钮仅在流式时附加在左侧。
            if (isStreaming) {
                // 打断按钮（红色方块，仅取消当前任务，不发送输入内容）
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Danger),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            // 发送箭头（生成中也可点：走 sendMessage 的「打断并发送」逻辑）
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() || pendingImages.isNotEmpty(),
                modifier = Modifier.size(44.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (inputText.isNotBlank() || pendingImages.isNotEmpty()) Accent else Panel2,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↑", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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

// 2026-08-07（设计稿 #14）：token 数格式化（1000→1k / 1000000→1M）
private fun fmtTokens(n: Long): String =
    when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format("%.1fk", n / 1_000.0)
        else -> n.toString()
    }

// 费用格式化（$）
private fun fmtCost(c: Double): String =
    if (c > 0) String.format("$%.2f", c) else "$0"

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


/** 2026-08-06 优化：会话分组小标题 */
@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = Muted2,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp),
    )
}

// ═══════════════════════════════════════════════════════════════════
// 2026-08-07：图片转述确认（OCR 增强）
// 上传图片后先弹确认框：预览图片 + 可折叠编辑「转述内容」，
// 避免 AI 对图片内容误判；确认后连同转述文本一起发送。
// ═══════════════════════════════════════════════════════════════════

/** OCR 成功后待确认的图片与识别文本。 */
private data class PendingImage(
    val imagePath: String,
    val ocrText: String,
)



// ═══════════════════════════════════════════════
// 2026-08-07：编辑资料对话框（改名 / Emoji 头像更换）
// ═══════════════════════════════════════════════

@Composable
private fun ProfileEditDialog(
    profile: ProfileStore.Profile,
    onSave: (ProfileStore.Profile) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var emoji by remember { mutableStateOf(profile.avatarEmoji) }
    val emojiOptions =
        listOf("🤖", "🦊", "🐱", "🐶", "🦉", "🐼", "🐸", "🐵", "🦄", "🐯", "🐨", "🐷", "🦁", "🐧")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile_title), color = Fg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 昵称
                LabeledField(stringResource(R.string.edit_profile_name)) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(Panel)
                                .border(1.dp, Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                // Emoji 头像选择
                Text(stringResource(R.string.edit_profile_avatar), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Muted)
                // 当前头像预览
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Bg2)
                                .border(1.dp, Border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji.ifBlank { "R" }, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (emoji.isBlank()) stringResource(R.string.edit_profile_no_avatar) else stringResource(R.string.edit_profile_has_avatar),
                        fontSize = 11.sp,
                        color = Muted2,
                    )
                }
                // Emoji 网格
                Column {
                    emojiOptions.chunked(7).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { e ->
                                Box(
                                    modifier =
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (e == emoji) AccentS else Panel)
                                            .border(
                                                1.dp,
                                                if (e == emoji) Accent.copy(alpha = 0.6f) else Border,
                                                CircleShape,
                                            )
                                            .clickable { emoji = e }
                                            .padding(4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(e, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ProfileStore.Profile(name, emoji)) }) {
                Text("保存", color = Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = Muted) }
        },
        containerColor = Panel,
    )
}

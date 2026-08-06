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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.WebDavStore
import com.reasonix.agents.data.WebDavSyncManager
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2
private val Success: Color @Composable get() = LocalPalette.current.success
private val Danger: Color @Composable get() = LocalPalette.current.danger

/**
 * WebDAV 同步二级界面（第八批，坚果云默认；第九批改为通用 WebDAV）。
 *
 * - WebDAV 配置：服务器地址（默认坚果云 https://dav.jianguoyun.com/dav/，可改任意 WebDAV 服务）、账号、密码
 *   （[CredentialCrypto] 加密存储）、远程备份路径；
 * - 手动同步：上传备份（含完整会话历史，复用第五批导出逻辑）/ 下载备份并恢复
 *   （复用第五批导入逻辑）；
 * - 定时同步：开关 + 每天自动上传时间（AlarmManager 后台执行，上传配置备份）；
 * - 同步状态：上次同步时间 + 结果（成功 / 失败 + 错误信息）。
 */
@Composable
fun SettingsWebDavScreen(
    onBack: () -> Unit,
    onSettingsRestored: (AppSettingsStore.Settings) -> Unit,
    viewModel: ChatViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── 配置状态（onValueChange 实时保存）──
    var settings by remember { mutableStateOf(WebDavStore.load(context)) }
    var passwordVisible by remember { mutableStateOf(false) }

    // ── 同步执行状态 ──
    var syncing by remember { mutableStateOf(false) }
    var syncingLabel by remember { mutableStateOf("") }

    // ── 结果弹窗 ──
    var resultTitle by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var resultIsError by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    fun saveSettings(newSettings: WebDavStore.WebDavSettings) {
        settings = newSettings
        WebDavStore.save(context, newSettings)
    }

    fun refreshStatus() {
        settings = WebDavStore.load(context)
    }

    fun showSyncResult(
        isError: Boolean,
        title: String,
        message: String,
    ) {
        resultIsError = isError
        resultTitle = title
        resultMessage = message
        showResult = true
    }

    // ── 上传备份（手动，含完整会话历史）──
    fun doUpload() {
        if (syncing) return
        syncing = true
        syncingLabel = "正在收集并上传备份…"
        scope.launch {
            val export = viewModel.exportBackup("")
            if (export.error != null) {
                syncing = false
                showSyncResult(true, "上传失败", export.error)
                return@launch
            }
            val result =
                withContext(Dispatchers.IO) {
                    WebDavSyncManager.upload(context, export.json.orEmpty())
                }
            WebDavStore.recordSyncResult(context, result.ok, result.message)
            syncing = false
            refreshStatus()
            showSyncResult(!result.ok, if (result.ok) "上传成功" else "上传失败", result.message)
        }
    }

    // ── 下载备份（手动，下载后恢复配置与会话）──
    fun doDownload() {
        if (syncing) return
        syncing = true
        syncingLabel = "正在下载并恢复…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { WebDavSyncManager.download(context) }
            if (!result.ok) {
                WebDavStore.recordSyncResult(context, false, result.message)
                syncing = false
                refreshStatus()
                showSyncResult(true, "下载失败", result.message)
                return@launch
            }
            // 复用第五批导入逻辑恢复
            val import = viewModel.importBackup(result.json.orEmpty(), "")
            val message =
                if (import.success) {
                    "下载成功，已恢复服务器配置 / 主题 / 模型 / 会话"
                } else {
                    "下载成功，但恢复失败：${import.message}"
                }
            WebDavStore.recordSyncResult(context, import.success, message)
            import.restoredSettings?.let { onSettingsRestored(it) }
            syncing = false
            refreshStatus()
            showSyncResult(!import.success, if (import.success) "下载并恢复成功" else "恢复失败", import.message)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding()
                .imePadding(),
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
                    text = "WebDAV 同步",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "通过 WebDAV 把备份（配置 + 会话）同步到任意 WebDAV 服务（默认坚果云），换机 / 重装后一键恢复。服务器地址可在下方修改。",
                fontSize = 12.sp,
                color = Muted2,
                lineHeight = 17.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── WebDAV 配置 ──
            SectionTitle("WebDAV 配置")
            WebDavSection(
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp)) },
                title = "连接配置",
                description = "通用 WebDAV：默认坚果云（https://dav.jianguoyun.com/dav/），可填任意 WebDAV 服务地址。密码将加密存储在本机。",
            ) {
                LabeledField("服务器地址") {
                    WebDavTextField(
                        value = settings.serverUrl,
                        onValueChange = { v -> saveSettings(settings.copy(serverUrl = v)) },
                        hint = WebDavStore.DEFAULT_SERVER_URL,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LabeledField("账号") {
                    WebDavTextField(
                        value = settings.username,
                        onValueChange = { v -> saveSettings(settings.copy(username = v)) },
                        hint = "账号（坚果云为邮箱）",
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LabeledField("密码") {
                    WebDavPasswordField(
                        value = settings.password,
                        visible = passwordVisible,
                        onValueChange = { v -> saveSettings(settings.copy(password = v)) },
                        onToggleVisible = { passwordVisible = !passwordVisible },
                        hint = "应用密码（坚果云为应用密码，非登录密码）",
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LabeledField("远程备份路径") {
                    WebDavTextField(
                        value = settings.remotePath,
                        onValueChange = { v -> saveSettings(settings.copy(remotePath = v)) },
                        hint = WebDavStore.DEFAULT_REMOTE_PATH,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 手动同步 ──
            SectionTitle("手动同步")
            WebDavSection(
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp)) },
                title = "立即同步",
                description = "上传：生成完整备份（含全部会话历史）写入 WebDAV；下载：拉取备份并恢复配置与会话。",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WebDavActionButton(
                        text = "上传备份",
                        icon = Icons.Default.CloudUpload,
                        enabled = !syncing,
                        onClick = { doUpload() },
                        modifier = Modifier.weight(1f),
                    )
                    WebDavActionButton(
                        text = "下载备份",
                        icon = Icons.Default.CloudDownload,
                        enabled = !syncing,
                        onClick = { doDownload() },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (syncing) {
                    Text(syncingLabel, fontSize = 12.sp, color = Muted2, modifier = Modifier.padding(top = 6.dp))
                }
                if (!settings.isConfigured) {
                    Text(
                        "配置不完整：请先填写服务器地址、账号、密码与远程路径。",
                        fontSize = 11.sp,
                        color = Danger,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 定时同步 ──
            SectionTitle("定时同步")
            WebDavSection(
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp)) },
                title = "每天自动上传备份",
                description = "开启后每天在指定时间自动上传配置备份（服务器配置 / 主题 / 模型，不含会话历史），失败会发通知提醒。",
            ) {
                SettingSwitch(
                    title = "启用定时同步",
                    checked = settings.autoSyncEnabled,
                    onCheckedChange = { on ->
                        val newSettings = settings.copy(autoSyncEnabled = on)
                        saveSettings(newSettings)
                        if (on) {
                            WebDavSyncManager.scheduleAutoSync(context, newSettings)
                        } else {
                            WebDavSyncManager.cancelAutoSync(context)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(6.dp))
                LabeledField("每天自动上传时间（HH:mm）") {
                    WebDavTextField(
                        value = settings.autoSyncTime,
                        onValueChange = { v ->
                            val newSettings = settings.copy(autoSyncTime = v)
                            saveSettings(newSettings)
                            // 时间变化后重设闹钟（仅定时开关开启时）
                            if (newSettings.autoSyncEnabled) {
                                WebDavSyncManager.scheduleAutoSync(context, newSettings)
                            }
                        },
                        hint = WebDavStore.DEFAULT_AUTO_TIME,
                        numeric = true,
                    )
                }
                if (settings.autoSyncEnabled) {
                    val (hour, minute) = WebDavSyncManager.parseTime(settings.autoSyncTime)
                    Text(
                        "将在每天 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} 自动上传配置备份。",
                        fontSize = 11.sp,
                        color = Fg2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 同步状态 ──
            SectionTitle("同步状态")
            WebDavSection(
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp)) },
                title = "最近一次同步",
                description = "记录最近一次手动或定时同步的结果。",
            ) {
                if (settings.lastSyncAt <= 0L) {
                    Text("尚未执行过同步。", fontSize = 12.sp, color = Muted2)
                } else {
                    InfoRow("上次同步", formatSyncTime(settings.lastSyncAt))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("结果", fontSize = 13.sp, color = Muted, modifier = Modifier.width(90.dp))
                        Text(
                            text = if (settings.lastSyncOk) "✅ 成功" else "❌ 失败",
                            fontSize = 13.sp,
                            color = if (settings.lastSyncOk) Success else Danger,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (settings.lastSyncMessage.isNotBlank()) {
                        Text(
                            settings.lastSyncMessage,
                            fontSize = 11.sp,
                            color = if (settings.lastSyncOk) Fg2 else Danger,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "提示：手动「上传备份」包含全部会话历史；定时同步为省电与稳定，仅上传配置备份（不含会话历史）。",
                fontSize = 11.sp,
                color = Muted2,
                lineHeight = 16.sp,
            )
        }
    }

    // ── 结果弹窗 ──
    if (showResult) {
        AlertDialog(
            onDismissRequest = { showResult = false },
            title = {
                Text(
                    resultTitle,
                    color = if (resultIsError) Danger else Success,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(resultMessage, fontSize = 13.sp, color = Fg2, lineHeight = 19.sp)
            },
            confirmButton = {
                TextButton(onClick = { showResult = false }) { Text("好的", color = Accent) }
            },
            containerColor = Panel,
        )
    }
}

// ═══════════════════════════════════════════════
// 区块组件
// ═══════════════════════════════════════════════

@Composable
private fun WebDavSection(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, fontSize = 15.sp, color = Fg, fontWeight = FontWeight.SemiBold)
        }
        Text(description, fontSize = 12.sp, color = Muted2, lineHeight = 17.sp)
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun WebDavTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    numeric: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LocalPalette.current.bg2)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
            cursorBrush = SolidColor(Accent),
            singleLine = true,
            keyboardOptions =
                if (numeric) {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions(keyboardType = KeyboardType.Text)
                },
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, fontSize = 12.sp, color = Muted2)
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WebDavPasswordField(
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    hint: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LocalPalette.current.bg2)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
            cursorBrush = SolidColor(Accent),
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, fontSize = 12.sp, color = Muted2)
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggleVisible, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (visible) "隐藏" else "显示",
                tint = Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun WebDavActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) Accent else Accent.copy(alpha = 0.4f))
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 同步时间格式化（yyyy-MM-dd HH:mm）。 */
private fun formatSyncTime(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

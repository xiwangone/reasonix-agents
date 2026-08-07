package com.reasonix.agents.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.BackupManager
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.viewmodel.BackupImportResult
import com.reasonix.agents.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
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
 * 备份与恢复二级界面（第五批 E-1）。
 *
 * - 导出：生成单文件 JSON 备份（服务器配置多套【凭据加密】/主题设置/自定义模型/全部会话历史），
 *   通过 SAF（CreateDocument）写入用户选择的位置；可选密码保护（PBKDF2 + AES-GCM 加密凭据区）。
 * - 导入：通过 SAF（OpenDocument）选择备份文件，解析恢复配置与会话；
 *   凭据解密失败（密码错误 / 换机密钥不可用）时给出明确提示。
 */
@Composable
fun SettingsBackupScreen(
    onBack: () -> Unit,
    onSettingsRestored: (AppSettingsStore.Settings) -> Unit,
    viewModel: ChatViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── 导出状态 ──
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordVisible by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    // ── 导入状态 ──
    var importPassword by remember { mutableStateOf("") }
    var importPasswordVisible by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

    // ── 结果提示 ──
    var resultTitle by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var resultIsError by remember { mutableStateOf(false) }

    // 导出：SAF 创建文件
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            exporting = true
            scope.launch {
                val result = viewModel.exportBackup(exportPassword)
                exporting = false
                if (result.error != null) {
                    resultIsError = true
                    resultTitle = "导出失败"
                    resultMessage = result.error
                } else if (BackupManager.write(context, uri, result.json.orEmpty())) {
                    resultIsError = false
                    resultTitle = "导出成功"
                    resultMessage =
                        buildString {
                            append("备份已保存（${result.serverCount} 套服务器配置，${result.sessionCount} 个会话历史）")
                            if (exportPassword.isNotBlank()) append("，已启用密码保护")
                        }
                } else {
                    resultIsError = true
                    resultTitle = "导出失败"
                    resultMessage = "无法写入所选文件，请重试"
                }
                showResult = true
            }
        }

    // 导入：SAF 打开文件
    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            importing = true
            scope.launch {
                val json = BackupManager.read(context, uri)
                importing = false
                if (json == null) {
                    resultIsError = true
                    resultTitle = "导入失败"
                    resultMessage = "无法读取所选文件"
                } else {
                    val result: BackupImportResult = viewModel.importBackup(json, importPassword)
                    resultIsError = !result.success
                    resultTitle = if (result.success) "导入成功" else "导入失败"
                    resultMessage = result.message
                    result.restoredSettings?.let { onSettingsRestored(it) }
                }
                showResult = true
            }
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
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
                    text = "备份与恢复",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 导出备份 ──
            BackupSection(
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp)) },
                title = "导出备份",
                description = "生成单文件 JSON 备份：服务器配置（多套，密码/Token 加密）、主题设置、自定义模型列表、全部会话历史。",
            ) {
                BackupPasswordField(
                    value = exportPassword,
                    visible = exportPasswordVisible,
                    onValueChange = { exportPassword = it },
                    onToggleVisible = { exportPasswordVisible = !exportPasswordVisible },
                    hint = "可选：设置备份密码（凭据将用密码加密）",
                )
                Spacer(modifier = Modifier.height(10.dp))
                PrimaryButton(
                    text = "导出备份文件",
                    enabled = !exporting,
                    onClick = {
                        val ts = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportLauncher.launch("reasonix-backup-$ts.json")
                    },
                )
                if (exporting) {
                    Text("正在收集会话历史…", fontSize = 12.sp, color = Muted2, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 导入备份 ──
            BackupSection(
                icon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp)) },
                title = "导入备份",
                description = "选择备份文件并恢复：服务器配置、主题设置、自定义模型与会话历史。凭据解密失败时会提示。",
            ) {
                BackupPasswordField(
                    value = importPassword,
                    visible = importPasswordVisible,
                    onValueChange = { importPassword = it },
                    onToggleVisible = { importPasswordVisible = !importPasswordVisible },
                    hint = "若备份设置了密码保护，请输入备份密码",
                )
                Spacer(modifier = Modifier.height(10.dp))
                PrimaryButton(
                    text = "选择备份文件导入",
                    enabled = !importing,
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                )
                if (importing) {
                    Text("正在解析并恢复…", fontSize = 12.sp, color = Muted2, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 说明 ──
            Text(
                "提示：无密码备份的凭据使用本机密钥（AndroidKeyStore）加密，恢复到其它设备时密钥不可用，相关凭据会按空值恢复并提示。",
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
private fun BackupSection(
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
private fun BackupPasswordField(
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
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) Accent else Accent.copy(alpha = 0.4f))
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

package com.reasonix.agents.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.CloudFilesApi
import com.reasonix.agents.data.CloudFilesStore
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import java.io.File

/**
 * filebrowser 云盘文件页（2026-08-06）。
 *
 * - 中转站定位：RikkaHub Agents ↔ Reasonix Agents 文件中转 + 个人云盘（ECS filebrowser，
 *   根 /root，nginx Basic Auth 前置）；
 * - 功能：登录（JWT）/ 列目录 / 进入目录 / 上传（本地文件）/ 下载 / 删除 / 新建目录；
 * - 与「WebDAV 同步」（坚果云等，备份通道）并列：本页面向文件浏览与中转。
 */
@Composable
fun CloudFilesScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(CloudFilesStore.load(context)) }
    var token by remember { mutableStateOf<String?>(null) }
    var currentPath by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<CloudFilesApi.RemoteFile>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val api = remember { CloudFilesApi(context) }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun refresh() {
        val t = token ?: return
        loading = true
        error = null
        scope.launch {
            try {
                entries = api.list(t, settings, currentPath)
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
                // token 失效 → 清空要求重新登录
                token = null
            }
            loading = false
        }
    }

    fun doLogin() {
        if (!settings.isConfigured) {
            toast("请先填写服务器地址 / 账号 / 密码")
            return
        }
        CloudFilesStore.save(context, settings)
        loading = true
        error = null
        scope.launch {
            try {
                token = api.login(settings)
                entries = api.list(token!!, settings, "")
            } catch (e: Exception) {
                error = e.message ?: "登录失败"
            }
            loading = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.bg)
                .safeDrawingPadding(),
    ) {
        // ── 标题栏 ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = palette.fg)
            }
            Icon(Icons.Default.Cloud, "云盘", tint = palette.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("云盘文件", color = palette.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (token != null) {
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, "刷新", tint = palette.fg)
                }
                IconButton(onClick = { showNewFolder = true }) {
                    Icon(Icons.Default.Add, "新建目录", tint = palette.fg)
                }
            }
        }

        // ── 配置区（未登录时显示）──
        if (token == null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                SectionTitle("连接")
                LabeledField("服务器地址") {
                    CloudTextField(
                        value = settings.serverUrl,
                        onValueChange = { v -> settings = settings.copy(serverUrl = v) },
                        hint = CloudFilesStore.DEFAULT_SERVER_URL,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LabeledField("账号") {
                    CloudTextField(
                        value = settings.username,
                        onValueChange = { v -> settings = settings.copy(username = v) },
                        hint = "filebrowser 账号",
                    )
                }
                Spacer(Modifier.height(8.dp))
                LabeledField("密码") {
                    CloudPasswordField(
                        value = settings.password,
                        onValueChange = { v -> settings = settings.copy(password = v) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { doLogin() }, enabled = !loading) {
                        Icon(Icons.Default.Login, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("登录云盘", color = palette.accent)
                    }
                    if (loading) {
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = palette.accent, strokeWidth = 2.dp)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("错误：$error", color = palette.danger, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "说明：filebrowser 云盘（默认 cloud.louxia.xyz）作为 RikkaHub Agents 与 Reasonix Agents 的文件中转站及个人云盘。登录后可在「文件」Tab 使用。",
                    color = palette.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(16.dp))
            }
        } else {
            // ── 文件浏览区（已登录）──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (currentPath.isEmpty()) "/" else "/$currentPath",
                    color = palette.fg2,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (currentPath.isNotEmpty()) {
                    TextButton(onClick = {
                        currentPath = currentPath.substringBeforeLast('/', "")
                        refresh()
                    }) {
                        Text("上级", color = palette.accent, fontSize = 13.sp)
                    }
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(entries, key = { it.name + it.isDir }) { f ->
                        FileRow(
                            file = f,
                            onClick = {
                                if (f.isDir) {
                                    currentPath =
                                        if (currentPath.isEmpty()) f.name else "$currentPath/${f.name}"
                                    refresh()
                                }
                            },
                            onDownload = {
                                scope.launch {
                                    val t = token ?: return@launch
                                    // 下载到应用缓存目录
                                    val dir = File(context.cacheDir, "cloud")
                                    val target = File(dir, f.name)
                                    val ok = api.download(t, settings, if (currentPath.isEmpty()) f.name else "$currentPath/${f.name}", target)
                                    toast(if (ok) "已下载：${target.absolutePath}" else "下载失败")
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    val t = token ?: return@launch
                                    val ok =
                                        api.delete(
                                            t,
                                            settings,
                                            if (currentPath.isEmpty()) f.name else "$currentPath/${f.name}",
                                        )
                                    toast(if (ok) "已删除" else "删除失败")
                                    refresh()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // ── 新建目录对话框 ──
    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("新建目录", color = palette.fg) },
            text = {
                LabeledField("目录名") {
                    CloudTextField(
                        value = newFolderName,
                        onValueChange = { v -> newFolderName = v },
                        hint = "如：shared",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFolderName.trim()
                    if (name.isEmpty()) {
                        toast("目录名不能为空")
                        return@TextButton
                    }
                    scope.launch {
                        val t = token ?: return@launch
                        val p = if (currentPath.isEmpty()) name else "$currentPath/$name"
                        val ok = api.mkdir(t, settings, p)
                        toast(if (ok) "已创建" else "创建失败")
                        newFolderName = ""
                        showNewFolder = false
                        refresh()
                    }
                }) { Text("创建", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) { Text("取消", color = palette.muted) }
            },
        )
    }
}

/** 云盘文件行：文件夹可进入，文件可下载 / 删除。 */
@Composable
private fun FileRow(
    file: CloudFilesApi.RemoteFile,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = file.isDir, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (file.isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (file.isDir) palette.accent else palette.muted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, color = palette.fg, fontSize = 14.sp, maxLines = 1)
            if (!file.isDir) {
                Text(fmtSize(file.size), color = palette.muted, fontSize = 11.sp)
            }
        }
        if (!file.isDir) {
            IconButton(onClick = onDownload, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Download, "下载", tint = palette.fg2, modifier = Modifier.size(18.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Delete, "删除", tint = palette.danger, modifier = Modifier.size(18.dp))
        }
    }
}

private fun fmtSize(size: Long): String = when {
    size >= 1024L * 1024 * 1024 -> String.format("%.1f GB", size / 1024.0 / 1024 / 1024)
    size >= 1024L * 1024 -> String.format("%.1f MB", size / 1024.0 / 1024)
    size >= 1024L -> String.format("%.1f KB", size / 1024.0)
    else -> "$size B"
}

/** 云盘配置输入框（浅色卡片式）。 */
@Composable
private fun CloudTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
) {
    val palette = LocalPalette.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = palette.fg, fontSize = 14.sp),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(palette.panel, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(hint, color = palette.muted, fontSize = 14.sp)
                }
                inner()
            }
        },
    )
}

/** 云盘密码输入框（带显隐切换）。 */
@Composable
private fun CloudPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val palette = LocalPalette.current
    var visible by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(palette.panel, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = palette.fg, fontSize = 14.sp),
            visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None
            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        )
        IconButton(onClick = { visible = !visible }, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

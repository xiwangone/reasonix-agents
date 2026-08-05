package com.reasonix.agents.ui.screen

import com.reasonix.agents.data.CustomModelStore
import com.reasonix.agents.data.model.ModelInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.CustomModelStore.CustomModel
import com.reasonix.agents.data.api.GitHubReleaseApi
import com.reasonix.agents.data.model.StatusInfo
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 调色板 — 从 LocalPalette 读取（支持主题切换）
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
private val Danger: Color @Composable get() = LocalPalette.current.danger

/**
 * 设置页 — 登录后全量设置（批 A-5）。
 * 含：主题预设体系、模型（下拉切换 + 添加/删除 + 刷新）、显示选项、
 * 网络（连接超时 / SSE 重连）、服务器信息、系统提示词、CI 监控、关于（多入口 + 检测更新）。
 */
@Composable
fun SettingsScreen(
    serverUrl: String,
    status: StatusInfo?,
    models: List<ModelInfo> = emptyList(),
    customModels: List<CustomModel> = emptyList(),
    currentModel: String = "",
    systemPrompt: String? = null,
    settings: AppSettingsStore.Settings,
    ciSettings: com.reasonix.agents.data.CiMonitorStore.CiSettings = com.reasonix.agents.data.CiMonitorStore.CiSettings(),
    onSettingsChange: (AppSettingsStore.Settings) -> Unit = {},
    onShowReasoningChange: (Boolean) -> Unit = {},
    onShowTokensChange: (Boolean) -> Unit = {},
    onModelSelect: (String) -> Unit = {},
    onRefreshModels: () -> Unit = {},
    onAddCustomModel: (CustomModel) -> Unit = {},
    onRemoveCustomModel: (String) -> Unit = {},
    onCiSettingsChange: (com.reasonix.agents.data.CiMonitorStore.CiSettings) -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onClose: (() -> Unit)? = null,
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

    var showAddModel by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }

    fun checkUpdate() {
        if (checkingUpdate) return
        checkingUpdate = true
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
                            "发现新版本 v${release.tagName}（当前 v$versionName）\n\n点击「前往下载」跳转 Release 页面。"
                        } else {
                            "暂时没有更新"
                        }
                    }
                } catch (e: Exception) {
                    // 批 C-6：网络错误提示「网络错误，请稍后重试」
                    "网络错误，请稍后重试"
                }
            }
            checkingUpdate = false
            updateResult = result
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
            // ── 标题栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设置",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f)
                )
                if (onClose != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .clickable { onClose() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("关闭", fontSize = 13.sp, color = Muted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 主题预设体系（批 A-2）──
            SectionTitle("主题")
            Text(
                text = "配色风格",
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChip(
                    "品牌紫蓝",
                    settings.themePreset == AppSettingsStore.THEME_PRESET_BRAND,
                    { onSettingsChange(settings.copy(themePreset = AppSettingsStore.THEME_PRESET_BRAND)) }
                )
                ThemeChip(
                    "Material",
                    settings.themePreset == AppSettingsStore.THEME_PRESET_MATERIAL,
                    { onSettingsChange(settings.copy(themePreset = AppSettingsStore.THEME_PRESET_MATERIAL)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "明暗模式",
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChip("跟随系统", settings.themeMode == AppSettingsStore.THEME_MODE_SYSTEM, {
                    onSettingsChange(settings.copy(themeMode = AppSettingsStore.THEME_MODE_SYSTEM))
                })
                ThemeChip("浅色", settings.themeMode == AppSettingsStore.THEME_MODE_LIGHT, {
                    onSettingsChange(settings.copy(themeMode = AppSettingsStore.THEME_MODE_LIGHT))
                })
                ThemeChip("深色", settings.themeMode == AppSettingsStore.THEME_MODE_DARK, {
                    onSettingsChange(settings.copy(themeMode = AppSettingsStore.THEME_MODE_DARK))
                })
            }
            Spacer(modifier = Modifier.height(10.dp))

            // ── 语言（批 A-5：全局生效；当前版本仅中文文案，偏好先持久化）──
            Text(
                text = "语言",
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChip("简体中文", settings.language == "zh", {
                    onSettingsChange(settings.copy(language = "zh"))
                })
                ThemeChip("English", settings.language == "en", {
                    onSettingsChange(settings.copy(language = "en"))
                })
            }
            Text(
                text = "当前版本仅提供中文界面，语言偏好已保存（预留）",
                fontSize = 10.sp,
                color = Muted2,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 模型（批 B-9 添加模型 / B-10 下拉切换 + 刷新）──
            SectionTitle("模型")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 模型下拉：服务端模型 + 自定义模型合并
                ModelDropdown(
                    models = models,
                    customModels = customModels,
                    currentModel = currentModel,
                    onSelect = onModelSelect,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRefreshModels,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg2)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新模型列表", tint = Muted, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { showAddModel = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg2)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加模型", tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 自定义模型列表（可删除）
            if (customModels.isNotEmpty()) {
                Text(
                    text = "自定义模型（本地）",
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                customModels.forEach { cm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cm.name,
                                fontSize = 13.sp,
                                color = if (cm.name == currentModel) Accent else Fg,
                                fontWeight = if (cm.name == currentModel) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = buildString {
                                    append(if (cm.provider == "builtin") "内置" else "自定义")
                                    if (cm.baseUrl.isNotBlank()) append(" · ${cm.baseUrl}")
                                    append(" · ${compatLabel(cm.compat)}")
                                },
                                fontSize = 10.sp,
                                color = Muted2
                            )
                        }
                        IconButton(
                            onClick = { onRemoveCustomModel(cm.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除模型", tint = Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (models.isEmpty() && customModels.isEmpty()) {
                InfoRow("当前模型", currentModel.ifEmpty { "—" })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 显示选项 ──
            SectionTitle("显示")
            SettingSwitch(
                title = "显示推理过程",
                checked = settings.showReasoning,
                onCheckedChange = onShowReasoningChange
            )
            SettingSwitch(
                title = "显示 Token / 费用",
                checked = settings.showTokens,
                onCheckedChange = onShowTokensChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 网络（批 B-11 设置项扩展）──
            SectionTitle("网络")
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

            Spacer(modifier = Modifier.height(16.dp))

            // ── 服务器信息 ──
            SectionTitle("服务器")
            InfoRow("地址", serverUrl)
            InfoRow("标签", status?.label ?: "—")
            InfoRow("计划模式", status?.plan?.let { if (it) "开" else "关" } ?: "—")
            InfoRow("工具审批", status?.toolApprovalMode ?: "—")
            InfoRow("余额", status?.balance?.display ?: "—")

            Spacer(modifier = Modifier.height(16.dp))

            // ── 系统提示词（只读）──
            SectionTitle("系统提示词")
            if (systemPrompt.isNullOrBlank()) {
                InfoRow("提示词", "—")
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = systemPrompt,
                        fontSize = 12.sp,
                        color = Fg2,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "只读：服务端不支持修改系统提示词",
                    fontSize = 11.sp,
                    color = Muted2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── CI 监控 ──
            SectionTitle("CI 监控")
            SettingSwitch(
                title = "启用悬浮球",
                checked = ciSettings.enabled,
                onCheckedChange = { on ->
                    onCiSettingsChange(ciSettings.copy(enabled = on))
                }
            )
            if (ciSettings.enabled) {
                Spacer(modifier = Modifier.height(8.dp))

                // GitHub Token（脱敏展示）
                InfoRow("GitHub Token", if (ciSettings.githubToken.isEmpty()) "未填写" else com.reasonix.agents.data.CiMonitorStore.maskToken(ciSettings.githubToken))
                CiTokenInput(
                    currentToken = ciSettings.githubToken,
                    onTokenChange = { t -> onCiSettingsChange(ciSettings.copy(githubToken = t)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Owner / Repo
                CiTextField(
                    label = "Owner",
                    value = ciSettings.owner,
                    onValueChange = { v -> onCiSettingsChange(ciSettings.copy(owner = v)) }
                )
                CiTextField(
                    label = "Repo",
                    value = ciSettings.repo,
                    onValueChange = { v -> onCiSettingsChange(ciSettings.copy(repo = v)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 刷新间隔
                InfoRow("刷新间隔", when (ciSettings.intervalMs) {
                    30_000L -> "30 秒"
                    60_000L -> "1 分钟"
                    300_000L -> "5 分钟"
                    else -> "${ciSettings.intervalMs / 1000} 秒"
                })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30_000L to "30s", 60_000L to "1m", 300_000L to "5m").forEach { (ms, label) ->
                        val selected = ciSettings.intervalMs == ms
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Accent.copy(alpha = 0.18f) else Panel)
                                .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                                .clickable { onCiSettingsChange(ciSettings.copy(intervalMs = ms)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (selected) Accent else Fg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "悬浮球颜色：运行中=橙 成功=绿 失败=红 排队=蓝",
                    fontSize = 11.sp,
                    color = Muted2
                )
                Text(
                    text = "提示：Token 仅存本机，不会上传",
                    fontSize = 11.sp,
                    color = Muted2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 关于（批 A-6 多入口 + 批 A-7 检测更新）──
            SectionTitle("关于")
            InfoRow("版本", "Reasonix Agents v$versionName")
            // 检查更新按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Bg2)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable { checkUpdate() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (checkingUpdate) "检查中…" else "检查更新",
                    fontSize = 13.sp,
                    color = Accent,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 关于页入口
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Bg2)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable { onOpenAbout() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "关于本应用（版本 / 项目 / 并列项目）",
                    fontSize = 13.sp,
                    color = Accent,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Muted2, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 仓库链接（可点击跳转浏览器；批 C-2：上游只保留协议上游一个连接）
            ClickableInfoRow("本项目", "github.com/xiwangone/reasonix-agents") {
                uriHandler.openUri("https://github.com/xiwangone/reasonix-agents")
            }
            ClickableInfoRow("并列项目", "RikkaHub Agents · github.com/xiwangone/rikkahub-agents") {
                uriHandler.openUri("https://github.com/xiwangone/rikkahub-agents")
            }
            ClickableInfoRow("上游项目", "协议上游 · github.com/esengine/DeepSeek-Reasonix") {
                uriHandler.openUri("https://github.com/esengine/DeepSeek-Reasonix")
            }
        }
    }

    // ── 添加模型弹窗（批 B-9）──
    if (showAddModel) {
        AddModelDialog(
            onAdd = { model ->
                onAddCustomModel(model)
                showAddModel = false
            },
            onDismiss = { showAddModel = false }
        )
    }

    // ── 更新结果弹窗（批 A-7）──
    updateResult?.let { result ->
        val hasUpdate = result.startsWith("发现新版本")
        AlertDialog(
            onDismissRequest = { updateResult = null },
            title = { Text(if (hasUpdate) "🎉 发现新版本" else "版本检查", color = Fg) },
            text = { Text(result, color = Fg2, fontSize = 13.sp) },
            confirmButton = {
                if (hasUpdate) {
                    TextButton(onClick = {
                        updateResult = null
                        uriHandler.openUri("https://github.com/xiwangone/reasonix-agents/releases/latest")
                    }) { Text("前往下载", color = Accent) }
                } else {
                    TextButton(onClick = { updateResult = null }) { Text("好的", color = Accent) }
                }
            },
            dismissButton = {
                TextButton(onClick = { updateResult = null }) { Text("关闭", color = Muted) }
            },
            containerColor = Panel
        )
    }
}

// ═══════════════════════════════════════════════
// 内部组件
// ═══════════════════════════════════════════════

/** 模型下拉（批 B-10）：服务端模型 + 自定义模型合并，选中即回调。 */
@Composable
private fun ModelDropdown(
    models: List<ModelInfo>,
    customModels: List<CustomModel>,
    currentModel: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val customNames = customModels.map { it.name }.toSet()
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Bg2)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentModel.ifEmpty { "选择模型" },
                    fontSize = 13.sp,
                    color = if (currentModel.isEmpty()) Muted2 else Fg,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Panel,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            // 自定义模型（本地）
            customModels.forEach { cm ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(cm.name, fontSize = 13.sp, color = Fg)
                            Text(
                                "自定义 · ${compatLabel(cm.compat)}",
                                fontSize = 10.sp,
                                color = Muted2
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(cm.name)
                    }
                )
            }
            if (customModels.isNotEmpty() && models.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider(color = Border, thickness = 1.dp)
            }
            // 服务端模型
            models.forEach { m ->
                val label = m.model.ifEmpty { m.ref }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(label, fontSize = 13.sp, color = Fg)
                            if (m.kind.isNotBlank()) {
                                Text(m.kind, fontSize = 10.sp, color = Muted2)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(m.ref)
                    }
                )
            }
            if (customModels.isEmpty() && models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("暂无模型（可点击 + 添加自定义模型）", fontSize = 12.sp, color = Muted2) },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

/**
 * 添加模型弹窗（批 B-9 + C-4）：模型名称 + key（provider/model 兼容格式）+ 其他必要字段。
 * key 例如 "openai/deepseek-v4-flash"、"opencode-zen/deepseek-v4-flash-free"，保存后按 key 独立分组。
 */
@Composable
private fun AddModelDialog(
    onAdd: (CustomModel) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("自定义") }
    var baseUrl by remember { mutableStateOf("") }
    var compat by remember { mutableStateOf("OpenAI") }
    var keyError by remember { mutableStateOf<String?>(null) }

    val providerOptions = listOf("内置", "自定义")
    val compatOptions = listOf("OpenAI", "DeepSeek-Reasonix", "其他")

    fun trySave() {
        val nameTrim = name.trim()
        val keyTrim = key.trim()
        when {
            nameTrim.isBlank() -> keyError = "请填写模型名称"
            keyTrim.isBlank() -> keyError = "请填写 Key（provider/model 格式）"
            !keyTrim.contains("/") -> keyError = "Key 需为 provider/model 格式，例如 openai/deepseek-v4-flash"
            keyTrim.startsWith("/") || keyTrim.endsWith("/") -> keyError = "Key 格式不正确：provider 和 model 均不能为空"
            else -> onAdd(
                CustomModel(
                    id = nameTrim,
                    name = nameTrim,
                    key = keyTrim,
                    provider = if (provider == "内置") "builtin" else "custom",
                    baseUrl = baseUrl.trim(),
                    compat = when (compat) {
                        "OpenAI" -> "openai"
                        "DeepSeek-Reasonix" -> "deepseek"
                        else -> "other"
                    }
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型", color = Fg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 模型名称
                LabeledField("模型名称（必填）") {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it; keyError = null },
                        textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
                // Key（provider/model 兼容格式）
                LabeledField("Key（provider/model，必填）") {
                    BasicTextField(
                        value = key,
                        onValueChange = { key = it; keyError = null },
                        textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Panel)
                            .border(1.dp, if (keyError != null) Danger else Border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "如 openai/deepseek-v4-flash 或 opencode-zen/deepseek-v4-flash-free，保存后按 key 独立分组",
                        fontSize = 10.sp,
                        color = Muted2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Provider
                LabeledField("Provider") {
                    SimpleDropdown(
                        label = provider,
                        options = providerOptions,
                        onSelect = { provider = it }
                    )
                }
                // base_url（自定义时）
                if (provider == "自定义") {
                    LabeledField("Base URL（可选）") {
                        BasicTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Panel)
                                .border(1.dp, Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
                // 兼容方式
                LabeledField("兼容方式") {
                    SimpleDropdown(
                        label = compat,
                        options = compatOptions,
                        onSelect = { compat = it }
                    )
                }
                if (keyError != null) {
                    Text(
                        text = keyError.orEmpty(),
                        fontSize = 11.sp,
                        color = Danger,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = "内置：由服务端模型管理；自定义：本地保存，用于第三方兼容服务",
                        fontSize = 10.sp,
                        color = Muted2
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { trySave() }) { Text("保存", color = Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
        },
        containerColor = Panel
    )
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

/** 通用下拉（弹窗内小选项）。 */
@Composable
private fun SimpleDropdown(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Panel)
                .border(1.dp, Border, RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontSize = 13.sp, color = Fg, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Panel
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 13.sp, color = Fg) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    }
                )
            }
        }
    }
}

/** 兼容方式中文标签。 */
private fun compatLabel(compat: String): String = when (compat) {
    "openai" -> "OpenAI"
    "deepseek" -> "DeepSeek-Reasonix"
    else -> "其他"
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Accent,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Accent else Panel)
            .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) Color.White else Muted
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Fg,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Fg2,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 可点击的 InfoRow（仓库链接跳转浏览器，批 A-6）。 */
@Composable
private fun ClickableInfoRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(90.dp)
        )
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Accent,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Muted2, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun CiTokenInput(currentToken: String, onTokenChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (editing) "新 Token" else "Token",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.width(64.dp)
            )
            if (editing) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                    singleLine = true
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Accent)
                        .clickable {
                            onTokenChange(draft.trim())
                            editing = false
                            draft = ""
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("保存", fontSize = 12.sp, color = Color.White)
                }
            } else {
                Text(
                    text = if (currentToken.isEmpty()) "点击填写" else com.reasonix.agents.data.CiMonitorStore.maskToken(currentToken),
                    fontSize = 13.sp,
                    color = if (currentToken.isEmpty()) Muted2 else Fg,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(6.dp))
                        .clickable { editing = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(if (currentToken.isEmpty()) "填写" else "更换", fontSize = 12.sp, color = Accent)
                }
            }
        }
        if (editing) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "输入 GitHub Personal Access Token（repo 权限）",
                fontSize = 11.sp,
                color = Muted2
            )
        }
    }
}

@Composable
private fun CiTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.width(64.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .background(Panel)
                .border(1.dp, Border, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
            singleLine = true
        )
    }
}

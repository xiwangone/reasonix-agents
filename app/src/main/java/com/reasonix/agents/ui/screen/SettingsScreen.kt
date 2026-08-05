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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.reasonix.agents.data.PromptStore
import com.reasonix.agents.data.api.GitHubReleaseApi
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2
private val Danger: Color @Composable get() = LocalPalette.current.danger

/**
 * 设置页（第四批：设置组件化）。
 *
 * - 系统提示词（后端只读）已移入二级界面「系统提示词」（第六批：新会话静默化，不再弹出展示页）；
 * - 保留「提示词」（用户自定义，含添加/保存/切换/删除，上限 10 条）区块；
 * - 其余已有设置项按功能分组归入二级界面：主题 / 模型 / 显示 / 网络 / 服务器信息 / CI 监控 / 关于。
 */
@Composable
fun SettingsScreen(
    customPrompts: List<PromptStore.CustomPrompt> = emptyList(),
    currentPromptId: String = "",
    onAddPrompt: (String, Boolean) -> Unit = { _, _ -> },
    onRemovePrompt: (String) -> Unit = {},
    onSetCurrentPrompt: (String) -> Unit = {},
    onOpenSystemPrompt: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    onOpenModel: () -> Unit = {},
    onOpenDisplay: () -> Unit = {},
    onOpenNetwork: () -> Unit = {},
    onOpenServerInfo: () -> Unit = {},
    onOpenCi: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenCli: () -> Unit = {},
    onOpenDeploy: () -> Unit = {},
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

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }

    // ── 提示词区块状态（第四批）──
    var showPromptHint by remember { mutableStateOf(false) }
    var limitHint by remember { mutableStateOf(false) }
    var draftActive by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    var draftError by remember { mutableStateOf(false) }

    // 满 10 条后的上限提示 3 秒后自动消失
    LaunchedEffect(limitHint) {
        if (limitHint) {
            delay(3_000)
            limitHint = false
        }
    }

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

    fun onAddClick() {
        if (customPrompts.size >= PromptStore.MAX_PROMPTS) {
            // 满 10 条：提示「已到上限无法添加，请删除后再试」
            limitHint = true
            showPromptHint = false
        } else {
            limitHint = false
            showPromptHint = true
            draftActive = true
            draftText = ""
            draftError = false
        }
    }

    fun saveDraft(select: Boolean) {
        if (draftText.isBlank()) {
            draftError = true
            return
        }
        onAddPrompt(draftText, select)
        draftActive = false
        draftText = ""
        draftError = false
        showPromptHint = false
    }

    fun cancelDraft() {
        draftActive = false
        draftText = ""
        draftError = false
        showPromptHint = false
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

            // ── 提示词（第四批：用户自定义提示词，注入会话上下文）──
            SectionTitle("提示词")
            Text(
                text = "自定义提示词会附加在系统提示词之后，随每条消息发送。最多 ${PromptStore.MAX_PROMPTS} 条。",
                fontSize = 11.sp,
                color = Muted2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onAddClick() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg2)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加提示词", tint = Accent, modifier = Modifier.size(18.dp))
                }
                Text("添加你的提示词", fontSize = 13.sp, color = Accent)
            }
            if (showPromptHint) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "添加你的提示词：可写入常驻指令（如代码风格、回答偏好、输出格式等），保存后可切换选用，随消息自动生效。",
                    fontSize = 11.sp,
                    color = Muted2
                )
            }
            if (limitHint) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "已到上限无法添加，请删除后再试",
                    fontSize = 11.sp,
                    color = Danger
                )
            }

            // 草稿槽（点加号出现的可编辑提示词槽）
            if (draftActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("新提示词", fontSize = 11.sp, color = Muted)
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = draftText,
                            onValueChange = {
                                draftText = it
                                draftError = false
                            },
                            textStyle = TextStyle(color = Fg, fontSize = 13.sp, lineHeight = 18.sp),
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Bg2)
                                .border(1.dp, if (draftError) Danger else Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                        if (draftError) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("提示词内容不能为空", fontSize = 11.sp, color = Danger)
                        }
                        // 保存 / 切换 / 取消
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { saveDraft(select = false) }) { Text("保存", fontSize = 13.sp, color = Accent) }
                            TextButton(onClick = { saveDraft(select = true) }) { Text("切换", fontSize = 13.sp, color = Accent) }
                            TextButton(onClick = { cancelDraft() }) { Text("取消", fontSize = 13.sp, color = Muted) }
                        }
                    }
                }
            }

            // 已保存的提示词槽列表
            if (customPrompts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                customPrompts.forEachIndexed { index, prompt ->
                    val selected = prompt.id == currentPromptId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Accent.copy(alpha = 0.06f) else Panel)
                            .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "提示词 ${index + 1}",
                                    fontSize = 11.sp,
                                    color = Muted,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Text("使用中", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Medium)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prompt.content,
                                fontSize = 12.sp,
                                color = Fg2,
                                lineHeight = 17.sp
                            )
                            // 切换 / 删除
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onSetCurrentPrompt(prompt.id) }) {
                                    Text(
                                        text = if (selected) "取消使用" else "切换",
                                        fontSize = 13.sp,
                                        color = if (selected) Muted else Accent
                                    )
                                }
                                TextButton(onClick = { onRemovePrompt(prompt.id) }) {
                                    Text("删除", fontSize = 13.sp, color = Danger)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 常规设置（第四批：设置组件化，点击进入二级界面）──
            SectionTitle("常规")
            // 第六批：系统提示词移入二级页面（只读展示完整内容）
            SettingEntry(
                Icons.Default.Info,
                "系统提示词",
                "服务端系统提示词（只读，完整查看）",
                onClick = onOpenSystemPrompt
            )
            Spacer(modifier = Modifier.height(6.dp))
            SettingEntry(Icons.Default.Palette, "主题", "配色风格 / 明暗模式 / 语言", onClick = onOpenTheme)
            Spacer(modifier = Modifier.height(6.dp))
            SettingEntry(Icons.Default.List, "模型", "模型切换 / 添加 / 删除自定义模型", onClick = onOpenModel)
            Spacer(modifier = Modifier.height(6.dp))
            SettingEntry(Icons.Default.Visibility, "显示", "推理过程 / Token 费用开关", onClick = onOpenDisplay)
            Spacer(modifier = Modifier.height(6.dp))
            SettingEntry(Icons.Default.Wifi, "网络", "连接超时 / SSE 断线重连", onClick = onOpenNetwork)
            Spacer(modifier = Modifier.height(6.dp))
            SettingEntry(Icons.Default.Build, "CLI 集成", "启用 reasonix 调用部署 CLI（aide-wrap.sh / oc-wrap.sh）", onClick = onOpenCli)

            Spacer(modifier = Modifier.height(16.dp))

            // ── 服务器 ──
            SectionTitle("服务器")
            SettingEntry(Icons.Default.Dns, "服务器信息", "地址 / 标签 / 计划模式 / 工具审批 / 余额", onClick = onOpenServerInfo)
            Spacer(modifier = Modifier.height(6.dp))
            SettingEntry(Icons.Default.OpenInNew, "部署自己的服务", "查看部署说明（GitHub README）", onClick = onOpenDeploy)

            Spacer(modifier = Modifier.height(16.dp))

            // ── 数据（第五批 E-1）──
            SectionTitle("数据")
            SettingEntry(Icons.Default.Lock, "备份与恢复", "导出 / 导入单文件备份（配置加密 + 会话历史）", onClick = onOpenBackup)

            Spacer(modifier = Modifier.height(16.dp))

            // ── 监控 ──
            SectionTitle("监控")
            SettingEntry(Icons.Default.CheckCircle, "CI 监控", "GitHub Actions 悬浮球 / Token / 刷新间隔", onClick = onOpenCi)

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

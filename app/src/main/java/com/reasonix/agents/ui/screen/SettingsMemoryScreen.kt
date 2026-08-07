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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.MemoryStore
import com.reasonix.agents.ui.theme.LocalPalette

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
 * 记忆二级页面（2026-08-06 新增：仿 RikkaHub Agents 记忆功能第一版）。
 *
 * 启用开关 + 记忆条目增删。启用后发送消息时自动注入【记忆】段落。
 * 数据自包含（直接读写 MemoryStore），不依赖 ViewModel。
 */
@Composable
fun SettingsMemoryScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current

    var memories by remember { mutableStateOf(MemoryStore.load(context)) }
    var enabled by remember { mutableStateOf(MemoryStore.isEnabled(context)) }
    var draftActive by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    // 2026-08-06 优化：记忆搜索
    var searchQuery by remember { mutableStateOf("") }
    val filteredMemories = memories.filter {
        searchQuery.isBlank() || it.content.contains(searchQuery, ignoreCase = true)
    }
    var limitHint by remember { mutableStateOf(false) }

    fun refresh() {
        memories = MemoryStore.load(context)
        enabled = MemoryStore.isEnabled(context)
    }

    fun onAddClick() {
        if (memories.size >= MemoryStore.MAX_MEMORIES) {
            limitHint = true
            draftActive = false
        } else {
            limitHint = false
            draftActive = true
            draftText = ""
        }
    }

    fun saveDraft() {
        val content = draftText.trim()
        if (content.isBlank()) return
        MemoryStore.add(context, content)
        refresh()
        draftActive = false
        draftText = ""
    }

    fun removeItem(id: String) {
        MemoryStore.remove(context, id)
        refresh()
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // 返回行 + 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Fg,
                    )
                }
                Text(
                    text = "记忆",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 启用开关
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用记忆注入", fontSize = 15.sp, color = Fg, fontWeight = FontWeight.Medium)
                    Text(
                        "开启后，发送消息时自动附带【记忆】段落（长期事实/约定）",
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        MemoryStore.setEnabled(context, it)
                        enabled = it
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 说明 + 添加按钮
            Text(
                text = "记忆是长期事实与约定（如「项目结构」「命名规范」「常用命令」），随消息自动携带，帮助模型记住跨会话上下文。",
                fontSize = 12.sp,
                color = Muted2,
                lineHeight = 17.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (limitHint) {
                Text(
                    text = "已到上限无法添加，请删除后再试",
                    fontSize = 12.sp,
                    color = Danger,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            // 草稿输入
            if (draftActive) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                ) {
                    Column {
                        BasicTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (draftText.isEmpty()) {
                                    Text("输入记忆内容…", fontSize = 14.sp, color = Muted)
                                }
                                inner()
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "取消",
                                fontSize = 13.sp,
                                color = Muted,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { draftActive = false; draftText = "" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "保存",
                                fontSize = 13.sp,
                                color = Accent,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { saveDraft() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2026-08-06 优化：记忆搜索框
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg2)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                    cursorBrush = SolidColor(Accent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = Muted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (searchQuery.isEmpty()) {
                                Text("搜索记忆…", fontSize = 14.sp, color = Muted)
                            }
                            inner()
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 添加按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Bg2)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable { onAddClick() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加记忆",
                    tint = Accent,
                    modifier = Modifier.size(18.dp),
                )
                Text("添加记忆", fontSize = 13.sp, color = Accent, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 记忆列表（2026-08-06：支持搜索过滤）
            if (memories.isEmpty()) {
                Text(
                    text = "暂无记忆，点击上方「添加记忆」创建",
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else if (filteredMemories.isEmpty()) {
                Text(
                    text = "无匹配记忆",
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                filteredMemories.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Panel)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Text(
                            text = item.content,
                            fontSize = 14.sp,
                            color = Fg,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        IconButton(onClick = { removeItem(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除记忆",
                                tint = Danger,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

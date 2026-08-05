package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.PromptStore
import com.reasonix.agents.ui.theme.LocalPalette
import kotlinx.coroutines.delay

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
 * 提示词二级页面（批七：提示词从设置页一级平铺移入二级界面）。
 *
 * 保留全部功能：查看 / 添加（上限 [PromptStore.MAX_PROMPTS] 条）/ 保存 / 切换 / 删除。
 * 设置页一级只保留「提示词」入口项。
 */
@Composable
fun SettingsPromptScreen(
    customPrompts: List<PromptStore.CustomPrompt> = emptyList(),
    currentPromptId: String = "",
    onAddPrompt: (String, Boolean) -> Unit = { _, _ -> },
    onRemovePrompt: (String) -> Unit = {},
    onSetCurrentPrompt: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    // ── 提示词区块状态（第四批逻辑原样保留，仅迁移到二级页）──
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
                    text = "提示词",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "自定义提示词会附加在系统提示词之后，随每条消息发送。最多 ${PromptStore.MAX_PROMPTS} 条。",
                fontSize = 11.sp,
                color = Muted2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = { onAddClick() },
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp)),
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
                    color = Muted2,
                )
            }
            if (limitHint) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "已到上限无法添加，请删除后再试",
                    fontSize = 11.sp,
                    color = Danger,
                )
            }

            // 草稿槽（点加号出现的可编辑提示词槽）
            if (draftActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Panel)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(12.dp),
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
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Bg2)
                                    .border(1.dp, if (draftError) Danger else Border, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                        if (draftError) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("提示词内容不能为空", fontSize = 11.sp, color = Danger)
                        }
                        // 保存 / 切换 / 取消
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Accent.copy(alpha = 0.06f) else Panel)
                                .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "提示词 ${index + 1}",
                                    fontSize = 11.sp,
                                    color = Muted,
                                    modifier = Modifier.weight(1f),
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
                                lineHeight = 17.sp,
                            )
                            // 切换 / 删除
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { onSetCurrentPrompt(prompt.id) }) {
                                    Text(
                                        text = if (selected) "取消使用" else "切换",
                                        fontSize = 13.sp,
                                        color = if (selected) Muted else Accent,
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
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无提示词，点击上方「添加你的提示词」创建",
                    fontSize = 12.sp,
                    color = Muted2,
                )
            }
        }
    }
}

package com.reasonix.agents.ui.components

// ═══════════════════════════════════════════════════════════════════
// 2026-08-07：注入上下文折叠卡
// 展示「本次对话注入给 AI 的上下文」——系统提示词（后端只读）/
// 用户提示词（可编辑保存）/ 记忆（可编辑保存）。
// 新对话默认折叠，可手动展开查看/编辑；避免遮挡聊天主内容。
// ═══════════════════════════════════════════════════════════════════

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.R
import com.reasonix.agents.ui.theme.LocalPalette

/**
 * 注入上下文折叠卡。
 *
 * @param systemPrompt 后端系统提示词（只读）
 * @param userPrompt 当前选中的用户提示词（可编辑）
 * @param memoryText 当前记忆注入文本（可编辑）
 * @param onSaveUserPrompt 保存用户提示词回调（内容）
 * @param onSaveMemory 保存记忆回调（内容）
 */
@Composable
fun InjectionContextCard(
    systemPrompt: String?,
    userPrompt: String,
    memoryText: String?,
    onSaveUserPrompt: (String) -> Unit,
    onSaveMemory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    // 默认折叠（新对话注入时默认折叠，可手动展开）
    var expanded by rememberSaveable { mutableStateOf(false) }
    // 编辑对话框状态
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    // 2026-08-08：各 section 独立折叠（默认折叠），点击标题行展开/收起
    var sysExpanded by rememberSaveable { mutableStateOf(false) }
    var userExpanded by rememberSaveable { mutableStateOf(false) }
    var memExpanded by rememberSaveable { mutableStateOf(false) }

    val sectionCount = listOfNotNull(systemPrompt, userPrompt.takeIf { it.isNotBlank() }, memoryText).size

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(p.bg2)
                .border(1.dp, p.border, RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded },
    ) {
        // 头部：标题 + 项数徽标 + 折叠箭头
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = p.accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.injection_context_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.fg,
            )
            Spacer(Modifier.width(8.dp))
            // 项数徽标
            Text(
                stringResource(R.string.injection_context_count, sectionCount),
                fontSize = 10.sp,
                color = p.muted2,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(p.bg.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (expanded) stringResource(R.string.injection_context_collapse) else stringResource(R.string.injection_context_expand),
                fontSize = 11.sp,
                color = p.muted,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = p.muted,
                modifier = Modifier.size(18.dp),
            )
        }

        if (expanded) {
            HorizontalDivider(color = p.border.copy(alpha = 0.5f))
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                // ── 系统提示词（只读，独立折叠）──
                systemPrompt?.let { sp ->
                    SectionHeader(
                        title = stringResource(R.string.section_system_prompt),
                        badge = stringResource(R.string.badge_backend_readonly),
                        badgeColor = p.muted2,
                        icon = { Icon(Icons.Default.Lock, null, Modifier.size(13.dp), tint = p.muted2) },
                        onEdit = null,
                        expanded = sysExpanded,
                        onToggle = { sysExpanded = !sysExpanded },
                    )
                    if (sysExpanded) SectionBody(sp)
                }

                // ── 用户提示词（可编辑，独立折叠）──
                if (userPrompt.isNotBlank()) {
                    SectionHeader(
                        title = stringResource(R.string.section_user_prompt),
                        badge = stringResource(R.string.badge_editable),
                        badgeColor = p.accent,
                        icon = null,
                        onEdit = { editTarget = EditTarget.UserPrompt(userPrompt) },
                        expanded = userExpanded,
                        onToggle = { userExpanded = !userExpanded },
                    )
                    if (userExpanded) SectionBody(userPrompt)
                }

                // ── 记忆（可编辑，独立折叠）──
                if (!memoryText.isNullOrBlank()) {
                    SectionHeader(
                        title = stringResource(R.string.section_memory),
                        badge = stringResource(R.string.badge_editable),
                        badgeColor = p.accent,
                        icon = null,
                        onEdit = { editTarget = EditTarget.Memory(memoryText) },
                        expanded = memExpanded,
                        onToggle = { memExpanded = !memExpanded },
                    )
                    if (memExpanded) SectionBody(memoryText)
                }
            }
        }
    }

    // 编辑对话框
    editTarget?.let { target ->
        EditInjectionDialog(
            target = target,
            onDismiss = { editTarget = null },
            onSave = { content ->
                when (target) {
                    is EditTarget.UserPrompt -> onSaveUserPrompt(content)
                    is EditTarget.Memory -> onSaveMemory(content)
                }
                editTarget = null
            },
        )
    }
}

/** 编辑目标：用户提示词 / 记忆。 */
private sealed class EditTarget {
    data class UserPrompt(val content: String) : EditTarget()
    data class Memory(val content: String) : EditTarget()
}

/** 区块标题行（含徽标与编辑按钮）。点击整行可折叠/展开该区块正文。 */
@Composable
private fun SectionHeader(
    title: String,
    badge: String,
    badgeColor: Color,
    icon: (@Composable () -> Unit)?,
    onEdit: (() -> Unit)?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val p = LocalPalette.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToggle() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = p.fg2)
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            icon?.invoke()
            icon?.let { Spacer(Modifier.width(3.dp)) }
            Text(badge, fontSize = 10.sp, color = badgeColor)
        }
        Spacer(Modifier.weight(1f))
        if (onEdit != null) {
            Text(
                stringResource(R.string.action_edit),
                fontSize = 11.sp,
                color = p.accent,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onEdit() }.padding(4.dp),
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = p.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** 区块内容（限制高度可滚动）。 */
@Composable
private fun SectionBody(text: String) {
    val p = LocalPalette.current
    Text(
        text = text,
        fontSize = 12.sp,
        color = p.fg2,
        fontFamily = FontFamily.Monospace,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(p.bg.copy(alpha = 0.5f))
                .padding(8.dp)
                .heightIn(max = 90.dp)
                .verticalScroll(rememberScrollState()),
    )
}

/** 编辑对话框（预填内容，保存回调）。 */
@Composable
private fun EditInjectionDialog(
    target: EditTarget,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val p = LocalPalette.current
    val initial =
        when (target) {
            is EditTarget.UserPrompt -> target.content
            is EditTarget.Memory -> target.content
        }
    var text by remember(target) { mutableStateOf(initial) }
    val title =
        when (target) {
            is EditTarget.UserPrompt -> stringResource(R.string.dialog_edit_user_prompt)
            is EditTarget.Memory -> stringResource(R.string.dialog_edit_memory)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.bg,
        titleContentColor = p.fg,
        textContentColor = p.fg2,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.fg) },
        text = {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.bg2)
                        .border(1.dp, p.border, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                textStyle = TextStyle(fontSize = 13.sp, color = p.fg),
                cursorBrush = SolidColor(p.accent),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.action_save), color = p.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = p.muted) }
        },
    )
}

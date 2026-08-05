package com.reasonix.agents.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.model.TodoItem
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val AccentS: Color @Composable get() = LocalPalette.current.accentS
private val Success: Color @Composable get() = LocalPalette.current.success
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * Todo 面板 — 展示 serve GET /todos 返回的任务进度。
 *
 * - 头部：标题 + 进度（x/y）+ 折叠/刷新
 * - 进度条：LinearProgressIndicator
 * - 列表：completed 划线 + 绿色勾；in_progress 高亮；pending 空心圈
 * - 空态：提示"暂无任务"
 */
@Composable
fun TodoPanel(
    todos: List<TodoItem>,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    val doneCount = todos.count { it.isCompleted }
    val progress = if (todos.isEmpty()) 0f else doneCount.toFloat() / todos.size

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Panel)
                .padding(vertical = 2.dp),
    ) {
        // ── 头部 ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "任务进度",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Fg,
                modifier = Modifier.weight(1f),
            )
            if (todos.isNotEmpty()) {
                Text(
                    text = "$doneCount/${todos.size}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (doneCount == todos.size) Success else Muted,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "收起" else "展开",
                tint = Muted,
                modifier =
                    Modifier
                        .size(18.dp)
                        .clickable { expanded = !expanded },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "刷新",
                tint = Muted,
                modifier =
                    Modifier
                        .size(16.dp)
                        .clickable { onRefresh() },
            )
        }

        if (expanded) {
            // ── 进度条 ──
            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(4.dp),
                color = Accent,
                trackColor = AccentS,
            )

            // ── 列表 / 空态 ──
            if (todos.isEmpty()) {
                Text(
                    text = "暂无任务",
                    fontSize = 12.sp,
                    color = Muted2,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    todos.forEach { todo -> TodoRow(todo) }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: TodoItem) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 状态图标
        when {
            todo.isCompleted -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已完成",
                    tint = Success,
                    modifier = Modifier.size(16.dp),
                )
            }

            todo.isInProgress -> {
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Accent),
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "待办",
                    tint = Muted2,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = todo.content.ifBlank { "(无标题)" },
            fontSize = 13.sp,
            color =
                when {
                    todo.isCompleted -> Muted2
                    todo.isInProgress -> Fg
                    else -> Fg2
                },
            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
            fontWeight = if (todo.isInProgress) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

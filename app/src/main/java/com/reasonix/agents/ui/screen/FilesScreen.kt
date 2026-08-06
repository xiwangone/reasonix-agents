package com.reasonix.agents.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import com.reasonix.agents.data.model.ChatItem
import com.reasonix.agents.ui.theme.LocalPalette

/**
 * 文件状态：读取 / 修改 / 新增 / 出错（驱动清单着色）
 */
enum class FileStatus { READ, MODIFIED, ADDED, ERROR }

/**
 * 会话文件条目 — 由工具调用事件聚合而来。
 *
 * @param path    文件路径（相对工作目录，如 src/main/AndroidManifest.xml）
 * @param status  最近一次操作的状态
 * @param content 最近一次 tool_result 输出缓存（用于内容预览）
 * @param tool    产生该条目的工具名（read_file / edit_file / write_file …）
 */
data class SessionFile(
    val path: String,
    val status: FileStatus,
    val content: String?,
    val tool: String,
)

/**
 * 文件清单聚合器 — 从会话消息流中提取工具调用涉及的路径。
 *
 * 服务端暂无 /file API（已核实），本页以「工具事件聚合」作为轻量替代：
 * read_file/glob 等 → READ；write_file/create → ADDED；edit_file/apply_patch → MODIFIED；
 * 工具报错 → ERROR。同一路径多次出现时取「最重」状态，内容缓存取最近一次 output。
 */
object SessionFileAggregator {
    private val READ_TOOLS = setOf("read_file", "read", "glob", "ls", "list", "view", "show")
    private val WRITE_TOOLS = setOf("write_file", "write", "create", "touch")
    private val MODIFY_TOOLS =
        setOf(
            "edit_file",
            "edit",
            "apply_patch",
            "patch",
            "str_replace_editor",
            "multi_edit",
            "write_and_exec",
        )

    fun aggregate(messages: List<ChatItem>): List<SessionFile> {
        val map = LinkedHashMap<String, SessionFile>()
        messages.forEach { item ->
            if (item !is ChatItem.ToolCard) return@forEach
            val path = extractPath(item.name, item.args) ?: return@forEach
            val status =
                when {
                    item.err != null -> FileStatus.ERROR
                    item.name in READ_TOOLS -> FileStatus.READ
                    item.name in WRITE_TOOLS -> FileStatus.ADDED
                    item.name in MODIFY_TOOLS -> FileStatus.MODIFIED
                    else -> FileStatus.MODIFIED
                }
            val prev = map[path]
            map[path] =
                SessionFile(
                    path = path,
                    status = mergeStatus(prev?.status, status),
                    content = item.output?.takeIf { it.isNotBlank() } ?: prev?.content,
                    tool = item.name,
                )
        }
        return map.values.toList().sortedBy { it.path }
    }

    /** 合并同一文件多次操作的状态：ERROR 优先，READ 最轻，ADDED/MODIFIED 取较新 */
    private fun mergeStatus(
        prev: FileStatus?,
        cur: FileStatus,
    ): FileStatus {
        if (prev == null) return cur
        if (cur == FileStatus.ERROR || prev == FileStatus.ERROR) return FileStatus.ERROR
        if (prev == FileStatus.READ) return cur
        return prev
    }

    /** 从工具参数中提取文件路径：JSON {file/path/subject} 或裸路径字符串 */
    private fun extractPath(
        name: String,
        args: String?,
    ): String? {
        if (args.isNullOrBlank()) return null
        return try {
            if (args.trimStart().startsWith("{")) {
                val obj = JsonParser.parseString(args).asJsonObject
                obj.get("file")?.asString
                    ?: obj.get("path")?.asString
                    ?: obj.get("subject")?.asString
                    ?: obj.get("file_path")?.asString
            } else {
                args.trim().removeSurrounding("\"").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            null
        }
    }
}

/** 树节点：目录或文件（轻量版仅两级展开交互） */
private class FileNode(
    val name: String,
    val isDir: Boolean,
    var file: SessionFile? = null,
    val children: MutableList<FileNode> = mutableListOf(),
)

/** 路径列表 → 树 */
private fun buildTree(files: List<SessionFile>): List<FileNode> {
    val root = mutableListOf<FileNode>()
    files.forEach { f ->
        val parts =
            f.path
                .trim('/')
                .split('/')
                .filter { it.isNotBlank() }
        var level = root
        parts.forEachIndexed { index, part ->
            val isLast = index == parts.lastIndex
            val existing = level.firstOrNull { it.name == part }
            if (existing != null) {
                if (isLast) existing.file = f
                level = existing.children
            } else {
                val node = FileNode(name = part, isDir = !isLast, file = if (isLast) f else null)
                level.add(node)
                level = node.children
            }
        }
    }
    return root
}

/**
 * 文件页（批 4 轻量聚合版）。
 *
 * 数据来自会话消息中的工具调用事件（[ChatItem.ToolCard]），
 * 服务端支持 /file 系列 API 后（见 docs/upstream-file-api-request.md）可平滑升级为直连模式。
 */
@Composable
fun FilesScreen(
    messages: List<ChatItem>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val files = remember(messages) { SessionFileAggregator.aggregate(messages) }
    val tree = remember(files) { buildTree(files) }
    var preview by remember { mutableStateOf<SessionFile?>(null) }
    val context = LocalContext.current
    // 导出：SAF 创建文件 → 写入文件清单 JSON（含路径/状态/工具/内容预览）
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            try {
                val sb = StringBuilder()
                sb.append("{\n  \"files\": [\n")
                files.forEachIndexed { i, f ->
                    sb.append("    {")
                    sb.append("\"path\": \"${escapeJson(f.path)}\", ")
                    sb.append("\"status\": \"${f.status.name}\", ")
                    sb.append("\"tool\": \"${escapeJson(f.tool)}\"")
                    if (!f.content.isNullOrBlank()) {
                        sb.append(", \"contentPreview\": \"${escapeJson(f.content.take(500))}\"")
                    }
                    sb.append("}")
                    if (i < files.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ]\n}\n")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "已导出 ${files.size} 个文件清单", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(palette.bg)
                .safeDrawingPadding(),
    ) {
        // ── 标题栏 ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "会话文件",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.fg,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${files.size} 个文件",
                fontSize = 12.sp,
                color = palette.muted,
                fontFamily = FontFamily.Monospace,
            )
            // 2026-08-06：导出会话文件清单（SAF CreateDocument → JSON）
            if (files.isNotEmpty()) {
                IconButton(onClick = { exportLauncher.launch("会话文件清单.json") }) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "导出文件清单",
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = palette.border, thickness = 1.dp)

        if (files.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 8.dp,
                    ),
            ) {
                items(tree, key = { it.name + it.isDir }) { node ->
                    FileNodeRow(node = node, indent = 0, onLeafClick = { preview = it })
                }
            }
        }
    }

    preview?.let { file ->
        FilePreviewDialog(file = file, onDismiss = { preview = null })
    }
}

/** 空态：说明 + 上游依赖提示 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = palette.muted2,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无文件记录",
            fontSize = 16.sp,
            color = palette.fg2,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                "本页从会话中的工具调用事件聚合文件清单（读取/修改/新增）。\n" +
                    "服务端暂未提供 /file API，完整文件浏览能力已向上游提交 feature request（见 docs/upstream-file-api-request.md）。",
            fontSize = 12.sp,
            color = palette.muted2,
            textAlign = TextAlign.Center,
        )
    }
}

/** 树节点行：目录可展开/收起，文件可点击预览 */
@Composable
private fun FileNodeRow(
    node: FileNode,
    indent: Int,
    onLeafClick: (SessionFile) -> Unit,
) {
    val palette = LocalPalette.current
    var expanded by remember(node) { mutableStateOf(true) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = (indent * 16).dp, end = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    if (node.isDir) {
                        expanded = !expanded
                    } else {
                        node.file?.let(onLeafClick)
                    }
                }.padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.isDir) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "收起" else "展开",
                tint = palette.muted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = palette.fg2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
            val file = node.file
            val icon =
                when (file?.status) {
                    FileStatus.READ -> Icons.Outlined.Article
                    FileStatus.MODIFIED -> Icons.Outlined.Edit
                    FileStatus.ADDED -> Icons.Outlined.AddCircleOutline
                    FileStatus.ERROR -> Icons.Outlined.ErrorOutline
                    null -> Icons.Outlined.Article
                }
            val tint =
                when (file?.status) {
                    FileStatus.READ -> palette.muted2
                    FileStatus.MODIFIED -> palette.warning
                    FileStatus.ADDED -> palette.success
                    FileStatus.ERROR -> palette.danger
                    null -> palette.muted2
                }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                fontSize = 13.sp,
                color = palette.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            file?.let {
                Text(
                    text = statusLabel(it.status),
                    fontSize = 10.sp,
                    color = tint,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }

    if (node.isDir && expanded) {
        node.children.forEach { child ->
            FileNodeRow(node = child, indent = indent + 1, onLeafClick = onLeafClick)
        }
    }
}

private fun statusLabel(status: FileStatus): String =
    when (status) {
        FileStatus.READ -> "read"
        FileStatus.MODIFIED -> "modified"
        FileStatus.ADDED -> "added"
        FileStatus.ERROR -> "error"
    }

/** 文件内容预览弹窗（基于 tool_result.output 缓存） */
@Composable
private fun FilePreviewDialog(
    file: SessionFile,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.panel,
        title = {
            Column {
                Text(
                    text = file.path,
                    fontSize = 15.sp,
                    color = palette.fg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${statusLabel(file.status)} · ${file.tool}",
                    fontSize = 11.sp,
                    color = palette.muted,
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
        text = {
            val content = file.content ?: "（无内容缓存：该工具未返回 output）"
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.bg2)
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
            ) {
                Text(
                    text = if (content.length > 8000) content.take(8000) + "\n…（内容过长已截断）" else content,
                    color = palette.fg2,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = palette.accent)
            }
        },
    )
}

/** 导出用：转义 JSON 字符串中的引号/反斜杠/换行。 */
private fun escapeJson(s: String): String =
    s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

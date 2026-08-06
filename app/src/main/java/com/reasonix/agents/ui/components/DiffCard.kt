package com.reasonix.agents.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import com.reasonix.agents.ui.markdown.DefaultGrammarLocator
import com.reasonix.agents.ui.theme.LocalPalette
import io.noties.prism4j.Prism4j

// ═══════════════════════════════════════════════
// 颜色常量
// ═══════════════════════════════════════════════

private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Success: Color @Composable get() = LocalPalette.current.success
private val Danger: Color @Composable get() = LocalPalette.current.danger
private val Warning: Color @Composable get() = LocalPalette.current.warning
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted

/**
 * diff 高亮配色快照：在 @Composable 上下文读取颜色后传入，
 * 供非 composable 的渲染逻辑（highlightDiff/appendNodes/fallbackHighlight）使用。
 */
private data class DiffHighlightColors(
    val deleted: Color,
    val inserted: Color,
    val coord: Color,
    val bold: Color,
    val unchanged: Color,
)

/** diff 行类型（与 Prism diff grammar 的 token 对应） */
enum class DiffLineType { CONTEXT, ADD, DELETE, HEADER }

/** 解析出的 diff 内容：目标文件（可空）+ 标准 unified diff 文本 */
data class DiffParseResult(
    val filePath: String?,
    val unifiedDiff: String,
)

/**
 * DiffParser — 把工具 args / output 解析为标准 unified diff。
 *
 * 支持的输入形态：
 * 1. 标准 unified diff（`--- a/…` `+++ b/…` `@@ …`）
 * 2. apply_patch 输出（`*** Begin Patch` / `*** Update File: path` / SEARCH-REPLACE 块）
 * 3. SEARCH/REPLACE 块（`<<<<<<< SEARCH … ======= … >>>>>>> REPLACE`）
 * 4. JSON args：`{file, search, replace}` 或 `{path, old_string, new_string}`
 *
 * 无法识别 → 返回 null（调用方回退到原始文本展示）。
 */
object DiffParser {
    fun parse(
        args: String?,
        output: String?,
    ): DiffParseResult? {
        output?.let {
            parseUnifiedDiff(it)?.let { r -> return r }
            parseApplyPatch(it)?.let { r -> return r }
            parseSearchReplaceText(it)?.let { r -> return r }
        }
        args?.let {
            parseArgsJson(it)?.let { r -> return r }
            parseSearchReplaceText(it)?.let { r -> return r }
        }
        return null
    }

    /** 标准 unified diff：含 --- / +++ / @@ 头 */
    private fun parseUnifiedDiff(text: String): DiffParseResult? {
        val lines = text.lines()
        val hasFileHeader =
            lines.any { it.startsWith("--- ") } &&
                lines.any { it.startsWith("+++ ") }
        val hasHunk = lines.any { it.startsWith("@@") }
        if (!hasFileHeader && !hasHunk) return null
        // 判定为 diff 的最低门槛：存在 +/-/空格 内容行
        if (lines.none { it.startsWith("+") || it.startsWith("-") || it.startsWith(" ") }) return null
        val filePath =
            lines
                .firstOrNull { it.startsWith("+++ ") }
                ?.removePrefix("+++ ")
                ?.removePrefix("b/")
                ?.takeIf { it.isNotBlank() && it != "/dev/null" }
        return DiffParseResult(filePath, text.trimEnd('\n'))
    }

    /** apply_patch 输出（opencode/apply_patch 风格）：转换 SEARCH/REPLACE 为 unified diff */
    private fun parseApplyPatch(text: String): DiffParseResult? {
        if (!text.contains("*** Begin Patch") && !text.contains("*** Update File")) return null
        val sb = StringBuilder()
        var currentFile: String? = null
        var inSearch = false
        var inReplace = false
        val searchLines = mutableListOf<String>()
        val replaceLines = mutableListOf<String>()
        var hasContent = false

        fun flushBlock() {
            if (searchLines.isEmpty() && replaceLines.isEmpty()) return
            val file = currentFile?.removePrefix("/")
            sb.append("@@ -1,${searchLines.size} +1,${replaceLines.size} @@\n")
            searchLines.forEach { sb.append("-$it\n") }
            replaceLines.forEach { sb.append("+$it\n") }
            hasContent = true
            searchLines.clear()
            replaceLines.clear()
        }

        text.lines().forEach { line ->
            when {
                line.startsWith("*** Begin Patch") -> {
                    Unit
                }

                // 忽略
                line.startsWith("*** Update File:") -> {
                    flushBlock()
                    currentFile = line.removePrefix("*** Update File:").trim()
                    if (currentFile != null) {
                        sb.append("--- a/$currentFile\n+++ b/$currentFile\n")
                    }
                }

                line.startsWith("*** End Patch") -> {
                    flushBlock()
                }

                line.startsWith("<<<<<<< SEARCH") -> {
                    inSearch = true
                    inReplace = false
                }

                line == "=======" -> {
                    inSearch = false
                    inReplace = true
                }

                line.startsWith(">>>>>>> REPLACE") -> {
                    inReplace = false
                    flushBlock()
                }

                inSearch -> {
                    searchLines.add(line)
                }

                inReplace -> {
                    replaceLines.add(line)
                }

                else -> {
                    // @@ 等统一 diff 行直接透传
                    sb.append(line).append('\n')
                    if (line.isNotBlank()) hasContent = true
                }
            }
        }
        flushBlock()
        if (!hasContent && sb.isBlank()) return null
        val filePath = currentFile?.removePrefix("/")?.takeIf { it.isNotBlank() }
        return DiffParseResult(filePath, sb.toString().trimEnd('\n'))
    }

    /** 裸 SEARCH/REPLACE 文本块 */
    private fun parseSearchReplaceText(text: String): DiffParseResult? {
        val searchMarker = "<<<<<<< SEARCH"
        val replaceMarker = "======="
        val endMarker = ">>>>>>> REPLACE"
        val startIdx = text.indexOf(searchMarker)
        val midIdx = text.indexOf(replaceMarker, startIdx + 1)
        val endIdx = text.indexOf(endMarker, midIdx + 1)
        if (startIdx < 0 || midIdx < 0 || endIdx < 0) return null

        val search =
            text
                .substring(startIdx + searchMarker.length, midIdx)
                .removePrefix("\n")
                .trimEnd('\n')
        val replace =
            text
                .substring(midIdx + replaceMarker.length, endIdx)
                .removePrefix("\n")
                .trimEnd('\n')

        val sb = StringBuilder()
        sb.append("@@ -1,${search.lines().size} +1,${replace.lines().size} @@\n")
        search.lines().forEach { sb.append("-$it\n") }
        replace.lines().forEach { sb.append("+$it\n") }
        return DiffParseResult(null, sb.toString().trimEnd('\n'))
    }

    /** JSON args：{file, search, replace} / {path, old_string, new_string} */
    private fun parseArgsJson(args: String): DiffParseResult? {
        if (!args.trimStart().startsWith("{")) return null
        return try {
            val obj = JsonParser.parseString(args).asJsonObject
            val search =
                obj.get("search")?.asString
                    ?: obj.get("old_string")?.asString
                    ?: obj.get("oldValue")?.asString
                    ?: return null
            val replace =
                obj.get("replace")?.asString
                    ?: obj.get("new_string")?.asString
                    ?: obj.get("newValue")?.asString
                    ?: return null
            if (search.isBlank() && replace.isBlank()) return null
            val file =
                obj.get("file")?.asString
                    ?: obj.get("path")?.asString
                    ?: obj.get("file_path")?.asString
            buildSearchReplace(file, search, replace)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildSearchReplace(
        filePath: String?,
        search: String,
        replace: String,
    ): DiffParseResult {
        val sb = StringBuilder()
        if (!filePath.isNullOrBlank()) {
            val p = filePath.removePrefix("/")
            sb.append("--- a/$p\n+++ b/$p\n")
        }
        sb.append("@@ -1,${search.lines().size} +1,${replace.lines().size} @@\n")
        search.lines().forEach { sb.append("-$it\n") }
        replace.lines().forEach { sb.append("+$it\n") }
        return DiffParseResult(filePath, sb.toString().trimEnd('\n'))
    }
}

// ═══════════════════════════════════════════════
// DiffCard — 可折叠 diff 渲染
// ═══════════════════════════════════════════════

/** 超过该行数默认折叠 */
private const val DIFF_FOLD_LIMIT = 60

/**
 * Diff 卡片：Prism_diff 语法高亮（deleted 红 / inserted 绿 / coord 橙 / unchanged 灰）
 * + 红绿行着色 + 可折叠 + 增减统计。
 */
@Composable
fun DiffCard(
    result: DiffParseResult,
    modifier: Modifier = Modifier,
) {
    val diff = result.unifiedDiff
    val lineCount = diff.lines().size
    val addCount = diff.lines().count { it.startsWith("+") && !it.startsWith("+++") }
    val delCount = diff.lines().count { it.startsWith("-") && !it.startsWith("---") }
    var expanded by remember { mutableStateOf(lineCount <= DIFF_FOLD_LIMIT) }

    val highlightColors =
        DiffHighlightColors(
            deleted = Danger,
            inserted = Success,
            coord = Accent,
            bold = Warning,
            unchanged = Fg2,
        )
    val highlighted = remember(diff, highlightColors) { highlightDiff(diff, highlightColors) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Bg2)
                .border(1.dp, Border, RoundedCornerShape(8.dp)),
    ) {
        // ── 头部 ──
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Code,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = result.filePath ?: "diff",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Fg,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+$addCount −$delCount",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Muted,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Muted,
                modifier = Modifier.size(16.dp),
            )
        }

        // ── 内容 ──
        if (expanded) {
            // 2026-08-06：去掉横向滚动，diff 内容在屏幕宽度内自动换行（适配全输出内容/手机屏幕）
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = highlighted,
                    color = Fg2,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                )
            }
            if (lineCount > DIFF_FOLD_LIMIT) {
                Text(
                    text = "共 $lineCount 行，点击头部收起",
                    fontSize = 10.sp,
                    color = Muted,
                    modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
                )
            }
        }
    }
}

/**
 * 用 Prism4j diff 语法高亮整块 diff（token alias/type → 颜色）。
 * 高亮失败时兜底按行首字符着色（- 红 / + 绿 / @@ 橙 / --- +++ 蓝紫 / 其他灰）。
 */
private fun highlightDiff(
    text: String,
    colors: DiffHighlightColors,
): AnnotatedString {
    return try {
        val prism4j = Prism4j(DefaultGrammarLocator())
        val grammar =
            DefaultGrammarLocator().grammar(prism4j, "diff")
                ?: return fallbackHighlight(text, colors)
        val nodes = prism4j.tokenize(text, grammar)
        if (nodes.isEmpty()) return fallbackHighlight(text, colors)
        buildAnnotatedString {
            appendNodes(nodes, colors)
        }
    } catch (e: Exception) {
        fallbackHighlight(text, colors)
    }
}

/** 递归渲染 Prism tokenize 节点树：Syntax 按类型着色，Text 直接追加 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendNodes(
    nodes: List<io.noties.prism4j.Prism4j.Node>,
    colors: DiffHighlightColors,
) {
    nodes.forEach { node ->
        if (node.isSyntax) {
            val syntax = node as io.noties.prism4j.Prism4j.Syntax
            val color =
                when {
                    syntax.alias() == "deleted" -> colors.deleted
                    syntax.alias() == "inserted" -> colors.inserted
                    syntax.type() == "coord" -> colors.coord
                    syntax.alias() == "bold" -> colors.bold
                    syntax.type() == "unchanged" -> colors.unchanged
                    else -> null
                }
            if (color != null) {
                withStyle(SpanStyle(color = color)) {
                    if (syntax.children().isNotEmpty()) {
                        appendNodes(syntax.children(), colors)
                    } else {
                        append(syntax.matchedString())
                    }
                }
            } else {
                if (syntax.children().isNotEmpty()) {
                    appendNodes(syntax.children(), colors)
                } else {
                    append(syntax.matchedString())
                }
            }
        } else {
            append((node as io.noties.prism4j.Prism4j.Text).literal())
        }
    }
}

/** 兜底：无 Prism 高亮时按行首字符着色 */
private fun fallbackHighlight(
    text: String,
    colors: DiffHighlightColors,
): AnnotatedString =
    buildAnnotatedString {
        text.lines().forEach { line ->
            val color =
                when {
                    line.startsWith("+++") || line.startsWith("---") -> colors.coord
                    line.startsWith("@@") -> colors.bold
                    line.startsWith("+") -> colors.inserted
                    line.startsWith("-") -> colors.deleted
                    else -> colors.unchanged
                }
            withStyle(SpanStyle(color = color)) { append(line) }
            append('\n')
        }
    }

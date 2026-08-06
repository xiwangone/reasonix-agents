package com.reasonix.agents.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import com.reasonix.agents.ui.markdown.DefaultGrammarLocator
import com.reasonix.agents.ui.theme.LocalChatFont
import com.reasonix.agents.ui.theme.LocalPalette
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration

import io.noties.markwon.PrecomputedTextSetterCompat

import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.markwon.utils.NoCopySpannableFactory
import io.noties.prism4j.Prism4j
import java.util.concurrent.Executors

// ═══════════════════════════════════════════════════════════════════
//  Reasonix 主题色板（深色基准值；明/暗由 LocalPalette 动态注入）
//  2026-08-07：浅色模式修复——Markdown 渲染不再硬编码近白文字，
//  改为跟随 LocalPalette（fg/fg2/bg2/panel2/accent），浅色下正文深色可读。
// ═══════════════════════════════════════════════════════════════════
private val CODE_BLOCK_BG_DARK = 0xFF2E2C2E.toInt() // 代码块恒深底（明暗一致，保持代码区对比度）
private val CODE_BLOCK_BG_LIGHT = 0xFFF1EEF6.toInt() // 浅色代码块背景
private val CODE_BLOCK_TEXT_DARK = 0xFFE8E6F0.toInt() // 代码块文字（深底）
private val CODE_BLOCK_TEXT_LIGHT = 0xFF3A3547.toInt() // 代码块文字（浅底）
private val FG_TEXT_DARK = 0xFFF5F2F0.toInt() // 深色模式正文基准（近白）
private val ACCENT_DARK = 0xFFEA8800.toInt() // 深色模式链接色

/**
 * 安卓原生 Markdown 渲染器 —— 基于 Markwon v4 + AndroidView。
 *
 * 覆盖的 Markdown 特性：
 * - 粗体/斜体/删除线  • 有序/无序列表  • 标题 H1~H6
 * - 引用块  • 内联代码 & 代码块  • 表格  • 任务列表
 * - 链接（可点击）  • 图片（Coil 异步加载）  • HTML 标签
 * - 语法高亮（Prism4j，当前无语言语法文件故退化为纯文本渲染）
 *
 * @param markdown   原始 Markdown 文本
 * @param modifier   外部修饰符
 * @param linkColor  链接颜色（默认使用 Reasonix 主题色）
 */
@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    codeBackground: Color = Color.Unspecified,
    codeTextColor: Color = Color.Unspecified,
    linkColor: Color = Color.Unspecified,
) {
    val context = LocalContext.current
    // 2026-08-06：聊天字体（RikkaHub ChatFont）——在 @Composable 上下文读取，供 factory 应用
    val chatTypeface = LocalChatFont.current
    // 2026-08-07：浅色模式修复——从 LocalPalette 动态取色（不再硬编码近白）
    val palette = LocalPalette.current
    val isDark = palette.fg.luminance() < 0.5f

    // 使用 applicationContext 避免泄露 Activity；按明暗缓存两个 Markwon 实例
    val markwon = remember(isDark) { buildMarkwon(context.applicationContext, isDark) }

    AndroidView(
        factory = { ctx ->
            // 2026-08-06：去掉 HorizontalScrollView 外层，TextView 直接铺满父容器宽度——
            // 长文本/代码/表格均在屏幕宽度内自动换行，适配不同屏幕布局（手机/平板均不横向溢出）。
            TextView(ctx).apply {
                // 2026-08-07：浅色下用 palette.fg（深色），深色下用近白
                setTextColor(if (isDark) Color(FG_TEXT_DARK).toArgb() else palette.fg.toArgb())
                // 2026-08-06：RikkaHub ChatFont 适配——应用聊天字体（默认/衬线/等宽/JetBrains Mono）
                chatTypeface?.let { typeface = it }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setLineSpacing(4f, 1f)
                movementMethod = LinkMovementMethod.getInstance()
                isClickable = true
                // 流式更新时不复制 Spannable，防止闪烁
                setSpannableFactory(NoCopySpannableFactory.getInstance())
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
        // 2026-08-06：必须 fillMaxWidth 提供明确宽度约束——否则 Compose 无约束测量时
        // TextView MATCH_PARENT 退化为 wrap_content，长文本按内容宽度展开不换行
        modifier = modifier.fillMaxWidth(),
    )
}

// ═══════════════════════════════════════════════════════════════════
//  Markwon 实例构建（全局缓存，避免重复初始化）
// ═══════════════════════════════════════════════════════════════════


private val LOCK = Any()
private val markwonCache = HashMap<Boolean, Markwon>()

private fun buildMarkwon(context: Context, isDark: Boolean): Markwon {
    markwonCache[isDark]?.let { return it }

    synchronized(LOCK) {
        markwonCache[isDark]?.let { return it }

        // Prism4j — 语法高亮引擎（暂无 language grammar）
        val prism4j = Prism4j(DefaultGrammarLocator())

        // Prism4j 主题（基于 Darkula，覆盖背景色以匹配 Reasonix）
        val prismTheme = Prism4jThemeDarkula.create()

        // Coil 2.x ImageLoader（兼容 Markwon image-coil 插件）
        val coilLoader =
            ImageLoader
                .Builder(context)
                .crossfade(true)
                .build()

        // dp → px
        val density = context.resources.displayMetrics.density
        val cellPaddingPx = (8f * density).toInt()
        val borderWidthPx =
            if (density >= 3f) {
                3
            } else if (density >= 1.5f) {
                2
            } else {
                1
            }

        val tableBorder = if (isDark) 0xFF3D3938.toInt() else 0xFFD8D2E2.toInt()
        val tableHeader = if (isDark) 0xFF2E2C2E.toInt() else 0xFFE9E4F2.toInt()
        val tableTheme =
            TableTheme
                .emptyBuilder()
                .tableBorderColor(tableBorder)
                .tableBorderWidth(borderWidthPx) // 实线边框（密度自适应）
                .tableCellPadding(cellPaddingPx) // 8dp 内边距
                .tableHeaderRowBackgroundColor(tableHeader)
                .tableEvenRowBackgroundColor(0x00000000) // 偶数行 透明
                .tableOddRowBackgroundColor(if (isDark) 0x0DFFFFFF.toInt() else 0x14000000.toInt()) // 奇数行 微染
                .build()

        val markwon =
            Markwon
                .builder(context)
                // ① 语法高亮（最先注册；同时设置 code 文字/背景色）
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prismTheme))
                // ② HTML 支持
                .usePlugin(HtmlPlugin.create())
                // ③ 图片 SchemeHandler 注册
                .usePlugin(ImagesPlugin.create())
                // ④ 图片异步加载 — Coil 2.x（显式传入 ImageLoader）
                .usePlugin(CoilImagesPlugin.create(context, coilLoader))
                // ⑤ 删除线扩展
                .usePlugin(StrikethroughPlugin.create())
                // ⑥ 表格支持
                .usePlugin(TablePlugin.create(tableTheme))
                // ⑦ 任务列表
                .usePlugin(TaskListPlugin.create(context))
                // ⑧ 自动链接
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(MarkwonInlineParserPlugin.create())
                // ⑨ 主题覆盖（必须在 SyntaxHighlightPlugin 之后注册，以覆盖内联代码背景色）
                .usePlugin(ReasonixThemePlugin(isDark))
                .textSetter(PrecomputedTextSetterCompat.create(Executors.newCachedThreadPool()))
                .build()
        markwonCache[isDark] = markwon
        return markwon
    }
}

/**
 * Reasonix 暗色主题插件 —— 覆盖核心颜色以匹配 index.html 色板。
 */
private class ReasonixThemePlugin(private val isDark: Boolean) : AbstractMarkwonPlugin() {
    override fun configureTheme(builder: MarkwonTheme.Builder) {
        val accent = if (isDark) ACCENT_DARK else 0xFF5B3DF0.toInt()
        val codeBg = if (isDark) 0xFF222022.toInt() else 0xFFEFEBF8.toInt()
        val border = if (isDark) 0xFF3D3938.toInt() else 0xFFD8D2E2.toInt()
        val fg = if (isDark) FG_TEXT_DARK else 0xFF1B1726.toInt()
        val fg2 = if (isDark) 0xFFCCC5C0.toInt() else 0xFF403A52.toInt()
        builder
            // 链接
            .linkColor(accent)
            .isLinkUnderlined(true)
            // 内联代码（覆盖 SyntaxHighlightPlugin 设置的值）
            .codeBackgroundColor(codeBg)
            .codeTextColor(fg2)
            // 代码块（明暗各自底色，保持对比度）
            .codeBlockBackgroundColor(if (isDark) CODE_BLOCK_BG_DARK else CODE_BLOCK_BG_LIGHT)
            .codeBlockTextColor(if (isDark) CODE_BLOCK_TEXT_DARK else CODE_BLOCK_TEXT_LIGHT)
            // 2026-08-07：代码块独立字体——正文按聊天字体，代码块恒 JetBrains Mono
            // （官方 API，替代 CodeFontPlugin/SpanFactory——后者依赖的 CodeBlock 类
            //   在 markwon 4.6.2 配套的 commonmark 0.13.0/0.21.0 中均不存在，是编译失败的根因）
            .codeBlockTypeface(Typeface.MONOSPACE)
            // 引用块
            .blockQuoteColor(border)
            // 标题分割线
            .headingBreakColor(border)
            // 水平分割线
            .thematicBreakColor(border)
            // 列表项
            .listItemColor(fg)
    }
}

/** 判断字符串是否不含 Markdown 语法。纯文本可跳过完整渲染管线。 */
fun isPlainText(text: String): Boolean {
    val patterns =
        listOf(
            "\\*\\*",
            "\\*",
            "__",
            "_",
            "```",
            "`",
            "##",
            "> ",
            "- ",
            "\\d+\\. ",
            "!\\[",
            "\\[",
            "\\|",
        )
    return patterns.none { Regex(it).containsMatchIn(text) }
}

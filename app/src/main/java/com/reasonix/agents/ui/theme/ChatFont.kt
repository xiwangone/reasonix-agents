package com.reasonix.agents.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.reasonix.agents.R
import com.reasonix.agents.data.AppSettingsStore

/**
 * 聊天字体（RikkaHub ChatFont 适配版，2026-08-06）。
 *
 * 从 RikkaHub（ui/theme/ChatFont.kt）移植：
 * - LocalChatFont：CompositionLocal 提供当前聊天字体（null = 系统默认）
 * - ChatFontProvider：按设置注入字体
 * - 内置 JetBrains Mono（RikkaHub 附带，303KB）
 *
 * 未涉及（跳过）：GoogleSans Flex（4.1MB 过大，避免 APK 膨胀）、
 * 自定义字体文件选择（CUSTOM，Reasonix 无文件选择器）。
 */
val LocalChatFont = staticCompositionLocalOf<Typeface?> { null }

@Composable
fun ChatFontProvider(
    chatFont: Int,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val typeface = remember(chatFont) { resolveChatFont(context, chatFont) }
    CompositionLocalProvider(LocalChatFont provides typeface) {
        content()
    }
}

fun resolveChatFont(
    context: Context,
    chatFont: Int,
): Typeface? =
    when (chatFont) {
        AppSettingsStore.CHAT_FONT_SERIF -> Typeface.SERIF
        AppSettingsStore.CHAT_FONT_MONO -> Typeface.MONOSPACE
        AppSettingsStore.CHAT_FONT_JETBRAINS ->
            ResourcesCompat.getFont(context, R.font.jetbrains_mono)
        else -> null // DEFAULT
    }

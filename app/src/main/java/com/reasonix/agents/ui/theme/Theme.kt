package com.reasonix.agents.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ReasonixAgentsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 第九批：Material3 colorScheme 与 LocalPalette 调色板联动。
 *
 * 让 Material3 原生组件（TopAppBar / SegmentedButton / OutlinedTextField /
 * FilledIconButton / AssistChip 等）自动使用与 LocalPalette 一致的配色，
 * 从而在会话界面重构中既保留主题预设体系（品牌紫蓝 / Material × 明暗），
 * 又能直接使用 MaterialTheme.colorScheme 的原生语义色。
 */
fun paletteColorScheme(palette: Palette, dark: Boolean): ColorScheme {
    val onAccent = Color.White
    val scheme: ColorScheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = onAccent,
            primaryContainer = palette.accentS,
            onPrimaryContainer = palette.fg,
            secondary = palette.violet,
            onSecondary = onAccent,
            secondaryContainer = palette.accentS,
            onSecondaryContainer = palette.fg,
            tertiary = palette.success,
            onTertiary = onAccent,
            background = palette.bg,
            onBackground = palette.fg,
            surface = palette.panel,
            onSurface = palette.fg,
            surfaceVariant = palette.panel2,
            onSurfaceVariant = palette.muted,
            surfaceContainerLowest = palette.bg,
            surfaceContainerLow = palette.bg2,
            surfaceContainer = palette.panel,
            surfaceContainerHigh = palette.panel2,
            surfaceContainerHighest = palette.card,
            outline = palette.border,
            outlineVariant = palette.borderStr,
            error = palette.danger,
            onError = Color.White,
            errorContainer = palette.dangerS,
            onErrorContainer = palette.danger,
            inverseSurface = palette.bg2,
            inverseOnSurface = palette.fg
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = onAccent,
            primaryContainer = palette.accentS,
            onPrimaryContainer = palette.fg,
            secondary = palette.violet,
            onSecondary = onAccent,
            secondaryContainer = palette.accentS,
            onSecondaryContainer = palette.fg,
            tertiary = palette.success,
            onTertiary = onAccent,
            background = palette.bg,
            onBackground = palette.fg,
            surface = palette.panel,
            onSurface = palette.fg,
            surfaceVariant = palette.panel2,
            onSurfaceVariant = palette.muted,
            surfaceContainerLowest = palette.bg,
            surfaceContainerLow = palette.bg2,
            surfaceContainer = palette.panel,
            surfaceContainerHigh = palette.panel2,
            surfaceContainerHighest = palette.card,
            outline = palette.border,
            outlineVariant = palette.borderStr,
            error = palette.danger,
            onError = Color.White,
            errorContainer = palette.dangerS,
            onErrorContainer = palette.danger,
            inverseSurface = palette.bg2,
            inverseOnSurface = palette.fg
        )
    }
    return scheme
}

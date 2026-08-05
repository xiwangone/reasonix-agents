package com.reasonix.agents.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 调色板（批 A-2 主题预设体系）：两套风格 × 明/暗 = 4 套配色。
 * - 品牌紫蓝（Reasonix Brand）：紫蓝渐变主色，默认主题
 * - Material：Material 3 标准色系（蓝/紫）
 * 各 Screen 通过 LocalPalette 取色，支持主题切换与跟随系统。
 */
data class Palette(
    val bg: Color,
    val bg2: Color,
    val panel: Color,
    val panel2: Color,
    val card: Color,
    val cardHover: Color,
    val border: Color,
    val borderStr: Color,
    val accent: Color,
    val accentS: Color,
    val violet: Color,
    val fg: Color,
    val fg2: Color,
    val muted: Color,
    val muted2: Color,
    val danger: Color,
    val dangerS: Color,
    val success: Color,
    val successS: Color,
    val warning: Color,
    val warningS: Color,
)

// ═══════════════════════════════════════════════
// 品牌紫蓝（Reasonix Brand）— 默认主题
// ═══════════════════════════════════════════════

val DarkPalette =
    Palette(
        bg = Color(0xFF16131E),
        bg2 = Color(0xFF1D1927),
        panel = Color(0xFF241F30),
        panel2 = Color(0xFF2A2438),
        card = Color(0xFF211C2C),
        cardHover = Color(0xFF2B2540),
        border = Color(0xFF37304A),
        borderStr = Color(0xFF51486B),
        accent = Color(0xFF7C5CFC),
        accentS = Color(0x2E7C5CFC),
        violet = Color(0xFFA78BFA),
        fg = Color(0xFFF4F2FA),
        fg2 = Color(0xFFC9C2E0),
        muted = Color(0xFF9B92B8),
        muted2 = Color(0xFF77709B),
        danger = Color(0xFFE04636),
        dangerS = Color(0x29E04636),
        success = Color(0xFF40A060),
        successS = Color(0x2440A060),
        warning = Color(0xFFE5B830),
        warningS = Color(0x29E5B830),
    )

val LightPalette =
    Palette(
        bg = Color(0xFFF7F5FC),
        bg2 = Color(0xFFEFEBF8),
        panel = Color(0xFFE7E1F4),
        panel2 = Color(0xFFDDD5EF),
        card = Color(0xFFFFFFFF),
        cardHover = Color(0xFFF1EDFB),
        border = Color(0xFFD2C9E8),
        borderStr = Color(0xFFB3A6D9),
        accent = Color(0xFF5B3DF0),
        accentS = Color(0x1F5B3DF0),
        violet = Color(0xFF8B5CF6),
        fg = Color(0xFF1B1726),
        fg2 = Color(0xFF403A52),
        muted = Color(0xFF6E6684),
        muted2 = Color(0xFF9A93AE),
        danger = Color(0xFFC03222),
        dangerS = Color(0x29C03222),
        success = Color(0xFF2E7D46),
        successS = Color(0x242E7D46),
        warning = Color(0xFFB58A12),
        warningS = Color(0x29B58A12),
    )

// ═══════════════════════════════════════════════
// Material 3 标准色系（备选主题）
// ═══════════════════════════════════════════════

val MaterialDarkPalette =
    Palette(
        bg = Color(0xFF1C1B1F),
        bg2 = Color(0xFF242329),
        panel = Color(0xFF2B2A31),
        panel2 = Color(0xFF313039),
        card = Color(0xFF28272D),
        cardHover = Color(0xFF33323A),
        border = Color(0xFF4A4951),
        borderStr = Color(0xFF6A6974),
        accent = Color(0xFF8AB4F8),
        accentS = Color(0x2E8AB4F8),
        violet = Color(0xFFD0BCFF),
        fg = Color(0xFFE6E1E5),
        fg2 = Color(0xFFCAC4D0),
        muted = Color(0xFFA19CA8),
        muted2 = Color(0xFF7E7984),
        danger = Color(0xFFF2B8B5),
        dangerS = Color(0x29F2B8B5),
        success = Color(0xFF81C995),
        successS = Color(0x2481C995),
        warning = Color(0xFFFDD663),
        warningS = Color(0x29FDD663),
    )

val MaterialLightPalette =
    Palette(
        bg = Color(0xFFFDF8FD),
        bg2 = Color(0xFFF4EFF4),
        panel = Color(0xFFEAE5EA),
        panel2 = Color(0xFFDFD9DF),
        card = Color(0xFFFFFFFF),
        cardHover = Color(0xFFF5EFF5),
        border = Color(0xFFD6D0D6),
        borderStr = Color(0xFFB6B0B6),
        accent = Color(0xFF2962A0),
        accentS = Color(0x1F2962A0),
        violet = Color(0xFF6750A4),
        fg = Color(0xFF1D1B20),
        fg2 = Color(0xFF44464F),
        muted = Color(0xFF76747C),
        muted2 = Color(0xFF9C9AA2),
        danger = Color(0xFFB3261E),
        dangerS = Color(0x29B3261E),
        success = Color(0xFF2E7D46),
        successS = Color(0x242E7D46),
        warning = Color(0xFFB58A12),
        warningS = Color(0x29B58A12),
    )

val LocalPalette = staticCompositionLocalOf { DarkPalette }

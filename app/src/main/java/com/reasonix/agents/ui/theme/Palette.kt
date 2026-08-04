package com.reasonix.agents.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 调色板：暗色（默认）/ 浅色两套。
 * 各 Screen 通过 LocalPalette 取色，支持主题切换。
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
    val warningS: Color
)

val DarkPalette = Palette(
    bg = Color(0xFF1C1A1B),
    bg2 = Color(0xFF222022),
    panel = Color(0xFF2A2729),
    panel2 = Color(0xFF2E2C2E),
    card = Color(0xFF282528),
    cardHover = Color(0xFF302E30),
    border = Color(0xFF3D3938),
    borderStr = Color(0xFF5A5452),
    accent = Color(0xFFEA8800),
    accentS = Color(0x26EA8800),
    violet = Color(0xFF9B6FD8),
    fg = Color(0xFFF5F2F0),
    fg2 = Color(0xFFCCC5C0),
    muted = Color(0xFF9E9896),
    muted2 = Color(0xFF7A7270),
    danger = Color(0xFFE04636),
    dangerS = Color(0x29E04636),
    success = Color(0xFF40A060),
    successS = Color(0x2440A060),
    warning = Color(0xFFE5B830),
    warningS = Color(0x29E5B830)
)

val LightPalette = Palette(
    bg = Color(0xFFF7F5F4),
    bg2 = Color(0xFFF0EEEC),
    panel = Color(0xFFE8E4E2),
    panel2 = Color(0xFFDEDAD8),
    card = Color(0xFFFFFFFF),
    cardHover = Color(0xFFF2F0EE),
    border = Color(0xFFD0CBC8),
    borderStr = Color(0xFFB0A8A4),
    accent = Color(0xFFC96F00),
    accentS = Color(0x26C96F00),
    violet = Color(0xFF7A55B8),
    fg = Color(0xFF1C1A1B),
    fg2 = Color(0xFF3D3836),
    muted = Color(0xFF6E6866),
    muted2 = Color(0xFF9A9490),
    danger = Color(0xFFC03222),
    dangerS = Color(0x29C03222),
    success = Color(0xFF2E7D46),
    successS = Color(0x242E7D46),
    warning = Color(0xFFB58A12),
    warningS = Color(0x29B58A12)
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

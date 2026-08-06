package com.reasonix.agents.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * RikkaHub Agents 主题预设适配版（2026-08-06）。
 *
 * 从 RikkaHub Agents 源码（me.rerere.rikkahub.ui.theme.presets）移植 7 套预设的核心配色，
 * 映射到 Reasonix 自研 Palette 体系：
 * - bg      ← background
 * - bg2     ← surfaceVariant（次级背景）
 * - panel   ← surfaceContainer 近似（用 surfaceVariant 变体）
 * - card    ← surfaceContainerHighest 近似（用 surfaceVariant 提亮/压暗）
 * - border  ← outlineVariant
 * - accent  ← primary
 * - fg      ← onBackground
 * - fg2     ← onSurfaceVariant
 * - muted   ← outline
 * - danger/success/warning 保留 Reasonix 默认值（RikkaHub 未提供语义色）
 *
 * 仅移植「核心可见配色」，未涉及的功能（如 RikkaHub 的 ExtendColors 代码高亮色板、
 * 自定义主题生成器）暂跳过。
 */
object RikkaPresets {
    /** 预设 id → 名称（设置页展示） */
    val ids = listOf("sakura", "ocean", "spring", "autumn", "black", "minimal", "claude")
    val names = mapOf(
        "sakura" to "Sakura 樱花",
        "ocean" to "Ocean 海洋",
        "spring" to "Spring 春意",
        "autumn" to "Autumn 秋实",
        "black" to "Black 纯黑",
        "minimal" to "Minimal 极简",
        "claude" to "Claude 暖橙",
    )

    fun darkPalette(id: String): Palette =
        when (id) {
            "sakura" -> sakuraDark
            "ocean" -> oceanDark
            "spring" -> springDark
            "autumn" -> autumnDark
            "black" -> blackDark
            "minimal" -> minimalDark
            "claude" -> claudeDark
            else -> DarkPalette
        }

    fun lightPalette(id: String): Palette =
        when (id) {
            "sakura" -> sakuraLight
            "ocean" -> oceanLight
            "spring" -> springLight
            "autumn" -> autumnLight
            "black" -> blackLight
            "minimal" -> minimalLight
            "claude" -> claudeLight
            else -> LightPalette
        }
}

// ═══════════ Sakura 樱花（粉） ═══════════
private val sakuraLight =
    Palette(
        bg = Color(0xFFFFF8F7), bg2 = Color(0xFFF3DDDF), panel = Color(0xFFF3DDDF),
        panel2 = Color(0xFFE7C8CB), card = Color(0xFFF3DDDF), cardHover = Color(0xFFE7C8CB),
        border = Color(0xFFD7C1C3), borderStr = Color(0xFFC9AFB2),
        accent = Color(0xFF8E4955), accentS = Color(0x338E4955), violet = Color(0xFF8E4955),
        fg = Color(0xFF22191A), fg2 = Color(0xFF524345), muted = Color(0xFF847374),
        muted2 = Color(0xFFA08E90),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val sakuraDark =
    Palette(
        bg = Color(0xFF1A1112), bg2 = Color(0xFF524345), panel = Color(0xFF524345),
        panel2 = Color(0xFF5C4A4C), card = Color(0xFF524345), cardHover = Color(0xFF5C4A4C),
        border = Color(0xFF524345), borderStr = Color(0xFF6E5C5E),
        accent = Color(0xFFFFB2BC), accentS = Color(0x33FFB2BC), violet = Color(0xFFFFB2BC),
        fg = Color(0xFFF0DEDF), fg2 = Color(0xFFD7C1C3), muted = Color(0xFF9F8C8E),
        muted2 = Color(0xFF7E6E70),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

// ═══════════ Ocean 海洋（青蓝） ═══════════
private val oceanLight =
    Palette(
        bg = Color(0xFFF6FAFD), bg2 = Color(0xFFDCE4E9), panel = Color(0xFFDCE4E9),
        panel2 = Color(0xFFCBD6DD), card = Color(0xFFDCE4E9), cardHover = Color(0xFFCBD6DD),
        border = Color(0xFFC0C8CD), borderStr = Color(0xFFA9B4BB),
        accent = Color(0xFF116682), accentS = Color(0x33116682), violet = Color(0xFF116682),
        fg = Color(0xFF171C1F), fg2 = Color(0xFF40484C), muted = Color(0xFF70787D),
        muted2 = Color(0xFF8E969B),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val oceanDark =
    Palette(
        bg = Color(0xFF0F1417), bg2 = Color(0xFF40484C), panel = Color(0xFF40484C),
        panel2 = Color(0xFF4A5256), card = Color(0xFF40484C), cardHover = Color(0xFF4A5256),
        border = Color(0xFF40484C), borderStr = Color(0xFF5A6266),
        accent = Color(0xFF8BD0EF), accentS = Color(0x338BD0EF), violet = Color(0xFF8BD0EF),
        fg = Color(0xFFDFE3E7), fg2 = Color(0xFFC0C8CD), muted = Color(0xFF8A9297),
        muted2 = Color(0xFF6E767B),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

// ═══════════ Spring 春意（绿） ═══════════
private val springLight =
    Palette(
        bg = Color(0xFFF9FAEF), bg2 = Color(0xFFE1E4D5), panel = Color(0xFFE1E4D5),
        panel2 = Color(0xFFD2D6C4), card = Color(0xFFE1E4D5), cardHover = Color(0xFFD2D6C4),
        border = Color(0xFFC5C8BA), borderStr = Color(0xFFADB1A1),
        accent = Color(0xFF4C662B), accentS = Color(0x334C662B), violet = Color(0xFF4C662B),
        fg = Color(0xFF1A1C16), fg2 = Color(0xFF44483D), muted = Color(0xFF75796C),
        muted2 = Color(0xFF94988B),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val springDark =
    Palette(
        bg = Color(0xFF12140E), bg2 = Color(0xFF44483D), panel = Color(0xFF44483D),
        panel2 = Color(0xFF4E5247), card = Color(0xFF44483D), cardHover = Color(0xFF4E5247),
        border = Color(0xFF44483D), borderStr = Color(0xFF5E6257),
        accent = Color(0xFFB1D18A), accentS = Color(0x33B1D18A), violet = Color(0xFFB1D18A),
        fg = Color(0xFFE2E3D8), fg2 = Color(0xFFC5C8BA), muted = Color(0xFF8F9285),
        muted2 = Color(0xFF73766A),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

// ═══════════ Autumn 秋实（金棕） ═══════════
private val autumnLight =
    Palette(
        bg = Color(0xFFFFF8F1), bg2 = Color(0xFFEBE1CF), panel = Color(0xFFEBE1CF),
        panel2 = Color(0xFFDED2BC), card = Color(0xFFEBE1CF), cardHover = Color(0xFFDED2BC),
        border = Color(0xFFCFC6B4), borderStr = Color(0xFFB8AE99),
        accent = Color(0xFF735C0C), accentS = Color(0x33735C0C), violet = Color(0xFF735C0C),
        fg = Color(0xFF1F1B13), fg2 = Color(0xFF4C4639), muted = Color(0xFF7E7667),
        muted2 = Color(0xFF9D9587),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val autumnDark =
    Palette(
        bg = Color(0xFF16130B), bg2 = Color(0xFF4C4639), panel = Color(0xFF4C4639),
        panel2 = Color(0xFF565043), card = Color(0xFF4C4639), cardHover = Color(0xFF565043),
        border = Color(0xFF4C4639), borderStr = Color(0xFF66604F),
        accent = Color(0xFFE3C46D), accentS = Color(0x33E3C46D), violet = Color(0xFFE3C46D),
        fg = Color(0xFFEAE1D4), fg2 = Color(0xFFCFC6B4), muted = Color(0xFF989080),
        muted2 = Color(0xFF7C7465),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

// ═══════════ Black 纯黑（灰阶） ═══════════
private val blackLight =
    Palette(
        bg = Color(0xFFFFFFFF), bg2 = Color(0xFFF7F7F7), panel = Color(0xFFF7F7F7),
        panel2 = Color(0xFFECECEC), card = Color(0xFFF7F7F7), cardHover = Color(0xFFECECEC),
        border = Color(0xFFEBEBEB), borderStr = Color(0xFFD0D0D0),
        accent = Color(0xFF606060), accentS = Color(0x33606060), violet = Color(0xFF606060),
        fg = Color(0xFF252525), fg2 = Color(0xFF444444), muted = Color(0xFFB5B5B5),
        muted2 = Color(0xFF8E8E8E),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val blackDark =
    Palette(
        bg = Color(0xFF1C1C1C), bg2 = Color(0xFF444444), panel = Color(0xFF444444),
        panel2 = Color(0xFF4E4E4E), card = Color(0xFF444444), cardHover = Color(0xFF4E4E4E),
        border = Color(0xFF444444), borderStr = Color(0xFF5E5E5E),
        accent = Color(0xFFEBEBEB), accentS = Color(0x33EBEBEB), violet = Color(0xFFEBEBEB),
        fg = Color(0xFFFCFCFC), fg2 = Color(0xFFEBEBEB), muted = Color(0xFF8E8E8E),
        muted2 = Color(0xFF707070),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

// ═══════════ Minimal 极简（蓝） ═══════════
private val minimalLight =
    Palette(
        bg = Color(0xFFFFFFFF), bg2 = Color(0xFFF1F2F4), panel = Color(0xFFF1F2F4),
        panel2 = Color(0xFFE5E7EA), card = Color(0xFFF1F2F4), cardHover = Color(0xFFE5E7EA),
        border = Color(0xFFE8E9EC), borderStr = Color(0xFFD0D3D8),
        accent = Color(0xFF2563EB), accentS = Color(0x332563EB), violet = Color(0xFF2563EB),
        fg = Color(0xFF16181D), fg2 = Color(0xFF3D4046), muted = Color(0xFFC7CACF),
        muted2 = Color(0xFF8E9196),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val minimalDark =
    Palette(
        bg = Color(0xFF0F1012), bg2 = Color(0xFF2B2D31), panel = Color(0xFF2B2D31),
        panel2 = Color(0xFF35373C), card = Color(0xFF2B2D31), cardHover = Color(0xFF35373C),
        border = Color(0xFF2E3035), borderStr = Color(0xFF484B51),
        accent = Color(0xFF8CB0FF), accentS = Color(0x338CB0FF), violet = Color(0xFF8CB0FF),
        fg = Color(0xFFE6E7EA), fg2 = Color(0xFFC7CACF), muted = Color(0xFF4C4F55),
        muted2 = Color(0xFF6E7178),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

// ═══════════ Claude 暖橙 ═══════════
private val claudeLight =
    Palette(
        bg = Color(0xFFFAF9F5), bg2 = Color(0xFFEDEAE0), panel = Color(0xFFEDEAE0),
        panel2 = Color(0xFFE0DCCF), card = Color(0xFFEDEAE0), cardHover = Color(0xFFE0DCCF),
        border = Color(0xFFE5E1D6), borderStr = Color(0xFFCCC7B8),
        accent = Color(0xFFC96442), accentS = Color(0x33C96442), violet = Color(0xFFC96442),
        fg = Color(0xFF262624), fg2 = Color(0xFF3A372F), muted = Color(0xFFBDB7A9),
        muted2 = Color(0xFF8A8578),
        danger = Color(0xFFBA1A1A), dangerS = Color(0x29BA1A1A),
        success = Color(0xFF407A40), successS = Color(0x24407A40),
        warning = Color(0xFF785831), warningS = Color(0x29785831),
    )

private val claudeDark =
    Palette(
        bg = Color(0xFF1F1E1D), bg2 = Color(0xFF3A372F), panel = Color(0xFF3A372F),
        panel2 = Color(0xFF444138), card = Color(0xFF3A372F), cardHover = Color(0xFF444138),
        border = Color(0xFF3A372F), borderStr = Color(0xFF545148),
        accent = Color(0xFFE4906E), accentS = Color(0x33E4906E), violet = Color(0xFFE4906E),
        fg = Color(0xFFEDEAE3), fg2 = Color(0xFFE5E1D6), muted = Color(0xFF5A564C),
        muted2 = Color(0xFF7C786E),
        danger = Color(0xFFFFB4AB), dangerS = Color(0x29FFB4AB),
        success = Color(0xFF81C995), successS = Color(0x2481C995),
        warning = Color(0xFFEABF8F), warningS = Color(0x29EABF8F),
    )

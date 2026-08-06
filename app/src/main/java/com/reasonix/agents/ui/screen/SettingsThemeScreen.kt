package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.theme.RikkaPresets

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Muted2: Color @Composable get() = LocalPalette.current.muted2

/**
 * 主题设置二级界面（第四批：设置组件化）。
 * 从设置页「主题」入口进入，含配色风格 / 明暗模式 / 语言。
 */
@Composable
fun SettingsThemeScreen(
    settings: AppSettingsStore.Settings,
    onSettingsChange: (AppSettingsStore.Settings) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            // ── 顶栏（返回 + 标题）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Fg)
                }
                Text(
                    text = "主题",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 配色风格（批 A-2）──
            SectionTitle("配色风格")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeChip(
                    "品牌紫蓝",
                    settings.themePreset == AppSettingsStore.THEME_PRESET_BRAND,
                    { onSettingsChange(settings.copy(themePreset = AppSettingsStore.THEME_PRESET_BRAND)) },
                )
                ThemeChip(
                    "Material",
                    settings.themePreset == AppSettingsStore.THEME_PRESET_MATERIAL,
                    { onSettingsChange(settings.copy(themePreset = AppSettingsStore.THEME_PRESET_MATERIAL)) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 2026-08-06 RikkaHub 主题预设适配版
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSettingsStore.RIKKA_PRESET_IDS.forEachIndexed { idx, rid ->
                    ThemeChip(
                        RikkaPresets.names[rid] ?: rid,
                        settings.themePreset == AppSettingsStore.THEME_PRESET_RIKKA_BASE + idx,
                        {
                            onSettingsChange(
                                settings.copy(themePreset = AppSettingsStore.THEME_PRESET_RIKKA_BASE + idx),
                            )
                        },
                        // 2026-08-06 优化：实时预览——chip 显示该预设主色圆点（深色模式用深色主色）
                        colorDot = RikkaPresets.lightPalette(rid).accent,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "品牌紫蓝：Reasonix 品牌色系；Material：Material 标准蓝紫；Sakura/Ocean/Spring/Autumn/Black/Minimal/Claude：RikkaHub Agents 预设移植。",
                fontSize = 10.sp,
                color = Muted2,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 明暗模式 ──
            SectionTitle("明暗模式")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeChip("跟随系统", settings.themeMode == AppSettingsStore.THEME_MODE_SYSTEM, {
                    onSettingsChange(settings.copy(themeMode = AppSettingsStore.THEME_MODE_SYSTEM))
                })
                ThemeChip("浅色", settings.themeMode == AppSettingsStore.THEME_MODE_LIGHT, {
                    onSettingsChange(settings.copy(themeMode = AppSettingsStore.THEME_MODE_LIGHT))
                })
                ThemeChip("深色", settings.themeMode == AppSettingsStore.THEME_MODE_DARK, {
                    onSettingsChange(settings.copy(themeMode = AppSettingsStore.THEME_MODE_DARK))
                })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 语言（批 A-5：全局生效；当前版本仅中文文案，偏好先持久化）──
            SectionTitle("语言")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeChip("简体中文", settings.language == "zh", {
                    onSettingsChange(settings.copy(language = "zh"))
                })
                ThemeChip("English", settings.language == "en", {
                    onSettingsChange(settings.copy(language = "en"))
                })
            }
            Text(
                text = "当前版本仅提供中文界面，语言偏好已保存（预留）",
                fontSize = 10.sp,
                color = Muted2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

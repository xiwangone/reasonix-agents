package com.reasonix.agents.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * 主题图标切换（批 B-13）。
 * 通过 setComponentEnabledSetting 在三个 launcher activity-alias 间切换：
 * - LauncherBrand      品牌紫蓝（默认）
 * - LauncherBrandDark  品牌深色
 * - LauncherMaterial   Material 风格
 * 主题预设/明暗变化时调用 [apply] 立即生效（无需重启）。
 */
object AppIconSwitcher {
    const val ALIAS_BRAND = "com.reasonix.agents.LauncherBrand"
    const val ALIAS_BRAND_DARK = "com.reasonix.agents.LauncherBrandDark"
    const val ALIAS_MATERIAL = "com.reasonix.agents.LauncherMaterial"

    private val aliases = listOf(ALIAS_BRAND, ALIAS_BRAND_DARK, ALIAS_MATERIAL)

    /** 根据主题预设 + 明暗模式决定启用哪个图标别名。 */
    fun resolveAlias(
        themePreset: Int,
        themeMode: Int,
    ): String =
        when {
            themePreset == 1 -> ALIAS_MATERIAL

            // Material 风格
            themeMode == 2 -> ALIAS_BRAND_DARK

            // 品牌紫蓝 · 深色
            else -> ALIAS_BRAND // 品牌紫蓝（默认）
        }

    fun apply(
        context: Context,
        themePreset: Int,
        themeMode: Int,
    ) {
        val target = resolveAlias(themePreset, themeMode)
        aliases.forEach { alias ->
            val enabled = alias == target
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, alias),
                if (enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}

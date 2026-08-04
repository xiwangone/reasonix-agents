package com.reasonix.agents.data

import android.content.Context

/**
 * 应用设置本地持久化（SharedPreferences）。
 * 主题模式：0=跟随系统，1=浅色，2=深色
 * 显示选项：推理过程折叠、token/费用显示、欢迎页提示
 */
object AppSettingsStore {

    private const val PREFS_NAME = "reasonix_app_settings"
    private const val KEY_THEME_MODE = "theme_mode"          // 0=system 1=light 2=dark
    private const val KEY_SHOW_REASONING = "show_reasoning"  // 默认 true
    private const val KEY_SHOW_TOKENS = "show_tokens"        // 默认 true

    data class Settings(
        val themeMode: Int = 0,
        val showReasoning: Boolean = true,
        val showTokens: Boolean = true
    )

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            themeMode = prefs.getInt(KEY_THEME_MODE, 0),
            showReasoning = prefs.getBoolean(KEY_SHOW_REASONING, true),
            showTokens = prefs.getBoolean(KEY_SHOW_TOKENS, true)
        )
    }

    fun save(context: Context, settings: Settings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, settings.themeMode)
            .putBoolean(KEY_SHOW_REASONING, settings.showReasoning)
            .putBoolean(KEY_SHOW_TOKENS, settings.showTokens)
            .apply()
    }

    fun setThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun setShowReasoning(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_REASONING, show).apply()
    }

    fun setShowTokens(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_TOKENS, show).apply()
    }
}

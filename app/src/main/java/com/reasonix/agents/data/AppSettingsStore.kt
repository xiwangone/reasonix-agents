package com.reasonix.agents.data

import android.content.Context

/**
 * 应用设置本地持久化（SharedPreferences）。
 *
 * 主题（批 A-2 主题预设体系）：
 * - themePreset：0=品牌紫蓝（Reasonix Brand 渐变风），1=Material 风格
 * - themeMode：  0=跟随系统，1=浅色，2=深色
 * - language：   界面语言（"zh"/"en"），当前版本仅中文文案，偏好先持久化预留
 *
 * 网络（批 B-11 设置项扩展）：
 * - connectTimeoutSec：REST 连接超时（秒）
 * - sseReconnectEnabled：SSE 断线自动重连开关
 * - sseReconnectMaxDelaySec：SSE 重连指数退避上限（秒）
 *
 * 安全（批 A-1）：
 * - httpWarningAcked：HTTP 明文一次性警告是否已确认
 */
object AppSettingsStore {
    private const val PREFS_NAME = "reasonix_app_settings"
    private const val KEY_THEME_PRESET = "theme_preset" // 0=brand 1=material
    private const val KEY_THEME_MODE = "theme_mode" // 0=system 1=light 2=dark
    private const val KEY_LANGUAGE = "language" // zh / en
    private const val KEY_SHOW_REASONING = "show_reasoning" // 默认 true
    private const val KEY_SHOW_TOKENS = "show_tokens" // 默认 true
    private const val KEY_CONNECT_TIMEOUT_SEC = "connect_timeout_sec"
    private const val KEY_SSE_RECONNECT_ENABLED = "sse_reconnect_enabled"
    private const val KEY_SSE_RECONNECT_MAX_DELAY_SEC = "sse_reconnect_max_delay_sec"
    private const val KEY_HTTP_WARNING_ACKED = "http_warning_acked"
    private const val KEY_CHAT_FONT = "chat_font"

    const val THEME_PRESET_BRAND = 0
    const val THEME_PRESET_MATERIAL = 1
    // 2026-08-06 RikkaHub 主题预设适配：2=sakura 3=ocean 4=spring 5=autumn 6=black 7=minimal 8=claude
    const val THEME_PRESET_RIKKA_BASE = 2
    const val THEME_PRESET_COUNT = 9

    /** RikkaHub 预设 id（与 RikkaPresets.ids 顺序一致） */
    val RIKKA_PRESET_IDS = listOf("sakura", "ocean", "spring", "autumn", "black", "minimal", "claude")

    fun rikkaPresetId(themePreset: Int): String {
        val idx = themePreset - THEME_PRESET_RIKKA_BASE
        return if (idx in RIKKA_PRESET_IDS.indices) RIKKA_PRESET_IDS[idx] else "sakura"
    }

    fun isRikkaPreset(themePreset: Int): Boolean =
        themePreset in THEME_PRESET_RIKKA_BASE until THEME_PRESET_RIKKA_BASE + RIKKA_PRESET_IDS.size

    const val THEME_MODE_SYSTEM = 0
    const val THEME_MODE_LIGHT = 1
    const val THEME_MODE_DARK = 2

    // 聊天字体（RikkaHub ChatFontFamily 适配：默认/衬线/等宽/JetBrains Mono）
    const val CHAT_FONT_DEFAULT = 0
    const val CHAT_FONT_SERIF = 1
    const val CHAT_FONT_MONO = 2
    const val CHAT_FONT_JETBRAINS = 3

    // 自动压缩（2026-08-13 仿 RikkaHub Agents 双模式）
    const val AUTO_COMPACT_OFF = 0
    const val AUTO_COMPACT_PERCENT = 1
    const val AUTO_COMPACT_TOKEN = 2
    // 网络代理（2026-08-13）：SOCKS5 / HTTP
    const val KEY_PROXY_ENABLED = "proxy_enabled"
    const val KEY_PROXY_TYPE = "proxy_type" // socks / http
    const val KEY_PROXY_HOST = "proxy_host"
    const val KEY_PROXY_PORT = "proxy_port"
    // 2026-08-18：自定义快捷操作
    const val KEY_QUICK_ACTIONS = "quick_actions"

    const val KEY_AUTO_COMPACT_MODE = "auto_compact_mode" // 0=关 1=百分比阈值 2=token 累计
    const val KEY_AUTO_COMPACT_THRESHOLD = "auto_compact_threshold" // 百分比 0-100 或 token 数
    const val DEFAULT_AUTO_COMPACT_THRESHOLD_PERCENT = 80
    const val DEFAULT_AUTO_COMPACT_THRESHOLD_TOKEN = 40000

    data class Settings(
        val themePreset: Int = THEME_PRESET_BRAND,
        val themeMode: Int = THEME_MODE_SYSTEM,
        val chatFont: Int = CHAT_FONT_DEFAULT,
        val language: String = "zh",
        val showReasoning: Boolean = true,
        val showTokens: Boolean = true,
        val connectTimeoutSec: Int = 30,
        val sseReconnectEnabled: Boolean = true,
        val sseReconnectMaxDelaySec: Int = 30,
        val httpWarningAcked: Boolean = false,
        // 自动压缩（2026-08-13）
        val autoCompactMode: Int = AUTO_COMPACT_OFF,
        val autoCompactThreshold: Int = DEFAULT_AUTO_COMPACT_THRESHOLD_PERCENT,
        // 网络代理（2026-08-13）
        val proxyEnabled: Boolean = false,
        val proxyType: String = "socks",
        val proxyHost: String = "",
        val proxyPort: Int = 1080,
        // 2026-08-18：自定义快捷操作（标签 + 提示词）
        val quickActions: List<QuickAction> = DEFAULT_QUICK_ACTIONS,
    )

    /** 快捷操作项 */
    data class QuickAction(
        val label: String,
        val prompt: String,
    )

    /** 默认快捷操作（放 object 顶层，避免 data class 默认值引用 companion 常量） */
    val DEFAULT_QUICK_ACTIONS = listOf(
        QuickAction("解释代码", "请解释以下代码的功能和结构："),
        QuickAction("修复错误", "请查找并修复以下代码中的错误："),
        QuickAction("编写测试", "请为以下代码编写单元测试："),
        QuickAction("代码审查", "请对以下代码进行代码审查，指出潜在问题和改进建议："),
        QuickAction("优化性能", "请分析以下代码的性能瓶颈并给出优化建议："),
    )

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 2026-08-18：加载自定义快捷操作
        val quickActionsJson = prefs.getString(KEY_QUICK_ACTIONS, null)
        val quickActions = if (!quickActionsJson.isNullOrBlank()) {
            try {
                val arr = com.google.gson.JsonParser.parseString(quickActionsJson).asJsonArray
                arr.map { el ->
                    val obj = el.asJsonObject
                    QuickAction(
                        label = obj.get("label")?.asString ?: "",
                        prompt = obj.get("prompt")?.asString ?: "",
                    )
                }.filter { it.label.isNotBlank() && it.prompt.isNotBlank() }
            } catch (_: Exception) {
                DEFAULT_QUICK_ACTIONS
            }
        } else {
            DEFAULT_QUICK_ACTIONS
        }

        return Settings(
            themePreset = prefs.getInt(KEY_THEME_PRESET, THEME_PRESET_BRAND),
            themeMode = prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM),
            chatFont = prefs.getInt(KEY_CHAT_FONT, CHAT_FONT_DEFAULT),
            language = prefs.getString(KEY_LANGUAGE, "zh") ?: "zh",
            showReasoning = prefs.getBoolean(KEY_SHOW_REASONING, true),
            showTokens = prefs.getBoolean(KEY_SHOW_TOKENS, true),
            connectTimeoutSec = prefs.getInt(KEY_CONNECT_TIMEOUT_SEC, 30),
            sseReconnectEnabled = prefs.getBoolean(KEY_SSE_RECONNECT_ENABLED, true),
            sseReconnectMaxDelaySec = prefs.getInt(KEY_SSE_RECONNECT_MAX_DELAY_SEC, 30),
            httpWarningAcked = prefs.getBoolean(KEY_HTTP_WARNING_ACKED, false),
            autoCompactMode = prefs.getInt(KEY_AUTO_COMPACT_MODE, AUTO_COMPACT_OFF),
            autoCompactThreshold = prefs.getInt(KEY_AUTO_COMPACT_THRESHOLD, DEFAULT_AUTO_COMPACT_THRESHOLD_PERCENT),
            proxyEnabled = prefs.getBoolean(KEY_PROXY_ENABLED, false),
            proxyType = prefs.getString(KEY_PROXY_TYPE, "socks") ?: "socks",
            proxyHost = prefs.getString(KEY_PROXY_HOST, "") ?: "",
            proxyPort = prefs.getInt(KEY_PROXY_PORT, 1080),
            quickActions = quickActions,
        )
    }

    fun save(
        context: Context,
        s: Settings,
    ) {
        // 2026-08-18：保存自定义快捷操作
        val quickActionsJson = com.google.gson.Gson().toJson(s.quickActions)

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_PRESET, s.themePreset)
            .putInt(KEY_THEME_MODE, s.themeMode)
            .putInt(KEY_CHAT_FONT, s.chatFont)
            .putString(KEY_LANGUAGE, s.language)
            .putBoolean(KEY_SHOW_REASONING, s.showReasoning)
            .putBoolean(KEY_SHOW_TOKENS, s.showTokens)
            .putInt(KEY_CONNECT_TIMEOUT_SEC, s.connectTimeoutSec)
            .putBoolean(KEY_SSE_RECONNECT_ENABLED, s.sseReconnectEnabled)
            .putInt(KEY_SSE_RECONNECT_MAX_DELAY_SEC, s.sseReconnectMaxDelaySec)
            .putBoolean(KEY_HTTP_WARNING_ACKED, s.httpWarningAcked)
            .putInt(KEY_AUTO_COMPACT_MODE, s.autoCompactMode)
            .putInt(KEY_AUTO_COMPACT_THRESHOLD, s.autoCompactThreshold)
            .putBoolean(KEY_PROXY_ENABLED, s.proxyEnabled)
            .putString(KEY_PROXY_TYPE, s.proxyType)
            .putString(KEY_PROXY_HOST, s.proxyHost)
            .putInt(KEY_PROXY_PORT, s.proxyPort)
            .putString(KEY_QUICK_ACTIONS, quickActionsJson)
            .apply()
    }

    fun setThemePreset(
        context: Context,
        preset: Int,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_PRESET, preset)
            .apply()
    }

    fun setThemeMode(
        context: Context,
        mode: Int,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
    }

    fun setLanguage(
        context: Context,
        language: String,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun setShowReasoning(
        context: Context,
        show: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_REASONING, show)
            .apply()
    }

    fun setShowTokens(
        context: Context,
        show: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_TOKENS, show)
            .apply()
    }

    fun setHttpWarningAcked(
        context: Context,
        acked: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HTTP_WARNING_ACKED, acked)
            .apply()
    }
}

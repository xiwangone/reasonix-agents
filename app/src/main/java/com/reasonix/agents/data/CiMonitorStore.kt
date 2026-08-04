package com.reasonix.agents.data

import android.content.Context

/**
 * CI 监控设置本地持久化（SharedPreferences）。
 * GitHub token 仅存本机，展示时脱敏。
 */
object CiMonitorStore {

    private const val PREFS_NAME = "reasonix_ci_monitor"
    private const val KEY_ENABLED = "ci_enabled"            // 悬浮窗总开关
    private const val KEY_GITHUB_TOKEN = "github_token"      // 只存本机，不明文外传
    private const val KEY_OWNER = "ci_owner"                 // 仓库 owner，默认 xiwangone
    private const val KEY_REPO = "ci_repo"                   // 仓库名，默认 reasonix-agents
    private const val KEY_INTERVAL_MS = "ci_interval_ms"     // 刷新间隔 ms，默认 60_000

    data class CiSettings(
        val enabled: Boolean = false,
        val githubToken: String = "",
        val owner: String = "xiwangone",
        val repo: String = "reasonix-agents",
        val intervalMs: Long = 60_000L
    )

    fun load(context: Context): CiSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return CiSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            githubToken = prefs.getString(KEY_GITHUB_TOKEN, "") ?: "",
            owner = prefs.getString(KEY_OWNER, "xiwangone") ?: "xiwangone",
            repo = prefs.getString(KEY_REPO, "reasonix-agents") ?: "reasonix-agents",
            intervalMs = prefs.getLong(KEY_INTERVAL_MS, 60_000L)
        )
    }

    fun save(context: Context, s: CiSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, s.enabled)
            .putString(KEY_GITHUB_TOKEN, s.githubToken)
            .putString(KEY_OWNER, s.owner)
            .putString(KEY_REPO, s.repo)
            .putLong(KEY_INTERVAL_MS, s.intervalMs)
            .apply()
    }

    /** 脱敏展示：前3后3+***；空或过短则原样 mask */
    fun maskToken(token: String): String = when {
        token.isEmpty() -> ""
        token.length <= 8 -> "***"
        else -> token.take(3) + "***" + token.takeLast(3)
    }
}

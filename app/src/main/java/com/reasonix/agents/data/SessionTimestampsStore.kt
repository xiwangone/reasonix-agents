package com.reasonix.agents.data

import android.content.Context

/**
 * 会话最近访问时间存储（2026-08-07 设计稿 #1：侧栏日期分组）。
 *
 * 服务端 /sessions 返回的 SessionInfo 无时间戳字段，故在本地记录每个会话的
 * 最近访问时间（epochMillis），侧栏据此分「今天 / 昨天 / 更早」三组。
 * 无本地记录的新会话视为「今天」。
 */
object SessionTimestampsStore {
    private const val PREFS_NAME = "reasonix_session_timestamps"
    private const val KEY_PREFIX = "ts_"

    fun loadAll(context: Context): Map<String, Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = mutableMapOf<String, Long>()
        for (key in prefs.all.keys) {
            if (key.startsWith(KEY_PREFIX)) {
                val name = key.removePrefix(KEY_PREFIX)
                val ts = prefs.getLong(key, 0L)
                if (ts > 0L) result[name] = ts
            }
        }
        return result
    }

    fun touch(
        context: Context,
        name: String,
    ) {
        if (name.isBlank()) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PREFIX + name, System.currentTimeMillis())
            .apply()
    }

    fun remove(
        context: Context,
        name: String,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREFIX + name)
            .apply()
    }
}

package com.reasonix.agents.data

import android.content.Context

/**
 * 会话置顶存储（2026-08-06 对齐 RikkaHub 会话抽屉：置顶）。
 *
 * 本地记录置顶会话名集合（服务端 SessionInfo 无置顶字段，置顶为客户端本地行为）。
 * 侧栏渲染时置顶会话排前并显示星标，点击星标可切换。
 */
object PinnedSessionsStore {
    private const val PREFS_NAME = "reasonix_pinned_sessions"
    private const val KEY_PINNED = "pinned_json"

    fun load(context: Context): Set<String> {
        val raw =
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PINNED, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    private fun save(
        context: Context,
        pinned: Set<String>,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PINNED, pinned.joinToString("\n"))
            .apply()
    }

    fun toggle(
        context: Context,
        name: String,
    ): Set<String> {
        val pinned = load(context).toMutableSet()
        if (!pinned.add(name)) pinned.remove(name)
        save(context, pinned)
        return pinned
    }
}

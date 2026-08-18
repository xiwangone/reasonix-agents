package com.reasonix.agents.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reasonix.agents.data.model.SessionInfo

/**
 * 会话列表本地缓存（离线浏览支持）。
 *
 * 网络可用时从服务器加载会话列表并缓存到 SharedPreferences；
 * 网络不可用时展示缓存的会话列表，支持离线浏览历史。
 *
 * 设计原则：
 * - 只缓存会话元数据（名称、路径、标题、轮次），不缓存完整消息内容
 * - 消息内容通过 API 获取，缓存仅作为离线降级方案
 * - 缓存数据在每次成功加载时更新，保证数据新鲜度
 */
object SessionCacheStore {
    private const val PREFS_NAME = "reasonix_session_cache"
    private const val KEY_SESSIONS = "cached_sessions"
    private const val KEY_LAST_UPDATE = "last_update_time"

    /** 缓存的会话列表 */
    data class CachedSessionList(
        val sessions: List<SessionInfo> = emptyList(),
        val lastUpdateTime: Long = 0L,
    )

    /**
     * 加载缓存的会话列表。
     *
     * @return 缓存的会话列表，如果无缓存则返回空列表
     */
    fun load(context: Context): List<SessionInfo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SessionInfo>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存会话列表到缓存。
     *
     * @param sessions 要缓存的会话列表
     */
    fun save(context: Context, sessions: List<SessionInfo>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SESSIONS, Gson().toJson(sessions))
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取缓存的最后更新时间。
     *
     * @return 最后更新时间戳（毫秒），如果无缓存则返回 0
     */
    fun getLastUpdateTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_UPDATE, 0L)
    }

    /**
     * 清除缓存。
     */
    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

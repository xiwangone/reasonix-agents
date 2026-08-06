package com.reasonix.agents.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 记忆存储（2026-08-06 新增：仿 RikkaHub Agents 记忆功能第一版）。
 *
 * 用户自定义长期记忆，持久化到 SharedPreferences（JSON）。
 * 启用后，发送消息时自动注入到会话（随用户消息一起发送，格式见 [activeMemoriesText]）。
 * 与提示词（PromptStore）的区别：记忆是长期事实/约定，由用户手动维护，随消息自动携带。
 */
object MemoryStore {
    private const val TAG = "MemoryStore"
    private const val PREFS_NAME = "reasonix_memories"
    private const val KEY_MEMORIES = "memories_json"
    private const val KEY_ENABLED = "enabled"

    /** 记忆条数上限 */
    const val MAX_MEMORIES = 20

    private val gson = Gson()

    /** 一条记忆。 */
    data class MemoryItem(
        val id: String = "",
        val content: String = "",
        val createdAt: Long = 0L,
    )

    fun load(context: Context): List<MemoryItem> {
        val raw =
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MEMORIES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<MemoryItem>>() {}.type
            gson.fromJson<List<MemoryItem>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析记忆失败", e)
            emptyList()
        }
    }

    fun save(
        context: Context,
        items: List<MemoryItem>,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEMORIES, gson.toJson(items))
            .apply()
    }

    /** 新增一条记忆（id 去重后追加）；返回新列表。 */
    fun add(
        context: Context,
        content: String,
    ): List<MemoryItem> {
        val items = load(context).toMutableList()
        val item = MemoryItem(id = "${System.currentTimeMillis()}-${items.size}", content = content.trim(), createdAt = System.currentTimeMillis())
        items.removeAll { it.id == item.id }
        items.add(item)
        val result = items.toList()
        save(context, result)
        return result
    }

    /** 删除记忆；返回新列表。 */
    fun remove(
        context: Context,
        id: String,
    ): List<MemoryItem> {
        val result = load(context).filterNot { it.id == id }
        save(context, result)
        return result
    }

    /** 启用开关（默认关闭）。 */
    fun isEnabled(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    /**
     * 注入文本：启用且存在记忆时返回「【记忆】…」段落，否则 null。
     * 由 ChatViewModel 拼入 effectiveInput（在提示词之后、用户文本之前）。
     */
    fun activeMemoriesText(context: Context): String? {
        if (!isEnabled(context)) return null
        val items = load(context).filter { it.content.isNotBlank() }
        if (items.isEmpty()) return null
        val body = items.joinToString("\n") { "- ${it.content}" }
        return "【记忆】\n$body"
    }
}

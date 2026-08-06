package com.reasonix.agents.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 记忆存储（2026-08-06 新增：仿 RikkaHub Agents 记忆功能）。
 *
 * 用户自定义长期记忆，持久化到 SharedPreferences（JSON）。
 * 启用后，发送消息时自动注入到会话（随用户消息一起发送）。
 * 支持两种维护方式：
 * 1. 手动：设置页「记忆」添加/删除；
 * 2. AI 直接管理（方案 A）：AI 在回复末尾写【记忆+】内容 / 【记忆-】内容 标记，
 *    客户端在 turn 结束时自动应用并剔除标记（见 processMarkers）。
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

    /** 按内容精确删除（AI 不知道 id，用内容匹配）。 */
    fun removeByContent(
        context: Context,
        content: String,
    ) {
        val result = load(context).filterNot { it.content == content.trim() }
        save(context, result)
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
     * 注入文本：启用时返回「【记忆】…」段落（含 AI 管理约定），否则 null。
     * 由 ChatViewModel 拼入 effectiveInput（在提示词之后、用户文本之前）。
     * 无记忆时也注入（携带约定，AI 首次即可写入）。
     */
    fun activeMemoriesText(context: Context): String? {
        if (!isEnabled(context)) return null
        val items = load(context).filter { it.content.isNotBlank() }
        val body = if (items.isEmpty()) "（暂无）" else items.joinToString("\n") { "- ${it.content}" }
        return "【记忆】\n$body\n约定：需要记住新事实时，在回复末尾单独一行写【记忆+】内容；删除某条记忆写【记忆-】内容（内容须与已存完全一致）。"
    }

    // ═══════════ AI 直接管理记忆（方案 A，2026-08-06）═══════════

    /** 标记前缀：新增记忆。 */
    private const val MARK_ADD = "【记忆+】"
    /** 标记前缀：删除记忆。 */
    private const val MARK_DEL = "【记忆-】"

    /**
     * 解析并应用回复中的记忆标记（逐行）：
     * - 行首为【记忆+】 → 该行其余部分作为新记忆写入
     * - 行首为【记忆-】 → 该行其余部分按内容精确删除
     * 返回剔除标记行后的干净内容（供 UI 展示）。
     */
    fun processMarkers(
        context: Context,
        content: String,
    ): String {
        val lines = content.split("\n")
        val kept = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith(MARK_ADD) -> {
                    val c = trimmed.removePrefix(MARK_ADD).trim()
                    if (c.isNotBlank()) add(context, c)
                    // 不保留该行
                }
                trimmed.startsWith(MARK_DEL) -> {
                    val c = trimmed.removePrefix(MARK_DEL).trim()
                    if (c.isNotBlank()) removeByContent(context, c)
                    // 不保留该行
                }
                else -> kept.add(line)
            }
        }
        return kept.joinToString("\n").trim()
    }
}

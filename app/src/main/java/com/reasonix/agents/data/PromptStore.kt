package com.reasonix.agents.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 用户自定义提示词本地存储（第四批：提示词功能）。
 *
 * 用户在设置页「提示词」区块添加的自定义提示词（最多 [MAX_PROMPTS] 条），
 * 持久化到 SharedPreferences（JSON），选中一条后发送消息时自动附加在
 * 系统提示词之后（随用户消息一起发给服务端）。
 */
object PromptStore {
    private const val TAG = "PromptStore"
    private const val PREFS_NAME = "reasonix_prompts"
    private const val KEY_PROMPTS = "prompts_json"
    private const val KEY_CURRENT = "current_prompt_id"

    /** 用户提示词数量上限 */
    const val MAX_PROMPTS = 10

    private val gson = Gson()

    /** 一条用户自定义提示词。 */
    data class CustomPrompt(
        val id: String = "",
        val content: String = "",
        val createdAt: Long = 0L
    )

    fun load(context: Context): List<CustomPrompt> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROMPTS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<CustomPrompt>>() {}.type
            gson.fromJson<List<CustomPrompt>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析自定义提示词失败", e)
            emptyList()
        }
    }

    fun save(context: Context, prompts: List<CustomPrompt>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROMPTS, gson.toJson(prompts))
            .apply()
    }

    /** 新增一条提示词（id 去重后追加）；返回新列表。 */
    fun add(context: Context, prompt: CustomPrompt): List<CustomPrompt> {
        val prompts = load(context).toMutableList()
        prompts.removeAll { it.id == prompt.id }
        prompts.add(prompt)
        val result = prompts.toList()
        save(context, result)
        return result
    }

    /** 删除提示词；返回新列表。 */
    fun remove(context: Context, id: String): List<CustomPrompt> {
        val result = load(context).filterNot { it.id == id }
        save(context, result)
        return result
    }

    /** 当前选中的提示词 id（选中后重开保留）。 */
    fun getCurrentId(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENT, "") ?: ""

    fun setCurrentId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CURRENT, id).apply()
    }
}

package com.reasonix.agents.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 自定义模型本地存储（批 B-9 添加模型弹窗）。
 * 用户在「添加模型」弹窗中录入的模型（provider 内置/自定义、base_url、兼容方式），
 * 持久化到 SharedPreferences（JSON），与服务端 GET /models 返回的模型合并展示。
 */
object CustomModelStore {
    private const val TAG = "CustomModelStore"
    private const val PREFS_NAME = "reasonix_custom_models"
    private const val KEY_MODELS = "models_json"
    private const val KEY_CURRENT = "current_model"
    private const val KEY_SEEDED = "zen_free_seeded"

    private val gson = Gson()

    // ── OpenCode Zen 免费模型预置（首次加载自动填充） ──
    private const val ZEN_FREE_BASE_URL = "https://opencode.ai/zen/v1"
    private val ZEN_FREE_MODELS = listOf(
        Triple("deepseek-v4-flash-free", "DeepSeek V4 Flash Free", "opencode-zen"),
        Triple("mimo-v2.5-free", "MiMo V2.5 Free", "opencode-zen"),
        Triple("hy3-free", "Hy3 Free", "opencode-zen"),
        Triple("nemotron-3-ultra-free", "Nemotron 3 Ultra Free", "opencode-zen"),
        Triple("nemotron-3.5-lightning-free", "Nemotron 3.5 Lightning Free", "opencode-zen"),
        Triple("laguna-s-2.1-free", "Laguna S 2.1 Free", "opencode-zen"),
    )

    /**
     * provider：builtin=内置 / custom=自定义；compat：openai / deepseek / other。
     * key：模型 key 兼容格式（如 "openai/deepseek-v4-flash"、"opencode-zen/deepseek-v4-flash-free"），
     * 按 key 分组展示（批 C-3/C-4 模型按 key 分组）。
     */
    data class CustomModel(
        val id: String = "",
        val name: String = "",
        val key: String = "",
        val provider: String = "custom",
        val baseUrl: String = "",
        val apiKey: String = "", // 2026-08-07：API Key（第三方兼容端点认证，可空）
        val compat: String = "openai",
    ) {
        /** 分组展示名：key 非空用 key，否则回退到 id/name。 */
        val groupLabel: String
            get() = key.ifBlank { id.ifBlank { name } }
    }

    fun load(context: Context): List<CustomModel> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MODELS, "") ?: ""

        // 首次加载：预置 Zen 免费模型
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            val seeded = ZEN_FREE_MODELS.map { (id, name, key) ->
                CustomModel(
                    id = id,
                    name = name,
                    key = key,
                    provider = "custom",
                    baseUrl = ZEN_FREE_BASE_URL,
                    apiKey = "",
                    compat = "openai",
                )
            }
            prefs.edit()
                .putBoolean(KEY_SEEDED, true)
                .apply()
            save(context, seeded)
            return seeded
        }

        if (raw.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<CustomModel>>() {}.type
            gson.fromJson<List<CustomModel>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析自定义模型失败", e)
            emptyList()
        }
    }

    fun save(
        context: Context,
        models: List<CustomModel>,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODELS, gson.toJson(models))
            .apply()
    }

    /** 新增模型（id 去重，同名覆盖）；返回新列表。 */
    fun add(
        context: Context,
        model: CustomModel,
    ): List<CustomModel> {
        val models = load(context).toMutableList()
        val id = model.id.ifBlank { model.name }
        models.removeAll { it.id == id || (it.name == model.name && it.name.isNotBlank()) }
        models.add(model.copy(id = id))
        val result = models.toList()
        save(context, result)
        return result
    }

    /** 删除模型；返回新列表。 */
    fun remove(
        context: Context,
        id: String,
    ): List<CustomModel> {
        val result = load(context).filterNot { it.id == id }
        save(context, result)
        return result
    }

    /** 本地记忆的「当前模型」（自定义模型选中后记录，重开保留）。 */
    fun getCurrent(context: Context): String =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENT, "") ?: ""

    fun setCurrent(
        context: Context,
        model: String,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENT, model)
            .apply()
    }
}

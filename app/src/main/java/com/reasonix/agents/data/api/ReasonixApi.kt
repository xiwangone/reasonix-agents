package com.reasonix.agents.data.api

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.AuthType
import java.net.Proxy
import com.reasonix.agents.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * 连接失败分类（批 A-1 错误分类提示）。
 * 连接页据此展示 401→认证失败 / 超时→网络不可达 / SSL→证书异常 / 404→路径错误 等提示。
 */
enum class ConnectFailKind { AUTH, TIMEOUT, SSL, NOT_FOUND, NETWORK, SERVER, UNKNOWN }

/** 连接诊断结果：OK 或带分类的失败原因。 */
sealed class ConnectResult {
    data object Ok : ConnectResult()

    data class Fail(
        val kind: ConnectFailKind,
        val message: String,
    ) : ConnectResult()
}

/**
 * Reasonix REST API — 对应 index.html 中 fetch() 调用的所有后端接口。
 */
class ReasonixApi(
    private val baseUrl: String,
    private val auth: AuthInfo? = null,
    private val connectTimeoutSec: Int = 30,
    /** 网络代理（2026-08-13）：null=直连 */
    private val proxy: Proxy? = null,
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(connectTimeoutSec.toLong(), TimeUnit.SECONDS)
            .readTimeout(connectTimeoutSec.toLong(), TimeUnit.SECONDS)
            .writeTimeout(connectTimeoutSec.toLong(), TimeUnit.SECONDS)
            .apply { proxy?.let { proxy(it) } }
            .build(),
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** Authorization 头：Basic（用户名:密码）或 Bearer（Token）；未配置认证时为 null。 */
    private val authHeader: String? =
        when (auth?.type) {
            AuthType.BASIC -> {
                "Basic " +
                    Base64.encodeToString(
                        "${auth.username}:${auth.password}".toByteArray(),
                        Base64.NO_WRAP,
                    )
            }

            AuthType.BEARER -> {
                "Bearer ${auth.token}"
            }

            else -> {
                null
            }
        }

    // ── 发送消息 ──
    suspend fun submit(input: String): Boolean =
        withContext(Dispatchers.IO) {
            post("/submit", mapOf("input" to input))
            true
        }

    // ── 取消当前操作 ──
    suspend fun cancel() =
        withContext(Dispatchers.IO) {
            post("/cancel")
        }

    // ── 获取历史消息 ──
    suspend fun getHistory(): List<HistoryMessage> =
        withContext(Dispatchers.IO) {
            val json = get("/history")
            if (json.isNullOrBlank()) return@withContext emptyList()
            try {
                gson.fromJson(json, object : TypeToken<List<HistoryMessage>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }

    // ── 获取服务器状态 ──
    suspend fun getStatus(): StatusInfo? =
        withContext(Dispatchers.IO) {
            val json = get("/status")
            if (json.isNullOrBlank()) return@withContext null
            try {
                gson.fromJson(json, StatusInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 连接诊断（批 A-1）：请求 /status 并按失败类型分类，供连接页给出针对性提示。
     * 分类：401/403→认证失败；超时→网络不可达；SSL→证书异常；404→路径错误；其余归网络/服务器/未知。
     */
    suspend fun diagnose(): ConnectResult =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url("$baseUrl/status")
            authHeader?.let { builder.header("Authorization", it) }
            try {
                val response = client.newCall(builder.get().build()).execute()
                val code = response.code
                response.close()
                when {
                    code in 200..299 -> {
                        ConnectResult.Ok
                    }

                    code == 401 || code == 403 -> {
                        ConnectResult.Fail(ConnectFailKind.AUTH, "认证失败（HTTP $code）：用户名/密码或 Token 不正确，请检查认证方式")
                    }

                    code == 404 -> {
                        ConnectResult.Fail(ConnectFailKind.NOT_FOUND, "路径错误（HTTP 404）：目标不是 Reasonix 服务，请确认端口（10002/443）与地址")
                    }

                    code in 500..599 -> {
                        ConnectResult.Fail(ConnectFailKind.SERVER, "服务器异常（HTTP $code）：服务端内部错误，请稍后重试")
                    }

                    else -> {
                        ConnectResult.Fail(ConnectFailKind.UNKNOWN, "连接失败（HTTP $code）")
                    }
                }
            } catch (e: SocketTimeoutException) {
                ConnectResult.Fail(ConnectFailKind.TIMEOUT, "网络不可达：连接超时，请检查地址/端口/网络或增大连接超时")
            } catch (e: SSLException) {
                ConnectResult.Fail(ConnectFailKind.SSL, "证书异常：HTTPS 证书验证失败，请确认服务器证书有效")
            } catch (e: UnknownHostException) {
                ConnectResult.Fail(ConnectFailKind.NETWORK, "网络不可达：无法解析主机名，请检查地址是否正确")
            } catch (e: ConnectException) {
                ConnectResult.Fail(ConnectFailKind.NETWORK, "网络不可达：无法连接到服务器，请检查地址/端口/网络")
            } catch (e: IOException) {
                ConnectResult.Fail(ConnectFailKind.NETWORK, "网络不可达：${e.message ?: "连接中断"}")
            } catch (e: Exception) {
                ConnectResult.Fail(ConnectFailKind.UNKNOWN, "连接失败：${e.message ?: "未知错误"}")
            }
        }

    // ── 获取任务清单（GET /todos）──
    // 服务端可能返回数组或 {todos:[...]} 对象，两种形态都兼容解析
    suspend fun getTodos(): List<TodoItem> =
        withContext(Dispatchers.IO) {
            val json = get("/todos")
            if (json.isNullOrBlank()) return@withContext emptyList()
            try {
                val arrayType = object : TypeToken<List<TodoItem>>() {}.type
                val asArray = gson.fromJson<List<TodoItem>>(json, arrayType)
                if (asArray != null) return@withContext asArray
                // 对象形态：{todos: [...]}
                val obj = gson.fromJson(json, JsonObject::class.java)
                val arr =
                    obj?.getAsJsonArray("todos")
                        ?: obj?.getAsJsonArray("items")
                        ?: return@withContext emptyList()
                gson.fromJson(arr, arrayType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    // ── 获取模型列表 ──
    suspend fun getModels(): ModelsResponse? =
        withContext(Dispatchers.IO) {
            val json = get("/models")
            if (json.isNullOrBlank()) return@withContext null
            try {
                gson.fromJson(json, ModelsResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }

    // ── 切换模型（官方 serve 协议：POST /submit {"input": "/model <ref>"} → 204）──
    // 2026-08-08：原 POST /settings 在服务端不存在（405 假阳性）；/model 斜杠命令
    // 由 serve submit handler 拦截并调 switchModel（会话级切换，保留历史）。
    suspend fun setModel(model: String) =
        withContext(Dispatchers.IO) {
            val ref = model.trim()
            if (ref.isBlank()) return@withContext
            post("/submit", mapOf("input" to "/model $ref"))
        }

    // ── 获取系统提示词（从 history 提取 role=system）──
    suspend fun getSystemPrompt(): String? =
        withContext(Dispatchers.IO) {
            val history = getHistory()
            history.firstOrNull { it.role == "system" }?.content
        }

    // ── 会话列表 ──
    suspend fun getSessions(): List<SessionInfo> =
        withContext(Dispatchers.IO) {
            val json = get("/sessions")
            if (json.isNullOrBlank()) return@withContext emptyList()
            try {
                gson.fromJson(json, object : TypeToken<List<SessionInfo>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }

    // ── 新建会话 ──
    suspend fun newSession() =
        withContext(Dispatchers.IO) {
            post("/new")
        }

    // ── 恢复会话 ──
    suspend fun resumeSession(path: String) =
        withContext(Dispatchers.IO) {
            post("/resume", mapOf("path" to path))
        }

    // ── 删除会话 ──
    suspend fun deleteSession(name: String) =
        withContext(Dispatchers.IO) {
            post("/delete-session", mapOf("name" to name))
        }

    // ── 压缩对话 ──
    suspend fun compact() =
        withContext(Dispatchers.IO) {
            post("/compact")
        }

    // ── 获取检查点 ──
    suspend fun getCheckpoints(): List<CheckpointInfo> =
        withContext(Dispatchers.IO) {
            val json = get("/checkpoints")
            if (json.isNullOrBlank()) return@withContext emptyList()
            try {
                gson.fromJson(json, object : TypeToken<List<CheckpointInfo>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }

    // ── 回退 ──
    suspend fun rewind(
        turn: Int,
        scope: String = "both",
    ) = withContext(Dispatchers.IO) {
        post("/rewind", mapOf("turn" to turn, "scope" to scope))
    }

    // ── 分叉 ──
    suspend fun fork(
        turn: Int,
        name: String = "",
    ) = withContext(Dispatchers.IO) {
        post("/fork", mapOf("turn" to turn, "name" to name))
    }

    // ── 总结 ──
    suspend fun summarize(
        turn: Int,
        mode: String,
    ) = withContext(Dispatchers.IO) {
        post("/summarize", mapOf("turn" to turn, "mode" to mode))
    }

    // ── 批准工具 ──
    suspend fun approve(
        id: String,
        allow: Boolean,
        session: Boolean = false,
        persist: Boolean = false,
        scope: String = "",
    ) = withContext(Dispatchers.IO) {
        post(
            "/approve",
            mapOf(
                "id" to id,
                "allow" to allow,
                "session" to session,
                "persist" to persist,
                "scope" to scope,
            ),
        )
    }

    // ── 回答提问卡片 ──
    suspend fun answer(
        id: String,
        answers: List<Map<String, Any>>,
    ) = withContext(Dispatchers.IO) {
        post("/answer", mapOf("id" to id, "answers" to answers))
    }

    // ── 计划模式 ──
    suspend fun setPlan(on: Boolean) =
        withContext(Dispatchers.IO) {
            post("/plan", mapOf("on" to on))
        }

    // ── 工具审批模式 ──
    suspend fun setToolApprovalMode(mode: String) =
        withContext(Dispatchers.IO) {
            post("/tool-approval-mode", mapOf("mode" to mode))
        }

    // ═══════════════════════════════════════════════
    // 内部 HTTP 辅助
    // ═══════════════════════════════════════════════

    private suspend fun get(path: String): String? {
        val builder =
            Request
                .Builder()
                .url("$baseUrl$path")
        authHeader?.let { builder.header("Authorization", it) }
        val request =
            builder
                .get()
                .build()
        return execute(request)
    }

    private suspend fun post(
        path: String,
        body: Any? = null,
    ) {
        val requestBody =
            if (body != null) {
                gson.toJson(body).toRequestBody(jsonMediaType)
            } else {
                "{}".toRequestBody(jsonMediaType)
            }
        val builder =
            Request
                .Builder()
                .url("$baseUrl$path")
        authHeader?.let { builder.header("Authorization", it) }
        val request =
            builder
                .post(requestBody)
                .build()
        execute(request)
    }

    private suspend fun execute(request: Request): String? =
        try {
            val response = client.newCall(request).execute()
            response.body?.string().also { response.close() }
        } catch (e: IOException) {
            null
        }
}

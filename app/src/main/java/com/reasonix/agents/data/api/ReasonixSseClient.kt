package com.reasonix.agents.data.api

import android.util.Base64
import com.google.gson.Gson
import com.reasonix.agents.data.model.ConnectionState
import com.reasonix.agents.data.model.SseEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SSE HTTP 层错误（非 2xx，如 404/401）。不重连，直接以该异常终止事件流，
 * 由上层（ChatViewModel）转为明确错误提示。
 */
class SseHttpException(val code: Int, url: String) :
    IOException("SSE 连接失败：HTTP $code（$url）")

/**
 * SSE 客户端 — 连接 /events 端点，实时接收服务端推送的消息流。
 *
 * 重连策略（批 2）：
 * - 网络层错误（连接超时 / DNS / 流中断）→ 指数退避自动重连（1s→2s→4s…上限 30s）
 * - HTTP 层错误（404/401 等非 2xx）→ 不重连，抛出 [SseHttpException]
 * - 重连成功（onOpen）→ 状态回到 [ConnectionState.CONNECTED]，上层据此补拉 /history 增量
 *
 * 通过 [connectionState] 暴露连接状态，驱动 Chat 顶栏绿/黄/红状态点。
 */
class ReasonixSseClient(
    private val baseUrl: String,
    private val credentials: Pair<String, String>? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()
    private var eventSource: EventSource? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Basic Auth 头；未配置凭据时为 null（向后兼容无认证直连场景） */
    private val authHeader: String? = credentials?.let { (u, p) ->
        "Basic " + Base64.encodeToString("$u:$p".toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 连接 SSE 并返回事件 Flow。
     * 调用方 collect 时自动连接，取消 collect 时自动断开。
     * 网络错误时由 retryWhen 指数退避自动重连；HTTP 错误直接以异常结束。
     */
    fun connect(): Flow<SseEvent> = callbackFlow {
        val active = AtomicBoolean(true)

        val builder = Request.Builder()
            .url("$baseUrl/events")
            .header("Accept", "text/event-stream")
        authHeader?.let { builder.header("Authorization", it) }
        val request = builder
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    val event = gson.fromJson(data, SseEvent::class.java)
                    trySend(event)
                } catch (_: Exception) {
                    // 解析失败则忽略
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                if (!active.get()) return
                val httpError = response != null && !response.isSuccessful
                _connectionState.value =
                    if (httpError) ConnectionState.DISCONNECTED
                    else ConnectionState.RECONNECTING
                if (httpError) {
                    close(SseHttpException(response.code, request.url.toString()))
                } else {
                    close(t ?: IOException("SSE 连接中断"))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                if (!active.get()) return
                // 服务端主动关闭连接 → 以异常结束本次订阅，交由 retryWhen 走重连
                _connectionState.value = ConnectionState.RECONNECTING
                close(IOException("SSE 连接已关闭"))
            }
        }

        val factory = EventSources.createFactory(client)
        eventSource = factory.newEventSource(request, listener)

        awaitClose {
            active.set(false)
            eventSource?.cancel()
            eventSource = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }.retryWhen { cause, attempt ->
        // HTTP 层错误（404/401 等）不重连；网络/流中断按指数退避重连，上限 30s
        if (cause is SseHttpException) {
            false
        } else {
            // attempt: 0→1s, 1→2s, 2→4s, 3→8s, 4→16s, ≥5→30s（封顶）
            val delayMs = (1000L shl attempt.coerceAtMost(5)).coerceAtMost(30_000L)
            delay(delayMs)
            true
        }
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

package com.reasonix.agents.data.repository

import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.api.ConnectResult
import com.reasonix.agents.data.api.ReasonixApi
import java.net.Proxy
import com.reasonix.agents.data.api.ReasonixSseClient
import com.reasonix.agents.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 仓库层 — 将 API、SSE 客户端整合为统一的聊天数据源。
 */
class ChatRepository(
    private val api: ReasonixApi,
    private val sseClient: ReasonixSseClient,
) {
    /** 连接参数（批 B-11/12）：认证方式 + REST 超时 + SSE 重连开关/退避上限 */
    data class ConnectionConfig(
        val auth: AuthInfo? = null,
        val connectTimeoutSec: Int = 30,
        val sseReconnectEnabled: Boolean = true,
        val sseReconnectMaxDelaySec: Int = 30,
        /** 网络代理（2026-08-13）：null=直连；非空时 OkHttp 经代理连接 */
        val proxy: Proxy? = null,
    )

    constructor(baseUrl: String, config: ConnectionConfig = ConnectionConfig()) : this(
        ReasonixApi(baseUrl, config.auth, config.connectTimeoutSec, proxy = config.proxy),
        ReasonixSseClient(
            baseUrl,
            config.auth,
            config.sseReconnectEnabled,
            config.sseReconnectMaxDelaySec * 1000L,
            proxy = config.proxy,
        ),
    )

    // ── SSE 事件流 ──
    fun sseEvents(): Flow<SseEvent> = sseClient.connect()

    /** SSE 连接状态（Connected/Reconnecting/Disconnected），驱动顶栏状态点 */
    fun sseConnectionState(): StateFlow<ConnectionState> = sseClient.connectionState

    fun disconnectSse() = sseClient.disconnect()

    // ── REST 接口 ──

    suspend fun submit(input: String) = api.submit(input)

    suspend fun cancel() = api.cancel()

    suspend fun getHistory(): List<HistoryMessage> = api.getHistory()

    suspend fun getTodos(): List<TodoItem> = api.getTodos()

    suspend fun getStatus(): StatusInfo? = api.getStatus()

    /** 连接诊断（批 A-1 错误分类提示） */
    suspend fun diagnose(): ConnectResult = api.diagnose()

    suspend fun getModels(): ModelsResponse? = api.getModels()

    suspend fun setModel(model: String) = api.setModel(model)

    suspend fun getSystemPrompt(): String? = api.getSystemPrompt()

    suspend fun getSessions(): List<SessionInfo> = api.getSessions()

    suspend fun newSession() = api.newSession()

    suspend fun resumeSession(path: String) = api.resumeSession(path)

    suspend fun deleteSession(name: String) = api.deleteSession(name)

    suspend fun compact() = api.compact()

    suspend fun getCheckpoints(): List<CheckpointInfo> = api.getCheckpoints()

    suspend fun rewind(
        turn: Int,
        scope: String = "both",
    ) = api.rewind(turn, scope)

    suspend fun fork(
        turn: Int,
        name: String = "",
    ) = api.fork(turn, name)

    suspend fun summarize(
        turn: Int,
        mode: String,
    ) = api.summarize(turn, mode)

    suspend fun approve(
        id: String,
        allow: Boolean,
        session: Boolean = false,
        persist: Boolean = false,
        scope: String = "",
    ) = api.approve(id, allow, session, persist, scope)

    suspend fun answer(
        id: String,
        answers: List<Map<String, Any>>,
    ) = api.answer(id, answers)

    suspend fun setPlan(on: Boolean) = api.setPlan(on)

    suspend fun setToolApprovalMode(mode: String) = api.setToolApprovalMode(mode)
}

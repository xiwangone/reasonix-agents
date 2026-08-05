package com.reasonix.agents.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.CustomModelStore
import com.reasonix.agents.data.PromptStore
import com.reasonix.agents.data.model.*
import com.reasonix.agents.data.repository.ChatRepository
import com.reasonix.agents.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatItem> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val status: StatusInfo? = null,
    val models: List<ModelInfo> = emptyList(),
    val currentModel: String = "",
    val systemPrompt: String? = null,
    val isStreaming: Boolean = false,
    val planMode: Boolean = false,
    val toolApprovalMode: String = "auto",
    val inputText: String = "",
    val serverUrl: String = "http://127.0.0.1:8920",
    val showSidebar: Boolean = false,
    val showRewindPicker: Boolean = false,
    val checkpoints: List<CheckpointInfo> = emptyList(),
    val showStatsDialog: Boolean = false,
    val settings: AppSettingsStore.Settings = AppSettingsStore.Settings(),
    val customModels: List<CustomModelStore.CustomModel> = emptyList(),
    val customPrompts: List<PromptStore.CustomPrompt> = emptyList(),
    val currentPromptId: String = "",
    val ciSettings: com.reasonix.agents.data.CiMonitorStore.CiSettings = com.reasonix.agents.data.CiMonitorStore.CiSettings(),
    val cumulativeTokens: Long = 0,
    val cumulativeCost: Double = 0.0,
    val cumulativeCacheHit: Long = 0,
    val cumulativeCacheMiss: Long = 0,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val error: String? = null
)

class ChatViewModel(
    application: Application,
    initialServerUrl: String = "http://127.0.0.1:8920",
    initialAuth: AuthInfo? = null,
    /**
     * 批 C-7：登录/连接成功进入主界面时默认新建会话。
     * true（默认）：初始加载时先调用服务端 newSession，且不加载上次会话的 messages；
     * false：恢复上次会话（切换服务器配置等场景保持原行为）。
     */
    private val freshSession: Boolean = true
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState(serverUrl = initialServerUrl))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** 当前认证信息（重建 repository 时复用，批 A-4/B-12）。 */
    private var currentAuth: AuthInfo? = initialAuth

    private var repository: ChatRepository = createRepository(initialServerUrl, initialAuth)
    private var sseCollectionJob: Job? = null
    private var connectionStateJob: Job? = null

    // 最近一次收到 SSE 事件的时间戳（重连后判定流是否还活着）
    private var lastSseEventAt: Long = 0L

    // 当前流的助手消息 builder（增量）
    private var currentAssistantMsgIndex: Int? = null
    private var pendingReasoning: StringBuilder? = null
    private var pendingContent: StringBuilder? = null
    private var pendingToolCards: MutableMap<String, ChatItem.ToolCard> = mutableMapOf()

    // 双 Esc 倒带
    private var lastEscTime: Long = 0L
    private val doubleEscWindowMs: Long = 600

    init {
        // 加载本地持久化的应用设置（主题/超时/重连等，批 A-2/B-11）
        _uiState.update { it.copy(settings = AppSettingsStore.load(getApplication())) }
        // 第四批：加载用户自定义提示词与当前选中项
        _uiState.update {
            it.copy(
                customPrompts = PromptStore.load(getApplication()),
                currentPromptId = PromptStore.getCurrentId(getApplication())
            )
        }
        // 批 C-7：登录/连接成功进入主界面时默认新建会话（不恢复上次会话）
        loadInitialData(freshSession = freshSession)
        collectConnectionState()
    }

    // ── 服务器配置 ──

    /** 动态切换服务器地址，重建 API/SSE 客户端并重新加载数据。 */
    fun configureServer(url: String, auth: AuthInfo? = null) {
        val normalized = url.trimEnd('/')
        currentAuth = auth
        repository = createRepository(normalized, auth)
        _uiState.update { it.copy(serverUrl = normalized, messages = emptyList(), error = null) }
        loadInitialData()
        collectConnectionState()
    }

    /** 按当前设置（连接超时 / SSE 重连开关与退避上限，批 B-11）构建 repository。 */
    private fun createRepository(url: String, auth: AuthInfo? = null): ChatRepository {
        val s = _uiState.value.settings
        return ChatRepository(
            url,
            ChatRepository.ConnectionConfig(
                auth = auth,
                connectTimeoutSec = s.connectTimeoutSec,
                sseReconnectEnabled = s.sseReconnectEnabled,
                sseReconnectMaxDelaySec = s.sseReconnectMaxDelaySec
            )
        )
    }

    // ── 初始化 ──

    private fun loadInitialData(freshSession: Boolean = false) {
        viewModelScope.launch {
            // 批 C-7：登录后新建会话——先让服务端切到新会话，再加载数据
            if (freshSession) {
                try {
                    repository.newSession()
                } catch (e: Exception) {
                    // 新建失败不阻塞其余数据加载（服务端可能不支持）
                }
            }
            val sessions = repository.getSessions()
            val status = repository.getStatus()
            // 批 C-7：新建会话时初始消息列表为空，不加载上次的 messages
            val history = if (freshSession) emptyList() else repository.getHistory()
            val modelsResp = repository.getModels()
            val systemPrompt = repository.getSystemPrompt()
            val todos = repository.getTodos()

            val historyItems = buildHistoryItems(history)

            _uiState.update {
                it.copy(
                    sessions = sessions,
                    todos = todos,
                    status = status,
                    models = modelsResp?.models ?: emptyList(),
                    currentModel = modelsResp?.current ?: status?.label ?: "",
                    systemPrompt = systemPrompt,
                    messages = historyItems,
                    planMode = status?.plan ?: false,
                    toolApprovalMode = status?.toolApprovalMode ?: "auto",
                    customModels = CustomModelStore.load(getApplication())
                )
            }
        }
    }

    // ── 切换模型 ──
    fun setModel(model: String) {
        val customNames = CustomModelStore.load(getApplication()).map { it.name }.toSet()
        if (model in customNames) {
            // 自定义模型（批 B-9）：本地记忆当前选择，服务端不识别故不 POST
            CustomModelStore.setCurrent(getApplication(), model)
            _uiState.update { it.copy(currentModel = model) }
            return
        }
        viewModelScope.launch {
            repository.setModel(model)
            // 刷新状态与模型列表
            val status = repository.getStatus()
            val modelsResp = repository.getModels()
            _uiState.update {
                it.copy(
                    status = status,
                    currentModel = modelsResp?.current ?: status?.label ?: model
                )
            }
        }
    }

    // ── 自定义模型管理（批 B-9）──

    fun addCustomModel(model: CustomModelStore.CustomModel) {
        CustomModelStore.add(getApplication(), model)
        _uiState.update {
            it.copy(customModels = CustomModelStore.load(getApplication()))
        }
    }

    fun removeCustomModel(id: String) {
        CustomModelStore.remove(getApplication(), id)
        _uiState.update {
            it.copy(customModels = CustomModelStore.load(getApplication()))
        }
    }

    // ── 自定义提示词管理（第四批：提示词功能）──

    /** 添加一条用户提示词；select=true 时保存并立即选中生效。 */
    fun addPrompt(content: String, select: Boolean = false) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        if (_uiState.value.customPrompts.size >= PromptStore.MAX_PROMPTS) return
        val id = java.util.UUID.randomUUID().toString()
        val updated = PromptStore.add(
            getApplication(),
            PromptStore.CustomPrompt(id = id, content = trimmed, createdAt = System.currentTimeMillis())
        )
        var currentId = _uiState.value.currentPromptId
        if (select) {
            currentId = id
            PromptStore.setCurrentId(getApplication(), id)
        }
        _uiState.update { it.copy(customPrompts = updated, currentPromptId = currentId) }
    }

    /** 删除提示词；若删除的是当前选中项则同步清除选中状态。 */
    fun removePrompt(id: String) {
        val updated = PromptStore.remove(getApplication(), id)
        val currentId = if (_uiState.value.currentPromptId == id) "" else _uiState.value.currentPromptId
        if (currentId != _uiState.value.currentPromptId) {
            PromptStore.setCurrentId(getApplication(), currentId)
        }
        _uiState.update { it.copy(customPrompts = updated, currentPromptId = currentId) }
    }

    /** 切换选中/取消选中某条提示词（单选：选中一条时其它自动取消）。 */
    fun setCurrentPrompt(id: String) {
        val currentId = if (_uiState.value.currentPromptId == id) "" else id
        PromptStore.setCurrentId(getApplication(), currentId)
        _uiState.update { it.copy(currentPromptId = currentId) }
    }

    /** 当前选中的提示词内容（发送消息时注入会话上下文）。 */
    fun activePromptContent(): String =
        _uiState.value.customPrompts
            .firstOrNull { it.id == _uiState.value.currentPromptId }
            ?.content?.trim() ?: ""

    // ── 更新 CI 监控设置 ──
    fun updateCiSettings(s: com.reasonix.agents.data.CiMonitorStore.CiSettings) {
        _uiState.update { it.copy(ciSettings = s) }
    }

    // ── 刷新模型列表 ──
    fun reloadModels() {
        viewModelScope.launch {
            val modelsResp = repository.getModels()
            _uiState.update { it.copy(models = modelsResp?.models ?: emptyList()) }
        }
    }

    /** 将后端历史记录转换为 ChatItem 列表，含工具调用/结果配对。 */
    private fun buildHistoryItems(history: List<HistoryMessage>): List<ChatItem> {
        val toolResults = history
            .filter { it.role == "tool" && it.toolCallId != null }
            .associateBy { it.toolCallId!! }

        return history.flatMap { hist ->
            when (hist.role) {
                "user" -> listOf(ChatItem.UserMessage(hist.content ?: ""))
                "assistant" -> {
                    val items = mutableListOf<ChatItem>()
                    hist.toolCalls?.forEach { tc ->
                        val result = toolResults[tc.id]
                        val isRunning = result == null
                        items.add(
                            ChatItem.ToolCard(
                                id = tc.id,
                                name = tc.name,
                                args = tc.arguments,
                                output = result?.content,
                                isRunning = isRunning,
                                expanded = !isRunning
                            )
                        )
                    }
                    val content = hist.content.orEmpty()
                    val reasoning = hist.reasoning
                    if (content.isNotBlank() || reasoning != null) {
                        items.add(
                            ChatItem.AssistantMessage(
                                content = content,
                                reasoning = reasoning
                            )
                        )
                    }
                    items
                }
                "system" -> listOf(ChatItem.SystemNotice(hist.content ?: ""))
                else -> emptyList()
            }
        }
    }

    // ── 输入 ──

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
    }

    // ── 设置页（批 A-2/A-5/B-11：全量设置，统一保存）──

    /** 更新全部应用设置；连接相关参数（超时/重连）变化时重建 repository 使配置生效。 */
    fun updateSettings(s: AppSettingsStore.Settings) {
        val old = _uiState.value.settings
        _uiState.update { it.copy(settings = s) }
        AppSettingsStore.save(getApplication(), s)
        val connectionChanged = old.connectTimeoutSec != s.connectTimeoutSec ||
            old.sseReconnectEnabled != s.sseReconnectEnabled ||
            old.sseReconnectMaxDelaySec != s.sseReconnectMaxDelaySec
        if (connectionChanged && _uiState.value.serverUrl.isNotBlank()) {
            repository = createRepository(_uiState.value.serverUrl, currentAuth)
            collectConnectionState()
        }
    }

    // ── 发送消息 ──

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        _uiState.update {
            it.copy(
                inputText = "",
                isStreaming = true,
                error = null
            )
        }

        // Slash 命令解析
        if (parseSlashCommand(text)) {
            _uiState.update { it.copy(isStreaming = false) }
            return
        }

        // 添加用户消息
        appendMessage(ChatItem.UserMessage(text))

        // 初始化流式缓冲区
        currentAssistantMsgIndex = null
        pendingContent = StringBuilder()
        pendingReasoning = StringBuilder()
        pendingToolCards.clear()

        // 提交消息 → 启动 SSE 监听
        // 第四批：选中用户提示词时，附加在系统提示词之后注入会话上下文
        val promptContent = activePromptContent()
        val effectiveInput = if (promptContent.isNotBlank()) "$promptContent\n\n$text" else text
        viewModelScope.launch {
            try {
                repository.submit(effectiveInput)
                startSseCollection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendMessage(ChatItem.ErrorMessage("提交失败: ${e.message}"))
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    /** 解析并执行斜杠命令。返回 true 表示已处理（不发往服务器）。 */
    private fun parseSlashCommand(text: String): Boolean {
        val cmd = text.trim()
        when {
            cmd == "/help" -> {
                appendMessage(ChatItem.SystemNotice("可用命令: /plan /yolo /auto /compact /compact auto /compact manual /rewind /fork /new /stats /status /sessions /summarize /resume /delete /theme /help"))
                return true
            }
            cmd == "/plan" -> {
                togglePlanMode()
                return true
            }
            cmd == "/yolo" -> {
                setToolApprovalMode("yolo")
                appendMessage(ChatItem.SystemNotice("已切换到 YOLO 模式"))
                return true
            }
            cmd == "/auto" -> {
                setToolApprovalMode("auto")
                appendMessage(ChatItem.SystemNotice("已切换到自动审批模式"))
                return true
            }
            cmd == "/compact" -> {
                compactConversation()
                return true
            }
            cmd == "/rewind" -> {
                showRewindPicker()
                return true
            }
            cmd == "/fork" -> {
                viewModelScope.launch {
                    repository.fork(1)
                    loadInitialData()
                }
                return true
            }
            cmd == "/new" -> {
                newSession()
                return true
            }
            cmd == "/stats" -> {
                _uiState.update { it.copy(showStatsDialog = true) }
                return true
            }
            cmd == "/status" -> {
                viewModelScope.launch {
                    val s = repository.getStatus()
                    appendMessage(ChatItem.SystemNotice(
                        "模型=${s?.label ?: "?"} 计划=${s?.plan ?: false} 模式=${s?.toolApprovalMode ?: "auto"} " +
                        "Token=${s?.used ?: 0}/${s?.window ?: 0}"
                    ))
                }
                return true
            }
            cmd == "/sessions" -> {
                viewModelScope.launch {
                    val s = repository.getSessions()
                    val list = s.joinToString("\n") { "- ${it.name}${if (it.current) " (当前)" else ""}" }
                    appendMessage(ChatItem.SystemNotice(list.ifBlank { "无会话" }))
                }
                return true
            }
            cmd.startsWith("/summarize") -> {
                viewModelScope.launch {
                    repository.summarize(1, "from")
                    appendMessage(ChatItem.SystemNotice("正在总结…"))
                }
                return true
            }
            cmd.startsWith("/delete ") -> {
                val name = cmd.removePrefix("/delete ").trim()
                viewModelScope.launch {
                    repository.deleteSession(name)
                    loadInitialData()
                    appendMessage(ChatItem.SystemNotice("已删除会话: $name"))
                }
                return true
            }
            cmd.startsWith("/resume ") -> {
                val name = cmd.removePrefix("/resume ").trim()
                viewModelScope.launch {
                    repository.resumeSession(name)
                    loadInitialData()
                    appendMessage(ChatItem.SystemNotice("已恢复会话: $name"))
                }
                return true
            }
            cmd == "/compact auto" -> {
                appendMessage(ChatItem.SystemNotice("已设置压缩模式: 自动"))
                return true
            }
            cmd == "/compact manual" -> {
                appendMessage(ChatItem.SystemNotice("已设置压缩模式: 手动"))
                return true
            }
            cmd == "/theme" -> {
                appendMessage(ChatItem.SystemNotice("主题切换功能开发中…"))
                return true
            }
        }
        return false
    }

    // ── SSE 事件处理 ──

    private fun startSseCollection() {
        sseCollectionJob?.cancel()
        sseCollectionJob = viewModelScope.launch {
            try {
                repository.sseEvents().collect { event ->
                    handleSseEvent(event)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.message?.contains("closed") != true) {
                    appendMessage(ChatItem.ErrorMessage("连接错误: ${e.message}"))
                }
            } finally {
                finalizeTurn()
            }
        }
    }

    // ── 任务清单（Todo 面板） ──

    fun loadTodos() {
        viewModelScope.launch {
            val todos = repository.getTodos()
            _uiState.update { it.copy(todos = todos) }
        }
    }

    // ── 连接状态收集 ──

    /**
     * 收集 SSE 连接状态（驱动顶栏绿/黄/红状态点）。
     * 检测「重连成功」（RECONNECTING → CONNECTED 上升沿）：补拉 /history 增量合并，
     * 避免断线期间产生的消息丢失。
     */
    private fun collectConnectionState() {
        connectionStateJob?.cancel()
        connectionStateJob = viewModelScope.launch {
            var prev = ConnectionState.DISCONNECTED
            repository.sseConnectionState().collect { s ->
                val reconnected = prev == ConnectionState.RECONNECTING && s == ConnectionState.CONNECTED
                prev = s
                _uiState.update { it.copy(connectionState = s) }
                if (reconnected) {
                    syncHistoryAfterReconnect()
                }
            }
        }
    }

    /** 重连成功后：拉 /history 重建消息 + 重置流式缓冲 + 卡流保险收尾 */
    private fun syncHistoryAfterReconnect() {
        val reconnectedAt = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val history = repository.getHistory()
                rebuildFromHistory(history)
            } catch (e: Exception) {
                appendMessage(ChatItem.ErrorMessage("重连后同步历史失败: ${e.message}"))
            }
            // 重置流式缓冲：后续事件（若有）从干净状态重新累积
            currentAssistantMsgIndex = null
            pendingContent = null
            pendingReasoning = null
            pendingToolCards.clear()
            // 保险：重连后若 5s 内无任何新 SSE 事件且仍显示流式中，
            // 说明服务端 turn 已随断线结束（turn_done 已丢失）→ 收尾
            if (_uiState.value.isStreaming) {
                viewModelScope.launch {
                    delay(5_000)
                    if (lastSseEventAt <= reconnectedAt && _uiState.value.isStreaming) {
                        finalizeTurn()
                    }
                }
            }
        }
    }

    private fun handleSseEvent(event: SseEvent) {
        lastSseEventAt = System.currentTimeMillis()
        when (event.kind) {
            "turn_started" -> {
                currentAssistantMsgIndex = null
                pendingContent = StringBuilder()
                pendingReasoning = StringBuilder()
                pendingToolCards.clear()
                loadTodos()
            }

            "reasoning" -> {
                event.reasoning?.let { r ->
                    pendingReasoning?.append(r)
                    updatePendingAssistant()
                }
            }

            "text" -> {
                event.text?.let { t ->
                    pendingContent?.append(t)
                    updatePendingAssistant()
                }
            }

            "message" -> {
                event.message?.let { msg ->
                    pendingContent = StringBuilder(msg.content ?: "")
                    pendingReasoning = if (msg.reasoning != null) StringBuilder(msg.reasoning) else null
                    updatePendingAssistant()
                }
            }

            "tool_dispatch" -> {
                event.tool?.let { tool ->
                    val card = ChatItem.ToolCard(
                        id = tool.id,
                        name = tool.name,
                        args = tool.args ?: tool.arguments,
                        isRunning = true
                    )
                    pendingToolCards[tool.id] = card
                    appendMessage(card)
                }
            }

            "tool_result" -> {
                event.tool?.let { tool ->
                    val card = ChatItem.ToolCard(
                        id = tool.id,
                        name = tool.name,
                        output = tool.output,
                        err = tool.err,
                        truncated = tool.truncated,
                        isRunning = false
                    )
                    pendingToolCards[tool.id] = card
                    replaceToolCard(tool.id, card)
                    loadTodos()
                }
            }

            "tool_progress" -> {
                event.tool?.let { tool ->
                    val card = ChatItem.ToolCard(
                        id = tool.id,
                        name = tool.name,
                        output = tool.output,
                        isRunning = true
                    )
                    pendingToolCards[tool.id] = card
                    replaceToolCard(tool.id, card)
                }
            }

            "usage" -> {
                event.usage?.let { u ->
                    appendMessage(ChatItem.UsageStats(u))
                    _uiState.update { state ->
                        state.copy(
                            cumulativeTokens = state.cumulativeTokens + u.totalTokens,
                            cumulativeCost = state.cumulativeCost + (u.costUsd ?: u.cost ?: 0.0),
                            cumulativeCacheHit = state.cumulativeCacheHit + u.cacheHitTokens,
                            cumulativeCacheMiss = state.cumulativeCacheMiss + u.cacheMissTokens
                        )
                    }
                }
            }

            "notice" -> {
                val text = event.text ?: event.err ?: return
                val isWarning = event.level == "warning"
                appendMessage(ChatItem.SystemNotice(text, isWarning))
            }

            "phase" -> {
                event.text?.let { txt ->
                    appendMessage(ChatItem.PhaseIndicator(txt))
                }
            }

            "approval_request" -> {
                event.approval?.let { a ->
                    appendMessage(ChatItem.ApprovalCard(a.id, a.tool, a.subject))
                }
            }

            "ask_request" -> {
                event.ask?.let { a ->
                    appendMessage(ChatItem.AskCard(a.id, a.questions))
                }
            }

            "compaction_started", "compaction_done" -> {
                event.compaction?.let { c ->
                    appendMessage(ChatItem.CompactionNotice(c.trigger, c.summary, c.messages))
                }
                if (event.kind == "compaction_done") {
                    viewModelScope.launch {
                        val history = repository.getHistory()
                        rebuildFromHistory(history)
                    }
                }
            }

            "turn_done" -> {
                finalizeTurn()
                loadTodos()
            }
        }
    }

    private fun updatePendingAssistant() {
        val content = pendingContent?.toString() ?: ""
        val reasoning = pendingReasoning?.toString()?.takeIf { it.isNotBlank() }

        val msg = ChatItem.AssistantMessage(
            content = content,
            reasoning = reasoning
        )

        val idx = currentAssistantMsgIndex
        if (idx != null) {
            _uiState.update { state ->
                if (idx < state.messages.size && state.messages[idx] is ChatItem.AssistantMessage) {
                    val updated = state.messages.toMutableList()
                    updated[idx] = msg
                    state.copy(messages = updated)
                } else {
                    // 索引失效（如 compaction 重建），回退到追加
                    val newList = state.messages + msg
                    currentAssistantMsgIndex = newList.size - 1
                    state.copy(messages = newList)
                }
            }
        } else {
            // 本轮首次助手消息 — 追加到列表末尾
            _uiState.update { state ->
                val newList = state.messages + msg
                currentAssistantMsgIndex = newList.size - 1
                state.copy(messages = newList)
            }
        }
    }

    private fun appendMessage(item: ChatItem) {
        _uiState.update { state ->
            state.copy(messages = state.messages + item)
        }
    }

    private fun replaceToolCard(id: String, card: ChatItem.ToolCard) {
        _uiState.update { state ->
            val list = state.messages.toMutableList()
            val idx = list.indexOfLast {
                it is ChatItem.ToolCard && it.id == id
            }
            if (idx >= 0) list[idx] = card
            state.copy(messages = list)
        }
    }

    private fun rebuildFromHistory(history: List<HistoryMessage>) {
        currentAssistantMsgIndex = null
        val items = buildHistoryItems(history)
        _uiState.update { it.copy(messages = items) }
    }

    private fun finalizeTurn() {
        currentAssistantMsgIndex = null
        pendingContent = null
        pendingReasoning = null
        _uiState.update { it.copy(isStreaming = false) }
        sseCollectionJob?.cancel()
        // 批 B-14：多步任务跑完 → 系统通知栏提醒
        if (pendingToolCards.isNotEmpty()) {
            val toolNames = pendingToolCards.values.map { it.name }.filter { it.isNotBlank() }.distinct()
            val summary = when {
                toolNames.isEmpty() -> "已完成一轮多步任务"
                toolNames.size <= 3 -> "已完成 ${toolNames.size} 个工具步骤：${toolNames.joinToString("、")}"
                else -> "已完成 ${toolNames.size} 个工具步骤（${toolNames.take(3).joinToString("、")} 等）"
            }
            NotificationHelper.notifyTaskDone(getApplication(), summary)
        }
        pendingToolCards.clear()
    }

    // ── 倒带 / 分叉 / 总结 ──

    fun showRewindPicker() {
        viewModelScope.launch {
            try {
                val checkpoints = repository.getCheckpoints()
                _uiState.update { it.copy(checkpoints = checkpoints, showRewindPicker = true) }
            } catch (e: Exception) {
                appendMessage(ChatItem.ErrorMessage("加载检查点失败: ${e.message}"))
            }
        }
    }

    fun dismissRewindPicker() {
        _uiState.update { it.copy(showRewindPicker = false) }
    }

    fun rewindTo(turn: Int, scope: String = "both") {
        viewModelScope.launch {
            repository.rewind(turn, scope)
            loadInitialData()
            _uiState.update { it.copy(showRewindPicker = false) }
        }
    }

    fun forkAt(turn: Int) {
        viewModelScope.launch {
            repository.fork(turn)
            loadInitialData()
            _uiState.update { it.copy(showRewindPicker = false) }
        }
    }

    fun summarizeAt(turn: Int, mode: String) {
        viewModelScope.launch {
            repository.summarize(turn, mode)
            _uiState.update { it.copy(showRewindPicker = false) }
            appendMessage(ChatItem.SystemNotice("正在从第 $turn 轮总结…"))
        }
    }

    // ── 统计对话框 ──

    fun showStatsDialog() {
        _uiState.update { it.copy(showStatsDialog = true) }
    }

    fun dismissStatsDialog() {
        _uiState.update { it.copy(showStatsDialog = false) }
    }

    // ── 双 Esc 倒带 ──

    fun tryDoubleEscRewind(): Boolean {
        if (_uiState.value.isStreaming || _uiState.value.inputText.isNotEmpty()) {
            return false
        }
        val now = System.currentTimeMillis()
        val within = (now - lastEscTime) < doubleEscWindowMs
        lastEscTime = now
        if (within) {
            showRewindPicker()
            return true
        }
        return false
    }

    // ── 操作 ──

    fun cancelStreaming() {
        viewModelScope.launch {
            repository.cancel()
            finalizeTurn()
        }
    }

    fun togglePlanMode() {
        val newPlan = !_uiState.value.planMode
        _uiState.update { it.copy(planMode = newPlan) }
        viewModelScope.launch { repository.setPlan(newPlan) }
    }

    fun setToolApprovalMode(mode: String) {
        _uiState.update { it.copy(toolApprovalMode = mode) }
        viewModelScope.launch { repository.setToolApprovalMode(mode) }
    }

    fun approveTool(id: String, session: Boolean = false, persist: Boolean = false, scope: String = "") {
        viewModelScope.launch {
            repository.approve(id, true, session, persist, scope)
            _uiState.update { state ->
                state.copy(messages = state.messages.filter {
                    it !is ChatItem.ApprovalCard || it.id != id
                })
            }
        }
    }

    fun denyTool(id: String) {
        viewModelScope.launch {
            repository.approve(id, false)
            _uiState.update { state ->
                state.copy(messages = state.messages.filter {
                    it !is ChatItem.ApprovalCard || it.id != id
                })
            }
        }
    }

    fun submitAskAnswers(id: String, answers: List<Map<String, String>>) {
        val formattedAnswers = answers.map { map ->
            map.mapValues { it.value }.toMap()
        }.map { it.mapValues { e -> e.value as Any } }

        viewModelScope.launch {
            repository.answer(id, formattedAnswers)
            _uiState.update { state ->
                state.copy(messages = state.messages.filter {
                    it !is ChatItem.AskCard || it.id != id
                })
            }
        }
    }

    fun newSession() {
        viewModelScope.launch {
            repository.newSession()
            _uiState.update { it.copy(messages = emptyList()) }
            loadInitialData()
        }
    }

    fun selectSession(path: String) {
        viewModelScope.launch {
            repository.resumeSession(path)
            loadInitialData()
        }
    }

    fun deleteSession(name: String) {
        viewModelScope.launch {
            repository.deleteSession(name)
            loadInitialData()
        }
    }

    fun compactConversation() {
        viewModelScope.launch {
            repository.compact()
        }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(showSidebar = !it.showSidebar) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectSse()
    }

    /**
     * ViewModel 工厂（批 A-4/B-12）：注入 Application + 初始服务器地址 + 认证信息。
     * 由 MainActivity 在 ReasonixApp 中创建 ChatViewModel 时使用。
     */
    class Factory(
        private val app: Application,
        private val serverUrl: String,
        private val auth: AuthInfo?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(app, serverUrl, auth) as T
        }
    }
}

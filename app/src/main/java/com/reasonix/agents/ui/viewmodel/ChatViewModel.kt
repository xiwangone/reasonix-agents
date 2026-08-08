package com.reasonix.agents.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.BackupManager
import com.reasonix.agents.data.CliIntegrationStore
import com.reasonix.agents.data.CustomModelStore
import com.reasonix.agents.data.MemoryStore
import com.reasonix.agents.data.PromptStore
import com.reasonix.agents.data.ServerConfigStore
import com.reasonix.agents.data.model.*
import com.reasonix.agents.data.repository.ChatRepository
import com.reasonix.agents.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** 忙时排队发送的最大消息条数（超过提示「正忙，请稍后」） */
const val MAX_PENDING_MESSAGES = 5

data class ChatUiState(
    val messages: List<ChatItem> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val status: StatusInfo? = null,
    val models: List<ModelInfo> = emptyList(),
    val currentModel: String = "",
    val systemPrompt: String? = null,
    // 2026-08-07：注入上下文——当前记忆注入文本（供 UI 查看/编辑）
    val memoryText: String? = null,
    val isStreaming: Boolean = false,
    /** 忙时排队待发送的消息（AI 空闲后依次自动发送，最多 [MAX_PENDING_MESSAGES] 条） */
    val pendingMessages: List<String> = emptyList(),
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
    val ciSettings: com.reasonix.agents.data.CiMonitorStore.CiSettings =
        com.reasonix.agents.data.CiMonitorStore
            .CiSettings(),
    val cliSettings: com.reasonix.agents.data.CliIntegrationStore.CliSettings =
        com.reasonix.agents.data.CliIntegrationStore
            .CliSettings(),
    val cumulativeTokens: Long = 0,
    val cumulativePromptTokens: Long = 0,
    val cumulativeCompletionTokens: Long = 0,
    val cumulativeCost: Double = 0.0,
    val cumulativeCacheHit: Long = 0,
    val cumulativeCacheMiss: Long = 0,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val error: String? = null,
    /** 正在删除的会话名称集合（乐观移除 + 加载指示） */
    val deletingSessions: Set<String> = emptySet(),
    val pendingTimeoutHit: Boolean = false,  // 2026-08-08: 排队超时标记
)

/** 备份导出结果（第五批 E-1）：json 为生成的备份文件内容，失败时 error 非空。 */
data class BackupExportResult(
    val json: String? = null,
    val sessionCount: Int = 0,
    val serverCount: Int = 0,
    val error: String? = null,
)

/** 备份导入结果（第五批 E-1）：success=false 时 message 为失败原因（含凭据解密失败提示）。 */
data class BackupImportResult(
    val success: Boolean,
    val message: String,
    val restoredSettings: AppSettingsStore.Settings? = null,
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
    private val freshSession: Boolean = false,  // 2026-08-08: 默认恢复上次会话+历史，不再强制新建
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState(serverUrl = initialServerUrl))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** 当前认证信息（重建 repository 时复用，批 A-4/B-12）。 */
    private var currentAuth: AuthInfo? = initialAuth

    private var repository: ChatRepository = createRepository(initialServerUrl, initialAuth)
    private var sseCollectionJob: Job? = null
    private var connectionStateJob: Job? = null

    // 最近一次收到 SSE 事件的时间戳（重连后判定流是否还活着）
    // 2026-08-08: 排队超时检测 Job
    private var pendingTimeoutJob: Job? = null

    private var lastSseEventAt: Long = 0L

    // 当前流的助手消息 builder（增量）
    private var currentAssistantMsgIndex: Int? = null
    // 2026-08-06 对齐 RikkaHub 层次结构：一轮助手回复 = 有序块序列（推理/正文/工具按到达顺序）
    private var pendingBlocks: MutableList<TurnBlock> = mutableListOf()
    // 推理/正文累积缓冲（用于记忆解析 + 块合并）
    private var pendingReasoning: StringBuilder? = null
    private var pendingContent: StringBuilder? = null
    // 2026-08-07：本轮 usage 快照（结束时追加一张汇总卡，不随事件反复插入）
    private var pendingUsage: UsagePayload? = null
    // 2026-08-07：流式 UI 刷新节流 job——高频 delta 事件合并成一次重组，避免每 token 全量重建消息列表
    private var uiRefreshJob: Job? = null
    // 2026-08-07：turn_done 兜底收尾 job——多 turn 合并模型下 turn_done 不立即 finalize，
    // 无新内容事件才收尾（防服务端长连接不关流导致消息永不落定）。
    // 注意：只跟踪「内容事件」（text/reasoning/tool/usage/turn_started 等），
    // notice/phase 等通知类事件不会顺延收尾，避免回复完成被无限推迟。
    private var turnFinalizeJob: Job? = null
    private var lastContentEventAt: Long = 0L

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
                currentPromptId = PromptStore.getCurrentId(getApplication()),
                memoryText = MemoryStore.activeMemoriesText(getApplication(), currentSessionKey()),
            )
        }
        // 第五批 E-3：加载 CLI 集成设置（默认关闭）
        _uiState.update { it.copy(cliSettings = CliIntegrationStore.load(getApplication())) }
        // 批 C-7：登录/连接成功进入主界面时默认新建会话（不恢复上次会话）
        loadInitialData(freshSession = freshSession)
        collectConnectionState()
    }

    // ── 服务器配置 ──

    /** 动态切换服务器地址，重建 API/SSE 客户端并重新加载数据。 */
    fun configureServer(
        url: String,
        auth: AuthInfo? = null,
    ) {
        val normalized = url.trimEnd('/')
        currentAuth = auth
        repository = createRepository(normalized, auth)
        _uiState.update { it.copy(serverUrl = normalized, messages = emptyList(), error = null) }
        loadInitialData()
        collectConnectionState()
    }

    /** 按当前设置（连接超时 / SSE 重连开关与退避上限，批 B-11）构建 repository。 */
    private fun createRepository(
        url: String,
        auth: AuthInfo? = null,
    ): ChatRepository {
        val s = _uiState.value.settings
        return ChatRepository(
            url,
            ChatRepository.ConnectionConfig(
                auth = auth,
                connectTimeoutSec = s.connectTimeoutSec,
                sseReconnectEnabled = s.sseReconnectEnabled,
                sseReconnectMaxDelaySec = s.sseReconnectMaxDelaySec,
            ),
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
                    cumulativeTokens = status?.used ?: 0,
                    cumulativeCacheHit = status?.cacheHit ?: 0,
                    cumulativeCacheMiss = status?.cacheMiss ?: 0,
                    cumulativeCost = status?.lastUsage?.totalCost ?: status?.lastUsage?.cost ?: status?.lastUsage?.costUsd ?: 0.0,
                    planMode = status?.plan ?: false,
                    toolApprovalMode = status?.toolApprovalMode ?: "auto",
                    customModels = CustomModelStore.load(getApplication()),
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
                    currentModel = modelsResp?.current ?: status?.label ?: model,
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
    fun addPrompt(
        content: String,
        select: Boolean = false,
    ) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        if (_uiState.value.customPrompts.size >= PromptStore.MAX_PROMPTS) return
        val id =
            java.util.UUID
                .randomUUID()
                .toString()
        val updated =
            PromptStore.add(
                getApplication(),
                PromptStore.CustomPrompt(id = id, content = trimmed, createdAt = System.currentTimeMillis()),
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
            ?.content
            ?.trim() ?: ""

    // ── 2026-08-07：注入上下文编辑（供「注入上下文」折叠卡使用）──

    /** 保存当前选中用户提示词的新内容（保存到 PromptStore 并更新状态）。 */
    fun saveActivePrompt(content: String) {
        val currentId = _uiState.value.currentPromptId
        if (currentId.isBlank()) return
        val updated =
            _uiState.value.customPrompts.map {
                if (it.id == currentId) it.copy(content = content.trim()) else it
            }
        PromptStore.save(getApplication(), updated)
        _uiState.update { it.copy(customPrompts = updated) }
    }

    /** 当前会话记忆 key：sessions 里 current 的 path；无则 null（=全局互通）。 */
    private fun currentSessionKey(): String? =
        _uiState.value.sessions.firstOrNull { it.current }?.path

    /** UI 层取当前会话 key（记忆模式对话框用）。 */
    fun currentSessionKeyForUi(): String? = currentSessionKey()

    /** 刷新 memoryText（记忆模式切换后调用）。 */
    fun refreshMemoryText() {
        val context: android.content.Context = getApplication()
        val text = MemoryStore.activeMemoriesText(context, currentSessionKey())
        _uiState.update { it.copy(memoryText = text) }
    }

    /** 保存记忆注入文本（单条替换：编辑内容即新的唯一记忆；空则清空）。 */
    fun saveMemoryText(content: String) {
        val context: android.content.Context = getApplication()
        val sessionKey = currentSessionKey()
        val memoryKey =
            when (MemoryStore.memoryMode(context, sessionKey)) {
                MemoryStore.MemoryMode.GLOBAL -> null
                MemoryStore.MemoryMode.LOCAL -> sessionKey
                MemoryStore.MemoryMode.OFF -> null
            }
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            MemoryStore.save(context, emptyList(), memoryKey)
        } else {
            MemoryStore.save(
                context,
                listOf(
                    MemoryStore.MemoryItem(
                        id = "manual-${System.currentTimeMillis()}",
                        content = trimmed,
                        createdAt = System.currentTimeMillis(),
                    ),
                ),
                memoryKey,
            )
        }
        _uiState.update { it.copy(memoryText = MemoryStore.activeMemoriesText(context, sessionKey)) }
    }

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
        // 2026-08-06：修复同 id 多次调用历史丢失——按「出现顺序」配对（队列），
        // 同一 toolCallId 多次调用各配对到对应结果，不因 associateBy 覆盖而丢记录。
        val toolResultsByCall =
            history
                .filter { it.role == "tool" && it.toolCallId != null }
                .groupBy { it.toolCallId!! }
                .mapValues { (_, list) -> ArrayDeque(list) }

        return history.flatMap { hist ->
            when (hist.role) {
                "user" -> {
                    listOf(ChatItem.UserMessage(hist.content ?: ""))
                }

                "assistant" -> {
                    // 2026-08-06 对齐 RikkaHub 层次结构：历史重建为 AssistantTurn 块序列。
                    // 历史只存单一 reasoning + toolCalls（交错顺序已丢失），近似顺序：推理块 → 正文块 → 工具块
                    val blocks = mutableListOf<TurnBlock>()
                    hist.reasoning?.takeIf { it.isNotBlank() }?.let { blocks.add(TurnBlock.Reasoning(it)) }
                    if (!hist.content.isNullOrBlank()) blocks.add(TurnBlock.Text(hist.content))
                    hist.toolCalls?.forEach { tc ->
                        val queue = toolResultsByCall[tc.id]
                        val result = queue?.removeFirstOrNull()
                        blocks.add(
                            TurnBlock.Tool(
                                id = tc.id,
                                name = tc.name,
                                args = tc.arguments,
                                output = result?.content,
                                isRunning = result == null,
                                expanded = result != null,
                            ),
                        )
                    }
                    if (blocks.isEmpty()) emptyList<ChatItem>() else listOf(ChatItem.AssistantTurn(blocks))
                }

                "system" -> {
                    listOf(ChatItem.SystemNotice(hist.content ?: ""))
                }

                else -> {
                    emptyList()
                }
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
        val connectionChanged =
            old.connectTimeoutSec != s.connectTimeoutSec ||
                old.sseReconnectEnabled != s.sseReconnectEnabled ||
                old.sseReconnectMaxDelaySec != s.sseReconnectMaxDelaySec
        if (connectionChanged && _uiState.value.serverUrl.isNotBlank()) {
            repository = createRepository(_uiState.value.serverUrl, currentAuth)
            collectConnectionState()
        }
    }

    // ── CLI 集成（第五批 E-3）──

    /** 更新 CLI 集成设置（开关/工具/工作目录/超时），立即持久化。 */
    fun updateCliSettings(s: CliIntegrationStore.CliSettings) {
        _uiState.update { it.copy(cliSettings = s) }
        CliIntegrationStore.save(getApplication(), s)
    }

    /**
     * 构建 CLI 集成注入指令（提示词层）。开关关闭时返回 null。
     * 按所选工具列出可用的部署 CLI 包装脚本（/root/aide-wrap.sh / /root/oc-wrap.sh）。
     */
    private fun cliInstruction(): String? {
        val s = _uiState.value.cliSettings
        if (!s.enabled) return null
        val scripts =
            when (s.tool) {
                CliIntegrationStore.TOOL_AIDER -> "/root/aide-wrap.sh"
                CliIntegrationStore.TOOL_OPENCODE -> "/root/oc-wrap.sh"
                else -> "/root/aide-wrap.sh / /root/oc-wrap.sh"
            }
        return buildString {
            append("你可使用部署的 CLI 工具（$scripts）完成任务。")
            append("脚本调用格式：bash <脚本> \"任务描述\" [工作目录] [超时秒]，")
            append("工作目录仅允许 /tmp 及子目录，超时默认 120s。")
            append("工作目录：${s.workdir.ifBlank { "/tmp" }}；调用超时：${s.timeoutSec}s。")
            append("如需执行，请通过可用的 shell 工具调用对应包装脚本。")
        }
    }

    // ── 发送消息 ──

    /**
     * 2026-08-07：重新生成最后一条用户消息（消息操作行「刷新」）。
     * 取最后一条用户消息文本回填输入框并重新发送；流式中禁用。
     */
    fun regenerateLast() {
        if (_uiState.value.isStreaming) return
        val lastUser =
            _uiState.value.messages.lastOrNull { it is ChatItem.UserMessage } as? ChatItem.UserMessage
                ?: return
        _uiState.update { it.copy(inputText = lastUser.content) }
        sendMessage()
    }

    /** 2026-08-07：删除指定内容的消息（消息操作行「删除」）。 */
    fun deleteMessage(content: String) {
        _uiState.update {
            it.copy(messages = it.messages.filterNot { m ->
                (m is ChatItem.UserMessage && m.content == content) ||
                    (m is ChatItem.AssistantMessage && m.content == content) ||
                    (m is ChatItem.AssistantTurn && m.blocks.any { b -> b is TurnBlock.Text && b.text == content })
            })
        }
    }

    fun sendMessage() {
        // 新消息清除旧的排队超时标记
        _uiState.update { it.copy(pendingTimeoutHit = false) }
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        // 2026-08-08 排队机制：AI 忙（isStreaming）时消息入队，最多 MAX_PENDING_MESSAGES 条
        if (_uiState.value.isStreaming) {
            val pending = _uiState.value.pendingMessages
            if (pending.size >= MAX_PENDING_MESSAGES) {
                _uiState.update { it.copy(inputText = "") }
                appendMessage(ChatItem.SystemNotice("AI 正忙，请稍后（最多排队 $MAX_PENDING_MESSAGES 条）", isWarning = true))
            } else {
                _uiState.update {
                    it.copy(inputText = "", pendingMessages = pending + text)
                }
            }
            return
        }

        sendText(text)
    }

    /** 实际发送一条消息（AI 空闲时直接发；排队 dequeue 时也走这里） */
    private fun sendText(text: String) {
        _uiState.update {
            it.copy(
                inputText = "",
                isStreaming = true,
                error = null,
            )
        }

        // Slash 命令解析
        if (parseSlashCommand(text)) {
            finishStreaming()
            return
        }

        // 添加用户消息
        appendMessage(ChatItem.UserMessage(text))

        // 初始化流式缓冲区（轮级重置：一轮回复=一条消息，多 turn 合并）
        currentAssistantMsgIndex = null
        pendingBlocks = mutableListOf()
        pendingContent = StringBuilder()
        pendingReasoning = StringBuilder()
        pendingUsage = null
        turnFinalizeJob?.cancel()
        turnFinalizeJob = null

        // 提交消息 → 启动 SSE 监听
        // 第四批：选中用户提示词时，附加在系统提示词之后注入会话上下文
        val promptContent = activePromptContent()
        // 2026-08-06：记忆功能第一版——启用时注入【记忆】段落（提示词之后、用户文本之前）
        val memoryText = MemoryStore.activeMemoriesText(getApplication(), currentSessionKey())
        // 第五批 E-3：CLI 集成开启时，注入部署 CLI 工具可用性指令（提示词层）
        val cliInstruction = cliInstruction()
        // 2026-08-07 防复述：注入内容（提示词/记忆/CLI 指令）仅供上下文参考，
        // 明确要求 AI 不要在回复中复述或重复它们（此前 AI 常把注入内容整段复述，观感即「历史内容反复出现」）
        val antiEcho =
            "（以下注入内容仅供你参考执行，回复时请直接回答用户问题，不要复述或重复任何注入内容。）"
        val effectiveInput =
            listOfNotNull(antiEcho, promptContent, memoryText, cliInstruction, text)
                .joinToString("\n\n")
        viewModelScope.launch {
            try {
                repository.submit(effectiveInput)
                startSseCollection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendMessage(ChatItem.ErrorMessage("提交失败: ${e.message}"))
                finishStreaming()
            }
        }
    }

    /**
     * 结束一轮流式：置 isStreaming=false 并尝试发送下一条排队消息。
     * 所有流式结束点（turn_done / 提交失败 / slash 中断）统一走这里，保证排队消息被消费。
     */
    private fun finishStreaming() {
        _uiState.update { it.copy(isStreaming = false) }
        // 排队超时兜底：延迟后检查是否仍卡在排队
        pendingTimeoutJob?.cancel()
        pendingTimeoutJob = viewModelScope.launch {
            delay(60_000L) // 60 秒
            val s = _uiState.value
            if (s.pendingMessages.isNotEmpty() && !s.isStreaming) {
                _uiState.update { it.copy(pendingTimeoutHit = true) }
                appendMessage(ChatItem.SystemNotice(
                    "您有 ${s.pendingMessages.size} 条消息排队超时未发出，AI 可能因异常未响应。请尝试新建会话或检查服务器状态。",
                    isWarning = true,
                ))
            }
        }
        maybeDequeueNext()
    }

    /** 从队列取第一条待发消息发送（AI 已空闲时）。 */
    private fun maybeDequeueNext() {
        val state = _uiState.value
        if (state.isStreaming || state.pendingMessages.isEmpty()) return
        val next = state.pendingMessages.first()
        _uiState.update {
            it.copy(pendingMessages = it.pendingMessages.drop(1))
        }
        sendText(next)
    }

    /**
     * 发送图片消息（第六批：本地 OCR 优先）。
     *
     * - [ocrText]：OCR 识别文本，作为消息内容发送（图片+文字展示在消息中）；
     *   识别失败选择「发送原图」时传空字符串，服务端侧用「[图片]」占位，本地消息展示图片。
     * - [imagePath]：本地缓存图片文件路径，仅用于本地消息展示（不发送给服务端）。
     */
    fun sendImageMessage(
        ocrText: String,
        imagePath: String?,
    ) {
        val text = ocrText.trim()
        // 2026-08-08：图片消息忙时不入队（队列仅存文字），提示正忙
        if (_uiState.value.isStreaming) {
            appendMessage(ChatItem.SystemNotice("AI 正忙，请稍后（图片消息不支持排队）", isWarning = true))
            return
        }
        _uiState.update {
            it.copy(
                isStreaming = true,
                error = null,
            )
        }

        // 添加用户消息（图片 + OCR 文字）
        appendMessage(ChatItem.UserMessage(text, imagePath))

        // 初始化流式缓冲区（轮级重置：一轮回复=一条消息，多 turn 合并）
        currentAssistantMsgIndex = null
        pendingBlocks = mutableListOf()
        pendingContent = StringBuilder()
        pendingReasoning = StringBuilder()
        pendingUsage = null
        turnFinalizeJob?.cancel()
        turnFinalizeJob = null

        // 提交消息 → 启动 SSE 监听（复用 sendMessage 的提示词/CLI 注入逻辑）
        val promptContent = activePromptContent()
        // 2026-08-06：记忆功能第一版——启用时注入【记忆】段落
        val memoryText = MemoryStore.activeMemoriesText(getApplication(), currentSessionKey())
        val cliInstruction = cliInstruction()
        val effectiveInput =
            listOfNotNull(promptContent, memoryText, cliInstruction, text.ifBlank { "[图片]" })
                .joinToString("\n\n")
        viewModelScope.launch {
            try {
                repository.submit(effectiveInput)
                startSseCollection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendMessage(ChatItem.ErrorMessage("提交失败: ${e.message}"))
                finishStreaming()
            }
        }
    }

    /** 解析并执行斜杠命令。返回 true 表示已处理（不发往服务器）。 */
    private fun parseSlashCommand(text: String): Boolean {
        val cmd = text.trim()
        when {
            cmd == "/help" -> {
                appendMessage(
                    ChatItem.SystemNotice(
                        "可用命令: /plan /yolo /auto /compact /compact auto /compact manual /rewind /fork /new /stats /status /sessions /summarize /resume /delete /theme /help",
                    ),
                )
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
                    appendMessage(
                        ChatItem.SystemNotice(
                            "模型=${s?.label ?: "?"} 计划=${s?.plan ?: false} 模式=${s?.toolApprovalMode ?: "auto"} " +
                                "Token=${s?.used ?: 0}/${s?.window ?: 0}",
                        ),
                    )
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
        sseCollectionJob =
            viewModelScope.launch {
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
        connectionStateJob =
            viewModelScope.launch {
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
            pendingBlocks = mutableListOf()
            pendingContent = null
            pendingReasoning = null
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

    /** 2026-08-07：内容事件（会顺延兜底收尾）；通知类事件（notice/phase）不在其中 */
    private val CONTENT_EVENT_KINDS =
        setOf(
            "turn_started", "reasoning", "text", "message",
            "tool_dispatch", "tool_result", "tool_progress", "usage",
            "approval_request", "ask_request",
            "compaction_started", "compaction_done",
        )

    private fun handleSseEvent(event: SseEvent) {
        lastSseEventAt = System.currentTimeMillis()
        // 2026-08-07：仅内容事件顺延兜底收尾——notice/phase 等通知类事件
        // 不再取消 turnFinalizeJob（否则回复完成被无限推迟，表现为发送后迟迟收不到回复）
        if (event.kind in CONTENT_EVENT_KINDS) {
            lastContentEventAt = System.currentTimeMillis()
        }
        when (event.kind) {
            "turn_started" -> {
                // 2026-08-07 多 turn 合并模型：不再重置累积缓冲/消息索引。
                // 服务端一轮回复可能含多个 turn（多段正文+工具），若每段独立成消息
                // 正文会被拆散、同命令工具卡反复出现；此处保持累积，全部合并进同一条 AssistantTurn。
                uiRefreshJob?.cancel()
                uiRefreshJob = null
                loadTodos()
            }

            "reasoning" -> {
                event.reasoning?.let { r ->
                    pendingReasoning = appendSnapshotOrDelta(pendingReasoning, r)
                    appendOrExtendBlock(TurnBlock.Reasoning(pendingReasoning.toString()))
                }
            }

            "text" -> {
                event.text?.let { t ->
                    pendingContent = appendSnapshotOrDelta(pendingContent, t)
                    appendOrExtendBlock(TurnBlock.Text(pendingContent.toString()))
                }
            }

            "message" -> {
                event.message?.let { msg ->
                    pendingContent = StringBuilder(msg.content ?: "")
                    pendingReasoning = if (msg.reasoning != null) StringBuilder(msg.reasoning) else null
                    // 整体重置本 turn 块：正文 + 推理（历史中的近似顺序：推理块在前、正文在后）
                    // 2026-08-07：不整体重置 blocks（多 turn 合并）——覆盖最后一个 Reasoning/Text 块，保留工具块
                    pendingReasoning?.toString()?.takeIf { it.isNotBlank() }?.let {
                        val ri = pendingBlocks.indexOfLast { b -> b is TurnBlock.Reasoning }
                        if (ri >= 0) pendingBlocks[ri] = TurnBlock.Reasoning(it) else pendingBlocks.add(TurnBlock.Reasoning(it))
                    }
                    if (msg.content?.isNotBlank() == true) {
                        val ti = pendingBlocks.indexOfLast { b -> b is TurnBlock.Text }
                        if (ti >= 0) pendingBlocks[ti] = TurnBlock.Text(msg.content) else pendingBlocks.add(TurnBlock.Text(msg.content))
                    }
                    updatePendingTurn()
                }
            }

            "tool_dispatch" -> {
                event.tool?.let { tool ->
                    val card =
                        TurnBlock.Tool(
                            id = tool.id,
                            name = tool.name,
                            args = tool.args ?: tool.arguments,
                            isRunning = true,
                        )
                    // 同一工具 id（或同名同参）重复 dispatch 原位替换，保留最新状态，避免重复卡。
                    // 2026-08-07：服务端每次调用 id 均非空，若仅 id 为空才按同名同参合并则永远不触发；
                    // 同命令反复执行（如多次 git status）只保留最新一张。
                    val idx =
                        pendingBlocks.indexOfLast {
                            it is TurnBlock.Tool &&
                                (it.id == tool.id ||
                                    (it.name == tool.name && it.args == (tool.args ?: tool.arguments)))
                        }
                    if (idx >= 0) {
                        pendingBlocks[idx] = card
                    } else {
                        pendingBlocks.add(card)
                    }
                    updatePendingTurn()
                }
            }

            "tool_result" -> {
                event.tool?.let { tool ->
                    updateToolBlock(
                        tool.id,
                        TurnBlock.Tool(
                            id = tool.id,
                            name = tool.name,
                            output = tool.output,
                            err = tool.err,
                            truncated = tool.truncated,
                            isRunning = false,
                        ),
                    )
                    loadTodos()
                }
            }

            "tool_progress" -> {
                event.tool?.let { tool ->
                    updateToolBlock(
                        tool.id,
                        TurnBlock.Tool(
                            id = tool.id,
                            name = tool.name,
                            output = tool.output,
                            isRunning = true,
                        ),
                    )
                }
            }

            "usage" -> {
                event.usage?.let { u ->
                    // 累计统计（会话级）——服务端推送的是会话累计快照，直接覆盖，避免多次事件虚高
                    _uiState.update { state ->
                        state.copy(
                            cumulativeTokens = u.totalTokens,
                            cumulativePromptTokens = u.promptTokens,
                            cumulativeCompletionTokens = u.completionTokens,
                            cumulativeCost = u.costUsd ?: u.cost ?: 0.0,
                            cumulativeCacheHit = u.cacheHitTokens,
                            cumulativeCacheMiss = u.cacheMissTokens,
                        )
                    }
                    // 2026-08-07：usage 事件只更新累计统计，不再插入对话（避免 token 卡反复出现）；
                    // 本轮结束时由 finalizeTurn 追加一张汇总卡
                    pendingUsage = u
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
                // 2026-08-07：turn_done 不立即 finalize——多 turn 时这只是中间段。
                // 调度 8s 兜底：之后无新事件才收尾（流关闭时 collect finally 会提前 finalize）。
                scheduleTurnFinalize()
                loadTodos()
            }
        }
    }

    /**
     * 2026-08-07 性能优化：流式 delta 节流刷新。
     * text/reasoning 事件高频到达（每 token 一次），每次都 updatePendingTurn 会
     * 整轮重建 AssistantTurn + 全列表拷贝；此处合并 60ms 窗口内的事件为一次重组。
     * 工具卡/整体覆盖/回合收尾等低频路径仍走立即 updatePendingTurn。
     */
    /**
     * 2026-08-07：turn_done 兜底收尾。多 turn 合并模型下，最后一个 turn_done 之后
     * 8s 无新事件即认为本轮结束并 finalize（流关闭时 collect finally 会提前触发）。
     */
    private fun scheduleTurnFinalize() {
        turnFinalizeJob?.cancel()
        val contentAt = lastContentEventAt
        turnFinalizeJob =
            viewModelScope.launch {
                delay(8000)
                turnFinalizeJob = null
                if (lastContentEventAt == contentAt) {
                    // 8s 内无新内容事件 → 本轮结束，收尾
                    finalizeTurn()
                } else {
                    // 有新内容（如多 turn 下一段已开始）→ 顺延重新调度
                    scheduleTurnFinalize()
                }
            }
    }

    // 2026-08-08：流式缓冲——高频增量顺延刷新，停顿 ≥250ms 才批量显示（防正文蹦字/跳变）
    private val streamBufferMs: Long = 250

    private fun scheduleUiRefresh() {
        // 流式中：增量到达顺延刷新（停顿缓冲）；非流式：立即刷新
        if (!_uiState.value.isStreaming) {
            updatePendingTurn()
            return
        }
        uiRefreshJob?.cancel()
        uiRefreshJob =
            viewModelScope.launch {
                delay(streamBufferMs)
                uiRefreshJob = null
                updatePendingTurn()
            }
    }

    /**
     * 2026-08-06 对齐 RikkaHub 层次结构：用当前 pendingBlocks 重建本轮 AssistantTurn 并替换 UI 中的对应消息。
     */
    private fun updatePendingTurn() {
        if (pendingBlocks.isEmpty()) return
        val turn = ChatItem.AssistantTurn(pendingBlocks.toList())
        val idx = currentAssistantMsgIndex
        if (idx != null) {
            _uiState.update { state ->
                if (idx < state.messages.size && state.messages[idx] is ChatItem.AssistantTurn) {
                    val updated = state.messages.toMutableList()
                    updated[idx] = turn
                    state.copy(messages = updated)
                } else {
                    // 索引失效（如 compaction 重建），回退到追加
                    val newList = state.messages + turn
                    currentAssistantMsgIndex = newList.size - 1
                    state.copy(messages = newList)
                }
            }
        } else {
            // 本轮首次助手消息 — 追加到列表末尾
            _uiState.update { state ->
                val newList = state.messages + turn
                currentAssistantMsgIndex = newList.size - 1
                state.copy(messages = newList)
            }
        }
    }

    /**
     * 追加或合并推理/正文块：
     * - 若 pendingBlocks 末尾是同类块 → 合并（流式增量）
     * - 否则新增块（推理→正文→推理 交错出现时各自成块）
     */
    /**
     * 2026-08-07：区分服务端 text/reasoning 事件的「增量」与「快照」语义。
     *
     * 服务端若推完整快照（新文本以旧文本开头），旧代码按增量 append 会导致
     * 「已说过的内容 + 末尾新增一两句」反复累积；此处自动判别：
     * - 快照（新文本以旧文本开头且更长）→ 覆盖，避免重复累积
     * - 增量（新片段不以旧文本开头）→ 追加
     */
    private fun appendSnapshotOrDelta(buffer: StringBuilder?, incoming: String): StringBuilder? {
        if (incoming.isEmpty()) return buffer
        if (buffer == null) return StringBuilder(incoming)
        val cur = buffer.toString()
        return if (incoming.startsWith(cur) && incoming.length > cur.length) {
            // 快照语义：完整文本重复推送 → 直接覆盖
            StringBuilder(incoming)
        } else {
            // 增量语义：新片段 → 追加
            buffer.append(incoming)
            buffer
        }
    }

    private fun appendOrExtendBlock(block: TurnBlock) {
        // 2026-08-07：正文/推理覆盖「最后一个同类块」（任意位置，而非仅末尾）。
        // 快照/增量累积的完整文本始终只保留一块，中间穿插工具/推理块也不会把正文拆散。
        when (block) {
            is TurnBlock.Text -> {
                val idx = pendingBlocks.indexOfLast { it is TurnBlock.Text }
                if (idx >= 0) pendingBlocks[idx] = block else pendingBlocks.add(block)
            }
            is TurnBlock.Reasoning -> {
                val idx = pendingBlocks.indexOfLast { it is TurnBlock.Reasoning }
                if (idx >= 0) pendingBlocks[idx] = block else pendingBlocks.add(block)
            }
            else -> pendingBlocks.add(block)
        }
        scheduleUiRefresh()
    }

    /** 更新工具块：按「最后一条运行中的同 id 块」回填（同 id 多次调用按顺序配对） */
    private fun updateToolBlock(
        id: String,
        card: TurnBlock.Tool,
    ) {
        // 2026-08-07：id 为空时退化为「同名同参」匹配，保证 result/progress 回填
        // 命中的是同一张卡，而不是每次新增一张重复的工具卡
        fun matches(it: TurnBlock.Tool): Boolean =
            it.id == id || (it.name == card.name && it.args == card.args)

        var target = -1
        for (i in pendingBlocks.indices) {
            val it = pendingBlocks[i]
            if (it is TurnBlock.Tool && matches(it) && it.isRunning) target = i
        }
        if (target < 0) {
            for (i in pendingBlocks.indices) {
                val it = pendingBlocks[i]
                if (it is TurnBlock.Tool && matches(it)) target = i
            }
        }
        if (target >= 0) pendingBlocks[target] = card
        updatePendingTurn()
    }

    private fun appendMessage(item: ChatItem) {
        _uiState.update { state ->
            state.copy(messages = state.messages + item)
        }
    }

    private fun rebuildFromHistory(history: List<HistoryMessage>) {
        currentAssistantMsgIndex = null
        val items = buildHistoryItems(history)
        _uiState.update { it.copy(messages = items) }
    }

    private fun finalizeTurn() {
        // 2026-08-07 幂等保护：流关闭/cancel 时 finally 也会调 finalizeTurn，
        // 若当前没有进行中的 turn（缓冲已空）直接返回，避免重复收尾/重复统计卡
        turnFinalizeJob?.cancel()
        turnFinalizeJob = null
        if (pendingContent == null && pendingBlocks.isEmpty() && currentAssistantMsgIndex == null) {
            return
        }
        // 2026-08-06：AI 直接管理记忆（方案 A）——turn 结束时解析回复中的【记忆+/-】标记，
        // 应用增删并剔除标记行后刷新 UI 展示
        val raw = pendingContent?.toString()
        if (!raw.isNullOrBlank() && MemoryStore.isEnabled(getApplication())) {
            val cleaned = MemoryStore.processMarkers(getApplication(), raw, currentSessionKey())
            if (cleaned != raw) {
                pendingContent = StringBuilder(cleaned)
                // 更新最后一个 Text 块为剔除标记后的内容
                val lastText = pendingBlocks.indexOfLast { it is TurnBlock.Text }
                if (lastText >= 0) {
                    pendingBlocks[lastText] = TurnBlock.Text(cleaned)
                    updatePendingTurn()
                }
            }
        }
        // 2026-08-07：取消节流刷新，立即落最终状态
        uiRefreshJob?.cancel()
        uiRefreshJob = null
        updatePendingTurn()
        currentAssistantMsgIndex = null
        pendingContent = null
        pendingReasoning = null
        finishStreaming()
        // 2026-08-07 SSE 可靠性：此处不 cancel sseCollectionJob。
        // 原先 turn_done 即掐断整条 SSE 流，一轮多 turn（多工具子回合/多段回复）时
        // 后续事件全部丢失；流生命周期由 sendMessage 重启 / 服务端关流 / onCleared 管理。
        // 批 B-14：多步任务跑完 → 系统通知栏提醒（从工具块统计）
        val toolNames =
            pendingBlocks
                .filterIsInstance<TurnBlock.Tool>()
                .map { it.name }
                .filter { it.isNotBlank() }
                .distinct()
        if (toolNames.isNotEmpty()) {
            val summary =
                when {
                    toolNames.size <= 3 -> "已完成 ${toolNames.size} 个工具步骤：${toolNames.joinToString("、")}"
                    else -> "已完成 ${toolNames.size} 个工具步骤（${toolNames.take(3).joinToString("、")} 等）"
                }
            NotificationHelper.notifyTaskDone(getApplication(), summary)
        }
        pendingBlocks = mutableListOf()
        // 2026-08-07：本轮 token 统计——结束本轮时在回复末尾追加一张汇总卡（含会话累计），
        // 不再随 usage 事件反复插入对话
        pendingUsage?.let { u ->
            if (_uiState.value.settings.showTokens) {
                appendMessage(ChatItem.UsageStats(u))
            }
            pendingUsage = null
        }
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

    fun rewindTo(
        turn: Int,
        scope: String = "both",
    ) {
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

    fun summarizeAt(
        turn: Int,
        mode: String,
    ) {
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

    fun approveTool(
        id: String,
        session: Boolean = false,
        persist: Boolean = false,
        scope: String = "",
    ) {
        viewModelScope.launch {
            repository.approve(id, true, session, persist, scope)
            _uiState.update { state ->
                state.copy(
                    messages =
                        state.messages.filter {
                            it !is ChatItem.ApprovalCard || it.id != id
                        },
                )
            }
        }
    }

    fun denyTool(id: String) {
        viewModelScope.launch {
            repository.approve(id, false)
            _uiState.update { state ->
                state.copy(
                    messages =
                        state.messages.filter {
                            it !is ChatItem.ApprovalCard || it.id != id
                        },
                )
            }
        }
    }

    fun submitAskAnswers(
        id: String,
        answers: List<Map<String, String>>,
    ) {
        val formattedAnswers =
            answers
                .map { map ->
                    map.mapValues { it.value }.toMap()
                }.map { it.mapValues { e -> e.value as Any } }

        viewModelScope.launch {
            repository.answer(id, formattedAnswers)
            _uiState.update { state ->
                state.copy(
                    messages =
                        state.messages.filter {
                            it !is ChatItem.AskCard || it.id != id
                        },
                )
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
        // 乐观移除：立即从列表中移除，同时标记删除中
        _uiState.update { state ->
            state.copy(
                sessions = state.sessions.filter { it.name != name },
                deletingSessions = state.deletingSessions + name,
            )
        }
        viewModelScope.launch {
            try {
                repository.deleteSession(name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "删除会话失败: $name", e)
            } finally {
                // 删除完成（成功或失败）后刷新列表，移除 loading 标记
                try {
                    loadInitialData()
                } catch (_: Exception) {
                }
                _uiState.update { it.copy(deletingSessions = it.deletingSessions - name) }
            }
        }
    }

    /** 批量删除会话（第五批 E-2：多选模式全选后批量删除）。 */
    fun deleteSessions(names: List<String>) {
        if (names.isEmpty()) return
        val nameSet = names.toSet()
        // 乐观移除：立即从列表中移除，同时标记删除中
        _uiState.update { state ->
            state.copy(
                sessions = state.sessions.filter { it.name !in nameSet },
                deletingSessions = state.deletingSessions + nameSet,
            )
        }
        viewModelScope.launch {
            names.forEach { name ->
                try {
                    repository.deleteSession(name)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "删除会话失败: $name", e)
                }
            }
            // 删除完成后刷新列表，移除 loading 标记
            try {
                loadInitialData()
            } catch (_: Exception) {
            }
            _uiState.update { it.copy(deletingSessions = it.deletingSessions - nameSet) }
        }
    }

    // ── 备份导入导出（第五批 E-1）──

    /**
     * 导出备份：收集服务器配置（多套）+ 主题设置 + 自定义模型 + 全部会话历史，构建单文件 JSON。
     * 会话历史需遍历服务端各会话（resume + history），完成后恢复原会话并刷新界面。
     */
    suspend fun exportBackup(password: String): BackupExportResult =
        try {
            val context = getApplication<Application>()
            var profiles = ServerConfigStore.loadProfiles(context)
            // 从未保存过 profiles 时，把「上次连接配置」作为一套导出
            if (profiles.isEmpty()) {
                val last = ServerConfigStore.load(context)
                if (last.ip.isNotBlank()) {
                    profiles =
                        listOf(
                            ServerConfigStore.ServerProfile(
                                name = last.ip,
                                ip = last.ip,
                                port = last.port,
                                useHttps = last.useHttps,
                                authType = last.authType,
                                username = last.username,
                                password = last.password,
                                token = last.token,
                            ),
                        )
                }
            }
            val sessions = collectAllSessionHistories()
            val payload =
                BackupManager.BackupPayload(
                    settings = AppSettingsStore.load(context),
                    customModels = CustomModelStore.load(context),
                    serverConfigs = profiles,
                    sessions = sessions,
                )
            BackupExportResult(
                json = BackupManager.buildJson(payload, password),
                sessionCount = sessions.size,
                serverCount = profiles.size,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ChatViewModel", "导出备份失败", e)
            BackupExportResult(error = "导出失败：${e.message ?: "未知错误"}")
        }

    /**
     * 导入备份：解析 JSON → 恢复服务器配置（凭据解密）/主题设置/自定义模型/会话历史。
     * 凭据解密失败（密码错误 / 换机密钥不可用）会在 message 中明确提示。
     */
    fun importBackup(
        json: String,
        password: String,
    ): BackupImportResult {
        val parsed = BackupManager.parse(json, password)
        if (parsed is BackupManager.ParseResult.Err) {
            return BackupImportResult(success = false, message = parsed.message)
        }
        val payload = (parsed as BackupManager.ParseResult.Ok).payload
        return try {
            val context = getApplication<Application>()
            var restored = 0
            // 恢复服务器配置（多套）+ 最近连接
            if (payload.serverConfigs.isNotEmpty()) {
                ServerConfigStore.saveProfiles(context, payload.serverConfigs)
                ServerConfigStore.saveLast(context, payload.serverConfigs.last())
                restored++
            }
            // 恢复主题设置
            AppSettingsStore.save(context, payload.settings)
            _uiState.update { it.copy(settings = payload.settings) }
            restored++
            // 恢复自定义模型列表
            if (payload.customModels.isNotEmpty()) {
                CustomModelStore.save(context, payload.customModels)
                _uiState.update { it.copy(customModels = payload.customModels) }
                restored++
            }
            // 恢复会话历史：本地存档 + 将备份中首个会话加载到聊天界面
            if (payload.sessions.isNotEmpty()) {
                BackupManager.saveSessions(context, payload.sessions)
                val first = payload.sessions.first()
                val history =
                    first.messages.map { m ->
                        HistoryMessage(
                            role = m.role,
                            content = m.content,
                            reasoning = m.reasoning,
                            toolCalls =
                                m.toolCalls?.map { tc ->
                                    ToolCallPayload(id = tc.id, name = tc.name, arguments = tc.arguments)
                                },
                        )
                    }
                _uiState.update { it.copy(messages = buildHistoryItems(history)) }
                restored++
            }
            val warning = parsed.warning?.let { "\n$it" } ?: ""
            BackupImportResult(
                success = true,
                message = "导入成功：已恢复 $restored 项（服务器配置/主题/模型/会话）$warning",
                restoredSettings = payload.settings,
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "导入备份失败", e)
            BackupImportResult(success = false, message = "恢复失败：${e.message ?: "未知错误"}")
        }
    }

    /**
     * 遍历服务端全部会话并抓取各自历史（备份用）。
     * 完成后恢复原当前会话并刷新界面数据，避免切换会话影响聊天页。
     */
    private suspend fun collectAllSessionHistories(): List<BackupManager.BackupSession> {
        val sessions = repository.getSessions()
        if (sessions.isEmpty()) return emptyList()
        val currentPath = sessions.firstOrNull { it.current }?.path
        val result = mutableListOf<BackupManager.BackupSession>()
        sessions.forEach { s ->
            try {
                if (!s.current) repository.resumeSession(s.path)
                val history = repository.getHistory()
                result.add(
                    BackupManager.BackupSession(
                        name = s.name,
                        path = s.path,
                        title = s.title,
                        turns = s.turns,
                        messages =
                            history.map { m ->
                                BackupManager.BackupMessage(
                                    role = m.role,
                                    content = m.content,
                                    reasoning = m.reasoning,
                                    toolCalls =
                                        m.toolCalls?.map { tc ->
                                            BackupManager.BackupToolCall(tc.id, tc.name, tc.arguments)
                                        },
                                )
                            },
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "收集会话历史失败: ${s.name}", e)
            }
        }
        // 恢复原当前会话并刷新
        if (currentPath != null) {
            try {
                repository.resumeSession(currentPath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "恢复当前会话失败", e)
            }
        }
        loadInitialData()
        return result
    }

    fun compactConversation() {
        viewModelScope.launch {
            repository.compact()
        }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(showSidebar = !it.showSidebar) }
    }

    /** 2026-08-07：侧边栏改 ModalNavigationDrawer 后，手势/遮罩关闭需显式置位（避免 toggle 双触发） */
    fun setSidebar(show: Boolean) {
        _uiState.update { it.copy(showSidebar = show) }
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
        private val auth: AuthInfo?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(app, serverUrl, auth) as T
    }
}

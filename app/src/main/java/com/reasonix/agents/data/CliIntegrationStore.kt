package com.reasonix.agents.data

import android.content.Context

/**
 * CLI 集成设置本地持久化（第五批 E-3）。
 *
 * 开关开启后，发送消息时 ChatViewModel 会在提示词层注入指令，
 * 告知模型可使用部署的 CLI 工具（aide-wrap.sh / oc-wrap.sh）完成任务。
 *
 * - enabled：启用 / 禁用 reasonix 调用部署 CLI
 * - tool：   所选工具（aider / opencode / all）
 * - workdir：工作目录（默认 /tmp）
 * - timeoutSec：调用超时（默认 120s）
 */
object CliIntegrationStore {
    private const val PREFS_NAME = "reasonix_cli_integration"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TOOL = "tool"
    private const val KEY_WORKDIR = "workdir"
    private const val KEY_TIMEOUT_SEC = "timeout_sec"

    /** 工具选择常量。 */
    const val TOOL_AIDER = "aider"
    const val TOOL_OPENCODE = "opencode"
    const val TOOL_ALL = "all"

    /** 默认值。 */
    const val DEFAULT_WORKDIR = "/tmp"
    const val DEFAULT_TIMEOUT_SEC = 120

    data class CliSettings(
        val enabled: Boolean = false,
        val tool: String = TOOL_ALL,
        val workdir: String = DEFAULT_WORKDIR,
        val timeoutSec: Int = DEFAULT_TIMEOUT_SEC,
    ) {
        /** 工具显示名。 */
        val toolLabel: String
            get() =
                when (tool) {
                    TOOL_AIDER -> "aider"
                    TOOL_OPENCODE -> "opencode"
                    else -> "全部"
                }
    }

    fun load(context: Context): CliSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return CliSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            tool = prefs.getString(KEY_TOOL, TOOL_ALL) ?: TOOL_ALL,
            workdir = prefs.getString(KEY_WORKDIR, DEFAULT_WORKDIR) ?: DEFAULT_WORKDIR,
            timeoutSec = prefs.getInt(KEY_TIMEOUT_SEC, DEFAULT_TIMEOUT_SEC),
        )
    }

    fun save(
        context: Context,
        s: CliSettings,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, s.enabled)
            .putString(KEY_TOOL, s.tool)
            .putString(KEY_WORKDIR, s.workdir)
            .putInt(KEY_TIMEOUT_SEC, s.timeoutSec.coerceIn(10, 3600))
            .apply()
    }
}

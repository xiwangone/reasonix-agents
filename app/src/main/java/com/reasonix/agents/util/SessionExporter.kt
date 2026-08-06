package com.reasonix.agents.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reasonix.agents.data.model.ChatItem
import com.reasonix.agents.data.model.TurnBlock

/**
 * 会话导出（批 B-16）：当前会话导出为文本 / JSON。
 * - 文本：带角色标签的可读 Markdown 风格转储
 * - JSON：结构化导出（角色 + 内容 + 元数据），便于程序化处理
 * 通过 SAF（ACTION_CREATE_DOCUMENT）写入用户选择的文件。
 */
object SessionExporter {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** 导出条目（JSON 形态）。 */
    private data class ExportEntry(
        val type: String,
        val role: String,
        val name: String? = null,
        val content: String = "",
        val extra: String? = null,
    )

    /** 构建文本导出（含每类消息的角色前缀）。 */
    fun buildText(items: List<ChatItem>): String {
        val sb = StringBuilder()
        items.forEach { item ->
            when (item) {
                is ChatItem.UserMessage -> {
                    append(sb, "用户", item.content)
                }

                is ChatItem.AssistantMessage -> {
                    if (!item.reasoning.isNullOrBlank()) {
                        append(sb, "推理", item.reasoning)
                    }
                    append(sb, "Reasonix", item.content)
                }

                // 2026-08-06：AssistantTurn 按块序导出（推理/正文/工具交错）
                is ChatItem.AssistantTurn -> {
                    item.blocks.forEach { block ->
                        when (block) {
                            is TurnBlock.Reasoning -> append(sb, "推理", block.text)
                            is TurnBlock.Text -> append(sb, "Reasonix", block.text)
                            is TurnBlock.Tool -> {
                                val detail =
                                    buildString {
                                        append(block.name)
                                        if (!block.args.isNullOrBlank()) append("\n参数：").append(block.args)
                                        if (!block.output.isNullOrBlank()) append("\n输出：").append(block.output)
                                        if (!block.err.isNullOrBlank()) append("\n错误：").append(block.err)
                                    }
                                append(sb, "工具", detail)
                            }
                        }
                    }
                }

                is ChatItem.ToolCard -> {
                    val detail =
                        buildString {
                            append(item.name)
                            if (!item.args.isNullOrBlank()) append("\n参数：").append(item.args)
                            if (!item.output.isNullOrBlank()) append("\n输出：").append(item.output)
                            if (!item.err.isNullOrBlank()) append("\n错误：").append(item.err)
                        }
                    append(sb, "工具", detail)
                }

                is ChatItem.SystemNotice -> {
                    append(sb, "系统", item.text)
                }

                is ChatItem.ErrorMessage -> {
                    append(sb, "错误", item.text)
                }

                is ChatItem.PhaseIndicator -> {
                    append(sb, "阶段", item.text)
                }

                is ChatItem.CompactionNotice -> {
                    append(sb, "压缩", "触发：${item.trigger ?: "—"}；摘要：${item.summary ?: "—"}；消息数：${item.messages}")
                }

                is ChatItem.ApprovalCard -> {
                    append(sb, "工具审批", "${item.tool} ${item.subject ?: ""}".trim())
                }

                is ChatItem.AskCard -> {
                    append(
                        sb,
                        "提问",
                        item.questions.joinToString("\n") { q ->
                            "${q.prompt}（选项：${q.options.joinToString("/") { it.label }}）"
                        },
                    )
                }

                is ChatItem.UsageStats -> {
                    append(sb, "用量", item.usage.toString())
                }
            }
        }
        return sb.toString().trim()
    }

    /** 构建 JSON 导出（结构化）。 */
    fun buildJson(items: List<ChatItem>): String {
        val entries =
            items.flatMap { item ->
                when (item) {
                    is ChatItem.UserMessage -> {
                        listOf(ExportEntry("message", "user", content = item.content))
                    }

                    is ChatItem.AssistantMessage -> {
                        listOf(ExportEntry("message", "assistant", content = item.content, extra = item.reasoning))
                    }

                    // 2026-08-06：AssistantTurn 按块展开成多条导出
                    is ChatItem.AssistantTurn -> {
                        item.blocks.map { block ->
                            when (block) {
                                is TurnBlock.Reasoning -> ExportEntry("message", "assistant", content = block.text, extra = "reasoning")
                                is TurnBlock.Text -> ExportEntry("message", "assistant", content = block.text)
                                is TurnBlock.Tool ->
                                    ExportEntry(
                                        "tool",
                                        "assistant",
                                        name = block.name,
                                        content = block.output ?: "",
                                        extra = block.args,
                                    )
                            }
                        }
                    }

                    is ChatItem.ToolCard -> {
                        listOf(
                            ExportEntry(
                                "tool",
                                "assistant",
                                name = item.name,
                                content = item.output ?: "",
                                extra = item.args,
                            ),
                        )
                    }

                    is ChatItem.SystemNotice -> {
                        listOf(ExportEntry("system", "system", content = item.text))
                    }

                    is ChatItem.ErrorMessage -> {
                        listOf(ExportEntry("error", "system", content = item.text))
                    }

                    is ChatItem.PhaseIndicator -> {
                        listOf(ExportEntry("phase", "system", content = item.text))
                    }

                    is ChatItem.CompactionNotice -> {
                        listOf(ExportEntry("compaction", "system", content = item.summary ?: "", extra = item.trigger))
                    }

                    is ChatItem.ApprovalCard -> {
                        listOf(ExportEntry("approval", "assistant", name = item.tool, content = item.subject ?: ""))
                    }

                    is ChatItem.AskCard -> {
                        listOf(ExportEntry("ask", "assistant", content = item.questions.joinToString("; ") { it.prompt }))
                    }

                    is ChatItem.UsageStats -> {
                        listOf(ExportEntry("usage", "system", content = item.usage.toString()))
                    }
                }
            }
        return gson.toJson(mapOf("type" to "reasonix-conversation", "items" to entries))
    }

    private fun append(
        sb: StringBuilder,
        role: String,
        content: String,
    ) {
        if (content.isBlank()) return
        sb.append("### ").append(role).append('\n')
        sb.append(content).append("\n\n")
    }

    /** 通过 ContentResolver 写入导出文件。 */
    fun write(
        context: Context,
        uri: Uri,
        content: String,
    ): Boolean =
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } != null
        } catch (e: Exception) {
            false
        }
}

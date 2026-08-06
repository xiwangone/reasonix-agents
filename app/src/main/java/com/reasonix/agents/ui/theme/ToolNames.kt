package com.reasonix.agents.ui.theme

/**
 * 工具名「英文「中文」」映射（2026-08-06，对齐 RikkaHub 中文化习惯）。
 *
 * 在工具折叠标题处展示：英文原名「中文翻译」，如 bash「终端命令」。
 * 未收录的工具名原样展示（不强行翻译）。
 */
object ToolNames {
    private val MAP =
        mapOf(
            "bash" to "终端命令",
            "terminal" to "终端命令",
            "shell" to "Shell 命令",
            "read_file" to "读取文件",
            "write_file" to "写入文件",
            "edit_file" to "编辑文件",
            "multi_edit" to "批量编辑",
            "delete_file" to "删除文件",
            "delete_symbol" to "删除符号",
            "move_file" to "移动文件",
            "grep" to "搜索文本",
            "glob" to "查找文件",
            "ls" to "列出目录",
            "explore" to "探索代码库",
            "research" to "调研",
            "review" to "代码审查",
            "security_review" to "安全审查",
            "memory" to "记忆",
            "remember" to "保存记忆",
            "forget" to "清理记忆",
            "ask" to "提问",
            "ask_user" to "向用户提问",
            "todo_write" to "更新任务清单",
            "web_fetch" to "抓取网页",
            "complete_step" to "完成步骤",
            "code_index" to "代码索引",
            "lsp_definition" to "符号定义",
            "lsp_references" to "符号引用",
            "lsp_hover" to "类型提示",
            "lsp_diagnostics" to "编译诊断",
            "mcp__codebase0graph__search_code" to "代码图搜索",
            "mcp__codebase0graph__search_graph" to "代码图查询",
            "mcp__codebase0graph__get_code_snippet" to "代码片段",
            "mcp__codebase0graph__get_architecture" to "架构总览",
            "mcp__codebase0graph__trace_path" to "调用链追踪",
            "mcp__anysearch__search" to "网络搜索",
            "mcp__anysearch__batch_search" to "批量搜索",
            "mcp__anysearch__extract" to "网页提取",
            "mcp__anysearch__get_sub_domains" to "领域发现",
            "run_skill" to "调用技能",
            "read_skill" to "读取技能",
            "task" to "子任务",
            "fleet" to "并行子任务",
            "parallel_tasks" to "并行任务",
            "wait" to "等待任务",
            "bash_output" to "读取输出",
        )

    /** 展示名：英文「中文」；未收录 → 原样返回 */
    fun display(name: String): String {
        val zh = MAP[name] ?: return name
        return "$name「$zh」"
    }
}

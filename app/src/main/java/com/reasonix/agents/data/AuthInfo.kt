package com.reasonix.agents.data

/**
 * 认证方式（批 A-4 扩展）。
 * 连接 Reasonix 服务时可选择：
 * - [AuthType.NONE]    无认证（直连）
 * - [AuthType.BASIC]   Basic Auth（用户名 + 密码）
 * - [AuthType.BEARER]  Bearer Token（API Token）
 */
enum class AuthType {
    NONE,
    BASIC,
    BEARER,
    ;

    companion object {
        fun from(name: String?): AuthType = entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** 认证配置；由连接页表单构建，传给 REST / SSE 客户端生成 Authorization 头。 */
data class AuthInfo(
    val type: AuthType = AuthType.NONE,
    val username: String = "",
    val password: String = "",
    val token: String = "",
) {
    val isActive: Boolean get() = type != AuthType.NONE
}

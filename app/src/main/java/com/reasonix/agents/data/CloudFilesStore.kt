package com.reasonix.agents.data

import android.content.Context

/**
 * filebrowser 云盘配置（2026-08-06）。
 *
 * - 服务器地址（默认 https://cloud.louxia.xyz）、账号、密码（[CredentialCrypto] 加密落盘）；
 * - filebrowser 是 RikkaHub Agents 与 Reasonix Agents 的中转站及个人云盘（ECS 侧
 *   部署于 127.0.0.1:10006，nginx Basic Auth 前置，根目录 /root）；
 * - 访问走 REST API：POST /api/login 取 JWT → /api/resources/{path} 列目录 / 上传
 *   → /api/raw/{path} 下载 → /api/rename /api/delete。
 */
object CloudFilesStore {
    private const val TAG = "CloudFilesStore"
    private const val PREFS_NAME = "reasonix_cloud"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"

    /** filebrowser 默认服务器地址（nginx Basic Auth 前置）。 */
    const val DEFAULT_SERVER_URL = "https://cloud.louxia.xyz"

    /** 云盘配置（内存视图，password 为明文）。 */
    data class CloudSettings(
        val serverUrl: String = DEFAULT_SERVER_URL,
        val username: String = "",
        val password: String = "",
    ) {
        /** 配置是否完整。 */
        val isConfigured: Boolean
            get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    fun load(context: Context): CloudSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        return CloudSettings(
            serverUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL,
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = decryptOrMigrate(context, storedPassword),
        )
    }

    fun save(context: Context, settings: CloudSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SERVER_URL, settings.serverUrl.trim())
            .putString(KEY_USERNAME, settings.username.trim())
            .putString(KEY_PASSWORD, CredentialCrypto.encrypt(settings.password))
            .apply()
    }

    /** 旧版明文密码 → 立即迁移为密文；密文解密失败按空处理。 */
    private fun decryptOrMigrate(context: Context, stored: String): String {
        if (stored.isEmpty()) return ""
        return CredentialCrypto.decrypt(stored) ?: stored.ifBlank { "" }
    }
}

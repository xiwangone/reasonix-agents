package com.reasonix.agents.data

import android.content.Context
import android.util.Log

/**
 * 服务器配置本地持久化存储（SharedPreferences）。
 * 保存用户上次输入的 IP / 端口 / 用户名 / 密码，下次打开自动回填。
 * password 使用 [CredentialCrypto]（AES-GCM + AndroidKeyStore）加密存储，禁止明文落盘。
 */
object ServerConfigStore {

    private const val TAG = "ServerConfigStore"
    private const val PREFS_NAME = "reasonix_server_config"
    private const val KEY_IP = "ip"
    private const val KEY_PORT = "port"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_USE_HTTPS = "use_https"

    data class Config(
        val ip: String = "",
        val port: String = "",
        val username: String = "",
        val password: String = "",
        val useHttps: Boolean = false
    )

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        return Config(
            ip = prefs.getString(KEY_IP, "") ?: "",
            port = prefs.getString(KEY_PORT, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            // 旧版明文 → 读取后立即迁移为密文；密文解密失败则按空处理
            password = when {
                storedPassword.isEmpty() -> ""
                CredentialCrypto.isEncrypted(storedPassword) ->
                    CredentialCrypto.decrypt(storedPassword) ?: ""
                else -> storedPassword.also { plaintext ->
                    // 迁移失败（如 KeyStore 异常）只保留内存明文，不落盘、不崩溃，下次 load 重试
                    try {
                        migratePlaintextPassword(context, plaintext)
                    } catch (e: Exception) {
                        Log.e(TAG, "密码加密迁移失败，下次加载重试", e)
                    }
                }
            },
            useHttps = prefs.getBoolean(KEY_USE_HTTPS, false)
        )
    }

    /** 将旧版明文密码原地加密迁移，避免继续明文留存。 */
    private fun migratePlaintextPassword(context: Context, plaintext: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PASSWORD, CredentialCrypto.encrypt(plaintext))
            .apply()
    }

    fun save(
        context: Context,
        ip: String,
        port: String,
        username: String = "",
        password: String = "",
        useHttps: Boolean = false
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IP, ip)
            .putString(KEY_PORT, port)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, CredentialCrypto.encrypt(password))
            .putBoolean(KEY_USE_HTTPS, useHttps)
            .apply()
    }
}

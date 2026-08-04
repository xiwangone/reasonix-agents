package com.reasonix.deepseek_reasonix_android.data

import android.content.Context

/**
 * 服务器配置本地持久化存储（SharedPreferences）。
 * 保存用户上次输入的 IP / 端口 / 用户名 / 密码，下次打开自动回填。
 */
object ServerConfigStore {

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
        return Config(
            ip = prefs.getString(KEY_IP, "") ?: "",
            port = prefs.getString(KEY_PORT, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = prefs.getString(KEY_PASSWORD, "") ?: "",
            useHttps = prefs.getBoolean(KEY_USE_HTTPS, false)
        )
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
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_USE_HTTPS, useHttps)
            .apply()
    }
}

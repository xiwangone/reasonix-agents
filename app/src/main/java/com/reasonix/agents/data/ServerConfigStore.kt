package com.reasonix.agents.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 服务器配置本地持久化存储（SharedPreferences）。
 *
 * 批 B-12「连接配置记忆 + 多服务器」：
 * - 单配置字段（ip/port/username/password/useHttps/authType/token）保存「上次连接配置」，
 *   重开应用自动回填；
 * - [profiles] 保存多套命名服务器配置，支持快速切换；
 * - 密码 / Token 使用 [CredentialCrypto]（AES-GCM + AndroidKeyStore）加密存储，禁止明文落盘。
 */
object ServerConfigStore {

    private const val TAG = "ServerConfigStore"
    private const val PREFS_NAME = "reasonix_server_config"
    private const val KEY_IP = "ip"
    private const val KEY_PORT = "port"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_USE_HTTPS = "use_https"
    private const val KEY_AUTH_TYPE = "auth_type"
    private const val KEY_TOKEN = "token"
    private const val KEY_PROFILES = "profiles_json"

    private val gson = Gson()

    /** 单套命名服务器配置（内存视图，password/token 为明文）。 */
    data class ServerProfile(
        val name: String = "",
        val ip: String = "",
        val port: String = "",
        val useHttps: Boolean = false,
        val authType: String = AuthType.NONE.name,
        val username: String = "",
        val password: String = "",
        val token: String = ""
    ) {
        val label: String
            get() = if (name.isNotBlank()) name else ip.ifBlank { "未命名服务器" }

        /** 生成认证信息（用于 REST / SSE 客户端）。 */
        fun toAuth(): AuthInfo = AuthInfo(
            type = AuthType.from(authType),
            username = username,
            password = password,
            token = token
        )
    }

    /** 持久化形态：password / token 已加密。 */
    private data class StoredProfile(
        val name: String,
        val ip: String,
        val port: String,
        val useHttps: Boolean,
        val authType: String,
        val username: String,
        val passwordEnc: String,
        val tokenEnc: String
    )

    /** 上次连接配置（自动回填用）。 */
    data class Config(
        val ip: String = "",
        val port: String = "",
        val username: String = "",
        val password: String = "",
        val useHttps: Boolean = false,
        val authType: String = AuthType.NONE.name,
        val token: String = ""
    )

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        val storedToken = prefs.getString(KEY_TOKEN, "") ?: ""
        return Config(
            ip = prefs.getString(KEY_IP, "") ?: "",
            port = prefs.getString(KEY_PORT, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            // 旧版明文 → 读取后立即迁移为密文；密文解密失败则按空处理
            password = decryptOrMigrate(context, KEY_PASSWORD, storedPassword),
            useHttps = prefs.getBoolean(KEY_USE_HTTPS, false),
            authType = prefs.getString(KEY_AUTH_TYPE, AuthType.NONE.name) ?: AuthType.NONE.name,
            token = decryptOrMigrate(context, KEY_TOKEN, storedToken)
        )
    }

    private fun decryptOrMigrate(context: Context, key: String, stored: String): String {
        if (stored.isEmpty()) return ""
        return when {
            CredentialCrypto.isEncrypted(stored) -> CredentialCrypto.decrypt(stored) ?: ""
            else -> stored.also { plaintext ->
                try {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(key, CredentialCrypto.encrypt(plaintext))
                        .apply()
                } catch (e: Exception) {
                    Log.e(TAG, "明文迁移失败，下次加载重试", e)
                }
            }
        }
    }

    fun save(
        context: Context,
        ip: String,
        port: String,
        username: String = "",
        password: String = "",
        useHttps: Boolean = false,
        authType: String = AuthType.NONE.name,
        token: String = ""
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IP, ip)
            .putString(KEY_PORT, port)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, CredentialCrypto.encrypt(password))
            .putBoolean(KEY_USE_HTTPS, useHttps)
            .putString(KEY_AUTH_TYPE, authType)
            .putString(KEY_TOKEN, CredentialCrypto.encrypt(token))
            .apply()
    }

    /** 保存最近一次成功连接（含认证信息），供下次重开自动回填。 */
    fun saveLast(context: Context, profile: ServerProfile) {
        save(
            context = context,
            ip = profile.ip,
            port = profile.port,
            username = profile.username,
            password = profile.password,
            useHttps = profile.useHttps,
            authType = profile.authType,
            token = profile.token
        )
    }

    // ── 多服务器配置（批 B-12）──

    fun loadProfiles(context: Context): List<ServerProfile> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<StoredProfile>>() {}.type
            val stored = gson.fromJson<List<StoredProfile>>(raw, type) ?: return emptyList()
            stored.map { p ->
                ServerProfile(
                    name = p.name,
                    ip = p.ip,
                    port = p.port,
                    useHttps = p.useHttps,
                    authType = p.authType,
                    username = p.username,
                    password = CredentialCrypto.decrypt(p.passwordEnc) ?: "",
                    token = CredentialCrypto.decrypt(p.tokenEnc) ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析服务器配置列表失败", e)
            emptyList()
        }
    }

    fun saveProfiles(context: Context, profiles: List<ServerProfile>) {
        val stored = profiles.map { p ->
            StoredProfile(
                name = p.name,
                ip = p.ip,
                port = p.port,
                useHttps = p.useHttps,
                authType = p.authType,
                username = p.username,
                passwordEnc = CredentialCrypto.encrypt(p.password),
                tokenEnc = CredentialCrypto.encrypt(p.token)
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILES, gson.toJson(stored))
            .apply()
    }
}

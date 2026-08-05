package com.reasonix.agents.data

import android.content.Context
import android.util.Log

/**
 * 坚果云 WebDAV 同步配置（第八批）。
 *
 * - 服务器地址 / 账号 / 远程备份路径 / 定时同步开关与时间；
 * - 密码使用 [CredentialCrypto]（AES-GCM + AndroidKeyStore）加密落盘，禁止明文；
 * - 同步状态（上次同步时间 + 成败 + 信息）随配置一起持久化，供设置页展示。
 */
object WebDavStore {
    private const val TAG = "WebDavStore"
    private const val PREFS_NAME = "reasonix_webdav"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_REMOTE_PATH = "remote_path"
    private const val KEY_AUTO_SYNC = "auto_sync"
    private const val KEY_AUTO_TIME = "auto_time"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"
    private const val KEY_LAST_SYNC_OK = "last_sync_ok"
    private const val KEY_LAST_SYNC_MSG = "last_sync_msg"

    /** 坚果云 WebDAV 默认服务器地址。 */
    const val DEFAULT_SERVER_URL = "https://dav.jianguoyun.com/dav/"

    /** 默认远程备份路径（坚果云目录下）。 */
    const val DEFAULT_REMOTE_PATH = "reasonix/backup.json"

    /** 默认定时同步时间（每天 02:00）。 */
    const val DEFAULT_AUTO_TIME = "02:00"

    /** WebDAV 同步配置（内存视图，password 为明文）。 */
    data class WebDavSettings(
        val serverUrl: String = DEFAULT_SERVER_URL,
        val username: String = "",
        val password: String = "",
        val remotePath: String = DEFAULT_REMOTE_PATH,
        val autoSyncEnabled: Boolean = false,
        val autoSyncTime: String = DEFAULT_AUTO_TIME,
        val lastSyncAt: Long = 0L,
        val lastSyncOk: Boolean = true,
        val lastSyncMessage: String = "",
    ) {
        /** 配置是否完整（地址/账号/密码/路径齐备才可执行同步）。 */
        val isConfigured: Boolean
            get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && remotePath.isNotBlank()
    }

    fun load(context: Context): WebDavSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        return WebDavSettings(
            serverUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL,
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            // 旧版明文 → 读取后立即迁移为密文；密文解密失败则按空处理
            password = decryptOrMigrate(context, storedPassword),
            remotePath = prefs.getString(KEY_REMOTE_PATH, DEFAULT_REMOTE_PATH) ?: DEFAULT_REMOTE_PATH,
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, false),
            autoSyncTime = prefs.getString(KEY_AUTO_TIME, DEFAULT_AUTO_TIME) ?: DEFAULT_AUTO_TIME,
            lastSyncAt = prefs.getLong(KEY_LAST_SYNC_AT, 0L),
            lastSyncOk = prefs.getBoolean(KEY_LAST_SYNC_OK, true),
            lastSyncMessage = prefs.getString(KEY_LAST_SYNC_MSG, "") ?: "",
        )
    }

    /** 保存配置（密码加密后落盘；不覆盖同步状态字段）。 */
    fun save(context: Context, settings: WebDavSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, settings.serverUrl)
            .putString(KEY_USERNAME, settings.username)
            .putString(KEY_PASSWORD, CredentialCrypto.encrypt(settings.password))
            .putString(KEY_REMOTE_PATH, settings.remotePath)
            .putBoolean(KEY_AUTO_SYNC, settings.autoSyncEnabled)
            .putString(KEY_AUTO_TIME, settings.autoSyncTime)
            .apply()
    }

    /** 记录一次同步结果（时间 + 成败 + 信息），供设置页「同步状态」展示。 */
    fun recordSyncResult(context: Context, ok: Boolean, message: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
            .putBoolean(KEY_LAST_SYNC_OK, ok)
            .putString(KEY_LAST_SYNC_MSG, message)
            .apply()
    }

    private fun decryptOrMigrate(context: Context, stored: String): String {
        if (stored.isEmpty()) return ""
        return when {
            CredentialCrypto.isEncrypted(stored) -> CredentialCrypto.decrypt(stored) ?: ""
            else -> stored.also { plaintext ->
                try {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_PASSWORD, CredentialCrypto.encrypt(plaintext))
                        .apply()
                } catch (e: Exception) {
                    Log.e(TAG, "明文迁移失败，下次加载重试", e)
                }
            }
        }
    }
}

package com.reasonix.agents.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份导入导出（第五批 E-1）：单文件 JSON 备份。
 *
 * 备份内容：
 * - 服务器配置（多套 profiles，密码 / Token 加密——无密码时用 [CredentialCrypto]（AndroidKeyStore
 *   AES-GCM）加密；设置备份密码时用 PBKDF2(密码) 派生密钥 AES-GCM 整体加密凭据区）；
 * - 主题设置（[AppSettingsStore.Settings]）；
 * - 自定义模型列表（[CustomModelStore]）；
 * - 全部会话历史（服务端各会话 history 快照）。
 *
 * 备份文件结构（v1）：
 * ```
 * {
 *   "app": "reasonix-agents", "version": 1, "createdAt": ...,
 *   "protected": false,                     // 是否密码保护
 *   "salt": null, "iv": null,               // 密码保护时的 KDF 盐 / GCM IV（Base64）
 *   "settings": {...}, "customModels": [...],
 *   "serverConfigs": [...],                 // 无密码：凭据字段为 CredentialCrypto 密文
 *   "serverConfigsEnc": "...",              // 有密码：凭据区整体密文（Base64）
 *   "sessions": [...]
 * }
 * ```
 */
object BackupManager {
    private const val TAG = "BackupManager"
    private const val APP = "reasonix-agents"
    private const val VERSION = 1

    /** PBKDF2 迭代次数与密钥位数（密码保护用）。 */
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_BITS = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val SALT_LENGTH = 16

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val random = SecureRandom()

    // ── 数据模型 ──

    /** 会话历史中的单条消息（可含工具调用）。 */
    data class BackupMessage(
        val role: String = "",
        val content: String? = null,
        val reasoning: String? = null,
        val toolCalls: List<BackupToolCall>? = null,
    )

    data class BackupToolCall(
        val id: String = "",
        val name: String = "",
        val arguments: String? = null,
    )

    /** 单个会话快照。 */
    data class BackupSession(
        val name: String = "",
        val path: String = "",
        val title: String? = null,
        val turns: Int = 0,
        val messages: List<BackupMessage> = emptyList(),
    )

    /** 备份载荷（内存明文视图：服务器配置凭据为明文，导出/导入时加解密）。 */
    data class BackupPayload(
        val settings: AppSettingsStore.Settings,
        val customModels: List<CustomModelStore.CustomModel>,
        val serverConfigs: List<ServerConfigStore.ServerProfile>,
        val sessions: List<BackupSession>,
        val createdAt: Long = System.currentTimeMillis(),
    )

    /** 解析结果：成功（可选警告，如部分凭据解密失败）或失败（带原因）。 */
    sealed class ParseResult {
        data class Ok(
            val payload: BackupPayload,
            val warning: String? = null,
        ) : ParseResult()

        data class Err(
            val message: String,
        ) : ParseResult()
    }

    // ── 文件内持久化形态（凭据已加密）──

    private data class StoredServerProfile(
        val name: String = "",
        val ip: String = "",
        val port: String = "",
        val useHttps: Boolean = false,
        val authType: String = AuthType.NONE.name,
        val username: String = "",
        val passwordEnc: String = "",
        val tokenEnc: String = "",
    )

    private data class BackupFile(
        val app: String = APP,
        val version: Int = VERSION,
        val createdAt: Long = System.currentTimeMillis(),
        val protected: Boolean = false,
        val salt: String? = null,
        val iv: String? = null,
        val settings: AppSettingsStore.Settings = AppSettingsStore.Settings(),
        val customModels: List<CustomModelStore.CustomModel> = emptyList(),
        val serverConfigs: List<StoredServerProfile>? = null,
        val serverConfigsEnc: String? = null,
        val sessions: List<BackupSession> = emptyList(),
    )

    // ── 导出：构建备份 JSON ──

    /**
     * 构建单文件备份 JSON。
     * @param password 备份密码；为空表示无密码保护（凭据用 CredentialCrypto 加密），
     *                 非空则凭据区用 PBKDF2(密码) 派生的 AES-GCM 密钥整体加密。
     */
    fun buildJson(
        payload: BackupPayload,
        password: String,
    ): String {
        val protected = password.isNotBlank()
        val file =
            if (protected) {
                // 密码保护：凭据区（明文 JSON）整体加密
                val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
                val key = deriveKey(password, salt)
                val plaintext = gson.toJson(payload.serverConfigs)
                val (iv, ciphertext) = aesGcmEncrypt(key, plaintext)
                BackupFile(
                    createdAt = payload.createdAt,
                    protected = true,
                    salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                    iv = Base64.encodeToString(iv, Base64.NO_WRAP),
                    settings = payload.settings,
                    customModels = payload.customModels,
                    serverConfigsEnc = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                    sessions = payload.sessions,
                )
            } else {
                // 无密码：凭据逐条用 CredentialCrypto（AndroidKeyStore）加密
                BackupFile(
                    createdAt = payload.createdAt,
                    protected = false,
                    settings = payload.settings,
                    customModels = payload.customModels,
                    serverConfigs =
                        payload.serverConfigs.map { p ->
                            StoredServerProfile(
                                name = p.name,
                                ip = p.ip,
                                port = p.port,
                                useHttps = p.useHttps,
                                authType = p.authType,
                                username = p.username,
                                passwordEnc = CredentialCrypto.encrypt(p.password),
                                tokenEnc = CredentialCrypto.encrypt(p.token),
                            )
                        },
                    sessions = payload.sessions,
                )
            }
        return gson.toJson(file)
    }

    // ── 导入：解析备份 JSON ──

    /**
     * 解析备份 JSON。
     * - 密码保护时密码错误 / 备份损坏 → [ParseResult.Err]；
     * - 无密码时某条凭据解密失败（如换机后 AndroidKeyStore 密钥不存在）→ 凭据置空并在
     *   [ParseResult.Ok.warning] 中提示「部分凭据解密失败」。
     */
    fun parse(
        json: String,
        password: String,
    ): ParseResult {
        return try {
            val file =
                gson.fromJson(json, BackupFile::class.java)
                    ?: return ParseResult.Err("备份文件格式无效")
            if (file.app != APP || file.version != VERSION) {
                return ParseResult.Err("不支持的备份文件版本（${file.app} v${file.version}）")
            }
            val warning: String?
            val profiles: List<ServerProfilePlain> =
                if (file.protected) {
                    warning = null
                    val salt = Base64.decode(file.salt ?: "", Base64.NO_WRAP)
                    val iv = Base64.decode(file.iv ?: "", Base64.NO_WRAP)
                    val ciphertext = Base64.decode(file.serverConfigsEnc ?: "", Base64.NO_WRAP)
                    try {
                        val key = deriveKey(password, salt)
                        val plaintext = aesGcmDecrypt(key, iv, ciphertext)
                        gson.fromJson(plaintext, object : TypeToken<List<ServerProfilePlain>>() {}.type)
                            ?: return ParseResult.Err("备份文件格式无效：凭据区为空")
                    } catch (e: Exception) {
                        Log.e(TAG, "密码解密失败", e)
                        return ParseResult.Err("密码错误或备份已损坏，无法解密凭据")
                    }
                } else {
                    val (plain, failures) = decryptWithCredentialCrypto(file.serverConfigs ?: emptyList())
                    warning = if (failures > 0) credentialWarning(failures) else null
                    plain
                }
            val payload =
                BackupPayload(
                    settings = file.settings,
                    customModels = file.customModels,
                    serverConfigs =
                        profiles.map { p ->
                            ServerConfigStore.ServerProfile(
                                name = p.name,
                                ip = p.ip,
                                port = p.port,
                                useHttps = p.useHttps,
                                authType = p.authType,
                                username = p.username,
                                password = p.password,
                                token = p.token,
                            )
                        },
                    sessions = file.sessions,
                    createdAt = file.createdAt,
                )
            ParseResult.Ok(payload, warning)
        } catch (e: Exception) {
            Log.e(TAG, "解析备份失败", e)
            ParseResult.Err("备份文件解析失败：${e.message ?: "格式错误"}")
        }
    }

    /** 无密码备份：逐条用 CredentialCrypto 解密；解密失败置空并返回失败条数。 */
    private fun decryptWithCredentialCrypto(stored: List<StoredServerProfile>): Pair<List<ServerProfilePlain>, Int> {
        var failures = 0
        val result =
            stored.map { p ->
                val password = decryptOrEmpty(p.passwordEnc) { failures++ }
                val token = decryptOrEmpty(p.tokenEnc) { failures++ }
                ServerProfilePlain(
                    name = p.name,
                    ip = p.ip,
                    port = p.port,
                    useHttps = p.useHttps,
                    authType = p.authType,
                    username = p.username,
                    password = password,
                    token = token,
                )
            }
        if (failures > 0) {
            Log.w(TAG, "有 $failures 条凭据解密失败（可能来自其它设备）")
        }
        return result to failures
    }

    private fun decryptOrEmpty(
        enc: String,
        onFail: () -> Unit,
    ): String {
        if (enc.isEmpty()) return ""
        val plain = CredentialCrypto.decrypt(enc)
        if (plain == null) {
            onFail()
            return ""
        }
        return plain
    }

    /** 解析结果的警告文案（凭据解密失败提示）。 */
    fun credentialWarning(failures: Int): String = "部分服务器凭据解密失败（$failures 条），已按空值恢复——备份可能来自其它设备，AndroidKeyStore 密钥不可用"

    // ── 密码保护：PBKDF2 + AES-GCM ──

    private fun deriveKey(
        password: String,
        salt: ByteArray,
    ): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /** 返回 (iv, ciphertext)。 */
    private fun aesGcmEncrypt(
        key: SecretKey,
        plaintext: String,
    ): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return iv to ciphertext
    }

    private fun aesGcmDecrypt(
        key: SecretKey,
        iv: ByteArray,
        ciphertext: ByteArray,
    ): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // ── 明文 profile 中间形态（Gson 反序列化目标）──

    private data class ServerProfilePlain(
        val name: String = "",
        val ip: String = "",
        val port: String = "",
        val useHttps: Boolean = false,
        val authType: String = AuthType.NONE.name,
        val username: String = "",
        val password: String = "",
        val token: String = "",
    )

    // ── 文件读写（SAF）──

    /** 通过 ContentResolver 读取备份文件内容；失败返回 null。 */
    fun read(
        context: Context,
        uri: Uri,
    ): String? =
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取备份文件失败", e)
            null
        }

    /** 通过 ContentResolver 写入备份文件；成功返回 true。 */
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
            Log.e(TAG, "写入备份文件失败", e)
            false
        }

    // ── 会话历史本地存档（导入后保留全部备份会话，供日后查看）──

    private const val PREFS_NAME = "reasonix_backup_sessions"
    private const val KEY_SESSIONS = "sessions_json"

    /** 保存导入的会话历史存档（覆盖式）。 */
    fun saveSessions(
        context: Context,
        sessions: List<BackupSession>,
    ) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSIONS, gson.toJson(sessions))
            .apply()
    }

    /** 读取会话历史存档。 */
    fun loadSessions(context: Context): List<BackupSession> {
        val raw =
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<BackupSession>>() {}.type
            gson.fromJson<List<BackupSession>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "解析会话存档失败", e)
            emptyList()
        }
    }
}

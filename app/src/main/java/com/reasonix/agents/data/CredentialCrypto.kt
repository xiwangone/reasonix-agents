package com.reasonix.agents.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 凭据加密基建：AES-256-GCM，密钥由 AndroidKeyStore 持有（不出设备、不可导出）。
 *
 * 存储格式：`enc:v1:` + Base64(IV(12B) + 密文)，IV 随机生成并随密文一起保存。
 * `enc:` 前缀用于区分旧版本遗留的明文值，便于 load 时识别并自动迁移。
 *
 * 安全约定：
 * - 空字符串不做加密（直接返回 ""），避免无意义密文；
 * - 加密失败直接抛异常，绝不降级回明文；
 * - 解密失败返回 null（如换机恢复后密钥不存在），调用方按空值处理。
 */
object CredentialCrypto {

    private const val TAG = "CredentialCrypto"

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "reasonix_credential_key"
    private const val PREFIX = "enc:v1:"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    /** 存储值是否为加密格式（false 表示旧版明文）。 */
    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    /** 加密明文；空串原样返回。 */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "加密失败", e)
            throw e
        }
    }

    /**
     * 解密；空串返回 ""，非加密格式或解密失败返回 null。
     * 解密失败（如密钥随应用数据被清除）时返回 null，调用方按空值处理。
     */
    fun decrypt(stored: String): String? {
        if (stored.isEmpty()) return ""
        if (!isEncrypted(stored)) return null
        return try {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "解密失败", e)
            null
        }
    }

    /** 取 AndroidKeyStore 中的 AES 密钥；不存在则生成（首次调用时）。 */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}

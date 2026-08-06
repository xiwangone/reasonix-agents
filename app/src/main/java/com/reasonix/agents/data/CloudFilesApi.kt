package com.reasonix.agents.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * filebrowser REST API 客户端（2026-08-06）。
 *
 * - 登录：POST /api/login {username, password} → JWT（X-Auth 头）；
 * - 列目录：GET /api/resources/{path} → JSON 数组（name/size/modified/isDir）；
 * - 上传：POST /api/resources/{path}?override=true（multipart）；
 * - 下载：GET /api/raw/{path} → 文件字节流；
 * - 新建目录：POST /api/mkdir/{path}；删除：POST /api/delete/{path}。
 *
 * 注意：filebrowser 官方版 WebDAV 已于 2023-11 弃用，故不走 WebDAV 通道，
 * 直接走 REST（与坚果云 WebDAV 通道并列）。
 */
class CloudFilesApi(private val context: Context) {
    companion object {
        private const val TAG = "CloudFilesApi"
        private const val DEFAULT_TIMEOUT_SEC = 60L
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
    }

    /** 云盘文件条目。 */
    data class RemoteFile(
        val name: String,
        val size: Long,
        val modified: String,
        val isDir: Boolean,
    )

    /** 登录获取 JWT。 */
    suspend fun login(settings: CloudFilesStore.CloudSettings): String = withContext(Dispatchers.IO) {
        val base = settings.serverUrl.trim().trimEnd('/')
        val body =
            JSONObject()
                .put("username", settings.username)
                .put("password", settings.password)
                .toString()
                .toRequestBody("application/json".toMediaType())
        val req =
            Request.Builder()
                .url("$base/api/login")
                .post(body)
                .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("登录失败 HTTP ${resp.code}")
            }
            val text = resp.body?.string().orEmpty()
            val json = JSONObject(text)
            json.getString("token")
        }
    }

    /** 列目录（path 为空 → 根目录）。 */
    suspend fun list(token: String, settings: CloudFilesStore.CloudSettings, path: String = ""): List<RemoteFile> =
        withContext(Dispatchers.IO) {
            val base = settings.serverUrl.trim().trimEnd('/')
            val p = path.trim('/')
            val url = if (p.isEmpty()) "$base/api/resources/" else "$base/api/resources/$p"
            val req =
                Request.Builder()
                    .url(url)
                    .header("X-Auth", token)
                    .get()
                    .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("列目录失败 HTTP ${resp.code}")
                }
                val text = resp.body?.string().orEmpty()
                val arr = JSONArray(text)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    RemoteFile(
                        name = o.optString("name"),
                        size = o.optLong("size", 0L),
                        modified = o.optString("modified"),
                        isDir = o.optBoolean("isDir", false),
                    )
                }
            }
        }

    /** 上传文件到指定路径（override 覆盖同名）。 */
    suspend fun upload(
        token: String,
        settings: CloudFilesStore.CloudSettings,
        remotePath: String,
        file: File,
        override: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val base = settings.serverUrl.trim().trimEnd('/')
        val p = remotePath.trim('/')
        val url = "$base/api/resources/$p?override=$override"
        val body =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
        val req =
            Request.Builder()
                .url(url)
                .header("X-Auth", token)
                .post(body)
                .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.e(TAG, "上传失败 HTTP ${resp.code}")
                false
            } else {
                true
            }
        }
    }

    /** 下载文件到本地。 */
    suspend fun download(
        token: String,
        settings: CloudFilesStore.CloudSettings,
        remotePath: String,
        target: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val base = settings.serverUrl.trim().trimEnd('/')
        val p = remotePath.trim('/')
        val url = "$base/api/raw/$p"
        val req =
            Request.Builder()
                .url(url)
                .header("X-Auth", token)
                .get()
                .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.e(TAG, "下载失败 HTTP ${resp.code}")
                false
            } else {
                target.parentFile?.mkdirs()
                resp.body?.byteStream()?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        }
    }

    /** 新建目录。 */
    suspend fun mkdir(
        token: String,
        settings: CloudFilesStore.CloudSettings,
        path: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val base = settings.serverUrl.trim().trimEnd('/')
        val p = path.trim('/')
        val req =
            Request.Builder()
                .url("$base/api/mkdir/$p")
                .header("X-Auth", token)
                .post(ByteArray(0).toRequestBody(null))
                .build()
        client.newCall(req).execute().use { it.isSuccessful }
    }

    /** 删除文件或目录。 */
    suspend fun delete(
        token: String,
        settings: CloudFilesStore.CloudSettings,
        path: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val base = settings.serverUrl.trim().trimEnd('/')
        val p = path.trim('/')
        val req =
            Request.Builder()
                .url("$base/api/delete/$p")
                .header("X-Auth", token)
                .post(ByteArray(0).toRequestBody(null))
                .build()
        client.newCall(req).execute().use { it.isSuccessful }
    }
}

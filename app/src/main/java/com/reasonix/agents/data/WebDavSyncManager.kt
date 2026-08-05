package com.reasonix.agents.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reasonix.agents.sync.WebDavSyncReceiver
import com.reasonix.agents.util.NotificationHelper
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 坚果云 WebDAV 同步（第八批）。
 *
 * - 上传：把备份 JSON（[BackupManager.buildJson] 产物）PUT 到 WebDAV 远程路径；
 * - 下载：从 WebDAV GET 备份 JSON（恢复时复用 [ChatViewModel.importBackup]）；
 * - 定时同步：AlarmManager 每天定时触发 [WebDavSyncReceiver]，后台上传「配置备份」
 *   （服务器配置 / 主题 / 自定义模型，不含会话历史——不依赖服务器连接，定时成功率更高；
 *   手动同步走 [ChatViewModel.exportBackup] 含完整会话历史）；
 * - 每次同步把结果（时间 + 成败 + 信息）写入 [WebDavStore]，设置页展示；
 *   定时同步失败时发系统通知提醒。
 *
 * 网络异常分类提示：超时（SocketTimeout）/ 无法解析域名（UnknownHost）/
 * 连接失败（Connect）/ 认证失败（HTTP 401/403）/ 其他网络错误。
 */
object WebDavSyncManager {
    private const val TAG = "WebDavSyncManager"

    private const val CONNECT_TIMEOUT_SEC = 15L
    private const val READ_TIMEOUT_SEC = 30L
    private const val WRITE_TIMEOUT_SEC = 30L

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
    }

    /** 同步结果（upload/download 统一形态；ok=false 时 message 为失败原因）。 */
    data class SyncResult(
        val ok: Boolean,
        val message: String,
        val json: String? = null,
    )

    // ── 上传 / 下载 ──

    /** 上传备份 JSON 到 WebDAV。 */
    fun upload(context: Context, json: String): SyncResult {
        val settings = WebDavStore.load(context)
        if (!settings.isConfigured) {
            return SyncResult(false, "WebDAV 配置不完整：请填写服务器地址、账号、密码与远程路径")
        }
        return try {
            val request =
                Request.Builder()
                    .url(buildUrl(settings))
                    .put(json.toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
                    .header("Authorization", Credentials.basic(settings.username, settings.password))
                    .build()
            client.newCall(request).execute().use { resp ->
                when {
                    resp.code == 401 || resp.code == 403 -> SyncResult(false, "认证失败：账号或密码错误（HTTP ${resp.code}）")
                    !resp.isSuccessful -> SyncResult(false, "上传失败：HTTP ${resp.code}，请检查远程路径是否可写")
                    else -> SyncResult(true, "上传成功")
                }
            }
        } catch (e: Exception) {
            SyncResult(false, networkError(e, "上传"))
        }
    }

    /** 从 WebDAV 下载备份 JSON；成功时 [SyncResult.json] 为备份内容。 */
    fun download(context: Context): SyncResult {
        val settings = WebDavStore.load(context)
        if (!settings.isConfigured) {
            return SyncResult(false, "WebDAV 配置不完整：请填写服务器地址、账号、密码与远程路径")
        }
        return try {
            val request =
                Request.Builder()
                    .url(buildUrl(settings))
                    .get()
                    .header("Authorization", Credentials.basic(settings.username, settings.password))
                    .build()
            client.newCall(request).execute().use { resp ->
                when {
                    resp.code == 401 || resp.code == 403 -> SyncResult(false, "认证失败：账号或密码错误（HTTP ${resp.code}）")
                    resp.code == 404 -> SyncResult(false, "远程备份不存在（HTTP 404）：请先执行上传")
                    !resp.isSuccessful -> SyncResult(false, "下载失败：HTTP ${resp.code}")
                    else -> {
                        val body = resp.body?.string() ?: ""
                        if (body.isBlank()) SyncResult(false, "下载失败：远程备份内容为空")
                        else SyncResult(true, "下载成功", body)
                    }
                }
            }
        } catch (e: Exception) {
            SyncResult(false, networkError(e, "下载"))
        }
    }

    /** 网络异常分类提示（超时 / 域名 / 连接 / 其他）。 */
    private fun networkError(e: Exception, action: String): String = when (e) {
        is SocketTimeoutException -> "${action}超时：请检查网络或服务器地址"
        is UnknownHostException -> "${action}失败：无法解析服务器地址，请检查是否填写正确"
        is ConnectException -> "${action}失败：无法连接服务器，请检查网络与地址"
        else -> "${action}失败：${e.message ?: "网络错误"}"
    }

    /** 拼接 WebDAV 完整 URL：服务器地址（去尾斜杠）+ 远程路径（去头斜杠）。 */
    private fun buildUrl(settings: WebDavStore.WebDavSettings): String {
        val base = settings.serverUrl.trimEnd('/')
        val path = settings.remotePath.trimStart('/')
        return "$base/$path"
    }

    // ── 备份 JSON 构建（后台定时同步用，不含会话历史）──

    /**
     * 构建「配置备份」JSON：服务器配置（多套，凭据加密）+ 主题设置 + 自定义模型，
     * 不含会话历史（不依赖服务器连接，后台定时上传成功率高）。
     * 逻辑与 [com.reasonix.agents.ui.viewmodel.ChatViewModel.exportBackup] 前半段一致。
     */
    fun buildConfigBackup(context: Context): String {
        var profiles = ServerConfigStore.loadProfiles(context)
        // 从未保存过 profiles 时，把「上次连接配置」作为一套导出
        if (profiles.isEmpty()) {
            val last = ServerConfigStore.load(context)
            if (last.ip.isNotBlank()) {
                profiles =
                    listOf(
                        ServerConfigStore.ServerProfile(
                            name = last.ip,
                            ip = last.ip,
                            port = last.port,
                            useHttps = last.useHttps,
                            authType = last.authType,
                            username = last.username,
                            password = last.password,
                            token = last.token,
                        )
                    )
            }
        }
        val payload =
            BackupManager.BackupPayload(
                settings = AppSettingsStore.load(context),
                customModels = CustomModelStore.load(context),
                serverConfigs = profiles,
                sessions = emptyList(),
            )
        return BackupManager.buildJson(payload, "")
    }

    // ── 定时同步（AlarmManager）──

    /**
     * 设置每天定时自动上传（RTC_WAKEUP + setInexactRepeating，省电）。
     * 关闭开关时取消已设闹钟。
     */
    fun scheduleAutoSync(context: Context, settings: WebDavStore.WebDavSettings) {
        cancelAutoSync(context)
        if (!settings.autoSyncEnabled) return
        val (hour, minute) = parseTime(settings.autoSyncTime)
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // 已过今天的时间点 → 顺延到明天
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            syncPendingIntent(context),
        )
        Log.d(TAG, "已设置每天 ${settings.autoSyncTime} 自动上传备份")
    }

    /** 取消定时同步闹钟。 */
    fun cancelAutoSync(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(syncPendingIntent(context))
        Log.d(TAG, "已取消定时同步")
    }

    private fun syncPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WebDavSyncReceiver::class.java).setAction(WebDavSyncReceiver.ACTION_AUTO_SYNC)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 解析 "HH:mm"；非法输入回退默认 02:00。 */
    fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull()?.coerceIn(0, 23) ?: 2
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return hour to minute
    }

    // ── 后台定时同步完整流程（WebDavSyncReceiver 调用）──

    /**
     * 后台同步：构建配置备份 → 上传 → 记录状态；失败时发系统通知提醒。
     * 配置不完整 / 定时开关已关时静默跳过（不打扰）。
     */
    fun performBackgroundSync(context: Context) {
        val settings = WebDavStore.load(context)
        if (!settings.autoSyncEnabled || !settings.isConfigured) return
        val json = buildConfigBackup(context)
        val result = upload(context, json)
        WebDavStore.recordSyncResult(context, result.ok, result.message)
        if (!result.ok) {
            Log.w(TAG, "定时同步失败：${result.message}")
            NotificationHelper.notify(
                context,
                "⚠️ 坚果云同步失败",
                "自动上传备份失败：${result.message}",
            )
        }
    }
}

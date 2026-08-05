package com.reasonix.agents.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reasonix.agents.data.WebDavSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 坚果云定时同步接收器（第八批）。
 *
 * 由 [WebDavSyncManager.scheduleAutoSync] 的 AlarmManager 闹钟触发（每天定时）：
 * 后台构建配置备份（服务器配置 / 主题 / 自定义模型，不含会话历史）并上传到 WebDAV，
 * 结果写入 [WebDavStore]，失败时发系统通知提醒。
 *
 * 通过 goAsync() 在广播之外执行协程网络任务，避免阻塞主线程；
 * Android 8+ 后台限制下短任务（备份 JSON 较小）可正常完成。
 */
class WebDavSyncReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "WebDavSyncReceiver"

        /** 闹钟触发动作（与 PendingIntent 匹配）。 */
        const val ACTION_AUTO_SYNC = "com.reasonix.agents.action.AUTO_SYNC"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTO_SYNC) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WebDavSyncManager.performBackgroundSync(context.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "后台同步异常", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

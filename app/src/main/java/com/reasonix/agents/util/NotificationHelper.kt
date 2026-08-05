package com.reasonix.agents.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reasonix.agents.R

/**
 * 任务完成系统通知（批 B-14）。
 * agent 多步任务跑完（turn_done 且本轮有工具调用）时，通过系统通知栏提醒用户。
 * API 33+ 需要 POST_NOTIFICATIONS 运行时权限（MainActivity 启动时请求）；
 * 未授权时静默跳过，不影响主流程。
 */
object NotificationHelper {

    private const val CHANNEL_ID = "reasonix_tasks"
    private const val CHANNEL_NAME = "Reasonix 任务通知"
    private const val NOTIFICATION_ID = 1001
    private const val SYNC_NOTIFICATION_ID = 1002

    /** 发送通用系统通知（后台坚果云同步失败提醒等）；无通知权限时静默返回。 */
    fun notify(context: Context, title: String, summary: String, id: Int = SYNC_NOTIFICATION_ID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reasonix)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: Exception) {
            // 通知失败（权限/服务异常）不影响主流程
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Agent 多步任务完成时提醒" }
            )
        }
    }

    /** 发送任务完成通知；无通知权限时静默返回。 */
    fun notifyTaskDone(context: Context, summary: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reasonix)
            .setContentTitle("✅ Reasonix 任务完成")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // 通知失败（权限/服务异常）不影响主流程
        }
    }
}

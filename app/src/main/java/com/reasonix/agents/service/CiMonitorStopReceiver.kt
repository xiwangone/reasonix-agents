package com.reasonix.agents.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * CI 监控停止接收器（2026-08-07）。
 *
 * Android 12+ 禁止从通知栏 action 通过 PendingIntent.getService 启动后台 service
 * （点击「停止」按钮会静默失败）。通知按钮改发广播，由本接收器停止前台服务。
 */
class CiMonitorStopReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        CiMonitorService.stop(context)
    }
}

package com.reasonix.agents.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.reasonix.agents.MainActivity
import com.reasonix.agents.R
import com.reasonix.agents.data.CiMonitorStore
import com.reasonix.agents.data.api.GitHubCiApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * CI 监控悬浮球服务（系统级悬浮窗）。
 * 定时轮询 GitHub Actions 最新一次运行，用颜色表示状态：
 *   运行中=橙 成功=绿 失败=红 排队=蓝 取消=灰 未知=暗灰
 * 支持拖动、点击展开/收起详情、手动刷新。
 */
class CiMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var bubble: FrameLayout? = null
    private var expanded = false
    private var currentState = "unknown"
    private var lastRunText = "—"
    private var lastError: String? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        private const val CHANNEL_ID = "ci_monitor"
        private const val NOTIF_ID = 1001
        private const val ACTION_STOP = "com.reasonix.agents.STOP_CI_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, CiMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CiMonitorService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("CI 监控运行中"))
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addBubble()
        scheduleRefresh(0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        bubble?.let { runCatching { windowManager?.removeView(it) } }
        bubble = null
        super.onDestroy()
    }

    // ── 悬浮球 ──

    private fun addBubble() {
        val settings = CiMonitorStore.load(this)
        if (bubble != null) return

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.TRANSPARENT)

        // 状态圆点（外层）
        val dot = View(this)
        dot.layoutParams = FrameLayout.LayoutParams(56, 56, Gravity.CENTER)
        dot.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(stateColor(currentState))
        }
        root.addView(dot)

        // 状态文字（内层 "CI"）
        val label = TextView(this)
        label.text = "CI"
        label.setTextColor(Color.WHITE)
        label.textSize = 13f
        label.gravity = Gravity.CENTER
        label.layoutParams = FrameLayout.LayoutParams(56, 56, Gravity.CENTER)
        root.addView(label)

        // 详情文字（展开时显示）
        val detail = TextView(this)
        detail.text = lastRunText
        detail.setTextColor(Color.WHITE)
        detail.textSize = 11f
        detail.gravity = Gravity.CENTER
        detail.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        detail.visibility = if (expanded) View.VISIBLE else View.GONE
        detail.setPadding(0, 4, 0, 0)
        root.addView(detail)

        root.setOnClickListener { toggleExpand() }
        root.setOnLongClickListener {
            stopSelf()
            true
        }
        setupDrag(root)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 240
        }

        bubble = root
        runCatching { windowManager?.addView(root, params) }
        refreshNow()
    }

    private fun setupDrag(view: View) {
        view.setOnTouchListener { _, event ->
            val params = view.layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    runCatching { windowManager?.updateViewLayout(view, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpand() {
        expanded = !expanded
        bubble?.let { b ->
            // 更新详情可见性：重建详情文本
            val detail = b.getChildAt(2) as TextView
            detail.text = detailText()
            detail.visibility = if (expanded) View.VISIBLE else View.GONE
        }
    }

    // ── 状态与刷新 ──

    private fun detailText(): String = buildString {
        append(lastRunText)
        if (expanded) {
            lastError?.let { append("\n").append(it) }
        }
    }

    private fun scheduleRefresh(delayMs: Long) {
        handler.postDelayed({ refreshNow(); scheduleRefresh(CiMonitorStore.load(this).intervalMs) }, delayMs)
    }

    private fun refreshNow() {
        val s = CiMonitorStore.load(this)
        if (s.githubToken.isBlank()) {
            currentState = "unknown"
            lastRunText = "未配置 token"
            lastError = "请在设置中填写 GitHub Token"
            updateBubble()
            return
        }
        scope.launch {
            val api = GitHubCiApi(s.githubToken)
            val run = api.getLatestRun(s.owner, s.repo)
            if (run == null) {
                currentState = "unknown"
                lastRunText = "查询失败"
                lastError = "无法获取 CI 状态（token/仓库错误或网络）"
            } else {
                currentState = run.state
                lastRunText = runLabel(run)
                lastError = null
            }
            updateBubble()
        }
    }

    private fun runLabel(run: GitHubCiApi.CiRun): String {
        val branch = run.branch.ifEmpty { "?" }
        val sha = if (run.headSha.length >= 7) run.headSha.take(7) else run.headSha
        return when (run.state) {
            "success" -> "✓ $branch $sha 成功"
            "failure" -> "✗ $branch $sha 失败"
            "cancelled" -> "— $branch $sha 已取消"
            "running" -> "◌ $branch $sha 运行中"
            "queued" -> "… $branch $sha 排队中"
            else -> "$branch $sha"
        }
    }

    private fun updateBubble() {
        bubble?.let { b ->
            val dot = b.getChildAt(0) as View
            (dot.background as? android.graphics.drawable.GradientDrawable)?.setColor(stateColor(currentState))
            val detail = b.getChildAt(2) as TextView
            detail.text = detailText()
            if (expanded) {
                detail.visibility = View.VISIBLE
            }
        }
        // 更新通知
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(NOTIF_ID, buildNotification("CI: $lastRunText")) }
    }

    private fun stateColor(state: String): Int = when (state) {
        "success" -> Color.rgb(64, 160, 96)     // 绿
        "failure" -> Color.rgb(224, 70, 54)     // 红
        "running" -> Color.rgb(234, 136, 0)     // 橙
        "queued" -> Color.rgb(59, 130, 246)     // 蓝
        "cancelled" -> Color.rgb(154, 152, 150) // 灰
        else -> Color.rgb(90, 84, 82)           // 暗灰
    }

    // ── 通知 ──

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CI 监控",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, CiMonitorService::class.java).setAction(ACTION_STOP)
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("CI 监控")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_ci_monitor)
            .setContentIntent(openPi)
            .addAction(0, "停止", stopPi)
            .setOngoing(true)
            .build()
    }
}

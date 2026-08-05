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
 * 定时轮询 GitHub Actions 最新一次运行：
 * - 空闲状态显示 24dp 状态圆点（红=失败 / 绿=成功 / 黄=运行中）；
 * - 点击展开 180×44dp 半透明黑底圆角面板，展示「CI: 运行中/成功/失败」+ 上次构建时间；
 * - 再点击缩回圆点；支持拖动（位置保持），长按停止服务。
 */
class CiMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var bubble: FrameLayout? = null
    private var expanded = false
    private var currentState = "unknown"
    private var lastRunText = "—"
    private var lastRunTimeText = "—"

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

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
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

    /** dp → px（批 C-5：悬浮球尺寸按密度换算）。 */
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ── 悬浮球 ──

    private fun addBubble() {
        if (bubble != null) return

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.TRANSPARENT)

        // 状态圆点（空闲态，批七：24dp 小圆，红=失败 / 绿=成功 / 黄=运行中）
        val dot = View(this)
        dot.layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
        dot.background =
            android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(stateColor(currentState))
            }
        root.addView(dot)

        // 展开面板（点击后，批七：180×44dp 黑底半透明 alpha=0.6 + 圆角 10dp）
        val panel = FrameLayout(this)
        panel.layoutParams = FrameLayout.LayoutParams(dp(180), dp(44), Gravity.CENTER)
        panel.background =
            android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                // Color.BLACK.copy(alpha = 0.6f) → 0x99 000000
                setColor(Color.argb(153, 0, 0, 0))
            }
        val stateText = TextView(this)
        stateText.text = statusLine()
        stateText.setTextColor(Color.WHITE)
        stateText.textSize = 13f
        stateText.gravity = Gravity.CENTER
        stateText.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            )
        panel.addView(stateText)
        val timeText = TextView(this)
        timeText.text = "上次构建: $lastRunTimeText"
        timeText.setTextColor(Color.argb(210, 255, 255, 255))
        timeText.textSize = 10f
        timeText.gravity = Gravity.CENTER
        timeText.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            )
        panel.addView(timeText)
        panel.visibility = View.GONE
        root.addView(panel)

        root.setOnClickListener { toggleExpand() }
        root.setOnLongClickListener {
            stopSelf()
            true
        }
        setupDrag(root)

        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT,
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

                else -> {
                    false
                }
            }
        }
    }

    private fun toggleExpand() {
        expanded = !expanded
        bubble?.let { b ->
            // 圆点（child 0）与展开面板（child 1）互斥显示
            b.getChildAt(0).visibility = if (expanded) View.GONE else View.VISIBLE
            val panel = b.getChildAt(1) as FrameLayout
            panel.visibility = if (expanded) View.VISIBLE else View.GONE
            if (expanded) {
                (panel.getChildAt(0) as TextView).text = statusLine()
                (panel.getChildAt(1) as TextView).text = "上次构建: $lastRunTimeText"
            }
        }
    }

    // ── 状态与刷新 ──

    /** 展开面板状态文字：「CI: 运行中/成功/失败」（批七）。 */
    private fun statusLine(): String =
        "CI: " +
            when (currentState) {
                "success" -> "成功"
                "failure" -> "失败"
                "cancelled" -> "已取消"
                "running", "queued" -> "运行中"
                else -> "未知"
            }

    private fun scheduleRefresh(delayMs: Long) {
        handler.postDelayed({
            refreshNow()
            scheduleRefresh(CiMonitorStore.load(this).intervalMs)
        }, delayMs)
    }

    private fun refreshNow() {
        val s = CiMonitorStore.load(this)
        if (s.githubToken.isBlank()) {
            currentState = "unknown"
            lastRunText = "未配置 token"
            lastRunTimeText = "—"
            updateBubble()
            return
        }
        scope.launch {
            val api = GitHubCiApi(s.githubToken)
            val run = api.getLatestRun(s.owner, s.repo)
            if (run == null) {
                currentState = "unknown"
                lastRunText = "查询失败"
                lastRunTimeText = "—"
            } else {
                currentState = run.state
                lastRunText = runLabel(run)
                lastRunTimeText = formatRunTime(run.createdAt)
            }
            updateBubble()
        }
    }

    /** 上次构建时间：GitHub ISO8601（UTC）→ 本地时区 "MM-dd HH:mm"。 */
    private fun formatRunTime(iso: String): String {
        if (iso.isBlank()) return "—"
        return try {
            val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            inFmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val outFmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US)
            inFmt.parse(iso)?.let { outFmt.format(it) } ?: "—"
        } catch (e: Exception) {
            "—"
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
            if (expanded) {
                val panel = b.getChildAt(1) as FrameLayout
                (panel.getChildAt(0) as TextView).text = statusLine()
                (panel.getChildAt(1) as TextView).text = "上次构建: $lastRunTimeText"
            }
        }
        // 更新通知
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(NOTIF_ID, buildNotification("CI: $lastRunText")) }
    }

    /** 三色状态映射（批七）：绿=成功 红=失败 黄=运行中；排队归运行中、取消归失败；未知为灰。 */
    private fun stateColor(state: String): Int =
        when (state) {
            "success" -> Color.rgb(64, 160, 96)

            // 绿
            "failure" -> Color.rgb(224, 70, 54)

            // 红
            "cancelled" -> Color.rgb(224, 70, 54)

            // 红（已取消归失败色）
            "running", "queued" -> Color.rgb(230, 190, 40)

            // 黄
            else -> Color.rgb(140, 140, 140) // 灰（未知/未配置）
        }

    // ── 通知 ──

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "CI 监控",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, CiMonitorService::class.java).setAction(ACTION_STOP)
        val stopPi =
            PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi =
            PendingIntent.getActivity(
                this,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }
        return builder
            .setContentTitle("CI 监控")
            .setContentText(text)
            // 批 C-5：通知小图标与悬浮球统一为 app 图标风格（ic_stat_reasonix）
            .setSmallIcon(R.drawable.ic_stat_reasonix)
            .setContentIntent(openPi)
            .addAction(0, "停止", stopPi)
            .setOngoing(true)
            .build()
    }
}

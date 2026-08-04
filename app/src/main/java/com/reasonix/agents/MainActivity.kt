package com.reasonix.agents

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.CiMonitorStore
import com.reasonix.agents.service.CiMonitorService
import com.reasonix.agents.ui.screen.ChatScreen
import com.reasonix.agents.ui.screen.ServerConfigScreen
import com.reasonix.agents.ui.theme.DarkPalette
import com.reasonix.agents.ui.theme.LightPalette
import com.reasonix.agents.ui.theme.LocalPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            var settings by remember { mutableStateOf(AppSettingsStore.load(context)) }
            var ciSettings by remember { mutableStateOf(CiMonitorStore.load(context)) }
            val systemDark = isSystemInDarkTheme()
            val palette = when (settings.themeMode) {
                1 -> LightPalette
                2 -> DarkPalette
                else -> if (systemDark) DarkPalette else LightPalette
            }

            CompositionLocalProvider(LocalPalette provides palette) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = palette.bg
            ) {
                var serverConfigured by remember { mutableStateOf(false) }
                var serverUrl by remember { mutableStateOf("http://127.0.0.1:8920") }
                var serverCredentials by remember { mutableStateOf<Pair<String, String>?>(null) }

                if (!serverConfigured) {
                    ServerConfigScreen(
                        onConnect = { url, credentials ->
                            serverUrl = url
                            serverCredentials = credentials
                            serverConfigured = true
                        }
                    )
                } else {
                    ChatScreen(
                        initialServerUrl = serverUrl,
                        initialCredentials = serverCredentials,
                        onSettingsChanged = { newSettings ->
                            settings = newSettings
                            AppSettingsStore.save(context, newSettings)
                        },
                        onCiSettingsChanged = { newCi ->
                            ciSettings = newCi
                            CiMonitorStore.save(context, newCi)
                            syncCiMonitor(context, newCi)
                        }
                    )
                }
            }
            }
        }
    }

    /** 根据设置启动/停止 CI 悬浮窗；未授权悬浮窗权限时引导授权 */
    private fun syncCiMonitor(context: android.content.Context, s: CiMonitorStore.CiSettings) {
        if (s.enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                // 引导用户去授权悬浮窗
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            }
            CiMonitorService.start(context)
        } else {
            CiMonitorService.stop(context)
        }
    }
}

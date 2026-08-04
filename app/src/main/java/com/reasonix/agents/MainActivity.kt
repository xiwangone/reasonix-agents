package com.reasonix.agents

import android.os.Bundle
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
                        }
                    )
                }
            }
            }
        }
    }
}

package com.reasonix.agents

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.reasonix.agents.ui.screen.ChatScreen
import com.reasonix.agents.ui.screen.ServerConfigScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Reasonix 暗色主题 — 覆盖 Material3 默认色板
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1C1A1B) // --bg
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
                    ChatScreen(initialServerUrl = serverUrl, initialCredentials = serverCredentials)
                }
            }
        }
    }
}

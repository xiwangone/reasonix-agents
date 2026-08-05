package com.reasonix.agents

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.CiMonitorStore
import com.reasonix.agents.service.CiMonitorService
import com.reasonix.agents.ui.navigation.Screens
import com.reasonix.agents.ui.screen.ChatScreen
import com.reasonix.agents.ui.screen.FilesScreen
import com.reasonix.agents.ui.screen.ServerConfigScreen
import com.reasonix.agents.ui.screen.SettingsScreen
import com.reasonix.agents.ui.theme.DarkPalette
import com.reasonix.agents.ui.theme.LightPalette
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.viewmodel.ChatViewModel

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
                    // 启动引导：未配置服务器时先进入 ServerConfigScreen
                    ServerConfigScreen(
                        onConnect = { url, credentials ->
                            serverUrl = url
                            serverCredentials = credentials
                            serverConfigured = true
                        }
                    )
                } else {
                    // 主框架：底部 Tab 导航（Chat / Files / Settings）
                    ReasonixApp(
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

// ═══════════════════════════════════════════════
// 主框架 — 手机底部 Tab 导航
// ═══════════════════════════════════════════════

/**
 * 主框架：Scaffold(bottomBar = NavigationBar) + NavHost。
 *
 * - Chat 为 startDestination；顶层切换使用
 *   `popUpTo(start){saveState=true} + launchSingleTop + restoreState`，
 *   切 Tab 后各页状态（滚动位置、输入等）保留。
 * - ChatViewModel 在 Activity 级创建并共享：切 Tab 不销毁
 *   会话状态（消息 / 流式 / SSE 集合协程）。
 */
@Composable
private fun ReasonixApp(
    initialServerUrl: String,
    initialCredentials: Pair<String, String>?,
    onSettingsChanged: (AppSettingsStore.Settings) -> Unit,
    onCiSettingsChanged: (CiMonitorStore.CiSettings) -> Unit
) {
    val chatViewModel: ChatViewModel = viewModel()
    val navController = rememberNavController()

    val palette = LocalPalette.current

    // 顶层导航：跳转指定 Tab，同时保留/恢复各 Tab 状态
    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.bg,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            NavigationBar(containerColor = palette.bg2) {
                Screens.tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTopLevel(tab.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.accent,
                            selectedTextColor = palette.fg,
                            indicatorColor = palette.accentS,
                            unselectedIconColor = palette.muted,
                            unselectedTextColor = palette.muted2
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screens.CHAT) {
                ChatScreen(
                    initialServerUrl = initialServerUrl,
                    initialCredentials = initialCredentials,
                    onNavigateToSettings = { navigateToTopLevel(Screens.SETTINGS) },
                    viewModel = chatViewModel
                )
            }

            composable(Screens.FILES) {
                val state by chatViewModel.uiState.collectAsState()
                FilesScreen(messages = state.messages)
            }

            composable(Screens.SETTINGS) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsScreen(
                    serverUrl = state.serverUrl,
                    status = state.status,
                    models = state.models,
                    currentModel = state.currentModel,
                    systemPrompt = state.systemPrompt,
                    settings = state.settings,
                    ciSettings = state.ciSettings,
                    onModelSelect = { model -> chatViewModel.setModel(model) },
                    onCiSettingsChange = { newCi ->
                        chatViewModel.updateCiSettings(newCi)
                        onCiSettingsChanged(newCi)
                    },
                    onThemeModeChange = { mode ->
                        chatViewModel.updateThemeMode(mode)
                        onSettingsChanged(state.settings.copy(themeMode = mode))
                    },
                    onShowReasoningChange = { show ->
                        chatViewModel.updateShowReasoning(show)
                        onSettingsChanged(state.settings.copy(showReasoning = show))
                    },
                    onShowTokensChange = { show ->
                        chatViewModel.updateShowTokens(show)
                        onSettingsChanged(state.settings.copy(showTokens = show))
                    },
                    onClose = null
                )
            }
        }
    }
}

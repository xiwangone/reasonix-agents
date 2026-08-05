package com.reasonix.agents

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reasonix.agents.data.AppSettingsStore
import com.reasonix.agents.data.AuthInfo
import com.reasonix.agents.data.CiMonitorStore
import com.reasonix.agents.service.CiMonitorService
import com.reasonix.agents.ui.navigation.Screens
import com.reasonix.agents.ui.screen.AboutScreen
import com.reasonix.agents.ui.screen.ChatScreen
import com.reasonix.agents.ui.screen.FilesScreen
import com.reasonix.agents.ui.screen.ServerConfigScreen
import com.reasonix.agents.ui.screen.SettingsCiScreen
import com.reasonix.agents.ui.screen.SettingsDisplayScreen
import com.reasonix.agents.ui.screen.SettingsModelScreen
import com.reasonix.agents.ui.screen.SettingsNetworkScreen
import com.reasonix.agents.ui.screen.SettingsScreen
import com.reasonix.agents.ui.screen.SettingsServerScreen
import com.reasonix.agents.ui.screen.SettingsThemeScreen
import com.reasonix.agents.ui.theme.DarkPalette
import com.reasonix.agents.ui.theme.LightPalette
import com.reasonix.agents.ui.theme.LocalPalette
import com.reasonix.agents.ui.theme.MaterialDarkPalette
import com.reasonix.agents.ui.theme.MaterialLightPalette
import com.reasonix.agents.ui.viewmodel.ChatViewModel
import com.reasonix.agents.util.AppIconSwitcher
import com.reasonix.agents.util.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 批 B-14：API 33+ 请求通知权限（任务完成提醒）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        NotificationHelper.ensureChannel(this)
        setContent {
            val context = LocalContext.current
            var settings by remember { mutableStateOf(AppSettingsStore.load(context)) }
            var ciSettings by remember { mutableStateOf(CiMonitorStore.load(context)) }
            val systemDark = isSystemInDarkTheme()
            // 批 A-2 主题预设体系：风格（品牌紫蓝/Material）× 明暗（跟随系统/浅/深）
            val dark = when (settings.themeMode) {
                AppSettingsStore.THEME_MODE_LIGHT -> false
                AppSettingsStore.THEME_MODE_DARK -> true
                else -> systemDark
            }
            val palette = when (settings.themePreset) {
                AppSettingsStore.THEME_PRESET_MATERIAL -> if (dark) MaterialDarkPalette else MaterialLightPalette
                else -> if (dark) DarkPalette else LightPalette
            }
            // 批 B-13：主题变化时切换 launcher 图标
            LaunchedEffect(settings.themePreset, settings.themeMode) {
                AppIconSwitcher.apply(context, settings.themePreset, settings.themeMode)
            }

            CompositionLocalProvider(LocalPalette provides palette) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.bg
                ) {
                var serverConfigured by remember { mutableStateOf(false) }
                var serverUrl by remember { mutableStateOf("http://127.0.0.1:8920") }
                var serverAuth by remember { mutableStateOf<AuthInfo?>(null) }

                if (!serverConfigured) {
                    // 启动引导：未配置服务器时先进入连接页（登录页，批 A-5 含主题/语言入口）
                    ServerConfigScreen(
                        settings = settings,
                        onSettingsChange = { newSettings ->
                            settings = newSettings
                            AppSettingsStore.save(context, newSettings)
                        },
                        onConnect = { url, auth ->
                            serverUrl = url
                            serverAuth = auth
                            serverConfigured = true
                        }
                    )
                } else {
                    // 主框架：底部 Tab 导航（Chat / Files / Settings）+ About 页
                    ReasonixApp(
                        settings = settings,
                        initialServerUrl = serverUrl,
                        initialAuth = serverAuth,
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
    settings: AppSettingsStore.Settings,
    initialServerUrl: String,
    initialAuth: AuthInfo?,
    onSettingsChanged: (AppSettingsStore.Settings) -> Unit,
    onCiSettingsChanged: (CiMonitorStore.CiSettings) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(application, initialServerUrl, initialAuth)
    )
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

            // About 页与设置二级界面隐藏底部导航栏（第四批：设置组件化）
            if (currentRoute != Screens.ABOUT && currentRoute?.startsWith("settings_") != true) {
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
                                indicatorColor = palette.accent.copy(alpha = 0.16f),
                                unselectedIconColor = palette.muted,
                                unselectedTextColor = palette.muted
                            )
                        )
                    }
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
                    initialAuth = initialAuth,
                    onNavigateToSettings = { navigateToTopLevel(Screens.SETTINGS) },
                    onNavigateToAbout = { navController.navigate(Screens.ABOUT) },
                    onNavigateToServerConfig = { navController.navigate(Screens.SERVER_CONFIG) },
                    viewModel = chatViewModel
                )
            }
            composable(Screens.FILES) {
                FilesScreen(
                    messages = chatViewModel.uiState.collectAsState().value.messages
                )
            }
            composable(Screens.SETTINGS) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsScreen(
                    systemPrompt = state.systemPrompt,
                    customPrompts = state.customPrompts,
                    currentPromptId = state.currentPromptId,
                    onAddPrompt = { content, select -> chatViewModel.addPrompt(content, select) },
                    onRemovePrompt = { id -> chatViewModel.removePrompt(id) },
                    onSetCurrentPrompt = { id -> chatViewModel.setCurrentPrompt(id) },
                    onOpenTheme = { navController.navigate(Screens.SETTINGS_THEME) },
                    onOpenModel = { navController.navigate(Screens.SETTINGS_MODEL) },
                    onOpenDisplay = { navController.navigate(Screens.SETTINGS_DISPLAY) },
                    onOpenNetwork = { navController.navigate(Screens.SETTINGS_NETWORK) },
                    onOpenServerInfo = { navController.navigate(Screens.SETTINGS_SERVER) },
                    onOpenCi = { navController.navigate(Screens.SETTINGS_CI) },
                    onOpenAbout = { navController.navigate(Screens.ABOUT) },
                    onClose = null
                )
            }
            composable(Screens.ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            // ── 设置二级界面（第四批：设置组件化）──
            composable(Screens.SETTINGS_THEME) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsThemeScreen(
                    settings = state.settings,
                    onSettingsChange = { newSettings ->
                        chatViewModel.updateSettings(newSettings)
                        onSettingsChanged(newSettings)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screens.SETTINGS_MODEL) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsModelScreen(
                    models = state.models,
                    customModels = state.customModels,
                    currentModel = state.currentModel,
                    onModelSelect = { model -> chatViewModel.setModel(model) },
                    onRefreshModels = { chatViewModel.reloadModels() },
                    onAddCustomModel = { model -> chatViewModel.addCustomModel(model) },
                    onRemoveCustomModel = { id -> chatViewModel.removeCustomModel(id) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screens.SETTINGS_DISPLAY) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsDisplayScreen(
                    settings = state.settings,
                    onSettingsChange = { newSettings ->
                        chatViewModel.updateSettings(newSettings)
                        onSettingsChanged(newSettings)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screens.SETTINGS_NETWORK) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsNetworkScreen(
                    settings = state.settings,
                    onSettingsChange = { newSettings ->
                        chatViewModel.updateSettings(newSettings)
                        onSettingsChanged(newSettings)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screens.SETTINGS_SERVER) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsServerScreen(
                    serverUrl = state.serverUrl,
                    status = state.status,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screens.SETTINGS_CI) {
                val state by chatViewModel.uiState.collectAsState()
                SettingsCiScreen(
                    ciSettings = state.ciSettings,
                    onCiSettingsChange = { newCi ->
                        chatViewModel.updateCiSettings(newCi)
                        onCiSettingsChanged(newCi)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            // ── 服务器配置页（批 C-1：配置列表「新增配置」跳转新建；连接成功后切回 Chat 并应用）──
            composable(Screens.SERVER_CONFIG) {
                ServerConfigScreen(
                    settings = settings,
                    onSettingsChange = onSettingsChanged,
                    onConnect = { url, auth ->
                        navController.popBackStack()
                        chatViewModel.configureServer(url, auth)
                    }
                )
            }
        }
    }
}

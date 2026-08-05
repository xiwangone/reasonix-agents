package com.reasonix.agents.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部 Tab 定义：路由常量 + 中文标签 + 选中/未选中双态图标。
 *
 * 借鉴 opencode_android_client 的 Screen 枚举设计（MainActivity.kt PhoneLayout），
 * 主框架（MainActivity.ReasonixApp）据此渲染 NavigationBar「导航栏」与 NavHost「导航宿主」。
 */
data class Tab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/** 顶层路由常量（NavigationBar 页签 + NavHost destination「目的地」） */
object Screens {
    /** 聊天页 */
    const val CHAT = "chat"

    /** 文件页（批 4 提供轻量文件浏览） */
    const val FILES = "files"

    /** 设置页（从 Chat 内的全屏覆盖层迁入底部 Tab） */
    const val SETTINGS = "settings"

    /** 关于页（批 A-6：设置页/侧边栏/顶栏多入口） */
    const val ABOUT = "about"

    /** 服务器配置页（批 C-1：Chat 左上角「配置」→「新增配置」跳转新建） */
    const val SERVER_CONFIG = "server_config"

    /** 设置二级界面（第四批：设置组件化，点击设置入口进入） */
    const val SETTINGS_THEME = "settings_theme"
    const val SETTINGS_MODEL = "settings_model"
    const val SETTINGS_DISPLAY = "settings_display"
    const val SETTINGS_NETWORK = "settings_network"
    const val SETTINGS_SERVER = "settings_server"
    const val SETTINGS_CI = "settings_ci"

    /** 设置二级界面（第五批）：备份与恢复 / CLI 集成 */
    const val SETTINGS_BACKUP = "settings_backup"
    const val SETTINGS_CLI = "settings_cli"

    /** 起始目的地：Chat */
    val startDestination: String = CHAT

    /** 底部导航栏页签顺序 */
    val tabs: List<Tab> = listOf(
        Tab(CHAT, "聊天", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        Tab(FILES, "文件", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen),
        Tab(SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
}

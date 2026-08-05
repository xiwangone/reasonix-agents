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

    /** 起始目的地：Chat */
    val startDestination: String = CHAT

    /** 底部导航栏页签顺序 */
    val tabs: List<Tab> = listOf(
        Tab(CHAT, "聊天", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        Tab(FILES, "文件", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen),
        Tab(SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
}

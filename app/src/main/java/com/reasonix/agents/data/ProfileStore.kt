package com.reasonix.agents.data

// ═══════════════════════════════════════════════════════════════════
// 2026-08-07：本地资料存储（左上角配置 → 编辑资料）
// 昵称 + Emoji 头像，仅本地生效（不影响服务端认证用户名）。
// ═══════════════════════════════════════════════════════════════════

import android.content.Context

object ProfileStore {
    private const val PREFS_NAME = "reasonix_profile"
    private const val KEY_NAME = "display_name"
    private const val KEY_AVATAR = "avatar_emoji"

    data class Profile(
        val displayName: String = "",
        val avatarEmoji: String = "",
    )

    fun load(context: Context): Profile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Profile(
            displayName = prefs.getString(KEY_NAME, "") ?: "",
            avatarEmoji = prefs.getString(KEY_AVATAR, "") ?: "",
        )
    }

    fun save(context: Context, profile: Profile) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, profile.displayName.trim())
            .putString(KEY_AVATAR, profile.avatarEmoji)
            .apply()
    }
}

package com.reasonix.agents.data.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases 检查（批 A-7 检测更新）。
 * 调用 GET /repos/{owner}/{repo}/releases/latest 获取最新发布版本，
 * 与本地 versionName 对比，有更新时由 UI 弹窗提示下载。
 * 公开仓库无需 token。
 */
class GitHubReleaseApi(
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build(),
) {
    private val gson = Gson()

    data class ReleaseInfo(
        val tagName: String = "",
        val name: String = "",
        @SerializedName("html_url") val htmlUrl: String = "",
        @SerializedName("published_at") val publishedAt: String = "",
    )

    /**
     * 查询指定仓库最新 Release。
     * @return ReleaseInfo；仓库暂无 Release（HTTP 404）返回 null（= 无更新）。
     * @throws Exception 网络错误 / 非 2xx（如 403 限流、5xx），由 UI 提示「网络错误，请稍后重试」。
     */
    suspend fun checkLatest(repo: String = DEFAULT_REPO): ReleaseInfo? =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url("https://api.github.com/repos/$repo/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .build()
            try {
                val resp = client.newCall(request).execute()
                // 404 = 仓库暂无 Release：视为「无更新」，不抛异常
                if (resp.code == 404) {
                    resp.close()
                    return@withContext null
                }
                if (!resp.isSuccessful) {
                    resp.close()
                    throw java.io.IOException("HTTP ${resp.code}")
                }
                val body = resp.body?.string() ?: return@withContext null
                resp.close()
                gson.fromJson(body, ReleaseInfo::class.java)
            } catch (e: Exception) {
                // 网络异常（DNS / 超时 / IO）向上抛出，由 UI 提示网络错误
                throw e
            }
        }

    companion object {
        const val DEFAULT_REPO = "xiwangone/reasonix-agents"

        /**
         * 版本号对比（支持 "1.0"、"v1.0.1"、"1.0.1-beta" 等形态）。
         * @return 1=latest 更新；0=相同；-1=latest 更旧
         */
        fun compareVersions(
            current: String,
            latest: String,
        ): Int {
            fun parse(v: String): List<Int> =
                v
                    .trim()
                    .trimStart('v', 'V')
                    .split('.', '-', '_')
                    .mapNotNull { it.toIntOrNull() }

            val cur = parse(current)
            val lat = parse(latest)
            val maxLen = maxOf(cur.size, lat.size)
            for (i in 0 until maxLen) {
                val a = cur.getOrElse(i) { 0 }
                val b = lat.getOrElse(i) { 0 }
                if (a != b) return if (a < b) 1 else -1
            }
            return 0
        }
    }
}

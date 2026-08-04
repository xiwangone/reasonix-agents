package com.reasonix.agents.data.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * GitHub Actions CI 状态查询（只读，GET /repos/{owner}/{repo}/actions/runs）。
 * token 仅用于请求头 Authorization，不落日志。
 */
class GitHubCiApi(
    private val token: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()

    /** 最新一次 CI 运行状态 */
    data class CiRun(
        val id: Long = 0,
        val name: String = "",
        @SerializedName("head_sha") val headSha: String = "",
        @SerializedName("head_branch") val branch: String = "",
        val status: String = "unknown",       // queued | in_progress | completed
        val conclusion: String? = null,       // success | failure | cancelled | null(运行中)
        @SerializedName("created_at") val createdAt: String = "",
        @SerializedName("updated_at") val updatedAt: String = ""
    ) {
        /** 统一状态：success / failure / running / queued / cancelled / unknown */
        val state: String
            get() = when {
                status == "completed" && conclusion == "success" -> "success"
                status == "completed" && conclusion == "failure" -> "failure"
                status == "completed" && conclusion == "cancelled" -> "cancelled"
                status == "in_progress" -> "running"
                status == "queued" -> "queued"
                else -> "unknown"
            }
    }

    suspend fun getLatestRun(owner: String, repo: String): CiRun? = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs?per_page=1"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        try {
            val resp = client.newCall(request).execute()
            if (!resp.isSuccessful) {
                resp.close()
                return@withContext null
            }
            val body = resp.body?.string() ?: return@withContext null
            resp.close()
            val arr = gson.fromJson(body, Array<CiRun>::class.java) ?: return@withContext null
            arr.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

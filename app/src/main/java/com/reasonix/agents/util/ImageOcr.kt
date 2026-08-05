package com.reasonix.agents.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * 图片工具（第六批：图片发送，本地 OCR 优先）。
 *
 * - 相册图片先拷贝到应用内部存储（避免选择器 Uri 权限失效），消息展示用本地文件路径；
 * - 采样解码控制内存占用（防 OOM）；
 * - 本地 OCR 使用 ML Kit 中文 bundled 模型（离线识别，不依赖后端视觉能力）。
 */
object ImageOcr {

    /** 内部存储图片目录。 */
    private fun imageDir(context: Context): File =
        File(context.filesDir, "images").apply { mkdirs() }

    /**
     * 拷贝相册图片到应用内部存储，返回本地文件绝对路径；失败返回 null。
     * 拷贝而非直接引用 Uri：选择器授权是一次性的，消息后续渲染需要稳定可读的文件。
     */
    fun copyToInternal(context: Context, uri: Uri): String? = try {
        val dest = File(imageDir(context), "img_${System.currentTimeMillis()}.jpg")
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { ins ->
            FileOutputStream(dest).use { out -> ins.copyTo(out) }
        }
        dest.absolutePath
    } catch (e: Exception) {
        null
    }

    /**
     * 采样解码本地图片（最长边不超过 [maxDim]，防 OOM）；失败返回 null。
     * 返回 ARGB_8888 位图（ML Kit 需要非硬件位图）。
     */
    fun decodeSampledBitmap(path: String, maxDim: Int = 2048): Bitmap? {
        return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(path, opts) ?: return null
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        null
    }
    }

    /**
     * 本地 OCR：ML Kit 中文识别（bundled 模型，离线可用）。
     * 返回识别文本（可能为空白字符串）；识别失败返回 null。
     */
    suspend fun recognize(context: Context, bitmap: Bitmap): String? {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result.text)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                cont.invokeOnCancellation { recognizer.close() }
            }
        } catch (e: Exception) {
            try {
                recognizer.close()
            } catch (_: Exception) {
                // ignore
            }
            null
        }
    }
}

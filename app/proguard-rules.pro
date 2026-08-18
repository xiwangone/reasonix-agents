# ═══════════════════════════════════════════════
# Reasonix Agents ProGuard Rules
# 2026-08-18：Release 启用混淆
# ═══════════════════════════════════════════════

# ── 保留行号信息（崩溃堆栈可追溯）──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Gson / JSON 序列化 ──
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.reasonix.agents.data.model.** { *; }
-keep class com.reasonix.agents.data.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ── Markwon ──
-keep class io.noties.markwon.** { *; }
-keep class io.noties.prism4j.** { *; }
-dontwarn com.caverock.androidsvg.**
-dontwarn pl.droidsonroids.gif.**
-dontwarn com.squareup.picasso.**
-dontwarn jp.wasabeef.glide.**

# ── Coil 图片加载 ──
-keep class coil.** { *; }
-dontwarn coil.**

# ── ML Kit OCR ──
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── AndroidX Compose ─>
-keep class androidx.compose.** { *; }

# ── Material3 ──
-keep class com.google.android.material.** { *; }

# ── Enums ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Parcelable ─>
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── R 文件（资源引用）──
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ── SSE 事件类（反射解析）──
-keep class com.reasonix.agents.data.model.SseEvent** { *; }
-keep class com.reasonix.agents.data.model.TurnBlock** { *; }
-keep class com.reasonix.agents.data.model.ChatItem** { *; }

# ── WebDav ──
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**

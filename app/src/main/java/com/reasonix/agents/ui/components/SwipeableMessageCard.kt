package com.reasonix.agents.ui.components

// ═══════════════════════════════════════════════════════════════════
// 2026-08-18：消息滑动手势卡片
// 左滑：引用回复（将消息内容注入输入框）
// 右滑：收藏消息（存本地）
// ═══════════════════════════════════════════════════════════════════

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.ui.theme.LocalPalette
import kotlin.math.roundToInt

/**
 * 可滑动的消息卡片包装器。
 *
 * @param content 消息内容（气泡等）
 * @param onSwipeLeft 左滑触发的回调（引用回复）
 * @param onSwipeRight 右滑触发的回调（收藏）
 * @param swipeThreshold 滑动触发阈值（dp）
 */
@Composable
fun SwipeableMessageCard(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    swipeThreshold: Float = 120f,
    content: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { swipeThreshold.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwiping by remember { mutableStateOf(false) }

    // 动画：松手后回弹
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isSwiping) offsetX else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "swipeOffset",
    )

    // 左滑比例（正值）和右滑比例（正值）
    val leftSwipeProgress = if (offsetX < 0) (-offsetX / swipeThresholdPx).coerceIn(0f, 1f) else 0f
    val rightSwipeProgress = if (offsetX > 0) (offsetX / swipeThresholdPx).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 底层：左右操作指示器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左滑：引用回复指示器
            if (onSwipeLeft != null && leftSwipeProgress > 0f) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = leftSwipeProgress * 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "引用回复",
                        tint = p.accent.copy(alpha = leftSwipeProgress),
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 右滑：收藏指示器
            if (onSwipeRight != null && rightSwipeProgress > 0f) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(p.warning.copy(alpha = rightSwipeProgress * 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "收藏",
                        tint = p.warning.copy(alpha = rightSwipeProgress),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // 上层：可滑动的内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffsetX }
                .pointerInput(onSwipeLeft, onSwipeRight) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isSwiping = true
                        },
                        onDragEnd = {
                            isSwiping = false
                            // 判断是否达到阈值
                            when {
                                offsetX < -swipeThresholdPx -> onSwipeLeft?.invoke()
                                offsetX > swipeThresholdPx -> onSwipeRight?.invoke()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            isSwiping = false
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(
                                -swipeThresholdPx * 1.5f,
                                swipeThresholdPx * 1.5f
                            )
                        },
                    )
                },
        ) {
            content()
        }
    }
}

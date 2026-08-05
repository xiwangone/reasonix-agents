package com.reasonix.agents.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.R
import com.reasonix.agents.ui.theme.LocalPalette

// ═══════════════════════════════════════════════
// 调色板
// ═══════════════════════════════════════════════

private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Violet: Color @Composable get() = LocalPalette.current.violet
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Fg2: Color @Composable get() = LocalPalette.current.fg2
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Panel2: Color @Composable get() = LocalPalette.current.panel2
private val Border: Color @Composable get() = LocalPalette.current.border

// ═══════════════════════════════════════════════
// WelcomeScreen
// ═══════════════════════════════════════════════

/**
 * 欢迎页 — 对应 index.html 的 .welcome 区块。
 * 无消息时展示 Logo + 标题 + 键盘提示 + 示例提问。
 */
@Composable
fun WelcomeScreen(
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Logo（品牌图标）──
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 标题 ──
            Text(
                text = "Reasonix",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Fg,
                letterSpacing = (-0.72).sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── 副标题 ──
            Text(
                text = "AI 编程助手",
                fontSize = 13.sp,
                color = Fg2,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 键盘提示 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        HintBadge(kbd = "/", label = "命令")
                        HintBadge(kbd = "Shift+Tab", label = "Plan")
                        HintBadge(kbd = "Ctrl+Y", label = "YOLO")
                        HintBadge(kbd = "Esc×2", label = "倒带")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 示例提问 ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ExamplePrompt("解释项目结构", onClick = onPromptClick)
                ExamplePrompt("查找并修复错误", onClick = onPromptClick)
                ExamplePrompt("为主模块编写测试", onClick = onPromptClick)
            }
        }
    }
}

/**
 * 键盘提示徽章：`<kbd>key</kbd> label`
 */
@Composable
private fun HintBadge(
    kbd: String,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Panel2,
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        ) {
            Text(
                text = kbd,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Fg2,
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Muted,
        )
    }
}

/**
 * 示例提问卡片：左侧 3dp accent 色条 + 文字，点击后填入输入框并发送。
 */
@Composable
private fun ExamplePrompt(
    text: String,
    onClick: (String) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onClick(text) },
        shape = RoundedCornerShape(6.dp),
        color = Bg2,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Accent),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                color = Fg2,
            )
        }
    }
}

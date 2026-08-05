package com.reasonix.agents.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasonix.agents.data.CustomModelStore.CustomModel
import com.reasonix.agents.data.model.ModelInfo
import com.reasonix.agents.ui.theme.LocalPalette

// 调色板 — 从 LocalPalette 读取（支持主题切换）
private val Bg: Color @Composable get() = LocalPalette.current.bg
private val Bg2: Color @Composable get() = LocalPalette.current.bg2
private val Panel: Color @Composable get() = LocalPalette.current.panel
private val Border: Color @Composable get() = LocalPalette.current.border
private val Accent: Color @Composable get() = LocalPalette.current.accent
private val Fg: Color @Composable get() = LocalPalette.current.fg
private val Muted: Color @Composable get() = LocalPalette.current.muted
private val Muted2: Color @Composable get() = LocalPalette.current.muted2
private val Danger: Color @Composable get() = LocalPalette.current.danger

/**
 * 模型设置二级界面（第四批：设置组件化）。
 * 从设置页「模型」入口进入，含模型下拉切换 + 刷新 + 添加自定义模型 + 删除。
 */
@Composable
fun SettingsModelScreen(
    models: List<ModelInfo>,
    customModels: List<CustomModel>,
    currentModel: String,
    onModelSelect: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onAddCustomModel: (CustomModel) -> Unit,
    onRemoveCustomModel: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showAddModel by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Bg)
                .safeDrawingPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            // ── 顶栏（返回 + 标题）──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Fg)
                }
                Text(
                    text = "模型",
                    fontSize = 20.sp,
                    color = Fg,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 模型（批 B-9 添加模型 / B-10 下拉切换 + 刷新）──
            SectionTitle("当前模型")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 模型下拉：服务端模型 + 自定义模型合并
                ModelDropdown(
                    models = models,
                    customModels = customModels,
                    currentModel = currentModel,
                    onSelect = onModelSelect,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onRefreshModels,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp)),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新模型列表", tint = Muted, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { showAddModel = true },
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg2)
                            .border(1.dp, Border, RoundedCornerShape(8.dp)),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加模型", tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 自定义模型列表（可删除）
            if (customModels.isNotEmpty()) {
                Text(
                    text = "自定义模型（本地）",
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                customModels.forEach { cm ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Panel)
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cm.name,
                                fontSize = 13.sp,
                                color = if (cm.name == currentModel) Accent else Fg,
                                fontWeight = if (cm.name == currentModel) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Text(
                                text =
                                    buildString {
                                        append(if (cm.provider == "builtin") "内置" else "自定义")
                                        if (cm.baseUrl.isNotBlank()) append(" · ${cm.baseUrl}")
                                        append(" · ${compatLabel(cm.compat)}")
                                    },
                                fontSize = 10.sp,
                                color = Muted2,
                            )
                        }
                        IconButton(
                            onClick = { onRemoveCustomModel(cm.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除模型", tint = Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (models.isEmpty() && customModels.isEmpty()) {
                InfoRow("当前模型", currentModel.ifEmpty { "—" })
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "自定义模型保存在本机，用于第三方兼容服务；服务端模型由 reasonix 服务端管理。",
                fontSize = 10.sp,
                color = Muted2,
            )
        }
    }

    // ── 添加模型弹窗（批 B-9）──
    if (showAddModel) {
        AddModelDialog(
            onAdd = { model ->
                onAddCustomModel(model)
                showAddModel = false
            },
            onDismiss = { showAddModel = false },
        )
    }
}

/** 模型下拉（批 B-10）：服务端模型 + 自定义模型合并，选中即回调。 */
@Composable
private fun ModelDropdown(
    models: List<ModelInfo>,
    customModels: List<CustomModel>,
    currentModel: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Bg2)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentModel.ifEmpty { "选择模型" },
                    fontSize = 13.sp,
                    color = if (currentModel.isEmpty()) Muted2 else Fg,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Panel,
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(10.dp)),
        ) {
            // 自定义模型（本地）
            customModels.forEach { cm ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(cm.name, fontSize = 13.sp, color = Fg)
                            Text(
                                "自定义 · ${compatLabel(cm.compat)}",
                                fontSize = 10.sp,
                                color = Muted2,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(cm.name)
                    },
                )
            }
            if (customModels.isNotEmpty() && models.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider(color = Border, thickness = 1.dp)
            }
            // 服务端模型
            models.forEach { m ->
                val label = m.model.ifEmpty { m.ref }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(label, fontSize = 13.sp, color = Fg)
                            if (m.kind.isNotBlank()) {
                                Text(m.kind, fontSize = 10.sp, color = Muted2)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(m.ref)
                    },
                )
            }
            if (customModels.isEmpty() && models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("暂无模型（可点击 + 添加自定义模型）", fontSize = 12.sp, color = Muted2) },
                    onClick = { expanded = false },
                )
            }
        }
    }
}

/**
 * 添加模型弹窗（批 B-9 + C-4）：模型名称 + key（provider/model 兼容格式）+ 其他必要字段。
 * key 例如 "openai/deepseek-v4-flash"、"opencode-zen/deepseek-v4-flash-free"，保存后按 key 独立分组。
 */
@Composable
private fun AddModelDialog(
    onAdd: (CustomModel) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("自定义") }
    var baseUrl by remember { mutableStateOf("") }
    var compat by remember { mutableStateOf("OpenAI") }
    var keyError by remember { mutableStateOf<String?>(null) }

    val providerOptions = listOf("内置", "自定义")
    val compatOptions = listOf("OpenAI", "DeepSeek-Reasonix", "其他")

    fun trySave() {
        val nameTrim = name.trim()
        val keyTrim = key.trim()
        when {
            nameTrim.isBlank() -> {
                keyError = "请填写模型名称"
            }

            keyTrim.isBlank() -> {
                keyError = "请填写 Key（provider/model 格式）"
            }

            !keyTrim.contains("/") -> {
                keyError = "Key 需为 provider/model 格式，例如 openai/deepseek-v4-flash"
            }

            keyTrim.startsWith("/") || keyTrim.endsWith("/") -> {
                keyError = "Key 格式不正确：provider 和 model 均不能为空"
            }

            else -> {
                onAdd(
                    CustomModel(
                        id = nameTrim,
                        name = nameTrim,
                        key = keyTrim,
                        provider = if (provider == "内置") "builtin" else "custom",
                        baseUrl = baseUrl.trim(),
                        compat =
                            when (compat) {
                                "OpenAI" -> "openai"
                                "DeepSeek-Reasonix" -> "deepseek"
                                else -> "other"
                            },
                    ),
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型", color = Fg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 模型名称
                LabeledField("模型名称（必填）") {
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            keyError = null
                        },
                        textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(Panel)
                                .border(1.dp, Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                // Key（provider/model 兼容格式）
                LabeledField("Key（provider/model，必填）") {
                    BasicTextField(
                        value = key,
                        onValueChange = {
                            key = it
                            keyError = null
                        },
                        textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(Panel)
                                .border(1.dp, if (keyError != null) Danger else Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    Text(
                        text = "如 openai/deepseek-v4-flash 或 opencode-zen/deepseek-v4-flash-free，保存后按 key 独立分组",
                        fontSize = 10.sp,
                        color = Muted2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // Provider
                LabeledField("Provider") {
                    SimpleDropdown(
                        label = provider,
                        options = providerOptions,
                        onSelect = { provider = it },
                    )
                }
                // base_url（自定义时）
                if (provider == "自定义") {
                    LabeledField("Base URL（可选）") {
                        BasicTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            textStyle = TextStyle(color = Fg, fontSize = 13.sp),
                            cursorBrush = SolidColor(Accent),
                            singleLine = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Panel)
                                    .border(1.dp, Border, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
                // 兼容方式
                LabeledField("兼容方式") {
                    SimpleDropdown(
                        label = compat,
                        options = compatOptions,
                        onSelect = { compat = it },
                    )
                }
                if (keyError != null) {
                    Text(
                        text = keyError.orEmpty(),
                        fontSize = 11.sp,
                        color = Danger,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    Text(
                        text = "内置：由服务端模型管理；自定义：本地保存，用于第三方兼容服务",
                        fontSize = 10.sp,
                        color = Muted2,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { trySave() }) { Text("保存", color = Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
        },
        containerColor = Panel,
    )
}

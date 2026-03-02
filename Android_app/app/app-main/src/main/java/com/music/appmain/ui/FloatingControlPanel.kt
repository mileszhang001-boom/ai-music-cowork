package com.music.appmain.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.core.api.models.EffectCommands
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Composable
fun FloatingControlPanel(
    signals: StandardizedSignals?,
    sceneDescriptor: SceneDescriptor?,
    effectCommands: EffectCommands?,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onScenarioClick: (SceneScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPopup by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FloatingActionButton(
            onClick = { showPopup = true },
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            containerColor = CarTheme.AccentPurple,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = "⚡",
                fontSize = 24.sp
            )
        }
    }

    if (showPopup) {
        ControlPopup(
            signals = signals,
            sceneDescriptor = sceneDescriptor,
            effectCommands = effectCommands,
            isRunning = isRunning,
            onStart = onStart,
            onStop = onStop,
            onScenarioClick = onScenarioClick,
            onDismiss = { showPopup = false }
        )
    }
}

private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Composable
private fun ControlPopup(
    signals: StandardizedSignals?,
    sceneDescriptor: SceneDescriptor?,
    effectCommands: EffectCommands?,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onScenarioClick: (SceneScenario) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左侧控制栏 - 20%
                    ControlSection(
                        isRunning = isRunning,
                        onStart = onStart,
                        onStop = onStop,
                        onScenarioClick = onScenarioClick,
                        onDismiss = onDismiss,
                        modifier = Modifier
                            .weight(0.2f)
                            .fillMaxHeight()
                    )

                    // 右侧数据栏 - 80%
                    JsonDataSection(
                        signals = signals,
                        sceneDescriptor = sceneDescriptor,
                        effectCommands = effectCommands,
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlSection(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onScenarioClick: (SceneScenario) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
        ) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 主控制按钮
        FloatingActionButton(
            onClick = {
                if (isRunning) onStop() else onStart()
            },
            modifier = Modifier.size(64.dp),
            containerColor = if (isRunning) Color(0xFFF44336) else CarTheme.AccentCyan,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = if (isRunning) "⏹" else "▶",
                fontSize = 28.sp
            )
        }

        Text(
            text = if (isRunning) "停止" else "开始",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "场景验证",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 场景按钮列表
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val scenarios = listOf(
                SceneScenario("rainy_emo", "🌧 雨天emo", "雨天+低落情绪", Color(0xFF5C6BC0)),
                SceneScenario("children_board", "👶 小朋友上车", "检测到儿童乘客", Color(0xFFFF9800)),
                SceneScenario("user_pop", "🎵 我喜欢流行乐", "用户偏好输入", Color(0xFF9C27B0)),
                SceneScenario("beach_vacation", "🏖️ 海边度假", "海边场景+放松", Color(0xFF00BCD4)),
                SceneScenario("romantic_date", "💕 浪漫约会", "夜晚+浪漫氛围", Color(0xFFE91E63)),
                SceneScenario("fatigue_alert", "😴 疲劳提醒", "检测到疲劳", Color(0xFFF44336))
            )

            scenarios.forEach { scenario ->
                ScenarioButton(
                    scenario = scenario,
                    onClick = { onScenarioClick(scenario) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScenarioButton(
    scenario: SceneScenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scenario.color
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = scenario.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun JsonDataSection(
    signals: StandardizedSignals?,
    sceneDescriptor: SceneDescriptor?,
    effectCommands: EffectCommands?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Layer 1
        JsonPanel(
            title = "Layer 1",
            jsonContent = signals?.let { json.encodeToString(it) } ?: "{}",
            modifier = Modifier.weight(1f)
        )

        // Layer 2
        JsonPanel(
            title = "Layer 2",
            jsonContent = sceneDescriptor?.let { json.encodeToString(it) } ?: "{}",
            modifier = Modifier.weight(1f)
        )

        // Layer 3
        JsonPanel(
            title = "Layer 3",
            jsonContent = effectCommands?.let { json.encodeToString(it) } ?: "{}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun JsonPanel(
    title: String,
    jsonContent: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CarTheme.AccentCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarTheme.AccentCyan.copy(alpha = 0.1f))
                    .padding(8.dp)
            )

            val scrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Text(
                text = jsonContent,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                ),
                color = Color(0xFF79C0FF),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(8.dp)
            )
        }
    }
}

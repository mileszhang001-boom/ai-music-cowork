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
            onScenarioClick = { scenario ->
                onScenarioClick(scenario)
                showPopup = false
            },
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
    var selectedJson by remember { mutableStateOf<String?>(null) }

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
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "控制面板",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        if (selectedJson != null) {
                            TextButton(onClick = { selectedJson = null }) {
                                Text("← 返回", color = CarTheme.AccentCyan)
                            }
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 20.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = selectedJson,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                        },
                        label = "json_content"
                    ) { jsonContent ->
                        if (jsonContent != null) {
                            JsonDisplayPanel(
                                jsonContent = jsonContent,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ControlContent(
                                isRunning = isRunning,
                                onStart = onStart,
                                onStop = onStop,
                                onScenarioClick = { scenario ->
                                    onScenarioClick(scenario)
                                },
                                onJsonClick = { json -> selectedJson = json },
                                signals = signals,
                                sceneDescriptor = sceneDescriptor,
                                effectCommands = effectCommands,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlContent(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onScenarioClick: (SceneScenario) -> Unit,
    onJsonClick: (String) -> Unit,
    signals: StandardizedSignals?,
    sceneDescriptor: SceneDescriptor?,
    effectCommands: EffectCommands?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = {
                if (isRunning) onStop() else onStart()
            },
            modifier = Modifier.size(72.dp),
            containerColor = if (isRunning) Color(0xFFF44336) else CarTheme.AccentCyan,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = if (isRunning) "⏹" else "▶",
                fontSize = 32.sp
            )
        }

        Text(
            text = if (isRunning) "停止运行" else "开始运行",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "场景验证",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val scenarios = listOf(
                SceneScenario("rainy_emo", "🌧 雨天emo", "雨天+低落情绪", Color(0xFF5C6BC0)),
                SceneScenario("children_board", "👶 小朋友上车", "检测到儿童乘客", Color(0xFFFF9800)),
                SceneScenario("user_pop", "🎵 我喜欢流行乐", "用户偏好输入", Color(0xFF9C27B0)),
                SceneScenario("beach_vacation", "🏖️ 海边度假", "海边场景+放松", Color(0xFF00BCD4)),
                SceneScenario("romantic_date", "💕 浪漫约会", "夜晚+浪漫氛围", Color(0xFFE91E63)),
                SceneScenario("fatigue_alert", "😴 疲劳提醒", "检测到疲劳", Color(0xFFF44336))
            )

            scenarios.chunked(2).forEach { rowScenarios ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowScenarios.forEach { scenario ->
                        Column(modifier = Modifier.weight(1f)) {
                            ScenarioButton(
                                scenario = scenario,
                                onClick = { onScenarioClick(scenario) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                JsonButton(
                                    label = "L1",
                                    onClick = {
                                        val jsonStr = signals?.let { json.encodeToString(it) } ?: "{}"
                                        onJsonClick("=== Layer 1 感知层 ===\n$jsonStr")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                JsonButton(
                                    label = "L2",
                                    onClick = {
                                        val jsonStr = sceneDescriptor?.let { json.encodeToString(it) } ?: "{}"
                                        onJsonClick("=== Layer 2 推理层 ===\n$jsonStr")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                JsonButton(
                                    label = "L3",
                                    onClick = {
                                        val jsonStr = effectCommands?.let { json.encodeToString(it) } ?: "{}"
                                        onJsonClick("=== Layer 3 生成层 ===\n$jsonStr")
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (rowScenarios.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(28.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = CarTheme.AccentCyan.copy(alpha = 0.8f)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
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
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scenario.color
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = scenario.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun JsonDisplayPanel(
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
        val scrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()
        
        Text(
            text = jsonContent,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            ),
            color = Color(0xFF79C0FF),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .horizontalScroll(horizontalScrollState)
                .padding(16.dp)
        )
    }
}

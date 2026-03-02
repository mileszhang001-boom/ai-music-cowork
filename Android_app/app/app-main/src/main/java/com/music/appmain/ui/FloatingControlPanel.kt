package com.music.appmain.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun FloatingControlPanel(
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
        modifier = modifier
            .fillMaxSize()
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

@Composable
private fun ControlPopup(
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
                    .width(320.dp)
                    .heightIn(max = 500.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "控制面板",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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

                    Spacer(modifier = Modifier.height(24.dp))

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
                            .heightIn(max = 280.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scenarios = listOf(
                            SceneScenario("rainy_emo", "雨天emo", "雨天+低落情绪", Color(0xFF5C6BC0)),
                            SceneScenario("children_board", "小朋友上车", "检测到儿童乘客", Color(0xFFFF9800)),
                            SceneScenario("rain_stops", "雨过天晴", "天气转晴+愉悦", Color(0xFF4CAF50)),
                            SceneScenario("noisy_car", "车内吵闹", "多人+高音量", Color(0xFFE91E63)),
                            SceneScenario("user_pop", "我喜欢流行乐", "用户偏好输入", Color(0xFF9C27B0)),
                            SceneScenario("beach_vacation", "海边度假", "海边场景+放松", Color(0xFF00BCD4)),
                            SceneScenario("romantic_date", "浪漫约会", "夜晚+浪漫氛围", Color(0xFFE91E63)),
                            SceneScenario("fatigue_alert", "疲劳提醒", "检测到疲劳", Color(0xFFF44336))
                        )

                        scenarios.chunked(2).forEach { rowScenarios ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowScenarios.forEach { scenario ->
                                    ScenarioButton(
                                        scenario = scenario,
                                        onClick = { onScenarioClick(scenario) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowScenarios.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
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
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scenario.color
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = scenario.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

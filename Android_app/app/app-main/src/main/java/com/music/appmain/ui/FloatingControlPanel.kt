package com.music.appmain.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    var layer1Json by remember(signals) { mutableStateOf(signals?.let { json.encodeToString(it) } ?: "{}") }
    var layer2Json by remember(sceneDescriptor) { mutableStateOf(sceneDescriptor?.let { json.encodeToString(it) } ?: "{}") }
    var layer3Json by remember(effectCommands) { mutableStateOf(effectCommands?.let { json.encodeToString(it) } ?: "{}") }
    
    var animTrigger by remember { mutableStateOf(0) }

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
                    .fillMaxHeight(0.4f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 关闭按钮 - 左上角
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 18.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 控制栏 - 主按钮 + 6场景按钮
                    ControlSection(
                        isRunning = isRunning,
                        onStart = onStart,
                        onStop = onStop,
                        onScenarioClick = { scenario ->
                            onScenarioClick(scenario)
                            animTrigger++
                            layer1Json = signals?.let { json.encodeToString(it) } ?: "{}"
                            layer2Json = sceneDescriptor?.let { json.encodeToString(it) } ?: "{}"
                            layer3Json = effectCommands?.let { json.encodeToString(it) } ?: "{}"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // JSON 展示栏
                    JsonDataSection(
                        layer1Json = layer1Json,
                        layer2Json = layer2Json,
                        layer3Json = layer3Json,
                        animTrigger = animTrigger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 主控制按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(72.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (isRunning) onStop() else onStart()
                },
                modifier = Modifier.size(56.dp),
                containerColor = if (isRunning) Color(0xFFF44336) else CarTheme.AccentCyan,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text(
                    text = if (isRunning) "⏹" else "▶",
                    fontSize = 24.sp
                )
            }
            Text(
                text = if (isRunning) "停止" else "开始",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 6个场景按钮 - 2行3列
        Column(
            modifier = Modifier.weight(1f),
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

            // 第一行3个
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.take(3).forEach { scenario ->
                    ScenarioButton(
                        scenario = scenario,
                        onClick = { onScenarioClick(scenario) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 第二行3个
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.drop(3).forEach { scenario ->
                    ScenarioButton(
                        scenario = scenario,
                        onClick = { onScenarioClick(scenario) },
                        modifier = Modifier.weight(1f)
                    )
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
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scenario.color
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = scenario.name,
            style = MaterialTheme.typography.labelSmall,
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
    layer1Json: String,
    layer2Json: String,
    layer3Json: String,
    animTrigger: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Layer 1
        JsonPanel(
            title = "Layer 1 - 感知层",
            jsonContent = layer1Json,
            animTrigger = animTrigger,
            modifier = Modifier.weight(1f)
        )

        // Layer 2
        JsonPanel(
            title = "Layer 2 - 推理层",
            jsonContent = layer2Json,
            animTrigger = animTrigger,
            modifier = Modifier.weight(1f)
        )

        // Layer 3
        JsonPanel(
            title = "Layer 3 - 生成层",
            jsonContent = layer3Json,
            animTrigger = animTrigger,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun JsonPanel(
    title: String,
    jsonContent: String,
    animTrigger: Int,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (animTrigger > 0) 0.5f else 1f,
        animationSpec = keyframes {
            durationMillis = 300
            0.5f at 150
            1f at 300
        },
        label = "fade"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (animTrigger > 0) 0.98f else 1f,
        animationSpec = keyframes {
            durationMillis = 300
            0.98f at 150
            1f at 300
        },
        label = "scale"
    )

    Card(
        modifier = modifier.alpha(alpha).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1117)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CarTheme.AccentCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarTheme.AccentCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )

            val scrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Text(
                text = jsonContent,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 10.sp
                ),
                color = Color(0xFF79C0FF),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(6.dp)
            )
        }
    }
}

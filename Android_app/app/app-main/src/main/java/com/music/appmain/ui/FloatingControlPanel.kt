package com.music.appmain.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.core.api.models.EffectCommands
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import com.music.appmain.ui.SceneScenario
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

private fun colorizeJson(jsonString: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < jsonString.length) {
            when {
                jsonString[i] == '"' -> {
                    var j = i + 1
                    while (j < jsonString.length && jsonString[j] != '"') {
                        if (jsonString[j] == '\\') j++
                        j++
                    }
                    val str = jsonString.substring(i, j + 1)
                    if (str.endsWith(": \"")) {
                        withStyle(SpanStyle(color = Color(0xFF7EE787))) { append(str) }
                    } else {
                        withStyle(SpanStyle(color = Color(0xFFA5D6FF))) { append(str) }
                    }
                    i = j + 1
                }
                jsonString[i].isDigit() || jsonString[i] == '-' -> {
                    var j = i
                    while (j < jsonString.length && (jsonString[j].isDigit() || jsonString[j] == '.' || jsonString[j] == '-' || jsonString[j] == 'e' || jsonString[j] == 'E' || jsonString[j] == '+')) {
                        j++
                    }
                    withStyle(SpanStyle(color = Color(0xFFFFA657))) { append(jsonString.substring(i, j)) }
                    i = j
                }
                i + 4 <= jsonString.length && jsonString.substring(i, i + 4) == "true" -> {
                    withStyle(SpanStyle(color = Color(0xFFFF7B72))) { append("true") }
                    i += 4
                }
                i + 5 <= jsonString.length && jsonString.substring(i, i + 5) == "false" -> {
                    withStyle(SpanStyle(color = Color(0xFFFF7B72))) { append("false") }
                    i += 5
                }
                i + 4 <= jsonString.length && jsonString.substring(i, i + 4) == "null" -> {
                    withStyle(SpanStyle(color = Color(0xFF8B949E))) { append("null") }
                    i += 4
                }
                else -> {
                    withStyle(SpanStyle(color = Color(0xFFC9D1D9))) { append(jsonString[i]) }
                    i++
                }
            }
        }
    }
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
    var showLayer1 by remember { mutableStateOf(true) }
    var showLayer2 by remember { mutableStateOf(true) }
    var showLayer3 by remember { mutableStateOf(true) }
    
    var layer1Json by remember { mutableStateOf("") }
    var layer2Json by remember { mutableStateOf("") }
    var layer3Json by remember { mutableStateOf("") }
    
    var animTrigger by remember { mutableStateOf(0) }
    
    val scope = rememberCoroutineScope()

    fun triggerRefresh(withAnimation: Boolean = true) {
        layer1Json = signals?.let { json.encodeToString(it) } ?: "{}"
        layer2Json = sceneDescriptor?.let { json.encodeToString(it) } ?: "{}"
        layer3Json = effectCommands?.let { json.encodeToString(it) } ?: "{}"
        
        if (withAnimation) {
            showLayer1 = false
            showLayer2 = false
            showLayer3 = false
            animTrigger++
            
            scope.launch {
                delay(50)
                showLayer1 = true
                delay(100)
                showLayer2 = true
                delay(100)
                showLayer3 = true
            }
        } else {
            showLayer1 = true
            showLayer2 = true
            showLayer3 = true
        }
    }

    LaunchedEffect(signals, sceneDescriptor, effectCommands) {
        triggerRefresh(withAnimation = false)
    }

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
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.8f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0D1117)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
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

                    ControlSection(
                        isRunning = isRunning,
                        onStart = {
                            onStart()
                            triggerRefresh(withAnimation = false)
                        },
                        onStop = onStop,
                        onScenarioClick = { scenario ->
                            onScenarioClick(scenario)
                            triggerRefresh(withAnimation = true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    JsonDataSection(
                        layer1Json = layer1Json,
                        layer2Json = layer2Json,
                        layer3Json = layer3Json,
                        showLayer1 = showLayer1,
                        showLayer2 = showLayer2,
                        showLayer3 = showLayer3,
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
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = if (isRunning) onStop else onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFEF4444) else CarTheme.AccentCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isRunning) "停止生成" else "开始生成",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScenarioButton("🌧", "雨夜氛围", SceneScenario("rainy_emo", "雨夜氛围", "雨天+低落", CarTheme.AccentPurple), onScenarioClick)
            ScenarioButton("👶", "带娃出行", SceneScenario("children_board", "带娃出行", "检测到儿童", CarTheme.AccentOrange), onScenarioClick)
            ScenarioButton("🎵", "流行音乐", SceneScenario("user_pop", "流行音乐", "用户偏好", CarTheme.AccentCyan), onScenarioClick)
            ScenarioButton("🏖️", "海滩度假", SceneScenario("beach_vacation", "海滩度假", "海边+放松", CarTheme.AccentPurple), onScenarioClick)
            ScenarioButton("💕", "浪漫约会", SceneScenario("romantic_date", "浪漫约会", "夜晚+浪漫", CarTheme.AccentPink), onScenarioClick)
            ScenarioButton("😴", "疲劳驾驶", SceneScenario("fatigue_alert", "疲劳驾驶", "检测到疲劳", Color(0xFFF44336)), onScenarioClick)
        }
    }
}

@Composable
private fun ScenarioButton(
    emoji: String,
    label: String,
    scenario: SceneScenario,
    onClick: (SceneScenario) -> Unit
) {
    Button(
        onClick = { onClick(scenario) },
        colors = ButtonDefaults.buttonColors(
            containerColor = CarTheme.AccentPurple.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$emoji $label",
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun JsonDataSection(
    layer1Json: String,
    layer2Json: String,
    layer3Json: String,
    showLayer1: Boolean,
    showLayer2: Boolean,
    showLayer3: Boolean,
    animTrigger: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JsonPanel(
            title = "Layer 1 - 感知层",
            jsonContent = layer1Json,
            showContent = showLayer1,
            animTrigger = animTrigger,
            headerColor = Color(0xFF7EE787),
            bgColor = Color(0xFF161B22),
            modifier = Modifier.weight(1f)
        )

        JsonPanel(
            title = "Layer 2 - 推理层",
            jsonContent = layer2Json,
            showContent = showLayer2,
            animTrigger = animTrigger,
            headerColor = Color(0xFFFFA657),
            bgColor = Color(0xFF161B22),
            modifier = Modifier.weight(1f)
        )

        JsonPanel(
            title = "Layer 3 - 生成层",
            jsonContent = layer3Json,
            showContent = showLayer3,
            animTrigger = animTrigger,
            headerColor = Color(0xFF79C0FF),
            bgColor = Color(0xFF161B22),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun JsonPanel(
    title: String,
    jsonContent: String,
    showContent: Boolean,
    animTrigger: Int,
    headerColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fade"
    )

    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.95f,
        animationSpec = tween(durationMillis = 300),
        label = "scale"
    )

    Card(
        modifier = modifier
            .alpha(alpha)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = bgColor
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
                color = headerColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )

            val scrollState = rememberScrollState()

            if (jsonContent.isNotEmpty() && showContent) {
                val colorized = remember(jsonContent) { colorizeJson(jsonContent) }
                
                Text(
                    text = colorized,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(10.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!showContent) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = headerColor
                        )
                    }
                }
            }
        }
    }
}

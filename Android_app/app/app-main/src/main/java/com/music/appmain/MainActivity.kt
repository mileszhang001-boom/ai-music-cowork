package com.music.appmain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.layer3.api.Layer3Config
import com.music.appmain.permission.PermissionManager
import com.music.appmain.permission.PermissionsState
import com.music.appmain.ui.SceneScenario
import com.music.appmain.voice.VoiceInputState
import com.music.appmain.voice.VoiceInputService
import com.music.appmain.voice.VoiceInputCallback
import com.music.appmain.ui.PlayerControlBar
import com.music.appmain.ui.CarAIPanel
import com.music.appmain.ui.CarTheme
import com.music.localmusic.player.PlayerState
import com.music.localmusic.player.PlaybackInfo
import com.music.perception.api.PerceptionConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    private var hasRequestedPermissions = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionManager = PermissionManager(this)
        
        setContent {
            MaterialTheme {
                MainScreen(
                    permissionManager = permissionManager,
                    lifecycleOwner = this
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!hasRequestedPermissions && !permissionManager.permissionsState.value.allGranted) {
            hasRequestedPermissions = true
        }
    }
}

@Composable
private fun MainScreen(
    permissionManager: PermissionManager,
    lifecycleOwner: ComponentActivity
) {
    val permissionsState = permissionManager.permissionsState.collectAsState()
    
    val perceptionConfig = remember {
        PerceptionConfig(
            ipCameraUrl = "http://172.31.2.252:8081/video",
            ipCameraUsername = "admin",
            ipCameraPassword = "123456",
            dashScopeApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a"
        )
    }
    
    val layer3Config = remember {
        Layer3Config.Builder()
            .setGenerationEngine(
                com.example.layer3.api.GenerationEngineConfig(
                    llmApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
                    llmBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    llmModel = "qwen-plus"
                )
            )
            .build()
    }
    
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(
            application = lifecycleOwner.application,
            lifecycleOwner = lifecycleOwner,
            perceptionConfig = perceptionConfig,
            layer3Config = layer3Config,
            llmApiKey = "sk-fb1a1b32bf914059a043ee4ebd1c845a",
            llmBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            llmModel = "qwen-plus"
        )
    )
    
    val isInitialized by viewModel.isInitializedFlow.collectAsState()
    val isRunning by viewModel.isRunningFlow.collectAsState()
    val standardizedSignals by viewModel.standardizedSignalsFlow.collectAsState()
    val sceneDescriptor by viewModel.sceneDescriptorFlow.collectAsState()
    val effectCommands by viewModel.effectCommandsFlow.collectAsState()
    val playerState by viewModel.playerStateFlow.collectAsState()
    val playbackInfo by viewModel.playbackInfoFlow.collectAsState()
    val trackCount by viewModel.trackCountFlow.collectAsState()
    val repeatMode by viewModel.repeatModeFlow.collectAsState()
    val currentAlbumArt by viewModel.currentAlbumArtFlow.collectAsState()
    val playlist by viewModel.playlistFlow.collectAsState()
    val playlistIndex by viewModel.playlistIndexFlow.collectAsState()
    // val voiceInputState by viewModel.voiceInputStateFlow.collectAsState()
    // val voiceRecognizedText by viewModel.voiceRecognizedTextFlow.collectAsState()
    // val voiceAmplitude by viewModel.voiceAmplitudeFlow.collectAsState()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasShownPermissionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var displayLayer1 by remember { mutableStateOf<String?>(null) }
    var displayLayer2 by remember { mutableStateOf<String?>(null) }
    var displayLayer3 by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(permissionsState.value.allGranted, hasShownPermissionDialog) {
        if (!permissionsState.value.allGranted && !hasShownPermissionDialog) {
            showPermissionDialog = true
            hasShownPermissionDialog = true
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CarAIPanel(
            signals = standardizedSignals,
            sceneDescriptor = sceneDescriptor,
            effectCommands = effectCommands,
            isRunning = isRunning,
            onStart = { viewModel.start() },
            onStop = { viewModel.stop() },
            onScenarioClick = { scenario ->
                if (isRunning) {
                    viewModel.stop()
                }
                viewModel.simulateScenario(scenario.id)
            },
            modifier = Modifier
                .fillMaxSize()
        )
    }
    
    if (showPermissionDialog && !permissionsState.value.allGranted) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("权限请求") },
            text = { Text("应用需要存储权限来访问本地音乐文件") },
            confirmButton = {
                Button(onClick = { 
                    permissionManager.requestPermissions()
                    showPermissionDialog = false 
                }) {
                    Text("授权")
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ControlPanel(
    isRunning: Boolean,
    isInitialized: Boolean,
    permissionsGranted: Boolean,
    // voiceInputState: VoiceInputState,
    // voiceRecognizedText: String,
    // voiceAmplitude: Float,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    onScenarioClick: (SceneScenario) -> Unit,
    // onVoiceInputStart: () -> Unit,
    // onVoiceInputStop: () -> Unit,
    // onVoiceInputCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scenarios = listOf(
        SceneScenario("rainy_emo", "雨天emo", "雨天+低落", Color(0xFF5C6BC0)),
        SceneScenario("children_board", "小朋友上车", "检测到儿童", Color(0xFFFF9800)),
        SceneScenario("user_pop", "我喜欢流行乐", "用户偏好", Color(0xFF9C27B0)),
        SceneScenario("beach_vacation", "海边度假", "海边+放松", Color(0xFF00838F)),
        SceneScenario("romantic_date", "浪漫约会", "夜晚+浪漫", Color(0xFFAD1457)),
        SceneScenario("fatigue_alert", "疲劳提醒", "检测到疲劳", Color(0xFFD32F2F))
    )
    
    // val isListening = voiceInputState is VoiceInputState.Listening || voiceInputState is VoiceInputState.Processing
    val isListening = false
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Button(
                onClick = if (isRunning) onStop else onStart,
                enabled = isInitialized && permissionsGranted && !isListening,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = if (isRunning) androidx.compose.material.icons.Icons.Default.Stop else androidx.compose.material.icons.Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "停止监测" else "端到端场景 - 自动监测",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // VoiceInputButton(
            //     voiceInputState = voiceInputState,
            //     voiceRecognizedText = voiceRecognizedText,
            //     voiceAmplitude = voiceAmplitude,
            //     onStart = onVoiceInputStart,
            //     onStop = onVoiceInputStop,
            //     onCancel = onVoiceInputCancel,
            //     enabled = isInitialized && permissionsGranted && !isRunning,
            //     modifier = Modifier.fillMaxWidth()
            // )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "场景验证",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                scenarios.take(4).forEach { scenario ->
                    ScenarioChip(
                        scenario = scenario,
                        onClick = { onScenarioClick(scenario) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                scenarios.drop(4).forEach { scenario ->
                    ScenarioChip(
                        scenario = scenario,
                        onClick = { onScenarioClick(scenario) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// @Composable
// private fun VoiceInputButton(
//     voiceInputState: VoiceInputState,
//     voiceRecognizedText: String,
//     voiceAmplitude: Float,
//     onStart: () -> Unit,
//     onStop: () -> Unit,
//     onCancel: () -> Unit,
//     enabled: Boolean,
//     modifier: Modifier = Modifier
// ) {
//     val isListening = voiceInputState is VoiceInputState.Listening || voiceInputState is VoiceInputState.Processing
//     val isError = voiceInputState is VoiceInputState.Error
//     val hasResult = voiceInputState is VoiceInputState.Result
//     
//     val backgroundColor = when {
//         isListening -> Color(0xFFE91E63)
//         isError -> Color(0xFFF44336)
//         hasResult -> Color(0xFF4CAF50)
//         else -> Color(0xFF2196F3)
//     }
//     
//     val buttonText = when {
//         isListening -> "正在聆听..."
//         isError -> (voiceInputState as VoiceInputState.Error).message
//         hasResult -> voiceRecognizedText.ifEmpty { "识别完成" }
//         else -> "🎤 语音输入"
//     }
//     
//     val pulseScale = if (isListening) {
//         1f + voiceAmplitude * 0.1f
//     } else {
//         1f
//     }
//     
//     Card(
//         modifier = modifier
//             .height(48.dp)
//             .graphicsLayer {
//                 scaleX = pulseScale
//                 scaleY = pulseScale
//             },
//         colors = CardDefaults.cardColors(containerColor = backgroundColor),
//         shape = RoundedCornerShape(8.dp),
//         onClick = {
//             if (!enabled) return@Card
//             when {
//                 isListening -> onStop()
//                 else -> onStart()
//             }
//         }
//     ) {
//         Box(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(horizontal = 12.dp),
//             contentAlignment = Alignment.Center
//         ) {
//             Row(
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.Center
//             ) {
//                 if (isListening) {
//                     androidx.compose.animation.AnimatedVisibility(
//                         visible = true,
//                         enter = androidx.compose.animation.fadeIn(),
//                         exit = androidx.compose.animation.fadeOut()
//                     ) {
//                         androidx.compose.foundation.layout.Box(
//                             modifier = Modifier
//                                 .size(16.dp)
//                                 .background(Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
//                         )
//                     }
//                     Spacer(modifier = Modifier.width(8.dp))
//                 }
//                 
//                 Text(
//                     text = buttonText,
//                     style = MaterialTheme.typography.titleMedium,
//                     fontWeight = FontWeight.Bold,
//                     color = Color.White,
//                     maxLines = 1,
//                     overflow = TextOverflow.Ellipsis
//                 )
//             }
//             
//             if (isListening) {
//                 LinearProgressIndicator(
//                     progress = { voiceAmplitude },
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .height(2.dp)
//                         .align(Alignment.BottomCenter),
//                     color = Color.White.copy(alpha = 0.5f),
//                     trackColor = Color.White.copy(alpha = 0.2f)
//                 )
//             }
//         }
//     }
// }

@Composable
private fun ScenarioChip(
    scenario: SceneScenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = scenario.color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, scenario.color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = scenario.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = scenario.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LayerJsonPanels(
    layer1Data: String?,
    layer2Data: String?,
    layer3Data: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LayerPanel(
            title = "Layer 1 - 感知层",
            jsonData = layer1Data,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        
        LayerPanel(
            title = "Layer 2 - 语义层",
            jsonData = layer2Data,
            color = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f)
        )
        
        LayerPanel(
            title = "Layer 3 - 执行层",
            jsonData = layer3Data,
            color = Color(0xFFFF5722),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LayerPanel(
    title: String,
    jsonData: String?,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isLayer1 = title.contains("Layer 1")
    val isLayer2 = title.contains("Layer 2")
    val isLayer3 = title.contains("Layer 3")
    
    val keyMetrics = remember(jsonData) {
        parseKeyMetrics(jsonData, isLayer1, isLayer2, isLayer3)
    }
    
    val scrollState = rememberScrollState()
    
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            if (keyMetrics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = keyMetrics.take(3).joinToString("  "),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.9f)
            ) {
                if (jsonData != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = jsonData,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF37474F)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "等待数据...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun parseKeyMetrics(jsonData: String?, isLayer1: Boolean, isLayer2: Boolean, isLayer3: Boolean): List<String> {
    if (jsonData == null || jsonData == "Loading...") return emptyList()
    
    return try {
        val metrics = mutableListOf<String>()
        
        if (isLayer1) {
            when {
                jsonData.contains("speed", ignoreCase = true) -> {
                    val speedMatch = Regex("\"speed\"\\s*:\\s*([\\d.]+)").find(jsonData)
                    if (speedMatch != null) metrics.add("🚗 ${speedMatch.groupValues[1]}km/h")
                }
                jsonData.contains("weather", ignoreCase = true) -> {
                    val weatherMatch = Regex("\"weather\"\\s*:\\s*\"([^\"]+)\"").find(jsonData)
                    if (weatherMatch != null) {
                        val weather = weatherMatch.groupValues[1]
                        val emoji = when {
                            weather.contains("rain", ignoreCase = true) -> "🌧️"
                            weather.contains("sunny", ignoreCase = true) -> "☀️"
                            weather.contains("cloud", ignoreCase = true) -> "☁️"
                            else -> "🌤️"
                        }
                        metrics.add("$emoji $weather")
                    }
                }
                jsonData.contains("temperature", ignoreCase = true) -> {
                    val tempMatch = Regex("\"temperature\"\\s*:\\s*(-?[\\d.]+)").find(jsonData)
                    if (tempMatch != null) metrics.add("🌡️ ${tempMatch.groupValues[1]}°C")
                }
                jsonData.contains("volume", ignoreCase = true) -> {
                    val volMatch = Regex("\"volume\"\\s*:\\s*([\\d.]+)").find(jsonData)
                    if (volMatch != null) metrics.add("🔊 ${volMatch.groupValues[1]}")
                }
                jsonData.contains("passengers", ignoreCase = true) || jsonData.contains("occupants", ignoreCase = true) -> {
                    val passMatch = Regex("\"(?:passengers|occupants)\"\\s*:\\s*(\\d+)").find(jsonData)
                    if (passMatch != null) metrics.add("👥 ${passMatch.groupValues[1]}人")
                }
                jsonData.contains("mood", ignoreCase = true) -> {
                    val moodMatch = Regex("\"mood\"\\s*:\\s*\"([^\"]+)\"").find(jsonData)
                    if (moodMatch != null) {
                        val mood = moodMatch.groupValues[1]
                        val emoji = when {
                            mood.contains("sad", ignoreCase = true) || mood.contains("emo", ignoreCase = true) -> "😢"
                            mood.contains("happy", ignoreCase = true) -> "😊"
                            mood.contains("tired", ignoreCase = true) || mood.contains("fatigue", ignoreCase = true) -> "😴"
                            else -> "😐"
                        }
                        metrics.add("$emoji $mood")
                    }
                }
            }
        } else if (isLayer2) {
            when {
                jsonData.contains("scene", ignoreCase = true) -> {
                    val sceneMatch = Regex("\"scene\"\\s*:\\s*\"([^\"]+)\"").find(jsonData)
                    if (sceneMatch != null) metrics.add("🎭 ${sceneMatch.groupValues[1]}")
                }
                jsonData.contains("emotion", ignoreCase = true) -> {
                    val emoMatch = Regex("\"emotion\"\\s*:\\s*\"([^\"]+)\"").find(jsonData)
                    if (emoMatch != null) {
                        val emo = emoMatch.groupValues[1]
                        val emoji = when {
                            emo.contains("sad", ignoreCase = true) || emo.contains("emo", ignoreCase = true) -> "😢"
                            emo.contains("happy", ignoreCase = true) -> "😊"
                            emo.contains("romantic", ignoreCase = true) -> "💕"
                            emo.contains("relax", ignoreCase = true) -> "😌"
                            else -> "😐"
                        }
                        metrics.add("$emoji $emo")
                    }
                }
                jsonData.contains("context", ignoreCase = true) -> {
                    val ctxMatch = Regex("\"context\"\\s*:\\s*\"([^\"]+)\"").find(jsonData)
                    if (ctxMatch != null) metrics.add("📍 ${ctxMatch.groupValues[1]}")
                }
                jsonData.contains("time", ignoreCase = true) -> {
                    val timeMatch = Regex("\"time\"\\s*:\\s*\"([^\"]+)\"").find(jsonData)
                    if (timeMatch != null) {
                        val time = timeMatch.groupValues[1]
                        val emoji = when {
                            time.contains("morning", ignoreCase = true) -> "🌅"
                            time.contains("afternoon", ignoreCase = true) -> "☀️"
                            time.contains("evening", ignoreCase = true) || time.contains("night", ignoreCase = true) -> "🌙"
                            else -> "⏰"
                        }
                        metrics.add("$emoji $time")
                    }
                }
            }
        } else if (isLayer3) {
            when {
                jsonData.contains("playlist", ignoreCase = true) -> {
                    val playlistMatch = Regex("\"playlist\"\\s*:\\s*\\[([^\\]]+)\\]").find(jsonData)
                    if (playlistMatch != null) {
                        val count = playlistMatch.groupValues[1].count { it == '{' }
                        metrics.add("🎵 $count 首歌")
                    }
                }
                jsonData.contains("colors", ignoreCase = true) -> {
                    val colorsMatch = Regex("\"colors\"\\s*:\\s*\\[([^\\]]+)\\]").find(jsonData)
                    if (colorsMatch != null) metrics.add("🎨 自定义配色")
                }
                jsonData.contains("lighting", ignoreCase = true) -> {
                    metrics.add("💡 灯光控制")
                }
                jsonData.contains("content", ignoreCase = true) -> {
                    val contentMatch = Regex("\"content\"\\s*:\\s*\\{").find(jsonData)
                    if (contentMatch != null) metrics.add("🎵 内容推荐")
                }
            }
        }
        
        metrics
    } catch (e: Exception) {
        emptyList()
    }
}

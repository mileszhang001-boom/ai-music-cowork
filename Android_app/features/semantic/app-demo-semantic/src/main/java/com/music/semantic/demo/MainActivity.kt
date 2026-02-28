package com.music.semantic.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.*
import com.music.semantic.SemanticEngine
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MainActivity : ComponentActivity() {
    
    private lateinit var semanticEngine: SemanticEngine
    private val json = Json { prettyPrint = true }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        semanticEngine = SemanticEngine(this)
        semanticEngine.initialize()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SemanticDemoScreen()
                }
            }
        }
    }
    
    @Composable
    fun SemanticDemoScreen() {
        var selectedScenario by remember { mutableStateOf("早晨通勤") }
        var inputJson by remember { mutableStateOf("") }
        var outputJson by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf("就绪") }
        
        val scope = rememberCoroutineScope()
        
        val scenarios = mapOf(
            "早晨通勤" to createMorningCommuteSignals(),
            "深夜驾驶" to createNightDriveSignals(),
            "疲劳提醒" to createFatigueSignals(),
            "家庭出行" to createFamilyOutingSignals(),
            "雨天驾驶" to createRainyDaySignals()
        )
        
        LaunchedEffect(selectedScenario) {
            val signals = scenarios[selectedScenario] ?: createMorningCommuteSignals()
            inputJson = json.encodeToString(StandardizedSignals.serializer(), signals)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "语义层 Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "选择预设场景:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scenarios.keys.forEach { scenario ->
                    FilterChip(
                        selected = selectedScenario == scenario,
                        onClick = { selectedScenario = scenario },
                        label = { Text(scenario, fontSize = 12.sp) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "输入 StandardizedSignals (可直接粘贴):",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedTextField(
                value = inputJson,
                onValueChange = { inputJson = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                placeholder = { Text("请输入或粘贴 JSON...", fontSize = 10.sp) },
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "正在推理..."
                        
                        try {
                            val signals = json.decodeFromString(StandardizedSignals.serializer(), inputJson)
                            val result = semanticEngine.processSignals(signals)
                            
                            result.fold(
                                onSuccess = { descriptor ->
                                    outputJson = json.encodeToString(SceneDescriptor.serializer(), descriptor)
                                    statusMessage = "推理成功"
                                },
                                onFailure = { error ->
                                    outputJson = "错误: ${error.message}"
                                    statusMessage = "推理失败"
                                }
                            )
                        } catch (e: Exception) {
                            outputJson = "解析错误: ${e.message}"
                            statusMessage = "输入格式错误"
                        }
                        
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("执行推理")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (statusMessage == "推理成功") Color.Green
                            else if (statusMessage == "推理失败" || statusMessage.contains("错误")) Color.Red
                            else Color.Gray,
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "状态: $statusMessage",
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "输出 SceneDescriptor:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = outputJson.ifEmpty { "点击\"执行推理\"查看结果" },
                    fontSize = 10.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
    
    private fun createMorningCommuteSignals(): StandardizedSignals {
        return StandardizedSignals(
            version = "1.0",
            timestamp = "2026-02-28T07:30:00Z",
            signals = Signals(
                vehicle = Vehicle(speed_kmh = 45.0, passenger_count = 1, gear = "D"),
                environment = Environment(
                    time_of_day = 7.5 / 24,
                    weather = "sunny",
                    temperature = 18.0,
                    date_type = "weekday"
                ),
                internal_camera = InternalCamera(mood = "neutral", confidence = 0.8),
                external_camera = ExternalCamera(scene_description = "city", brightness = 0.7)
            ),
            confidence = Confidence(overall = 0.85)
        )
    }
    
    private fun createNightDriveSignals(): StandardizedSignals {
        return StandardizedSignals(
            version = "1.0",
            timestamp = "2026-02-28T23:00:00Z",
            signals = Signals(
                vehicle = Vehicle(speed_kmh = 60.0, passenger_count = 1, gear = "D"),
                environment = Environment(
                    time_of_day = 23.0 / 24,
                    weather = "clear",
                    temperature = 15.0,
                    date_type = "weekday"
                ),
                internal_camera = InternalCamera(mood = "calm", confidence = 0.75),
                external_camera = ExternalCamera(scene_description = "highway", brightness = 0.2)
            ),
            confidence = Confidence(overall = 0.8)
        )
    }
    
    private fun createFatigueSignals(): StandardizedSignals {
        return StandardizedSignals(
            version = "1.0",
            timestamp = "2026-02-28T14:30:00Z",
            signals = Signals(
                vehicle = Vehicle(speed_kmh = 80.0, passenger_count = 1, gear = "D"),
                environment = Environment(
                    time_of_day = 14.5 / 24,
                    weather = "cloudy",
                    temperature = 22.0,
                    date_type = "weekday"
                ),
                internal_camera = InternalCamera(mood = "tired", confidence = 0.9),
                external_camera = ExternalCamera(scene_description = "highway", brightness = 0.5)
            ),
            confidence = Confidence(overall = 0.85)
        )
    }
    
    private fun createFamilyOutingSignals(): StandardizedSignals {
        return StandardizedSignals(
            version = "1.0",
            timestamp = "2026-02-28T10:00:00Z",
            signals = Signals(
                vehicle = Vehicle(speed_kmh = 40.0, passenger_count = 4, gear = "D"),
                environment = Environment(
                    time_of_day = 10.0 / 24,
                    weather = "sunny",
                    temperature = 25.0,
                    date_type = "weekend"
                ),
                internal_camera = InternalCamera(
                    mood = "happy",
                    confidence = 0.85,
                    passengers = Passengers(children = 1, adults = 2, seniors = 0)
                ),
                external_camera = ExternalCamera(scene_description = "suburban", brightness = 0.8)
            ),
            confidence = Confidence(overall = 0.9)
        )
    }
    
    private fun createRainyDaySignals(): StandardizedSignals {
        return StandardizedSignals(
            version = "1.0",
            timestamp = "2026-02-28T08:00:00Z",
            signals = Signals(
                vehicle = Vehicle(speed_kmh = 35.0, passenger_count = 1, gear = "D"),
                environment = Environment(
                    time_of_day = 8.0 / 24,
                    weather = "rain",
                    temperature = 16.0,
                    date_type = "weekday"
                ),
                internal_camera = InternalCamera(mood = "calm", confidence = 0.7),
                external_camera = ExternalCamera(scene_description = "city", brightness = 0.4)
            ),
            confidence = Confidence(overall = 0.8)
        )
    }
}

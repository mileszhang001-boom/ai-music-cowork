package com.music.appmain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.core.api.models.StandardizedSignals
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.EffectCommands

/**
 * 车载座舱 AI 娱乐系统主面板
 * 完全复刻 Web UI 的三层布局
 */
@Composable
fun CarAIPanel(
    signals: StandardizedSignals?,
    sceneDescriptor: SceneDescriptor?,
    effectCommands: EffectCommands?,
    isRunning: Boolean = false,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onScenarioClick: (SceneScenario) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarTheme.PrimaryBg)
            .drawBehind {
                // 绘制弥散光背景
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CarTheme.AccentCyan.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.15f),
                        radius = size.width * 0.4f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.15f),
                    radius = size.width * 0.4f
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CarTheme.AccentPurple.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.width * 0.35f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.85f),
                    radius = size.width * 0.35f
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CarTheme.AccentPink.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.35f, size.height * 0.5f),
                        radius = size.width * 0.3f
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.5f),
                    radius = size.width * 0.3f
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CarTheme.AccentOrange.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.width * 0.35f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.width * 0.35f
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // 左侧信息面板 - 32%
            LeftInfoPanel(
                signals = signals,
                sceneDescriptor = sceneDescriptor,
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // 右侧生成层 - 68%
            RightGenerationPanel(
                effectCommands = effectCommands,
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
            )
        }

        // 浮动控制面板
        FloatingControlPanel(
            signals = signals,
            sceneDescriptor = sceneDescriptor,
            effectCommands = effectCommands,
            isRunning = isRunning,
            onStart = onStart,
            onStop = onStop,
            onScenarioClick = onScenarioClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 左侧信息面板 - 固定高度分区，各自滚动
 */
@Composable
private fun LeftInfoPanel(
    signals: StandardizedSignals?,
    sceneDescriptor: SceneDescriptor?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 感知层 - 60% 高度，可滚动
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) {
            Layer1DataPanel(signals = signals)
        }
        
        // 推理层 - 40% 高度，可滚动
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        ) {
            Layer2DataPanel(sceneDescriptor = sceneDescriptor)
        }
    }
}

/**
 * 右侧生成层
 */
@Composable
private fun RightGenerationPanel(
    effectCommands: EffectCommands?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Layer3 展示区域 - 100%
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Layer3DataPanel(effectCommands = effectCommands)
        }
    }
}

/**
 * 控制区域
 */
@Composable
private fun ControlArea(
    isRunning: Boolean = false,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onScenarioClick: (SceneScenario) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scenarios = listOf(
        SceneScenario("rainy_emo", "雨天emo", "雨天+低落", CarTheme.AccentPurple),
        SceneScenario("children_board", "小朋友", "检测到儿童", CarTheme.AccentOrange),
        SceneScenario("rain_stops", "雨过天晴", "天气转晴", CarTheme.AccentCyan),
        SceneScenario("noisy_car", "车内闹", "多人+高音量", CarTheme.AccentPink),
        SceneScenario("user_pop", "流行乐", "用户偏好", CarTheme.AccentCyan),
        SceneScenario("beach_vacation", "海边", "海边+放松", CarTheme.AccentPurple),
        SceneScenario("romantic_date", "浪漫", "夜晚+浪漫", CarTheme.AccentPink),
        SceneScenario("fatigue_alert", "疲劳", "检测到疲劳", Color(0xFFF44336))
    )
    
    Column(
        modifier = modifier
            .background(
                color = CarTheme.GlassBg,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        // 主控制按钮
        Button(
            onClick = { if (isRunning) onStop() else onStart() },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFE53935) else CarTheme.AccentCyan
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (isRunning) "停止监测" else "端到端场景 - 自动监测",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 场景验证按钮矩阵 - 2行4列
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.take(4).forEach { scenario ->
                    Button(
                        onClick = { onScenarioClick(scenario) },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scenario.color.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = scenario.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.drop(4).forEach { scenario ->
                    Button(
                        onClick = { onScenarioClick(scenario) },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scenario.color.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = scenario.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 玻璃卡片容器
 */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = CarTheme.GlassBg,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

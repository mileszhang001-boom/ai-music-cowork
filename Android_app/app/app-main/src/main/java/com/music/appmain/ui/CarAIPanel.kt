package com.music.appmain.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import com.music.core.api.models.EffectCommands
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import com.music.localmusic.models.Track
import com.music.localmusic.player.PlaybackInfo
import com.music.localmusic.player.PlayerState
import kotlin.random.Random

val LocalThemeColors = compositionLocalOf { listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified) }

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val alpha: Float,
    val size: Float,
    val char: Char
)

private fun generateParticles(count: Int): List<Particle> {
    val chars = listOf('·', '○', '●', '◦', '◇', '□', '■', '△', '▽')
    return List(count) {
        Particle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            vx = (Random.nextFloat() - 0.5f) * 0.01f,
            vy = (Random.nextFloat() - 0.5f) * 0.01f,
            alpha = Random.nextFloat() * 0.3f + 0.1f,
            size = Random.nextFloat() * 4f + 2f,
            char = chars.random()
        )
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        val hex = colorString.removePrefix("#")
        when (hex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$hex"))
            8 -> Color(android.graphics.Color.parseColor("#$hex"))
            else -> CarTheme.AccentCyan
        }
    } catch (e: Exception) {
        CarTheme.AccentCyan
    }
}

private fun interpolateColor(color1: Color, color2: Color, fraction: Float): Color {
    val r = (color1.red + (color2.red - color1.red) * fraction)
    val g = (color1.green + (color2.green - color1.green) * fraction)
    val b = (color1.blue + (color2.blue - color1.blue) * fraction)
    val a = (color1.alpha + (color2.alpha - color1.alpha) * fraction)
    return Color(red = r, green = g, blue = b, alpha = a)
}

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
    playerState: PlayerState = PlayerState.Idle,
    playbackInfo: PlaybackInfo = PlaybackInfo(),
    playlist: List<Track> = emptyList(),
    currentTrackIndex: Int = -1,
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onPlayTrack: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 从 EffectCommands 提取主题色
    val themeColors = remember(effectCommands) {
        val colors = effectCommands?.commands?.lighting?.colors?.take(2)
        if (colors != null && colors.size >= 2) {
            val color1 = parseColor(colors[0])
            val color2 = parseColor(colors[1])
            // 四个光源：两个直接用这两个色值，另外两个用渐变色
            val gradient1 = interpolateColor(color1, color2, 0.33f)
            val gradient2 = interpolateColor(color1, color2, 0.66f)
            listOf(color1, color2, gradient1, gradient2)
        } else {
            listOf(
                CarTheme.AccentCyan,
                CarTheme.AccentPurple,
                CarTheme.AccentPink,
                CarTheme.AccentOrange
            )
        }
    }
    
    // 动画状态
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // 弥散光动画 - 模拟 organic-float (加速版)
    val light1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light1"
    )
    
    val light2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light2"
    )
    
    val light3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light3"
    )
    
    val light4Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light4"
    )
    
    // 粒子系统状态
    var particles by remember { mutableStateOf(generateParticles(50)) }
    
    // 更新粒子位置
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30)
            particles = particles.map { particle ->
                particle.copy(
                    x = particle.x + particle.vx,
                    y = particle.y + particle.vy
                ).let { p ->
                    if (p.x < 0 || p.x > 1f) p.copy(vx = -p.vx)
                    if (p.y < 0 || p.y > 1f) p.copy(vy = -p.vy)
                    p
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarTheme.PrimaryBg)
    ) {
        // 弥散光背景 - 使用动画偏移 (增大移动范围)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val offsetX1 = (light1Offset - 0.5f) * size.width * 0.5f
            val offsetY1 = (light1Offset - 0.5f) * size.height * 0.4f
            
            val offsetX2 = (light2Offset - 0.5f) * size.width * 0.45f
            val offsetY2 = (light2Offset - 0.5f) * size.height * 0.35f
            
            val offsetX3 = (light3Offset - 0.5f) * size.width * 0.4f
            val offsetY3 = (light3Offset - 0.5f) * size.height * 0.38f
            
            val offsetX4 = (light4Offset - 0.5f) * size.width * 0.48f
            val offsetY4 = (light4Offset - 0.5f) * size.height * 0.32f
            
            val color1 = themeColors.getOrElse(0) { CarTheme.AccentCyan }
            val color2 = themeColors.getOrElse(1) { CarTheme.AccentPurple }
            val color3 = themeColors.getOrElse(2) { CarTheme.AccentPink }
            val color4 = themeColors.getOrElse(3) { CarTheme.AccentOrange }
            
            // 弥散光1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color1.copy(alpha = 0.4f),
                        color1.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f + offsetX1, size.height * 0.08f + offsetY1),
                    radius = size.width * 0.5f
                ),
                center = Offset(size.width * 0.15f + offsetX1, size.height * 0.08f + offsetY1),
                radius = size.width * 0.5f
            )
            
            // 弥散光2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color2.copy(alpha = 0.35f),
                        color2.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f + offsetX2, size.height * 0.78f + offsetY2),
                    radius = size.width * 0.45f
                ),
                center = Offset(size.width * 0.15f + offsetX2, size.height * 0.78f + offsetY2),
                radius = size.width * 0.45f
            )
            
            // 弥散光3
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color3.copy(alpha = 0.3f),
                        color3.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.35f + offsetX3, size.height * 0.43f + offsetY3),
                    radius = size.width * 0.4f
                ),
                center = Offset(size.width * 0.35f + offsetX3, size.height * 0.43f + offsetY3),
                radius = size.width * 0.4f
            )
            
            // 弥散光4
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color4.copy(alpha = 0.35f),
                        color4.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f + offsetX4, size.height * 0.78f + offsetY4),
                    radius = size.width * 0.45f
                ),
                center = Offset(size.width * 0.85f + offsetX4, size.height * 0.78f + offsetY4),
                radius = size.width * 0.45f
            )
        }
        
        // 粒子层
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { particle ->
                val x = particle.x * size.width
                val y = particle.y * size.height
                drawCircle(
                    color = Color.White.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }
        
        // 内容层 - 使用动态主题色
        CompositionLocalProvider(LocalThemeColors provides themeColors) {
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
                playerState = playerState,
                playbackInfo = playbackInfo,
                playlist = playlist,
                currentTrackIndex = currentTrackIndex,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onPlayTrack = onPlayTrack,
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
            )
        }
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
    playerState: PlayerState,
    playbackInfo: PlaybackInfo,
    playlist: List<Track>,
    currentTrackIndex: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayTrack: (Int) -> Unit,
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
            Layer3DataPanel(
                effectCommands = effectCommands,
                playerState = playerState,
                playbackInfo = playbackInfo,
                playlist = playlist,
                currentTrackIndex = currentTrackIndex,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onPlayTrack = onPlayTrack
            )
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

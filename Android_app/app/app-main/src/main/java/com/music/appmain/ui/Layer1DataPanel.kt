package com.music.appmain.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.StandardizedSignals

@Composable
fun Layer1DataPanel(
    signals: StandardizedSignals?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val themeColors = LocalThemeColors.current
            val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }
            val secondaryColor = themeColors.getOrElse(1) { CarTheme.AccentPurple }

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(pulseAlpha)
                    .background(
                        color = if (signals != null) Color(0xFF4CAF50) else CarTheme.TextMuted,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (signals != null) "AI 正在感知" else "等待感知",
                style = CarTheme.GradientTitle.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    fontSize = 14.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (signals != null) {
            val story = buildPerceptionStory(signals)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                story.paragraphs.forEachIndexed { index, paragraph ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(paragraph) {
                        kotlinx.coroutines.delay(index * 120L)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(400)
                        )
                    ) {
                        StoryParagraph(
                            icon = paragraph.icon,
                            text = paragraph.text,
                            highlight = paragraph.highlight
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "waiting")
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dots"
                    )
                    Text(
                        text = "👁️",
                        fontSize = 28.sp,
                        modifier = Modifier.alpha(dotAlpha)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "传感器待连接...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTheme.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryParagraph(
    icon: String,
    text: String,
    highlight: Boolean = false
) {
    val themeColors = LocalThemeColors.current
    val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (highlight) primaryColor.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 1.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (highlight) CarTheme.TextPrimary else CarTheme.TextSecondary,
                fontWeight = if (highlight) FontWeight.Medium else FontWeight.Normal,
                lineHeight = 22.sp
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class StoryParagraphData(
    val icon: String,
    val text: String,
    val highlight: Boolean = false
)

private data class PerceptionStory(
    val paragraphs: List<StoryParagraphData>
)

private fun buildPerceptionStory(signals: StandardizedSignals): PerceptionStory {
    val paragraphs = mutableListOf<StoryParagraphData>()

    val env = signals.signals.environment
    val cam = signals.signals.internal_camera
    val vehicle = signals.signals.vehicle
    val mic = signals.signals.internal_mic

    if (env != null) {
        val weatherEmoji = when (env.weather) {
            "rainy" -> "🌧️"
            "sunny" -> "☀️"
            "cloudy" -> "☁️"
            "clear" -> "🌤️"
            "snowy" -> "❄️"
            else -> "🌍"
        }
        val weatherText = when (env.weather) {
            "rainy" -> "外面正下着雨"
            "sunny" -> "阳光正好"
            "cloudy" -> "天空有些阴沉"
            "clear" -> "天气晴朗"
            "snowy" -> "外面飘着雪"
            else -> "天气未知"
        }
        val tempText = env.temperature?.let { "，气温 ${it.toInt()}°C" } ?: ""
        paragraphs.add(StoryParagraphData(weatherEmoji, "$weatherText$tempText"))

        val timeEmoji = when {
            (env.time_of_day ?: 12.0) < 7.0 -> "🌅"
            (env.time_of_day ?: 12.0) < 12.0 -> "🌞"
            (env.time_of_day ?: 12.0) < 18.0 -> "🌇"
            (env.time_of_day ?: 12.0) < 22.0 -> "🌆"
            else -> "🌙"
        }
        val timeText = when {
            (env.time_of_day ?: 12.0) < 6.0 -> "深夜时分"
            (env.time_of_day ?: 12.0) < 7.0 -> "天刚蒙蒙亮"
            (env.time_of_day ?: 12.0) < 9.0 -> "清晨出发"
            (env.time_of_day ?: 12.0) < 12.0 -> "上午时光"
            (env.time_of_day ?: 12.0) < 14.0 -> "午后时分"
            (env.time_of_day ?: 12.0) < 18.0 -> "下午的路上"
            (env.time_of_day ?: 12.0) < 20.0 -> "傍晚归途"
            (env.time_of_day ?: 12.0) < 22.0 -> "夜幕降临"
            else -> "深夜归家"
        }
        val dateText = when (env.date_type) {
            "weekday" -> "工作日"
            "weekend" -> "周末"
            else -> null
        }
        val fullTimeText = if (dateText != null) "$dateText · $timeText" else timeText
        paragraphs.add(StoryParagraphData(timeEmoji, fullTimeText))
    }

    if (cam != null) {
        val passengers = cam.passengers
        if (passengers != null) {
            val total = (passengers.adults ?: 0) + (passengers.children ?: 0) + (passengers.seniors ?: 0)
            val passengerText = when {
                total <= 1 && (passengers.children ?: 0) == 0 -> "独自驾驶"
                (passengers.children ?: 0) > 0 && total > 1 -> "车里有小朋友，一家人出行"
                (passengers.seniors ?: 0) > 0 -> "载着长辈，稳稳地开"
                total == 2 -> "两个人的旅途"
                total > 2 -> "车里很热闹，${total}个人一起"
                else -> "独自驾驶"
            }
            val passengerEmoji = when {
                (passengers.children ?: 0) > 0 -> "👨‍👩‍👧"
                total == 2 -> "👫"
                total > 2 -> "👥"
                else -> "🧑"
            }
            paragraphs.add(StoryParagraphData(passengerEmoji, passengerText))
        }

        val moodText = when (cam.mood) {
            "sad" -> "看起来有些低落..."
            "happy" -> "心情不错的样子 ☺"
            "excited" -> "看起来很兴奋！"
            "neutral" -> "表情平静"
            "relaxed" -> "很放松的状态"
            "romantic" -> "氛围有点浪漫~"
            "tired" -> "有些疲惫了..."
            else -> null
        }
        if (moodText != null) {
            val moodEmoji = when (cam.mood) {
                "sad" -> "😔"
                "happy" -> "😊"
                "excited" -> "🤩"
                "neutral" -> "😐"
                "relaxed" -> "😌"
                "romantic" -> "🥰"
                "tired" -> "😴"
                else -> "😊"
            }
            paragraphs.add(StoryParagraphData(moodEmoji, moodText, highlight = cam.mood == "tired"))
        }
    }

    if (vehicle != null) {
        val speedText = when {
            (vehicle.speed_kmh ?: 0.0) < 5.0 -> null
            (vehicle.speed_kmh ?: 0.0) < 30.0 -> "车速较慢，可能在市区"
            (vehicle.speed_kmh ?: 0.0) < 80.0 -> "正常行驶中"
            (vehicle.speed_kmh ?: 0.0) < 120.0 -> "在高速上飞驰"
            else -> "车速很快，注意安全"
        }
        if (speedText != null) {
            paragraphs.add(StoryParagraphData("🚗", speedText))
        }
    }

    if (mic != null) {
        val noiseText = when {
            (mic.noise_level ?: 0.0) > 0.7 -> "车内有些吵闹"
            (mic.voice_count ?: 0) > 2 -> "大家在聊天"
            (mic.has_voice == true) -> "有人在说话"
            else -> null
        }
        if (noiseText != null) {
            paragraphs.add(StoryParagraphData("🎙️", noiseText))
        }
    }

    if (paragraphs.isEmpty()) {
        paragraphs.add(StoryParagraphData("👁️", "正在感知周围环境..."))
    }

    return PerceptionStory(paragraphs)
}

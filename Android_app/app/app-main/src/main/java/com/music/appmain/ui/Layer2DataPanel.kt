package com.music.appmain.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.SceneDescriptor

@Composable
fun Layer2DataPanel(
    sceneDescriptor: SceneDescriptor?,
    modifier: Modifier = Modifier
) {
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

            val infiniteTransition = rememberInfiniteTransition(label = "think")
            val thinkAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "thinkAlpha"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(if (sceneDescriptor != null) thinkAlpha else 0.3f)
                    .background(
                        color = if (sceneDescriptor != null) CarTheme.AccentPurple else CarTheme.TextMuted,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (sceneDescriptor != null) "AI 在想..." else "等待推理",
                style = CarTheme.GradientTitle.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    fontSize = 14.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sceneDescriptor != null) {
            val thoughts = buildAIThoughts(sceneDescriptor)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                thoughts.forEachIndexed { index, thought ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(thought) {
                        kotlinx.coroutines.delay(index * 150L)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(500)) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(500)
                        )
                    ) {
                        ThoughtBubble(thought = thought)
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
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dots"
                    )
                    Text(
                        text = "🧠",
                        fontSize = 24.sp,
                        modifier = Modifier.alpha(dotAlpha)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "等待场景分析...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTheme.TextMuted
                    )
                }
            }
        }
    }
}

private data class AIThought(
    val text: String,
    val type: ThoughtType = ThoughtType.NORMAL,
    val sceneName: String? = null
)

private enum class ThoughtType {
    NORMAL,
    CONCLUSION,
    HIGHLIGHT
}

@Composable
private fun ThoughtBubble(thought: AIThought) {
    val themeColors = LocalThemeColors.current
    val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }
    val secondaryColor = themeColors.getOrElse(1) { CarTheme.AccentPurple }

    when (thought.type) {
        ThoughtType.CONCLUSION -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.12f),
                                secondaryColor.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "→ 为你切换到",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CarTheme.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "「${thought.sceneName ?: "智能推荐"}」模式",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            )
                        )
                    )
                }
            }
        }
        ThoughtType.HIGHLIGHT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = primaryColor.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = thought.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CarTheme.TextPrimary,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ThoughtType.NORMAL -> {
            Text(
                text = thought.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CarTheme.TextSecondary,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildAIThoughts(descriptor: SceneDescriptor): List<AIThought> {
    val thoughts = mutableListOf<AIThought>()

    descriptor.scene_narrative?.let { narrative ->
        thoughts.add(AIThought(
            text = "\"$narrative\"",
            type = ThoughtType.HIGHLIGHT
        ))
    }

    if (thoughts.isEmpty()) {
        val coreJudgment = buildCoreJudgment(descriptor)
        if (coreJudgment != null) {
            thoughts.add(AIThought(text = coreJudgment))
        }
    }

    val sceneName = buildSceneDisplayName(descriptor)
    thoughts.add(AIThought(
        text = "",
        type = ThoughtType.CONCLUSION,
        sceneName = sceneName
    ))

    return thoughts
}

private fun buildCoreJudgment(descriptor: SceneDescriptor): String? {
    descriptor.intent.social_context?.let { social ->
        return when (social) {
            "solo" -> "一个人的时光"
            "couple" -> "两个人的氛围"
            "family" -> "一家人出行"
            "friends" -> "朋友聚会"
            "group" -> "人多热闹"
            else -> null
        }
    }
    val valence = descriptor.intent.mood.valence
    val arousal = descriptor.intent.mood.arousal
    return when {
        valence < 0.3 && arousal < 0.3 -> "有些低落和疲惫"
        valence < 0.3 && arousal > 0.7 -> "情绪有些焦躁"
        valence > 0.7 && arousal > 0.7 -> "心情很好，精力充沛"
        valence > 0.7 && arousal < 0.3 -> "心情不错，很放松"
        valence > 0.7 -> "心情愉悦"
        valence < 0.3 -> "情绪有些低落"
        else -> null
    }
}

private fun buildSceneDisplayName(descriptor: SceneDescriptor): String {
    descriptor.scene_name?.let { name ->
        return when (name) {
            "rainy_emo" -> "雨夜氛围"
            "children_board" -> "亲子时光"
            "rain_stops" -> "雨后清新"
            "noisy_car" -> "欢乐车厢"
            "user_pop" -> "流行精选"
            "beach_vacation" -> "海边假日"
            "romantic_date" -> "浪漫之夜"
            "fatigue_alert" -> "提神醒脑"
            "melancholy_night" -> "深夜独处"
            "cheerful_day" -> "阳光心情"
            "energetic_group" -> "活力派对"
            "relaxed_vacation" -> "悠闲假日"
            else -> name
        }
    }
    return when (descriptor.scene_type) {
        "emotional_ride" -> "情绪旅途"
        "family_trip" -> "家庭出行"
        "commute" -> "日常通勤"
        "road_trip" -> "公路旅行"
        "date_night" -> "约会之夜"
        "fatigue_driving" -> "安全守护"
        "scenic_drive" -> "风景之旅"
        else -> "智能推荐"
    }
}



package com.music.appmain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.SceneDescriptor
import java.text.SimpleDateFormat
import java.util.*

data class MomentTrack(
    val title: String,
    val artist: String?
)

data class JourneyMoment(
    val timestamp: Long = System.currentTimeMillis(),
    val sceneName: String,
    val sceneEmoji: String,
    val tracks: MutableList<MomentTrack> = mutableListOf()
)

class JourneyTimelineState {
    private val _moments = mutableStateListOf<JourneyMoment>()
    val moments: List<JourneyMoment> get() = _moments

    private var lastSceneId: String? = null
    private val playedTrackKeys = mutableSetOf<String>()

    fun recordMoment(sceneDescriptor: SceneDescriptor?) {
        if (sceneDescriptor == null) return
        if (sceneDescriptor.scene_id == lastSceneId) return

        lastSceneId = sceneDescriptor.scene_id
        playedTrackKeys.clear()

        _moments.add(
            JourneyMoment(
                sceneName = resolveSceneName(sceneDescriptor),
                sceneEmoji = resolveSceneEmoji(sceneDescriptor)
            )
        )
    }

    fun recordPlayedTrack(title: String, artist: String?) {
        val current = _moments.lastOrNull() ?: return
        val key = "${title}|${artist}"
        if (playedTrackKeys.add(key)) {
            current.tracks.add(MomentTrack(title, artist))
        }
    }

    fun clear() {
        _moments.clear()
        lastSceneId = null
        playedTrackKeys.clear()
    }

    private fun resolveSceneName(descriptor: SceneDescriptor): String {
        return when (descriptor.scene_name) {
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
            else -> descriptor.scene_name ?: when (descriptor.scene_type) {
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
    }

    private fun resolveSceneEmoji(descriptor: SceneDescriptor): String {
        return when (descriptor.scene_name) {
            "rainy_emo" -> "🌧️"
            "children_board" -> "👶"
            "rain_stops" -> "🌈"
            "noisy_car" -> "🎉"
            "user_pop" -> "🎵"
            "beach_vacation" -> "🏖️"
            "romantic_date" -> "💕"
            "fatigue_alert" -> "⚡"
            "melancholy_night" -> "🌙"
            "cheerful_day" -> "☀️"
            "energetic_group" -> "🔥"
            "relaxed_vacation" -> "🌴"
            else -> when (descriptor.scene_type) {
                "emotional_ride" -> "💭"
                "family_trip" -> "👨‍👩‍👧"
                "commute" -> "🚗"
                "road_trip" -> "🛣️"
                "date_night" -> "🌹"
                "fatigue_driving" -> "☕"
                "scenic_drive" -> "🏔️"
                else -> "🎶"
            }
        }
    }

}

@Composable
fun rememberJourneyTimelineState(): JourneyTimelineState {
    return remember { JourneyTimelineState() }
}

@Composable
fun JourneyTimelinePanel(
    state: JourneyTimelineState,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }
    val secondaryColor = themeColors.getOrElse(1) { CarTheme.AccentPurple }
    val listState = rememberLazyListState()

    LaunchedEffect(state.moments.size) {
        if (state.moments.isNotEmpty()) {
            listState.animateScrollToItem(state.moments.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val headerHeight = 28.dp
        val spacerHeight = 12.dp
        val listHeight = maxHeight - headerHeight - spacerHeight

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "旅程回忆",
                    style = CarTheme.GradientTitle.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        fontSize = 14.sp
                    )
                )

                if (state.moments.isNotEmpty()) {
                    Text(
                        text = "${state.moments.size} 个场景",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CarTheme.TextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacerHeight))

            if (state.moments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "旅程开始后，这里会记录每个场景的音乐",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarTheme.TextMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listHeight),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(state.moments) { index, moment ->
                        TimelineMomentItem(
                            moment = moment,
                            isCurrent = index == state.moments.size - 1,
                            accentColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineMomentItem(
    moment: JourneyMoment,
    isCurrent: Boolean,
    accentColor: Color
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(moment.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isCurrent) accentColor.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = moment.sceneEmoji,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = moment.sceneName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isCurrent) CarTheme.TextPrimary else CarTheme.TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CarTheme.TextMuted
                )
            )
        }

        if (moment.tracks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            moment.tracks.forEach { track ->
                Row(
                    modifier = Modifier.padding(start = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "♪",
                        fontSize = 10.sp,
                        color = accentColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (track.artist != null) "${track.title} · ${track.artist}" else track.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CarTheme.TextMuted
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyStatsBar(
    state: JourneyTimelineState,
    modifier: Modifier = Modifier
) {
    if (state.moments.isEmpty()) return

    val themeColors = LocalThemeColors.current
    val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }

    val duration = if (state.moments.size >= 2) {
        val diff = state.moments.last().timestamp - state.moments.first().timestamp
        val minutes = diff / 60000
        if (minutes < 1) "刚刚开始" else "${minutes}分钟"
    } else {
        "刚刚开始"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = primaryColor.copy(alpha = 0.06f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(label = "场景", value = "${state.moments.size}")
        StatDivider()
        StatItem(label = "时长", value = duration)
        StatDivider()
        StatItem(label = "曲目", value = "${state.moments.sumOf { it.tracks.size }}")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = CarTheme.TextPrimary
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CarTheme.TextMuted
            )
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(CarTheme.TextMuted.copy(alpha = 0.2f))
    )
}

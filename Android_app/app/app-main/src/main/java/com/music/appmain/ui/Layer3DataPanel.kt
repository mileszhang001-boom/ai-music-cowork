package com.music.appmain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.EffectCommands
import com.music.localmusic.models.Track
import com.music.localmusic.player.PlaybackInfo
import com.music.localmusic.player.PlayerState

@Composable
fun Layer3DataPanel(
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
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 第一层：场景 + 音效模式
        SceneInfoSection(effectCommands)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 1.dp,
            color = CarTheme.TextMuted.copy(alpha = 0.2f)
        )

        // 第二层：播放器播控
        PlayerControlSection(
            playerState = playerState,
            playbackInfo = playbackInfo,
            currentTrack = playlist.getOrNull(currentTrackIndex),
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            thickness = 1.dp,
            color = CarTheme.TextMuted.copy(alpha = 0.2f)
        )

        // 第三层：歌单列表
        PlaylistSection(
            playlist = playlist,
            currentTrackIndex = currentTrackIndex,
            onPlayTrack = onPlayTrack,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SceneInfoSection(effectCommands: EffectCommands?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val themeColors = LocalThemeColors.current
        val primaryColor = themeColors.getOrElse(0) { CarTheme.AccentCyan }
        val secondaryColor = themeColors.getOrElse(1) { CarTheme.AccentPurple }
        
        Column {
            Text(
                text = "场景",
                style = MaterialTheme.typography.labelMedium,
                color = CarTheme.TextMuted
            )
            Text(
                text = extractLightingTheme(effectCommands),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "音效",
                style = MaterialTheme.typography.labelMedium,
                color = CarTheme.TextMuted
            )
            Text(
                text = extractAudioPreset(effectCommands),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = secondaryColor
            )
        }
    }
}

@Composable
private fun PlayerControlSection(
    playerState: PlayerState,
    playbackInfo: PlaybackInfo,
    currentTrack: Track?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val isPlaying = playerState is PlayerState.Playing

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 当前播放歌曲信息
        if (currentTrack != null) {
            Text(
                text = currentTrack.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CarTheme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentTrack.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = CarTheme.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "等待生成歌单...",
                style = MaterialTheme.typography.bodyMedium,
                color = CarTheme.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 进度条（可拖动）
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val progress = if (playbackInfo.duration > 0) {
            playbackInfo.currentPosition.toFloat() / playbackInfo.duration.toFloat()
        } else {
            0f
        }

        LaunchedEffect(progress) {
            if (!isDragging) {
                sliderPosition = progress
            }
        }

        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                isDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                val newPosition = (sliderPosition * playbackInfo.duration).toLong()
                onSeek(newPosition)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = CarTheme.AccentCyan,
                activeTrackColor = CarTheme.AccentCyan,
                inactiveTrackColor = CarTheme.TextMuted.copy(alpha = 0.2f)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(playbackInfo.currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = CarTheme.TextMuted
            )
            Text(
                text = formatDuration(playbackInfo.duration),
                style = MaterialTheme.typography.labelSmall,
                color = CarTheme.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "⏮",
                    fontSize = 24.sp,
                    color = CarTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            FloatingActionButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp),
                containerColor = CarTheme.AccentCyan,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "⏭",
                    fontSize = 24.sp,
                    color = CarTheme.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    playlist: List<Track>,
    currentTrackIndex: Int,
    onPlayTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "歌单列表",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = CarTheme.TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (playlist.isNotEmpty()) {
            val listState = rememberLazyListState()

            LaunchedEffect(currentTrackIndex) {
                if (currentTrackIndex >= 0 && currentTrackIndex < playlist.size) {
                    listState.animateScrollToItem(currentTrackIndex)
                }
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist) { index, track ->
                    PlaylistTrackItem(
                        index = index + 1,
                        title = track.title,
                        artist = track.artist,
                        isPlaying = index == currentTrackIndex,
                        onClick = { onPlayTrack(index) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "等待生成歌单...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarTheme.TextMuted
                )
            }
        }
    }
}

@Composable
private fun PlaylistTrackItem(
    index: Int,
    title: String,
    artist: String,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                CarTheme.AccentCyan.copy(alpha = 0.1f)
            } else {
                Color.Transparent
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelMedium,
                color = if (isPlaying) CarTheme.AccentCyan else CarTheme.TextMuted,
                modifier = Modifier.width(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isPlaying) CarTheme.TextPrimary else CarTheme.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = CarTheme.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isPlaying) {
                Text(
                    text = "▶",
                    fontSize = 14.sp,
                    color = CarTheme.AccentCyan
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun extractLightingTheme(effectCommands: EffectCommands?): String {
    val theme = effectCommands?.commands?.lighting?.theme
        ?: effectCommands?.commands?.lighting?.action
    return mapLightingTheme(theme)
}

private fun mapLightingTheme(theme: String?): String {
    return when (theme?.lowercase()) {
        "rainy_night", "rainy-night" -> "雨夜氛围"
        "sunny_vibe", "sunny-vibe", "sunny_day", "sunny-day" -> "晴朗氛围"
        "coastal_sunset", "coastal-sunset", "beach", "coastal" -> "海岸日落"
        "city_neon", "city-neon", "city_night", "city-night" -> "城市霓虹"
        "romantic_dinner", "romantic-dinner", "romantic", "date" -> "浪漫晚餐"
        "energy_boost", "energy-boost", "energetic" -> "能量提升"
        "chill_relax", "chill-relax", "relaxed", "relaxing" -> "轻松 chill"
        "melancholy_blue", "melancholy-blue", "sad", "melancholy" -> "忧郁蓝调"
        "excited_party", "excited-party", "excited", "party" -> "兴奋派对"
        "peaceful_journey", "peaceful-journey", "peaceful", "calm" -> "平静旅途"
        "highway", "road" -> "公路氛围"
        "suburban", "suburbs" -> "郊区氛围"
        "default", "none", "" -> "默认氛围"
        else -> theme ?: "默认氛围"
    }
}

private fun extractAudioPreset(effectCommands: EffectCommands?): String {
    val preset = effectCommands?.commands?.audio?.preset
        ?: effectCommands?.commands?.audio?.action
    return mapAudioPreset(preset)
}

private fun mapAudioPreset(preset: String?): String {
    return when (preset?.lowercase()) {
        "pop_boost", "pop-boost", "pop" -> "流行增强"
        "rock_bass", "rock-bass", "rock" -> "摇滚低音"
        "jazz_smooth", "jazz-smooth", "jazz" -> "爵士流畅"
        "classical_refined", "classical-refined", "classical" -> "古典优雅"
        "electronic_pulse", "electronic-pulse", "electronic", "edm" -> "电子脉冲"
        "ambient_chill", "ambient-chill", "ambient" -> "氛围放松"
        "lofi_relax", "lofi-relax", "lo-fi", "lofi" -> "低保真"
        "bass_boost", "bass-boost", "bass" -> "低音增强"
        "vocal_clarity", "vocal-clarity", "vocal" -> "人声清晰"
        "balanced", "normal" -> "均衡音效"
        "rnb", "r&b" -> "节奏布鲁斯"
        "hiphop", "hip-hop" -> "嘻哈"
        "country" -> "乡村"
        "indie" -> "独立音乐"
        "default", "none", "" -> "标准音效"
        else -> preset ?: "标准音效"
    }
}

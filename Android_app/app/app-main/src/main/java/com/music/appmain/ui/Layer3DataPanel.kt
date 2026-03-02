package com.music.appmain.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.core.api.models.EffectCommands

/**
 * Layer 3 生成层 - Web UI 风格
 * 将 EffectCommands 数据转换为沉浸式展示
 */
@Composable
fun Layer3DataPanel(
    effectCommands: EffectCommands?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 场景信息头部
        SceneInfoHeader(effectCommands)

        Spacer(modifier = Modifier.height(24.dp))

        // 歌单展示区
        PlaylistDisplay(effectCommands)

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * 场景信息头部 - lighting-theme / audio-preset
 */
@Composable
private fun SceneInfoHeader(effectCommands: EffectCommands?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = extractLightingTheme(effectCommands),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = CarTheme.TextPrimary
            ),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = extractAudioPreset(effectCommands),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = CarTheme.TextSecondary,
                letterSpacing = 1.sp
            ),
            textAlign = TextAlign.End
        )
    }
}

/**
 * 歌单展示 - 上一首/当前首/下一首
 */
@Composable
private fun PlaylistDisplay(effectCommands: EffectCommands?) {
    val playlist = extractPlaylist(effectCommands)
    val currentIndex = 0

    val prevTrack = playlist.getOrNull(currentIndex - 1) ?: playlist.lastOrNull()
    val currentTrack = playlist.getOrNull(currentIndex)
    val nextTrack = playlist.getOrNull(currentIndex + 1) ?: playlist.firstOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 上一首
        if (playlist.size > 1 && prevTrack != null) {
            PlaylistItem(
                label = "上一首",
                title = prevTrack.title,
                artist = prevTrack.artist,
                isCurrent = false,
                modifier = Modifier.padding(end = 40.dp)
            )
        }

        // 当前首
        if (currentTrack != null) {
            PlaylistItem(
                label = "正在播放",
                title = currentTrack.title,
                artist = currentTrack.artist,
                isCurrent = true,
                modifier = Modifier.padding(end = 20.dp)
            )
        }

        // 下一首
        if (nextTrack != null) {
            PlaylistItem(
                label = "下一首",
                title = nextTrack.title,
                artist = nextTrack.artist,
                isCurrent = false,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        
        // 如果没有歌单，显示提示
        if (playlist.isEmpty()) {
            Text(
                text = "等待生成歌单...",
                style = MaterialTheme.typography.bodyMedium,
                color = CarTheme.TextMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 20.dp)
            )
        }
    }
}

/**
 * 歌单项 - 标题 + 艺术家两行
 */
@Composable
private fun PlaylistItem(
    label: String,
    title: String,
    artist: String,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CarTheme.TextMuted.copy(alpha = 0.5f)
            ),
            textAlign = TextAlign.End
        )
        
        Text(
            text = title,
            style = if (isCurrent) {
                CarTheme.LyricsCurrent.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CarTheme.AccentCyan,
                            CarTheme.TextPrimary,
                            CarTheme.AccentPurple
                        )
                    )
                )
            } else {
                CarTheme.LyricsMuted.copy(
                    fontSize = 22.sp,
                    color = CarTheme.TextMuted.copy(alpha = if (isCurrent) 1f else 0.4f)
                )
            },
            textAlign = TextAlign.End,
            maxLines = 1
        )
        
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = CarTheme.TextSecondary.copy(alpha = if (isCurrent) 0.8f else 0.5f)
            ),
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

/**
 * 歌词展示 - 3行
 */
@Composable
private fun LyricsDisplay(effectCommands: EffectCommands?) {
    val lyrics = extractLyrics(effectCommands)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // 上一句
        if (lyrics.size > 1) {
            Text(
                text = lyrics[lyrics.size - 2],
                style = CarTheme.LyricsMuted.copy(
                    fontSize = 24.sp,
                    color = CarTheme.TextMuted.copy(alpha = 0.3f)
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 20.dp)
            )
        }

        // 当前句
        if (lyrics.isNotEmpty()) {
            Box(
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = lyrics.last(),
                    style = CarTheme.LyricsCurrent.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CarTheme.AccentCyan,
                                CarTheme.TextPrimary,
                                CarTheme.AccentPurple
                            )
                        )
                    ),
                    textAlign = TextAlign.End
                )
            }
        }

        // 下一句
        Text(
            text = "下一句歌词...",
            style = CarTheme.LyricsNormal.copy(
                color = CarTheme.TextSecondary.copy(alpha = 0.5f)
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}

/**
 * 歌曲预览
 */
@Composable
private fun SongPreview(effectCommands: EffectCommands?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一首
        PreviewCard(
            icon = "⏮",
            label = "上一首",
            title = extractPrevSong(effectCommands),
            artist = "未知艺术家",
            opacity = 0.6f
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 下一首
        PreviewCard(
            icon = "⏭",
            label = "下一首",
            title = extractNextSong(effectCommands),
            artist = "智能推荐",
            opacity = 0.8f
        )
    }
}

/**
 * 预览卡片
 */
@Composable
private fun PreviewCard(
    icon: String,
    label: String,
    title: String,
    artist: String,
    opacity: Float
) {
    Row(
        modifier = Modifier
            .background(
                color = CarTheme.GlassBg.copy(alpha = opacity),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp
            )
        }

        // 信息
        Column {
            Text(
                text = label.uppercase(),
                style = CarTheme.ChipLabel
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = CarTheme.TextPrimary
                )
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CarTheme.TextSecondary
                )
            )
        }
    }
}

/**
 * 提取歌曲标题
 */
private fun extractSongTitle(effectCommands: EffectCommands?): String {
    return effectCommands?.commands?.content?.playlist?.firstOrNull()?.title
        ?: effectCommands?.scene_id?.let { "场景 $it" }
        ?: "夜雨寄北"
}

/**
 * 提取艺术家
 */
private fun extractArtist(effectCommands: EffectCommands?): String {
    return effectCommands?.commands?.content?.playlist?.firstOrNull()?.artist
        ?: effectCommands?.commands?.content?.playlist?.firstOrNull()?.let { track ->
            track.energy?.let { "能量 ${(it * 100).toInt()}% · 智能生成" }
        }
        ?: "林宥嘉 · 忧郁蓝调"
}

/**
 * 提取歌词
 */
private fun extractLyrics(effectCommands: EffectCommands?): List<String> {
    // 从 EffectCommands 中提取或生成歌词
    val playlist = effectCommands?.commands?.content?.playlist
    return playlist?.map { it.title }?.filter { it.isNotEmpty() }
        ?: listOf(
            "窗外的雨下了一整夜",
            "我在车里听着老唱片",
            "那些回不去的时光啊",
            "像你的温柔在耳边回响"
        )
}

/**
 * 提取上一首
 */
private fun extractPrevSong(effectCommands: EffectCommands?): String {
    return effectCommands?.commands?.content?.playlist?.getOrNull(0)?.title
        ?: "演员"
}

/**
 * 提取下一首
 */
private fun extractNextSong(effectCommands: EffectCommands?): String {
    return effectCommands?.commands?.content?.playlist?.getOrNull(2)?.title
        ?: "成全"
}

/**
 * 提取灯光主题
 */
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

/**
 * 提取音频预设
 */
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

/**
 * 提取歌单
 */
private fun extractPlaylist(effectCommands: EffectCommands?): List<com.music.core.api.models.Track> {
    return effectCommands?.commands?.content?.playlist
        ?: emptyList()
}

package com.music.appmain.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.localmusic.player.PlayerState
import com.music.localmusic.player.PlaybackInfo
import com.music.localmusic.models.Track

@Composable
fun PlayerControlBar(
    playerState: PlayerState,
    playbackInfo: PlaybackInfo,
    playlistSize: Int,
    currentIndex: Int,
    currentAlbumArt: Bitmap?,
    playlist: List<Track>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "音乐播放器",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                if (playlistSize > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${currentIndex + 1}/$playlistSize",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (val state = playerState) {
                is PlayerState.Idle -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "等待 Layer3 播放列表...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                is PlayerState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "加载中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                is PlayerState.Playing -> {
                    CurrentTrackInfo(
                        title = state.track.title,
                        artist = state.track.artist,
                        position = playbackInfo.currentPosition,
                        duration = playbackInfo.duration,
                        albumArt = currentAlbumArt,
                        onSeek = onSeek
                    )
                }
                is PlayerState.Paused -> {
                    CurrentTrackInfo(
                        title = state.track.title,
                        artist = state.track.artist,
                        position = playbackInfo.currentPosition,
                        duration = playbackInfo.duration,
                        albumArt = currentAlbumArt,
                        onSeek = onSeek
                    )
                }

                is PlayerState.Error -> {
                    Text(
                        text = "错误: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            PlaybackControls(
                playerState = playerState,
                onPause = onPause,
                onResume = onResume,
                onNext = onNext,
                onPrevious = onPrevious
            )
            
            // 只有在播放器状态不是 Idle 时才显示播放列表（等待 Layer3 结果）
            if (playlist.isNotEmpty() && playerState !is PlayerState.Idle) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Divider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "播放列表",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                PlaylistView(
                    playlist = playlist,
                    currentIndex = currentIndex,
                    onPlayTrack = onPlayTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CurrentTrackInfo(
    title: String,
    artist: String,
    position: Long,
    duration: Long,
    albumArt: Bitmap?,
    onSeek: (Long) -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        albumArt?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "专辑封面",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } ?: run {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "默认封面",
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .height(72.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (duration > 0) {
                Column {
                    Slider(
                        value = position.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(position),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    playerState: PlayerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val isPlaying = playerState is PlayerState.Playing
    val isPaused = playerState is PlayerState.Paused
    val hasTrack = isPlaying || isPaused
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasTrack) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "上一首",
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            FilledIconButton(
                onClick = if (isPlaying) onPause else onResume,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "下一首",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistView(
    playlist: List<Track>,
    currentIndex: Int,
    onPlayTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < playlist.size) {
            listState.animateScrollToItem(currentIndex)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(playlist) { index, track ->
            PlaylistItem(
                track = track,
                index = index,
                isPlaying = index == currentIndex,
                onClick = { onPlayTrack(index) }
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    track: Track,
    index: Int,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.width(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "正在播放",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

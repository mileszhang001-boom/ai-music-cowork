package com.example.layer3.api

import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.api.model.Track
import com.example.layer3.api.model.Playlist
import com.example.layer3.api.model.PlaylistRecommendation
import kotlinx.coroutines.flow.Flow

interface IContentEngine {
    val currentPlaylistFlow: Flow<Playlist?>
    val currentTrackFlow: Flow<Track?>
    val playbackStateFlow: Flow<PlaybackState>
    
    suspend fun generatePlaylist(scene: SceneDescriptor): Result<Playlist>
    
    suspend fun getRecommendations(basedOn: String, limit: Int = 10): Result<PlaylistRecommendation>
    
    suspend fun playPlaylist(playlist: Playlist): Result<Unit>
    
    suspend fun playTrack(track: Track): Result<Unit>
    
    suspend fun pause(): Result<Unit>
    
    suspend fun resume(): Result<Unit>
    
    suspend fun next(): Result<Unit>
    
    suspend fun previous(): Result<Unit>
    
    suspend fun seek(positionMs: Long): Result<Unit>
    
    suspend fun setVolume(level: Double): Result<Unit>
    
    fun getCurrentTrack(): Track?
    
    fun getCurrentPlaylist(): Playlist?
    
    fun getPlaybackState(): PlaybackState
}

data class PlaybackState(
    val is_playing: Boolean = false,
    val position_ms: Long = 0,
    val duration_ms: Long = 0,
    val shuffle_enabled: Boolean = false,
    val repeat_mode: RepeatMode = RepeatMode.OFF
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

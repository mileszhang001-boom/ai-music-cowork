package com.music.localmusic.player

import com.music.core.api.models.Track

sealed class PlayerState {
    object Idle : PlayerState()
    
    object Loading : PlayerState()
    
    data class Playing(
        val track: Track,
        val position: Long,
        val duration: Long
    ) : PlayerState()
    
    data class Paused(
        val track: Track,
        val position: Long,
        val duration: Long
    ) : PlayerState()
    
    data class Error(
        val message: String,
        val track: Track? = null
    ) : PlayerState()
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

enum class ShuffleMode {
    OFF,
    ON
}

data class PlaybackInfo(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f
)

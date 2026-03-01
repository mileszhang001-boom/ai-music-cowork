package com.music.localmusic.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.music.core.api.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayer(context: Context) {
    private val appContext = context.applicationContext
    
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build()
    private val playlistManager = PlaylistManager()
    
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfo: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()
    
    private var progressUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    _playerState.value = PlayerState.Idle
                }
                Player.STATE_BUFFERING -> {
                    val currentTrack = playlistManager.currentTrack
                    if (currentTrack != null) {
                        _playerState.value = PlayerState.Loading
                    }
                }
                Player.STATE_READY -> {
                    val currentTrack = playlistManager.currentTrack
                    if (currentTrack != null) {
                        if (exoPlayer.playWhenReady) {
                            _playerState.value = PlayerState.Playing(
                                track = currentTrack,
                                position = exoPlayer.currentPosition,
                                duration = exoPlayer.duration
                            )
                            startProgressUpdate()
                        } else {
                            _playerState.value = PlayerState.Paused(
                                track = currentTrack,
                                position = exoPlayer.currentPosition,
                                duration = exoPlayer.duration
                            )
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    onPlaybackEnded()
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val currentTrack = playlistManager.currentTrack
            if (currentTrack != null) {
                if (isPlaying) {
                    _playerState.value = PlayerState.Playing(
                        track = currentTrack,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration
                    )
                    startProgressUpdate()
                } else if (exoPlayer.playbackState == Player.STATE_READY) {
                    _playerState.value = PlayerState.Paused(
                        track = currentTrack,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration
                    )
                    stopProgressUpdate()
                }
            }
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val currentTrack = playlistManager.currentTrack
            _playerState.value = PlayerState.Error(
                message = error.message ?: "Unknown error",
                track = currentTrack
            )
        }
    }
    
    init {
        exoPlayer.addListener(playerListener)
    }
    
    fun play(track: Track, filePath: String? = null) {
        val uri = filePath?.let { Uri.fromFile(java.io.File(it)) }
            ?: Uri.parse(track.track_id)
        
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        
        playlistManager.setPlaylist(listOf(track))
    }
    
    fun play(playlist: List<Track>, startIndex: Int = 0, filePathResolver: ((Track) -> String?)? = null) {
        if (playlist.isEmpty()) return
        
        playlistManager.setPlaylist(playlist, startIndex)
        
        val mediaItems = playlist.map { track ->
            val uri = filePathResolver?.invoke(track)?.let { Uri.fromFile(java.io.File(it)) }
                ?: Uri.parse(track.track_id)
            MediaItem.fromUri(uri)
        }
        
        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }
    
    fun pause() {
        exoPlayer.pause()
    }
    
    fun resume() {
        exoPlayer.play()
    }
    
    fun next() {
        val nextTrack = playlistManager.moveToNext()
        if (nextTrack != null) {
            exoPlayer.seekToNext()
            exoPlayer.playWhenReady = true
        } else {
            stop()
        }
    }
    
    fun previous() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            return
        }
        
        val prevTrack = playlistManager.moveToPrevious()
        if (prevTrack != null) {
            exoPlayer.seekToPrevious()
            exoPlayer.playWhenReady = true
        }
    }
    
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }
    
    fun seekTo(trackIndex: Int, position: Long = 0) {
        if (trackIndex in 0 until exoPlayer.mediaItemCount) {
            playlistManager.setCurrentIndex(trackIndex)
            exoPlayer.seekTo(trackIndex, position)
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playbackInfo.value = _playbackInfo.value.copy(playbackSpeed = speed)
    }
    
    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }
    
    fun stop() {
        exoPlayer.stop()
        _playerState.value = PlayerState.Idle
        stopProgressUpdate()
    }
    
    fun release() {
        stopProgressUpdate()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        _playerState.value = PlayerState.Idle
    }
    
    fun getPlaylistManager(): PlaylistManager = playlistManager
    
    fun getCurrentPosition(): Long = exoPlayer.currentPosition
    
    fun getDuration(): Long = exoPlayer.duration
    
    fun isPlaying(): Boolean = exoPlayer.isPlaying
    
    private fun onPlaybackEnded() {
        when (playlistManager.repeatMode.value) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.playWhenReady = true
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                if (playlistManager.hasNext) {
                    next()
                } else {
                    stop()
                }
            }
        }
    }
    
    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = scope.launch {
            while (isActive) {
                _playbackInfo.value = PlaybackInfo(
                    currentPosition = exoPlayer.currentPosition,
                    duration = exoPlayer.duration,
                    bufferedPosition = exoPlayer.bufferedPosition,
                    isPlaying = exoPlayer.isPlaying,
                    playbackSpeed = exoPlayer.playbackParameters.speed
                )
                delay(100)
            }
        }
    }
    
    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
}

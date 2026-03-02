package com.music.localmusic.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.music.localmusic.models.Track
import com.music.localmusic.session.MediaSessionManager
import com.music.localmusic.util.CoverGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayer(context: Context) {
    
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build()
    private val playlistManager = PlaylistManager()
    private val audioFocusManager: AudioFocusManager = AudioFocusManager(appContext)
    private var mediaSessionManager: MediaSessionManager? = null
    
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfo: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()
    
    private var progressUpdateJob: Job? = null
    private var listener: Listener? = null
    private var currentTrack: Track? = null
    private var wasPlayingBeforeFocusLoss: Boolean = false
    private var isDucking: Boolean = false
    private var savedVolume: Float = 1.0f
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    _playerState.value = PlayerState.Idle
                    listener?.onStateChanged(PlayerState.Idle)
                }
                Player.STATE_BUFFERING -> {
                    val track = playlistManager.currentTrack
                    if (track != null) {
                        _playerState.value = PlayerState.Loading
                        listener?.onStateChanged(PlayerState.Loading)
                        listener?.onBuffering(true)
                    }
                }
                Player.STATE_READY -> {
                    val track = playlistManager.currentTrack
                    if (track != null) {
                        listener?.onBuffering(false)
                        if (exoPlayer.playWhenReady) {
                            val state = PlayerState.Playing(
                                track = track,
                                position = exoPlayer.currentPosition,
                                duration = exoPlayer.duration
                            )
                            _playerState.value = state
                            listener?.onStateChanged(state)
                            startProgressUpdate()
                        } else {
                            val state = PlayerState.Paused(
                                track = track,
                                position = exoPlayer.currentPosition,
                                duration = exoPlayer.duration
                            )
                            _playerState.value = state
                            listener?.onStateChanged(state)
                        }
                        updateMediaSessionMetadata(track)
                    }
                }
                Player.STATE_ENDED -> {
                    onPlaybackEnded()
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val track = playlistManager.currentTrack
            if (track != null) {
                if (isPlaying) {
                    val state = PlayerState.Playing(
                        track = track,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration
                    )
                    _playerState.value = state
                    listener?.onStateChanged(state)
                    startProgressUpdate()
                    updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING)
                } else if (exoPlayer.playbackState == Player.STATE_READY) {
                    val state = PlayerState.Paused(
                        track = track,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration
                    )
                    _playerState.value = state
                    listener?.onStateChanged(state)
                    stopProgressUpdate()
                    updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED)
                }
            }
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val track = playlistManager.currentTrack
            val state = PlayerState.Error(
                message = error.message ?: "Unknown error",
                track = track
            )
            _playerState.value = state
            listener?.onStateChanged(state)
            listener?.onError(error.message ?: "Unknown error")
        }
    }
    
    interface Listener {
        fun onStateChanged(state: PlayerState)
        fun onTrackChanged(track: Track)
        fun onPositionChanged(position: Long, duration: Long)
        fun onBuffering(isBuffering: Boolean)
        fun onError(error: String)
        fun onPlaybackCompleted()
    }
    
    init {
        initializePlayer()
        initializeAudioFocus()
        initializeMediaSession()
    }
    
    private fun initializePlayer() {
        exoPlayer.addListener(playerListener)
    }
    
    private fun initializeAudioFocus() {
        audioFocusManager.setCallback(object : AudioFocusManager.AudioFocusCallback {
            override fun onFocusGained() {
                if (wasPlayingBeforeFocusLoss) {
                    wasPlayingBeforeFocusLoss = false
                    resume()
                }
                if (isDucking) {
                    isDucking = false
                    exoPlayer.volume = savedVolume
                }
            }
            
            override fun onFocusLost() {
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                pause()
            }
            
            override fun onFocusLostTransient() {
                wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                pause()
            }
            
            override fun onFocusLostTransientCanDuck() {
                if (exoPlayer.isPlaying) {
                    isDucking = true
                    savedVolume = exoPlayer.volume
                    exoPlayer.volume = savedVolume * 0.3f
                }
            }
        })
    }
    
    private fun initializeMediaSession() {
        mediaSessionManager = MediaSessionManager(appContext).apply {
            initialize()
            setCallback(object : MediaSessionManager.MediaSessionCallback {
                override fun onPlay() {
                    if (exoPlayer.isPlaying) {
                        pause()
                    } else {
                        resume()
                    }
                }
                
                override fun onPause() {
                    pause()
                }
                
                override fun onStop() {
                    stop()
                }
                
                override fun onSkipToNext() {
                    next()
                }
                
                override fun onSkipToPrevious() {
                    previous()
                }
                
                override fun onSeekTo(positionMs: Long) {
                    seekTo(positionMs)
                }
            })
        }
    }
    
    fun setListener(listener: Listener) {
        this.listener = listener
    }
    
    fun play(track: Track, filePath: String? = null) {
        requestAudioFocusAndPlay()
        
        currentTrack = track
        playlistManager.setPlaylist(listOf(track))
        
        val uri = filePath?.let { Uri.fromFile(java.io.File(it)) }
            ?: Uri.parse(track.filePath)
        
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        
        listener?.onTrackChanged(track)
        updateMediaSessionMetadata(track)
    }
    
    fun play(playlist: List<Track>, startIndex: Int = 0, filePathResolver: ((Track) -> String?)? = null) {
        if (playlist.isEmpty()) return
        
        requestAudioFocusAndPlay()
        
        playlistManager.setPlaylist(playlist, startIndex)
        
        val startTrack = playlist.getOrNull(startIndex) ?: playlist[0]
        currentTrack = startTrack
        
        val mediaItems = playlist.map { track ->
            val uri = filePathResolver?.invoke(track)?.let { Uri.fromFile(java.io.File(it)) }
                ?: Uri.parse(track.filePath)
            MediaItem.fromUri(uri)
        }
        
        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        
        listener?.onTrackChanged(startTrack)
        updateMediaSessionMetadata(startTrack)
    }
    
    fun pause() {
        exoPlayer.pause()
        updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED)
    }
    
    fun resume() {
        requestAudioFocusAndPlay()
        exoPlayer.play()
        updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING)
    }
    
    fun next() {
        val nextTrack = playlistManager.moveToNext()
        if (nextTrack != null) {
            currentTrack = nextTrack
            exoPlayer.seekToNext()
            exoPlayer.playWhenReady = true
            listener?.onTrackChanged(nextTrack)
            updateMediaSessionMetadata(nextTrack)
        } else {
            stop()
            listener?.onPlaybackCompleted()
        }
    }
    
    fun previous() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
            return
        }
        
        val prevTrack = playlistManager.moveToPrevious()
        if (prevTrack != null) {
            currentTrack = prevTrack
            exoPlayer.seekToPrevious()
            exoPlayer.playWhenReady = true
            listener?.onTrackChanged(prevTrack)
            updateMediaSessionMetadata(prevTrack)
        }
    }
    
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        listener?.onPositionChanged(position, exoPlayer.duration)
    }
    
    fun seekTo(trackIndex: Int, position: Long = 0) {
        if (trackIndex in 0 until exoPlayer.mediaItemCount) {
            playlistManager.setCurrentIndex(trackIndex)
            exoPlayer.seekTo(trackIndex, position)
            
            val track = playlistManager.getTrackAt(trackIndex)
            if (track != null) {
                currentTrack = track
                listener?.onTrackChanged(track)
                updateMediaSessionMetadata(track)
            }
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playbackInfo.value = _playbackInfo.value.copy(playbackSpeed = speed)
    }
    
    fun setVolume(volume: Float) {
        savedVolume = volume.coerceIn(0f, 1f)
        if (!isDucking) {
            exoPlayer.volume = savedVolume
        }
    }
    
    fun stop() {
        exoPlayer.stop()
        _playerState.value = PlayerState.Idle
        listener?.onStateChanged(PlayerState.Idle)
        stopProgressUpdate()
        audioFocusManager.abandonAudioFocus()
        updateMediaSessionState(PlaybackStateCompat.STATE_STOPPED)
    }
    
    fun release() {
        stopProgressUpdate()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        audioFocusManager.abandonAudioFocus()
        mediaSessionManager?.release()
        mediaSessionManager = null
        _playerState.value = PlayerState.Idle
    }
    
    fun getPlaylistManager(): PlaylistManager = playlistManager
    
    fun getCurrentPosition(): Long = exoPlayer.currentPosition
    
    fun getDuration(): Long = exoPlayer.duration
    
    fun isPlaying(): Boolean = exoPlayer.isPlaying
    
    fun getCurrentAlbumArt(): Bitmap? {
        return currentTrack?.let { CoverGenerator.generate(it) }
    }
    
    fun toggleRepeatMode(): RepeatMode {
        return playlistManager.toggleRepeatMode()
    }
    
    fun toggleShuffle(): Boolean {
        return playlistManager.toggleShuffle()
    }
    
    fun getQueue(): List<Track> {
        return playlistManager.playlist.value
    }
    
    fun getCurrentTrack(): Track? {
        return currentTrack ?: playlistManager.currentTrack
    }
    
    private fun requestAudioFocusAndPlay() {
        audioFocusManager.requestAudioFocus()
    }
    
    private fun onPlaybackEnded() {
        listener?.onPlaybackCompleted()
        
        when (playlistManager.repeatMode.value) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.playWhenReady = true
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
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
                val position = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                
                _playbackInfo.value = PlaybackInfo(
                    currentPosition = position,
                    duration = duration,
                    bufferedPosition = exoPlayer.bufferedPosition,
                    isPlaying = exoPlayer.isPlaying,
                    playbackSpeed = exoPlayer.playbackParameters.speed
                )
                
                listener?.onPositionChanged(position, duration)
                updateMediaSessionPosition(position)
                
                delay(100)
            }
        }
    }
    
    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
    
    private fun updateMediaSessionMetadata(track: Track) {
        val albumArt = getCurrentAlbumArt()
        mediaSessionManager?.updateMetadata(track, exoPlayer.duration, albumArt)
    }
    
    private fun updateMediaSessionState(state: Int) {
        mediaSessionManager?.updatePlaybackState(state, exoPlayer.currentPosition)
    }
    
    private fun updateMediaSessionPosition(position: Long) {
        val state = if (exoPlayer.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        mediaSessionManager?.updatePlaybackState(state, position)
    }
}

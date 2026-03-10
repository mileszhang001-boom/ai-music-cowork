package com.music.localmusic.player

import android.content.Context
import android.graphics.Bitmap
import android.media.audiofx.Equalizer
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
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
    private var savedVolume: Float = 1.5f
    private val defaultVolumeGain: Float = 1.5f
    private var equalizer: Equalizer? = null
    private var eqEnabled: Boolean = false
    private var pendingBass: Int = 0
    private var pendingMid: Int = 0
    private var pendingTreble: Int = 0
    
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
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = exoPlayer.currentMediaItemIndex
            playlistManager.setCurrentIndex(index)
            val track = playlistManager.currentTrack
            if (track != null) {
                currentTrack = track
                listener?.onTrackChanged(track)
                updateMediaSessionMetadata(track)
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
        exoPlayer.volume = defaultVolumeGain
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
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.playWhenReady = true
        } else if (playlistManager.repeatMode.value == RepeatMode.ALL && exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(0, 0)
            exoPlayer.playWhenReady = true
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

        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.playWhenReady = true
        } else if (playlistManager.repeatMode.value == RepeatMode.ALL && exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(exoPlayer.mediaItemCount - 1, 0)
            exoPlayer.playWhenReady = true
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
        savedVolume = volume.coerceIn(0f, 3.0f)
        if (!isDucking) {
            exoPlayer.volume = savedVolume
        }
    }

    fun setEq(bass: Int, mid: Int, treble: Int) {
        pendingBass = bass
        pendingMid = mid
        pendingTreble = treble
        try {
            equalizer?.release()
            equalizer = null
            val sessionId = exoPlayer.audioSessionId
            if (sessionId == 0) {
                Log.w("MusicPlayer", "audioSessionId=0, EQ deferred")
                return
            }
            equalizer = Equalizer(0, sessionId).apply {
                enabled = eqEnabled
            }
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            if (numBands < 3) return

            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val range = (maxLevel - minLevel).toFloat()

            val bassLevel = (minLevel + range * ((bass + 6).coerceIn(0, 12) / 12f)).toInt().toShort()
            val midLevel = (minLevel + range * ((mid + 6).coerceIn(0, 12) / 12f)).toInt().toShort()
            val trebleLevel = (minLevel + range * ((treble + 6).coerceIn(0, 12) / 12f)).toInt().toShort()

            eq.setBandLevel(0.toShort(), bassLevel)
            if (numBands >= 5) {
                eq.setBandLevel(1.toShort(), bassLevel)
                eq.setBandLevel(2.toShort(), midLevel)
                eq.setBandLevel(3.toShort(), trebleLevel)
                eq.setBandLevel(4.toShort(), trebleLevel)
            } else {
                eq.setBandLevel(1.toShort(), midLevel)
                eq.setBandLevel((numBands - 1).toShort(), trebleLevel)
            }
            Log.d("MusicPlayer", "EQ set: bass=$bass mid=$mid treble=$treble (bands=$numBands, range=$minLevel~$maxLevel, session=$sessionId, enabled=$eqEnabled)")
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Failed to set EQ", e)
        }
    }

    fun reapplyEq() {
        if (eqEnabled && (pendingBass != 0 || pendingMid != 0 || pendingTreble != 0)) {
            Log.d("MusicPlayer", "Reapplying EQ after session change")
            setEq(pendingBass, pendingMid, pendingTreble)
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        eqEnabled = enabled
        try {
            equalizer?.enabled = enabled
            Log.d("MusicPlayer", "EQ enabled=$enabled")
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Failed to toggle EQ", e)
        }
    }

    fun resetAudioEffects() {
        setPlaybackSpeed(1.0f)
        setVolume(defaultVolumeGain)
        setEq(0, 0, 0)
        setEqEnabled(false)
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
        equalizer?.release()
        equalizer = null
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
                playlistManager.setCurrentIndex(0)
                exoPlayer.seekTo(0, 0)
                exoPlayer.playWhenReady = true
                val track = playlistManager.currentTrack
                if (track != null) {
                    currentTrack = track
                    listener?.onTrackChanged(track)
                    updateMediaSessionMetadata(track)
                }
            }
            RepeatMode.OFF -> {
                stop()
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

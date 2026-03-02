package com.music.localmusic.session

import android.content.Context
import android.graphics.Bitmap
import androidx.media.session.MediaSession
import androidx.media.MediaMetadata
import androidx.media.session.PlaybackState
import com.music.localmusic.models.Track

class MediaSessionManager(private val context: Context) {

    private var mediaSession: MediaSession? = null
    private var callback: MediaSessionCallback? = null

    interface MediaSessionCallback {
        fun onPlay()
        fun onPause()
        fun onStop()
        fun onSkipToNext()
        fun onSkipToPrevious()
        fun onSeekTo(positionMs: Long)
    }

    fun initialize() {
        mediaSession = MediaSession.Builder(context, "CarMusicPlayer").build().apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    callback?.onPlay()
                }
                override fun onPause() {
                    callback?.onPause()
                }
                override fun onStop() {
                    callback?.onStop()
                }
                override fun onSkipToNext() {
                    callback?.onSkipToNext()
                }
                override fun onSkipToPrevious() {
                    callback?.onSkipToPrevious()
                }
                override fun onSeekTo(pos: Long) {
                    callback?.onSeekTo(pos)
                }
            })
            isActive = true
        }
    }
    
    fun setCallback(callback: MediaSessionCallback) {
        this.callback = callback
    }
    
    fun updateMetadata(track: Track, duration: Long, albumArt: Bitmap? = null) {
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

        track.album?.let {
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, it)
        }

        albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    fun updatePlaybackState(state: Int, position: Long = 0) {
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )
            .setState(state, position, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    fun release() {
        mediaSession?.release()
        mediaSession = null
    }
}

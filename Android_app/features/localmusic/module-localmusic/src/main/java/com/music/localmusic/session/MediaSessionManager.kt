package com.music.localmusic.session

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.music.localmusic.models.Track

class MediaSessionManager(private val context: Context) {

    private var mediaSession: MediaSessionCompat? = null
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
        mediaSession = MediaSessionCompat(context, "CarMusicPlayer").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
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
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

        track.album?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
        }

        albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    fun updatePlaybackState(state: Int, position: Long = 0) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, position, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    fun release() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }
}

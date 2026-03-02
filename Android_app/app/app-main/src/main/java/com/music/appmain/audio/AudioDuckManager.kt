package com.music.appmain.audio

import android.content.Context
import android.media.AudioManager

class AudioDuckManager(private val context: Context) {

    private var audioManager: AudioManager? = null
    private var volumeCallback: VolumeCallback? = null

    interface VolumeCallback {
        fun onDuckVolume(ratio: Float)
        fun onRestoreVolume()
    }

    fun setVolumeCallback(callback: VolumeCallback) {
        this.volumeCallback = callback
    }

    fun duck() {
        volumeCallback?.onDuckVolume(0.3f)
    }

    fun unduck() {
        volumeCallback?.onRestoreVolume()
    }

    fun release() {
        volumeCallback = null
    }
}

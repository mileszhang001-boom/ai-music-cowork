package com.music.appmain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioDuckManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioDuckManager"
        private const val DUCK_VOLUME = 0.3f
        private const val STREAM_TYPE = AudioManager.STREAM_MUSIC
    }
    
    private val audioManager: AudioManager = 
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var originalVolume: Int = 0
    private var maxVolume: Int = 0
    private var isDucked: Boolean = false
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                if (isDucked) {
                    restoreVolume()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost")
                unduck()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost temporarily")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost transient - can duck")
            }
        }
    }
    
    init {
        maxVolume = audioManager.getStreamMaxVolume(STREAM_TYPE)
        originalVolume = audioManager.getStreamVolume(STREAM_TYPE)
        Log.d(TAG, "Initialized with maxVolume=$maxVolume, originalVolume=$originalVolume")
    }
    
    fun duck(): Boolean {
        if (isDucked) {
            Log.w(TAG, "Already ducked, skipping")
            return true
        }
        
        return try {
            originalVolume = audioManager.getStreamVolume(STREAM_TYPE)
            
            val focusGranted = requestAudioFocus()
            if (!focusGranted) {
                Log.w(TAG, "Failed to gain audio focus, proceeding with duck anyway")
            }
            
            val duckedVolume = (maxVolume * DUCK_VOLUME).toInt().coerceAtLeast(1)
            audioManager.setStreamVolume(
                STREAM_TYPE,
                duckedVolume,
                AudioManager.FLAG_SHOW_UI
            )
            
            isDucked = true
            Log.i(TAG, "Ducked volume from $originalVolume to $duckedVolume")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duck volume", e)
            false
        }
    }
    
    fun unduck(): Boolean {
        if (!isDucked) {
            Log.w(TAG, "Not ducked, skipping unduck")
            return true
        }
        
        return try {
            restoreVolume()
            abandonAudioFocus()
            isDucked = false
            Log.i(TAG, "Unducked volume restored to $originalVolume")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unduck volume", e)
            false
        }
    }
    
    private fun restoreVolume() {
        val currentVolume = audioManager.getStreamVolume(STREAM_TYPE)
        if (currentVolume != originalVolume) {
            audioManager.setStreamVolume(
                STREAM_TYPE,
                originalVolume,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }
    
    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(false)
                .build()
            
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                STREAM_TYPE,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
    
    fun isDucked(): Boolean = isDucked
    
    fun getCurrentVolume(): Int = audioManager.getStreamVolume(STREAM_TYPE)
    
    fun getOriginalVolume(): Int = originalVolume
    
    fun release() {
        if (isDucked) {
            unduck()
        }
        abandonAudioFocus()
        Log.d(TAG, "AudioDuckManager released")
    }
}

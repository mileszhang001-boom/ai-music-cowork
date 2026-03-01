package com.music.perception.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log

class SensorManager(private val context: Context) {

    private var locationManager: LocationManager? = null
    var currentLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                10f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        currentLocation = location
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
            )
        } catch (e: Exception) {
            Log.e("SensorManager", "Location error: ${e.message}")
        }
    }

    data class MicData(
        val volume: Double = 0.0,
        val hasVoice: Boolean = false,
        val voiceCount: Int = 0,
        val noiseLevel: Double = 0.0
    )

    private var baselineNoiseLevel: Double = 0.02
    private var audioRecord: AudioRecord? = null
    private var bufferSize: Int = 0

    @SuppressLint("MissingPermission")
    fun startAudio() {
        if (audioRecord != null) return
        
        val sampleRate = 44100
        bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("SensorManager", "Invalid buffer size")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("SensorManager", "AudioRecord initialization failed")
                audioRecord = null
                return
            }
            
            audioRecord?.startRecording()
            Log.d("SensorManager", "Audio recording started")
        } catch (e: Exception) {
            Log.e("SensorManager", "Error starting audio: ${e.message}")
        }
    }

    fun stopAudio() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d("SensorManager", "Audio recording stopped")
        } catch (e: Exception) {
            Log.e("SensorManager", "Error stopping audio: ${e.message}")
        }
    }

    fun getAudioMetrics(): MicData {
        if (audioRecord == null) {
            startAudio()
            if (audioRecord == null) return MicData()
        }

        val buffer = ShortArray(bufferSize)
        val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1

        if (read > 0) {
            var sumSquare = 0.0
            for (i in 0 until read) {
                val sample = buffer[i].toDouble() / 32767.0
                sumSquare += sample * sample
            }
            val rms = Math.sqrt(sumSquare / read)

            if (rms < baselineNoiseLevel) {
                baselineNoiseLevel = rms
            } else {
                baselineNoiseLevel += (rms - baselineNoiseLevel) * 0.05
            }
            
            if (baselineNoiseLevel > 0.3) baselineNoiseLevel = 0.3

            val gain = 5.0
            val amplifiedRms = (rms * gain).coerceAtMost(1.0)
            val amplifiedNoise = (baselineNoiseLevel * gain).coerceAtMost(1.0)

            val voiceThreshold = (amplifiedNoise * 1.2).coerceAtLeast(0.05)
            
            val hasVoice = amplifiedRms > voiceThreshold

            val voiceCount = if (hasVoice) 1 else 0

            return MicData(
                volume = amplifiedRms,
                hasVoice = hasVoice,
                voiceCount = voiceCount,
                noiseLevel = amplifiedNoise
            )
        }
        return MicData()
    }

    fun getAudioAmplitude(): Double {
        return getAudioMetrics().volume
    }
}

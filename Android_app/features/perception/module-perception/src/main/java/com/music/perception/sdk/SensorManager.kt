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
    private var cumulativeVoiceCount: Int = 0
    private var isRecording: Boolean = false

    @SuppressLint("MissingPermission")
    fun startAudio() {
        Log.d("SensorManager", "startAudio called, audioRecord=$audioRecord")
        if (audioRecord != null) return
        
        val sampleRate = 44100
        bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.d("SensorManager", "bufferSize=$bufferSize")

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("SensorManager", "Invalid buffer size")
            return
        }

        try {
            // 尝试多种音源：DEFAULT -> MIC -> VOICE_RECOGNITION
            val audioSources = listOf(
                MediaRecorder.AudioSource.DEFAULT to "DEFAULT",
                MediaRecorder.AudioSource.MIC to "MIC",
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
                MediaRecorder.AudioSource.CAMCORDER to "CAMCORDER"
            )
            
            for ((source, name) in audioSources) {
                try {
                    Log.d("SensorManager", "Trying audio source: $name ($source)")
                    audioRecord = AudioRecord(
                        source,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    
                    if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                        Log.d("SensorManager", "AudioRecord initialized successfully with source: $name")
                        break
                    } else {
                        Log.w("SensorManager", "AudioRecord failed with source: $name, state=${audioRecord?.state}")
                        audioRecord?.release()
                        audioRecord = null
                    }
                } catch (e: Exception) {
                    Log.w("SensorManager", "AudioSource $name failed: ${e.message}")
                    audioRecord = null
                }
            }
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("SensorManager", "All audio sources failed")
                audioRecord = null
                return
            }
            
            audioRecord?.startRecording()
            isRecording = true
            Log.d("SensorManager", "Audio recording started successfully, recordingState=${audioRecord?.recordingState}")
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
            if (audioRecord == null) {
                Log.e("SensorManager", "AudioRecord is null after startAudio()")
                return MicData()
            }
        }

        val buffer = ShortArray(bufferSize)
        Log.d("SensorManager", "Reading audio buffer, bufferSize=$bufferSize, recordingState=${audioRecord?.recordingState}")
        val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
        Log.d("SensorManager", "Audio read result: $read samples")

        if (read > 0) {
            var sumSquare = 0.0
            var maxSample = 0.0
            for (i in 0 until read) {
                val sample = buffer[i].toDouble() / 32767.0
                sumSquare += sample * sample
                if (Math.abs(sample) > maxSample) maxSample = Math.abs(sample)
            }
            val rms = Math.sqrt(sumSquare / read)
            Log.d("SensorManager", "Raw audio: rms=$rms, maxSample=$maxSample, sumSquare=$sumSquare")

            // 自适应噪音基线更新（更缓慢）
            if (rms < baselineNoiseLevel) {
                baselineNoiseLevel = rms
            } else {
                baselineNoiseLevel += (rms - baselineNoiseLevel) * 0.02
            }
            
            // 限制噪音基线最大值
            if (baselineNoiseLevel > 0.3) baselineNoiseLevel = 0.3

            // 大幅提高信号增益（从3倍改为20倍）
            val gain = 20.0
            val amplifiedRms = (rms * gain).coerceAtMost(1.0)
            val amplifiedNoise = (baselineNoiseLevel * gain).coerceAtMost(1.0)

            // 降低语音阈值：从1.2倍改为1.5倍噪音，最小0.02
            val voiceThreshold = (amplifiedNoise * 1.5).coerceAtLeast(0.02)
            
            // VAD判定：使用放大后的RMS与阈值比较
            val hasVoice = amplifiedRms > voiceThreshold

            // 累积语音计数
            if (hasVoice) {
                cumulativeVoiceCount++
            }

            // 添加调试日志
            Log.d("SensorManager", "Audio: rms=${String.format("%.6f", rms)}, ampRms=${String.format("%.3f", amplifiedRms)}, noise=${String.format("%.3f", amplifiedNoise)}, threshold=${String.format("%.3f", voiceThreshold)}, hasVoice=$hasVoice, voiceCount=$cumulativeVoiceCount")

            return MicData(
                volume = amplifiedRms,
                hasVoice = hasVoice,
                voiceCount = cumulativeVoiceCount,
                noiseLevel = amplifiedNoise
            )
        } else {
            Log.e("SensorManager", "Audio read failed or no data: read=$read")
        }
        return MicData()
    }

    fun getAudioAmplitude(): Double {
        return getAudioMetrics().volume
    }
}

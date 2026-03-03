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

    private var baselineNoiseLevel: Double = 0.0
    private var audioRecord: AudioRecord? = null
    private var bufferSize: Int = 0
    private var isRecording: Boolean = false
    private var noiseCalibrationSamples: Int = 0
    private val noiseCalibrationWindow: DoubleArray = DoubleArray(10)
    private var calibrationIndex: Int = 0
    private var isCalibrated: Boolean = false
    
    private var voiceSegments: Int = 0
    private var lastVoiceState: Boolean = false
    private var voiceSegmentStartTime: Long = 0
    private val voiceSegmentDurations: MutableList<Long> = mutableListOf()

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
        val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1

        if (read > 0) {
            var sumSquare = 0.0
            var maxSample = 0.0
            var zeroCrossings = 0
            
            for (i in 0 until read) {
                val sample = buffer[i].toDouble() / 32767.0
                sumSquare += sample * sample
                if (Math.abs(sample) > maxSample) maxSample = Math.abs(sample)
                if (i > 0 && ((buffer[i-1] > 0 && buffer[i] < 0) || (buffer[i-1] < 0 && buffer[i] > 0))) {
                    zeroCrossings++
                }
            }
            
            val rms = Math.sqrt(sumSquare / read)
            val zeroCrossingRate = zeroCrossings.toDouble() / read
            
            val dbLevel = if (rms > 0) 20 * Math.log10(rms) else -100.0
            val normalizedVolume = ((dbLevel + 60) / 60.0).coerceIn(0.0, 1.0)
            
            val calibratedNoise = if (!isCalibrated && noiseCalibrationSamples < 10) {
                noiseCalibrationWindow[calibrationIndex] = rms
                calibrationIndex = (calibrationIndex + 1) % 10
                noiseCalibrationSamples++
                if (noiseCalibrationSamples >= 10) {
                    isCalibrated = true
                    baselineNoiseLevel = noiseCalibrationWindow.sorted().take(5).average()
                    Log.d("SensorManager", "Noise calibration complete: baseline=$baselineNoiseLevel")
                }
                baselineNoiseLevel
            } else {
                if (rms < baselineNoiseLevel * 0.8) {
                    baselineNoiseLevel = baselineNoiseLevel * 0.99 + rms * 0.01
                }
                baselineNoiseLevel
            }
            
            val normalizedNoise = (calibratedNoise * 50).coerceIn(0.0, 1.0)
            
            val voiceThreshold = (calibratedNoise * 2.5).coerceAtLeast(0.005)
            val highFreqIndicator = zeroCrossingRate > 0.1 && zeroCrossingRate < 0.5
            val energyIndicator = rms > voiceThreshold
            val dynamicIndicator = maxSample > calibratedNoise * 3
            
            val hasVoice = (energyIndicator && (highFreqIndicator || dynamicIndicator)) || 
                           (rms > calibratedNoise * 4)
            
            val currentTime = System.currentTimeMillis()
            if (hasVoice && !lastVoiceState) {
                voiceSegmentStartTime = currentTime
                voiceSegments++
            }
            lastVoiceState = hasVoice
            
            val estimatedSpeakerCount = estimateSpeakerCount(rms, zeroCrossingRate, calibratedNoise)
            
            Log.d("SensorManager", "Audio: rms=${String.format("%.4f", rms)}, db=${String.format("%.1f", dbLevel)}, " +
                    "vol=${String.format("%.3f", normalizedVolume)}, noise=${String.format("%.4f", calibratedNoise)}, " +
                    "zcr=${String.format("%.3f", zeroCrossingRate)}, hasVoice=$hasVoice, speakers=$estimatedSpeakerCount")

            return MicData(
                volume = normalizedVolume,
                hasVoice = hasVoice,
                voiceCount = estimatedSpeakerCount,
                noiseLevel = normalizedNoise
            )
        } else {
            Log.e("SensorManager", "Audio read failed or no data: read=$read")
        }
        return MicData()
    }
    
    private fun estimateSpeakerCount(rms: Double, zcr: Double, noiseLevel: Double): Int {
        if (rms < noiseLevel * 1.5) return 0
        
        val energyRatio = rms / noiseLevel
        
        val speakerEstimate = when {
            energyRatio < 2.0 -> 1
            energyRatio < 4.0 -> if (zcr > 0.15) 2 else 1
            energyRatio < 8.0 -> if (zcr > 0.2) 3 else 2
            else -> if (zcr > 0.25) 4 else 3
        }
        
        return speakerEstimate.coerceIn(0, 4)
    }

    fun getAudioAmplitude(): Double {
        return getAudioMetrics().volume
    }
}

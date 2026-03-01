package com.example.layer3.api

import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.api.model.AudioConfig
import com.example.layer3.api.model.EqualizerBand
import kotlinx.coroutines.flow.Flow

interface IAudioEngine {
    val currentConfigFlow: Flow<AudioConfig?>
    val audioStateFlow: Flow<AudioState>
    
    suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioConfig>
    
    suspend fun applyConfig(config: AudioConfig): Result<Unit>
    
    suspend fun setEqualizerBand(band: EqualizerBand): Result<Unit>
    
    suspend fun setEqualizerPreset(presetName: String): Result<Unit>
    
    suspend fun setBassBoost(level: Double): Result<Unit>
    
    suspend fun setTrebleBoost(level: Double): Result<Unit>
    
    suspend fun enableSpatialAudio(enabled: Boolean): Result<Unit>
    
    suspend fun setSpatialMode(mode: String): Result<Unit>
    
    suspend fun resetToDefault(): Result<Unit>
    
    fun getCurrentConfig(): AudioConfig?
    
    fun getAudioState(): AudioState
    
    fun getAvailablePresets(): List<String>
}

data class AudioState(
    val eq_preset: String = "flat",
    val bass_boost: Double = 0.0,
    val treble_boost: Double = 0.0,
    val spatial_enabled: Boolean = false,
    val spatial_mode: String = "stereo",
    val loudness_enabled: Boolean = true
)

package com.example.layer3.api

import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.AudioCommand
import kotlinx.coroutines.flow.Flow

interface IAudioEngine {
    val audioStateFlow: Flow<AudioCommand>
    
    suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioCommand>
    
    fun applyAudioConfig(command: AudioCommand)
    
    fun setPreset(preset: String)
    
    fun setVolume(volumeDb: Int)
}

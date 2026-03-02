package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.IAudioEngine
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.AudioCommand
import com.music.core.api.models.AudioSettings
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEngine(
    private val context: Context
) : IAudioEngine {
    
    private val _audioStateFlow = MutableStateFlow(AudioCommand(action = "set", preset = "flat", settings = AudioSettings(volume_db = 50)))
    override val audioStateFlow: Flow<AudioCommand> = _audioStateFlow.asStateFlow()

    override suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioCommand> {
        return try {
            val command = AudioCommand(
                action = "set",
                preset = scene.hints.audio?.preset ?: "flat",
                settings = AudioSettings(volume_db = (scene.intent.energy_level * 100).toInt())
            )
            Result.success(command)
        } catch (e: Exception) {
            Logger.e("AudioEngine: Failed to generate audio", e)
            Result.failure(e)
        }
    }

    override fun applyAudioConfig(command: AudioCommand) {
        _audioStateFlow.value = command
        Logger.i("AudioEngine: Applied audio command: $command")
    }

    override fun setVolume(volumeDb: Int) {
        val current = _audioStateFlow.value
        _audioStateFlow.value = current.copy(settings = current.settings?.copy(volume_db = volumeDb) ?: AudioSettings(volume_db = volumeDb))
    }

    override fun setPreset(preset: String) {
        val current = _audioStateFlow.value
        _audioStateFlow.value = current.copy(preset = preset)
    }

    fun destroy() {
        Logger.i("AudioEngine: Destroyed")
    }
}

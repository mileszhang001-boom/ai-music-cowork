package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.IAudioEngine
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.AudioCommand
import com.music.core.api.models.AudioSettings
import com.music.core.api.models.EqSettings
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioPreset(
    val bass: Int,
    val mid: Int,
    val treble: Int,
    val volumeDb: Int,
    val speed: Float
)

class AudioEngine(
    private val context: Context
) : IAudioEngine {

    companion object {
        private val AUDIO_PRESETS = mapOf(
            "standard" to AudioPreset(bass = 0, mid = 0, treble = 0, volumeDb = 0, speed = 1.0f),
            "night_mode" to AudioPreset(bass = -3, mid = -2, treble = 1, volumeDb = -6, speed = 0.93f),
            "outdoor" to AudioPreset(bass = 4, mid = 2, treble = 1, volumeDb = 4, speed = 1.0f),
            "bass_boost" to AudioPreset(bass = 6, mid = 0, treble = -2, volumeDb = 2, speed = 1.0f),
            "rain_mode" to AudioPreset(bass = -2, mid = 0, treble = 3, volumeDb = -3, speed = 0.95f),
            "vocal_clarity" to AudioPreset(bass = -2, mid = 4, treble = 2, volumeDb = 0, speed = 1.0f),
            "energetic" to AudioPreset(bass = 5, mid = 2, treble = 2, volumeDb = 5, speed = 1.08f),
            "relaxed" to AudioPreset(bass = 2, mid = 0, treble = -2, volumeDb = -4, speed = 0.93f),
            "kids" to AudioPreset(bass = -3, mid = 2, treble = 4, volumeDb = -3, speed = 1.0f),
            "romantic" to AudioPreset(bass = 2, mid = 2, treble = 0, volumeDb = -5, speed = 0.93f)
        )

        private val SCENE_PRESET_MAP = mapOf(
            "morning_commute" to "vocal_clarity",
            "evening_commute" to "standard",
            "highway_cruise" to "bass_boost",
            "city_drive" to "standard",
            "rainy_drive" to "rain_mode",
            "night_drive" to "night_mode",
            "sunset_drive" to "relaxed",
            "kids_mode" to "kids",
            "romantic_evening" to "romantic",
            "fatigue_alert" to "energetic",
            "beach_vacation" to "outdoor",
            "road_trip" to "bass_boost",
            "focus_work" to "vocal_clarity",
            "party_mode" to "energetic",
            "meditation" to "relaxed",
            "workout" to "energetic",
            "study" to "vocal_clarity",
            "sleep" to "night_mode"
        )
    }
    
    private val _audioStateFlow = MutableStateFlow(AudioCommand(action = "set", preset = "standard", settings = AudioSettings(volume_db = 0)))
    override val audioStateFlow: Flow<AudioCommand> = _audioStateFlow.asStateFlow()

    override suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioCommand> {
        return try {
            val hintPreset = scene.hints.audio?.preset
            val scenePreset = SCENE_PRESET_MAP[scene.scene_type]
            val presetName = hintPreset ?: scenePreset ?: "standard"
            val preset = AUDIO_PRESETS[presetName] ?: AUDIO_PRESETS["standard"]!!

            val constraintVolumeDb = scene.intent.constraints?.max_volume_db
            val finalVolumeDb = constraintVolumeDb ?: preset.volumeDb

            val energy = scene.intent.energy_level
            val speedAdjust = when {
                energy >= 0.8 -> 0.05f
                energy >= 0.6 -> 0.02f
                energy <= 0.2 -> -0.05f
                energy <= 0.3 -> -0.03f
                else -> 0f
            }
            val finalSpeed = (preset.speed + speedAdjust).coerceIn(0.85f, 1.15f)

            val command = AudioCommand(
                action = "set",
                preset = presetName,
                settings = AudioSettings(
                    eq = EqSettings(
                        bass = preset.bass,
                        mid = preset.mid,
                        treble = preset.treble
                    ),
                    volume_db = finalVolumeDb,
                    speed = finalSpeed
                )
            )
            Logger.i("AudioEngine: preset=$presetName, eq=(${preset.bass}/${preset.mid}/${preset.treble}), vol=${finalVolumeDb}dB, speed=$finalSpeed for scene=${scene.scene_name}")
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
        val presetConfig = AUDIO_PRESETS[preset] ?: AUDIO_PRESETS["standard"]!!
        _audioStateFlow.value = current.copy(
            preset = preset,
            settings = AudioSettings(
                eq = EqSettings(bass = presetConfig.bass, mid = presetConfig.mid, treble = presetConfig.treble),
                volume_db = presetConfig.volumeDb,
                speed = presetConfig.speed
            )
        )
    }

    fun destroy() {
        Logger.i("AudioEngine: Destroyed")
    }
}

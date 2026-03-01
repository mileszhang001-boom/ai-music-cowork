package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.*
import com.example.layer3.api.model.*
import com.example.layer3.sdk.data.CacheManager
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AudioEngine(
    private val context: Context,
    private val config: AudioEngineConfig
) : IAudioEngine {
    private val configCache = CacheManager<AudioConfig>(maxSize = 20)
    
    private val _currentConfigFlow = MutableStateFlow<AudioConfig?>(null)
    private val _audioStateFlow = MutableStateFlow(AudioState())
    
    private var currentConfig: AudioConfig? = null
    private var audioState = AudioState()

    override val currentConfigFlow: Flow<AudioConfig?> = _currentConfigFlow.asStateFlow()
    override val audioStateFlow: Flow<AudioState> = _audioStateFlow.asStateFlow()

    override suspend fun generateAudioConfig(scene: SceneDescriptor): Result<AudioConfig> {
        return try {
            val cacheKey = "audio_${scene.sceneId}"
            configCache.get(cacheKey)?.let {
                Logger.d("AudioEngine: Returning cached config for scene ${scene.sceneId}")
                return Result.success(it)
            }

            val hints = scene.hints?.audio
            val mood = scene.intent.mood
            
            val preset = hints?.preset ?: inferPreset(mood, scene.intent.energyLevel)
            val spatialMode = hints?.spatialMode ?: inferSpatialMode(scene.intent.energyLevel)
            val bassBoost = hints?.bassBoost ?: calculateBassBoost(mood, scene.intent.energyLevel)
            val trebleBoost = hints?.trebleBoost ?: calculateTrebleBoost(mood)
            
            val equalizer = createEqualizerConfig(preset, bassBoost, trebleBoost)
            val spatial = createSpatialConfig(spatialMode)
            
            val audioConfig = AudioConfig(
                configId = UUID.randomUUID().toString(),
                equalizer = equalizer,
                spatial = spatial,
                volume = VolumeConfig(
                    level = 0.5,
                    muted = false,
                    autoAdjust = true
                ),
                enhancements = AudioEnhancements(
                    bassBoost = bassBoost,
                    trebleBoost = trebleBoost,
                    loudnessEnabled = true,
                    surroundEnabled = spatialMode != SpatialModes.STEREO
                ),
                active = true
            )
            
            configCache.put(cacheKey, audioConfig)
            currentConfig = audioConfig
            audioState = AudioState(
                eqPreset = preset,
                bassBoost = bassBoost,
                trebleBoost = trebleBoost,
                spatialEnabled = spatial.enabled,
                spatialMode = spatialMode
            )
            
            _currentConfigFlow.value = audioConfig
            _audioStateFlow.value = audioState
            
            Logger.i("AudioEngine: Generated audio config with preset=$preset")
            Result.success(audioConfig)
        } catch (e: Exception) {
            Logger.e("AudioEngine: Failed to generate config", e)
            Result.failure(Layer3Error.AudioError("Failed to generate audio config: ${e.message}"))
        }
    }

    override suspend fun applyConfig(config: AudioConfig): Result<Unit> {
        return try {
            currentConfig = config
            audioState = AudioState(
                eqPreset = config.equalizer?.presetName ?: AudioPresets.FLAT,
                bassBoost = config.enhancements.bassBoost,
                trebleBoost = config.enhancements.trebleBoost,
                spatialEnabled = config.spatial?.enabled ?: false,
                spatialMode = config.spatial?.mode ?: SpatialModes.STEREO
            )
            _currentConfigFlow.value = config
            _audioStateFlow.value = audioState
            Logger.i("AudioEngine: Applied config ${config.configId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Layer3Error.AudioError("Failed to apply config: ${e.message}"))
        }
    }

    override suspend fun setEqualizerBand(band: EqualizerBand): Result<Unit> {
        return try {
            val config = currentConfig ?: return Result.failure(Layer3Error.AudioError("No active config"))
            val updatedBands = config.equalizer?.bands?.map {
                if (it.frequencyHz == band.frequencyHz) band else it
            } ?: listOf(band)
            
            applyConfig(config.copy(
                equalizer = config.equalizer?.copy(bands = updatedBands)
            ))
        } catch (e: Exception) {
            Result.failure(Layer3Error.AudioError("Failed to set EQ band: ${e.message}"))
        }
    }

    override suspend fun setEqualizerPreset(presetName: String): Result<Unit> {
        return try {
            val config = currentConfig ?: return Result.failure(Layer3Error.AudioError("No active config"))
            val presetBands = getPresetBands(presetName)
            
            applyConfig(config.copy(
                equalizer = EqualizerConfig(
                    enabled = true,
                    bands = presetBands,
                    presetName = presetName
                )
            ))
        } catch (e: Exception) {
            Result.failure(Layer3Error.AudioError("Failed to set EQ preset: ${e.message}"))
        }
    }

    override suspend fun setBassBoost(level: Double): Result<Unit> {
        val clampedLevel = level.coerceIn(0.0, config.maxBassBoost)
        audioState = audioState.copy(bassBoost = clampedLevel)
        _audioStateFlow.value = audioState
        return Result.success(Unit)
    }

    override suspend fun setTrebleBoost(level: Double): Result<Unit> {
        val clampedLevel = level.coerceIn(0.0, config.maxTrebleBoost)
        audioState = audioState.copy(trebleBoost = clampedLevel)
        _audioStateFlow.value = audioState
        return Result.success(Unit)
    }

    override suspend fun enableSpatialAudio(enabled: Boolean): Result<Unit> {
        audioState = audioState.copy(spatialEnabled = enabled)
        _audioStateFlow.value = audioState
        return Result.success(Unit)
    }

    override suspend fun setSpatialMode(mode: String): Result<Unit> {
        audioState = audioState.copy(spatialMode = mode)
        _audioStateFlow.value = audioState
        return Result.success(Unit)
    }

    override suspend fun resetToDefault(): Result<Unit> {
        return try {
            audioState = AudioState(
                eqPreset = config.defaultPreset,
                bassBoost = 0.0,
                trebleBoost = 0.0,
                spatialEnabled = config.enableSpatialAudio,
                spatialMode = config.defaultSpatialMode
            )
            _audioStateFlow.value = audioState
            Logger.i("AudioEngine: Reset to default")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Layer3Error.AudioError("Failed to reset: ${e.message}"))
        }
    }

    override fun getCurrentConfig(): AudioConfig? = currentConfig

    override fun getAudioState(): AudioState = audioState

    override fun getAvailablePresets(): List<String> = listOf(
        AudioPresets.FLAT,
        AudioPresets.BASS_BOOST,
        AudioPresets.TREBLE_BOOST,
        AudioPresets.VOCAL,
        AudioPresets.ROCK,
        AudioPresets.POP,
        AudioPresets.CLASSICAL,
        AudioPresets.ELECTRONIC,
        AudioPresets.JAZZ,
        AudioPresets.PODCAST
    )

    private fun inferPreset(mood: Mood, energyLevel: Double): String {
        return when {
            energyLevel > 0.7 -> AudioPresets.ELECTRONIC
            energyLevel > 0.5 -> AudioPresets.POP
            mood.valence < 0.4 -> AudioPresets.CLASSICAL
            mood.arousal < 0.3 -> AudioPresets.JAZZ
            else -> AudioPresets.FLAT
        }
    }

    private fun inferSpatialMode(energyLevel: Double): String {
        return when {
            energyLevel > 0.7 && config.enableSpatialAudio -> SpatialModes.SURROUND_5_1
            config.enableSpatialAudio -> SpatialModes.STEREO
            else -> SpatialModes.STEREO
        }
    }

    private fun calculateBassBoost(mood: Mood, energyLevel: Double): Double {
        val baseBoost = if (energyLevel > 0.6) 0.15 else 0.05
        val moodAdjustment = mood.arousal * 0.1
        return (baseBoost + moodAdjustment).coerceIn(0.0, config.maxBassBoost)
    }

    private fun calculateTrebleBoost(mood: Mood): Double {
        return if (mood.valence > 0.6) 0.05 else 0.0
    }

    private fun createEqualizerConfig(preset: String, bassBoost: Double, trebleBoost: Double): EqualizerConfig {
        val baseBands = getPresetBands(preset)
        val adjustedBands = baseBands.map { band ->
            when {
                band.frequencyHz < 200 -> band.copy(gainDb = band.gainDb + bassBoost * 10)
                band.frequencyHz > 8000 -> band.copy(gainDb = band.gainDb + trebleBoost * 10)
                else -> band
            }
        }
        
        return EqualizerConfig(
            enabled = true,
            bands = adjustedBands,
            presetName = preset
        )
    }

    private fun createSpatialConfig(mode: String): SpatialAudioConfig {
        return SpatialAudioConfig(
            enabled = mode != SpatialModes.STEREO && config.enableSpatialAudio,
            mode = mode,
            roomSize = "medium",
            listenerPosition = listOf(0.5, 0.5)
        )
    }

    private fun getPresetBands(preset: String): List<EqualizerBand> {
        return when (preset) {
            AudioPresets.BASS_BOOST -> listOf(
                EqualizerBand(60, 6.0),
                EqualizerBand(150, 4.0),
                EqualizerBand(400, 0.0),
                EqualizerBand(1000, 0.0),
                EqualizerBand(2400, 0.0),
                EqualizerBand(6000, 0.0),
                EqualizerBand(15000, 0.0)
            )
            AudioPresets.TREBLE_BOOST -> listOf(
                EqualizerBand(60, 0.0),
                EqualizerBand(150, 0.0),
                EqualizerBand(400, 0.0),
                EqualizerBand(1000, 0.0),
                EqualizerBand(2400, 2.0),
                EqualizerBand(6000, 4.0),
                EqualizerBand(15000, 6.0)
            )
            AudioPresets.VOCAL -> listOf(
                EqualizerBand(60, -2.0),
                EqualizerBand(150, 0.0),
                EqualizerBand(400, 2.0),
                EqualizerBand(1000, 4.0),
                EqualizerBand(2400, 3.0),
                EqualizerBand(6000, 1.0),
                EqualizerBand(15000, 0.0)
            )
            AudioPresets.ROCK -> listOf(
                EqualizerBand(60, 4.0),
                EqualizerBand(150, 2.0),
                EqualizerBand(400, -1.0),
                EqualizerBand(1000, 0.0),
                EqualizerBand(2400, 1.0),
                EqualizerBand(6000, 3.0),
                EqualizerBand(15000, 4.0)
            )
            AudioPresets.ELECTRONIC -> listOf(
                EqualizerBand(60, 5.0),
                EqualizerBand(150, 3.0),
                EqualizerBand(400, 0.0),
                EqualizerBand(1000, -1.0),
                EqualizerBand(2400, 0.0),
                EqualizerBand(6000, 2.0),
                EqualizerBand(15000, 4.0)
            )
            AudioPresets.CLASSICAL -> listOf(
                EqualizerBand(60, 2.0),
                EqualizerBand(150, 1.0),
                EqualizerBand(400, 0.0),
                EqualizerBand(1000, 0.0),
                EqualizerBand(2400, 0.0),
                EqualizerBand(6000, 1.0),
                EqualizerBand(15000, 2.0)
            )
            AudioPresets.JAZZ -> listOf(
                EqualizerBand(60, 2.0),
                EqualizerBand(150, 1.0),
                EqualizerBand(400, 1.0),
                EqualizerBand(1000, 0.0),
                EqualizerBand(2400, 1.0),
                EqualizerBand(6000, 2.0),
                EqualizerBand(15000, 2.0)
            )
            else -> listOf(
                EqualizerBand(60, 0.0),
                EqualizerBand(150, 0.0),
                EqualizerBand(400, 0.0),
                EqualizerBand(1000, 0.0),
                EqualizerBand(2400, 0.0),
                EqualizerBand(6000, 0.0),
                EqualizerBand(15000, 0.0)
            )
        }
    }

    fun destroy() {
        configCache.clear()
        _currentConfigFlow.value = null
        Logger.i("AudioEngine: Destroyed")
    }
}

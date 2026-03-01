package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.*
import com.example.layer3.api.model.*
import com.example.layer3.sdk.algorithm.ColorAdjuster
import com.example.layer3.sdk.data.CacheManager
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LightingEngine(
    private val context: Context,
    private val config: LightingControllerConfig
) : ILightingEngine {
    private val colorAdjuster = ColorAdjuster()
    private val configCache = CacheManager<LightingConfig>(maxSize = 20)
    
    private val _currentConfigFlow = MutableStateFlow<LightingConfig?>(null)
    private val _lightingStateFlow = MutableStateFlow(LightingState())
    
    private var currentConfig: LightingConfig? = null
    private var lightingState = LightingState()

    override val currentConfigFlow: Flow<LightingConfig?> = _currentConfigFlow.asStateFlow()
    override val lightingStateFlow: Flow<LightingState> = _lightingStateFlow.asStateFlow()

    init {
        initializeThemeColors()
    }

    private fun initializeThemeColors() {
        colorAdjuster.setThemeColors("happy", listOf("#FFD700", "#FFA500", "#FF6347"))
        colorAdjuster.setThemeColors("calm", listOf("#4169E1", "#87CEEB", "#98FB98"))
        colorAdjuster.setThemeColors("energetic", listOf("#FF4500", "#FF1493", "#9400D3"))
        colorAdjuster.setThemeColors("focused", listOf("#FFFFFF", "#F0F8FF", "#E6E6FA"))
        colorAdjuster.setThemeColors("romantic", listOf("#FF69B4", "#FFB6C1", "#DDA0DD"))
    }

    override suspend fun generateLightingConfig(scene: SceneDescriptor): Result<LightingConfig> {
        return try {
            val cacheKey = "lighting_${scene.sceneId}"
            configCache.get(cacheKey)?.let {
                Logger.d("LightingEngine: Returning cached config for scene ${scene.sceneId}")
                return Result.success(it)
            }

            val mood = scene.intent.mood
            val hints = scene.hints?.lighting
            
            val colorTheme = hints?.colorTheme ?: inferColorTheme(mood)
            val pattern = hints?.pattern ?: inferPattern(scene.intent.energyLevel)
            val intensity = hints?.intensity ?: calculateIntensity(mood)
            
            val colors = colorAdjuster.getColorsForTheme(colorTheme)
            val zoneConfigs = generateZoneConfigs(colors, intensity, pattern)
            
            val lightingScene = LightingScene(
                sceneId = scene.sceneId,
                name = scene.sceneName ?: "Lighting Scene",
                zones = zoneConfigs,
                animation = AnimationConfig(
                    type = getAnimationType(pattern),
                    durationMs = 1000,
                    easing = "easeInOut",
                    loop = true
                ),
                syncWithMusic = shouldSyncWithMusic(scene),
                syncIntensity = intensity
            )
            
            val config = LightingConfig(
                configId = UUID.randomUUID().toString(),
                scene = lightingScene,
                transitionDurationMs = config.timeoutMs,
                active = true
            )
            
            configCache.put(cacheKey, config)
            currentConfig = config
            _currentConfigFlow.value = config
            
            Logger.i("LightingEngine: Generated config with ${zoneConfigs.size} zones")
            Result.success(config)
        } catch (e: Exception) {
            Logger.e("LightingEngine: Failed to generate config", e)
            Result.failure(Layer3Error.LightingError("Failed to generate lighting config: ${e.message}"))
        }
    }

    override suspend fun applyConfig(config: LightingConfig): Result<Unit> {
        return try {
            currentConfig = config
            lightingState = LightingState(
                isOn = config.active,
                activeZones = config.scene.zones.map { it.zoneId },
                currentPattern = config.scene.zones.firstOrNull()?.pattern ?: "static",
                brightness = config.scene.zones.firstOrNull()?.brightness ?: 1.0,
                musicSyncEnabled = config.scene.syncWithMusic
            )
            _currentConfigFlow.value = config
            _lightingStateFlow.value = lightingState
            Logger.i("LightingEngine: Applied config ${config.configId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Layer3Error.LightingError("Failed to apply config: ${e.message}"))
        }
    }

    override suspend fun setZoneConfig(zoneId: String, zoneConfig: ZoneConfig): Result<Unit> {
        return try {
            val config = currentConfig ?: return Result.failure(Layer3Error.LightingError("No active config"))
            val updatedZones = config.scene.zones.map {
                if (it.zoneId == zoneId) zoneConfig else it
            }
            val updatedConfig = config.copy(
                scene = config.scene.copy(zones = updatedZones)
            )
            applyConfig(updatedConfig)
        } catch (e: Exception) {
            Result.failure(Layer3Error.LightingError("Failed to set zone config: ${e.message}"))
        }
    }

    override suspend fun setBrightness(level: Double): Result<Unit> {
        lightingState = lightingState.copy(brightness = level.coerceIn(0.0, 1.0))
        _lightingStateFlow.value = lightingState
        return Result.success(Unit)
    }

    override suspend fun setColor(hex: String): Result<Unit> {
        return try {
            val config = currentConfig ?: return Result.failure(Layer3Error.LightingError("No active config"))
            val updatedZones = config.scene.zones.map {
                it.copy(color = ColorConfig(hex = hex))
            }
            applyConfig(config.copy(scene = config.scene.copy(zones = updatedZones)))
        } catch (e: Exception) {
            Result.failure(Layer3Error.LightingError("Failed to set color: ${e.message}"))
        }
    }

    override suspend fun setPattern(pattern: String): Result<Unit> {
        lightingState = lightingState.copy(currentPattern = pattern)
        _lightingStateFlow.value = lightingState
        return Result.success(Unit)
    }

    override suspend fun enableMusicSync(enabled: Boolean): Result<Unit> {
        lightingState = lightingState.copy(musicSyncEnabled = enabled)
        _lightingStateFlow.value = lightingState
        return Result.success(Unit)
    }

    override suspend fun turnOff(): Result<Unit> {
        lightingState = lightingState.copy(isOn = false)
        _lightingStateFlow.value = lightingState
        return Result.success(Unit)
    }

    override suspend fun turnOn(): Result<Unit> {
        lightingState = lightingState.copy(isOn = true)
        _lightingStateFlow.value = lightingState
        return Result.success(Unit)
    }

    override fun getCurrentConfig(): LightingConfig? = currentConfig

    override fun getLightingState(): LightingState = lightingState

    override fun getAvailableZones(): List<String> = listOf(
        LightingZones.DASHBOARD,
        LightingZones.DOOR_LEFT,
        LightingZones.DOOR_RIGHT,
        LightingZones.FOOTWELL,
        LightingZones.CEILING
    )

    private fun inferColorTheme(mood: Mood): String {
        val valence = mood.valence
        val arousal = mood.arousal
        
        return when {
            valence > 0.6 && arousal > 0.5 -> "happy"
            valence < 0.4 -> "melancholic"
            arousal > 0.7 -> "energetic"
            arousal < 0.3 -> "calm"
            valence > 0.5 && arousal < 0.5 -> "romantic"
            else -> "focused"
        }
    }

    private fun inferPattern(energyLevel: Double): String {
        return when {
            energyLevel > 0.7 -> LightingPatterns.MUSIC_SYNC
            energyLevel > 0.4 -> LightingPatterns.PULSE
            energyLevel > 0.2 -> LightingPatterns.BREATHING
            else -> LightingPatterns.STATIC
        }
    }

    private fun calculateIntensity(mood: Mood): Double {
        return (mood.arousal * 0.7 + mood.valence * 0.3).coerceIn(0.2, 1.0)
    }

    private fun generateZoneConfigs(
        colors: List<String>,
        intensity: Double,
        pattern: String
    ): List<ZoneConfig> {
        val zones = getAvailableZones()
        return zones.mapIndexed { index, zoneId ->
            val colorIndex = index % colors.size
            val adjustedColor = colorAdjuster.adjustColor(colors[colorIndex], intensity)
            
            ZoneConfig(
                zoneId = zoneId,
                enabled = true,
                color = adjustedColor,
                brightness = intensity,
                pattern = pattern,
                speed = calculatePatternSpeed(pattern, intensity)
            )
        }
    }

    private fun calculatePatternSpeed(pattern: String, intensity: Double): Double {
        return when (pattern) {
            LightingPatterns.BREATHING -> 0.5 + intensity * 0.5
            LightingPatterns.PULSE -> 0.8 + intensity * 0.7
            LightingPatterns.WAVE -> 1.0 + intensity * 0.5
            else -> 1.0
        }
    }

    private fun getAnimationType(pattern: String): String {
        return when (pattern) {
            LightingPatterns.BREATHING -> "fade"
            LightingPatterns.PULSE -> "pulse"
            LightingPatterns.WAVE -> "wave"
            LightingPatterns.MUSIC_SYNC -> "reactive"
            else -> "none"
        }
    }

    private fun shouldSyncWithMusic(scene: SceneDescriptor): Boolean {
        return scene.intent.energyLevel > 0.5
    }

    fun destroy() {
        configCache.clear()
        _currentConfigFlow.value = null
        Logger.i("LightingEngine: Destroyed")
    }
}

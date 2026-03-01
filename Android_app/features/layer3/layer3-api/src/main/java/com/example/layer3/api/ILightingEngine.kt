package com.example.layer3.api

import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.api.model.LightingConfig
import com.example.layer3.api.model.ZoneConfig
import kotlinx.coroutines.flow.Flow

interface ILightingEngine {
    val currentConfigFlow: Flow<LightingConfig?>
    val lightingStateFlow: Flow<LightingState>
    
    suspend fun generateLightingConfig(scene: SceneDescriptor): Result<LightingConfig>
    
    suspend fun applyConfig(config: LightingConfig): Result<Unit>
    
    suspend fun setZoneConfig(zoneId: String, zoneConfig: ZoneConfig): Result<Unit>
    
    suspend fun setBrightness(level: Double): Result<Unit>
    
    suspend fun setColor(hex: String): Result<Unit>
    
    suspend fun setPattern(pattern: String): Result<Unit>
    
    suspend fun enableMusicSync(enabled: Boolean): Result<Unit>
    
    suspend fun turnOff(): Result<Unit>
    
    suspend fun turnOn(): Result<Unit>
    
    fun getCurrentConfig(): LightingConfig?
    
    fun getLightingState(): LightingState
    
    fun getAvailableZones(): List<String>
}

data class LightingState(
    val is_on: Boolean = false,
    val active_zones: List<String> = emptyList(),
    val current_pattern: String = "static",
    val brightness: Double = 1.0,
    val music_sync_enabled: Boolean = false
)

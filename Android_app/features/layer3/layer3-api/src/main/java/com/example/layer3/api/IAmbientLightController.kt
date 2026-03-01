package com.example.layer3.api

import com.example.layer3.api.model.LightingConfig
import com.example.layer3.api.model.ZoneConfig
import kotlinx.coroutines.flow.Flow

interface IAmbientLightController {
    val connectionStateFlow: Flow<ConnectionState>
    val controllerStateFlow: Flow<ControllerState>
    
    suspend fun connect(address: String): Result<Unit>
    
    suspend fun disconnect(): Result<Unit>
    
    suspend fun discoverControllers(): Result<List<ControllerInfo>>
    
    suspend fun applyLightingConfig(config: LightingConfig): Result<Unit>
    
    suspend fun setZoneColor(zoneId: String, hexColor: String): Result<Unit>
    
    suspend fun setZoneBrightness(zoneId: String, brightness: Double): Result<Unit>
    
    suspend fun setZonePattern(zoneId: String, pattern: String): Result<Unit>
    
    suspend fun turnZoneOn(zoneId: String): Result<Unit>
    
    suspend fun turnZoneOff(zoneId: String): Result<Unit>
    
    suspend fun turnAllOn(): Result<Unit>
    
    suspend fun turnAllOff(): Result<Unit>
    
    suspend fun startMusicSync(audioSource: String): Result<Unit>
    
    suspend fun stopMusicSync(): Result<Unit>
    
    fun getConnectionState(): ConnectionState
    
    fun getControllerState(): ControllerState
    
    fun getSupportedZones(): List<String>
    
    fun getSupportedPatterns(): List<String>
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ControllerState(
    val is_on: Boolean = false,
    val active_zones: List<String> = emptyList(),
    val current_pattern: String = "static",
    val brightness: Double = 1.0,
    val music_sync_active: Boolean = false
)

data class ControllerInfo(
    val id: String,
    val name: String,
    val address: String,
    val type: String,
    val firmware_version: String? = null,
    val supported_zones: List<String> = emptyList(),
    val supported_patterns: List<String> = emptyList()
)

package com.example.demo.controller

import android.util.Log
import com.example.layer3.api.*
import com.example.layer3.api.model.LightingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAmbientLightController : IAmbientLightController {
    
    private val tag = "MockAmbientLightController"
    
    private val _connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _controllerStateFlow = MutableStateFlow(ControllerState())
    
    private var currentState = ControllerState()
    
    override val connectionStateFlow: Flow<ConnectionState> = _connectionStateFlow.asStateFlow()
    override val controllerStateFlow: Flow<ControllerState> = _controllerStateFlow.asStateFlow()
    
    override suspend fun connect(address: String): Result<Unit> {
        Log.d(tag, "connect() called with address: $address")
        _connectionStateFlow.value = ConnectionState.CONNECTING
        
        Thread.sleep(500)
        
        _connectionStateFlow.value = ConnectionState.CONNECTED
        Log.i(tag, "Connected to mock controller at $address")
        
        return Result.success(Unit)
    }
    
    override suspend fun disconnect(): Result<Unit> {
        Log.d(tag, "disconnect() called")
        _connectionStateFlow.value = ConnectionState.DISCONNECTED
        currentState = ControllerState()
        _controllerStateFlow.value = currentState
        
        Log.i(tag, "Disconnected from mock controller")
        return Result.success(Unit)
    }
    
    override suspend fun discoverControllers(): Result<List<ControllerInfo>> {
        Log.d(tag, "discoverControllers() called")
        
        val controllers = listOf(
            ControllerInfo(
                id = "mock-controller-1",
                name = "Mock Ambient Light Controller",
                address = "192.168.1.100",
                type = "mock",
                firmware_version = "1.0.0",
                supported_zones = listOf("dashboard", "door_left", "door_right", "footwell", "ceiling"),
                supported_patterns = listOf("static", "breathing", "pulse", "wave", "music_sync", "rainbow")
            )
        )
        
        Log.i(tag, "Discovered ${controllers.size} mock controllers")
        return Result.success(controllers)
    }
    
    override suspend fun applyLightingConfig(config: LightingConfig): Result<Unit> {
        Log.d(tag, "applyLightingConfig() called with config: ${config.config_id}")
        Log.i(tag, "Applying lighting config for scene: ${config.scene.name}")
        Log.i(tag, "  - Zones: ${config.scene.zones.map { it.zone_id }}")
        Log.i(tag, "  - Sync with music: ${config.scene.sync_with_music}")
        Log.i(tag, "  - Transition duration: ${config.transition_duration_ms}ms")
        
        config.scene.zones.forEach { zone ->
            Log.i(tag, "  Zone ${zone.zone_id}:")
            Log.i(tag, "    - Color: ${zone.color.hex}")
            Log.i(tag, "    - Brightness: ${zone.brightness}")
            Log.i(tag, "    - Pattern: ${zone.pattern}")
            Log.i(tag, "    - Speed: ${zone.speed}")
        }
        
        currentState = ControllerState(
            is_on = config.active,
            active_zones = config.scene.zones.filter { it.enabled }.map { it.zone_id },
            current_pattern = config.scene.zones.firstOrNull()?.pattern ?: "static",
            brightness = config.scene.zones.firstOrNull()?.brightness ?: 1.0,
            music_sync_active = config.scene.sync_with_music
        )
        _controllerStateFlow.value = currentState
        
        Log.i(tag, "Lighting config applied successfully")
        return Result.success(Unit)
    }
    
    override suspend fun setZoneColor(zoneId: String, hexColor: String): Result<Unit> {
        Log.d(tag, "setZoneColor() called: zone=$zoneId, color=$hexColor")
        Log.i(tag, "Setting zone $zoneId color to $hexColor")
        return Result.success(Unit)
    }
    
    override suspend fun setZoneBrightness(zoneId: String, brightness: Double): Result<Unit> {
        Log.d(tag, "setZoneBrightness() called: zone=$zoneId, brightness=$brightness")
        Log.i(tag, "Setting zone $zoneId brightness to $brightness")
        
        if (zoneId == currentState.active_zones.firstOrNull()) {
            currentState = currentState.copy(brightness = brightness)
            _controllerStateFlow.value = currentState
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun setZonePattern(zoneId: String, pattern: String): Result<Unit> {
        Log.d(tag, "setZonePattern() called: zone=$zoneId, pattern=$pattern")
        Log.i(tag, "Setting zone $zoneId pattern to $pattern")
        
        if (zoneId == currentState.active_zones.firstOrNull()) {
            currentState = currentState.copy(current_pattern = pattern)
            _controllerStateFlow.value = currentState
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun turnZoneOn(zoneId: String): Result<Unit> {
        Log.d(tag, "turnZoneOn() called: zone=$zoneId")
        Log.i(tag, "Turning on zone: $zoneId")
        
        val newActiveZones = (currentState.active_zones + zoneId).distinct()
        currentState = currentState.copy(
            is_on = true,
            active_zones = newActiveZones
        )
        _controllerStateFlow.value = currentState
        
        return Result.success(Unit)
    }
    
    override suspend fun turnZoneOff(zoneId: String): Result<Unit> {
        Log.d(tag, "turnZoneOff() called: zone=$zoneId")
        Log.i(tag, "Turning off zone: $zoneId")
        
        val newActiveZones = currentState.active_zones.filter { it != zoneId }
        currentState = currentState.copy(
            is_on = newActiveZones.isNotEmpty(),
            active_zones = newActiveZones
        )
        _controllerStateFlow.value = currentState
        
        return Result.success(Unit)
    }
    
    override suspend fun turnAllOn(): Result<Unit> {
        Log.d(tag, "turnAllOn() called")
        Log.i(tag, "Turning all zones ON")
        
        currentState = currentState.copy(
            is_on = true,
            active_zones = getSupportedZones()
        )
        _controllerStateFlow.value = currentState
        
        return Result.success(Unit)
    }
    
    override suspend fun turnAllOff(): Result<Unit> {
        Log.d(tag, "turnAllOff() called")
        Log.i(tag, "Turning all zones OFF")
        
        currentState = ControllerState(is_on = false)
        _controllerStateFlow.value = currentState
        
        return Result.success(Unit)
    }
    
    override suspend fun startMusicSync(audioSource: String): Result<Unit> {
        Log.d(tag, "startMusicSync() called with audioSource: $audioSource")
        Log.i(tag, "Starting music sync with audio source: $audioSource")
        
        currentState = currentState.copy(music_sync_active = true)
        _controllerStateFlow.value = currentState
        
        return Result.success(Unit)
    }
    
    override suspend fun stopMusicSync(): Result<Unit> {
        Log.d(tag, "stopMusicSync() called")
        Log.i(tag, "Stopping music sync")
        
        currentState = currentState.copy(music_sync_active = false)
        _controllerStateFlow.value = currentState
        
        return Result.success(Unit)
    }
    
    override fun getConnectionState(): ConnectionState {
        return _connectionStateFlow.value
    }
    
    override fun getControllerState(): ControllerState {
        return _controllerStateFlow.value
    }
    
    override fun getSupportedZones(): List<String> {
        return listOf(
            "dashboard",
            "door_left", 
            "door_right",
            "footwell",
            "ceiling"
        )
    }
    
    override fun getSupportedPatterns(): List<String> {
        return listOf(
            "static",
            "breathing",
            "pulse",
            "wave",
            "music_sync",
            "rainbow",
            "fade"
        )
    }
}

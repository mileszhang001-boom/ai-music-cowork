package com.music.appmain.controller

import android.util.Log
import com.example.layer3.api.IAmbientLightController
import com.example.layer3.api.ConnectionState
import com.example.layer3.api.ControllerState
import com.example.layer3.api.ControllerInfo
import com.example.layer3.api.model.LightingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisabledAmbientLightController : IAmbientLightController {

    private val tag = "DisabledAmbientLightCtrl"

    private val _connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _controllerStateFlow = MutableStateFlow(ControllerState())

    override val connectionStateFlow: Flow<ConnectionState> = _connectionStateFlow.asStateFlow()
    override val controllerStateFlow: Flow<ControllerState> = _controllerStateFlow.asStateFlow()

    private val disabledMessage = "氛围灯控制已禁用"

    override suspend fun connect(address: String): Result<Unit> {
        Log.w(tag, "connect() called but ambient light control is disabled. address=$address")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun disconnect(): Result<Unit> {
        Log.w(tag, "disconnect() called but ambient light control is disabled")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun discoverControllers(): Result<List<ControllerInfo>> {
        Log.w(tag, "discoverControllers() called but ambient light control is disabled")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun applyLightingConfig(config: LightingConfig): Result<Unit> {
        Log.w(tag, "applyLightingConfig() called but ambient light control is disabled. configId=${config.configId}")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun setZoneColor(zoneId: String, hexColor: String): Result<Unit> {
        Log.w(tag, "setZoneColor() called but ambient light control is disabled. zoneId=$zoneId, color=$hexColor")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun setZoneBrightness(zoneId: String, brightness: Double): Result<Unit> {
        Log.w(tag, "setZoneBrightness() called but ambient light control is disabled. zoneId=$zoneId, brightness=$brightness")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun setZonePattern(zoneId: String, pattern: String): Result<Unit> {
        Log.w(tag, "setZonePattern() called but ambient light control is disabled. zoneId=$zoneId, pattern=$pattern")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun turnZoneOn(zoneId: String): Result<Unit> {
        Log.w(tag, "turnZoneOn() called but ambient light control is disabled. zoneId=$zoneId")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun turnZoneOff(zoneId: String): Result<Unit> {
        Log.w(tag, "turnZoneOff() called but ambient light control is disabled. zoneId=$zoneId")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun turnAllOn(): Result<Unit> {
        Log.w(tag, "turnAllOn() called but ambient light control is disabled")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun turnAllOff(): Result<Unit> {
        Log.w(tag, "turnAllOff() called but ambient light control is disabled")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun startMusicSync(audioSource: String): Result<Unit> {
        Log.w(tag, "startMusicSync() called but ambient light control is disabled. audioSource=$audioSource")
        return Result.failure(Exception(disabledMessage))
    }

    override suspend fun stopMusicSync(): Result<Unit> {
        Log.w(tag, "stopMusicSync() called but ambient light control is disabled")
        return Result.failure(Exception(disabledMessage))
    }

    override fun getConnectionState(): ConnectionState {
        return _connectionStateFlow.value
    }

    override fun getControllerState(): ControllerState {
        return _controllerStateFlow.value
    }

    override fun getSupportedZones(): List<String> {
        return emptyList()
    }

    override fun getSupportedPatterns(): List<String> {
        return emptyList()
    }
}

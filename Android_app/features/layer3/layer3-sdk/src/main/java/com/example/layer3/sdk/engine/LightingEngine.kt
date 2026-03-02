package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.ILightingEngine
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.LightingCommand
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LightingEngine(
    private val context: Context
) : ILightingEngine {
    
    private val _lightingStateFlow = MutableStateFlow(LightingCommand(action = "set", theme = "default", intensity = 1.0))
    override val lightingStateFlow: Flow<LightingCommand> = _lightingStateFlow.asStateFlow()

    override suspend fun generateLighting(scene: SceneDescriptor): Result<LightingCommand> {
        return try {
            val command = LightingCommand(
                action = "set",
                theme = scene.scene_name ?: "default",
                intensity = scene.intent.energy_level.coerceIn(0.0, 1.0)
            )
            Result.success(command)
        } catch (e: Exception) {
            Logger.e("LightingEngine: Failed to generate lighting", e)
            Result.failure(e)
        }
    }

    override fun applyLighting(command: LightingCommand) {
        _lightingStateFlow.value = command
        Logger.i("LightingEngine: Applied lighting command: $command")
    }

    override fun setTheme(theme: String) {
        val current = _lightingStateFlow.value
        _lightingStateFlow.value = current.copy(theme = theme)
    }

    override fun setIntensity(intensity: Double) {
        val current = _lightingStateFlow.value
        _lightingStateFlow.value = current.copy(intensity = intensity.coerceIn(0.0, 1.0))
    }

    fun destroy() {
        Logger.i("LightingEngine: Destroyed")
    }
}

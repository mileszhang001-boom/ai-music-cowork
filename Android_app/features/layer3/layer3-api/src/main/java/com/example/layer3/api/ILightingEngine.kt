package com.example.layer3.api

import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.LightingCommand
import kotlinx.coroutines.flow.Flow

interface ILightingEngine {
    val lightingStateFlow: Flow<LightingCommand>
    
    suspend fun generateLighting(scene: SceneDescriptor): Result<LightingCommand>
    
    fun applyLighting(command: LightingCommand)
    
    fun setTheme(theme: String)
    
    fun setIntensity(intensity: Double)
}

package com.example.layer3.api

import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.EffectCommands
import kotlinx.coroutines.flow.Flow

interface IGenerationEngine {
    val effectCommandsFlow: Flow<EffectCommands>
    
    suspend fun generateScene(sceneId: String): Result<SceneDescriptor>
    
    suspend fun generateEffects(scene: SceneDescriptor): Result<EffectCommands>
    
    fun start()
    
    fun stop()
    
    fun destroy()
    
    fun updateConfig(config: Layer3Config)
}

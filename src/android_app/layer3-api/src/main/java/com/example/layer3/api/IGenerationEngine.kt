package com.example.layer3.api

import com.example.layer3.api.model.SceneDescriptor
import com.example.layer3.api.model.EffectCommands
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

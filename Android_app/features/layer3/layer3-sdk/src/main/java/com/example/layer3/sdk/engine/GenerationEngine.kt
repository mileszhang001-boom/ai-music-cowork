package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.IGenerationEngine
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.EffectCommands
import com.music.core.api.models.ContentCommand
import com.music.core.api.models.Commands
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class GenerationEngine(
    private val context: Context,
    private val contentEngine: ContentEngine,
    private val lightingEngine: LightingEngine,
    private val audioEngine: AudioEngine
) : IGenerationEngine {
    
    private val _effectCommandsFlow = MutableStateFlow(
        EffectCommands(scene_id = UUID.randomUUID().toString(), commands = Commands())
    )
    
    private var isRunning = false

    override val effectCommandsFlow: Flow<EffectCommands> = _effectCommandsFlow.asStateFlow()

    override suspend fun generateScene(sceneId: String): Result<SceneDescriptor> {
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun generateEffects(scene: SceneDescriptor): Result<EffectCommands> {
        return try {
            val playlistResult = contentEngine.generatePlaylist(scene)
            val lightingResult = lightingEngine.generateLighting(scene)
            val audioResult = audioEngine.generateAudioConfig(scene)

            val effectCommands = EffectCommands(
                scene_id = scene.scene_id,
                commands = Commands(
                    content = ContentCommand(action = "play", playlist = playlistResult.getOrNull()),
                    lighting = lightingResult.getOrNull(),
                    audio = audioResult.getOrNull()
                )
            )
            
            _effectCommandsFlow.value = effectCommands
            Logger.i("GenerationEngine: Generated effect commands")
            Result.success(effectCommands)
        } catch (e: Exception) {
            Logger.e("GenerationEngine: Failed to generate effects", e)
            Result.failure(e)
        }
    }

    override fun updateConfig(config: com.example.layer3.api.Layer3Config) {
        Logger.i("GenerationEngine: Config updated")
    }

    override fun start() {
        isRunning = true
        Logger.i("GenerationEngine: Started")
    }

    override fun stop() {
        isRunning = false
        Logger.i("GenerationEngine: Stopped")
    }

    override fun destroy() {
        stop()
        _effectCommandsFlow.value = EffectCommands(scene_id = UUID.randomUUID().toString(), commands = Commands())
        Logger.i("GenerationEngine: Destroyed")
    }

    fun isRunning(): Boolean = isRunning
}

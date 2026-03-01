package com.example.layer3.sdk.engine

import android.content.Context
import com.example.layer3.api.*
import com.example.layer3.api.model.*
import com.example.layer3.sdk.algorithm.SceneKeywordMatcher
import com.example.layer3.sdk.data.CacheManager
import com.example.layer3.sdk.data.SceneTemplateLoader
import com.example.layer3.sdk.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class GenerationEngine(
    private val context: Context,
    private val config: GenerationEngineConfig,
    private val contentEngine: ContentEngine,
    private val lightingEngine: LightingEngine,
    private val audioEngine: AudioEngine
) : IGenerationEngine {
    private val templateLoader = SceneTemplateLoader(context)
    private val keywordMatcher = SceneKeywordMatcher()
    private val sceneCache = CacheManager<SceneDescriptor>(maxSize = 20)
    
    private val _effectCommandsFlow = MutableStateFlow(
        EffectCommands(sequenceId = UUID.randomUUID().toString())
    )
    
    private var isRunning = false
    private var currentConfig: GenerationEngineConfig = config

    override val effectCommandsFlow: Flow<EffectCommands> = _effectCommandsFlow.asStateFlow()

    suspend fun initialize() {
        templateLoader.loadTemplates()
        Logger.i("GenerationEngine: Initialized")
    }

    override suspend fun generateScene(sceneId: String): Result<SceneDescriptor> {
        return try {
            sceneCache.get(sceneId)?.let {
                Logger.d("GenerationEngine: Returning cached scene $sceneId")
                return Result.success(it)
            }

            val template = templateLoader.getTemplateById(sceneId)
            val scene = if (template != null) {
                templateLoader.toSceneDescriptor(template)
            } else {
                generateDefaultScene(sceneId)
            }
            
            sceneCache.put(sceneId, scene)
            Logger.i("GenerationEngine: Generated scene $sceneId")
            Result.success(scene)
        } catch (e: Exception) {
            Logger.e("GenerationEngine: Failed to generate scene", e)
            Result.failure(Layer3Error.GenerationError("Failed to generate scene: ${e.message}", sceneId))
        }
    }

    override suspend fun generateEffects(scene: SceneDescriptor): Result<EffectCommands> {
        return try {
            val commands = mutableListOf<EffectCommand>()
            var priority = 0

            val playlistResult = contentEngine.generatePlaylist(scene)
            playlistResult.getOrNull()?.let { playlist ->
                commands.add(EffectCommand(
                    commandId = UUID.randomUUID().toString(),
                    engineType = EngineTypes.CONTENT,
                    action = "play_playlist",
                    params = mapOf("playlist_id" to playlist.id),
                    priority = priority++
                ))
            }

            val lightingResult = lightingEngine.generateLightingConfig(scene)
            lightingResult.getOrNull()?.let { lightingConfig ->
                commands.add(EffectCommand(
                    commandId = UUID.randomUUID().toString(),
                    engineType = EngineTypes.LIGHTING,
                    action = "apply_config",
                    params = mapOf("config_id" to lightingConfig.configId),
                    priority = priority++
                ))
            }

            val audioResult = audioEngine.generateAudioConfig(scene)
            audioResult.getOrNull()?.let { audioConfig ->
                commands.add(EffectCommand(
                    commandId = UUID.randomUUID().toString(),
                    engineType = EngineTypes.AUDIO,
                    action = "apply_config",
                    params = mapOf("config_id" to audioConfig.configId),
                    priority = priority++
                ))
            }

            val effectCommands = EffectCommands(
                commands = commands,
                sequenceId = UUID.randomUUID().toString(),
                sceneId = scene.sceneId
            )
            
            _effectCommandsFlow.value = effectCommands
            Logger.i("GenerationEngine: Generated ${commands.size} effect commands")
            Result.success(effectCommands)
        } catch (e: Exception) {
            Logger.e("GenerationEngine: Failed to generate effects", e)
            Result.failure(Layer3Error.GenerationError("Failed to generate effects: ${e.message}"))
        }
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
        sceneCache.clear()
        _effectCommandsFlow.value = EffectCommands(sequenceId = UUID.randomUUID().toString())
        Logger.i("GenerationEngine: Destroyed")
    }

    override fun updateConfig(config: Layer3Config) {
        currentConfig = config.generationEngine
        Logger.i("GenerationEngine: Config updated")
    }

    suspend fun executeScene(sceneId: String): Result<EffectCommands> {
        return try {
            val sceneResult = generateScene(sceneId)
            val scene = sceneResult.getOrThrow()
            
            generateEffects(scene)
        } catch (e: Exception) {
            Logger.e("GenerationEngine: Failed to execute scene", e)
            Result.failure(Layer3Error.GenerationError("Failed to execute scene: ${e.message}", sceneId))
        }
    }

    suspend fun findMatchingTemplates(keywords: List<String>): List<SceneDescriptor> {
        val templates = templateLoader.getTemplates()
        val matched = keywordMatcher.matchByKeywords(templates, keywords)
        
        return matched.map { (template, score) ->
            templateLoader.toSceneDescriptor(template)
        }
    }

    private fun generateDefaultScene(sceneId: String): SceneDescriptor {
        return SceneDescriptor(
            version = "2.0",
            sceneId = sceneId,
            sceneName = "Default Scene",
            sceneNarrative = "A default scene with neutral settings",
            intent = Intent(
                mood = Mood(valence = 0.5, arousal = 0.5),
                energyLevel = 0.5,
                atmosphere = "neutral"
            ),
            hints = Hints(
                music = MusicHints(
                    genres = listOf("pop"),
                    tempo = "medium"
                ),
                lighting = LightingHints(
                    colorTheme = "focused",
                    pattern = "static",
                    intensity = 0.5
                ),
                audio = AudioHints(
                    preset = "flat"
                )
            ),
            meta = SceneDescriptorMeta(
                source = "generated",
                confidence = 0.5
            )
        )
    }

    fun isRunning(): Boolean = isRunning
}

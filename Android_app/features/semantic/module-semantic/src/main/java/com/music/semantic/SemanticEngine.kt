package com.music.semantic

import android.content.Context
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import com.music.semantic.llm.LlmClient
import com.music.semantic.llm.PromptBuilder
import com.music.semantic.rules.RulesEngine
import com.music.semantic.template.TemplateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

class SemanticEngine(
    context: Context,
    private val llmApiKey: String = "",
    private val llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    private val llmModel: String = "qwen-plus",
    private val debounceThreshold: Double = 0.3
) {
    private val appContext = context.applicationContext
    
    private val templateManager = TemplateManager(appContext)
    private val rulesEngine = RulesEngine()
    private val promptBuilder = PromptBuilder()
    private val llmClient = LlmClient(llmApiKey, llmBaseUrl, llmModel)
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val _sceneDescriptorFlow = MutableStateFlow<SceneDescriptor?>(null)
    val sceneDescriptorFlow: StateFlow<SceneDescriptor?> = _sceneDescriptorFlow.asStateFlow()
    
    private val _engineStateFlow = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineStateFlow: StateFlow<EngineState> = _engineStateFlow.asStateFlow()
    
    private var currentDescriptor: SceneDescriptor? = null
    
    var isInitialized = false
        private set
    
    fun initialize(): Boolean {
        val loaded = templateManager.loadTemplates()
        isInitialized = loaded
        return loaded
    }
    
    suspend fun processSignals(signals: StandardizedSignals): Result<SceneDescriptor> = withContext(Dispatchers.Default) {
        _engineStateFlow.value = EngineState.Processing
        
        try {
            val ruleResult = rulesEngine.matchTemplate(signals)
            
            val newDescriptor = if (ruleResult.matched && ruleResult.templateId != null) {
                val template = templateManager.getTemplateById(ruleResult.templateId)
                if (template != null) {
                    templateManager.toSceneDescriptor(template)
                } else {
                    generateDescriptor(signals)
                }
            } else {
                generateDescriptor(signals)
            }
            
            if (shouldUpdateScene(newDescriptor)) {
                currentDescriptor = newDescriptor
                _sceneDescriptorFlow.value = newDescriptor
                _engineStateFlow.value = EngineState.Ready(newDescriptor)
                Result.success(newDescriptor)
            } else {
                val existing = currentDescriptor ?: newDescriptor
                _engineStateFlow.value = EngineState.Ready(existing)
                Result.success(existing)
            }
        } catch (e: Exception) {
            _engineStateFlow.value = EngineState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    private suspend fun generateDescriptor(signals: StandardizedSignals): SceneDescriptor {
        if (llmClient.isReady()) {
            return callLlm(signals)
        }
        return createDefaultDescriptor()
    }
    
    private fun shouldUpdateScene(newDescriptor: SceneDescriptor): Boolean {
        val current = currentDescriptor ?: return true
        
        if (current.scene_type != newDescriptor.scene_type) {
            return true
        }
        
        val valenceDiff = abs(current.intent.mood.valence - newDescriptor.intent.mood.valence)
        val arousalDiff = abs(current.intent.mood.arousal - newDescriptor.intent.mood.arousal)
        val energyDiff = abs(current.intent.energy_level - newDescriptor.intent.energy_level)
        
        val changeMagnitude = (valenceDiff + arousalDiff + energyDiff) / 3.0
        
        return changeMagnitude >= debounceThreshold
    }
    
    private suspend fun callLlm(signals: StandardizedSignals): SceneDescriptor {
        val messages = promptBuilder.buildMessages(signals)
        val result = llmClient.chat(messages)
        
        return result.fold(
            onSuccess = { content ->
                parseSceneDescriptor(content)
            },
            onFailure = {
                createDefaultDescriptor()
            }
        )
    }
    
    private fun parseSceneDescriptor(content: String): SceneDescriptor {
        return try {
            val cleanContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()
            json.decodeFromString(SceneDescriptor.serializer(), cleanContent)
        } catch (e: Exception) {
            createDefaultDescriptor()
        }
    }
    
    private fun createDefaultDescriptor(): SceneDescriptor {
        return SceneDescriptor(
            version = "2.0",
            scene_id = "scene_${UUID.randomUUID()}",
            scene_type = "default",
            scene_name = "默认场景",
            scene_narrative = "默认驾驶场景",
            intent = com.music.core.api.models.Intent(
                mood = com.music.core.api.models.Mood(
                    valence = 0.5,
                    arousal = 0.4
                ),
                energy_level = 0.4,
                atmosphere = "neutral"
            ),
            hints = com.music.core.api.models.Hints(
                music = com.music.core.api.models.MusicHints(
                    genres = listOf("pop"),
                    tempo = "moderate"
                ),
                lighting = com.music.core.api.models.LightingHints(
                    color_theme = "calm",
                    pattern = "steady",
                    intensity = 0.4
                ),
                audio = com.music.core.api.models.AudioHints(
                    preset = "standard"
                )
            ),
            announcement = "祝您驾驶愉快",
            meta = com.music.core.api.models.Meta(
                confidence = 0.5,
                source = "default"
            )
        )
    }
    
    fun getCurrentScene(): SceneDescriptor? = currentDescriptor
    
    fun forceUpdate(descriptor: SceneDescriptor) {
        currentDescriptor = descriptor
        _sceneDescriptorFlow.value = descriptor
        _engineStateFlow.value = EngineState.Ready(descriptor)
    }
    
    fun reset() {
        currentDescriptor = null
        _sceneDescriptorFlow.value = null
        _engineStateFlow.value = EngineState.Idle
    }
}

sealed class EngineState {
    object Idle : EngineState()
    object Processing : EngineState()
    data class Ready(val scene: SceneDescriptor) : EngineState()
    data class Error(val message: String) : EngineState()
}

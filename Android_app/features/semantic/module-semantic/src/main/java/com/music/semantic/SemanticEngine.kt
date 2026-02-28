package com.music.semantic

import android.content.Context
import com.music.core.api.models.SceneDescriptor
import com.music.core.api.models.StandardizedSignals
import com.music.semantic.llm.LlmClient
import com.music.semantic.llm.PromptBuilder
import com.music.semantic.rules.RulesEngine
import com.music.semantic.template.TemplateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

class SemanticEngine(
    context: Context,
    private val llmApiKey: String = "",
    private val llmBaseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    private val llmModel: String = "qwen-plus"
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
    
    var isInitialized = false
        private set
    
    fun initialize(): Boolean {
        val loaded = templateManager.loadTemplates()
        isInitialized = loaded
        return loaded
    }
    
    suspend fun processSignals(signals: StandardizedSignals): Result<SceneDescriptor> = withContext(Dispatchers.Default) {
        try {
            val ruleResult = rulesEngine.matchTemplate(signals)
            
            if (ruleResult.matched && ruleResult.templateId != null) {
                val template = templateManager.getTemplateById(ruleResult.templateId)
                if (template != null) {
                    val descriptor = templateManager.toSceneDescriptor(template)
                    return@withContext Result.success(descriptor)
                }
            }
            
            if (llmClient.isReady()) {
                val descriptor = callLlm(signals)
                return@withContext Result.success(descriptor)
            }
            
            val defaultDescriptor = createDefaultDescriptor()
            Result.success(defaultDescriptor)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}

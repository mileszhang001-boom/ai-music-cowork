package com.music.semantic.template

import android.content.Context
import android.util.Log
import com.music.core.api.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class TemplateData(
    val template_id: String,
    val scene_type: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val priority: Int = 1,
    val intent: TemplateIntent,
    val hints: TemplateHints,
    val announcement_templates: List<String>? = null,
    val triggers: Map<String, kotlinx.serialization.json.JsonElement>? = null
)

@Serializable
data class TemplateIntent(
    val mood: TemplateMood,
    val energy_level: Double,
    val atmosphere: String? = null,
    val constraints: TemplateConstraints? = null
)

@Serializable
data class TemplateConstraints(
    val max_volume_db: Int? = null,
    val avoid_explicit: Boolean? = null
)

@Serializable
data class TemplateMood(
    val valence: Double,
    val arousal: Double
)

@Serializable
data class TemplateHints(
    val music: TemplateMusicHints? = null,
    val lighting: TemplateLightingHints? = null,
    val audio: TemplateAudioHints? = null
)

@Serializable
data class TemplateMusicHints(
    val genres: List<String>? = null,
    val tempo: String? = null
)

@Serializable
data class TemplateLightingHints(
    val color_theme: String? = null,
    val pattern: String? = null,
    val intensity: Double? = null
)

@Serializable
data class TemplateAudioHints(
    val preset: String? = null
)

class TemplateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TemplateManager"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    private var templates: List<TemplateData> = emptyList()
    
    fun loadTemplates(): Boolean {
        return try {
            Log.d(TAG, "开始加载模板文件...")
            val templatesJson = context.assets.open("templates/preset_templates.json").bufferedReader().use { it.readText() }
            Log.d(TAG, "模板文件大小: ${templatesJson.length} 字符")
            val templatesContainer = json.decodeFromString<TemplatesContainer>(templatesJson)
            templates = templatesContainer.templates
            Log.i(TAG, "成功加载 ${templates.size} 个模板")
            true
        } catch (e: Exception) {
            Log.e(TAG, "加载模板失败: ${e.message}", e)
            false
        }
    }
    
    fun getTemplateById(templateId: String): TemplateData? {
        return templates.find { it.template_id == templateId }
    }
    
    fun getTemplatesByCategory(category: String): List<TemplateData> {
        return templates.filter { it.category == category }
    }
    
    fun getAllTemplates(): List<TemplateData> {
        return templates
    }
    
    fun toSceneDescriptor(template: TemplateData): SceneDescriptor {
        return SceneDescriptor(
            version = "2.0",
            scene_id = "scene_${UUID.randomUUID()}",
            scene_type = template.scene_type,
            scene_name = template.name,
            scene_narrative = template.description,
            intent = Intent(
                mood = Mood(
                    valence = template.intent.mood.valence,
                    arousal = template.intent.mood.arousal
                ),
                energy_level = template.intent.energy_level,
                atmosphere = template.intent.atmosphere,
                constraints = template.intent.constraints?.let {
                    Constraints(
                        max_volume_db = it.max_volume_db
                    )
                }
            ),
            hints = Hints(
                music = template.hints.music?.let {
                    MusicHints(genres = it.genres, tempo = it.tempo)
                },
                lighting = template.hints.lighting?.let {
                    LightingHints(
                        color_theme = it.color_theme,
                        pattern = it.pattern,
                        intensity = it.intensity
                    )
                },
                audio = template.hints.audio?.let {
                    AudioHints(preset = it.preset)
                }
            ),
            announcement = template.announcement_templates?.firstOrNull(),
            meta = Meta(
                confidence = 1.0,
                source = "template",
                template_id = template.template_id
            )
        )
    }
    
    @Serializable
    private data class TemplatesContainer(
        val version: String,
        val total: Int,
        val templates: List<TemplateData>
    )
}

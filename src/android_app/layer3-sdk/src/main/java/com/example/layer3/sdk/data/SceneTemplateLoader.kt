package com.example.layer3.sdk.data

import android.content.Context
import com.example.layer3.api.model.*
import com.example.layer3.sdk.util.JsonLoader
import com.example.layer3.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class SceneTemplate(
    @SerializedName("template_id") val templateId: String,
    @SerializedName("template_name") val templateName: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("keywords") val keywords: List<String> = emptyList(),
    @SerializedName("intent") val intentData: IntentData? = null,
    @SerializedName("hints") val hintsData: HintsData? = null,
    @SerializedName("lighting_preset") val lightingPreset: String? = null,
    @SerializedName("audio_preset") val audioPreset: String? = null
)

data class IntentData(
    @SerializedName("mood") val mood: MoodData? = null,
    @SerializedName("energy_level") val energyLevel: Double = 0.5,
    @SerializedName("atmosphere") val atmosphere: String = ""
)

data class MoodData(
    @SerializedName("valence") val valence: Double = 0.5,
    @SerializedName("arousal") val arousal: Double = 0.5
)

data class HintsData(
    @SerializedName("music") val music: MusicHintsData? = null,
    @SerializedName("lighting") val lighting: LightingHintsData? = null,
    @SerializedName("audio") val audio: AudioHintsData? = null
)

data class MusicHintsData(
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("artists") val artists: List<String> = emptyList(),
    @SerializedName("tempo") val tempo: String? = null
)

data class LightingHintsData(
    @SerializedName("color_theme") val colorTheme: String = "",
    @SerializedName("pattern") val pattern: String = "static",
    @SerializedName("intensity") val intensity: Double? = null
)

data class AudioHintsData(
    @SerializedName("preset") val preset: String = "",
    @SerializedName("spatial_mode") val spatialMode: String? = null
)

data class SceneTemplatesFile(
    @SerializedName("templates") val templates: List<SceneTemplate> = emptyList(),
    @SerializedName("version") val version: String = "1.0"
)

class SceneTemplateLoader(private val context: Context) {
    private val gson = Gson()
    private var templates: List<SceneTemplate> = emptyList()
    private val templateCache = CacheManager<SceneDescriptor>(maxSize = 20)

    suspend fun loadTemplates(fileName: String = "scene_templates.json"): Result<List<SceneTemplate>> {
        return try {
            val json = JsonLoader.loadJsonFromAssets(context, fileName)
            if (json == null) {
                Logger.w("SceneTemplateLoader: Templates file not found: $fileName")
                Result.success(emptyList())
            } else {
                val templatesFile = gson.fromJson(json, SceneTemplatesFile::class.java)
                this.templates = templatesFile.templates
                Logger.i("SceneTemplateLoader: Loaded ${templates.size} templates")
                Result.success(templates)
            }
        } catch (e: Exception) {
            Logger.e("SceneTemplateLoader: Failed to load templates", e)
            Result.failure(e)
        }
    }

    fun getTemplates(): List<SceneTemplate> = templates

    fun getTemplateById(templateId: String): SceneTemplate? {
        return templates.find { it.templateId == templateId }
    }

    fun findTemplatesByKeywords(keywords: List<String>): List<SceneTemplate> {
        if (keywords.isEmpty()) return emptyList()
        return templates.filter { template ->
            keywords.any { keyword ->
                template.keywords.any { it.equals(keyword, ignoreCase = true) } ||
                template.templateName.contains(keyword, ignoreCase = true) ||
                template.description?.contains(keyword, ignoreCase = true) == true
            }
        }
    }

    fun toSceneDescriptor(template: SceneTemplate): SceneDescriptor {
        val mood = template.intentData?.mood?.let {
            Mood(valence = it.valence, arousal = it.arousal)
        } ?: Mood()

        val intent = Intent(
            mood = mood,
            energyLevel = template.intentData?.energyLevel ?: 0.5,
            atmosphere = template.intentData?.atmosphere ?: ""
        )

        val musicHints = template.hintsData?.music?.let {
            MusicHints(
                genres = it.genres,
                artists = it.artists,
                tempo = it.tempo
            )
        }

        val lightingHints = template.hintsData?.lighting?.let {
            LightingHints(
                colorTheme = it.colorTheme,
                pattern = it.pattern,
                intensity = it.intensity
            )
        }

        val audioHints = template.hintsData?.audio?.let {
            AudioHints(
                preset = it.preset,
                spatialMode = it.spatialMode
            )
        }

        return SceneDescriptor(
            version = "2.0",
            sceneId = template.templateId,
            sceneName = template.templateName,
            sceneNarrative = template.description,
            intent = intent,
            hints = Hints(music = musicHints, lighting = lightingHints, audio = audioHints),
            meta = SceneDescriptorMeta(
                source = "template",
                templateId = template.templateId
            )
        )
    }

    fun getCachedSceneDescriptor(templateId: String): SceneDescriptor? {
        return templateCache.get(templateId)
    }

    fun cacheSceneDescriptor(templateId: String, descriptor: SceneDescriptor) {
        templateCache.put(templateId, descriptor)
    }
}

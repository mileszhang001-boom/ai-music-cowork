package com.music.core.api.models

import kotlinx.serialization.Serializable

@Serializable
data class SceneDescriptor(
    val version: String = "2.0",
    val scene_id: String,
    val scene_type: String,
    val scene_name: String? = null,
    val scene_narrative: String? = null,
    val intent: Intent,
    val hints: Hints,
    val announcement: String? = null,
    val meta: Meta? = null
)

@Serializable
data class Intent(
    val mood: Mood,
    val energy_level: Double,
    val atmosphere: String? = null,
    val social_context: String? = null,
    val constraints: Constraints? = null,
    val user_overrides: UserOverrides? = null
)

@Serializable
data class Mood(
    val valence: Double,
    val arousal: Double
)

@Serializable
data class Constraints(
    val content_rating: String? = null,
    val max_volume_db: Int? = null
)

@Serializable
data class UserOverrides(
    val exclude_tags: List<String>? = null
)

@Serializable
data class Hints(
    val music: MusicHints? = null,
    val lighting: LightingHints? = null,
    val audio: AudioHints? = null
)

@Serializable
data class MusicHints(
    val genres: List<String>? = null,
    val tempo: String? = null
)

@Serializable
data class LightingHints(
    val color_theme: String? = null,
    val pattern: String? = null,
    val intensity: Double? = null
)

@Serializable
data class AudioHints(
    val preset: String? = null
)

@Serializable
data class Meta(
    val confidence: Double? = null,
    val source: String? = null,
    val template_id: String? = null
)

package com.example.layer3.api.model

data class Mood(
    val valence: Double = 0.5,
    val arousal: Double = 0.5
)

data class EnergyCurvePoint(
    val time_offset_s: Double = 0.0,
    val energy_level: Double = 0.5
)

data class Constraints(
    val max_volume_db: Double? = null,
    val avoid_vocal: Boolean = false,
    val avoid_explicit: Boolean = false,
    val preferred_genres: List<String> = emptyList(),
    val blocked_artists: List<String> = emptyList(),
    val max_duration_sec: Double? = null
)

data class UserOverrides(
    val preferred_genre: String? = null,
    val preferred_artist: String? = null,
    val preferred_mood: String? = null,
    val specific_song: String? = null
)

data class Transition(
    val type: String = "fade",
    val duration_ms: Long? = null,
    val trigger_event: String? = null
)

data class Intent(
    val mood: Mood = Mood(),
    val energy_level: Double = 0.5,
    val energy_curve: List<EnergyCurvePoint> = emptyList(),
    val atmosphere: String = "",
    val constraints: Constraints? = null,
    val user_overrides: UserOverrides? = null,
    val transition: Transition? = null
)

data class MusicHints(
    val genres: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val eras: List<String> = emptyList(),
    val tempo: String? = null,
    val vocal_style: String? = null,
    val language: String? = null
)

data class LightingHints(
    val color_theme: String = "",
    val pattern: String = "static",
    val intensity: Double? = null,
    val transition: String? = null
)

data class AudioHints(
    val preset: String = "",
    val spatial_mode: String? = null,
    val bass_boost: Double? = null,
    val treble_boost: Double? = null
)

data class Hints(
    val music: MusicHints? = null,
    val lighting: LightingHints? = null,
    val audio: AudioHints? = null
)

data class Announcement(
    val text: String = "",
    val voice_style: String = "",
    val trigger: String? = null,
    val delay_ms: Long? = null
)

data class SceneDescriptorMeta(
    val created_at: Long = System.currentTimeMillis(),
    val source: String = "template",
    val template_id: String? = null,
    val confidence: Double? = null,
    val reasoning: String? = null
)

data class SceneDescriptor(
    val version: String = "2.0",
    val scene_id: String,
    val scene_name: String? = null,
    val scene_narrative: String? = null,
    val intent: Intent = Intent(),
    val hints: Hints? = null,
    val announcement: Announcement? = null,
    val meta: SceneDescriptorMeta? = null
)

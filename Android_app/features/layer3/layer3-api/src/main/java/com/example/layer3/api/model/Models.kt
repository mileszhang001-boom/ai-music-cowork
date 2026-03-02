package com.example.layer3.api.model

data class EffectCommands(
    val commands: List<EffectCommand> = emptyList(),
    val sequenceId: String,
    val sceneId: String? = null
)

data class EffectCommand(
    val commandId: String,
    val engineType: String,
    val action: String,
    val params: Map<String, Any>,
    val priority: Int = 0
)

object EngineTypes {
    const val CONTENT = "content"
    const val LIGHTING = "lighting"
    const val AUDIO = "audio"
}

data class Intent(
    val mood: Mood,
    val energyLevel: Double,
    val atmosphere: String? = null,
    val socialContext: String? = null,
    val constraints: Constraints? = null,
    val userOverrides: UserOverrides? = null
)

data class Mood(
    val valence: Double,
    val arousal: Double
)

data class Constraints(
    val contentRating: String? = null,
    val maxVolumeDb: Int? = null
)

data class UserOverrides(
    val excludeTags: List<String>? = null
)

data class Hints(
    val music: MusicHints? = null,
    val lighting: LightingHints? = null,
    val audio: AudioHints? = null
)

data class MusicHints(
    val genres: List<String>? = null,
    val tempo: String? = null
)

data class LightingHints(
    val colorTheme: String? = null,
    val pattern: String? = null,
    val intensity: Double? = null
)

data class AudioHints(
    val preset: String? = null
)

data class SceneDescriptorMeta(
    val confidence: Double? = null,
    val source: String? = null,
    val templateId: String? = null
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val tracks: List<Track>,
    val trackCount: Int,
    val totalDurationMs: Long? = null,
    val sceneId: String? = null,
    val tags: List<String>? = null
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val energy: Double? = null,
    val genre: String? = null
)

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

data class LightingConfig(
    val configId: String,
    val theme: String? = null,
    val colors: List<String>? = null,
    val pattern: String? = null,
    val intensity: Double? = null
)

data class AudioConfig(
    val configId: String,
    val preset: String? = null,
    val eqBass: Int? = null,
    val eqMid: Int? = null,
    val eqTreble: Int? = null,
    val volumeDb: Int? = null
)

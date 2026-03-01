package com.example.layer3.api.model

data class MoodMapping(
    val mood_id: String,
    val mood_name: String,
    val valence_range: ClosedRange<Double>,
    val arousal_range: ClosedRange<Double>,
    val music_hints: MusicMoodHints,
    val lighting_hints: LightingMoodHints,
    val audio_hints: AudioMoodHints
)

data class MusicMoodHints(
    val genres: List<String> = emptyList(),
    val tempo_range: ClosedRange<Int>? = null,
    val energy_range: ClosedRange<Double>? = null,
    val preferred_keys: List<String> = emptyList(),
    val vocal_preference: String? = null
)

data class LightingMoodHints(
    val color_palette: List<String> = emptyList(),
    val pattern: String = "static",
    val intensity_range: ClosedRange<Double>? = null,
    val animation_speed: Double = 1.0
)

data class AudioMoodHints(
    val eq_preset: String = "flat",
    val bass_adjustment: Double = 0.0,
    val treble_adjustment: Double = 0.0,
    val spatial_mode: String? = null
)

data class MoodMappingConfig(
    val mappings: List<MoodMapping> = emptyList(),
    val default_mapping: MoodMapping? = null,
    val version: String = "1.0"
)

object DefaultMoodMappings {
    val HAPPY = MoodMapping(
        mood_id = "happy",
        mood_name = "Happy",
        valence_range = 0.6..1.0,
        arousal_range = 0.5..1.0,
        music_hints = MusicMoodHints(
            genres = listOf("pop", "dance", "upbeat"),
            tempo_range = 100..180,
            energy_range = 0.6..1.0,
            vocal_preference = "any"
        ),
        lighting_hints = LightingMoodHints(
            color_palette = listOf("#FFD700", "#FFA500", "#FF6347"),
            pattern = "pulse",
            intensity_range = 0.6..1.0,
            animation_speed = 1.2
        ),
        audio_hints = AudioMoodHints(
            eq_preset = "pop",
            bass_adjustment = 0.1,
            treble_adjustment = 0.05
        )
    )

    val CALM = MoodMapping(
        mood_id = "calm",
        mood_name = "Calm",
        valence_range = 0.4..0.7,
        arousal_range = 0.0..0.4,
        music_hints = MusicMoodHints(
            genres = listOf("ambient", "classical", "acoustic"),
            tempo_range = 60..100,
            energy_range = 0.0..0.4,
            vocal_preference = "instrumental"
        ),
        lighting_hints = LightingMoodHints(
            color_palette = listOf("#4169E1", "#87CEEB", "#98FB98"),
            pattern = "breathing",
            intensity_range = 0.2..0.5,
            animation_speed = 0.5
        ),
        audio_hints = AudioMoodHints(
            eq_preset = "classical",
            bass_adjustment = -0.1,
            treble_adjustment = 0.0
        )
    )

    val ENERGETIC = MoodMapping(
        mood_id = "energetic",
        mood_name = "Energetic",
        valence_range = 0.5..1.0,
        arousal_range = 0.7..1.0,
        music_hints = MusicMoodHints(
            genres = listOf("electronic", "rock", "hip-hop"),
            tempo_range = 120..200,
            energy_range = 0.7..1.0,
            vocal_preference = "any"
        ),
        lighting_hints = LightingMoodHints(
            color_palette = listOf("#FF4500", "#FF1493", "#9400D3"),
            pattern = "music_sync",
            intensity_range = 0.7..1.0,
            animation_speed = 1.5
        ),
        audio_hints = AudioMoodHints(
            eq_preset = "electronic",
            bass_adjustment = 0.2,
            treble_adjustment = 0.1,
            spatial_mode = "surround_5_1"
        )
    )

    val FOCUSED = MoodMapping(
        mood_id = "focused",
        mood_name = "Focused",
        valence_range = 0.3..0.7,
        arousal_range = 0.4..0.7,
        music_hints = MusicMoodHints(
            genres = listOf("lo-fi", "ambient", "instrumental"),
            tempo_range = 70..110,
            energy_range = 0.2..0.5,
            vocal_preference = "instrumental"
        ),
        lighting_hints = LightingMoodHints(
            color_palette = listOf("#FFFFFF", "#F0F8FF", "#E6E6FA"),
            pattern = "static",
            intensity_range = 0.5..0.7,
            animation_speed = 0.0
        ),
        audio_hints = AudioMoodHints(
            eq_preset = "flat",
            bass_adjustment = 0.0,
            treble_adjustment = 0.0
        )
    )

    val ROMANTIC = MoodMapping(
        mood_id = "romantic",
        mood_name = "Romantic",
        valence_range = 0.5..0.9,
        arousal_range = 0.2..0.5,
        music_hints = MusicMoodHints(
            genres = listOf("r&b", "jazz", "soul"),
            tempo_range = 60..100,
            energy_range = 0.2..0.5,
            vocal_preference = "any"
        ),
        lighting_hints = LightingMoodHints(
            color_palette = listOf("#FF69B4", "#FFB6C1", "#DDA0DD"),
            pattern = "fade",
            intensity_range = 0.3..0.6,
            animation_speed = 0.3
        ),
        audio_hints = AudioMoodHints(
            eq_preset = "jazz",
            bass_adjustment = 0.05,
            treble_adjustment = -0.05
        )
    )
}
